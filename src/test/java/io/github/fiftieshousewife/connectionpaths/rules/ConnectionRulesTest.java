package io.github.fiftieshousewife.connectionpaths.rules;

import io.github.fiftieshousewife.connectionpaths.source.MethodKey;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConnectionRulesTest {

    private static final ConnectionRules RULES = ConnectionRules.of(ConnectionRules.parse(List.of(
        "# a comment",
        "",
        "jndi javax.naming.InitialContext#<init>",
        "directory javax.naming.directory.InitialDirContext#<init>",
        "database org.example.Jdbc#*",
        "database javax.sql.DataSource#getConnection"), "test"));

    @Test
    void aWildcardMatchesEveryMethodButTheConstructor() {
        assertAll(
            () -> assertThat(kind(new MethodKey("org.example.Jdbc", "query", "String"), Set.of())).contains("database"),
            () -> assertThat(kind(new MethodKey("org.example.Jdbc", "<init>", ""), Set.of())).isEmpty());
    }

    @Test
    void aRuleOnASupertypeMatchesItsSubtypes() {
        final MethodKey call = new MethodKey("org.pool.PooledSource", "getConnection", "");
        assertThat(kind(call, Set.of("javax.sql.DataSource"))).contains("database");
    }

    @Test
    void aConstructorMatchesOnlyARuleForItsOwnType() {
        final MethodKey call = new MethodKey("javax.naming.directory.InitialDirContext", "<init>", "String");
        assertThat(kind(call, Set.of("javax.naming.InitialContext"))).contains("directory");
    }

    @Test
    void theBuiltInRulesLoadAndCanBeExtended(@TempDir final Path dir) throws IOException {
        final Path extra = Files.writeString(dir.resolve("rules.txt"), "queue com.corp.Bus#publish\n");
        final ConnectionRules rules = ConnectionRules.load(Optional.of(extra));
        assertAll(
            () -> assertThat(rules.getRules()).extracting(ConnectionRules.Rule::kind)
                .contains("database", "http", "queue"),
            () -> assertThat(rules.match(new MethodKey("com.corp.Bus", "publish", "String"), Map.of())).isPresent());
    }

    @Test
    void aBadLineSaysWhereItIs() {
        assertThatThrownBy(() -> ConnectionRules.parse(List.of("database DriverManager"), "mine.txt"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("mine.txt line 1");
    }

    @Test
    void aRuleCanNameAnAnnotationInstead() {
        final ConnectionRules.Rule rule = ConnectionRules.parse(List.of("http @retrofit2.http.GET"), "mine.txt").get(0);
        assertAll(
            () -> assertThat(rule.isAnnotation()).isTrue(),
            () -> assertThat(rule).hasToString("@retrofit2.http.GET"),
            () -> assertThatThrownBy(() -> ConnectionRules.parse(List.of("http @"), "mine.txt"))
                .hasMessageContaining("@<annotation>"));
    }

    @Test
    void aMissingRulesFileIsReported(@TempDir final Path dir) {
        final Optional<Path> missing = Optional.of(dir.resolve("missing.txt"));
        assertThatThrownBy(() -> ConnectionRules.load(missing)).hasMessageContaining("missing.txt");
    }

    private static Optional<String> kind(final MethodKey call, final Set<String> supertypes) {
        final Map<String, Set<String>> unknownMethods =
            supertypes.stream().collect(Collectors.toMap(type -> type, type -> Set.of()));
        return RULES.match(call, unknownMethods).map(ConnectionRules.Rule::kind);
    }
}
