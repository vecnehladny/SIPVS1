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

    public void verify(Document document) throws InvalidDocumentException{
        Element root = document.getDocumentElement();

        if (!ValidatorUtils.checkAttributeValueS(root, "xmlns:xzep", "http://www.ditec.sk/ep/signature_formats/xades_zep/v1.0")) {
            throw new InvalidDocumentException(PREFIX_ER + "Root element must have attribute xmlns:xzep with value set=http://www.ditec.sk/ep/signature_formats/xades_zep/v1.0");
        }

        LOGGER.info(PREFIX_OK + "xmlns:xzep");

        if (!ValidatorUtils.checkAttributeValue(root, "xmlns:ds", "http://www.w3.org/2000/09/xmldsig#")) {
            throw new InvalidDocumentException(PREFIX_ER + "ENVELOPE VALIDATION | ERROR : Root element must have attribute xmlns:ds with value set=http://www.w3.org/2000/09/xmldsig#");
        }

        LOGGER.info( PREFIX_OK + "xmlns:ds");
    }
}
