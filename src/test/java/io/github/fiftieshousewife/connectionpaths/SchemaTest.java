package io.github.fiftieshousewife.connectionpaths;

import static org.assertj.core.api.Assertions.assertThat;

import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

// The facts under-test reads: the example repo's, checked against a copy of under-test's schema for them,
// src/test/resources/schema/external-calls.schema.json, taken from its schema/ folder.
class SchemaTest {

    @Test
    void writesFactsThatUnderTestsSchemaAccepts(@TempDir final Path dir) throws IOException {
        final Path out = dir.resolve("facts.json");
        Main.run(List.of("--module", "src/test/resources/example", "--out", out.toString()));
        final Schema schema = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12)
            .getSchema(Files.readString(Path.of("src/test/resources/schema/external-calls.schema.json")));
        assertThat(schema.validate(Files.readString(out), InputFormat.JSON)).isEmpty();
    }
}
