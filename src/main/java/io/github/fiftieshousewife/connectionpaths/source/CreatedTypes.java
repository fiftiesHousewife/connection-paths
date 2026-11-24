package io.github.fiftieshousewife.connectionpaths.source;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.StringLiteralExpr;
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
    private static final Pattern CLASS_NAME = Pattern.compile("([a-z_][\\w]*\\.)+[A-Z][\\w$]*");

    private CreatedTypes() {
    }

    // An enum, a class a framework creates, or a test class, which the test runner creates.
    static boolean isCreatedWithoutNew(final TypeDeclaration<?> type, final boolean tests) {
        return type instanceof EnumDeclaration || tests && !TypeHeads.isAbstractClass(type)
            || type.getAnnotations().stream().anyMatch(a -> MANAGED.contains(a.getName().getIdentifier()));
    }

    static Stream<String> namedInStrings(final CompilationUnit file) {
        return file.findAll(StringLiteralExpr.class).stream().map(StringLiteralExpr::asString)
            .filter(text -> CLASS_NAME.matcher(text).matches());
    }
}
