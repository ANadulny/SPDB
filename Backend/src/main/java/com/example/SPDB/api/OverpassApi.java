package com.example.SPDB.api;

import com.example.SPDB.data.*;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
public class OverpassApi {
    String apiUrl = "https://lz4.overpass-api.de/api/interpreter?data=";

    @PostMapping("/api")
//    String OverpassApi(@RequestBody DataWrapper wrapper) throws IOException {
    String OverpassApi() throws IOException {
        log.info("Overpass api");
//        log.info("wrapper = {}", wrapper);

        // == Testing data ==
        //Przykładowy obiekt do testów - jeziora 15km wokół Płocka
        List<Tag> tags = new ArrayList() {{
            add(new Tag("natural", "water"));
            add(new Tag("water", "lake"));
        }};
        SearchedObject searchedObject = new SearchedObject(tags, 150000.0f, 10);

        DataWrapper testingWrapper = new DataWrapper(new Point(52.5464521,19.7008606), searchedObject, new ArrayList<SearchedObject>(), false, 1000, VehicleType.CAR);
        log.info("testingWrapper = {}", testingWrapper);
        String query = prepareQuery(searchedObject, new Point(52.5464521,19.7008606));
        
        //W tym miejscu dostajemy listę wszystkich punktów, które są szukane przez użytkownika
        String responseFromOverpass = this.readDataFromURL(apiUrl + query);
        if(responseFromOverpass == null){
            return null;
        }
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(responseFromOverpass);
        }catch(JSONException e) {
            return null;
        }
        //Tutaj jest już obiekt

        //Tutaj będzie graphhopper

        //Tutaj będzie zawężanie do tych, które spełniają warunki z listy

        //Tutaj będzie można już zwrócić odpowiedź

        return jsonObject.toString();
    }

    String prepareQuery(SearchedObject searchedObject, Point aroundPoint){
        StringBuilder query = new StringBuilder();
        StringBuilder nodePart = new StringBuilder("node");
        StringBuilder wayPart = new StringBuilder("way");
        StringBuilder relationPart = new StringBuilder("relation");

        query.append("[out:json][timeout:25];");
        for(Tag tag : searchedObject.getTags()){
            nodePart.append("[\"" + tag.getCategory() + "\"=\"" + tag.getObjectType() + "\"]");
            wayPart.append("[\"" + tag.getCategory() + "\"=\"" + tag.getObjectType() + "\"]");
            relationPart.append("[\"" + tag.getCategory() + "\"=\"" + tag.getObjectType() + "\"]");
        }
        nodePart.append("(around:" + searchedObject.getDistance() + "," + aroundPoint.getLng() + "," + aroundPoint.getLat() + ");");
        wayPart.append("(around:" + searchedObject.getDistance() + "," + aroundPoint.getLng() + "," + aroundPoint.getLat() + ");");
        relationPart.append("(around:" + searchedObject.getDistance() + "," + aroundPoint.getLng() + "," + aroundPoint.getLat() + ");");
        query.append("(").append(nodePart).append(wayPart).append(relationPart).append(");");
        query.append("out%20body;>;out;");
        return query.toString();
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
