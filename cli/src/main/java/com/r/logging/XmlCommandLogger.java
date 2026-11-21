package com.r.logging;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class XmlCommandLogger {

    private static final long EMPTY_FILE_SIZE = 0L;

    private final Path logPath;
    private final String username;

    public XmlCommandLogger(Path logPath) {
        this.logPath = logPath;
        this.username = System.getProperty("user.name");
    }

    public synchronized void log(
            String command,
            List<String> arguments
    ) throws IOException {
        try {
            ensureParentDirectoryExists();
            DocumentBuilderFactory factory = createDocumentBuilderFactory();
            Document document = loadOrCreateDocument(factory);
            appendEvent(document, command, arguments);
            writeDocument(document);
        } catch (Exception exception) {
            throw new IOException(
                    "Failed to write XML log: " + exception.getMessage(),
                    exception
            );
        }
    }

    private void ensureParentDirectoryExists() throws IOException {
        Path parent = logPath.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private DocumentBuilderFactory createDocumentBuilderFactory()
            throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(
                "http://apache.org/xml/features/disallow-doctype-decl",
                true
        );
        factory.setFeature(
                "http://xml.org/sax/features/external-general-entities",
                false
        );
        factory.setFeature(
                "http://xml.org/sax/features/external-parameter-entities",
                false
        );
        return factory;
    }

    private Document loadOrCreateDocument(DocumentBuilderFactory factory)
            throws Exception {
        if (Files.exists(logPath) && Files.size(logPath) > EMPTY_FILE_SIZE) {
            return factory.newDocumentBuilder().parse(logPath.toFile());
        }
        Document document = factory.newDocumentBuilder().newDocument();
        Element root = document.createElement("events");
        document.appendChild(root);
        return document;
    }

    private void appendEvent(
            Document document,
            String command,
            List<String> arguments
    ) {
        Element event = document.createElement("event");
        event.setAttribute(
                "timestamp",
                OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        );
        event.setAttribute("username", username);

        Element commandElement = document.createElement("command");
        commandElement.setTextContent(command);
        event.appendChild(commandElement);

        Element argumentsElement = document.createElement("arguments");
        for (String argument : arguments) {
            Element argumentElement = document.createElement("argument");
            argumentElement.setTextContent(argument);
            argumentsElement.appendChild(argumentElement);
        }
        event.appendChild(argumentsElement);
        document.getDocumentElement().appendChild(event);
    }

    private void writeDocument(Document document) throws TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setFeature(
                XMLConstants.FEATURE_SECURE_PROCESSING,
                true
        );
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.transform(
                new DOMSource(document),
                new StreamResult(logPath.toFile())
        );
    }
}