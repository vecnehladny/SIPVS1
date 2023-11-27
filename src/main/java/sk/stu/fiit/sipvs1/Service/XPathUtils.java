package sk.stu.fiit.sipvs1.Service;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;

public class XPathUtils {

    public static final Logger LOGGER = Logger.getLogger("XPathUtils");

    private static final Map<String, String> NS_MAP;

    static {
        NS_MAP = Map.of("ds", "http://www.w3.org/2000/09/xmldsig#", "xades", "http://www.ditec.sk/ep/signature_formats/xades_zep/v1.0");
    }

    private static final NamespaceContext NAMESPACE_CONTEXT = new NamespaceContext() {
        public String getNamespaceURI(String prefix) {
            return NS_MAP.get(prefix);
        }
        public Iterator getPrefixes(String val) {
            return null;
        }
        public String getPrefix(String uri) {
            return null;
        }
    };

    public static Node selectSingleNode(Document document, String xpathExpr) {
        return selectSingleNode(document.getDocumentElement(), xpathExpr);
    }

    public static Node selectSingleNode(Document document, String xpathExpr, Map<String, String> nsMap) {
        return selectSingleNode(document.getDocumentElement(), xpathExpr, nsMap);
    }

    public static Node selectSingleNode(Element element, String xpathExpr) {
        try {
            XPath xPath = XPathFactory.newInstance().newXPath();
            xPath.setNamespaceContext(NAMESPACE_CONTEXT);
            return (Node) xPath.compile(xpathExpr).evaluate(element, XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            LOGGER.log(Level.SEVERE, "", e);
        }
        return null;
    }

    public static Node selectSingleNode(Element element, String xpathExpr, Map<String, String> nsMap) {
        try {
            XPath xPath = XPathFactory.newInstance().newXPath();
            xPath.setNamespaceContext(new NamespaceContext() {
                @Override
                public String getNamespaceURI(String prefix) {
                    return nsMap.get(prefix);
                }

                @Override
                public String getPrefix(String namespaceURI) {
                    return null;
                }

                @Override
                public Iterator<String> getPrefixes(String namespaceURI) {
                    return null;
                }
            });
            return (Node) xPath.compile(xpathExpr).evaluate(element, XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            LOGGER.log(Level.SEVERE, "", e);
        }
        return null;
    }

    public static Element selectSingleElement(Document document, String xpathExpr) {
        return selectSingleElement(document.getDocumentElement(), xpathExpr);
    }

    public static Element selectSingleElement(Document document, String xpathExpr, Map<String, String> nsMap) {
        return selectSingleElement(document.getDocumentElement(), xpathExpr, nsMap);
    }

    public static Element selectSingleElement(Node node, String xpathExpr) {
        return selectSingleElement((Element) node, xpathExpr);
    }

    public static Element selectSingleElement(Node node, String xpathExpr, Map<String, String> nsMap) {
        return selectSingleElement((Element) node, xpathExpr, nsMap);
    }

    public static Element selectSingleElement(Element element, String xpathExpr) {
        try {
            XPath xPath = XPathFactory.newInstance().newXPath();
            xPath.setNamespaceContext(NAMESPACE_CONTEXT);
            return (Element) xPath.compile(xpathExpr).evaluate(element, XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            LOGGER.log(Level.SEVERE, "", e);
        }
        return null;
    }

    public static Element selectSingleElement(Element element, String xpathExpr, Map<String, String> nsMap) {
        try {
            XPath xPath = XPathFactory.newInstance().newXPath();
            xPath.setNamespaceContext(new NamespaceContext() {
                @Override
                public String getNamespaceURI(String prefix) {
                    return nsMap.get(prefix);
                }

                @Override
                public String getPrefix(String namespaceURI) {
                    return null;
                }

                @Override
                public Iterator<String> getPrefixes(String namespaceURI) {
                    return null;
                }
            });
            return (Element) xPath.compile(xpathExpr).evaluate(element, XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            LOGGER.log(Level.SEVERE, "", e);
        }
        return null;
    }

    public static List<Node> selectNodeList(Document document, String xpathExpr) {
        return selectNodeList(document.getDocumentElement(), xpathExpr);
    }

    public static List<Node> selectNodeList(Node node, String xpathExpr) {
        return selectNodeList((Element) node, xpathExpr);
    }

    public static List<Node> selectNodeList(Element element, String xpathExpr) {
        try {
            XPath xPath = XPathFactory.newInstance().newXPath();
            xPath.setNamespaceContext(NAMESPACE_CONTEXT);
            NodeList nodeList = (NodeList) xPath.compile(xpathExpr)
                                                .evaluate(element, XPathConstants.NODESET);
            return nodelistToList(nodeList);
        } catch (XPathExpressionException e) {
            LOGGER.log(Level.SEVERE, "", e);
        }
        return null;
    }

    private static List<Node> nodelistToList(NodeList nodelist) {
        return IntStream.range(0, nodelist.getLength())
                        .mapToObj(nodelist::item)
                        .toList();
    }
}
