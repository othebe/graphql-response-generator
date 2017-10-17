package hydrator.implementation;

import graphql.schema.GraphQLEnumValueDefinition;
import hydrator.ICustomScalarHydrator;
import hydrator.IEnumHydrator;
import hydrator.IScalarHydrator;
import hydrator.IValueHydratorFactory;
import org.fluttercode.datafactory.impl.DataFactory;

import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * Implementation of {@link IValueHydratorFactory} that returns random data.
 */
public class RandomValueHydratorFactory implements IValueHydratorFactory {
    private final IScalarHydrator scalarHydrator;
    private final IEnumHydrator enumHydrator;
    private final ICustomScalarHydrator customScalarHydrator;

    public RandomValueHydratorFactory(ICustomScalarHydrator customScalarHydrator) {
        this.scalarHydrator = new RandomScalarHydrator();
        this.enumHydrator = new RandomEnumHydrator();
        this.customScalarHydrator = customScalarHydrator;
    }

    @Override
    public IScalarHydrator provideScalarHydrator() {
        return scalarHydrator;
    }

    @Override
    public IEnumHydrator provideEnumHydrator() {
        return enumHydrator;
    }

    @Override
    public ICustomScalarHydrator provideCustomScalarHydrator() {
        return customScalarHydrator;
    }

    /*******************************************************************************************************************
     * RANDOM VALUE HYDRATORS
     ******************************************************************************************************************/

    private static class RandomScalarHydrator implements IScalarHydrator {
        private static final int STRING_LENGTH = 24;

        private final DataFactory dataFactory;

        private RandomScalarHydrator() {
            this.dataFactory = new DataFactory();
        }

        @Override
        public String hydrateString() {
            return dataFactory.getRandomText(STRING_LENGTH);
        }

        @Override
        public String hydrateId() {
            return String.valueOf(dataFactory.getNumberBetween(0, Integer.MAX_VALUE));
        }
    }


    private static class RandomEnumHydrator implements IEnumHydrator {
        @Override
        public String hydrateEnum(List<GraphQLEnumValueDefinition> enumDefinitions) {
            int numKeys = enumDefinitions.size();
            int index = new Random((new Date()).getTime()).nextInt(numKeys);

            return enumDefinitions.get(index).getValue().toString();
        }
    }
}
