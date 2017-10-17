package hydrator;

public interface IValueHydratorFactory {
    IScalarHydrator provideScalarHydrator();
    IEnumHydrator provideEnumHydrator();
    ICustomScalarHydrator provideCustomScalarHydrator();
}
