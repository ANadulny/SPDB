package Backend.SPDB.data;

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

    @Override
    public String toString() {
        return "SearchedObject{" +
                "tags=" + tags +
                ", distance=" + distance +
                ", time=" + time +
                '}';
    }
}