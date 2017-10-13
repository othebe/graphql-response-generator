import generator.*;
import graphql.GraphQLError;
import graphql.execution.preparsed.PreparsedDocumentEntry;
import hydrator.implementation.RandomValueHydratorFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

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
            ResponseHydrator responseHydrator = new ResponseHydrator(schemaReader.getGraphQLSchema(), new RandomValueHydratorFactory());
            ResponseNode response = responseHydrator.hydrate(preparsedDocumentEntry.getDocument(), getOverride());

            System.out.println(response.toJsonString());
        }
    }

    /**
     * Returns a response override with the structure:
     * hero {
     *     name: Hans
     * }
     * @return
     */
    private static Map<String, Object> getOverride() {
        Map<String, Object> heroOverride = new HashMap<>();
        heroOverride.put("name", "Hans");

        Map<String, Object> rootOverride = new HashMap<>();
        rootOverride.put("hero", heroOverride);

        return rootOverride;
    }
}
