package jepsen.cosmosDB;

import java.util.List;

public class MyList {
    String id;
    List<Integer> values;

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
}
