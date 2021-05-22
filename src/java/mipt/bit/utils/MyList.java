package mipt.bit.utils;

import java.util.List;

public class MyList {
    private String id;
    private List<Long> values;

    public MyList(String id, List<Long> values) {
        this.id = id;
        this.values = values;
    }

    public MyList() {
    }

    public String getId() {
        return id;
    }

    public List<Long> getValues() {
        return values;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setValues(List<Long> values) {
        this.values = values;
    }

    @Override
    public String toString() {
        return "MyList{" + "id='" + id + '\'' + ", values=" + values + '}';
    }
}