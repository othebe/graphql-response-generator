package generator;

import graphql.InvalidSyntaxError;
import graphql.execution.preparsed.NoOpPreparsedDocumentProvider;
import graphql.execution.preparsed.PreparsedDocumentEntry;
import graphql.execution.preparsed.PreparsedDocumentProvider;
import graphql.language.Document;
import graphql.parser.Parser;
import graphql.schema.GraphQLSchema;
import graphql.validation.ValidationError;
import graphql.validation.Validator;
import org.antlr.v4.runtime.misc.ParseCancellationException;

import java.util.List;

/**
 * Parses a query/mutation request.
 */
public class RequestParser {
    private final GraphQLSchema graphQLSchema;
    private final PreparsedDocumentProvider preparsedDocumentProvider;

    public RequestParser(GraphQLSchema graphQLSchema) {
        this.graphQLSchema = graphQLSchema;
        this.preparsedDocumentProvider = NoOpPreparsedDocumentProvider.INSTANCE;
    }

    /**
     * Parses and validates a query.
     * @param query Query to parse and validate.
     * @return {@link graphql.execution.preparsed.PreparsedDocumentEntry} containing the document or errors.
     */
    public PreparsedDocumentEntry parseAndValidate(String query) {
        return preparsedDocumentProvider.get(query, queryArg -> {
            // Parse.
            ParseResult parseResult = parse(queryArg);
            if (parseResult.isFailure()) {
                return new PreparsedDocumentEntry(InvalidSyntaxError.toInvalidSyntaxError(parseResult.getException()));
            }

            // Validate.
            List<ValidationError> errors = validate(parseResult.getDocument());
            if (!errors.isEmpty()) {
                return new PreparsedDocumentEntry(errors);
            }

            return new PreparsedDocumentEntry(parseResult.getDocument());
        });
    }

    private ParseResult parse(String query) {
        Parser parser = new Parser();

        Document document = null;
        try {
            document = parser.parseDocument(query);
        } catch (ParseCancellationException e) {
            ParseResult.ofError(e);
        }

        return ParseResult.of(document);
    }

    private List<ValidationError> validate(Document document) {
        Validator validator = new Validator();
        return validator.validateDocument(graphQLSchema, document);
    }

    private static class ParseResult {
        private final Document document;
        private final Exception exception;

        private ParseResult(Document document, Exception exception) {
            this.document = document;
            this.exception = exception;
        }

        private boolean isFailure() {
            return document == null;
        }

        private Document getDocument() {
            return document;
        }

        private Exception getException() {
            return exception;
        }

        private static ParseResult of(Document document) {
            return new ParseResult(document, null);
        }

        private static ParseResult ofError(Exception e) {
            return new ParseResult(null, e);
        }
    }
}
