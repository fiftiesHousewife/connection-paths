package io.github.fiftieshousewife.connectionpaths;

/**
 * A method or constructor: the type that declares it, its name, and its parameter types as simple names without
 * generics ({@code URL,String}). Constructors are named {@code <init>}; a type's field initialisers and initialiser
 * blocks are {@code <fields>}.
 */
public record MethodKey(String type, String name, String parameters) implements Comparable<MethodKey> {

    static final String CONSTRUCTOR = "<init>";
    static final String FIELDS = "<fields>";

    static MethodKey fieldsOf(final String type) {
        return new MethodKey(type, FIELDS, "");
    }

    MethodKey inType(final String otherType) {
        return new MethodKey(otherType, name, parameters);
    }

    boolean isConstructor() {
        return CONSTRUCTOR.equals(name);
    }

    int parameterCount() {
        return parameters.isEmpty() ? 0 : parameters.split(",", -1).length;
    }

    /**
     * The key that overriding matches on: type, name and number of parameters. Parameter types aren't compared,
     * because an override of a generic method ({@code save(T)}) names a different type ({@code save(Order)}).
     */
    String overrideKey(final String inType) {
        return inType + "#" + name + "/" + parameterCount();
    }

    /** The class's simple name and the method's, as people read a stack trace: {@code OrderStore.save}. */
    String shortName() {
        return type.substring(type.lastIndexOf('.') + 1) + "." + name;
    }

    @Override
    public String toString() {
        return type + "#" + name + "(" + parameters + ")";
    }

    @Override
    public int compareTo(final MethodKey other) {
        return toString().compareTo(other.toString());
    }
}
