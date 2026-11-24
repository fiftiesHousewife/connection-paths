package io.github.fiftieshousewife.connectionpaths.source;

import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import java.util.Optional;
import java.util.Set;

// A test class's rule, extension or Testcontainers container that starts a server on this machine for its tests,
// such as an embedded LDAP or mail server: a stand-in already in place for the system its tests connect to.
final class StandIns {

    private static final Set<String> RULE_ANNOTATIONS = Set.of("Rule", "ClassRule", "RegisterExtension");
    private static final Set<String> SERVERS = Set.of("GreenMailRule", "GreenMailExtension", "WireMockRule",
        "WireMockClassRule", "WireMockExtension", "MockWebServer", "SingleInstancePostgresRule", "PreparedDbRule",
        "EmbeddedKafkaRule", "EmbeddedKafkaExtension");

    private StandIns() {
    }

    static Optional<String> in(final FieldDeclaration field) {
        if (!(field.getElementType() instanceof ClassOrInterfaceType type)) {
            return Optional.empty();
        }
        final boolean starts = annotated(field, Set.of("Container"))
            || annotated(field, RULE_ANNOTATIONS) && startsAServer(type);
        return starts ? Optional.of(nameOf(type)) : Optional.empty();
    }

    private static boolean annotated(final FieldDeclaration field, final Set<String> annotations) {
        return field.getAnnotations().stream().anyMatch(a -> annotations.contains(a.getName().getIdentifier()));
    }

    private static boolean startsAServer(final ClassOrInterfaceType type) {
        final String name = type.getNameAsString();
        return SERVERS.contains(name) || name.startsWith("Embedded") || name.startsWith("InMemory");
    }

    // The type's full name: resolved, or, where its library isn't on the classpath, from the file's imports.
    private static String nameOf(final ClassOrInterfaceType type) {
        final String resolved = TypeHeads.qualifiedName(type);
        if (!resolved.startsWith("?.")) {
            return resolved;
        }
        return type.findCompilationUnit().stream().flatMap(unit -> unit.getImports().stream())
            .filter(i -> !i.isAsterisk() && i.getName().getIdentifier().equals(type.getNameAsString()))
            .map(ImportDeclaration::getNameAsString).findFirst().orElse(type.getNameAsString());
    }
}
