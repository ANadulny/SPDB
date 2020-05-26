package com.example.SPDB;

import nice.fontaine.overpass.Overpass;
import nice.fontaine.overpass.models.query.statements.ComplexQuery;
import nice.fontaine.overpass.models.query.statements.NodeQuery;
import nice.fontaine.overpass.models.query.statements.RelationQuery;
import nice.fontaine.overpass.models.query.statements.WayQuery;
import nice.fontaine.overpass.models.query.statements.base.Statement;
import nice.fontaine.overpass.models.response.OverpassResponse;
import nice.fontaine.overpass.models.response.geometries.Element;
import nice.fontaine.overpass.models.response.geometries.Relation;
import nice.fontaine.overpass.models.response.geometries.Way;
import nice.fontaine.overpass.models.response.geometries.Node;
import nice.fontaine.overpass.models.response.geometries.members.Member;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import retrofit2.Call;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Response;

@RestController
public class OverepassTest {
    public String CheckClass(Element element){
        if(element.getClass() == nice.fontaine.overpass.models.response.geometries.Way.class){
            return "way";
        }else if(element.getClass() == nice.fontaine.overpass.models.response.geometries.Node.class){
            return "node";
        }else if(element.getClass() == nice.fontaine.overpass.models.response.geometries.Relation.class){
            return "relation";
        }
        return "undefined";
    }

    public long ReturnOneNode(Element element){
        if(element.getClass() == nice.fontaine.overpass.models.response.geometries.Way.class){
            return ((Way) element).nodes[0];
        }else if(element.getClass() == nice.fontaine.overpass.models.response.geometries.Node.class){
            return ((Node) element).id;
        }else if(element.getClass() == nice.fontaine.overpass.models.response.geometries.Relation.class){
            Member member =  ((Relation)element).members[0];
            if(member.type.equals("way")){
                return 0;
            }
        }
        return -1;
    }

    @GetMapping("/testapi")
    String TestOverpass() throws IOException, JSONException {
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
        /*for(int i = 0; i < myway.nodes.length; i++){
            builder.append(myway.nodes[i] + "\n");
        }*/
        //for(int i = 0; i < response.body().elements.length; i++){
       //     builder.append(this.ReturnOneNode(response.body().elements[i]) + "\n");
       // }

        URL url = new URL("https://lz4.overpass-api.de/api/interpreter?data=[out:json][timeout:25];(way[%22natural%22=%22water%22][%22water%22=%22lake%22](around:15000,52.5464521,19.7008606);relation[%22natural%22=%22water%22][%22water%22=%22lake%22](around:15000,52.5464521,19.7008606););out%20body;%3E;out%20skel%20qt;");
        BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
        String str = "";

        while (null != (str = br.readLine())) {
            builder.append(str);
            //System.out.println(str);
        }

        JSONObject json = new JSONObject(builder.toString());
        return builder.toString();

        //return myway.nodes.toString();
        //return response.body();
    }

}