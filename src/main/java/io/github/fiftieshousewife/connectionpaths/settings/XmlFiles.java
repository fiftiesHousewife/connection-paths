package io.github.fiftieshousewife.connectionpaths.settings;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/** Walks an XML file's elements and text, never reading a document type or an outside entity. */
public final class XmlFiles {

    /** What a walk through an XML file is told, element by element. */
    public interface Handler {

        /** An element starts; the reader is at it, so its name, attributes and line can be read. */
        void start(XMLStreamReader element);

        /** Some of the text inside the current element. */
        void text(String text);

        /** The element with this name ends. */
        void end(String name);
    }

    private XmlFiles() {
    }

    /** Walks the file, telling the handler each element's start, text and end. */
    public static void walk(final Path file, final Handler handler) throws IOException, XMLStreamException {
        final XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        try (InputStream in = Files.newInputStream(file)) {
            final XMLStreamReader reader = factory.createXMLStreamReader(in);
            try {
                while (reader.hasNext()) {
                    tell(handler, reader.next(), reader);
                }
            } finally {
                reader.close();
            }
        }
    }

    private static void tell(final Handler handler, final int event, final XMLStreamReader reader) {
        switch (event) {
            case XMLStreamConstants.START_ELEMENT -> handler.start(reader);
            case XMLStreamConstants.CHARACTERS, XMLStreamConstants.CDATA -> handler.text(reader.getText());
            case XMLStreamConstants.END_ELEMENT -> handler.end(reader.getLocalName());
            default -> {
                // Comments, processing instructions and the document's start and end say nothing about settings.
            }
        }
    }
}
