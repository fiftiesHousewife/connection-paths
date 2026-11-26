package io.github.fiftieshousewife.connectionpaths.source;

import io.github.fiftieshousewife.connectionpaths.source.syntax.Signatures;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The annotations by which a test class starts a framework, such as Spring, Quarkus or Micronaut, or containers. The
 * framework wires objects together and may open connections that no call in the source shows, so these tests are
 * listed by the annotation rather than by a chain of calls. Its {@code @ActiveProfiles} names the Spring profiles it
 * runs with.
 */
public final class Frameworks {

    private static final Set<String> STARTERS = Set.of("SpringBootTest", "DataJpaTest", "DataJdbcTest", "JdbcTest",
        "JooqTest", "DataMongoTest", "DataRedisTest", "DataCassandraTest", "DataElasticsearchTest", "DataNeo4jTest",
        "DataLdapTest", "DataR2dbcTest", "WebMvcTest", "WebFluxTest", "RestClientTest", "GraphQlTest",
        "ContextConfiguration", "SpringJUnitConfig", "SpringJUnitWebConfig", "WebAppConfiguration", "Testcontainers",
        "QuarkusTest", "QuarkusIntegrationTest", "QuarkusMainTest", "MicronautTest");
    private static final List<String> PORTS = List.of("RANDOM_PORT", "DEFINED_PORT");
    private static final Set<String> RUN_WITH = Set.of("RunWith", "ExtendWith");
    private static final Set<String> RUNNERS = Set.of("SpringRunner", "SpringJUnit4ClassRunner", "SpringExtension",
        "QuarkusTestExtension", "MicronautJunit5Extension", "Arquillian", "ArquillianExtension");

    private Frameworks() {
    }

    // The annotation on a type that starts a framework, as written: @SpringBootTest, with the port it serves the
    // application on, if any: @SpringBootTest(webEnvironment = RANDOM_PORT).
    static Optional<String> startedBy(final TypeDeclaration<?> type) {
        return type.getAnnotations().stream().map(Frameworks::started).flatMap(Optional::stream).findFirst();
    }

    // The Spring profiles a type's @ActiveProfiles names, in its order.
    static List<String> profilesOf(final TypeDeclaration<?> type) {
        return type.getAnnotationByName("ActiveProfiles").stream()
            .flatMap(annotation -> annotation.findAll(StringLiteralExpr.class).stream())
            .map(StringLiteralExpr::asString).filter(profile -> !profile.isBlank()).toList();
    }

    private static Optional<String> started(final AnnotationExpr annotation) {
        final String name = annotation.getName().getIdentifier();
        if (STARTERS.contains(name)) {
            return Optional.of("@" + name + onAPort(annotation));
        }
        return RUN_WITH.contains(name) ? runnerIn(annotation).map(runner -> "@" + name + "(" + runner + ".class)")
            : Optional.empty();
    }

    private static String onAPort(final AnnotationExpr annotation) {
        return PORTS.stream().filter(port -> annotation.toString().contains(port)).findFirst()
            .map(port -> "(webEnvironment = " + port + ")").orElse("");
    }

    /** Whether an annotation that starts a framework serves the application on a port of this machine. */
    public static boolean servesOnAPort(final String annotation) {
        return PORTS.stream().anyMatch(annotation::contains);
    }

    private static Optional<String> runnerIn(final AnnotationExpr annotation) {
        return annotation.findAll(ClassExpr.class).stream()
            .map(type -> Signatures.simple(type.getType().asString()))
            .filter(RUNNERS::contains)
            .findFirst();
    }
}
