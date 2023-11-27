package sk.stu.fiit.sipvs1.Service.validators;

import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import sk.stu.fiit.sipvs1.Model.InvalidDocumentException;
import sk.stu.fiit.sipvs1.Service.ValidatorUtils;

import java.util.logging.Logger;

@Service
public class EnvelopeValidator {

    public static final Logger LOGGER = Logger.getLogger("EnvelopeValidator");
    public static final String PREFIX_ER = "ENVELOPE | ERROR : ";
    public static final String PREFIX_OK = "ENVELOPE | OK : ";

    public void validate(Document document) throws InvalidDocumentException{
        Element root = document.getDocumentElement();

        if (!ValidatorUtils.checkAttributeValueS(root, "xmlns:xzep", "http://www.ditec.sk/ep/signature_formats/xades_zep/v1.0")) {
            invalidateDocument("Root element must have attribute xmlns:xzep with value set=http://www.ditec.sk/ep/signature_formats/xades_zep/v1.0");
        }

        logOk("xmlns:xzep");

        if (!ValidatorUtils.checkAttributeValue(root, "xmlns:ds", "http://www.w3.org/2000/09/xmldsig#")) {
            invalidateDocument("ENVELOPE VALIDATION | ERROR : Root element must have attribute xmlns:ds with value set=http://www.w3.org/2000/09/xmldsig#");
        }

        logOk("xmlns:ds");
    }

    private void logOk(String x) {
        LOGGER.info(PREFIX_OK + x);
    }

    private void invalidateDocument(String s) throws InvalidDocumentException {
        throw new InvalidDocumentException(PREFIX_ER + s);
    }
}
