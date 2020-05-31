package Backend.SPDB.data;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Point{
    double lat;
    double lng;

    public Point(double latitude, double longitude) {
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