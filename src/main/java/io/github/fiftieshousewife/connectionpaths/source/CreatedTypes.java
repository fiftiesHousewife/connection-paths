package io.github.fiftieshousewife.connectionpaths.source;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import java.util.Arrays;
import java.util.Set;
import java.util.regex.Pattern;
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
