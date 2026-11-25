package io.github.fiftieshousewife.connectionpaths.facts;

import io.github.fiftieshousewife.connectionpaths.source.SourceModel;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import java.util.List;
import java.util.Optional;

// Which test framework runs a test, from its test annotation and what the file imports: JUnit Jupiter, as junit6 where
// its API on the classpath is version 6 or later; JUnit 4; TestNG; or JUnit 3, a test with no test annotation.
final class TestFrameworks {

    private static final int JUNIT6 = 6;

    private final SourceModel model;
    private final int jupiter;

    TestFrameworks(final SourceModel model, final int jupiter) {
        this.model = model;
        this.jupiter = jupiter;
    }

    Optional<String> of(final SourceModel.Location at) {
        return model.reread(at.file()).flatMap(unit -> unit.findFirst(MethodDeclaration.class,
                method -> method.getBegin().map(p -> p.line).orElse(0) == at.line())
            .flatMap(method -> ofItsAnnotations(unit, method)));
    }

    private Optional<String> ofItsAnnotations(final CompilationUnit unit, final MethodDeclaration method) {
        final Optional<String> found = method.getAnnotations().stream().map(a -> named(unit, a))
            .flatMap(Optional::stream).findFirst();
        final boolean annotated = method.getAnnotations().stream().anyMatch(a -> a.getName().getIdentifier()
            .endsWith("Test"));
        return found.isPresent() || annotated ? found : Optional.of("junit3");
    }

    // The framework a test annotation belongs to: by its own qualified name, or by the import it comes from.
    private Optional<String> named(final CompilationUnit unit, final AnnotationExpr annotation) {
        final String name = annotation.getNameAsString();
        final List<String> from = name.contains(".") ? List.of(name) : unit.getImports().stream()
            .filter(i -> !i.isStatic()).map(i -> i.isAsterisk() ? i.getNameAsString() + "." + name
                : i.getNameAsString()).filter(i -> i.endsWith("." + name)).toList();
        return from.stream().map(this::frameworkOf).flatMap(Optional::stream).findFirst();
    }

    private Optional<String> frameworkOf(final String annotation) {
        if (annotation.startsWith("org.junit.jupiter.")) {
            return Optional.of(jupiter >= JUNIT6 ? "junit6" : "junit5");
        }
        if (annotation.startsWith("org.testng.")) {
            return Optional.of("testng");
        }
        return annotation.startsWith("org.junit.") ? Optional.of("junit4") : Optional.empty();
    }
}
