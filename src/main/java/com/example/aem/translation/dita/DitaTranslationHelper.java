package com.example.aem.translation.dita;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;

public class DitaTranslationHelper {

    private static final Set<String> TRANSLATABLE_ELEMENTS = new HashSet<>(Arrays.asList(
        "title", "shortdesc", "p", "li", "dd", "dt", "th", "td",
        "figcap", "linktext", "searchtitle", "alt", "note",
        "taskcmd", "taskstep", "taskinfo", "choice", "choption",
        "chdescr", "choptionval", "chdesc", "stepxmp", "stepresult",
        "prolog", "metadata", "data", "ddhd", "dthd", "linkinfo"
    ));

    private static final Set<String> PRESERVE_STRUCTURE_ELEMENTS = new HashSet<>(Arrays.asList(
        "topic", "task", "concept", "reference", "article",
        "section", "example", "ul", "ol", "dl", "table", "tbody",
        "thead", "tr", "simpletable", "sthead", "strow",
        "fig", "image", "coderef", "xref", "link", "map", "topicref"
    ));

    public static class DitaParseResult {
        private final Document document;
        private final List<DitaElement> translatableElements;
        private final Map<String, String> idToOriginal;
        private final String ditaType;

        public DitaParseResult(Document document, List<DitaElement> translatableElements, 
                Map<String, String> idToOriginal, String ditaType) {
            this.document = document;
            this.translatableElements = translatableElements;
            this.idToOriginal = idToOriginal;
            this.ditaType = ditaType;
        }

        public Document getDocument() { return document; }
        public List<DitaElement> getTranslatableElements() { return translatableElements; }
        public Map<String, String> getIdToOriginal() { return idToOriginal; }
        public String getDitaType() { return ditaType; }
    }

    public static class DitaElement {
        private final String id;
        private final String elementName;
        private final String content;
        private final String path;
        private boolean translated;
        private String translatedContent;

        public DitaElement(String id, String elementName, String content, String path) {
            this.id = id;
            this.elementName = elementName;
            this.content = content;
            this.path = path;
            this.translated = false;
        }

        public String getId() { return id; }
        public String getElementName() { return elementName; }
        public String getContent() { return content; }
        public String getPath() { return path; }
        public boolean isTranslated() { return translated; }
        public void setTranslated(boolean translated) { this.translated = translated; }
        public String getTranslatedContent() { return translatedContent; }
        public void setTranslatedContent(String translatedContent) { 
            this.translatedContent = translatedContent; 
            this.translated = true;
        }
    }

    public static DitaParseResult parseDitaContent(String ditaXml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        
        Document document = builder.parse(new InputSource(new StringReader(ditaXml)));
        
        List<DitaElement> translatableElements = new ArrayList<>();
        Map<String, String> idToOriginal = new HashMap<>();
        
        String ditaType = detectDitaType(document);
        extractTranslatableElements(document.getDocumentElement(), "", translatableElements, idToOriginal);
        
        return new DitaParseResult(document, translatableElements, idToOriginal, ditaType);
    }

    private static String detectDitaType(Document document) {
        Element root = document.getDocumentElement();
        String rootName = root.getLocalName();
        
        if ("topic".equals(rootName)) return "topic";
        if ("task".equals(rootName)) return "task";
        if ("concept".equals(rootName)) return "concept";
        if ("reference".equals(rootName)) return "reference";
        if ("map".equals(rootName)) return "map";
        
        return "unknown";
    }

    private static void extractTranslatableElements(Element element, String path, 
            List<DitaElement> elements, Map<String, String> idToOriginal) {
        
        String currentPath = path + "/" + element.getLocalName();
        String id = element.getAttribute("id");
        
        if (TRANSLATABLE_ELEMENTS.contains(element.getLocalName())) {
            String content = getTextContent(element);
            if (content != null && !content.trim().isEmpty()) {
                String elementId = id != null && !id.isEmpty() ? id : UUID.randomUUID().toString();
                elements.add(new DitaElement(elementId, element.getLocalName(), content, currentPath));
                idToOriginal.put(elementId, content);
            }
        }
        
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                extractTranslatableElements((Element) child, currentPath, elements, idToOriginal);
            }
        }
    }

    private static String getTextContent(Element element) {
        StringBuilder sb = new StringBuilder();
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.TEXT_NODE) {
                sb.append(child.getTextContent());
            }
        }
        return sb.toString().trim();
    }

    public static void applyTranslations(DitaParseResult parseResult, 
            Map<String, String> translations) throws Exception {
        
        applyTranslationsToElement(parseResult.getDocument().getDocumentElement(), 
                translations, parseResult.getTranslatableElements());
    }

    private static void applyTranslationsToElement(Element element, 
            Map<String, String> translations, List<DitaElement> ditaElements) {
        
        String id = element.getAttribute("id");
        
        if (TRANSLATABLE_ELEMENTS.contains(element.getLocalName()) && id != null && !id.isEmpty()) {
            String translatedText = translations.get(id);
            if (translatedText != null) {
                element.setTextContent(translatedText);
            }
        }
        
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                applyTranslationsToElement((Element) child, translations, ditaElements);
            }
        }
    }

    public static String documentToString(Document document) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        
        DOMSource source = new DOMSource(document);
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        transformer.transform(source, result);
        
        return writer.toString();
    }

    public static List<String> extractKeywords(Document document) throws Exception {
        List<String> keywords = new ArrayList<>();
        XPath xpath = XPathFactory.newInstance().newXPath();
        
        NodeList keywordNodes = (NodeList) xpath.compile("//keyword").evaluate(document, XPathConstants.NODESET);
        for (int i = 0; i < keywordNodes.getLength(); i++) {
            keywords.add(keywordNodes.item(i).getTextContent());
        }
        
        return keywords;
    }

    public static String generateTranslationBatch(List<DitaElement> elements, String sourceLang, String targetLang) {
        StringBuilder batch = new StringBuilder();
        batch.append("Translate the following DITA content from ").append(sourceLang).append(" to ").append(targetLang).append(":\n\n");
        
        for (DitaElement element : elements) {
            batch.append("[ID: ").append(element.getId()).append("]\n");
            batch.append(element.getContent()).append("\n\n");
        }
        
        return batch.toString();
    }

    public static Map<String, String> parseTranslationResponse(String response) {
        Map<String, String> translations = new HashMap<>();
        
        String[] lines = response.split("\n");
        String currentId = null;
        StringBuilder currentContent = new StringBuilder();
        
        for (String line : lines) {
            if (line.startsWith("[ID:")) {
                if (currentId != null && currentContent.length() > 0) {
                    translations.put(currentId, currentContent.toString().trim());
                }
                currentId = line.substring(4, line.indexOf("]"));
                currentContent = new StringBuilder();
            } else if (currentId != null) {
                currentContent.append(line).append("\n");
            }
        }
        
        if (currentId != null && currentContent.length() > 0) {
            translations.put(currentId, currentContent.toString().trim());
        }
        
        return translations;
    }
}
