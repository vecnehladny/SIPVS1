package sk.stu.fiit.sipvs1.Service.validators;

import org.apache.xml.security.c14n.CanonicalizationException;
import org.apache.xml.security.c14n.Canonicalizer;
import org.apache.xml.security.c14n.InvalidCanonicalizerException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.provider.X509CertificateObject;
import org.bouncycastle.util.encoders.Base64;
import org.springframework.stereotype.Service;
import org.thymeleaf.util.ListUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import sk.stu.fiit.sipvs1.Model.InvalidDocumentException;
import sk.stu.fiit.sipvs1.Service.ValidatorConstants;
import sk.stu.fiit.sipvs1.Service.ValidatorUtils;
import sk.stu.fiit.sipvs1.Service.XPathUtils;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringWriter;
import java.security.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class XMLSignatureValidator {

    private final Logger LOGGER = Logger.getLogger("XMLSignatureValidator");
    public static final String PREFIX_ER = "XML SIGNATURE | ERROR : ";
    public static final String PREFIX_OK = "XML SIGNATURE | OK : ";


    public void verify(Document document) throws InvalidDocumentException {

        org.apache.xml.security.Init.init();
        Security.addProvider(new BouncyCastleProvider());

        verifySignatureMethodAndCanonicalizationMethod(document);
        LOGGER.info(PREFIX_OK + "ds:SignatureMethod and ds:CanonicalizationMethod");

        verifyTransformsAndDigestMethod(document);
        LOGGER.info(PREFIX_OK + "ds:Transforms and ds:DigestMethod");

        coreValidation1(document);
        LOGGER.info(PREFIX_OK + "Core Validation - ds:Manifest and ds:DigestValue");

        coreValidation2(document);
        LOGGER.info(PREFIX_OK + "Core Validation - ds:SignedInfo and ds:SignatureValue");

        verifySignature(document);
        LOGGER.info(PREFIX_OK + "ds:Signature ID and namespace xmlns:ds");

        verifySignatureValueId(document);
        LOGGER.info(PREFIX_OK + "ds:SignatureValue ID");

        verifySignedInfoReferencesAndAttributeValues(document);
        LOGGER.info(PREFIX_OK + "References, ID types ds:SignedInfo");

        verifyKeyInfoContent(document);
        LOGGER.info(PREFIX_OK + "Content ds:KeyInfo");

        verifySignaturePropertiesContent(document);
        LOGGER.info(PREFIX_OK + "Content ds:SignatureProperties");

        checkReferenceDSManifest(document);
        LOGGER.info(PREFIX_OK + "Elements ds:Manifest");

        verifyManifestDigestValue(document);
        LOGGER.info(PREFIX_OK + "References ds:Manifest");

    }


    // Assertion ds:SignatureMethod and ds:CanonicalizationMethod URI
    private void verifySignatureMethodAndCanonicalizationMethod(Document document) throws InvalidDocumentException {

        Element signatureMethod = XPathUtils.selectSingleElement(document,"//ds:Signature/ds:SignedInfo/ds:SignatureMethod");

        if(null == signatureMethod) {
            throw new InvalidDocumentException(PREFIX_ER + "element ds:Signature/ds:SignedInfo/ds:SignatureMethod not found");
        }

        //Assertion ds:SignatureMethod
        if (!ValidatorUtils.checkAttributeValue(signatureMethod, "Algorithm", ValidatorConstants.SIGNATURE_METHODS)) {
            throw new InvalidDocumentException(PREFIX_ER + "Attribute algorithm of element ds:SignatureMethod does not contain a URI for any of the supported algorithms");
        }

        Element canonicalizationMethod =  XPathUtils.selectSingleElement(document, "//ds:Signature/ds:SignedInfo/ds:CanonicalizationMethod");

        if(null == canonicalizationMethod) {
            throw new InvalidDocumentException(PREFIX_ER + "element ds:Signature/ds:SignedInfo/ds:CanonicalizationMethod not found");
        }

        //Assertion ds:CanonicalizationMethod
        if (!ValidatorUtils.checkAttributeValue(canonicalizationMethod, "Algorithm", ValidatorConstants.CANONICALIZATION_METHODS)) {
            throw new InvalidDocumentException(PREFIX_ER + "Attribute algorithm of element ds:CanonicalizationMethod does not contain a URI for any of the supported algorithms");
        }

    }

    // Assertion ds:Transforms and ds:DigestMethod elements URI
    private void verifyTransformsAndDigestMethod(Document document) throws InvalidDocumentException {

        List<Node> transformsElements = XPathUtils.selectNodeList(document, "//ds:Signature/ds:SignedInfo/ds:Reference/ds:Transforms");

        if (ListUtils.isEmpty(transformsElements)) {
            throw new InvalidDocumentException(PREFIX_ER + "element ds:Signature/ds:SignedInfo/ds:Reference/ds:Transforms not found");
        }

        for (Node element : transformsElements) {
            Element transformsElement = (Element) element;
            Element transformElement = (Element) transformsElement.getElementsByTagName("ds:Transform")
                                                                  .item(0);

            // Assertion ds:Transforms Element
            if (!ValidatorUtils.checkAttributeValue(transformElement, "Algorithm", ValidatorConstants.TRANSFORM_METHODS)) {
                throw new InvalidDocumentException(PREFIX_ER + "Attribute algorithm of element ds:Transforms does not contain a URI for any of the supported algorithms");
            }
        }


        List<Node> digestMethodElements = XPathUtils.selectNodeList(document, "//ds:Signature/ds:SignedInfo/ds:Reference/ds:DigestMethod");

        if (ListUtils.isEmpty(digestMethodElements)) {
            throw new InvalidDocumentException(PREFIX_ER + "element ds:Signature/ds:SignedInfo/ds:Reference/ds:DigestMethod not found");
        }

        for (Node methodElement : digestMethodElements) {
            Element digestMethodElement = (Element) methodElement;

            // Assertion ds:DigestMethod Element
            if (!ValidatorUtils.checkAttributeValue(digestMethodElement, "Algorithm", ValidatorConstants.DIGEST_METHODS)) {
                throw new InvalidDocumentException(PREFIX_ER + "Attribute algorithm of element ds:DigestMethod does not contain a URI for any of the supported algorithms");
            }
        }

    }


    // Bod 4.2.2. - must have Id attribute and specific amespace xmlns:ds
    private void verifySignature(Document document) throws InvalidDocumentException {

        Element signatureElement = (Element) document.getElementsByTagName("ds:Signature")
                                                     .item(0);

        if (signatureElement == null) {
            throw new InvalidDocumentException(PREFIX_ER + "element ds:Signature not found");
        }

        if (!signatureElement.hasAttribute("Id")) {
            throw new InvalidDocumentException(PREFIX_ER + "Element ds:Signature does not have attribute Id");
        }

        if (!ValidatorUtils.checkAttributeValue(signatureElement, "Id")) {
            throw new InvalidDocumentException(PREFIX_ER + "Attribute Id of element ds:Signature does not have any value");
        }

        if (!ValidatorUtils.checkAttributeValue(signatureElement, "xmlns:ds", "http://www.w3.org/2000/09/xmldsig#")) {
            throw new InvalidDocumentException(PREFIX_ER + "Element ds:Signature does not have correct namespace xmlns:ds");
        }
    }


    //Assertion ds:SignatureValue must have id attribute
    private void verifySignatureValueId(Document document) throws InvalidDocumentException {
        Element signatureValueElement = (Element) document.getElementsByTagName("ds:SignatureValue")
                                                          .item(0);

        if (signatureValueElement == null) {
            throw new InvalidDocumentException(PREFIX_ER + "Element ds:SignatureValue not found");
        }

        if (!signatureValueElement.hasAttribute("Id")) {
            throw new InvalidDocumentException(PREFIX_ER + "Element ds:SignatureValue does not have attribute Id");
        }

    }


    /**
     * Assertion of existing references in SignedInfo
     * ds:KeyInfo element,
     * ds:SignatureProperties element,
     * xades:SignedProperties element,
     */
    private void verifySignedInfoReferencesAndAttributeValues(Document document) throws InvalidDocumentException {
        List<Node> referencesElements = XPathUtils.selectNodeList(document, "//ds:Signature/ds:SignedInfo/ds:Reference");
        if (ListUtils.isEmpty(referencesElements)) {
            throw new InvalidDocumentException(PREFIX_ER + "element ds:Signature/ds:SignedInfo/ds:Reference not found");
        }

        for (Node referencesElement : referencesElements) {
            Element referenceElement = (Element) referencesElement;
            String uri = referenceElement.getAttribute("URI")
                                         .substring(1);
            String actualType = referenceElement.getAttribute("Type");
            Element referencedElement = XPathUtils.selectSingleElement(document, String.format("//ds:Signature//*[@Id='%s']", uri));

            if (null == referencedElement) {
                throw new InvalidDocumentException(PREFIX_ER + "verifying existence of reference in ds:SignedInfo. Error getting element with Id " + uri);
            }

            String referencedElementName = referencedElement.getNodeName();

            if (!ValidatorConstants.REFERENCES.containsKey(referencedElementName)) {
                throw new InvalidDocumentException(PREFIX_ER + "verifying the existence of references in ds:SignedInfo. Unknown reference " + referencedElementName);
            }

            String expectedReferenceType = ValidatorConstants.REFERENCES.get(referencedElementName);

            if (!actualType.equals(expectedReferenceType)) {
                throw new InvalidDocumentException(PREFIX_ER + "verifying match of references in ds:SignedInfo. " + actualType + " does not match " + expectedReferenceType);
            }


            Element keyInfoReferenceElement = XPathUtils.selectSingleElement(document, "//ds:Signature/ds:SignedInfo/ds:Reference[@Type='http://www.w3.org/2000/09/xmldsig#Object']");

            if (null == keyInfoReferenceElement) {
                throw new InvalidDocumentException(PREFIX_ER + "verifying the existence of references in ds:SignedInfo. Error getting element with Type http://www.w3.org/2000/09/xmldsig#Object or No reference in ds:KeyInfo element for ds:Reference element");
            }


            Element signaturePropertieReferenceElement = XPathUtils.selectSingleElement(document, "//ds:Signature/ds:SignedInfo/ds:Reference[@Type='http://www.w3.org/2000/09/xmldsig#SignatureProperties']");

            if (null == signaturePropertieReferenceElement) {
                throw new InvalidDocumentException(PREFIX_ER + "verifying the existence of references in ds:SignedInfo. Error getting element with Type http://www.w3.org/2000/09/xmldsig#SignatureProperties or No reference in ds:SignatureProperties element for ds:Reference element");
            }


            Element signedInfoReferenceElement = XPathUtils.selectSingleElement(document, "//ds:Signature/ds:SignedInfo/ds:Reference[@Type='http://uri.etsi.org/01903#SignedProperties']");

            if (null == signedInfoReferenceElement) {
                throw new InvalidDocumentException(PREFIX_ER + "verifying the existence of references in ds:SignedInfo. Error getting element with Type http://uri.etsi.org/01903#SignedProperties or No reference in xades:SignedProperties element for ds:Reference element");
            }
        }

    }


    /*
     * Content verification ds:KeyInfo:
     * 	- must have Id attribute,
     * 	- must have ds:X509Data, which contains elements: ds:X509Certificate, ds:X509IssuerSerial, ds:X509SubjectName,
     * 	- Value of elements ds:X509IssuerSerial and ds:X509SubjectName are same that are in certf. in ds:X509Certificate
     */
    private void verifyKeyInfoContent(Document document) throws InvalidDocumentException {

        Element keyInfoElement = (Element) document.getElementsByTagName("ds:KeyInfo")
                                                   .item(0);

        if (null == keyInfoElement) {
            throw new InvalidDocumentException(PREFIX_ER + "Element ds:Signature does not exist");
        }

        if (!keyInfoElement.hasAttribute("Id")) {
            throw new InvalidDocumentException(PREFIX_ER + "Element ds:Signature does not contain attribute Id");
        }

        if (!ValidatorUtils.checkAttributeValue(keyInfoElement, "Id")) {
            throw new InvalidDocumentException(PREFIX_ER + "Attribute Id elementu ds:Signature does not contain any value");
        }

        Element xDataElement = (Element) keyInfoElement.getElementsByTagName("ds:X509Data")
                                                       .item(0);

        if (null == xDataElement) {
            throw new InvalidDocumentException(PREFIX_ER + "Element ds:KeyInfo does not contain element ds:X509Data");
        }

        Element xCertificateElement = (Element) xDataElement.getElementsByTagName("ds:X509Certificate")
                                                            .item(0);
        Element xIssuerSerialElement = (Element) xDataElement.getElementsByTagName("ds:X509IssuerSerial")
                                                             .item(0);
        Element xSubjectNameElement = (Element) xDataElement.getElementsByTagName("ds:X509SubjectName")
                                                            .item(0);

        if (null == xCertificateElement) {
            throw new InvalidDocumentException(PREFIX_ER + "Element ds:X509Data does not contain element ds:X509Certificate");
        }

        if (null == xIssuerSerialElement) {
            throw new InvalidDocumentException(PREFIX_ER + "Element ds:X509Data does not contain element ds:X509IssuerSerial");
        }

        if (null == xSubjectNameElement) {
            throw new InvalidDocumentException(PREFIX_ER + "Element ds:X509Data does not contain element ds:X509SubjectName");
        }

        Element xIssuerNameElement = (Element) xIssuerSerialElement.getElementsByTagName("ds:X509IssuerName")
                                                                   .item(0);
        Element xSerialNumberElement = (Element) xIssuerSerialElement.getElementsByTagName("ds:X509SerialNumber")
                                                                     .item(0);

        if (null == xIssuerNameElement) {
            throw new InvalidDocumentException(PREFIX_ER + "Element ds:X509IssuerSerial does not contain element ds:X509IssuerName");
        }

        if (null == xSerialNumberElement) {
            throw new InvalidDocumentException(PREFIX_ER + "Element ds:X509IssuerSerial does not contain element ds:X509SerialNumber");
        }

        X509CertificateObject certificate = ValidatorUtils.getCertificate(document);

        String certifIssuerName = certificate.getIssuerX500Principal()
                                             .toString()
                                             .replaceAll("ST=", "S=");
        String certifSerialNumber = certificate.getSerialNumber()
                                               .toString();
        String certifSubjectName = certificate.getSubjectX500Principal()
                                              .toString();

        if (!xIssuerNameElement.getTextContent().equals(certifIssuerName)) {
            throw new InvalidDocumentException(PREFIX_ER + "Element ds:X509IssuerName does not match the value on the certificate");
        }

        if (!xSerialNumberElement.getTextContent().equals(certifSerialNumber)) {
            throw new InvalidDocumentException(PREFIX_ER + "Element ds:X509SerialNumberdoes not match the value on the certificate");
        }

        if (!xSubjectNameElement.getTextContent().equals(certifSubjectName)) {
            throw new InvalidDocumentException(PREFIX_ER + "Element ds:X509SubjectName does not contain element ds:X509SerialNumber");
        }

    }


    /*
     * Content verification ds:SignatureProperties:
     * 	- must have Id attribute,
     * 	- must have 2 elements ds:SignatureProperty for xzep:SignatureVersion and xzep:ProductInfos,
     * 	- both ds:SignatureProperty must have attribute Target set to ds:Signature
     */

    private void verifySignaturePropertiesContent(Document document) throws InvalidDocumentException {

        Element signaturePropertiesElement = (Element) document.getElementsByTagName("ds:SignatureProperties")
                                                               .item(0);

        if (null == signaturePropertiesElement) {
            throw new InvalidDocumentException(PREFIX_ER + "Element ds:SignatureProperties not found");
        }

        if (!signaturePropertiesElement.hasAttribute("Id")) {
            throw new InvalidDocumentException(PREFIX_ER + "Element ds:SignatureProperties does not contain attribute Id");
        }

        if (!ValidatorUtils.checkAttributeValue(signaturePropertiesElement, "Id")) {
            throw new InvalidDocumentException(PREFIX_ER + "Attribute Id of element ds:SignatureProperties does not contain any value");
        }

        Element signatureVersionElement = null;
        Element productInfosElement = null;

        for (int i = 0; i < signaturePropertiesElement.getElementsByTagName("ds:SignatureProperty").getLength(); i++) {

            Element tempElement = (Element) signaturePropertiesElement.getElementsByTagName("ds:SignatureProperty")
                                                                      .item(i);

            if (tempElement != null) {
                Element tempElement2 = (Element) tempElement.getElementsByTagName("xzep:SignatureVersion")
                                                            .item(0);
                if (tempElement2 != null) {
                    signatureVersionElement = tempElement2;
                } else {
                    tempElement2 = (Element) tempElement.getElementsByTagName("xzep:ProductInfos")
                                                        .item(0);
                    if (tempElement != null) {
                        productInfosElement = tempElement2;
                    }
                }
            }
        }

        if (null == signatureVersionElement) {
            throw new InvalidDocumentException(PREFIX_ER + "ds:SignatureProperties does not have element ds:SignatureProperty, which have another element xzep:SignatureVersion");
        }

        if (null == productInfosElement) {
            throw new InvalidDocumentException(PREFIX_ER + "ds:SignatureProperties does not have element ds:SignatureProperty, which have another element xzep:ProductInfos");
        }

        Element signature = (Element) document.getElementsByTagName("ds:Signature")
                                              .item(0);
        if (null == signature) {
            throw new InvalidDocumentException(PREFIX_ER + "Element ds:Signature not found");
        }

        String signatureId = signature.getAttribute("Id");

        Element sigVerParentElement = (Element) signatureVersionElement.getParentNode();
        Element pInfoParentElement = (Element) productInfosElement.getParentNode();
        String targetSigVer = sigVerParentElement.getAttribute("Target");
        String targetPInfo = pInfoParentElement.getAttribute("Target");

        if (!targetSigVer.equals("#" + signatureId)) {

            throw new InvalidDocumentException(PREFIX_ER + "Attribute Target of element xzep:SignatureVersion does not refer to ds:Signature");

        }

        if (!targetPInfo.equals("#" + signatureId)) {
            throw new InvalidDocumentException(PREFIX_ER + "Attribute Target of element xzep:ProductInfos does not refer to ds:Signature");

        }
    }


    private boolean checkReferenceDSManifest(Document document) throws InvalidDocumentException {
        List<Node> manifestElements = XPathUtils.selectNodeList(document, "//ds:Signature/ds:Object/ds:Manifest");

        if (ListUtils.isEmpty(manifestElements)) {
            throw new InvalidDocumentException(PREFIX_ER + "//ds:Signature/ds:Object/ds:Manifest not found");
        }

        for (Node element : manifestElements) {
            Element manifestElement = (Element) element;

            if (!manifestElement.hasAttribute("Id")) {
                throw new InvalidDocumentException(PREFIX_ER + "Manifest ID not found");
            }

            List<Node> referenceElements = XPathUtils.selectNodeList(manifestElement, "ds:Reference");

            if (ListUtils.isEmpty(referenceElements)) {
                throw new InvalidDocumentException(PREFIX_ER + manifestElement + ":ds:Reference not found");
            }

            if (referenceElements.size() != 1) {
                throw new InvalidDocumentException(PREFIX_ER + manifestElement + ": multiple ds:Reference");
            }

            List<Node> transformsElements = XPathUtils.selectNodeList(referenceElements.get(0), "ds:Transforms/ds:Transform");

            if (ListUtils.isEmpty(transformsElements)) {
                throw new InvalidDocumentException(PREFIX_ER + manifestElement + ":ds:Transforms/ds:Transform  not found");
            }

            for (Node transformsElement : transformsElements) {
                Element transformElement = (Element) transformsElement;

                /*
                 * ds:Transforms musí byť z množiny podporovaných algoritmov pre daný element podľa profilu XAdES_ZEP
                 */
                if (!ValidatorUtils.checkAttributeValue(transformElement, "Algorithm", ValidatorConstants.MANIFEST_TRANSFORM_METHODS)) {
                    throw new InvalidDocumentException(PREFIX_ER + manifestElement + ":ds:Transforms/ds:Transform  wrong type");
                }
            }

            Element digestMethodElement = XPathUtils.selectSingleElement(referenceElements.get(0), "ds:DigestMethod");

            if (null == digestMethodElement) {
                throw new InvalidDocumentException(PREFIX_ER + manifestElement + " ds:DigestMethod  not found");
            }

            /*
             * ds:DigestMethod – musí obsahovať URI niektorého z podporovaných algoritmov podľa profilu XAdES_ZEP
             */
            if (!ValidatorUtils.checkAttributeValue(digestMethodElement, "Algorithm", ValidatorConstants.DIGEST_METHODS)) {
                throw new InvalidDocumentException(PREFIX_ER + manifestElement + " ds:DigestMethod  wrong Algorithm");
            }

            /*
             * overenie hodnoty Type atribútu voči profilu XAdES_ZEP
             */
            if (!ValidatorUtils.checkAttributeValue((Element) referenceElements.get(0), "Type", "http://www.w3.org/2000/09/xmldsig#Object")) {
                throw new InvalidDocumentException(PREFIX_ER + manifestElement + " Type wrong URL");
            }
        }

        return true;
    }


    private boolean verifyManifestDigestValue(Document document) throws InvalidDocumentException {
        List<Node> referenceElements = XPathUtils.selectNodeList(document, "//ds:Signature/ds:Object/ds:Manifest/ds:Reference");

        if(!ListUtils.isEmpty(referenceElements)) {
            for (Node element : referenceElements) {
                Element referenceElement = (Element) element;
                String uri = referenceElement.getAttribute("URI")
                                             .substring(1);

                Element objectElement = findByAttributeValue(document, "ds:Object", "Id", uri);
                Element digestValueElement = (Element) referenceElement.getElementsByTagName("ds:DigestValue")
                                                                       .item(0);
                Element digestMethodlement = (Element) referenceElement.getElementsByTagName("ds:DigestMethod")
                                                                       .item(0);

                String digestMethod = digestMethodlement.getAttribute("Algorithm");
                digestMethod = ValidatorConstants.DIGEST_ALG.get(digestMethod);

                NodeList transformsElements = referenceElement.getElementsByTagName("ds:Transforms");

                for (int j = 0; j < transformsElements.getLength(); j++) {
                    Element transformsElement = (Element) transformsElements.item(j);
                    Element transformElement = (Element) transformsElement.getElementsByTagName("ds:Transform")
                                                                          .item(j);
                    String transformMethod = transformElement.getAttribute("Algorithm");
                    if(null != transformMethod) {
                        byte[] objectElementBytes = fromElementToString(objectElement).getBytes();

                        if ("http://www.w3.org/TR/2001/REC-xml-c14n-20010315".equals(transformMethod)) {

                            try {
                                Canonicalizer canonicalizer = Canonicalizer.getInstance(transformMethod);
                                objectElementBytes = canonicalizer.canonicalize(objectElementBytes);

                            } catch (SAXException | InvalidCanonicalizerException | CanonicalizationException |
                                     ParserConfigurationException | IOException e) {
                                LOGGER.log(Level.SEVERE, "", e);
                                throw new InvalidDocumentException(PREFIX_ER + "canonicalizer", e);
                            }
                        }

                        if ("http://www.w3.org/2000/09/xmldsig#base64".equals(transformMethod)) {

                            objectElementBytes = Base64.decode(objectElementBytes);
                        }

                        MessageDigest messageDigest = null;
                        try {
                            messageDigest = MessageDigest.getInstance(digestMethod);

                        } catch (NoSuchAlgorithmException e) {
                            throw new InvalidDocumentException(PREFIX_ER + "MessageDigest alg doesnt exist", e);
                        }

                        String actualDigestValue = new String(Base64.encode(messageDigest.digest(objectElementBytes)));
                        String expectedDigestValue = digestValueElement.getTextContent();

                        if (!expectedDigestValue.equals(actualDigestValue)) {
                            throw new InvalidDocumentException(PREFIX_ER + "Manifest Reference Digest value not same");
                        }
                    }
                }
            }
        }

        return true;
    }


    private Element findByAttributeValue(Document document, String elementType, String attributeName, String attributeValue) {
        NodeList elements = document.getElementsByTagName(elementType);
        for (int i = 0; i < elements.getLength(); i++) {
            Element element = (Element) elements.item(i);
            if (element.hasAttribute(attributeName) && element.getAttribute(attributeName)
                                                              .equals(attributeValue)) {
                return element;
            }
        }

        return null;
    }

    private String fromElementToString(Element element) {
        try {
            StreamResult result = new StreamResult(new StringWriter());
            Transformer transformer = TransformerFactory.newInstance()
                                                        .newTransformer();
            transformer.transform(new DOMSource(element), result);
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

            return result.getWriter()
                         .toString();
        } catch (TransformerException e) {
            LOGGER.log(Level.SEVERE, "", e);
        }
        return null;
    }

    private boolean coreValidation1(Document document) throws InvalidDocumentException {
        List<Node> referencesElements = XPathUtils.selectNodeList(document, "//ds:Signature/ds:SignedInfo/ds:Reference");

        if (ListUtils.isEmpty(referencesElements)) {
            throw new InvalidDocumentException(PREFIX_ER + " ds:Signature/ds:SignedInfo/ds:Reference not found");
        }

        for (Node referencesElement : referencesElements) {
            Element referenceElement = (Element) referencesElement;
            String uri = referenceElement.getAttribute("URI")
                                         .substring(1);

            Element manifestElement = findByAttributeValue(document, "ds:Manifest", "Id", uri);

            if (manifestElement == null) { continue; }

            Element digestValueElement = (Element) referenceElement.getElementsByTagName("ds:DigestValue")
                                                                   .item(0);
            String expectedDigestValue = digestValueElement.getTextContent();
            Element digestMethodElement = (Element) referenceElement.getElementsByTagName("ds:DigestMethod")
                                                                    .item(0);

            if (!ValidatorUtils.checkAttributeValue(digestMethodElement, "Algorithm", ValidatorConstants.DIGEST_METHODS)) {
                throw new InvalidDocumentException(PREFIX_ER + "ds:DigestMethod error");
            }

            String digestMethod = digestMethodElement.getAttribute("Algorithm");
            digestMethod = ValidatorConstants.DIGEST_ALG.get(digestMethod);

            String toBytes = fromElementToString(manifestElement);

            if (null != toBytes) {
                byte[] manifestElementBytes = toBytes.getBytes();
                NodeList transformsElements = manifestElement.getElementsByTagName("ds:Transforms");

                for (int j = 0; j < transformsElements.getLength(); j++) {

                    Element transformsElement = (Element) transformsElements.item(j);
                    Element transformElement = (Element) transformsElement.getElementsByTagName("ds:Transform")
                                                                          .item(0);
                    String transformMethod = transformElement.getAttribute("Algorithm");

                    if ("http://www.w3.org/TR/2001/REC-xml-c14n-20010315".equals(transformMethod)) {
                        try {
                            Canonicalizer canonicalizer = Canonicalizer.getInstance(transformMethod);
                            manifestElementBytes = canonicalizer.canonicalize(manifestElementBytes);
                        } catch (SAXException | InvalidCanonicalizerException | CanonicalizationException |
                                 ParserConfigurationException | IOException e) {
                            throw new InvalidDocumentException(PREFIX_ER + "Core validation canonical error", e);
                        }
                    }
                }

                MessageDigest messageDigest = null;
                try {
                    messageDigest = MessageDigest.getInstance(digestMethod);

                } catch (NoSuchAlgorithmException e) {
                    throw new InvalidDocumentException(PREFIX_ER + "Core validation alg error", e);
                }

                String actualDigestValue = new String(Base64.encode(messageDigest.digest(manifestElementBytes)));

                if (!expectedDigestValue.equals(actualDigestValue)) {
                    throw new InvalidDocumentException(PREFIX_ER + "Core validation error digest not same");
                }
            } else {
                throw new InvalidDocumentException(PREFIX_ER + "Core validation error element to string");
            }
        }

        return true;
    }

    private boolean coreValidation2(Document document) throws InvalidDocumentException {
        Element signatureElement = (Element) document.getElementsByTagName("ds:Signature")
                                                     .item(0);
        Element signedInfoElement = (Element) signatureElement.getElementsByTagName("ds:SignedInfo")
                                                              .item(0);
        Element canonicalizationMethodElement = (Element) signedInfoElement.getElementsByTagName(
                                                                                   "ds:CanonicalizationMethod")
                                                                           .item(0);
        Element signatureMethodElement = (Element) signedInfoElement.getElementsByTagName("ds:SignatureMethod")
                                                                    .item(0);
        Element signatureValueElement = (Element) signatureElement.getElementsByTagName("ds:SignatureValue")
                                                                  .item(0);
        String toBytes = fromElementToString(signedInfoElement);

        if(null != toBytes) {
            byte[] signedInfoElementBytes = toBytes.getBytes();
            String canonicalizationMethod = canonicalizationMethodElement.getAttribute("Algorithm");

            try {
                Canonicalizer canonicalizer = Canonicalizer.getInstance(canonicalizationMethod);
                signedInfoElementBytes = canonicalizer.canonicalize(signedInfoElementBytes);
            } catch (SAXException | InvalidCanonicalizerException | CanonicalizationException |
                     ParserConfigurationException | IOException e) {
                throw new InvalidDocumentException(PREFIX_ER + "error canonicalizer", e);
            }

            X509CertificateObject certificate = ValidatorUtils.getCertificate(document);
            String signatureMethod = signatureMethodElement.getAttribute("Algorithm");
            signatureMethod = ValidatorConstants.SIGN_ALG.get(signatureMethod);

            Signature signer = null;
            try {
                signer = Signature.getInstance(signatureMethod);
                signer.initVerify(certificate.getPublicKey());
                signer.update(signedInfoElementBytes);
            } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e1) {
                throw new InvalidDocumentException(PREFIX_ER + "Core validation error", e1);
            }

            byte[] signatureValueBytes = signatureValueElement.getTextContent()
                                                              .getBytes();
            byte[] decodedSignatureValueBytes = Base64.decode(signatureValueBytes);
            boolean verificationResult = false;

            try {
                verificationResult = signer.verify(decodedSignatureValueBytes);
            } catch (SignatureException e1) {
                throw new InvalidDocumentException(PREFIX_ER + "Core validation error - verification dig sig error", e1);
            }

            if (verificationResult == false) {
                throw new InvalidDocumentException(PREFIX_ER + "Core validation error - verify ds:SignedInfo, ds:SignatureValue not same");
            }

            return true;
        } else {
            throw new InvalidDocumentException(PREFIX_ER + "error transform to byte");
        }
    }
}
