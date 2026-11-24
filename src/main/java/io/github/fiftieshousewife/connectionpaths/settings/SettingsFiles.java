package io.github.fiftieshousewife.connectionpaths.settings;

import io.github.fiftieshousewife.connectionpaths.address.Address;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;
import java.util.regex.Pattern;
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
public final class SettingsFiles {

    /** A setting's value, and where it is. */
    record Setting(String value, String file, int line) {
    }

    private static final Pattern PLACEHOLDER = Pattern.compile(".*(YOUR[-_]|<[^>]+>|\\$\\{|CHANGE[-_ ]?ME|TODO|XXX"
        + "|(?i:example\\.(com|org|net)|changeme|placeholder|//(undefined|unset|invalid)\\b)).*");


    private final Map<String, Setting> settings = new TreeMap<>();
    private final Map<String, Setting> loosely = new TreeMap<>();
    private final Optional<SettingsFiles> testClasspath;

    private SettingsFiles(final List<Path> files, final Optional<SettingsFiles> testClasspath) {
        this.testClasspath = testClasspath;
        files.forEach(this::readFile);
    }

    /** The settings in the folders' files; and, apart, those on the test classpath, test resources first. */
    public static SettingsFiles read(final List<Path> folders) {
        return new SettingsFiles(filesIn(folders),
            Optional.of(new SettingsFiles(SettingsFileFinder.testClasspathFilesIn(folders), Optional.empty())));
    }

    /** The settings a test runs with: those in test resources, then main resources. */
    public SettingsFiles onTheTestClasspath() {
        return testClasspath.orElse(this);
    }

    /** The properties, YAML and XML files in the folders, in the order their settings are kept. */
    public static List<Path> filesIn(final List<Path> folders) {
        return SettingsFileFinder.filesIn(folders);
    }

    Optional<Setting> get(final String key) {
        return Optional.ofNullable(settings.get(key)).or(() -> Optional.ofNullable(loosely.get(loose(key))));
    }

    // A key as Spring compares keys: without dashes, with underscores as dots, in lower case.
    private static String loose(final String key) {
        return key.chars().filter(c -> c != '-').map(c -> c == '_' ? '.' : Character.toLowerCase(c))
            .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
    }

    /** Every value in the settings files. */
    public Stream<String> values() {
        return settings.values().stream().map(Setting::value);
    }

    int size() {
        return settings.size();
    }

    // Whether a value looks like one a person is meant to fill in, or a stand-in until one is: YOUR-..., CHANGE-ME,
    // ${...}, example.com, http://undefined/.
    static boolean isPlaceholder(final String value) {
        return value != null && PLACEHOLDER.matcher(value).matches();
    }

    /** A setting's address with its value from the settings files, where they hold one. */
    public Address withValue(final Address address) {
        if (!Address.SETTING.equals(address.getSource())) {
            return address.toBuilder().placeholder(address.isLiteral() && isPlaceholder(address.getValue())).build();
        }
        return get(address.getValue())
            .map(s -> address.toBuilder().settingValue(s.value()).settingFile(s.file()).settingLine(s.line())
                .placeholder(isPlaceholder(s.value())).build())
            .orElse(address);
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

    // Receives each setting a file holds.
    @FunctionalInterface
    interface Found {
        void setting(String key, String value, int line);
    }
}
