package generator;

import graphql.Scalars;
import graphql.language.*;
import graphql.schema.*;
import hydrator.IScalarHydrator;
import hydrator.IValueHydratorFactory;

import java.util.*;

/**
 * Hydrates the response to a query/mutation.
 */
public class ResponseHydrator {
    private final static int DEFAULT_LIST_SIZE = 2;

    private final GraphQLSchema graphQLSchema;
    private final IValueHydratorFactory valueHydratorFactory;

    private int listSize = DEFAULT_LIST_SIZE;

    public ResponseHydrator(GraphQLSchema graphQLSchema, IValueHydratorFactory valueHydratorFactory) {
        this.graphQLSchema = graphQLSchema;
        this.valueHydratorFactory = valueHydratorFactory;
    }

    /**
     * Sets the list size when hydrating lists.
     * @param listSize Determines how many items are populated into a list response.
     */
    public void setListSize(int listSize) {
        this.listSize = listSize;
    }

    /**
     * Hydrates a parsed tree using the valueHydratorFactory.
     * @param document Parsed tree document.
     * @return ResponseNode tree containing the hydrated response.
     */
    public ResponseNode hydrate(Document document) {
        return hydrate(document, null);
    }

    /**
     * Hydrates a parsed tree using the valueHydratorFactory and overrides values.
     * @param document Parsed tree document.
     * @param override Map containing values to override.
     * @return ResponseNode tree containing the hydrated response.
     */
    public ResponseNode hydrate(Document document, Map<String, Object> override) {
        Map<String, ResponseNode> children = new HashMap<>();

        for (Definition definition : document.getDefinitions()) {
            // Handle top-level query and mutation requests.
            if (definition instanceof OperationDefinition) {
                OperationDefinition operationDefinition = (OperationDefinition) definition;

                // TODO (othebe): Check for query or mutation.
                ResponseNode selectionResponse = hydrateSelectionRec(
                        graphQLSchema.getQueryType(),
                        operationDefinition.getSelectionSet(),
                        override);

                children.put(operationDefinition.getName(), selectionResponse);
            } else {
                throw new RuntimeException(String.format("Unhandled definition: %s", definition.toString()));
            }
        }

        return ResponseNode.createBranchNode(children);
    }

    /**
     * Helper function to hydrate a GraphQLType with fields provided in the selectionSet.
     * @param parentType SelectionSet parent type.
     * @param selectionSet Fields to include for the parent.
     * @param override Map containing values to override.
     * @return ResponseNode tree containing the hydrated response.
     * TODO (othebe): Simplify branching logic for leaf nodes, lists and overrides.
     */
    private ResponseNode hydrateSelectionRec(GraphQLOutputType parentType, SelectionSet selectionSet, Map<String, Object> override) {
        Map<String, ResponseNode> children = new HashMap<>();

        for (Selection selection : selectionSet.getSelections()) {
            if (selection instanceof Field) {
                Field field = (Field) selection;
                String fieldName = field.getName();
                GraphQLOutputType selectionType = getOutputTypeForField(parentType, fieldName);
                boolean isList = isListType(selectionType);
                boolean hasOverride = override != null && override.containsKey(fieldName);

                ResponseNode fieldResponse;

                boolean isLeafNode = field.getSelectionSet() == null;
                if (isLeafNode) {
                    // Leaf node w/overrides.
                    // Creates a leaf node w/the override values.
                    if (hasOverride) {
                        fieldResponse = isList ?
                                ResponseNode.createLeafNodeList((List<Object>) override.get(fieldName)) :
                                ResponseNode.createLeafNode(override.get(fieldName));
                    }
                    // Leaf node w/no overrides.
                    // Hydrate w/either a list of values, or a single value.
                    else {
                        if (isList) {
                            List<Object> responses = new LinkedList<>();
                            for (int i = 0; i < listSize; i++) {
                                responses.add(
                                        hydrateValue(selectionType, valueHydratorFactory));
                            }
                            fieldResponse = ResponseNode.createLeafNodeList(responses);
                        } else {
                            fieldResponse = ResponseNode.createLeafNode(
                                    hydrateValue(selectionType, valueHydratorFactory));
                        }
                    }
                } else {
                    // Branch node w/overrides.
                    // Creates a branch node w/a list of branching nodes, or a single branch node.
                    if (hasOverride) {
                        if (isList) {
                            List<Map<String, Object>> childOverrides = (List<Map<String, Object>>) override.get(fieldName);

                            List<ResponseNode> responses = new LinkedList<>();
                            for (Map<String, Object> childOverride : childOverrides) {
                                responses.add(
                                        hydrateSelectionRec(selectionType, field.getSelectionSet(), childOverride));
                            }
                            fieldResponse = ResponseNode.combine(responses);
                        } else {
                            // TODO (othebe): Consider asserting here, or replacing the map with ResponseNode.
                            Map<String, Object> childOverride = (Map<String, Object>) override.get(fieldName);
                            fieldResponse = hydrateSelectionRec(selectionType, field.getSelectionSet(), childOverride);
                        }
                    }
                    // Branch node w/no overrides.
                    // Create a branch node w/either a single hydrated node, or a list of branching nodes.
                    else {
                        if (isList) {
                            List<ResponseNode> responses = new LinkedList<>();
                            for (int i = 0; i < listSize; i++) {
                                responses.add(
                                        hydrateSelectionRec(selectionType, field.getSelectionSet(), null));
                            }
                            fieldResponse = ResponseNode.combine(responses);
                        } else {
                            fieldResponse = hydrateSelectionRec(selectionType, field.getSelectionSet(), null);
                        }
                    }
                }

                String selectionName = field.getAlias() == null ? field.getName() : field.getAlias();
                children.put(selectionName, fieldResponse);
            } else {
                // TODO (othebe): Handle fragments.
                throw new RuntimeException(String.format("Unhandled selection: %s", selection.toString()));
            }
        }

        return ResponseNode.createBranchNode(children);
    }

    /**
     * Provides the {@link graphql.schema.GraphQLOutputType} for a field in the graphQLType.
     * @param graphQLType The object containing the field.
     * @param fieldName The name of the field.
     * @return {@link graphql.schema.GraphQLOutputType}
     */
    private GraphQLOutputType getOutputTypeForField(GraphQLType graphQLType, String fieldName) {
        if (graphQLType instanceof GraphQLFieldsContainer) {
            return ((GraphQLFieldsContainer) graphQLType).getFieldDefinition(fieldName).getType();
        } else if (graphQLType instanceof GraphQLList) {
            return getOutputTypeForField(((GraphQLList) graphQLType).getWrappedType(), fieldName);
        } else {
            throw new RuntimeException(String.format("Extracting fields from unhandled output type %s.%s", graphQLType.getName(), fieldName));
        }
    }

    /**
     * Uses IValueHydratorFactory to hydrate values in the response.
     * @param graphQLType The value to hydrate.
     * @param valueHydratorFactory Value hydrator provider.
     * @return A hydrated value.
     * TODO (othebe): Custom scalars.
     */
    private Object hydrateValue(GraphQLType graphQLType, IValueHydratorFactory valueHydratorFactory) {
        GraphQLType resolvedType = graphQLType;

        // Resolve types for wrapped data.
        if (graphQLType instanceof GraphQLNonNull) {
            resolvedType = ((GraphQLNonNull) graphQLType).getWrappedType();
        }

        // List.
        // TODO (othebe): Add support for lists.
        if (resolvedType instanceof GraphQLList) {
            return hydrateValue(((GraphQLList) resolvedType).getWrappedType(), valueHydratorFactory);
        }

        // Enum.
        else if (resolvedType instanceof GraphQLEnumType) {
            return valueHydratorFactory.provideEnumHydrator().hydrateEnum(((GraphQLEnumType) resolvedType).getValues());
        }

        // Scalar.
        // TODO (othebe): Incomplete.
        else if (resolvedType instanceof GraphQLScalarType) {
            IScalarHydrator scalarHydrator = valueHydratorFactory.provideScalarHydrator();

            if (resolvedType.equals(Scalars.GraphQLString)) {
                return scalarHydrator.hydrateString();
            }

            else if (resolvedType.equals(Scalars.GraphQLID)) {
                return scalarHydrator.hydrateId();
            }

            else {
                throw new RuntimeException(String.format("Unhandled scalar type: %s", resolvedType.getName()));
            }
        }

        throw new RuntimeException(String.format("Unhandled value type: %s", resolvedType.getName()));
    }

    /**
     * Determines if the type is a list.
     * @param outputType Checks if this is a list.
     * @return True if list, else false.
     */
    private static boolean isListType(GraphQLOutputType outputType) {
        GraphQLType resolvedType = outputType;

        if (resolvedType instanceof GraphQLNonNull) {
            resolvedType = ((GraphQLNonNull) resolvedType).getWrappedType();
        }

        return resolvedType instanceof GraphQLList;
    }
}
