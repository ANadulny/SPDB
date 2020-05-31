package Backend.SPDB.data;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Tag{
    String category;
    String objectType;

    public Tag(String category, String objectType) {
        this.category = category;
        this.objectType = objectType;
    }

    @Override
    public String toString() {
        return "[\"" + this.category + "\"=\"" + this.objectType + "\"]";
    }
}
