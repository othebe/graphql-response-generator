package generator;

import java.util.*;

/**
 * This class represents a Node for the response document.
 */
public class ResponseNode {
    // TODO (othebe): Avoid list/non-list versions by wrapping into a single data type.
    private Object value;
    private List<Object> valueList;

    private Map<String, ResponseNode> children;
    private Map<String, List<ResponseNode>> childrenList;

    private ResponseNode() { }

    /**
     * Creates a leaf ResponseNode representing a single value.
     * @param value Value of node.
     * @return ResponseNode.
     */
    public static ResponseNode createLeafNode(Object value) {
        ResponseNode responseNode = new ResponseNode();
        responseNode.value = value;
        return responseNode;
    }

    /**
     * Creates a leaf ResponseNode representing a list of values.
     * @param valueList List of node values.
     * @return ResponseNode.
     */
    public static ResponseNode createLeafNodeList(List<Object> valueList) {
        ResponseNode responseNode = new ResponseNode();
        responseNode.valueList = valueList;
        return responseNode;
    }

    /**
     * Creates a branch ResponseNode to a single value for a type.
     * @param children Map of names to ResponseNodes.
     * @return ResponseNode.
     */
    public static ResponseNode createBranchNode(Map<String, ResponseNode> children) {
        ResponseNode responseNode = new ResponseNode();
        responseNode.children = children;
        return responseNode;
    }

    /**
     * Creates a branch ResponseNode to a list of values for a type.
     * @param childrenList Map of names to ResponseNodes.
     * @return ResponseNode.
     */
    public static ResponseNode createBranchNodeList(Map<String, List<ResponseNode>> childrenList) {
        ResponseNode responseNode = new ResponseNode();
        responseNode.childrenList = childrenList;
        return responseNode;
    }

    /**
     * Combines this node with another similar node by merging values to return a new ResponseNode.
     * - Leaf nodes combine values to return a single leaf node containing a list of merged values.
     * - Branch nodes combine branch children to return a single branch node containing a list of children mapped under the same key.
     * @param other Other ResponseNode that is also a leaf or branching node.
     * @return ResponseNode.
     */
    public ResponseNode combine(ResponseNode other) {
        if (isLeafNode()) {
            List<Object> combined = new LinkedList<>();
            combined.addAll(this.extractValuesAsList());
            combined.addAll(other.extractValuesAsList());

            return createLeafNodeList(combined);
        } else {
            Map<String, List<ResponseNode>> combined = new HashMap<>();
            combined.putAll(this.extractChildrenAsMappedList());

            Map<String, List<ResponseNode>> otherChildrenMappedList = other.extractChildrenAsMappedList();
            for (String key : otherChildrenMappedList.keySet()) {
                if (combined.containsKey(key)) {
                    combined.get(key).addAll(otherChildrenMappedList.get(key));
                }
            }

            return createBranchNodeList(combined);
        }
    }

    /**
     * Combines a list of ResponseNodes.
     * @param responses ResponseNode list.
     * @return ResponseNode.
     */
    public static ResponseNode combine(List<ResponseNode> responses) {
        return responses.stream()
                .reduce((r1, r2) -> r1.combine(r2))
                .orElse(null);
    }

    /**
     * Determines if this node is a leaf node.
     * @return True if leaf node, else false.
     */
    public boolean isLeafNode() {
        return value != null || valueList != null;
    }

    /**
     * Determines if this node represents a list of values, or list of children.
     * @return True if list, else false.
     */
    public boolean isList() {
        return valueList != null || childrenList != null;
    }

    /**
     * Returns a GQL spec JSON string for this document.
     * @return GQP spec JSON string.
     */
    public String toJsonString() {
        return String.format("{%s}", toString());
    }

    @Override
    // TODO (othebe) : Use strong casting to get around instanceof usage.
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();

        if (isLeafNode()) {
            if (isList()) {
                Iterator<Object> iterator = valueList.iterator();
                while (iterator.hasNext()) {
                    Object value = iterator.next();
                    if (value instanceof String) {
                        stringBuilder.append(String.format("\"%s\"", value.toString()));
                    } else {
                        stringBuilder.append(value.toString());
                    }

                    if (iterator.hasNext()) {
                        stringBuilder.append(",");
                    }
                }
                return String.format("[%s]", stringBuilder.toString());
            } else {
                if (value instanceof String) {
                    return String.format("\"%s\"", value.toString());
                } else {
                    return value.toString();
                }
            }
        } else {
            if (isList()) {
                Iterator<String> iterator = childrenList.keySet().iterator();
                while (iterator.hasNext()) {
                    String key = iterator.next();
                    List<ResponseNode> values = childrenList.get(key);

                    stringBuilder.append(String.format("\"%s\":[", key));

                    Iterator<ResponseNode> valueIterator = values.iterator();
                    while (valueIterator.hasNext()) {
                        ResponseNode value = valueIterator.next();
                        String childString = value.toString();
                        if (value.isLeafNode()) {
                            stringBuilder.append(String.format("%s", childString));
                        } else {
                            stringBuilder.append(String.format("{%s}", childString));
                        }

                        if (valueIterator.hasNext()) {
                            stringBuilder.append(",");
                        }
                    }
                    stringBuilder.append("]");

                    if (iterator.hasNext()) {
                        stringBuilder.append(",");
                    }
                }

                return stringBuilder.toString();
            } else {
                Iterator<String> iterator = children.keySet().iterator();
                while (iterator.hasNext()) {
                    String key = iterator.next();
                    ResponseNode value = children.get(key);

                    stringBuilder.append(String.format("\"%s\":", key));

                    String childString = value.toString();
                    if (value.isLeafNode()) {
                        stringBuilder.append(String.format("%s", childString));
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

    /**
     * Returns a list of values represented by this node.
     * @return
     */
    private List<Object> extractValuesAsList() {
        List<Object> list = new LinkedList<>();
        if (isList()) {
            list.addAll(valueList);
        } else {
            list.add(value);
        }

        return list;
    }

    /**
     * Returns a map containing a list of children represented by this node.
     * @return
     */
    private Map<String, List<ResponseNode>> extractChildrenAsMappedList() {
        Map<String, List<ResponseNode>> mappedList = new HashMap<>();
        if (isList()) {
            for (String key : childrenList.keySet()) {
                List<ResponseNode> responseNodes = new LinkedList<>(childrenList.get(key));
                mappedList.put(key, responseNodes);
            }
        } else {
            for (String key : children.keySet()) {
                List<ResponseNode> responseNodes = new LinkedList<>();
                responseNodes.add(children.get(key));
                mappedList.put(key, responseNodes);
            }
        }

        return mappedList;
    }
}
