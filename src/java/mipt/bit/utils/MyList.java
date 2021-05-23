package mipt.bit.utils;

import java.util.List;

public class MyList {
    private String id;
    private String key;
    private List<Integer> values;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public MyList(String id, String key, List<Integer> values) {
        this.id = id;
        this.values = values;
        this.key = key;
    }

    public MyList(String id, List<Integer> values) {
        this.id = id;
        this.values = values;
    }

    public MyList() {
    }

    public String getId() {
        return id;
    }

    public List<Integer> getValues() {
        return values;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setValues(List<Integer> values) {
        this.values = values;
    }

    @Override
    public String toString() {
        return "MyList{" + "id='" + id + '\'' + ", key='" + key + '\'' + ", values=" + values + '}';
    }
}