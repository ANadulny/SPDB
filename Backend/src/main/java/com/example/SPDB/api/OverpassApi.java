package com.example.SPDB.api;

import com.example.SPDB.data.*;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
public class OverpassApi {
    String apiUrl = "https://lz4.overpass-api.de/api/interpreter?data=";

    @PostMapping("/api")
//    String OverpassApi() throws IOException {
    String OverpassApi(@RequestBody DataWrapper wrapper) throws IOException {
        log.info("Overpass api");
        log.info("wrapper = {}", wrapper);
        log.info("wrapper query = {}", wrapper.prepareQuery());

        //W tym miejscu dostajemy listę wszystkich punktów, które są szukane przez użytkownika
        String responseFromOverpass = getResponseFromOverpass(wrapper);

        JSONObject jsonObject = createJsonObject(responseFromOverpass);

        //Przygotowanie obiektów dla grafu hoppera
        HashMap<Point, Long> searchingObjectsMap = getObjectPointsMapWithObjectId(jsonObject);
        if (searchingObjectsMap == null){
            log.error("Searching objects map is equal null!");
            return null;
        }

        log.info("searchingObjectsMap = {}", searchingObjectsMap.toString());
        searchingObjectsMap = graphHopperFilterTravelTime(searchingObjectsMap, wrapper);

        log.info("after graph hopper filter searchingObjectsMap = {}", searchingObjectsMap.toString());

        //Tutaj będzie zawężanie do tych, które spełniają warunki z listy
        searchingObjectsMap = filterSearchingObjectsWithUserConditions(searchingObjectsMap, wrapper);
        log.info("searchingObjectsMap = {}", searchingObjectsMap);

        //Tutaj jest zwracana odpowiedz po usunięciu obiektów nie spełniających warunki użytkownika
        jsonObject = removeInvalidObjects(jsonObject, searchingObjectsMap);
        log.info("jsonObject = {}", jsonObject);

        return "jsonObject.toString()";
//        return jsonObject.toString();
    }

    // TODO
    private JSONObject removeInvalidObjects(JSONObject jsonObject, HashMap<Point, Long> searchingObjectsMap) {
        return null;
    }

    // TODO
    private HashMap<Point, Long> filterSearchingObjectsWithUserConditions(HashMap<Point, Long> searchingObjectsMap, DataWrapper wrapper) {
        VehicleType vehicleType = wrapper.getVehicleType();
        List<SearchedObject> searchedObjects = wrapper.getSearchedObjects();
        boolean isAnd = wrapper.isAnd();

        return null;
    }

    private JSONObject createJsonObject(String response) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            return jsonObject;
        }catch(JSONException e) {
            log.error("JSONException = {}", e.getMessage());
            return null;
        }
    }

    private HashMap<Point, Long> getObjectPointsMapWithObjectId(JSONObject jsonObject) {
        HashMap<Point, Long> findingPoints = new HashMap<Point, Long>();
        // wariant dla obiektu typu pojedynczy punkt na mapie
        try {
            JSONArray elements = jsonObject.getJSONArray("elements");
            // TODO need changes
            if (elements.length() > 0 && isSinglePoint(elements.getJSONObject(0))) {
                log.info("json array of single points");
                for (int i=0; i < elements.length(); i++) {
                    JSONObject element = elements.getJSONObject(i);
                    double lat = Double.parseDouble(element.getString("lat"));
                    double lon = Double.parseDouble(element.getString("lon"));
                    long id = Long.parseLong(element.getString("id"));
                    findingPoints.put(new Point(lat, lon), id);
                }
            } else {
                // TODO
                log.info("json array of polygon objects");
            }

        }catch(JSONException e) {
            log.error("JSONException = {}", e.getMessage());
            return null;
        }

        // TODO wariant dla obiektów typu polygon

        return findingPoints;
    }

    private boolean isSinglePoint(JSONObject jsonObject) {
        return jsonObject.isNull("nodes");
    }

    private String getResponseFromOverpass(DataWrapper wrapper) throws MalformedURLException {
        String response = this.readDataFromURL(apiUrl + wrapper.prepareQuery());
        return response == null ? "" : response;
    }

    private HashMap<Point, Long> graphHopperFilterTravelTime(HashMap<Point, Long> searchingObjects, DataWrapper wrapper) throws MalformedURLException {
        HashMap<Point, Long> filteredPoints = new HashMap<Point, Long>();
        for(Map.Entry<Point, Long> entry : searchingObjects.entrySet()) {
            Point key = entry.getKey();
            long value = entry.getValue();
            if (isDistanceEnoughToCheckGraphHooper(wrapper.getPrecision(), wrapper.getStartingPoint(), key, wrapper.getSearchedObject().getDistance())) {
                String graphHopperResponse = getGraphHopperResponse(wrapper.getStartingPoint(), key, wrapper.getVehicleType());
                log.info("graphHopperResponse = {}", graphHopperResponse);
                if (isTimeOk(graphHopperResponse, wrapper.getSearchedObject().getTime())) {
                    filteredPoints.put(key, value);
                }
            } else {
                filteredPoints.put(key, value);
            }
        }
        return filteredPoints;
    }

    // TODO check if precision work correctly - (precision / 100)???
    private boolean isDistanceEnoughToCheckGraphHooper(double precision, Point startingPoint, Point endingPoint, double distance) {
        return calculateDistanceBetweenPoints(startingPoint, endingPoint) > (precision / 100) * distance ? true : false;
    }

    private double calculateDistanceBetweenPoints(Point startingPoint, Point endingPoint) {
        double x1, x2, y1, y2;
        x1=startingPoint.getLat();
        y1=startingPoint.getLng();
        x2=endingPoint.getLat();
        y2=endingPoint.getLng();
        return Math.sqrt((x2-x1)*(x2-x1) + (y2-y1)*(y2-y1));
    }

    private boolean isTimeOk(String graphHopperResponse, long time) {
        try {
            long travelingTime = createJsonObject(graphHopperResponse).getJSONArray("paths").getJSONObject(0).getLong("time");
            travelingTime /= 1000; // converting from ms to s
            log.info("travelingTime = {}", travelingTime);
            return travelingTime < time ? true : false;
        }catch(JSONException e) {
            log.error("JSONException in isTimeOk method = {}", e.getMessage());
            return false;
        }
    }

    private String getGraphHopperResponse(Point startingPoint, Point endingPoint, VehicleType vehicleType) throws MalformedURLException {
        String graphHopperUrl = "https://graphhopper.com/api/1/route?calc_points=false&key=9f251f13-8860-4ec1-b248-29334abc9e46&"; //point=52.2248,21.0005&point=52.2248,21.0035&vehicle=car
        return this.readDataFromURL(graphHopperUrl + "point=" + startingPoint.getLat() + "," + startingPoint.getLng() +
                "&point=" + endingPoint.getLat() + "," + endingPoint.getLng() + "&vehicle=" + vehicleType);
    }

    private String readDataFromURL(String address) throws MalformedURLException {
        URL url = new URL(address);
        StringBuilder builder = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
            String str = "";
            while (null != (str = br.readLine())) {
                builder.append(str);
            }
        }catch(IOException e){
            return null;
        }
        return builder.toString();
    }
}
