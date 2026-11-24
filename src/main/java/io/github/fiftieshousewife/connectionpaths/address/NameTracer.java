package io.github.fiftieshousewife.connectionpaths.address;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.ReturnStmt;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

// Follows a variable, parameter or field to the values it is given: its initialiser, assignments and setters.
final class NameTracer {

    private static final Set<String> ADDRESS_SETTERS = Set.of("setUrl", "setURL", "setJdbcUrl", "setHost", "setUri",
        "setURI", "setServerName", "setServerUrl", "setProviderUrl", "setConnectionString", "add", "url", "uri");
    private static final Set<String> TABLE_PUTS = Set.of("put", "setProperty", "addToEnvironment");

    private final AddressTracer expressions;

    NameTracer(final AddressTracer expressions) {
        this.expressions = expressions;
    }

    Address name(final NameExpr name, final Trace trace) {
        final String id = name.getNameAsString();
        final Optional<CallableDeclaration<?>> method = Declarations.enclosingMethod(name);
        if (method.flatMap(m -> m.getParameterByName(id)).isPresent()) {
            return Address.of(Address.PARAMETER, id + " of " + method.get().getNameAsString());
        }
        final List<Expression> local = method.map(m -> valuesIn(m, id)).orElse(List.of());
        return local.isEmpty() ? fieldAround(name, id, trace) : follow(local, trace);
    }

    // A field of the class the name is in, or of a class around it.
    private Address fieldAround(final Node node, final String id, final Trace trace) {
        return Declarations.enclosingTypes(node).filter(type -> type.getFieldByName(id).isPresent()).findFirst()
            .or(() -> Declarations.enclosingType(node))
            .map(type -> fieldIn(type, id, trace)).orElse(Address.builtBy(id));
    }

    // What a method declared in the source returns, if the call can be resolved to it and it says something.
    Optional<Address> returnedBy(final MethodCallExpr call, final Trace trace) {
        final List<Expression> returned = Declarations.of(call)
            .map(method -> method.findAll(ReturnStmt.class).stream()
                .map(ReturnStmt::getExpression).flatMap(Optional::stream).toList())
            .orElse(List.of());
        return returned.isEmpty() ? Optional.empty()
            : Optional.of(follow(returned, trace)).filter(AddressText::saysSomething)
                .or(() -> Declarations.of(call).flatMap(method -> fromFieldsRead(method, trace)));
    }

    // A value built in ways that can't be followed, such as a URL appended host by host, still comes from the fields
    // its method reads: the setting one of them is filled from names it.
    private Optional<Address> fromFieldsRead(final Node method, final Trace trace) {
        return Declarations.enclosingType(method).flatMap(type -> method.findAll(NameExpr.class).stream()
            .map(NameExpr::getNameAsString).distinct()
            .filter(id -> type.getFieldByName(id).isPresent() && valuesIn(method, id).isEmpty())
            .map(id -> fieldIn(type, id, trace)).filter(Address::isNamed).findFirst());
    }

    // One element of an array written out where it is made, such as urls[i]: any of them.
    Address element(final ArrayAccessExpr access, final Trace trace) {
        final Node scope = Declarations.enclosingMethod(access).<Node>map(Node.class::cast).orElse(access);
        final List<Expression> elements = Iterated.elementsOf(scope, access.getName()).toList();
        return elements.isEmpty() ? Address.builtBy(AddressText.shorten(access)) : follow(elements, trace);
    }

    Address field(final FieldAccessExpr access, final Trace trace) {
        final Optional<TypeDeclaration<?>> owner = Declarations.of(access)
            .flatMap(Declarations::enclosingType)
            .or(() -> Declarations.enclosingType(access));
        return owner.map(type -> fieldIn(type, access.getNameAsString(), trace))
            .orElse(Address.builtBy(access.toString()));
    }

    private Address fieldIn(final TypeDeclaration<?> type, final String id, final Trace trace) {
        return SettingAnnotations.ofField(type, id).orElseGet(() -> {
            final List<Expression> values = valuesIn(type, id);
            return values.isEmpty() ? Address.builtBy(id) : follow(values, trace);
        });
    }

    private Address follow(final List<Expression> values, final Trace trace) {
        return AddressTracer.best(values.stream().filter(trace::firstVisit)
            .map(value -> expressions.expression(value, trace)).toList().stream());
    }

    // Every value a name is given inside a node: initialisers, assignments, and address setters called on it. A
    // compound assignment such as url += "/" adds to a value the name already has; it isn't one of its own.
    private static List<Expression> valuesIn(final Node scope, final String id) {
        final Stream<Expression> initialised = scope
            .findAll(VariableDeclarator.class, v -> v.getNameAsString().equals(id)).stream()
            .map(VariableDeclarator::getInitializer).flatMap(Optional::stream);
        final Stream<Expression> assigned = scope
            .findAll(AssignExpr.class, a -> a.getOperator() == AssignExpr.Operator.ASSIGN
                && id.equals(targetName(a.getTarget()))).stream()
            .map(AssignExpr::getValue);
        final Stream<Expression> set = scope.findAll(MethodCallExpr.class, c -> isAddressSetterOn(c, id)).stream()
            .map(c -> c.getArgument(0));
        final Stream<Expression> put = scope.findAll(MethodCallExpr.class, c -> isProviderUrlPutOn(c, id)).stream()
            .map(c -> c.getArgument(1));
        return Stream.of(initialised, assigned, set, put, Iterated.valuesOf(scope, id)).flatMap(s -> s).toList();
    }

    private static boolean isAddressSetterOn(final MethodCallExpr call, final String id) {
        return ADDRESS_SETTERS.contains(call.getNameAsString()) && !call.getArguments().isEmpty()
            && call.getScope().map(s -> id.equals(targetName(s))).orElse(false);
    }

    // A JNDI or JMS environment table given its provider URL: env.put(Context.PROVIDER_URL, url).
    private static boolean isProviderUrlPutOn(final MethodCallExpr call, final String id) {
        final boolean onTheTable = call.getScope().map(s -> id.equals(targetName(s))).orElse(false);
        return onTheTable && TABLE_PUTS.contains(call.getNameAsString()) && call.getArguments().size() == 2
            && isProviderUrlKey(call.getArgument(0));
    }

    private static boolean isProviderUrlKey(final Expression key) {
        return key.toString().endsWith("PROVIDER_URL")
            || Declarations.constant(key).filter("java.naming.provider.url"::equals).isPresent();
    }

    private static String targetName(final Expression target) {
        if (target instanceof NameExpr name) {
            return name.getNameAsString();
        }
        return target instanceof FieldAccessExpr field ? field.getNameAsString() : "";
    }
}
