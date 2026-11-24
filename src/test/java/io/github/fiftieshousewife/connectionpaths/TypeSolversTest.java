package io.github.fiftieshousewife.connectionpaths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TypeSolversTest {

    @Test
    void resolvesTheJdkButNotTheLibrariesThisToolRunsWith(@TempDir final Path dir) {
        final CombinedTypeSolver solver = TypeSolvers.create(Options.builder().mainSource(dir).build()).solver();
        assertAll(
            () -> assertThat(solver.tryToSolveType("java.sql.DriverManager").isSolved()).isTrue(),
            () -> assertThat(solver.tryToSolveType("org.xml.sax.SAXException").isSolved()).isTrue(),
            () -> assertThat(solver.tryToSolveType("org.slf4j.Logger").isSolved()).isFalse());
    }

    @Test
    void notesClasspathEntriesItCannotUse(@TempDir final Path dir) throws IOException {
        final Path notAJar = Files.writeString(dir.resolve("broken.jar"), "not a zip");
        final Options options = Options.builder()
            .mainSource(dir)
            .classpathEntry(dir.resolve("missing.jar"))
            .classpathEntry(dir)
            .classpathEntry(notAJar)
            .build();
        assertThat(TypeSolvers.create(options).notes()).satisfiesExactly(
            note -> assertThat(note).startsWith("not found: "),
            note -> assertThat(note).startsWith("a class folder, not read"),
            note -> assertThat(note).startsWith("could not read: broken.jar"));
    }

    @Test
    void readsAJarWhoseModuleInfoItCannotParse(@TempDir final Path dir) throws IOException {
        final Path jar = jar(dir.resolve("odd.jar"), "module-info.class", "readme.txt");
        final Options options = Options.builder().mainSource(dir).classpathEntry(jar).build();
        assertThat(TypeSolvers.create(options).notes()).containsExactly("read without its module-info: odd.jar");
    }

    @Test
    void aCopyWithoutModuleInfoKeepsEverythingElse(@TempDir final Path dir) throws IOException {
        final Path copy = TypeSolvers.withoutModuleInfo(jar(dir.resolve("a.jar"), "module-info.class", "readme.txt"));
        try (ZipFile zip = new ZipFile(copy.toFile())) {
            assertAll(
                () -> assertThat(zip.getEntry("readme.txt")).isNotNull(),
                () -> assertThat(zip.getEntry("module-info.class")).isNull());
        }
    }

    private static Path jar(final Path file, final String... names) throws IOException {
        try (OutputStream stream = Files.newOutputStream(file); ZipOutputStream zip = new ZipOutputStream(stream)) {
            for (final String name : names) {
                zip.putNextEntry(new ZipEntry(name));
                zip.write("not a class".getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return file;
    }
}
