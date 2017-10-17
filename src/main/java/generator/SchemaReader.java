package generator;

import graphql.schema.GraphQLSchema;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.schema.idl.UnExecutableSchemaGenerator;

import java.io.File;

/**
 * Parses a graphQL schema.
 */
public class SchemaReader {
    private GraphQLSchema graphQLSchema;

    public SchemaReader(File schemaFile) {
        SchemaParser schemaParser = new SchemaParser();
        TypeDefinitionRegistry typeDefinitionRegistry = schemaParser.parse(schemaFile);

        this.graphQLSchema = UnExecutableSchemaGenerator.makeUnExecutableSchema(typeDefinitionRegistry);
    }

    public GraphQLSchema getGraphQLSchema() {
        return graphQLSchema;
    }
}
