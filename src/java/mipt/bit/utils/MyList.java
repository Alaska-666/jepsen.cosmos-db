package mipt.bit.utils;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MyList {
    private String id;
    private String key;
    private List<Long> values;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public MyList(String id, String key, List<Long> values) {
        this.id = id;
        this.values = values;
        this.key = key;
    }

    public MyList(String id, String key, List<Long> values1, List<Long> values2) {
        this.id = id;
        this.values = Stream.of(values1, values2).flatMap(Collection::stream).collect(Collectors.toList());
        this.key = key;
    }

    public MyList() {
    }

    public String getId() {
        return id;
    }

    public Long getLongId() {
        return Long.valueOf(id);
    }

    public Long getLastValue() {
        return values.get(values.size() - 1);
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
        return "MyList{" + "id='" + id + '\'' + ", key='" + key + '\'' + ", values=" + values + '}';
    }
}