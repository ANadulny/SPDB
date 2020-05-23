package com.example.SPDB.data;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SearchedObject{
    List<Tag> tags;
    double distance;
    long time; //seconds

    public SearchedObject(List<Tag> tags, double distance, long time) {
        this.tags = tags;
        this.distance = distance;
        this.time = time;
    }
}