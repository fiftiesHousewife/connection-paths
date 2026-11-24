package io.github.fiftieshousewife.connectionpaths.settings;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Reads the text of an XML file's innermost elements as settings, keyed by their element path with and without the
 * root element: Openfire's {@code <jive><database><serverURL>} is both {@code jive.database.serverURL} and
 * {@code database.serverURL}. Document types and outside entities are never read.
 */
public final class XmlSettings {

    private XmlSettings() {
    }

    static void read(final Path file, final SettingsFiles.Found found) throws IOException {
        try {
            XmlFiles.walk(file, new Walk(found));
        } catch (XMLStreamException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    // Tracks the element path, and reports an element's text when it has no child elements.
    private static final class Walk implements XmlFiles.Handler {
        private final SettingsFiles.Found found;
        private final Deque<String> path = new ArrayDeque<>();
        private final StringBuilder characters = new StringBuilder();
        private int line;
        private boolean hasChildren;

        Walk(final SettingsFiles.Found found) {
            this.found = found;
        }

        @Override
        public void start(final XMLStreamReader element) {
            path.addLast(element.getLocalName());
            characters.setLength(0);
            hasChildren = false;
            line = element.getLocation().getLineNumber();
        }

        @Override
        public void text(final String more) {
            characters.append(more);
        }

        @Override
        public void end(final String name) {
            final String value = characters.toString().strip();
            if (!hasChildren && !value.isEmpty()) {
                final String full = String.join(".", path);
                found.setting(full, value, line);
                if (path.size() > 1) {
                    found.setting(full.substring(full.indexOf('.') + 1), value, line);
                }
            }
            path.removeLast();
            characters.setLength(0);
            hasChildren = true;
        }
    }
}
