package com.example.SPDB.data;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DataWrapper {
    Point startingPoint;
    List<SearchedObject> searchedObjects;
    int maxObjects;

    public DataWrapper(Point startingPoint, List<SearchedObject> searchedObjects, int maxObjects) {
        this.startingPoint = startingPoint;
        this.searchedObjects = searchedObjects;
        this.maxObjects = maxObjects;
    }
}