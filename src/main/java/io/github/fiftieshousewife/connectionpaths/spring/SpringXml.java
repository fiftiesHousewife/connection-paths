package io.github.fiftieshousewife.connectionpaths.spring;

import io.github.fiftieshousewife.connectionpaths.settings.XmlFiles;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

/**
 * Reads the beans a Spring XML file defines: each {@code <bean>}'s id, class, and the values of its properties, given
 * as {@code <property name="url" value="..."/>}, as a {@code <value>} inside the property, or as a {@code p:url}
 * attribute. Document types and outside entities are never read.
 */
@Slf4j
public final class SpringXml {

    // One bean definition, and where it is.
    @Value
    @Builder
    static class Bean {
        String id;
        String type;
        Map<String, String> properties;
        String file;
        int line;
    }

    private SpringXml() {
    }

    /** The beans in the files that are Spring bean definitions; other XML files hold none. */
    public static List<Bean> read(final List<Path> files) {
        final List<Bean> beans = new ArrayList<>();
        files.stream().filter(file -> file.toString().endsWith(".xml")).forEach(file -> beans.addAll(read(file)));
        return beans;
    }

    private static List<Bean> read(final Path file) {
        final BeanWalk walk = new BeanWalk(file.toString().replace('\\', '/'));
        try {
            XmlFiles.walk(file, walk);
            return walk.beans();
        } catch (IOException | XMLStreamException e) {
            log.debug("No beans read from {}: {}", file, e.getMessage());
            return List.of();
        }
    }
}
