package io.github.fiftieshousewife.connectionpaths;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Parameter lists as simple type names without generics, the same whether read from a declaration in the source or
 * from a resolved call: {@code Map<String, List<Order>>} and {@code java.util.Map<java.lang.String, ...>} are both
 * {@code Map}, and a varargs {@code String...} is {@code String[]}.
 */
final class Signatures {

    private static final Pattern GENERICS = Pattern.compile("<[^<>]*>");
    private static final String UNKNOWN = "?";

    private Signatures() {
    }

    static String of(final List<Parameter> parameters) {
        final List<String> names = new ArrayList<>();
        for (final Parameter parameter : parameters) {
            names.add(simple(parameter.getType().asString()) + (parameter.isVarArgs() ? "[]" : ""));
        }
        return String.join(",", names);
    }

    /** From the declaration in the source where there is one, so both sides of a call read the same way. */
    @SuppressWarnings({"PMD.AvoidCatchingGenericException", "PMD.AvoidCatchingThrowable"})
    static String of(final ResolvedMethodLikeDeclaration resolved) {
        final Optional<Node> declaration = declarationOf(resolved);
        if (declaration.isPresent() && declaration.get() instanceof CallableDeclaration<?> callable) {
            return of(callable.getParameters());
        }
        final List<String> names = new ArrayList<>();
        for (int i = 0; i < resolved.getNumberOfParams(); i++) {
            try {
                names.add(simple(resolved.getParam(i).describeType()));
            } catch (RuntimeException | StackOverflowError e) {
                names.add(UNKNOWN);
            }
        }
        return String.join(",", names);
    }

    @SuppressWarnings({"PMD.AvoidCatchingGenericException", "PMD.AvoidCatchingThrowable"})
    private static Optional<Node> declarationOf(final ResolvedMethodLikeDeclaration resolved) {
        try {
            return resolved.toAst();
        } catch (RuntimeException | StackOverflowError e) {
            return Optional.empty();
        }
    }

    /** Drops leading annotations, as in {@code @NonNull String}; arguments to them hold no spaces in practice. */
    private static String withoutAnnotations(final String type) {
        String text = type.strip();
        while (text.startsWith("@") && text.indexOf(' ') > 0) {
            text = text.substring(text.indexOf(' ') + 1).strip();
        }
        return text;
    }

    static String simple(final String type) {
        String text = withoutAnnotations(type).replace("...", "[]").strip();
        String previous;
        do {
            previous = text;
            text = GENERICS.matcher(text).replaceAll("");
        } while (!text.equals(previous));
        final int dot = text.lastIndexOf('.');
        return (dot < 0 ? text : text.substring(dot + 1)).replace(" ", "");
    }
}
