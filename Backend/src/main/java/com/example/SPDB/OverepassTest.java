package com.example.SPDB;


import nice.fontaine.overpass.Overpass;
import nice.fontaine.overpass.models.query.statements.ComplexQuery;
import nice.fontaine.overpass.models.query.statements.NodeQuery;
import nice.fontaine.overpass.models.query.statements.RelationQuery;
import nice.fontaine.overpass.models.query.statements.WayQuery;
import nice.fontaine.overpass.models.query.statements.base.Statement;
import nice.fontaine.overpass.models.response.OverpassResponse;
import nice.fontaine.overpass.models.response.geometries.Way;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import retrofit2.Call;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import retrofit2.Response;

@RestController
public class OverepassTest {

    @GetMapping("/api")
    String TestOverpass() throws IOException {
        NodeQuery node = new NodeQuery.Builder()
                .timeout(25)
                .tag("natural", "water")
                .tag("water", "lake")
                .around(52.546, 19.700, (float) 15000.0)
                .build();
        WayQuery way = new WayQuery.Builder()
                .timeout(25)
                .tag("natural", "water")
                .tag("water", "lake")
                .around(52.546, 19.700, (float) 15000.0)
                .build();
        RelationQuery relation = new RelationQuery.Builder()
                .timeout(25)
                .tag("natural", "water")
                .tag("water", "lake")
                .around(52.546, 19.700, (float) 15000.0)
                .build();
        List<Statement> statements = new ArrayList() {{add(node); add(relation); add(way);}};


        ComplexQuery complex = new ComplexQuery.Builder()
                .union(statements)
                .build();

        Call<OverpassResponse> call = Overpass.ask(complex);
        StringBuilder builder = new StringBuilder();
        Response<OverpassResponse> response = call.execute();

        //return builder.toString();
        Way myway = (Way)response.body().elements[0];
        for(int i = 0; i < myway.nodes.length; i++){
            builder.append(myway.nodes[i] + "\n");
        }
        return builder.toString();
        //return myway.nodes.toString();
        //return response.body().toString();
    }
}

@Getter
@Setter
class Point{
    float latitude;
    float longitude;

    public Point(float latitude, float longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }
}

@Getter
@Setter
class SearchedObject{
    Tag tag;
    float distance;
    int time; //seconds

    public SearchedObject(com.example.SPDB.Tag tag, float distance, int time) {
        this.tag = tag;
        this.distance = distance;
        this.time = time;
    }
}

@Getter
@Setter
class Tag{
    String category;
    String objectType;

    public Tag(String category, String objectType) {
        this.category = category;
        this.objectType = objectType;
    }

}

@Getter
@Setter
class DataClass {
    Point startingPoint;
    List<SearchedObject> searchedObjects;
    int maxObjects;

    public DataClass(com.example.SPDB.Point startingPoint, List<SearchedObject> searchedObjects, int maxObjects) {
        this.startingPoint = startingPoint;
        this.searchedObjects = searchedObjects;
        this.maxObjects = maxObjects;
    }
}