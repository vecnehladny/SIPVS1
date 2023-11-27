package sk.stu.fiit.sipvs1.Service.validators;

import org.bouncycastle.jce.provider.X509CertificateObject;
import org.bouncycastle.tsp.TimeStampToken;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import sk.stu.fiit.sipvs1.Model.InvalidDocumentException;
import sk.stu.fiit.sipvs1.Service.ValidatorUtils;

import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509CRL;
import java.security.cert.X509CRLEntry;
import java.util.logging.Logger;

@Service
public class CertificateValidator {

    public static final Logger LOGGER = Logger.getLogger("CertificateValidator");
    public static final String PREFIX_ER = "CERTIFICATE | ERROR : ";
    public static final String PREFIX_OK = "CERTIFICATE | OK : ";

    public void verify(Document document) throws InvalidDocumentException {
        X509CRL crl = ValidatorUtils.getCRL();
        TimeStampToken timeStampToken = ValidatorUtils.getTimestampToken(document);
        X509CertificateObject certificateObject = ValidatorUtils.getCertificate(document);

        try {
            certificateObject.checkValidity(timeStampToken.getTimeStampInfo()
                                                          .getGenTime());
        } catch (CertificateExpiredException e) {
            throw new InvalidDocumentException(PREFIX_ER + "The certificate was expired at the time of signing", e);
        } catch (CertificateNotYetValidException e) {
            throw new InvalidDocumentException(PREFIX_ER + "The certificate was not yet valid at the time of signing", e);
        }

        X509CRLEntry entry = crl.getRevokedCertificate(certificateObject.getSerialNumber());
        if (entry != null && timeStampToken.getTimeStampInfo()
                                           .getGenTime()
                                           .after(entry.getRevocationDate())) {
            throw new InvalidDocumentException(PREFIX_ER + "The certificate was revoked at the time of signing");
        }

        LOGGER.info(PREFIX_OK);
    }
}
