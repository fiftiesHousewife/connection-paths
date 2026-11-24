package io.github.fiftieshousewife.connectionpaths.source;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

// What a type's declaration says: its supertypes and superclass, the class a @Nested test class sits in, and whether
// it is a base class whose public test...() methods are JUnit 3 tests.
final class TypeHeads {

    private static final Set<String> JUNIT3_BASES = Set.of("TestCase", "AbstractDependencyInjectionSpringContextTests",
        "AbstractTransactionalSpringContextTests", "AbstractTransactionalDataSourceSpringContextTests",
        "AbstractJpaTests", "AbstractSpringContextTests");

    private TypeHeads() {
    }

    static List<String> supertypes(final TypeDeclaration<?> type) {
        return declaredSupertypes(type).map(TypeHeads::qualifiedName).toList();
    }

    private static Stream<ClassOrInterfaceType> declaredSupertypes(final TypeDeclaration<?> type) {
        if (type instanceof ClassOrInterfaceDeclaration declaration) {
            return Stream.concat(declaration.getExtendedTypes().stream(), declaration.getImplementedTypes().stream());
        }
        if (type instanceof EnumDeclaration declaration) {
            return declaration.getImplementedTypes().stream();
        }
        return type instanceof RecordDeclaration declaration ? declaration.getImplementedTypes().stream()
            : Stream.empty();
    }

    static Optional<String> superclass(final TypeDeclaration<?> type) {
        if (type instanceof ClassOrInterfaceDeclaration declaration && !declaration.isInterface()) {
            return declaration.getExtendedTypes().stream().findFirst().map(TypeHeads::qualifiedName);
        }
        return Optional.empty();
    }

    static boolean isAbstractClass(final TypeDeclaration<?> type) {
        return type instanceof ClassOrInterfaceDeclaration declaration && declaration.isAbstract();
    }

    static Optional<String> nestedTestClassIn(final TypeDeclaration<?> type) {
        if (!type.isAnnotationPresent("Nested")) {
            return Optional.empty();
        }
        return type.getParentNode()
            .filter(TypeDeclaration.class::isInstance)
            .flatMap(parent -> ((TypeDeclaration<?>) parent).getFullyQualifiedName());
    }

    static boolean isJunit3Base(final String type) {
        return JUNIT3_BASES.contains(type.substring(type.lastIndexOf('.') + 1));
    }

    static SourceModel.Location location(final Node node, final String file) {
        return new SourceModel.Location(file, node.getBegin().map(p -> p.line).orElse(0));
    }

    // A supertype's full name, or ?.Name where it can't be resolved.
    @SuppressWarnings({"PMD.AvoidCatchingGenericException", "PMD.AvoidCatchingThrowable"})
    static String qualifiedName(final ClassOrInterfaceType type) {
        try {
            return type.resolve().asReferenceType().getQualifiedName();
        } catch (RuntimeException | StackOverflowError e) {
            return "?." + type.getNameAsString();
        }
    }
}
