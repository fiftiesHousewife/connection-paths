package io.github.fiftieshousewife.connectionpaths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OptionsParserTest {

    @Test
    void aModuleMeansItsMainAndTestSource() {
        final Options options = OptionsParser.parse(List.of("--module", "server"));
        assertAll(
            () -> assertThat(options.getMainSources()).containsExactly(Path.of("server/src/main/java")),
            () -> assertThat(options.getTestSources()).containsExactly(Path.of("server/src/test/java")),
            () -> assertThat(options.getMaxImplementations()).isEqualTo(Options.DEFAULT_MAX_IMPLEMENTATIONS),
            () -> assertThat(options.getOut()).isEqualTo(Path.of("connection-paths.json")));
    }

    @Test
    void readsEveryOption() {
        final String classpath = "x.jar" + File.pathSeparator + "y.jar";
        final Options options = OptionsParser.parse(List.of("--main", "a", "--test", "b", "--rules", "r.txt",
            "--max-implementations", "5", "--out", "facts.json", "--classpath", classpath));
        assertAll(
            () -> assertThat(options.getMainSources()).containsExactly(Path.of("a")),
            () -> assertThat(options.getTestSources()).containsExactly(Path.of("b")),
            () -> assertThat(options.extraRules()).contains(Path.of("r.txt")),
            () -> assertThat(options.getMaxImplementations()).isEqualTo(5),
            () -> assertThat(options.getOut()).isEqualTo(Path.of("facts.json")),
            () -> assertThat(options.getClasspath()).containsExactly(Path.of("x.jar"), Path.of("y.jar")));
    }

    @Test
    void readsTheClasspathFromAFile(@TempDir final Path dir) throws IOException {
        final Path file = Files.writeString(dir.resolve("cp.txt"), "a.jar\nb.jar" + File.pathSeparator + "c.jar\n");
        assertThat(OptionsParser.classpath("@" + file))
            .containsExactly(Path.of("a.jar"), Path.of("b.jar"), Path.of("c.jar"));
    }

    @Test
    void explainsWrongArguments() {
        assertAll(
            () -> assertThatThrownBy(() -> OptionsParser.parse(List.of("--rules", "r.txt")))
                .hasMessageContaining("at least one"),
            () -> assertThatThrownBy(() -> OptionsParser.parse(List.of("--main")))
                .hasMessageContaining("needs a value"),
            () -> assertThatThrownBy(() -> OptionsParser.parse(List.of("--colour", "red")))
                .hasMessageContaining("Unknown option --colour"),
            () -> assertThatThrownBy(() -> OptionsParser.parse(List.of("--main", "a", "--max-implementations", "0")))
                .hasMessageContaining("1 or more"),
            () -> assertThatThrownBy(() -> OptionsParser.parse(List.of("--main", "a", "--max-implementations", "x")))
                .hasMessageContaining("must be a number"));
    }
}
