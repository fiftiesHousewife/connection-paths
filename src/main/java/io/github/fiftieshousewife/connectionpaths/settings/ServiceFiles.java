package io.github.fiftieshousewife.connectionpaths.settings;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/** The classes named in META-INF/services files, which Java's ServiceLoader creates by name. */
public final class ServiceFiles {

    private ServiceFiles() {
    }

    /** The class names in the service files under the folders, outside build output. */
    public static List<String> namedIn(final List<Path> folders) {
        return folders.stream().filter(Files::isDirectory).flatMap(ServiceFiles::serviceFilesIn)
            .flatMap(ServiceFiles::classNames).toList();
    }

    private static Stream<Path> serviceFilesIn(final Path folder) {
        try (Stream<Path> paths = Files.walk(folder)) {
            return paths.filter(p -> p.toString().replace('\\', '/').contains("/META-INF/services/"))
                .filter(p -> !p.toString().replace('\\', '/').contains("/target/") && Files.isRegularFile(p))
                .toList().stream();
        } catch (IOException e) {
            throw new UncheckedIOException("Can't read " + folder, e);
        }
    }

    private static Stream<String> classNames(final Path file) {
        try {
            return Files.readAllLines(file, StandardCharsets.UTF_8).stream()
                .map(line -> line.replaceAll("#.*", "").strip()).filter(line -> !line.isEmpty());
        } catch (IOException e) {
            return Stream.empty();
        }
    }
}
