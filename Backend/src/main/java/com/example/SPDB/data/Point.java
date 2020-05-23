package com.example.SPDB.data;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Point{
    double longitude;
    double latitude;

    public Point(double longitude, double latitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }
}