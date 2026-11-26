package io.github.fiftieshousewife.connectionpaths.settings;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

// The properties, YAML and XML files under some folders that may hold settings, outside build output, tool files and
// modules of their own inside a folder, and no larger than a megabyte, in the order their settings are kept: production
// before test, then by name. A file for one Spring profile (application-dev.yml) is left out: its settings hold only
// where that profile is active.
final class SettingsFileFinder {

    private static final Set<String> SKIPPED_FOLDERS =
        Set.of("build", "target", ".git", ".gradle", ".idea", "node_modules", "out");
    private static final Set<String> SKIPPED_FILES = Set.of("pom.xml", "build.xml", "ivy.xml", "checkstyle.xml");
    private static final Set<String> BUILD_FILES = Set.of("pom.xml", "build.gradle", "build.gradle.kts", "build.xml");
    private static final Set<String> EXTENSIONS = Set.of("properties", "yml", "yaml", "xml");
    private static final long LARGEST = 1_000_000;
    private static final Pattern PROFILE_FILE =
        Pattern.compile("(application|bootstrap)-([^.]+)\\.(properties|ya?ml)");
    private static final Comparator<Path> TEST_FIRST =
        Comparator.comparing((Path file) -> !isTestSettings(file)).thenComparing(Comparator.naturalOrder());

    private SettingsFileFinder() {
    }

    static List<Path> filesIn(final List<Path> folders) {
        return folders.stream().filter(Files::isDirectory).flatMap(SettingsFileFinder::settingsFilesIn)
            .filter(file -> !isProfileFile(file))
            .sorted(Comparator.comparing(SettingsFileFinder::isTestSettings).thenComparing(Comparator.naturalOrder()))
            .toList();
    }

    // The files a test finds on its classpath, test resources before main ones.
    static List<Path> testClasspathFilesIn(final List<Path> folders) {
        return filesIn(folders).stream().filter(SettingsFileFinder::isOnTheTestClasspath).sorted(TEST_FIRST).toList();
    }

    // The Spring profiles' files a test finds on its classpath, the last profile's first.
    static List<Path> profileFilesIn(final List<Path> folders, final List<String> profiles) {
        if (profiles.isEmpty()) {
            return List.of();
        }
        final List<Path> onTheClasspath = folders.stream().filter(Files::isDirectory)
            .flatMap(SettingsFileFinder::settingsFilesIn).filter(SettingsFileFinder::isOnTheTestClasspath).toList();
        final List<String> lastFirst = new ArrayList<>(profiles);
        Collections.reverse(lastFirst);
        return lastFirst.stream().flatMap(profile -> onTheClasspath.stream().filter(file -> isFileOf(file, profile))
            .sorted(TEST_FIRST)).toList();
    }

    private static boolean isFileOf(final Path file, final String profile) {
        final Matcher matcher = PROFILE_FILE.matcher(String.valueOf(file.getFileName()));
        return matcher.matches() && matcher.group(2).equals(profile);
    }

    static boolean isOnTheTestClasspath(final Path file) {
        final String set = innermostSourceSet(file);
        return set.startsWith("/src/test/resources/") || set.startsWith("/src/main/resources/");
    }

    // The folder's files, but for those of a module inside it, a folder with a build file of its own.
    private static Stream<Path> settingsFilesIn(final Path folder) {
        try (Stream<Path> paths = Files.walk(folder)) {
            final List<Path> all = paths.toList();
            final List<Path> modules = all.stream()
                .filter(path -> BUILD_FILES.contains(String.valueOf(path.getFileName()))).map(Path::getParent)
                .filter(module -> !module.equals(folder)).toList();
            return all.stream().filter(SettingsFileFinder::isSettingsFile)
                .filter(file -> modules.stream().noneMatch(file::startsWith));
        } catch (IOException e) {
            throw new UncheckedIOException("Can't read " + folder, e);
        }
    }

    private static boolean isSettingsFile(final Path path) {
        final String name = String.valueOf(path.getFileName());
        final boolean wanted = EXTENSIONS.contains(name.substring(name.lastIndexOf('.') + 1))
            && !SKIPPED_FILES.contains(name);
        return wanted && Files.isRegularFile(path) && !inSkippedFolder(path) && smallEnough(path);
    }

    private static boolean inSkippedFolder(final Path path) {
        return StreamSupport.stream(path.spliterator(), false)
            .anyMatch(part -> SKIPPED_FOLDERS.contains(part.toString()));
    }

    private static boolean smallEnough(final Path path) {
        try {
            return Files.size(path) <= LARGEST;
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean isTestSettings(final Path file) {
        return innermostSourceSet(file).startsWith("/src/test/");
    }

    // The path from its innermost /src/main/ or /src/test/ on, as a repo can sit inside another's test resources.
    private static String innermostSourceSet(final Path file) {
        final String path = file.toAbsolutePath().normalize().toString().replace('\\', '/');
        final int at = Math.max(path.lastIndexOf("/src/main/"), path.lastIndexOf("/src/test/"));
        return at < 0 ? "" : path.substring(at);
    }

    private static boolean isProfileFile(final Path file) {
        return PROFILE_FILE.matcher(String.valueOf(file.getFileName())).matches();
    }
}
