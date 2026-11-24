package io.github.fiftieshousewife.connectionpaths.settings;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.dataformat.yaml.YAMLMapper;

// Reads a YAML file with a YAML parser, and reports each scalar value of its mappings by its path, with the line it is
// on: spring: datasource: url: x is spring.datasource.url. Lists are left out. A file of several documents is read
// one document after another.
@Slf4j
final class YamlWalk {

    private static final YAMLMapper YAML = new YAMLMapper();

    private final Deque<String> path = new ArrayDeque<>();
    private final JsonParser parser;
    private final SettingsFiles.Found found;

    private YamlWalk(final JsonParser parser, final SettingsFiles.Found found) {
        this.parser = parser;
        this.found = found;
    }

    static void read(final String text, final SettingsFiles.Found found) {
        try (JsonParser parser = YAML.createParser(text)) {
            final YamlWalk walk = new YamlWalk(parser, found);
            for (JsonToken token = parser.nextToken(); token != null; token = parser.nextToken()) {
                walk.step(token);
            }
        } catch (JacksonException e) {
            log.warn("YAML settings read only in part: {}", e.getOriginalMessage());
        }
    }

    private void step(final JsonToken token) {
        if (token == JsonToken.START_OBJECT) {
            path.push(Objects.requireNonNullElse(parser.currentName(), ""));
        } else if (token == JsonToken.END_OBJECT) {
            path.pop();
        } else if (token == JsonToken.START_ARRAY) {
            parser.skipChildren();
        } else if (isValue(token)) {
            found.setting(keyOf(parser.currentName()), parser.getString(), parser.currentTokenLocation().getLineNr());
        }
    }

    private static boolean isValue(final JsonToken token) {
        return token.isScalarValue() && token != JsonToken.VALUE_NULL;
    }

    // The keys of the open mappings, outermost first, then this one's.
    private String keyOf(final String name) {
        final List<String> keys = new ArrayList<>(path);
        Collections.reverse(keys);
        keys.add(name);
        return keys.stream().filter(key -> !key.isEmpty()).collect(Collectors.joining("."));
    }
}
