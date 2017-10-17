import generator.*;
import graphql.GraphQLError;
import graphql.execution.preparsed.PreparsedDocumentEntry;
import hydrator.ICustomScalarHydrator;
import hydrator.implementation.RandomValueHydratorFactory;

import java.io.File;
import java.util.*;

public class StarWarsResponseExample {
    private static final String SCHEMA_PATH = "starwars/schema.graphql";
    private static final String QUERY_PATH = "starwars/query.graphql";

    public static void main(String[] args) throws Exception {
        StringBuilder queryStringBuilder = new StringBuilder();
        File queryFile = new File(ClassLoader.getSystemClassLoader().getResource(QUERY_PATH).getFile());
        File schemaFile = new File(ClassLoader.getSystemClassLoader().getResource(SCHEMA_PATH).getFile());
        Scanner queryIn = new Scanner(queryFile);

        while (queryIn.hasNextLine()) {
            queryStringBuilder.append(queryIn.nextLine());
        }

        SchemaReader schemaReader = new SchemaReader(schemaFile);
        RequestParser requestParser = new RequestParser(schemaReader.getGraphQLSchema());

        PreparsedDocumentEntry preparsedDocumentEntry = requestParser.parseAndValidate(queryStringBuilder.toString());

        if (preparsedDocumentEntry.hasErrors()) {
            for (GraphQLError error : preparsedDocumentEntry.getErrors()) {
                System.out.println(error.getMessage());
            }
        } else {
            ResponseHydrator responseHydrator = new ResponseHydrator(schemaReader.getGraphQLSchema(), new RandomValueHydratorFactory(customScalarHydrator));
            ResponseNode response = responseHydrator.hydrate(preparsedDocumentEntry.getDocument(), getOverride());

            System.out.println(response.toJsonString());
        }
    }

    private final static ICustomScalarHydrator customScalarHydrator = new ICustomScalarHydrator() {
        @Override
        public boolean canHydrate(String typeName) {
            switch (typeName) {
                case "URI":
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public Object hydrate(String typeName) {
            if (typeName.compareTo("URI") == 0) {
                return "http://www.usetheforce.com";
            }

            return null;
        }
    };

    /**
     * Returns a response override with the structure:
     * hero {
     *     name: Hans
     *     friends: [
     *          { name: R2-D2 },
     *          { name: Luke Skywalker }
     *     ]
     * }
     * @return
     */
    private static Map<String, Object> getOverride() {
        Map<String, Object> heroOverride = new HashMap<>();
        heroOverride.put("name", "Hans");

        List<Map<String, Object>> friendsOverride = new LinkedList<>();
        Map<String, Object> friendR2D2 = new HashMap<>();
        friendR2D2.put("name", "R2-D2");
        friendsOverride.add(friendR2D2);
        Map<String, Object> friendLuke = new HashMap<>();
        friendLuke.put("name", "Luke Sykwalker");
        friendsOverride.add(friendLuke);
        heroOverride.put("friends", friendsOverride);

        Map<String, Object> rootOverride = new HashMap<>();
        rootOverride.put("hero", heroOverride);

        return rootOverride;
    }
}
