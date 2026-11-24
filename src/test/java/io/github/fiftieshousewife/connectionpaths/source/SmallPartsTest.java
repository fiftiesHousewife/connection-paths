package io.github.fiftieshousewife.connectionpaths.source;

import io.github.fiftieshousewife.connectionpaths.ConnectionPaths;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.Test;

class SmallPartsTest {

    @Test
    void aMethodKeyReadsLikeAStackTraceLine() {
        final MethodKey key = new MethodKey("com.example.db.Database", "open", "String,int");
        assertAll(
            () -> assertThat(key.toString()).isEqualTo("com.example.db.Database#open(String,int)"),
            () -> assertThat(key.shortName()).isEqualTo("Database.open"),
            () -> assertThat(key.overrideKey("a.Store")).isEqualTo("a.Store#open/2"),
            () -> assertThat(key.compareTo(new MethodKey("com.example.db.Database", "close", ""))).isPositive(),
            () -> assertThat(key.isConstructor()).isFalse(),
            () -> assertThat(MethodKey.fieldsOf("a.B").toString()).isEqualTo("a.B#<fields>()"));
    }

    @Test
    void parameterTypesAreSimpleNamesWithoutGenerics() {
        assertAll(
            () -> assertThat(Signatures.simple("java.util.Map<java.lang.String, java.util.List<a.Order>>"))
                .isEqualTo("Map"),
            () -> assertThat(Signatures.simple("@NonNull String")).isEqualTo("String"),
            () -> assertThat(Signatures.simple("java.lang.String...")).isEqualTo("String[]"),
            () -> assertThat(Signatures.simple("Map.Entry<K, V>[]")).isEqualTo("Entry[]"));
    }

    @Test
    void unresolvedReasonsKeepMissingTypesButGroupExpressions() {
        assertAll(
            () -> assertThat(Unresolved.generalise("Unsolved symbol : JID")).isEqualTo("Unsolved symbol : JID"),
            () -> assertThat(Unresolved.generalise("Unsolved symbol in Post.deliver(to) : org.mail.Post"))
                .isEqualTo("Unsolved symbol : org.mail.Post"),
            () -> assertThat(Unresolved.generalise("Unsolved symbol in packet.getFrom() : packet"))
                .isEqualTo("Unsolved symbol in an expression"),
            () -> assertThat(Unresolved.generalise("Method 'getNode' cannot be resolved"))
                .isEqualTo("Method '_' cannot be resolved"),
            () -> assertThat(Unresolved.generalise("x".repeat(200))).hasSize(93));
    }

    @Test
    void unresolvedCountsAndOrdersReasons() {
        final Unresolved unresolved = new Unresolved();
        final SourceModel.Location at = new SourceModel.Location("A.java", 3);
        unresolved.add(new IllegalStateException("one"), at);
        unresolved.add(new IllegalStateException("two"), at);
        unresolved.add(new IllegalStateException("two"), at);
        assertAll(
            () -> assertThat(unresolved.getTotal()).isEqualTo(3),
            () -> assertThat(unresolved.top(1)).containsExactly(
                new Unresolved.Reason("IllegalStateException: two", 2, "A.java:3 two")));
    }

    @Test
    void theVersionComesFromTheJarOrSaysDevelopment() {
        assertThat(ConnectionPaths.version()).isEqualTo("development");
    }
}
