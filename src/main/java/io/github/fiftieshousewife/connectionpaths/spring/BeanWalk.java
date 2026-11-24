package io.github.fiftieshousewife.connectionpaths.spring;

import io.github.fiftieshousewife.connectionpaths.settings.XmlFiles;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.IntStream;
import javax.xml.stream.XMLStreamReader;

// Collects the beans in a Spring XML file, with a bean inside a property kept apart from the one around it. A file
// whose root element isn't <beans> holds none.
final class BeanWalk implements XmlFiles.Handler {

    private static final String P_NAMESPACE = "http://www.springframework.org/schema/p";

    private final String file;
    private final List<SpringXml.Bean> found = new ArrayList<>();
    private final Deque<SpringXml.Bean.BeanBuilder> open = new ArrayDeque<>();
    private final Deque<Map<String, String>> properties = new ArrayDeque<>();
    private final StringBuilder characters = new StringBuilder();
    private String root;
    private String property;

    BeanWalk(final String file) {
        this.file = file;
    }

    List<SpringXml.Bean> beans() {
        return "beans".equals(root) ? found : List.of();
    }

    @Override
    public void start(final XMLStreamReader element) {
        characters.setLength(0);
        final String name = element.getLocalName();
        root = root == null ? name : root;
        if ("bean".equals(name)) {
            open.push(bean(element));
        } else if ("property".equals(name) && !open.isEmpty()) {
            property = element.getAttributeValue(null, "name");
            putProperty(element.getAttributeValue(null, "value"));
        }
    }

    @Override
    public void text(final String more) {
        characters.append(more);
    }

    @Override
    public void end(final String name) {
        if ("value".equals(name)) {
            putProperty(characters.toString().strip());
        } else if ("property".equals(name)) {
            property = null;
        } else if ("bean".equals(name) && !open.isEmpty()) {
            found.add(open.pop().properties(Map.copyOf(properties.pop())).build());
        }
        characters.setLength(0);
    }

    private void putProperty(final String value) {
        if (property != null && value != null && !properties.isEmpty()) {
            properties.peek().put(property, value);
        }
    }

    private SpringXml.Bean.BeanBuilder bean(final XMLStreamReader element) {
        final String id = element.getAttributeValue(null, "id");
        final Map<String, String> fromAttributes = new TreeMap<>();
        IntStream.range(0, element.getAttributeCount())
            .filter(i -> P_NAMESPACE.equals(element.getAttributeNamespace(i)))
            .forEach(i -> fromAttributes.put(element.getAttributeLocalName(i), element.getAttributeValue(i)));
        properties.push(fromAttributes);
        return SpringXml.Bean.builder()
            .id(id == null ? element.getAttributeValue(null, "name") : id)
            .type(element.getAttributeValue(null, "class"))
            .file(file)
            .line(element.getLocation().getLineNumber());
    }
}
