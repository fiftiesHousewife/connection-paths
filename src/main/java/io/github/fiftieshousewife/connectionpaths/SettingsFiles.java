package io.github.fiftieshousewife.connectionpaths;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

/**
 * The settings in a repo's properties, YAML and XML files, by dotted key. YAML and XML are flattened to the path of
 * their keys or elements: {@code <database><serverURL>} is {@code database.serverURL}, with and without the root
 * element. Where a key is in more than one file, the value kept is from production settings before test ones, then
 * a file without a profile ({@code application.yml}) before one with ({@code application-dev.yml}), then file-name
 * order. Keys are matched loosely, as Spring matches them: {@code jdbcUrl}, {@code jdbc-url} and {@code JDBC_URL}
 * are one key.
 */
@Slf4j
final class SettingsFiles {

    /** A setting's value, and where it is. */
    record Setting(String value, String file, int line) {
    }

    private static final Set<String> SKIPPED_FOLDERS =
        Set.of("build", "target", ".git", ".gradle", ".idea", "node_modules", "out");
    private static final Set<String> SKIPPED_FILES = Set.of("pom.xml", "build.xml", "ivy.xml", "checkstyle.xml");
    private static final Set<String> EXTENSIONS = Set.of("properties", "yml", "yaml", "xml");
    private static final long LARGEST = 1_000_000;
    /** Capitals for YOUR-, CHANGE-ME, TODO and XXX, so ordinary words don't count; any case for the rest. */
    private static final Pattern PLACEHOLDER = Pattern.compile(".*(YOUR[-_]|<[^>]+>|\\$\\{|CHANGE[-_ ]?ME|TODO|XXX"
        + "|(?i:example\\.(com|org|net)|changeme|placeholder|//(undefined|unset|invalid)\\b)).*");

    /** A Spring profile's file, which holds settings only for when that profile is on. */
    private static final Pattern PROFILE_FILE = Pattern.compile("(application|bootstrap)-[^.]+\\.(properties|ya?ml)");

    private final Map<String, Setting> settings = new TreeMap<>();
    private final Map<String, Setting> loosely = new TreeMap<>();

    private SettingsFiles() {
    }

    static SettingsFiles read(final List<Path> folders) {
        final SettingsFiles files = new SettingsFiles();
        filesIn(folders).forEach(files::readFile);
        return files;
    }

    /** The properties, YAML and XML files in the folders, in the order their settings are kept. */
    static List<Path> filesIn(final List<Path> folders) {
        return folders.stream().filter(Files::isDirectory).flatMap(SettingsFiles::settingsFilesIn)
            .sorted(Comparator.comparing(SettingsFiles::isTestSettings).thenComparing(SettingsFiles::isProfileFile)
                .thenComparing(Comparator.naturalOrder()))
            .toList();
    }

    Optional<Setting> get(final String key) {
        return Optional.ofNullable(settings.get(key)).or(() -> Optional.ofNullable(loosely.get(loose(key))));
    }

    /** A key as Spring compares keys: without dashes, with underscores as dots, in lower case. */
    private static String loose(final String key) {
        final StringBuilder text = new StringBuilder(key.length());
        for (final char c : key.toCharArray()) {
            if (c != '-') {
                text.append(c == '_' ? '.' : Character.toLowerCase(c));
            }
        }
        return text.toString();
    }

    private static boolean isTestSettings(final Path file) {
        return file.toString().replace('\\', '/').contains("/src/test/");
    }

    private static boolean isProfileFile(final Path file) {
        return PROFILE_FILE.matcher(String.valueOf(file.getFileName())).matches();
    }

    int size() {
        return settings.size();
    }

    /**
     * Whether a value looks like one a person is meant to fill in, or a stand-in until one is: YOUR-..., CHANGE-ME,
     * ${...}, example.com, http://undefined/.
     */
    static boolean isPlaceholder(final String value) {
        return value != null && PLACEHOLDER.matcher(value).matches();
    }

    /** A setting's address with its value from the settings files, where they hold one. */
    Address withValue(final Address address) {
        if (!Address.SETTING.equals(address.getSource())) {
            return address.toBuilder().placeholder(address.isLiteral() && isPlaceholder(address.getValue())).build();
        }
        return get(address.getValue())
            .map(s -> address.toBuilder().settingValue(s.value()).settingFile(s.file()).settingLine(s.line())
                .placeholder(isPlaceholder(s.value())).build())
            .orElse(address);
    }

    private static Stream<Path> settingsFilesIn(final Path folder) {
        try (Stream<Path> paths = Files.walk(folder)) {
            return paths.filter(SettingsFiles::isSettingsFile).toList().stream();
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
        for (final Path part : path) {
            if (SKIPPED_FOLDERS.contains(part.toString())) {
                return true;
            }
        }
        return false;
    }

    private static boolean smallEnough(final Path path) {
        try {
            return Files.size(path) <= LARGEST;
        } catch (IOException e) {
            return false;
        }
    }

    private void readFile(final Path file) {
        final String name = file.toString().replace('\\', '/');
        try {
            if (name.endsWith(".xml")) {
                XmlSettings.read(file, (key, value, line) -> add(key, new Setting(value, name, line)));
                return;
            }
            final List<String> lines = Files.readAllLines(file, StandardCharsets.ISO_8859_1);
            if (name.endsWith(".properties")) {
                TextSettings.properties(lines, (key, value, line) -> add(key, new Setting(value, name, line)));
            } else {
                TextSettings.yaml(lines, (key, value, line) -> add(key, new Setting(value, name, line)));
            }
        } catch (IOException | IllegalArgumentException e) {
            log.debug("Settings not read from {}: {}", name, e.getMessage());
        }
    }

    private void add(final String key, final Setting setting) {
        settings.putIfAbsent(key, setting);
        loosely.putIfAbsent(loose(key), setting);
    }

    /** Receives each setting a file holds. */
    @FunctionalInterface
    interface Found {
        void setting(String key, String value, int line);
    }
}
