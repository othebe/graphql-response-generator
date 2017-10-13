package generator;

import java.util.Iterator;
import java.util.Map;

public class ResponseNode {
    private Object value;
    private Map<String, ResponseNode> children;

    private ResponseNode(Object value, Map<String, ResponseNode> children) {
        this.value = value;
        this.children = children;
    }

    public static ResponseNode createLeafNode(Object value) {
        return new ResponseNode(value, null);
    }

    public static ResponseNode createBranchNode(Map<String, ResponseNode> children) {
        return new ResponseNode(null, children);
    }

    public boolean isLeafNode() {
        return value != null;
    }

    public Object getValue() {
        return value;
    }

    public Map<String, ResponseNode> getChildren() {
        return children;
    }

    public String toJsonString() {
        return String.format("{%s}", toString());
    }

    @Override
    public String toString() {
        if (isLeafNode()) {
            return value.toString();
        } else {
            StringBuilder stringBuilder = new StringBuilder();

            Iterator<String> iterator = children.keySet().iterator();
            while (iterator.hasNext()) {
                String key = iterator.next();
                ResponseNode value = children.get(key);

                stringBuilder.append(String.format("\"%s\":", key));

                String childString = value.toString();
                if (value.isLeafNode()) {
                    stringBuilder.append(String.format("\"%s\"", childString));
                } else {
                    stringBuilder.append(String.format("{%s}", childString));
                }

                if (iterator.hasNext()) {
                    stringBuilder.append(",");
                }
            }

            return stringBuilder.toString();
        }
    }
}
