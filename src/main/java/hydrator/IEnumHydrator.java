package hydrator;

import graphql.schema.GraphQLEnumValueDefinition;

import java.util.List;

/**
 * Provides a strategy to hydrate enums.
 */
public interface IEnumHydrator {
    String hydrateEnum(List<GraphQLEnumValueDefinition> enumDefinitions);
}
