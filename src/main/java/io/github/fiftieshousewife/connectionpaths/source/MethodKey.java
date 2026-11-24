package io.github.fiftieshousewife.connectionpaths.source;

/**
 * A method or constructor: the type that declares it, its name, and its parameter types as simple names without
 * generics ({@code URL,String}). Constructors are named {@code <init>}; a type's instance field initialisers and
 * initialiser blocks are {@code <fields>}, and its static ones, with its enum constants, are {@code <statics>}.
 */
public record MethodKey(String type, String name, String parameters) implements Comparable<MethodKey> {

    public static final String CONSTRUCTOR = "<init>";
    static final String FIELDS = "<fields>";
    static final String STATICS = "<statics>";

    public static MethodKey fieldsOf(final String type) {
        return new MethodKey(type, FIELDS, "");
    }

    public static MethodKey staticsOf(final String type) {
        return new MethodKey(type, STATICS, "");
    }

    /** Whether this stands for a type's initialisers rather than a method the source declares by name. */
    public boolean isInitialisers() {
        return FIELDS.equals(name) || STATICS.equals(name);
    }

    public MethodKey inType(final String otherType) {
        return new MethodKey(otherType, name, parameters);
    }

    public boolean isConstructor() {
        return CONSTRUCTOR.equals(name);
    }

    int parameterCount() {
        return parameters.isEmpty() ? 0 : parameters.split(",", -1).length;
    }

    /**
     * The key that overriding is first matched on: type, name and number of parameters. The parameter types are then
     * compared by {@link Signatures#couldBeTheSame}, because an override of a generic method ({@code save(T)}) names
     * a different type ({@code save(Order)}).
     */
    public String overrideKey(final String inType) {
        return inType + "#" + name + "/" + parameterCount();
    }

    // The class's simple name and the method's, as people read a stack trace: OrderStore.save.
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
