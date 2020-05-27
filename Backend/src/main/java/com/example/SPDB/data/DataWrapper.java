package com.example.SPDB.data;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DataWrapper {
    Point startingPoint;
    SearchedObject searchedObject; //poszukiwane obiekty
    List<SearchedObject> searchedObjects; //lista obiektów, które muszą albo mogą być wokół poszukiwanych obiektów
    boolean isAnd; //true - koniunkcja false - alternatywa
    double precision; //jaki procent ma być przepuszczany przez graphhoppera
    VehicleType vehicleType;

    public DataWrapper(Point startingPoint, SearchedObject searchedObject, List<SearchedObject> searchedObjects, boolean isAnd, double precision, VehicleType vehicleType) {
        this.startingPoint = startingPoint;
        this.searchedObject = searchedObject;
        this.searchedObjects = searchedObjects;
        this.isAnd = isAnd;
        this.precision = precision;
        this.vehicleType = vehicleType;
    }

    @Override
    public String toString() {
        return "DataWrapper{\n" +
                " startingPoint=" + startingPoint + '\n' +
                " searchedObject=" + searchedObject + '\n' +
                " searchedObjects=" + searchedObjects + '\n' +
                " isAnd=" + isAnd + '\n' +
                " precision=" + precision + '\n' +
                " vehicleType=" + vehicleType + '\n' +
                '}';
    }

    public String prepareQuery() {
        StringBuilder query = new StringBuilder();
        StringBuilder nodePart = new StringBuilder("node");
        StringBuilder wayPart = new StringBuilder("way");
        StringBuilder relationPart = new StringBuilder("relation");

        query.append("[out:json][timeout:25];");
        for(Tag tag : this.searchedObject.getTags()){
            nodePart.append(tag);
            wayPart.append(tag);
            relationPart.append(tag);
        }

        String around = "(around:" + this.searchedObject.getDistance() + "," + this.startingPoint.getLat() + "," + this.startingPoint.getLng() + ");";
        nodePart.append(around);
        wayPart.append(around);
        relationPart.append(around);
        query.append("(").append(nodePart).append(wayPart).append(relationPart).append(");");
        query.append("out%20body;>;out;");
        return query.toString();
    }
}