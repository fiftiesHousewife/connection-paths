package io.github.fiftieshousewife.connectionpaths.address;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.TextBlockLiteralExpr;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Follows a connection call's address back through the source: its first argument where that is an address (or, if
 * not, what the call is made on), then through variables, fields, object creations and the JDK's own methods, as far
 * as a literal, a setting key passed to a method, an environment variable, a system property, a JNDI name or a local
 * resource.
 */
public final class AddressTracer {

    private record Handler(Predicate<Expression> handles, BiFunction<Expression, Trace, Address> address) {
    }

    private final NameTracer names = new NameTracer(this);
    private final CallTracer calls = new CallTracer(this, names);
    private final List<Handler> handlers = List.of(
        new Handler(StringLiteralExpr.class::isInstance,
            (e, t) -> Address.of(Address.LITERAL, ((StringLiteralExpr) e).asString())),
        new Handler(TextBlockLiteralExpr.class::isInstance,
            (e, t) -> Address.of(Address.LITERAL, ((TextBlockLiteralExpr) e).asString().strip())),
        new Handler(EnclosedExpr.class::isInstance, (e, t) -> expression(((EnclosedExpr) e).getInner(), t)),
        new Handler(CastExpr.class::isInstance, (e, t) -> expression(((CastExpr) e).getExpression(), t)),
        new Handler(AddressTracer::isConcatenation, (e, t) -> joined((BinaryExpr) e, t)),
        new Handler(ConditionalExpr.class::isInstance, (e, t) -> eitherOf((ConditionalExpr) e, t)),
        new Handler(NameExpr.class::isInstance, (e, t) -> names.name((NameExpr) e, t)),
        new Handler(FieldAccessExpr.class::isInstance, (e, t) -> names.field((FieldAccessExpr) e, t)),
        new Handler(MethodCallExpr.class::isInstance, (e, t) -> calls.call((MethodCallExpr) e, t)),
        new Handler(ObjectCreationExpr.class::isInstance, (e, t) -> calls.creation((ObjectCreationExpr) e, t)),
        new Handler(ClassExpr.class::isInstance, (e, t) -> Address.of(Address.CLASSPATH_RESOURCE,
            "the jar or folder holding " + ((ClassExpr) e).getType())));

    /**
     * The address of a connection call: a method call, an object creation or a constructor's super(...). For a
     * process, it is the command.
     */
    public Address trace(final Node call, final String kind) {
        if (mentionsDns(call)) {
            return Address.of(Address.DNS, "the machine's DNS servers");
        }
        final Optional<Address> fromArgument = AddressText.firstArgument(call).map(a -> expression(a, new Trace()));
        final Optional<Address> fromScope = AddressText.scopeOf(call).map(s -> expression(s, new Trace()));
        final boolean command = "process".equals(kind);
        return fromArgument.filter(a -> command || AddressText.isAnAddress(a))
            .or(() -> fromScope)
            .orElse(Address.builtBy(AddressText.shorten(call)));
    }

    /** The address an expression gives, such as an argument passed to a method whose parameter is the address. */
    public Address expression(final Expression expression) {
        return expression(expression, new Trace());
    }

    Address expression(final Expression expression, final Trace trace) {
        if (trace.tooLong()) {
            return Address.builtBy(AddressText.shorten(expression));
        }
        return handlers.stream().filter(h -> h.handles().test(expression)).findFirst()
            .map(h -> h.address().apply(expression, trace))
            .orElse(Address.builtBy(AddressText.shorten(expression)));
    }

    private static boolean isConcatenation(final Expression expression) {
        return expression instanceof BinaryExpr binary && binary.getOperator() == BinaryExpr.Operator.PLUS;
    }

    // A string built from parts: a named part says most; otherwise the literal parts, with {?} for the others.
    private Address joined(final BinaryExpr binary, final Trace trace) {
        final Address left = expression(binary.getLeft(), trace);
        final Address right = expression(binary.getRight(), trace);
        if (left.isNamed()) {
            return left;
        }
        if (right.isNamed()) {
            return right;
        }
        return left.isLiteral() || right.isLiteral()
            ? Address.of(Address.LITERAL, part(left) + part(right)) : best(Stream.of(left, right));
    }

    private Address eitherOf(final ConditionalExpr conditional, final Trace trace) {
        return best(Stream.of(expression(conditional.getThenExpr(), trace),
            expression(conditional.getElseExpr(), trace)));
    }

    private static String part(final Address address) {
        return address.isLiteral() ? address.getValue() : Address.UNKNOWN_PART;
    }

    static Address best(final Stream<Address> candidates) {
        return candidates.min(Address.MOST_INFORMATIVE).orElse(Address.builtBy("?"));
    }

    // A class that looks hosts up through JNDI's DNS provider reaches the machine's DNS servers.
    private static boolean mentionsDns(final Node call) {
        return Declarations.enclosingType(call).stream()
            .flatMap(type -> type.findAll(StringLiteralExpr.class).stream())
            .map(StringLiteralExpr::asString)
            .anyMatch(text -> text.startsWith("dns:") || text.contains("jndi.dns.DnsContextFactory"));
    }
}
