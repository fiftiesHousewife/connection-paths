package io.github.fiftieshousewife.connectionpaths;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

/**
 * Reads the beans a Spring XML file defines: each {@code <bean>}'s id, class, and the values of its properties, given
 * as {@code <property name="url" value="..."/>}, as a {@code <value>} inside the property, or as a {@code p:url}
 * attribute. Document types and outside entities are never read.
 */
@Slf4j
final class SpringXml {

    private static final String P_NAMESPACE = "http://www.springframework.org/schema/p";

    /** One bean definition, and where it is. */
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
    static List<Bean> read(final List<Path> files) {
        final List<Bean> beans = new ArrayList<>();
        files.stream().filter(file -> file.toString().endsWith(".xml")).forEach(file -> beans.addAll(read(file)));
        return beans;
    }

    private static List<Bean> read(final Path file) {
        final XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        try (InputStream in = Files.newInputStream(file)) {
            final XMLStreamReader reader = factory.createXMLStreamReader(in);
            try {
                return new Walk(file.toString().replace('\\', '/')).through(reader);
            } finally {
                reader.close();
            }
        } catch (IOException | XMLStreamException e) {
            log.debug("No beans read from {}: {}", file, e.getMessage());
            return List.of();
        }
    }

    /** Collects beans, with a bean inside a property kept apart from the one around it. */
    private static final class Walk {
        private final String file;
        private final List<Bean> beans = new ArrayList<>();
        private final Deque<Bean.BeanBuilder> open = new ArrayDeque<>();
        private final Deque<Map<String, String>> properties = new ArrayDeque<>();
        private final StringBuilder text = new StringBuilder();
        private String property;

        Walk(final String file) {
            this.file = file;
        }

        List<Bean> through(final XMLStreamReader reader) throws XMLStreamException {
            reader.nextTag();
            if (!"beans".equals(reader.getLocalName())) {
                return List.of();
            }
            while (reader.hasNext()) {
                final int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    start(reader);
                } else if (event == XMLStreamConstants.CHARACTERS || event == XMLStreamConstants.CDATA) {
                    text.append(reader.getText());
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    end(reader.getLocalName());
                }
            }
            return beans;
        }

        private void start(final XMLStreamReader reader) {
            text.setLength(0);
            final String name = reader.getLocalName();
            if ("bean".equals(name)) {
                open.push(bean(reader));
            } else if ("property".equals(name) && !open.isEmpty()) {
                property = reader.getAttributeValue(null, "name");
                final String value = reader.getAttributeValue(null, "value");
                if (property != null && value != null) {
                    properties.peek().put(property, value);
                }
            }
        }

        private Bean.BeanBuilder bean(final XMLStreamReader reader) {
            final String id = reader.getAttributeValue(null, "id");
            final Bean.BeanBuilder bean = Bean.builder()
                .id(id == null ? reader.getAttributeValue(null, "name") : id)
                .type(reader.getAttributeValue(null, "class"))
                .file(file)
                .line(reader.getLocation().getLineNumber());
            final Map<String, String> fromAttributes = new TreeMap<>();
            for (int i = 0; i < reader.getAttributeCount(); i++) {
                if (P_NAMESPACE.equals(reader.getAttributeNamespace(i))) {
                    fromAttributes.put(reader.getAttributeLocalName(i), reader.getAttributeValue(i));
                }
            }
            properties.push(fromAttributes);
            return bean;
        }

        private void end(final String name) {
            if ("value".equals(name) && property != null && !open.isEmpty()) {
                properties.peek().put(property, text.toString().strip());
            } else if ("property".equals(name)) {
                property = null;
            } else if ("bean".equals(name) && !open.isEmpty()) {
                beans.add(open.pop().properties(Map.copyOf(properties.pop())).build());
            }
            text.setLength(0);
        }
    }
}
