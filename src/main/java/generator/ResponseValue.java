package generator;

public class ResponseValue {
    private final Object value;

    public ResponseValue(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
