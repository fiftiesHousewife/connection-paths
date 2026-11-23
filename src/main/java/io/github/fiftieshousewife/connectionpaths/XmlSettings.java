package io.github.fiftieshousewife.connectionpaths;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Reads the text of an XML file's innermost elements as settings, keyed by their element path with and without the
 * root element: Openfire's {@code <jive><database><serverURL>} is both {@code jive.database.serverURL} and
 * {@code database.serverURL}. Document types and outside entities are never read.
 */
final class XmlSettings {

    private XmlSettings() {
    }

    static void read(final Path file, final SettingsFiles.Found found) throws IOException {
        final XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        try (InputStream in = Files.newInputStream(file)) {
            final XMLStreamReader reader = factory.createXMLStreamReader(in);
            try {
                new Walk(found).through(reader);
            } finally {
                reader.close();
            }
        } catch (XMLStreamException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    /** Tracks the element path, and reports an element's text when it has no child elements. */
    private static final class Walk {
        private final SettingsFiles.Found found;
        private final Deque<String> path = new ArrayDeque<>();
        private final StringBuilder text = new StringBuilder();
        private int line;
        private boolean hasChildren;

        Walk(final SettingsFiles.Found found) {
            this.found = found;
        }

        void through(final XMLStreamReader reader) throws XMLStreamException {
            while (reader.hasNext()) {
                final int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    start(reader.getLocalName(), reader.getLocation().getLineNumber());
                } else if (event == XMLStreamConstants.CHARACTERS || event == XMLStreamConstants.CDATA) {
                    text.append(reader.getText());
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    end();
                }
            }
        }

        private void start(final String name, final int at) {
            path.addLast(name);
            text.setLength(0);
            hasChildren = false;
            line = at;
        }

        private void end() {
            final String value = text.toString().strip();
            if (!hasChildren && !value.isEmpty()) {
                final String full = String.join(".", path);
                found.setting(full, value, line);
                if (path.size() > 1) {
                    found.setting(full.substring(full.indexOf('.') + 1), value, line);
                }
            }
            path.removeLast();
            text.setLength(0);
            hasChildren = true;
        }
    }
}
