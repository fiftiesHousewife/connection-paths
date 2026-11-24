package io.github.fiftieshousewife.connectionpaths.source;

import io.github.fiftieshousewife.connectionpaths.source.syntax.TypeHeads;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// The types a piece of test code puts a mock in place of: a field annotated @Mock or @MockBean, and each
// mock(T.class) it calls. A spy runs the real code, so it doesn't count.
final class MockedTypes {

    private static final Set<String> MOCK_ANNOTATIONS = Set.of("Mock", "MockBean", "MockitoBean");

    private MockedTypes() {
    }

    static Set<String> in(final Node code) {
        final Stream<String> fields = code instanceof FieldDeclaration field && isMockField(field)
            ? typeName(field.getElementType()).stream() : Stream.empty();
        final Stream<String> made = code.findAll(MethodCallExpr.class, MockedTypes::isMockCall).stream()
            .map(call -> call.getArgument(0)).filter(ClassExpr.class::isInstance)
            .map(argument -> ((ClassExpr) argument).getType()).flatMap(type -> typeName(type).stream());
        return Stream.concat(fields, made).collect(Collectors.toSet());
    }

    private static boolean isMockField(final FieldDeclaration field) {
        return field.getAnnotations().stream().anyMatch(a -> MOCK_ANNOTATIONS.contains(a.getName().getIdentifier()));
    }

    private static boolean isMockCall(final MethodCallExpr call) {
        return "mock".equals(call.getNameAsString()) && !call.getArguments().isEmpty();
    }

    private static Optional<String> typeName(final Type type) {
        return type instanceof ClassOrInterfaceType named ? Optional.of(TypeHeads.qualifiedName(named))
            : Optional.empty();
    }
}
