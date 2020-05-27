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
import java.util.ArrayList;
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

        // TODO create method
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(responseFromOverpass);
        }catch(JSONException e) {
            log.error("JSONException = {}", e.getMessage());
            return null;
        }

        //Przygotowanie obiektów dla grafu hoppera
        HashMap<Point, Long> searchingObjectsMap;
        try {
            searchingObjectsMap = getObjectPointsMapWithObjectId(jsonObject);
        }catch(JSONException e) {
            log.error("JSONException = {}", e.getMessage());
            return null;
        }

        log.info("searchingObjectsMap = {}", searchingObjectsMap.toString());
        searchingObjectsMap = graphHopperFilterTravelTime(searchingObjectsMap, wrapper);

        log.info("after graph hopper filter searchingObjectsMap = {}", searchingObjectsMap.toString());

//        log.info("jsonObject = {}", jsonObject);

        //Tutaj będzie graphhopper

        //Tutaj będzie zawężanie do tych, które spełniają warunki z listy

        //Tutaj będzie można już zwrócić odpowiedź

        return jsonObject.toString();
    }

    private HashMap<Point, Long> getObjectPointsMapWithObjectId(JSONObject jsonObject) throws JSONException {
        HashMap<Point, Long> findingPoints = new HashMap<Point, Long>();
        // wariant dla obiektu typu pojedynczy punkt na mapie
        JSONArray elements = jsonObject.getJSONArray("elements");
        for (int i=0; i < elements.length(); i++) {
            JSONObject element = elements.getJSONObject(i);
            double lat = Double.parseDouble(element.getString("lat"));
            double lon = Double.parseDouble(element.getString("lon"));
            long id = Long.parseLong(element.getString("id"));
            findingPoints.put(new Point(lat, lon), id);
        }

        // TODO wariant dla obiektów typu polygon

        return findingPoints;
    }

    private String getResponseFromOverpass(DataWrapper wrapper) throws MalformedURLException {
        String response = this.readDataFromURL(apiUrl + wrapper.prepareQuery());
        return response == null ? "" : response;
    }

    // TODO check if works
    private HashMap<Point, Long> graphHopperFilterTravelTime(HashMap<Point, Long> searchingObjects, DataWrapper wrapper) throws MalformedURLException {
        HashMap<Point, Long> filteredPoints = new HashMap<Point, Long>();
        for(Map.Entry<Point, Long> entry : searchingObjects.entrySet()) {
            Point key = entry.getKey();
            long value = entry.getValue();
            String graphHopperResponse = getGraphHopperResponse(wrapper.getStartingPoint(), key, wrapper.getVehicleType());
            log.info("graphHopperResponse = {}", graphHopperResponse);
            if (isTimeOk(graphHopperResponse, wrapper.getSearchedObject().getTime())) {
                filteredPoints.put(key, value);
            }
        }
        return filteredPoints;
    }

    private boolean isTimeOk(String graphHopperResponse, long time) {
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(graphHopperResponse);
            long travelingTime = jsonObject.getJSONArray("paths").getJSONObject(0).getLong("time");
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

    String readDataFromURL(String address) throws MalformedURLException {
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
