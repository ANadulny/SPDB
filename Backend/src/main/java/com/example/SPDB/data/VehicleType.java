package com.example.SPDB.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum VehicleType {
    CAR("Car"),
    BIKE("Bike"),
    FOOT("Foot");

    private final String stringValue;

    @Override
    public String toString() {
        return this.stringValue;
    }

    @JsonCreator
    public static VehicleType fromText(String text){
        for(VehicleType v : VehicleType.values()){
            if(v.toString().equals(text)){
                return v;
            }
        }
        throw new IllegalArgumentException();
    }
}
