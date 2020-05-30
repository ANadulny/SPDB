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
import java.util.*;

@Slf4j
@RestController
public class OverpassApi {
    String apiUrl = "https://lz4.overpass-api.de/api/interpreter?data=";

    @PostMapping("/api")
    String OverpassApi(@RequestBody DataWrapper wrapper) throws IOException {
        log.info("Overpass api");
        log.info("wrapper = {}", wrapper);
        log.info("wrapper query = {}", wrapper.prepareQuery());

        //W tym miejscu dostajemy listę wszystkich punktów, które są szukane przez użytkownika
        String responseFromOverpass = getResponseFromOverpass(wrapper);

        JSONObject jsonObject = createJsonObject(responseFromOverpass);

        //Przygotowanie obiektów dla grafu hoppera
        HashMap<Long, Point> searchingObjectsMap = getObjectPointsMapWithObjectId(jsonObject);
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
        List<Long> searchingObjectsIdNumbers = new ArrayList<>(searchingObjectsMap.keySet());
        log.info("searchingObjectsIdNumbers = {}", searchingObjectsIdNumbers);
        jsonObject = removeInvalidObjects(jsonObject, searchingObjectsIdNumbers);
        log.info("jsonObject = {}", jsonObject);

        return jsonObject.toString();
    }

    // TODO
    private JSONObject removeInvalidObjects(JSONObject jsonObject, List<Long> searchingObjectsIdList) {
        log.info("removeInvalidObjects method");
        try {
            JSONArray elements = jsonObject.getJSONArray("elements");
            JSONArray newElementsJSONObject = new JSONArray();

            for (int i=0; i < elements.length(); i++) {
                JSONObject element = elements.getJSONObject(i);
                long id = Long.parseLong(element.getString("id"));
                String type = element.getString("type");

                if (isNodeType(type)) {
                    newElementsJSONObject.put(element);
                } else if (!searchingObjectsIdList.isEmpty() && searchingObjectsIdList.get(0) == id) {
                    log.info("is correct object found with id {}", id);
                    newElementsJSONObject.put(element);
                    searchingObjectsIdList.remove(0);
                }
            }

            jsonObject.put("elements", newElementsJSONObject);
        }catch(JSONException e) {
            log.error("JSONException = {}. It was error in removeInvalidObjects method!", e.getMessage());
            return new JSONObject();
        }
        return jsonObject;
    }

    // TODO
    private HashMap<Long, Point> filterSearchingObjectsWithUserConditions(HashMap<Long, Point> searchingObjectsMap, DataWrapper wrapper) {
        VehicleType vehicleType = wrapper.getVehicleType();
        List<SearchedObject> searchedObjects = wrapper.getSearchedObjects();
        boolean isAnd = wrapper.isAnd();

        return searchingObjectsMap;
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

    private HashMap<Long, Point> getObjectPointsMapWithObjectId(JSONObject jsonObject) {
        HashMap<Long, Point> findingPoints = new HashMap<Long, Point>();
        // wariant dla obiektu typu pojedynczy punkt na mapie
        try {
            JSONArray elements = jsonObject.getJSONArray("elements");
            List <Polygon> polygons = new ArrayList<>();
            for (int i=0; i < elements.length(); i++) {
                JSONObject element = elements.getJSONObject(i);
                long id = Long.parseLong(element.getString("id"));
                String type = element.getString("type");

                if (isNodeType(type)) {
                    // TODO posortowac raz i przegladac czy pierwszy elem zgadza sie , jak tak to ustaw punkt i usun elem z listy - uwaga case z dowama takimi samymi node'ami w liscie
                    log.info("is node type");
                    if(polygons.size() > 0) {
                        log.info("Polygon with id = {} filling with Point parameter", id);

                        //TODO needs work - usunac sortowanie nodow
                        boolean foundElement = false;
                        for (long node: polygons.get(0).getNodes()) {
                            if (node == id) {
                                if(!foundElement) {
                                    foundElement = true;
                                    double lat = Double.parseDouble(element.getString("lat"));
                                    double lon = Double.parseDouble(element.getString("lon"));
                                    findingPoints.put(polygons.get(0).getId(), new Point(lat, lon));
                                }
                                i++;
                                if ( i < elements.length()) {
                                    element = elements.getJSONObject(i);
                                    id = Long.parseLong(element.getString("id"));
                                }
                            }
                        }
                        polygons.remove(0);
                    } else {
                        log.warn("It is some problem in getObjectPointsMapWithObjectId in iteration = {}", i);
                    }
                } else if (isSinglePoint(element, type)) {
                    log.info("json array of single points");
                    double lat = Double.parseDouble(element.getString("lat"));
                    double lon = Double.parseDouble(element.getString("lon"));
                    findingPoints.put(id, new Point(lat, lon));
                } else {
                    log.info("json array of polygon objects");
                    ArrayList<Long> nodes = jsonStringToArray(element.getJSONArray("nodes"));
                    Collections.sort(nodes);
                    polygons.add(new Polygon(id, nodes));
                }
            }
        }catch(JSONException e) {
            log.error("JSONException = {}", e.getMessage());
            return null;
        }

        return findingPoints;
    }

    private boolean isNodeType(String type) {
        return Objects.equals(type, "node");
    }

    ArrayList<Long> jsonStringToArray(JSONArray jsonArray) throws JSONException {
        ArrayList<Long> longArray = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            longArray.add(Long.parseLong(jsonArray.getString(i)));
        }
        return longArray;
    }

    private boolean isSinglePoint(JSONObject jsonObject, String objectType) {
        return jsonObject.isNull("nodes") && !isNodeType((objectType));
    }

    private String getResponseFromOverpass(DataWrapper wrapper) throws MalformedURLException {
        String response = this.readDataFromURL(apiUrl + wrapper.prepareQuery());
        return response == null ? "" : response;
    }

    private HashMap<Long, Point> graphHopperFilterTravelTime(HashMap<Long, Point> searchingObjects, DataWrapper wrapper) throws MalformedURLException {
        HashMap<Long, Point> filteredPoints = new HashMap<Long, Point>();
        for(Map.Entry<Long, Point> entry : searchingObjects.entrySet()) {
            Point point = entry.getValue();
            long id = entry.getKey();
            if (isDistanceEnoughToCheckGraphHooper(wrapper.getPrecision(), wrapper.getStartingPoint(), point, wrapper.getSearchedObject().getDistance())) {
                String graphHopperResponse = getGraphHopperResponse(wrapper.getStartingPoint(), point, wrapper.getVehicleType());
                log.info("graphHopperResponse = {}", graphHopperResponse);
                if (isTimeOk(graphHopperResponse, wrapper.getSearchedObject().getTime())) {
                    filteredPoints.put(id, point);
                }
            } else {
                filteredPoints.put(id, point);
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
