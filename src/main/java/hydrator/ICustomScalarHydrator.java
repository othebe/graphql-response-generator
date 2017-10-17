package hydrator;

public interface ICustomScalarHydrator {
    boolean canHydrate(String typeName);
    Object hydrate(String typeName);
}
