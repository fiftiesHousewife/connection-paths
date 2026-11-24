package io.github.fiftieshousewife.connectionpaths;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.ReturnStmt;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Follows a variable, parameter or field to the values it is given: its initialiser, assignments and setters. */
final class NameTracer {

    private static final Set<String> ADDRESS_SETTERS = Set.of("setUrl", "setURL", "setJdbcUrl", "setHost", "setUri",
        "setURI", "setServerName", "setServerUrl", "setProviderUrl", "setConnectionString");
    /** Spring's {@code ${key}} or {@code ${key:default}}. */
    private static final Set<String> PREFIX_MEMBERS = Set.of("prefix", "value");
    static final Pattern PLACEHOLDER = Pattern.compile("^\\$\\{([^}:]+)(:.*)?}$");

    private final AddressTracer expressions;

    NameTracer(final AddressTracer expressions) {
        this.expressions = expressions;
    }

    Address name(final NameExpr name, final AddressTracer.Trace trace) {
        final String id = name.getNameAsString();
        final Optional<CallableDeclaration<?>> method = enclosingMethod(name);
        final Optional<Parameter> parameter = method.flatMap(m -> m.getParameterByName(id));
        if (parameter.isPresent()) {
            return Address.of(Address.PARAMETER, id + " of " + method.get().getNameAsString());
        }
        final List<Expression> local = method.map(m -> valuesIn(m, id)).orElse(List.of());
        if (!local.isEmpty()) {
            return follow(local, trace);
        }
        return fieldAround(name, id, trace);
    }

    /** A field of the class the name is in, or of a class around it. */
    private Address fieldAround(final Node node, final String id, final AddressTracer.Trace trace) {
        for (Optional<TypeDeclaration<?>> type = enclosingType(node); type.isPresent();
             type = enclosingType(type.get())) {
            if (type.get().getFieldByName(id).isPresent()) {
                return fieldIn(type.get(), id, trace);
            }
        }
        return enclosingType(node).map(type -> fieldIn(type, id, trace)).orElse(Address.builtBy(id));
    }

    /**
     * The text of an expression made only of string literals and constant fields, such as a setting key passed to a
     * method: read without tracing calls, so it costs nothing to try.
     */
    static Optional<String> constant(final Expression expression) {
        if (expression instanceof StringLiteralExpr literal) {
            return Optional.of(literal.asString());
        }
        if (expression instanceof BinaryExpr binary && binary.getOperator() == BinaryExpr.Operator.PLUS) {
            return constant(binary.getLeft()).flatMap(left -> constant(binary.getRight()).map(right -> left + right));
        }
        final String id = expression instanceof NameExpr name ? name.getNameAsString()
            : expression instanceof FieldAccessExpr field ? field.getNameAsString() : null;
        if (id == null) {
            return Optional.empty();
        }
        final Optional<Node> declared = expression instanceof FieldAccessExpr field ? declarationOf(field)
            : Optional.empty();
        return declared.or(() -> fieldDeclaredAround(expression, id))
            .flatMap(node -> node.findFirst(VariableDeclarator.class, v -> v.getNameAsString().equals(id)))
            .flatMap(VariableDeclarator::getInitializer)
            .filter(StringLiteralExpr.class::isInstance)
            .map(e -> ((StringLiteralExpr) e).asString());
    }

    private static Optional<Node> fieldDeclaredAround(final Node node, final String id) {
        for (Optional<TypeDeclaration<?>> type = enclosingType(node); type.isPresent();
             type = enclosingType(type.get())) {
            final Optional<Node> field = type.get().getFieldByName(id).map(Node.class::cast);
            if (field.isPresent()) {
                return field;
            }
        }
        return Optional.empty();
    }

    /** What a method declared in the source returns, if the call can be resolved to it and it says something. */
    Optional<Address> returnedBy(final MethodCallExpr call, final AddressTracer.Trace trace) {
        final List<Expression> returned = declarationOf(call)
            .map(method -> method.findAll(ReturnStmt.class).stream()
                .map(ReturnStmt::getExpression).flatMap(Optional::stream).toList())
            .orElse(List.of());
        if (returned.isEmpty()) {
            return Optional.empty();
        }
        final Address found = follow(returned, trace);
        return Address.BUILT_AT_RUN_TIME.equals(found.getSource()) ? Optional.empty() : Optional.of(found);
    }

    // The symbol solver reports anything it can't follow as an exception, of many kinds, and sometimes overflows.
    @SuppressWarnings({"PMD.AvoidCatchingGenericException", "PMD.AvoidCatchingThrowable"})
    private static Optional<Node> declarationOf(final MethodCallExpr call) {
        try {
            return call.resolve().toAst();
        } catch (RuntimeException | StackOverflowError e) {
            return Optional.empty();
        }
    }

    Address field(final FieldAccessExpr access, final AddressTracer.Trace trace) {
        final String id = access.getNameAsString();
        final Optional<TypeDeclaration<?>> owner = declarationOf(access)
            .flatMap(NameTracer::enclosingType)
            .or(() -> enclosingType(access));
        return owner.map(type -> fieldIn(type, id, trace)).orElse(Address.builtBy(access.toString()));
    }

    private Address fieldIn(final TypeDeclaration<?> type, final String id, final AddressTracer.Trace trace) {
        final Optional<Address> annotated = type.getFields().stream()
            .filter(f -> f.getVariables().stream().anyMatch(v -> v.getNameAsString().equals(id)))
            .flatMap(f -> f.getAnnotations().stream())
            .map(NameTracer::settingFromValueAnnotation)
            .flatMap(Optional::stream)
            .findFirst()
            .or(() -> boundSetting(type, id));
        if (annotated.isPresent()) {
            return annotated.get();
        }
        final List<Expression> values = valuesIn(type, id);
        return values.isEmpty() ? Address.builtBy(id) : follow(values, trace);
    }

    private Address follow(final List<Expression> values, final AddressTracer.Trace trace) {
        final List<Address> found = new ArrayList<>();
        for (final Expression value : values) {
            if (trace.firstVisit(value)) {
                found.add(expressions.expression(value, trace));
            }
        }
        return AddressTracer.best(found.stream());
    }

    /** Every value a name is given inside a node: initialisers, assignments, and address setters called on it. */
    private static List<Expression> valuesIn(final Node scope, final String id) {
        final List<Expression> values = new ArrayList<>();
        scope.findAll(VariableDeclarator.class, v -> v.getNameAsString().equals(id))
            .forEach(v -> v.getInitializer().ifPresent(values::add));
        // A compound assignment such as url += "/" adds to a value the name already has; it isn't one of its own.
        scope.findAll(AssignExpr.class, a -> a.getOperator() == AssignExpr.Operator.ASSIGN
                && id.equals(targetName(a.getTarget())))
            .forEach(a -> values.add(a.getValue()));
        scope.findAll(MethodCallExpr.class, c -> ADDRESS_SETTERS.contains(c.getNameAsString())
                && c.getScope().map(s -> id.equals(targetName(s))).orElse(false) && !c.getArguments().isEmpty())
            .forEach(c -> values.add(c.getArgument(0)));
        return values;
    }

    private static String targetName(final Expression target) {
        if (target instanceof NameExpr name) {
            return name.getNameAsString();
        }
        return target instanceof FieldAccessExpr field ? field.getNameAsString() : "";
    }

    /**
     * A field of a class annotated {@code @ConfigurationProperties("orders-db")}, which Spring fills from the settings:
     * the setting {@code orders-db.<field>}.
     */
    private static Optional<Address> boundSetting(final TypeDeclaration<?> type, final String id) {
        if (type.getFieldByName(id).isEmpty()) {
            return Optional.empty();
        }
        return type.getAnnotationByName("ConfigurationProperties").flatMap(NameTracer::prefixOf)
            .map(prefix -> Address.builder().source(Address.SETTING).value(prefix + "." + id)
                .via("@ConfigurationProperties").build());
    }

    private static Optional<String> prefixOf(final AnnotationExpr annotation) {
        if (annotation instanceof SingleMemberAnnotationExpr single) {
            return constant(single.getMemberValue());
        }
        return annotation instanceof NormalAnnotationExpr normal ? normal.getPairs().stream()
            .filter(pair -> PREFIX_MEMBERS.contains(pair.getNameAsString())).findFirst()
            .flatMap(pair -> constant(pair.getValue())) : Optional.empty();
    }

    /** Spring's {@code @Value("${orders.db.url}")} on a field: the setting it reads. */
    private static Optional<Address> settingFromValueAnnotation(final AnnotationExpr annotation) {
        if (!"Value".equals(annotation.getName().getIdentifier())
            || !(annotation instanceof SingleMemberAnnotationExpr single)
            || !(single.getMemberValue() instanceof StringLiteralExpr text)) {
            return Optional.empty();
        }
        final Matcher matcher = PLACEHOLDER.matcher(text.asString());
        return matcher.matches()
            ? Optional.of(Address.builder().source(Address.SETTING).value(matcher.group(1)).via("@Value").build())
            : Optional.of(Address.of(Address.LITERAL, text.asString()));
    }

    private static Optional<CallableDeclaration<?>> enclosingMethod(final Node node) {
        for (Optional<Node> at = node.getParentNode(); at.isPresent(); at = at.get().getParentNode()) {
            if (at.get() instanceof CallableDeclaration<?> method) {
                return Optional.of(method);
            }
        }
        return Optional.empty();
    }

    static Optional<TypeDeclaration<?>> enclosingType(final Node node) {
        for (Optional<Node> at = node.getParentNode(); at.isPresent(); at = at.get().getParentNode()) {
            if (at.get() instanceof TypeDeclaration<?> type) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

    // The symbol solver reports anything it can't follow as an exception, of many kinds, and sometimes overflows.
    @SuppressWarnings({"PMD.AvoidCatchingGenericException", "PMD.AvoidCatchingThrowable"})
    private static Optional<Node> declarationOf(final FieldAccessExpr access) {
        try {
            return access.resolve().toAst();
        } catch (RuntimeException | StackOverflowError e) {
            return Optional.empty();
        }
    }

    /** Whether a call is to a method the JDK declares, such as {@code String.format} or {@code URI.create}. */
    @SuppressWarnings({"PMD.AvoidCatchingGenericException", "PMD.AvoidCatchingThrowable"})
    static boolean declaredInJdk(final MethodCallExpr call) {
        try {
            final String type = call.resolve().declaringType().getQualifiedName();
            return type.startsWith("java.") || type.startsWith("javax.");
        } catch (RuntimeException | StackOverflowError e) {
            return false;
        }
    }
}
