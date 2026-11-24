package io.github.fiftieshousewife.connectionpaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** The calls that open or use a connection, read from the built-in rules file and, optionally, the user's own. */
public final class ConnectionRules {

    static final String WILDCARD = "*";
    private static final String BUILT_IN = "/connection-rules.txt";

    /** One rule: calls to {@code type#method} (or to a subtype's) are a connection of this kind. */
    public record Rule(String kind, String type, String method) {

        boolean matchesMethod(final String name) {
            return WILDCARD.equals(method) ? !MethodKey.CONSTRUCTOR.equals(name) : method.equals(name);
        }

        /**
         * Through a supertype, a {@code *} rule matches only the methods that supertype declares, so a template's
         * own setters aren't taken for its queries; but every method, where it declares none (a marker such as
         * Spring Data's {@code Repository}) or what it declares isn't known.
         */
        boolean matchesThrough(final String name, final Set<String> declared) {
            final boolean anyDeclared = !WILDCARD.equals(method) || declared.isEmpty() || declared.contains(name);
            return anyDeclared && matchesMethod(name);
        }

        @Override
        public String toString() {
            return type + "#" + method;
        }
    }

    private final List<Rule> rules;

    private ConnectionRules(final List<Rule> rules) {
        this.rules = List.copyOf(rules);
    }

    /** The built-in rules, then the extra file's, if one is given. */
    public static ConnectionRules load(final Optional<Path> extra) {
        final List<Rule> all = new ArrayList<>(builtIn());
        extra.ifPresent(file -> all.addAll(parse(read(file), file.toString())));
        return new ConnectionRules(all);
    }

    static ConnectionRules of(final List<Rule> rules) {
        return new ConnectionRules(rules);
    }

    List<Rule> getRules() {
        return rules;
    }

    /**
     * The rule a call matches: first a rule naming the type that declares the method, then one naming any of that
     * type's supertypes, in the order the rules are listed. The supertypes come with the names of the methods each
     * declares, where they are known.
     */
    Optional<Rule> match(final MethodKey called, final Map<String, Set<String>> supertypes) {
        final Optional<Rule> exact = rules.stream()
            .filter(rule -> rule.type().equals(called.type()) && rule.matchesMethod(called.name()))
            .findFirst();
        if (exact.isPresent() || called.isConstructor()) {
            return exact;
        }
        return rules.stream()
            .filter(rule -> supertypes.containsKey(rule.type())
                && rule.matchesThrough(called.name(), supertypes.get(rule.type())))
            .findFirst();
    }

    static List<Rule> parse(final List<String> lines, final String source) {
        final List<Rule> parsed = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            final String line = lines.get(i).strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            final String[] parts = line.split("\\s+");
            final int hash = parts.length == 2 ? parts[1].indexOf('#') : -1;
            if (hash <= 0 || hash == parts[1].length() - 1) {
                throw new IllegalArgumentException(source + " line " + (i + 1)
                    + ": expected '<kind> <type>#<method>', got '" + line + "'");
            }
            parsed.add(new Rule(parts[0], parts[1].substring(0, hash), parts[1].substring(hash + 1)));
        }
        return parsed;
    }

    private static List<Rule> builtIn() {
        try (InputStream in = ConnectionRules.class.getResourceAsStream(BUILT_IN)) {
            if (in == null) {
                throw new IllegalStateException("The built-in rules " + BUILT_IN + " are missing from the jar");
            }
            final String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return parse(text.lines().toList(), BUILT_IN);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<String> read(final Path file) {
        try {
            return Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Can't read the rules file " + file, e);
        }
    }
}
