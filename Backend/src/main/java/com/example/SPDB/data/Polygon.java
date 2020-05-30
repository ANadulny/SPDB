package com.example.SPDB.data;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;

@Getter
@Setter
public class Polygon {
    private Long id;
    private ArrayList<Long> nodes;
    private Point nearestPoint;

    public Polygon(Long id, ArrayList<Long> nodes) {
        this.id = id;
        this.nodes = nodes;
        this.nearestPoint = new Point(0,0);
    }

    @Override
    public String toString() {
        return "Polygon{" +
                "id=" + id +
                ", nodes=" + nodes +
                ", nearestPoint=" + nearestPoint +
                '}';
    }
}
