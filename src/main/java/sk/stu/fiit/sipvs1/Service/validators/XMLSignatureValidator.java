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

    private static final Logger LOGGER = Logger.getLogger("XMLSignatureValidator");
    public static final String PREFIX_ER = "XML SIGNATURE | ERROR : ";
    public static final String PREFIX_OK = "XML SIGNATURE | OK : ";


    public void validate(Document document) throws InvalidDocumentException {

        org.apache.xml.security.Init.init();
        Security.addProvider(new BouncyCastleProvider());

        verifySignatureMethodAndCanonicalizationMethod(document);
        logOk("ds:SignatureMethod and ds:CanonicalizationMethod");

        verifyTransformsAndDigestMethod(document);
        logOk("ds:Transforms and ds:DigestMethod");

        coreValidation1(document);
        logOk("Core Validation - ds:Manifest and ds:DigestValue");

        coreValidation2(document);
        logOk("Core Validation - ds:SignedInfo and ds:SignatureValue");

        verifySignature(document);
        logOk("ds:Signature ID and namespace xmlns:ds");

        verifySignatureValueId(document);
        logOk("ds:SignatureValue ID");

        verifySignedInfoReferencesAndAttributeValues(document);
        logOk("References, ID types ds:SignedInfo");

        verifyKeyInfoContent(document);
        logOk("Content ds:KeyInfo");

        verifySignaturePropertiesContent(document);
        logOk("Content ds:SignatureProperties");

        checkReferenceDSManifest(document);
        logOk("Elements ds:Manifest");

        verifyManifestDigestValue(document);
        logOk("References ds:Manifest");

    }


    // Assertion ds:SignatureMethod and ds:CanonicalizationMethod URI
    private void verifySignatureMethodAndCanonicalizationMethod(Document document) throws InvalidDocumentException {

        Element signatureMethod = XPathUtils.selectSingleElement(document,"//ds:Signature/ds:SignedInfo/ds:SignatureMethod");

        if(null == signatureMethod) {
            invalidateDocument("Element ds:Signature/ds:SignedInfo/ds:SignatureMethod not found");
        }

        //Assertion ds:SignatureMethod
        if (ValidatorUtils.checkAttributeValueNot(signatureMethod, ValidatorConstants.ALGORITHM, ValidatorConstants.SIGNATURE_METHODS)) {
            invalidateDocument("Attribute algorithm of element ds:SignatureMethod does not contain a URI for any of the supported algorithms");
        }

        Element canonicalizationMethod =  XPathUtils.selectSingleElement(document, "//ds:Signature/ds:SignedInfo/ds:CanonicalizationMethod");

        if(null == canonicalizationMethod) {
            invalidateDocument("element ds:Signature/ds:SignedInfo/ds:CanonicalizationMethod not found");
        }

        //Assertion ds:CanonicalizationMethod
        if (ValidatorUtils.checkAttributeValueNot(canonicalizationMethod,
                                                  ValidatorConstants.ALGORITHM,
                                                  ValidatorConstants.CANONICALIZATION_METHODS)) {
            invalidateDocument("Attribute algorithm of element ds:CanonicalizationMethod does not contain a URI for any of the supported algorithms");
        }

    }

    // Assertion ds:Transforms and ds:DigestMethod elements URI
    private void verifyTransformsAndDigestMethod(Document document) throws InvalidDocumentException {

        List<Node> transformsElements = XPathUtils.selectNodeList(document, "//ds:Signature/ds:SignedInfo/ds:Reference/ds:Transforms");

        if (ListUtils.isEmpty(transformsElements)) {
            invalidateDocument("element ds:Signature/ds:SignedInfo/ds:Reference/ds:Transforms not found");
        }

        for (Node element : transformsElements) {
            Element transformsElement = (Element) element;
            Element transformElement = (Element) transformsElement.getElementsByTagName(ValidatorConstants.DS_TRANSFORM)
                                                                  .item(0);

            // Assertion ds:Transforms Element
            if (ValidatorUtils.checkAttributeValueNot(transformElement,
                                                      ValidatorConstants.ALGORITHM,
                                                      ValidatorConstants.TRANSFORM_METHODS)) {
                invalidateDocument("Attribute algorithm of element ds:Transforms does not contain a URI for any of the supported algorithms");
            }
        }


        List<Node> digestMethodElements = XPathUtils.selectNodeList(document, "//ds:Signature/ds:SignedInfo/ds:Reference/ds:DigestMethod");

        if (ListUtils.isEmpty(digestMethodElements)) {
            invalidateDocument("element ds:Signature/ds:SignedInfo/ds:Reference/ds:DigestMethod not found");
        }

        for (Node methodElement : digestMethodElements) {
            Element digestMethodElement = (Element) methodElement;

            // Assertion ds:DigestMethod Element
            if (ValidatorUtils.checkAttributeValueNot(digestMethodElement,
                                                      ValidatorConstants.ALGORITHM,
                                                      ValidatorConstants.DIGEST_METHODS)) {
                invalidateDocument("Attribute algorithm of element ds:DigestMethod does not contain a URI for any of the supported algorithms");
            }
        }

    }


    // Bod 4.2.2. - must have Id attribute and specific amespace xmlns:ds
    private void verifySignature(Document document) throws InvalidDocumentException {

        Element signatureElement = (Element) document.getElementsByTagName(ValidatorConstants.DS_SIGNATURE)
                                                     .item(0);

        if (signatureElement == null) {
            invalidateDocument("element ds:Signature not found");
        }

        if (!signatureElement.hasAttribute("Id")) {
            invalidateDocument("Element ds:Signature does not have attribute Id");
        }

        if (ValidatorUtils.checkAttributeValueNot(signatureElement, "Id")) {
            invalidateDocument("Attribute Id of element ds:Signature does not have any value");
        }

        if (!ValidatorUtils.checkAttributeValue(signatureElement, "xmlns:ds", "http://www.w3.org/2000/09/xmldsig#")) {
            invalidateDocument("Element ds:Signature does not have correct namespace xmlns:ds");
        }
    }


    //Assertion ds:SignatureValue must have id attribute
    private void verifySignatureValueId(Document document) throws InvalidDocumentException {
        Element signatureValueElement = (Element) document.getElementsByTagName("ds:SignatureValue")
                                                          .item(0);

        if (signatureValueElement == null) {
            invalidateDocument("Element ds:SignatureValue not found");
        }

        if (!signatureValueElement.hasAttribute("Id")) {
            invalidateDocument("Element ds:SignatureValue does not have attribute Id");
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
            invalidateDocument("element ds:Signature/ds:SignedInfo/ds:Reference not found");
        }

        for (Node referencesElement : referencesElements) {
            Element referenceElement = (Element) referencesElement;
            String uri = referenceElement.getAttribute("URI")
                                         .substring(1);
            String actualType = referenceElement.getAttribute("Type");
            Element referencedElement = XPathUtils.selectSingleElement(document, String.format("//ds:Signature//*[@Id='%s']", uri));

            if (null == referencedElement) {
                invalidateDocument("verifying existence of reference in ds:SignedInfo. Error getting element with Id " + uri);
            }

            String referencedElementName = referencedElement.getNodeName();

            if (!ValidatorConstants.REFERENCES.containsKey(referencedElementName)) {
                invalidateDocument("verifying the existence of references in ds:SignedInfo. Unknown reference " + referencedElementName);
            }

            String expectedReferenceType = ValidatorConstants.REFERENCES.get(referencedElementName);

            if (!actualType.equals(expectedReferenceType)) {
                invalidateDocument("verifying match of references in ds:SignedInfo. " + actualType + " does not match " + expectedReferenceType);
            }


            Element keyInfoReferenceElement = XPathUtils.selectSingleElement(document, "//ds:Signature/ds:SignedInfo/ds:Reference[@Type='http://www.w3.org/2000/09/xmldsig#Object']");

            if (null == keyInfoReferenceElement) {
                invalidateDocument("verifying the existence of references in ds:SignedInfo. Error getting element with Type http://www.w3.org/2000/09/xmldsig#Object or No reference in ds:KeyInfo element for ds:Reference element");
            }


            Element signaturePropertieReferenceElement = XPathUtils.selectSingleElement(document, "//ds:Signature/ds:SignedInfo/ds:Reference[@Type='http://www.w3.org/2000/09/xmldsig#SignatureProperties']");

            if (null == signaturePropertieReferenceElement) {
                invalidateDocument("verifying the existence of references in ds:SignedInfo. Error getting element with Type http://www.w3.org/2000/09/xmldsig#SignatureProperties or No reference in ds:SignatureProperties element for ds:Reference element");
            }


            Element signedInfoReferenceElement = XPathUtils.selectSingleElement(document, "//ds:Signature/ds:SignedInfo/ds:Reference[@Type='http://uri.etsi.org/01903#SignedProperties']");

            if (null == signedInfoReferenceElement) {
                invalidateDocument("verifying the existence of references in ds:SignedInfo. Error getting element with Type http://uri.etsi.org/01903#SignedProperties or No reference in xades:SignedProperties element for ds:Reference element");
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
            invalidateDocument("Element ds:Signature does not exist");
        }

        if (!keyInfoElement.hasAttribute("Id")) {
            invalidateDocument("Element ds:Signature does not contain attribute Id");
        }

        if (ValidatorUtils.checkAttributeValueNot(keyInfoElement, "Id")) {
            invalidateDocument("Attribute Id elementu ds:Signature does not contain any value");
        }

        Element xDataElement = (Element) keyInfoElement.getElementsByTagName("ds:X509Data")
                                                       .item(0);

        if (null == xDataElement) {
            invalidateDocument("Element ds:KeyInfo does not contain element ds:X509Data");
        }

        Element xCertificateElement = (Element) xDataElement.getElementsByTagName("ds:X509Certificate")
                                                            .item(0);
        Element xIssuerSerialElement = (Element) xDataElement.getElementsByTagName("ds:X509IssuerSerial")
                                                             .item(0);
        Element xSubjectNameElement = (Element) xDataElement.getElementsByTagName("ds:X509SubjectName")
                                                            .item(0);

        if (null == xCertificateElement) {
            invalidateDocument("Element ds:X509Data does not contain element ds:X509Certificate");
        }

        if (null == xIssuerSerialElement) {
            invalidateDocument("Element ds:X509Data does not contain element ds:X509IssuerSerial");
        }

        if (null == xSubjectNameElement) {
            invalidateDocument("Element ds:X509Data does not contain element ds:X509SubjectName");
        }

        Element xIssuerNameElement = (Element) xIssuerSerialElement.getElementsByTagName("ds:X509IssuerName")
                                                                   .item(0);
        Element xSerialNumberElement = (Element) xIssuerSerialElement.getElementsByTagName("ds:X509SerialNumber")
                                                                     .item(0);

        if (null == xIssuerNameElement) {
            invalidateDocument("Element ds:X509IssuerSerial does not contain element ds:X509IssuerName");
        }

        if (null == xSerialNumberElement) {
            invalidateDocument("Element ds:X509IssuerSerial does not contain element ds:X509SerialNumber");
        }

        X509CertificateObject certificate = ValidatorUtils.getCertificate(document);

        String certifIssuerName = certificate.getIssuerX500Principal()
                                             .toString()
                                             .replace("ST=", "S=");
        String certifSerialNumber = certificate.getSerialNumber()
                                               .toString();
        String certifSubjectName = certificate.getSubjectX500Principal()
                                              .toString();

        if (!xIssuerNameElement.getTextContent().equals(certifIssuerName)) {
            invalidateDocument("Element ds:X509IssuerName does not match the value on the certificate");
        }

        if (!xSerialNumberElement.getTextContent().equals(certifSerialNumber)) {
            invalidateDocument("Element ds:X509SerialNumberdoes not match the value on the certificate");
        }

        if (!xSubjectNameElement.getTextContent().equals(certifSubjectName)) {
            invalidateDocument("Element ds:X509SubjectName does not contain element ds:X509SerialNumber");
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
            invalidateDocument("Element ds:SignatureProperties not found");
        }

        if (!signaturePropertiesElement.hasAttribute("Id")) {
            invalidateDocument("Element ds:SignatureProperties does not contain attribute Id");
        }

        if (ValidatorUtils.checkAttributeValueNot(signaturePropertiesElement, "Id")) {
            invalidateDocument("Attribute Id of element ds:SignatureProperties does not contain any value");
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
            invalidateDocument("ds:SignatureProperties does not have element ds:SignatureProperty, which have another element xzep:SignatureVersion");
        }

        if (null == productInfosElement) {
            invalidateDocument("ds:SignatureProperties does not have element ds:SignatureProperty, which have another element xzep:ProductInfos");
        }

        Element signature = (Element) document.getElementsByTagName(ValidatorConstants.DS_SIGNATURE)
                                              .item(0);
        if (null == signature) {
            invalidateDocument("Element ds:Signature not found");
        }

        String signatureId = signature.getAttribute("Id");

        Element sigVerParentElement = (Element) signatureVersionElement.getParentNode();
        Element pInfoParentElement = (Element) productInfosElement.getParentNode();
        String targetSigVer = sigVerParentElement.getAttribute("Target");
        String targetPInfo = pInfoParentElement.getAttribute("Target");

        if (!targetSigVer.equals("#" + signatureId)) {

            invalidateDocument("Attribute Target of element xzep:SignatureVersion does not refer to ds:Signature");

        }

        if (!targetPInfo.equals("#" + signatureId)) {
            invalidateDocument("Attribute Target of element xzep:ProductInfos does not refer to ds:Signature");

        }
    }


    private boolean checkReferenceDSManifest(Document document) throws InvalidDocumentException {
        List<Node> manifestElements = XPathUtils.selectNodeList(document, "//ds:Signature/ds:Object/ds:Manifest");

        if (ListUtils.isEmpty(manifestElements)) {
            invalidateDocument("//ds:Signature/ds:Object/ds:Manifest not found");
        }

        for (Node element : manifestElements) {
            Element manifestElement = (Element) element;

            if (!manifestElement.hasAttribute("Id")) {
                invalidateDocument("Manifest ID not found");
            }

            List<Node> referenceElements = XPathUtils.selectNodeList(manifestElement, "ds:Reference");

            if (ListUtils.isEmpty(referenceElements)) {
                invalidateDocument(manifestElement + ":ds:Reference not found");
            }

            if (referenceElements.size() != 1) {
                invalidateDocument(manifestElement + ": multiple ds:Reference");
            }

            List<Node> transformsElements = XPathUtils.selectNodeList(referenceElements.get(0), "ds:Transforms/ds:Transform");

            if (ListUtils.isEmpty(transformsElements)) {
                invalidateDocument(manifestElement + ":ds:Transforms/ds:Transform  not found");
            }

            for (Node transformsElement : transformsElements) {
                Element transformElement = (Element) transformsElement;

                /*
                 * ds:Transforms musí byť z množiny podporovaných algoritmov pre daný element podľa profilu XAdES_ZEP
                 */
                if (ValidatorUtils.checkAttributeValueNot(transformElement,
                                                          ValidatorConstants.ALGORITHM,
                                                          ValidatorConstants.MANIFEST_TRANSFORM_METHODS)) {
                    invalidateDocument(manifestElement + ":ds:Transforms/ds:Transform  wrong type");
                }
            }

            Element digestMethodElement = XPathUtils.selectSingleElement(referenceElements.get(0),
                                                                         ValidatorConstants.DS_DIGEST_METHOD);

            if (null == digestMethodElement) {
                invalidateDocument(manifestElement + " ds:DigestMethod  not found");
            }

            /*
             * ds:DigestMethod – musí obsahovať URI niektorého z podporovaných algoritmov podľa profilu XAdES_ZEP
             */
            if (ValidatorUtils.checkAttributeValueNot(digestMethodElement,
                                                      ValidatorConstants.ALGORITHM,
                                                      ValidatorConstants.DIGEST_METHODS)) {
                invalidateDocument(manifestElement + " ds:DigestMethod  wrong Algorithm");
            }

            /*
             * overenie hodnoty Type atribútu voči profilu XAdES_ZEP
             */
            if (!ValidatorUtils.checkAttributeValue((Element) referenceElements.get(0), "Type", "http://www.w3.org/2000/09/xmldsig#Object")) {
                invalidateDocument(manifestElement + " Type wrong URL");
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
                Element digestMethodlement = (Element) referenceElement.getElementsByTagName(ValidatorConstants.DS_DIGEST_METHOD)
                                                                       .item(0);

                String digestMethod = digestMethodlement.getAttribute(ValidatorConstants.ALGORITHM);
                digestMethod = ValidatorConstants.DIGEST_ALG.get(digestMethod);

                NodeList transformsElements = referenceElement.getElementsByTagName("ds:Transforms");

                for (int j = 0; j < transformsElements.getLength(); j++) {
                    Element transformsElement = (Element) transformsElements.item(j);
                    Element transformElement = (Element) transformsElement.getElementsByTagName(ValidatorConstants.DS_TRANSFORM)
                                                                          .item(j);
                    String transformMethod = transformElement.getAttribute(ValidatorConstants.ALGORITHM);
                    if(null != transformMethod) {
                        byte[] objectElementBytes = fromElementToString(objectElement).getBytes();

                        if ("http://www.w3.org/TR/2001/REC-xml-c14n-20010315".equals(transformMethod)) {

                            try {
                                Canonicalizer canonicalizer = Canonicalizer.getInstance(transformMethod);
                                objectElementBytes = canonicalizer.canonicalize(objectElementBytes);

                            } catch (SAXException | InvalidCanonicalizerException | CanonicalizationException |
                                     ParserConfigurationException | IOException e) {
                                LOGGER.log(Level.SEVERE, "", e);
                                invalidateDocument("canonicalizer", e);
                            }
                        }

                        if ("http://www.w3.org/2000/09/xmldsig#base64".equals(transformMethod)) {

                            objectElementBytes = Base64.decode(objectElementBytes);
                        }

                        MessageDigest messageDigest = null;
                        try {
                            messageDigest = MessageDigest.getInstance(digestMethod);

                        } catch (NoSuchAlgorithmException e) {
                            invalidateDocument("MessageDigest alg doesnt exist", e);
                        }

                        String actualDigestValue = new String(Base64.encode(messageDigest.digest(objectElementBytes)));
                        String expectedDigestValue = digestValueElement.getTextContent();

                        if (!expectedDigestValue.equals(actualDigestValue)) {
                            invalidateDocument("Manifest Reference Digest value not same");
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
            invalidateDocument(" ds:Signature/ds:SignedInfo/ds:Reference not found");
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
            Element digestMethodElement = (Element) referenceElement.getElementsByTagName(ValidatorConstants.DS_DIGEST_METHOD)
                                                                    .item(0);

            if (ValidatorUtils.checkAttributeValueNot(digestMethodElement,
                                                      ValidatorConstants.ALGORITHM,
                                                      ValidatorConstants.DIGEST_METHODS)) {
                invalidateDocument("ds:DigestMethod error");
            }

            String digestMethod = digestMethodElement.getAttribute(ValidatorConstants.ALGORITHM);
            digestMethod = ValidatorConstants.DIGEST_ALG.get(digestMethod);

            String toBytes = fromElementToString(manifestElement);

            if (null != toBytes) {
                byte[] manifestElementBytes = toBytes.getBytes();
                NodeList transformsElements = manifestElement.getElementsByTagName("ds:Transforms");

                for (int j = 0; j < transformsElements.getLength(); j++) {

                    Element transformsElement = (Element) transformsElements.item(j);
                    Element transformElement = (Element) transformsElement.getElementsByTagName(ValidatorConstants.DS_TRANSFORM)
                                                                          .item(0);
                    String transformMethod = transformElement.getAttribute(ValidatorConstants.ALGORITHM);

                    if ("http://www.w3.org/TR/2001/REC-xml-c14n-20010315".equals(transformMethod)) {
                        try {
                            Canonicalizer canonicalizer = Canonicalizer.getInstance(transformMethod);
                            manifestElementBytes = canonicalizer.canonicalize(manifestElementBytes);
                        } catch (SAXException | InvalidCanonicalizerException | CanonicalizationException |
                                 ParserConfigurationException | IOException e) {
                            invalidateDocument("Core validation canonical error", e);
                        }
                    }
                }

                MessageDigest messageDigest = null;
                try {
                    messageDigest = MessageDigest.getInstance(digestMethod);

                } catch (NoSuchAlgorithmException e) {
                    invalidateDocument("Core validation alg error", e);
                }

                String actualDigestValue = new String(Base64.encode(messageDigest.digest(manifestElementBytes)));

                if (!expectedDigestValue.equals(actualDigestValue)) {
                    invalidateDocument("Core validation error digest not same");
                }
            } else {
                invalidateDocument("Core validation error element to string");
            }
        }

        return true;
    }

    private boolean coreValidation2(Document document) throws InvalidDocumentException {
        Element signatureElement = (Element) document.getElementsByTagName(ValidatorConstants.DS_SIGNATURE)
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
            String canonicalizationMethod = canonicalizationMethodElement.getAttribute(ValidatorConstants.ALGORITHM);

            try {
                Canonicalizer canonicalizer = Canonicalizer.getInstance(canonicalizationMethod);
                signedInfoElementBytes = canonicalizer.canonicalize(signedInfoElementBytes);
            } catch (SAXException | InvalidCanonicalizerException | CanonicalizationException |
                     ParserConfigurationException | IOException e) {
                invalidateDocument("error canonicalizer", e);
            }

            X509CertificateObject certificate = ValidatorUtils.getCertificate(document);
            String signatureMethod = signatureMethodElement.getAttribute(ValidatorConstants.ALGORITHM);
            signatureMethod = ValidatorConstants.SIGN_ALG.get(signatureMethod);

            Signature signer = null;
            try {
                signer = Signature.getInstance(signatureMethod);
                signer.initVerify(certificate.getPublicKey());
                signer.update(signedInfoElementBytes);
            } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e1) {
                invalidateDocument("Core validation error", e1);
            }

            byte[] signatureValueBytes = signatureValueElement.getTextContent()
                                                              .getBytes();
            byte[] decodedSignatureValueBytes = Base64.decode(signatureValueBytes);
            boolean verificationResult = false;

            try {
                verificationResult = signer.verify(decodedSignatureValueBytes);
            } catch (SignatureException e1) {
                invalidateDocument("Core validation error - verification dig sig error", e1);
            }

            if (!verificationResult) {
                invalidateDocument("Core validation error - verify ds:SignedInfo, ds:SignatureValue not same");
            }

            return true;
        } else {
            invalidateDocument("error transform to byte");
        }
        return false;
    }

    private void logOk(String x) {
        LOGGER.info(PREFIX_OK + x);
    }

    private void invalidateDocument(String s) throws InvalidDocumentException {
        throw new InvalidDocumentException(PREFIX_ER + s);
    }

    private void invalidateDocument(String s, Exception e) throws InvalidDocumentException {
        throw new InvalidDocumentException(PREFIX_ER + s, e);
    }
}
