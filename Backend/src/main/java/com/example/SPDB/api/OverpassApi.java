package com.example.SPDB.api;

import com.example.SPDB.data.Point;
import com.example.SPDB.data.SearchedObject;
import com.example.SPDB.data.Tag;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
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

    @PostMapping("/data")
    public String userData(@RequestParam String lat, @RequestParam String lng) throws JSONException {
        log.info("lat = {}", lat);
        log.info("lng = {}", lng);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("lat", lat);
        jsonObject.put("lng", lng);
        return jsonObject.toString();
    }

    @GetMapping("/api")
    String OverpassApi() throws IOException {
        //Przykładowy obiekt do testów - jeziora 15km wokół Płocka
        List<Tag> tags = new ArrayList() {{
            add(new Tag("natural", "water"));
            add(new Tag("water", "lake"));
        }};
        SearchedObject searchedObject = new SearchedObject(tags, 150000.0f, 10);

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
        nodePart.append("(around:" + searchedObject.getDistance() + "," + aroundPoint.getLongitude() + "," + aroundPoint.getLatitude() + ");");
        wayPart.append("(around:" + searchedObject.getDistance() + "," + aroundPoint.getLongitude() + "," + aroundPoint.getLatitude() + ");");
        relationPart.append("(around:" + searchedObject.getDistance() + "," + aroundPoint.getLongitude() + "," + aroundPoint.getLatitude() + ");");
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
