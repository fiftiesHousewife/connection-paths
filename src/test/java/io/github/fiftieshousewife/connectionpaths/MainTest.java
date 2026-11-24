package io.github.fiftieshousewife.connectionpaths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MainTest {

    @Test
    void writesTheFactsFile(@TempDir final Path dir) throws IOException {
        final Path out = dir.resolve("facts/connection-paths.json");
        final int status = Main.run(List.of("--module", "src/test/resources/example", "--out", out.toString()));
        final String json = Files.readString(out);
        assertAll(
            () -> assertThat(status).isEqualTo(Main.OK),
            () -> assertThat(json).contains("\"schema\" : \"connection-paths/3\""),
            () -> assertThat(json).contains("\"tests_reaching\" : 20"),
            () -> assertThat(json).contains("\"through_set_up\" : true"),
            () -> assertThat(json).contains("\"dispatch_points_not_followed\""));
    }

    @Test
    void wrongArgumentsGiveTheUsage() {
        assertAll(
            () -> assertThat(Main.run(List.of())).isEqualTo(Main.WRONG_ARGUMENTS),
            () -> assertThat(Main.run(List.of("--colour", "red"))).isEqualTo(Main.WRONG_ARGUMENTS),
            () -> assertThat(Main.run(List.of("--help"))).isEqualTo(Main.OK));
    }

    @Test
    void aFileThatCantBeWrittenFails(@TempDir final Path dir) throws IOException {
        final Path blocked = Files.writeString(dir.resolve("a-file"), "");
        final String out = blocked.resolve("facts.json").toString();
        assertThat(Main.run(List.of("--main", "src/test/resources/example/src/main/java", "--out", out)))
            .isEqualTo(Main.FAILED);
    }

    @Test
    void aMissingFolderIsSkipped(@TempDir final Path dir) {
        final Path out = dir.resolve("facts.json");
        assertThat(Main.run(List.of("--main", dir.resolve("nothing").toString(),
            "--main", "src/test/resources/example/src/main/java", "--out", out.toString())))
            .isEqualTo(Main.OK);
    }

    @Test
    void noFolderThatExistsIsWrong(@TempDir final Path dir) {
        assertThat(Main.run(List.of("--main", dir.resolve("nothing").toString())))
            .isEqualTo(Main.WRONG_ARGUMENTS);
    }

    @Test
    void aBadRulesFileIsWrong(@TempDir final Path dir) throws IOException {
        final Path rules = Files.writeString(dir.resolve("rules.txt"), "not a rule");
        assertThat(Main.run(List.of("--main", "src/test/resources/example/src/main/java", "--rules", rules.toString(),
            "--out", dir.resolve("facts.json").toString())))
            .isEqualTo(Main.WRONG_ARGUMENTS);
    }

    @Test
    void writesTheSameFactsEveryTime(@TempDir final Path dir) throws IOException {
        final Path first = dir.resolve("first.json");
        final Path second = dir.resolve("second.json");
        Main.run(List.of("--module", "src/test/resources/example", "--out", first.toString()));
        Main.run(List.of("--module", "src/test/resources/example", "--out", second.toString()));
        assertThat(Files.readString(second)).isEqualTo(Files.readString(first));
    }
}
