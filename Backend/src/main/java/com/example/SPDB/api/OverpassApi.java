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
        String responseFromOverpass = getResponseFromOverpass(wrapper.prepareQuery());

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
//        Collections.sort(searchingObjectsIdNumbers);
        jsonObject = removeInvalidObjects(jsonObject, searchingObjectsIdNumbers);
        log.info("jsonObject = {}", jsonObject);

        return jsonObject.toString();
    }

    private JSONObject removeInvalidObjects(JSONObject jsonObject, List<Long> searchingObjectsIdList) {
        log.info("removeInvalidObjects method");
        try {
            JSONArray elements = jsonObject.getJSONArray("elements");
            JSONArray newElementsJSONObject = new JSONArray();

            for (int i=0; i < elements.length(); i++) {
                JSONObject element = elements.getJSONObject(i);
                long id = Long.parseLong(element.getString("id"));
                String type = element.getString("type");

                if (isNodeTypeWithoutTag(type, element)) {
                    newElementsJSONObject.put(element);
                } else if (!searchingObjectsIdList.isEmpty()) {
                    for (int j = 0; j < searchingObjectsIdList.size(); j++) {
                        if (searchingObjectsIdList.get(j) == id) {
                            log.info("is correct object found with id {}", id);
                            newElementsJSONObject.put(element);
                            searchingObjectsIdList.remove(j);
                        }
                    }
                }
            }

            if (!searchingObjectsIdList.isEmpty()) {
                log.error("searchingObjectsIdList is not empty and = {}", searchingObjectsIdList);
            }
            jsonObject.put("elements", newElementsJSONObject);
        }catch(JSONException e) {
            log.error("JSONException = {}. It was error in removeInvalidObjects method!", e.getMessage());
            return new JSONObject();
        }
        return jsonObject;
    }

    private HashMap<Long, Point> filterSearchingObjectsWithUserConditions(HashMap<Long, Point> searchingObjectsMap, DataWrapper wrapper) {
        List<SearchedObject> searchedObjects = wrapper.getSearchedObjects();
        HashMap<Long, Point> filteredSearchingObjectMap = new HashMap<>();
        log.info("searchedObjects = {}", searchedObjects);

        for(Map.Entry<Long, Point> entry : searchingObjectsMap.entrySet()) {
            Point point = entry.getValue();
            boolean foundObjectsIsOk = wrapper.isAnd() ? true : false;
            for (SearchedObject searchedObject : searchedObjects) {
                try {
                    String responseFromOverpass = getResponseFromOverpass(prepareConditionQuery(searchedObject, point));
                    JSONObject jsonObject = createJsonObject(responseFromOverpass);
                    if (wrapper.isAnd() && !isFoundSearchingConditionObject(jsonObject)) {
                        foundObjectsIsOk = false;
                    } else if (!wrapper.isAnd() && isFoundSearchingConditionObject(jsonObject)) {
                        foundObjectsIsOk = true;
                        continue;
                    }
                } catch (MalformedURLException e) {
                    log.error("MalformedURLException in filterSearchingObjectsWithUserConditions");
                }
            }

            if (foundObjectsIsOk) {
                filteredSearchingObjectMap.put(entry.getKey(), entry.getValue());
            }
        }
        return filteredSearchingObjectMap;
    }

    private boolean isFoundSearchingConditionObject(JSONObject jsonObject) {
        try {
            JSONArray elements = jsonObject.getJSONArray("elements");
            return elements == null ? false : true;
        }catch(JSONException e) {
            log.error("JSONException in isFoundSearchingConditionObject = {}", e.getMessage());
            return false;
        }
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

                if (isNodeTypeWithoutTag(type, element)) {
                    log.info("is node type without tag in iteration = {}", i);

                    boolean foundElement = false;
                    for (int j = 0; j < polygons.size() && !foundElement; j++) {
                        Polygon polygon = polygons.get(j);
                        if (polygon.isFirstNode() && polygon.getFirstNode() == id) {
                            log.info("Polygon with id = {} filling with Point parameter", polygons.get(j).getId());
                            foundElement = true;
                            double lat = Double.parseDouble(element.getString("lat"));
                            double lon = Double.parseDouble(element.getString("lon"));
                            findingPoints.put(polygons.get(j).getId(), new Point(lat, lon));
                            polygons.remove(j);
                        }
                    }
                } else if (isSinglePoint(element)) {
                    log.info("json array of single points in iteration = {}", i);
                    double lat = Double.parseDouble(element.getString("lat"));
                    double lon = Double.parseDouble(element.getString("lon"));
                    findingPoints.put(id, new Point(lat, lon));
                } else {
                    log.info("json array of polygon objects in iteration = {}", i);
                    ArrayList<Long> nodes = jsonStringToArray(element.getJSONArray("nodes"));
                    polygons.add(new Polygon(id, nodes));
                }
            }

            if (!polygons.isEmpty()) {
                log.error("It is problem in getObjectPointsMapWithObjectId. Polygons list is not empty and polygons = {}", polygons);
            }
        }catch(JSONException e) {
            log.error("JSONException = {}", e.getMessage());
            return null;
        }

        return findingPoints;
    }

    private boolean isNodeTypeWithoutTag(String type, JSONObject jsonObject) {
        return Objects.equals(type, "node") && jsonObject.isNull("tags");
    }

    ArrayList<Long> jsonStringToArray(JSONArray jsonArray) throws JSONException {
        ArrayList<Long> longArray = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            longArray.add(Long.parseLong(jsonArray.getString(i)));
        }
        return longArray;
    }

    private boolean isSinglePoint(JSONObject jsonObject) {
        return jsonObject.isNull("nodes");
    }

    private String getResponseFromOverpass(String query) throws MalformedURLException {
        String response = this.readDataFromURL(apiUrl + query);
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
                if (graphHopperResponse == null) {
                    log.warn("graphHopperResponse is null for {}. API limit was reached", entry);
                    filteredPoints.put(id, point);
                } else if (isTimeOk(graphHopperResponse, wrapper.getSearchedObject().getTime())) {
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

    private String prepareConditionQuery(SearchedObject searchedObject, Point startingPoint) {
        StringBuilder query = new StringBuilder();
        StringBuilder nodePart = new StringBuilder("node");
        StringBuilder wayPart = new StringBuilder("way");
        StringBuilder relationPart = new StringBuilder("relation");

        query.append("[out:json][timeout:25];");
        for(Tag tag : searchedObject.getTags()){
            nodePart.append(tag);
            wayPart.append(tag);
            relationPart.append(tag);
        }

        String around = "(around:" + searchedObject.getDistance() + "," + startingPoint.getLat() + "," + startingPoint.getLng() + ");";
        nodePart.append(around);
        wayPart.append(around);
        relationPart.append(around);
        query.append("(").append(nodePart).append(wayPart).append(relationPart).append(");");
        query.append(">;out;");
        return query.toString();
    }
}
