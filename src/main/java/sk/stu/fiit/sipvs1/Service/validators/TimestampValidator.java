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

    public void verify(Document document) throws InvalidDocumentException {
        X509CRL crl = ValidatorUtils.getCRL();
        TimeStampToken token = ValidatorUtils.getTimestampToken(document);
        verifyTimestampCerfificate(crl, token);
        verifyMessageImprint(token, document);
        LOGGER.info(PREFIX_OK + "References ds:Manifest");
    }


    private void verifyTimestampCerfificate(X509CRL crl, TimeStampToken ts_token) throws InvalidDocumentException {
        X509CertificateHolder signer = null;
        Store<X509CertificateHolder> certHolders = ts_token.getCertificates();
        ArrayList<X509CertificateHolder> certList = new ArrayList<>(certHolders.getMatches(null));
        BigInteger serialNumToken = ts_token.getSID()
                                            .getSerialNumber();
        X500Name issuerToken = ts_token.getSID()
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
            throw new InvalidDocumentException(PREFIX_ER + "Signed certificate of TS is not found in document");
        }

        if (!signer.isValidOn(new Date())) {
            throw new InvalidDocumentException(PREFIX_ER + "Signed certificate of TS is not valid compared to current time");
        }

        LOGGER.info(PREFIX_OK + "TS compared to UTC NOW");

        if (null != crl.getRevokedCertificate(signer.getSerialNumber())) {
            throw new InvalidDocumentException(PREFIX_ER + "Signed certificate of TS is not valid compared to last valid CRL");
        }

        LOGGER.info(PREFIX_OK + "TS compared to last valid CRL");

    }


    private void verifyMessageImprint(TimeStampToken ts_token, Document document) throws InvalidDocumentException {
        byte[] messageImprint = ts_token.getTimeStampInfo()
                                        .getMessageImprintDigest();
        String hashAlg = ts_token.getTimeStampInfo()
                                 .getHashAlgorithm()
                                 .getAlgorithm()
                                 .getId();
        Node signatureValueNode = XPathUtils.selectSingleNode(document, "//ds:Signature/ds:SignatureValue");

        if (signatureValueNode == null) {
            throw new InvalidDocumentException(PREFIX_ER + "Element ds:SignatureValue not found");
        }

        byte[] signatureValue = Base64.decode(signatureValueNode.getTextContent()
                                                                .getBytes());

        MessageDigest messageDigest = null;
        try {
            messageDigest = MessageDigest.getInstance(hashAlg, "BC");
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new InvalidDocumentException(PREFIX_ER + "Not supported algorithm in message digest", e);
        }

        if (!Arrays.equals(messageImprint, messageDigest.digest(signatureValue))) {
            throw new InvalidDocumentException(PREFIX_ER + "MessageImprint from TS and signature ds:SignatureValue does not match");
        }

        LOGGER.info(PREFIX_OK + "MessageImprint from TS compared to ds:SignatureValue");
    }
}
