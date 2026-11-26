package io.github.fiftieshousewife.connectionpaths.settings;

import io.github.fiftieshousewife.connectionpaths.address.SettingAnnotations;
import io.github.fiftieshousewife.connectionpaths.address.Address;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * The settings in a repo's properties, YAML and XML files, by dotted key. YAML and XML are flattened to the path of
 * their keys or elements: {@code <database><serverURL>} is {@code database.serverURL}, with and without the root
 * element. Where a key is in more than one file, the value kept is from production settings before test ones, then
 * a file without a profile ({@code application.yml}) before one with ({@code application-dev.yml}), then file-name
 * order. Keys are matched loosely, as Spring matches them: {@code jdbcUrl}, {@code jdbc-url} and {@code JDBC_URL}
 * are one key. A test's Spring profiles' files ({@code application-dev.yml}) are left out, but for a test that
 * activates their profile, whose settings come before the others.
 */
public final class SettingsFiles {

    /** A setting's value, and where it is. */
    record Setting(String value, String file, int line) {
    }

    // The folders the files were found in, which hold the profiles' files too, and the embedded database Spring Boot
    // would start from the classpath.
    private record Origin(List<Path> folders, Optional<Setting> embeddedDatabase) {
    }

    private final Map<String, Setting> settings = new TreeMap<>();
    private final Map<String, Setting> loosely = new TreeMap<>();
    private final Optional<SettingsFiles> testClasspath;
    private final Optional<SettingsFiles> under;
    private final Origin origin;

    private SettingsFiles(final List<Path> files, final Optional<SettingsFiles> testClasspath, final Origin origin) {
        this.testClasspath = testClasspath;
        this.under = Optional.empty();
        this.origin = origin;
        files.forEach(file -> SettingsFileReader.read(file, this::add));
    }

    // Profiles' files, whose settings come before those under them.
    private SettingsFiles(final List<Path> files, final SettingsFiles under) {
        this.testClasspath = Optional.empty();
        this.under = Optional.of(under);
        this.origin = under.origin;
        files.forEach(file -> SettingsFileReader.read(file, this::add));
    }

    /** The settings in the folders' files; and, apart, those on the test classpath, test resources first. */
    public static SettingsFiles read(final List<Path> folders) {
        return read(folders, List.of());
    }

    /**
     * The settings in the folders' files, and those on the test classpath; where neither names a Spring Boot data
     * source, the embedded database Spring Boot would start from the classpath, if it has one.
     */
    public static SettingsFiles read(final List<Path> folders, final List<Path> classpath) {
        final Origin origin = new Origin(List.copyOf(folders), EmbeddedDatabases.on(classpath));
        return new SettingsFiles(filesIn(folders), Optional.of(
            new SettingsFiles(SettingsFileFinder.testClasspathFilesIn(folders), Optional.empty(), origin)), origin);
    }

    /**
     * These settings, with those of the Spring profiles' files on the test classpath before them, the last profile's
     * first, as Spring takes them.
     */
    public SettingsFiles forProfiles(final List<String> profiles) {
        final List<Path> files = SettingsFileFinder.profileFilesIn(origin.folders(), profiles);
        return files.isEmpty() ? this : new SettingsFiles(files, this);
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
        return Optional.ofNullable(settings.get(key)).or(() -> Optional.ofNullable(loosely.get(loose(key))))
            .or(() -> under.map(files -> files.get(key)).orElseGet(() -> origin.embeddedDatabase()
                .filter(e -> loose(key).equals(loose(Address.DATA_SOURCE_URL)))));
    }

    // A key as Spring compares keys: without dashes, with underscores as dots, in lower case.
    private static String loose(final String key) {
        return key.chars().filter(c -> c != '-').map(c -> c == '_' ? '.' : Character.toLowerCase(c))
            .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
    }

    /** Every value in the settings files. */
    public Stream<String> values() {
        return Stream.concat(settings.values().stream().map(Setting::value),
            under.stream().flatMap(SettingsFiles::values));
    }

    /** Whether the files hold any settings at all. */
    public boolean isEmpty() {
        return settings.isEmpty() && under.map(SettingsFiles::isEmpty).orElse(true);
    }

    /** The files the settings come from, as the facts name files. */
    public List<String> files() {
        return Stream.concat(settings.values().stream().map(Setting::file),
            under.stream().flatMap(u -> u.files().stream())).distinct().sorted().toList();
    }

    // Whether a value looks like one a person is meant to fill in, or a stand-in until one is: YOUR-..., CHANGE-ME,
    // ${...}, example.com, http://undefined/.
    static boolean isPlaceholder(final String value) {
        return value != null && Address.looksLikeAPlaceholder(value);
    }

    /** A setting's address with its value from the settings files, where they hold one. */
    public Address withValue(final Address address) {
        if (!Address.SETTING.equals(address.getSource())) {
            return address.toBuilder().placeholder(address.isLiteral() && isPlaceholder(address.getValue())).build();
        }
        return get(address.getValue())
            .map(s -> address.toBuilder().settingValue(resolved(s.value())).settingFile(s.file()).settingLine(s.line())
                .placeholder(isPlaceholder(resolved(s.value()))).build())
            .orElseGet(() -> address.toBuilder()
                .placeholder(address.getSettingValue() != null && isPlaceholder(address.getSettingValue())).build());
    }

    // A value that refers to another, ${DB_URL:jdbc:h2:mem:orders}, is its default where it has one: what runs when
    // nothing sets the variable. Without one, it stays as it is, known only at run time.
    private static String resolved(final String value) {
        return SettingAnnotations.defaultOf(value.strip()).orElse(value);
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
