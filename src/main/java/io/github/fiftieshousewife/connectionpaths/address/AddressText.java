package io.github.fiftieshousewife.connectionpaths.address;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithArguments;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

// What text looks like: an address (a URL, a host), or a setting's key; and the parts of a call an address is in.
final class AddressText {

    private static final int SHORT = 60;
    private static final Pattern SCHEME = Pattern.compile("[a-zA-Z][a-zA-Z0-9+.-]*:.*");
    private static final Pattern NAME_CHARACTERS = Pattern.compile("[\\w.-]+", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Set<String> HOST_ENDINGS = Set.of("com", "org", "net", "io", "internal", "local");

    private AddressText() {
    }

    static boolean saysSomething(final Address address) {
        return !Address.BUILT_AT_RUN_TIME.equals(address.getSource());
    }

    // A literal counts only if it looks like an address, so a message or a query isn't taken for one; a path such as
    // /owners/1 does, as a request relative to the server its client was given.
    static boolean isAnAddress(final Address address) {
        if (!address.isLiteral() || isRelativePath(address)) {
            return true;
        }
        final String text = address.getValue().replace(Address.UNKNOWN_PART, "x");
        return SCHEME.matcher(text).matches() || isHost(hostPart(text));
    }

    static boolean isRelativePath(final Address address) {
        return address.isRelativePath();
    }

    // The text before any port or path.
    private static String hostPart(final String text) {
        final int end = text.indexOf(':') < 0 ? text.indexOf('/') : text.indexOf(':');
        return end < 0 ? text : text.substring(0, end);
    }

    // localhost, or a dotted name such as orders-db.internal or 10.0.0.7.
    private static boolean isHost(final String name) {
        return "localhost".equals(name) || isDottedName(name);
    }

    // Letters, digits, dashes and underscores, in two or more parts joined by single dots.
    private static boolean isDottedName(final String name) {
        return NAME_CHARACTERS.matcher(name).matches() && name.indexOf('.') > 0 && !name.endsWith(".")
            && !name.contains("..");
    }

    // A dotted name that isn't a host, such as database.serverURL: a setting's key.
    static boolean isSettingKey(final String text) {
        return isDottedName(text) && Character.isLetter(text.charAt(0))
            && !HOST_ENDINGS.contains(text.substring(text.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT));
    }

    static Optional<Expression> firstArgument(final Node call) {
        if (call instanceof NodeWithArguments<?> withArguments && !withArguments.getArguments().isEmpty()) {
            return Optional.of(withArguments.getArguments().get(0));
        }
        return Optional.empty();
    }

    static Optional<Expression> scopeOf(final Node call) {
        return call instanceof MethodCallExpr method ? method.getScope() : Optional.empty();
    }

    static String shorten(final Node node) {
        final String text = node.toString().replaceAll("\\s+", " ");
        return text.length() > SHORT ? text.substring(0, SHORT) + "..." : text;
    }
}
