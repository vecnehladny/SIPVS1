package sk.stu.fiit.sipvs1.Service;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x509.Certificate;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.provider.X509CertificateObject;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.util.encoders.Base64;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import sk.stu.fiit.sipvs1.Model.InvalidDocumentException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.cert.*;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ValidatorUtils {

    public static final Logger LOGGER = Logger.getLogger("ValidatorService");

    public static boolean checkAttributeValue(Element element, String attribute, String expectedValue) {
        return element.getAttribute(attribute).equals(expectedValue);
    }

    public static boolean checkAttributeValueS(Element element, String attribute, String expectedValue) {
        return element.getAttribute(attribute).equals(expectedValue);
    }

    public static boolean checkAttributeValueNot(Element element, String attribute, List<String> expectedValues) {
        for (String expectedValue : expectedValues) {
            if (checkAttributeValue(element, attribute, expectedValue)) {
                return false;
            }
        }
        return true;
    }

    public static boolean checkAttributeValueNot(Element element, String attribute) {
        return element.getAttribute(attribute).isEmpty();
    }


    public static X509CertificateObject getCertificate(Document document) throws InvalidDocumentException {

        Element keyInfoElement = (Element) document.getElementsByTagName("ds:KeyInfo")
                                                   .item(0);

        if (keyInfoElement == null) {
            throw new InvalidDocumentException("Document does not contain an element ds:KeyInfo");
        }

        Element x509DataElement = (Element) keyInfoElement.getElementsByTagName("ds:X509Data")
                                                          .item(0);

        if (x509DataElement == null) {
            throw new InvalidDocumentException("Document does not contain an element ds:X509Data");
        }

        Element x509Certificate = (Element) x509DataElement.getElementsByTagName("ds:X509Certificate")
                                                           .item(0);

        if (x509Certificate == null) {
            throw new InvalidDocumentException("Document does not contain an element ds:X509Certificate");
        }

        X509CertificateObject certObject;

        try (ASN1InputStream inputStream = new ASN1InputStream(new ByteArrayInputStream(Base64.decode(x509Certificate.getTextContent())))) {
            ASN1Sequence sequence = (ASN1Sequence) inputStream.readObject();
            certObject = new X509CertificateObject(Certificate.getInstance(sequence));

        } catch (CertificateParsingException | IOException e) {
            throw new InvalidDocumentException("Could not load certificate" , e);

        }

        return certObject;
    }


    public static TimeStampToken getTimestampToken(Document document) throws InvalidDocumentException {

        TimeStampToken token = null;

        Node timestamp = XPathUtils.selectSingleNode(document, "//xades:EncapsulatedTimeStamp", Map.of("xades", "http://uri.etsi.org/01903/v1.3.2#"));

        if (timestamp == null) {
            throw new InvalidDocumentException("Document does not have timestamp");
        }

        try {
            token = new TimeStampToken(new CMSSignedData(Base64.decode(timestamp.getTextContent())));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "", e);
        }

        return token;
    }

    public static X509CRL getCRL() throws InvalidDocumentException {

        ByteArrayInputStream crlData = null;

        try {
            crlData = new ByteArrayInputStream(Files.readAllBytes(Path.of("src/main/resources/DTCCACrl.crl")));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "", e);
        }

        if (crlData == null) {
            throw new InvalidDocumentException("Could not load CRL file");
        }

        CertificateFactory certFactory;
        try {
            Security.addProvider(new BouncyCastleProvider());
            certFactory = CertificateFactory.getInstance("X.509", "BC");
        } catch (CertificateException | NoSuchProviderException e) {
            throw new InvalidDocumentException("Cannot create certification factory instance", e);
        }


        X509CRL crl;

        try {
            crl = (X509CRL) certFactory.generateCRL(crlData);
        } catch (CRLException e) {
            throw new InvalidDocumentException("Could not get CRL from data", e);
        }


        return crl;
    }
}
