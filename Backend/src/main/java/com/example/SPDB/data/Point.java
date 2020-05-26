package com.example.SPDB.data;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Point{
    double lat;
    double lng;

    public Point(double longitude, double latitude) {
        this.lat = latitude;
        this.lng = longitude;
    }

    @Override
    public String toString() {
        return "Point{" +
                "lat=" + lat +
                ", lng=" + lng +
                '}';
    }
}