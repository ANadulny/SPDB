package com.example.SPDB;


import nice.fontaine.overpass.Overpass;
import nice.fontaine.overpass.models.query.statements.NodeQuery;
import nice.fontaine.overpass.models.response.OverpassResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;

@RestController
public class OverepassTest {

    @GetMapping("/api")
    String TestOverpass() throws IOException {
        NodeQuery node = new NodeQuery.Builder()
                .timeout(25)
                .tag("amenity", "post_box")
                .around(52.5, 13.4, (float) 500.0)
                .build();

        Call<OverpassResponse> call = Overpass.ask(node);
        return call.execute().body().toString();
    }
}
