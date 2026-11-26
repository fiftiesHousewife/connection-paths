package io.github.fiftieshousewife.connectionpaths.settings;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BiConsumer;
import lombok.extern.slf4j.Slf4j;

// The settings in one properties, YAML or XML file, by key; a file that can't be read is warned about and skipped.
@Slf4j
final class SettingsFileReader {

    private SettingsFileReader() {
    }

    static void read(final Path file, final BiConsumer<String, SettingsFiles.Setting> add) {
        final String name = file.toString().replace('\\', '/');
        final SettingsFiles.Found found =
            (key, value, line) -> add.accept(key, new SettingsFiles.Setting(value, name, line));
        try {
            if (name.endsWith(".xml")) {
                XmlSettings.read(file, found);
                return;
            }
            final List<String> lines = Files.readAllLines(file, StandardCharsets.ISO_8859_1);
            if (name.endsWith(".properties")) {
                TextSettings.properties(lines, found);
            } else {
                TextSettings.yaml(lines, found);
            }
        } catch (IOException | IllegalArgumentException e) {
            log.warn("Settings not read from {}: {}", name, e.getMessage());
        }
    }
}
