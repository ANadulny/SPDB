package Backend.SPDB.data;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class FutureObject {
    boolean foundObjectsIsOk;
    Point value;
    long key;

    public FutureObject(boolean foundObjectsIsOk, Point value, long key) {
        this.foundObjectsIsOk = foundObjectsIsOk;
        this.value = value;
        this.key = key;
    }
}
