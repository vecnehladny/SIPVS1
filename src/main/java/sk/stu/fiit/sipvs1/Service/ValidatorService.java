package sk.stu.fiit.sipvs1.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import sk.stu.fiit.sipvs1.Model.InvalidDocumentException;
import sk.stu.fiit.sipvs1.Service.validators.CertificateValidator;
import sk.stu.fiit.sipvs1.Service.validators.EnvelopeValidator;
import sk.stu.fiit.sipvs1.Service.validators.TimestampValidator;
import sk.stu.fiit.sipvs1.Service.validators.XMLSignatureValidator;
import sk.stu.fiit.sipvs1.wrapper.XadesValidationResult;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class ValidatorService {

    private static final Logger LOGGER = Logger.getLogger("ValidatorService");

    @Autowired
    CertificateValidator certificateValidator;

    @Autowired
    EnvelopeValidator envelopeValidator;

    @Autowired
    TimestampValidator timestampValidator;

    @Autowired
    XMLSignatureValidator xmlSignatureValidator;

    public List<XadesValidationResult> validateDocuments(List<MultipartFile> files) {

        List<XadesValidationResult> results = new ArrayList<>();

        for (MultipartFile f : files) {
            XadesValidationResult xadesValidationResult = new XadesValidationResult();
            xadesValidationResult.setFilename(f.getOriginalFilename());

            String documentContent = null;
            Document document = null;
            try {
                documentContent = readFile(f);
                documentContent = sanitizeFile(documentContent);
                document = convertToDocument(documentContent);

            } catch (ParserConfigurationException | IOException | SAXException e) {
                xadesValidationResult.setResultClass(ValidatorConstants.DANGER);
                if(e instanceof IOException) {
                    LOGGER.log(Level.SEVERE, String.format("Can't open or read %s", f.getName()), e);
                    xadesValidationResult.setMessage("Can't open or read file");
                } else {
                    LOGGER.log(Level.SEVERE, String.format("Can't parse %s content into org.w3c.dom.Document", f.getName()), e);
                    xadesValidationResult.setMessage("Can't parse file's content into or.w3c.dom.Document");
                }
                results.add(xadesValidationResult);
                continue;
            }

            try {
                validate(document);
                LOGGER.info("Document " + f.getOriginalFilename() + " is valid");
                xadesValidationResult.setResultClass("success");
                xadesValidationResult.setMessage("Valid");
            } catch (InvalidDocumentException e) {
                LOGGER.log(Level.SEVERE, e.getMessage());
                LOGGER.log(Level.SEVERE, String.format("Document %s is not valid", f.getOriginalFilename()));
                xadesValidationResult.setResultClass(ValidatorConstants.DANGER);
                xadesValidationResult.setMessage("Invalid");
                xadesValidationResult.setException(e);
            }
            results.add(xadesValidationResult);
        }

        return results;

    }

    private void validate(Document document) throws InvalidDocumentException {
        envelopeValidator.validate(document);
        xmlSignatureValidator.validate(document);
        timestampValidator.validate(document);
        certificateValidator.validate(document);

    }


    private String readFile(MultipartFile file) throws IOException {
        return new String(file.getInputStream().readAllBytes());
    }

    private String sanitizeFile(String s) {
        if (s.startsWith(ValidatorConstants.UTF8_BOM)) {
            s = s.substring(1);
        }

        if (!s.startsWith("<?xml")) {
            s = ValidatorConstants.XML_HEADER + s;
        }
        return s;
    }

    private Document convertToDocument(String s) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
        documentFactory.setNamespaceAware(true);
        DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
        InputSource source = new InputSource(new StringReader(s));
        return documentBuilder.parse(source);
    }
}
