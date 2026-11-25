package io.github.fiftieshousewife.connectionpaths;

import io.github.fiftieshousewife.connectionpaths.facts.Facts;
import io.github.fiftieshousewife.connectionpaths.facts.FactsBuilder;
import io.github.fiftieshousewife.connectionpaths.graph.CallGraph;
import io.github.fiftieshousewife.connectionpaths.rules.ConnectionRules;
import io.github.fiftieshousewife.connectionpaths.settings.ServiceFiles;
import io.github.fiftieshousewife.connectionpaths.settings.SettingsFiles;
import io.github.fiftieshousewife.connectionpaths.source.SourceModel;
import io.github.fiftieshousewife.connectionpaths.source.SourceReader;
import io.github.fiftieshousewife.connectionpaths.source.TypeSolvers;
import io.github.fiftieshousewife.connectionpaths.spring.SpringBeans;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/** Reads the source, builds the call graph, and returns or writes the facts. */
public final class ConnectionPaths {

    private static final JsonMapper JSON = JsonMapper.builder()
        .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
        .enable(SerializationFeature.INDENT_OUTPUT)
        .build();

    private ConnectionPaths() {
    }

    /** Finds the connections and the tests that reach them. */
    public static Facts find(final Options options) {
        final TypeSolvers.Setup setup = TypeSolvers.create(options);
        final ConnectionRules rules = ConnectionRules.load(options.extraRules());
        final SourceModel model =
            new SourceReader(setup.solver(), rules).read(options.getMainSources(), options.getTestSources());
        // Beans defined in Spring XML open connections the Java source never shows.
        new SpringBeans(setup.solver(), rules).addTo(model, SettingsFiles.filesIn(options.getSettings()));
        final SettingsFiles settings = SettingsFiles.read(options.getSettings(), options.getClasspath());
        // A class named in a setting or a service file may be created by name.
        settings.values().forEach(model::created);
        ServiceFiles.namedIn(options.getSettings()).forEach(model::created);
        final CallGraph graph = new CallGraph(model, options.getMaxImplementations());
        return new FactsBuilder(model, graph, settings).build(version(), setup);
    }

    static void write(final Facts facts, final Path out) {
        try {
            final Path parent = out.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (OutputStream stream = Files.newOutputStream(out)) {
                JSON.writeValue(stream, facts);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Can't write " + out, e);
        }
    }

    static String toJson(final Facts facts) {
        return JSON.writeValueAsString(facts);
    }

    public static String version() {
        final String fromJar = ConnectionPaths.class.getPackage().getImplementationVersion();
        return fromJar == null ? "development" : fromJar;
    }
}
