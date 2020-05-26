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
        return "DataWrapper{" +
                "startingPoint=" + startingPoint +
                ", searchedObject=" + searchedObject +
                ", searchedObjects=" + searchedObjects +
                ", isAnd=" + isAnd +
                ", precision=" + precision +
                ", vehicleType=" + vehicleType +
                '}';
    }
}