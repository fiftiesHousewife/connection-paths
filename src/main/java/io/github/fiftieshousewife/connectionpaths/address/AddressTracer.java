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
import com.github.javaparser.ast.nodeTypes.NodeWithArguments;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Follows a connection call's address back through the source: its first argument where that is an address (or, if
 * not, what the call is made on), then through variables, fields, object creations and the JDK's own methods, as far
 * as a literal, a setting key passed to a method, an environment variable, a system property, a JNDI name or a local
 * resource.
 */
public final class AddressTracer {

    private static final int MAX_STEPS = 40;
    private static final int SHORT = 60;
    /** Starts like a URL or URI: a scheme and a colon. */
    private static final Pattern SCHEME = Pattern.compile("[a-zA-Z][a-zA-Z0-9+.-]*:.*");
    /** The characters of a host name or address, or of a setting's key. */
    private static final Pattern NAME_CHARACTERS = Pattern.compile("[\\w.-]+");
    /** The endings that make a dotted name a host rather than a setting, such as {@code mail.example.com}. */
    private static final Set<String> HOST_ENDINGS = Set.of("com", "org", "net", "io", "internal", "local");
    private static final Set<String> RESOURCE_METHODS =
        Set.of("getResource", "getResources", "getResourceAsStream", "getSystemResource", "getSystemResources",
            "getSystemResourceAsStream");
    private static final Set<String> LOCAL_FILE_TYPES =
        Set.of("File", "FileInputStream", "FileOutputStream", "FileReader", "RandomAccessFile");
    /** Factory methods whose argument names an algorithm, a class or a logger, never an address. */
    private static final Set<String> NOT_ADDRESSES =
        Set.of("getInstance", "forName", "getLogger", "getBundle", "valueOf", "getDefault");
    private static final Set<String> JDK_HELPERS = Set.of("String", "URI", "URL", "Paths", "Path", "Objects");

    /** One trace: how many steps it has taken, and the values it has already followed. */
    static final class Trace {
        private int steps;
        private final Set<Node> followed = new HashSet<>();

        boolean tooLong() {
            return ++steps > MAX_STEPS;
        }

        boolean firstVisit(final Node value) {
            return followed.add(value);
        }
    }

    private final NameTracer names = new NameTracer(this);

    /**
     * The address of a connection call: a method call, an object creation or a constructor's super(...). For a
     * process, it is the command.
     */
    public Address trace(final Node call, final String kind) {
        if (mentionsDns(call)) {
            return Address.of(Address.DNS, "the machine's DNS servers");
        }
        final Optional<Address> fromArgument = firstArgument(call).map(a -> expression(a, new Trace()));
        final Optional<Address> fromScope = scopeOf(call).map(s -> expression(s, new Trace()));
        final boolean command = "process".equals(kind);
        return fromArgument.filter(a -> command || isAnAddress(a))
            .or(() -> fromScope)
            .orElse(Address.builtBy(shorten(call)));
    }

    private static boolean saysSomething(final Address address) {
        return !Address.BUILT_AT_RUN_TIME.equals(address.getSource());
    }

    /** A literal counts only if it looks like an address, so a message or a query isn't taken for one. */
    private static boolean isAnAddress(final Address address) {
        if (!address.isLiteral()) {
            return true;
        }
        final String text = address.getValue().replace(Address.UNKNOWN_PART, "x");
        return SCHEME.matcher(text).matches() || isHost(hostPart(text));
    }

    /** The text before any port or path. */
    private static String hostPart(final String text) {
        final int end = text.indexOf(':') < 0 ? text.indexOf('/') : text.indexOf(':');
        return end < 0 ? text : text.substring(0, end);
    }

    /** {@code localhost}, or a dotted name such as {@code orders-db.internal} or {@code 10.0.0.7}. */
    private static boolean isHost(final String name) {
        return "localhost".equals(name) || isDottedName(name);
    }

    /** Letters, digits, dashes and underscores, in two or more parts joined by single dots. */
    private static boolean isDottedName(final String name) {
        return NAME_CHARACTERS.matcher(name).matches() && name.indexOf('.') > 0 && !name.endsWith(".")
            && !name.contains("..");
    }

    /** A dotted name that isn't a host, such as {@code database.serverURL}: a setting's key. */
    private static boolean isSettingKey(final String text) {
        return isDottedName(text) && Character.isLetter(text.charAt(0))
            && !HOST_ENDINGS.contains(text.substring(text.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT));
    }

    Address expression(final Expression expression, final Trace trace) {
        if (trace.tooLong()) {
            return Address.builtBy(shorten(expression));
        }
        if (expression instanceof StringLiteralExpr literal) {
            return Address.of(Address.LITERAL, literal.asString());
        }
        if (expression instanceof TextBlockLiteralExpr block) {
            return Address.of(Address.LITERAL, block.asString().strip());
        }
        if (expression instanceof EnclosedExpr enclosed) {
            return expression(enclosed.getInner(), trace);
        }
        if (expression instanceof CastExpr cast) {
            return expression(cast.getExpression(), trace);
        }
        return compound(expression, trace);
    }

    private Address compound(final Expression expression, final Trace trace) {
        if (expression instanceof BinaryExpr binary && binary.getOperator() == BinaryExpr.Operator.PLUS) {
            return joined(expression(binary.getLeft(), trace), expression(binary.getRight(), trace));
        }
        if (expression instanceof ConditionalExpr conditional) {
            return best(Stream.of(expression(conditional.getThenExpr(), trace),
                expression(conditional.getElseExpr(), trace)));
        }
        if (expression instanceof NameExpr name) {
            return names.name(name, trace);
        }
        if (expression instanceof FieldAccessExpr field) {
            return names.field(field, trace);
        }
        if (expression instanceof MethodCallExpr call) {
            return call(call, trace);
        }
        if (expression instanceof ObjectCreationExpr creation) {
            return creation(creation, trace);
        }
        if (expression instanceof ClassExpr type) {
            return Address.of(Address.CLASSPATH_RESOURCE, "the jar or folder holding " + type.getType());
        }
        return Address.builtBy(shorten(expression));
    }

    /** A string built from parts: a named part says most; otherwise the literal parts, with {?} for the others. */
    private static Address joined(final Address left, final Address right) {
        if (left.isNamed() || right.isNamed()) {
            return left.isNamed() ? left : right;
        }
        if (!left.isLiteral() && !right.isLiteral()) {
            return best(Stream.of(left, right));
        }
        return Address.of(Address.LITERAL, part(left) + part(right));
    }

    private static String part(final Address address) {
        return address.isLiteral() ? address.getValue() : Address.UNKNOWN_PART;
    }

    static Address best(final Stream<Address> candidates) {
        return candidates.min(Address.MOST_INFORMATIVE).orElse(Address.builtBy("?"));
    }

    /**
     * A call: one that reads an address by name; else one into the source, followed into what it returns; else,
     * as for a builder's chain or the JDK's own helpers, what it is called on, or failing that its first argument.
     * A builder's address is in its first call, so the later calls' arguments, such as timeouts, are passed over.
     */
    private Address call(final MethodCallExpr call, final Trace trace) {
        final String name = call.getNameAsString();
        if (NOT_ADDRESSES.contains(name)) {
            return Address.builtBy(shorten(call));
        }
        final String scope = call.getScope().map(Node::toString).orElse("");
        final Optional<Address> known =
            firstArgument(call).flatMap(NameTracer::constant).flatMap(text -> lookedUp(scope, name, text));
        if (known.isPresent()) {
            return known.get();
        }
        final boolean jdk = JDK_HELPERS.contains(scope) || NameTracer.declaredInJdk(call);
        final Optional<Address> returned = jdk ? Optional.empty() : names.returnedBy(call, trace);
        if (returned.isPresent()) {
            return returned.get();
        }
        return call.getScope().map(s -> expression(s, trace)).filter(AddressTracer::saysSomething)
            .or(() -> firstArgument(call).map(a -> expression(a, trace))
                .filter(AddressTracer::isAnAddress).filter(AddressTracer::saysSomething))
            .orElse(Address.builtBy(shorten(call)));
    }

    /** A call that reads an address by name: an environment variable, a system property, a resource or a setting. */
    private static Optional<Address> lookedUp(final String scope, final String name, final String text) {
        if ("System".equals(scope)) {
            return fromSystem(name, text);
        }
        if (RESOURCE_METHODS.contains(name)) {
            return Optional.of(Address.of(Address.CLASSPATH_RESOURCE, text));
        }
        if ("lookup".equals(name)) {
            return Optional.of(Address.of(Address.JNDI_NAME, text));
        }
        if (isSettingKey(text)) {
            return Optional.of(Address.builder().source(Address.SETTING).value(text)
                .via(scope.isEmpty() ? name : scope + "." + name).build());
        }
        return Optional.empty();
    }

    private static Optional<Address> fromSystem(final String name, final String text) {
        if ("getenv".equals(name)) {
            return Optional.of(Address.of(Address.ENVIRONMENT_VARIABLE, text));
        }
        return "getProperty".equals(name) ? Optional.of(Address.of(Address.SYSTEM_PROPERTY, text)) : Optional.empty();
    }

    private Address creation(final ObjectCreationExpr creation, final Trace trace) {
        final String type = creation.getType().getNameAsString();
        if (LOCAL_FILE_TYPES.contains(type)) {
            return Address.of(Address.LOCAL_FILE, firstArgument(creation).map(AddressTracer::shorten).orElse(type));
        }
        return firstArgument(creation).map(a -> expression(a, trace)).orElse(Address.builtBy("new " + type));
    }

    private static Optional<Expression> firstArgument(final Node call) {
        if (call instanceof NodeWithArguments<?> withArguments && !withArguments.getArguments().isEmpty()) {
            final List<Expression> arguments = withArguments.getArguments();
            return Optional.of(arguments.get(0));
        }
        return Optional.empty();
    }

    private static Optional<Expression> scopeOf(final Node call) {
        return call instanceof MethodCallExpr method ? method.getScope() : Optional.empty();
    }

    /** A class that looks hosts up through JNDI's DNS provider reaches the machine's DNS servers. */
    private static boolean mentionsDns(final Node call) {
        return NameTracer.enclosingType(call).stream()
            .flatMap(type -> type.findAll(StringLiteralExpr.class).stream())
            .map(StringLiteralExpr::asString)
            .anyMatch(text -> text.startsWith("dns:") || text.contains("jndi.dns.DnsContextFactory"));
    }

    private static String shorten(final Node node) {
        final String text = node.toString().replaceAll("\\s+", " ");
        return text.length() > SHORT ? text.substring(0, SHORT) + "..." : text;
    }
}
