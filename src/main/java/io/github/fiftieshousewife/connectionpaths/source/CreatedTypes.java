package io.github.fiftieshousewife.connectionpaths.source;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// Which types the code creates objects of, besides with new: an enum's constants; a class a framework creates, by
// its annotation; a test class; and any class named in a string, which may be created by name.
final class CreatedTypes {

    private static final Set<String> MANAGED = Set.of("Component", "Service", "Repository", "Controller",
        "RestController", "Configuration", "Named", "Singleton", "ApplicationScoped", "RequestScoped",
        "SessionScoped", "Dependent", "Stateless", "Stateful", "MessageDriven", "WebServlet", "WebFilter",
        "WebListener", "Path", "Provider", "ServerEndpoint");
    private static final Pattern PACKAGE_PART = Pattern.compile("[a-z_]\\w*");
    private static final Pattern CLASS_PART = Pattern.compile("[A-Z][\\w$]*");

    private CreatedTypes() {
    }

    // An enum, a class a framework creates, or a test class, which the test runner creates.
    static boolean isCreatedWithoutNew(final TypeDeclaration<?> type, final boolean tests) {
        return type instanceof EnumDeclaration || tests && !TypeHeads.isAbstractClass(type)
            || type.getAnnotations().stream().anyMatch(a -> MANAGED.contains(a.getName().getIdentifier()));
    }

    // The types created as the program runs: those created whatever runs, then, until no more are found, those
    // created in static code or in the code of a type already created, or a supertype of one.
    static Set<String> live(final SourceModel model, final Set<String> roots, final Map<MethodKey, Set<String>> by) {
        final Set<String> live = new HashSet<>(roots);
        final Set<String> running = new HashSet<>();
        Set<String> found = live;
        while (!found.isEmpty()) {
            found.forEach(type -> {
                running.add(type);
                running.addAll(model.types().ancestors(type));
            });
            found = by.entrySet().stream().filter(e -> runs(model, e.getKey(), running))
                .flatMap(e -> e.getValue().stream()).filter(type -> !live.contains(type)).collect(Collectors.toSet());
            live.addAll(found);
        }
        return live;
    }

    // What each method or initialiser outside the test classes creates.
    static Map<MethodKey, Set<String>> outsideTests(final TestModel tests, final Map<MethodKey, Set<String>> by) {
        return by.entrySet().stream().filter(e -> !tests.isTestType(e.getKey().type()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static boolean runs(final SourceModel model, final MethodKey creator, final Set<String> running) {
        return MethodKey.STATICS.equals(creator.name()) || model.tests().isStaticMethod(creator)
            || running.contains(creator.type());
    }

    static Stream<String> namedInStrings(final CompilationUnit file) {
        return file.findAll(StringLiteralExpr.class).stream().map(StringLiteralExpr::asString)
            .filter(CreatedTypes::isClassName);
    }

    // A full class name such as com.example.Store: lower-case package parts, then the class's own name.
    private static boolean isClassName(final String text) {
        final String[] parts = text.split("\\.", -1);
        final int last = parts.length - 1;
        return last > 0 && CLASS_PART.matcher(parts[last]).matches()
            && Arrays.stream(parts, 0, last).allMatch(part -> PACKAGE_PART.matcher(part).matches());
    }
}
