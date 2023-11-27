package sk.stu.fiit.sipvs1.Service.validators;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.util.Store;
import org.bouncycastle.util.encoders.Base64;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import sk.stu.fiit.sipvs1.Model.InvalidDocumentException;
import sk.stu.fiit.sipvs1.Service.ValidatorUtils;
import sk.stu.fiit.sipvs1.Service.XPathUtils;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.X509CRL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.logging.Logger;

@Service
public class TimestampValidator {

    public static final Logger LOGGER = Logger.getLogger("TimestampValidator");
    public static final String PREFIX_ER = "TIMESTAMP | ERROR : ";
    public static final String PREFIX_OK = "TIMESTAMP | OK : ";

    public void validate(Document document) throws InvalidDocumentException {
        X509CRL crl = ValidatorUtils.getCRL();
        TimeStampToken token = ValidatorUtils.getTimestampToken(document);
        verifyTimestampCerfificate(crl, token);
        verifyMessageImprint(token, document);
        logOk("References ds:Manifest");
    }


    private void verifyTimestampCerfificate(X509CRL crl, TimeStampToken tsToken) throws InvalidDocumentException {
        X509CertificateHolder signer = null;
        Store<X509CertificateHolder> certHolders = tsToken.getCertificates();
        ArrayList<X509CertificateHolder> certList = new ArrayList<>(certHolders.getMatches(null));
        BigInteger serialNumToken = tsToken.getSID()
                                           .getSerialNumber();
        X500Name issuerToken = tsToken.getSID()
                                      .getIssuer();

        for (X509CertificateHolder certHolder : certList) {
            if (certHolder.getSerialNumber()
                          .equals(serialNumToken) && certHolder.getIssuer()
                                                               .equals(issuerToken)) {
                signer = certHolder;
                break;
            }
        }

        if (null == signer) {
            invalidateDocument("Signed certificate of TS is not found in document");
        }

        if (!signer.isValidOn(new Date())) {
            invalidateDocument("Signed certificate of TS is not valid compared to current time");
        }

        logOk("TS compared to UTC NOW");

        if (null != crl.getRevokedCertificate(signer.getSerialNumber())) {
            invalidateDocument("Signed certificate of TS is not valid compared to last valid CRL");
        }

        logOk("TS compared to last valid CRL");

    }


    private void verifyMessageImprint(TimeStampToken tsToken, Document document) throws InvalidDocumentException {
        byte[] messageImprint = tsToken.getTimeStampInfo()
                                       .getMessageImprintDigest();
        String hashAlg = tsToken.getTimeStampInfo()
                                .getHashAlgorithm()
                                .getAlgorithm()
                                .getId();
        Node signatureValueNode = XPathUtils.selectSingleNode(document, "//ds:Signature/ds:SignatureValue");

        if (signatureValueNode == null) {
            invalidateDocument("Element ds:SignatureValue not found");
        }

        byte[] signatureValue = Base64.decode(signatureValueNode.getTextContent()
                                                                .getBytes());

        MessageDigest messageDigest = null;
        try {
            messageDigest = MessageDigest.getInstance(hashAlg, "BC");
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            invalidateDocument("Not supported algorithm in message digest", e);
        }

        if (!Arrays.equals(messageImprint, messageDigest.digest(signatureValue))) {
            invalidateDocument("MessageImprint from TS and signature ds:SignatureValue does not match");
        }

        logOk("MessageImprint from TS compared to ds:SignatureValue");
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
