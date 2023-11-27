package sk.stu.fiit.sipvs1.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import sk.stu.fiit.sipvs1.Model.InvalidDocumentException;
import sk.stu.fiit.sipvs1.Service.validators.CertificateValidator;
import sk.stu.fiit.sipvs1.Service.validators.EnvelopeValidator;
import sk.stu.fiit.sipvs1.Service.validators.TimestampValidator;
import sk.stu.fiit.sipvs1.Service.validators.XMLSignatureValidator;
import sk.stu.fiit.sipvs1.wrapper.XadesValidationResult;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class ValidatorService {

    public final Logger LOGGER = Logger.getLogger("ValidatorService");

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
            try {
                documentContent = readFile(f);
                documentContent = removeUTF8BOM(documentContent);
                documentContent = addXMLHeader(documentContent);

            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Can't open or read " + f.getName(), e);
                xadesValidationResult.setResultClass("danger");
                xadesValidationResult.setMessage("Can't open or read file");
                results.add(xadesValidationResult);
                continue;
            }


            Document document = null;
            try {
                document = convertToDocument(documentContent);

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Can't parse " + f.getName() + " content into org.w3c.dom.Document", e);
                xadesValidationResult.setResultClass("danger");
                xadesValidationResult.setMessage("Can't parse file's content into or.w3c.dom.Document");
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
                LOGGER.log(Level.SEVERE, "Document " + f.getOriginalFilename() + " is not valid");
                xadesValidationResult.setResultClass("danger");
                xadesValidationResult.setMessage("Invalid");
                xadesValidationResult.setException(e);
            }
            results.add(xadesValidationResult);
        }

        return results;

    }

    private void validate(Document document) throws InvalidDocumentException {
        envelopeValidator.verify(document);
        xmlSignatureValidator.verify(document);
        timestampValidator.verify(document);
        certificateValidator.verify(document);

    }


    private String readFile(MultipartFile file) throws IOException {
        return new String(file.getInputStream().readAllBytes());
    }

    private String removeUTF8BOM(String s) {
        if (s.startsWith(ValidatorConstants.UTF8_BOM)) {
            LOGGER.fine("Contains UTF8 BOM");
            s = s.substring(1);
        }
        return s;
    }

    private String addXMLHeader(String s) {
        if (!s.startsWith("<?xml")) {
            s = ValidatorConstants.XML_HEADER + s;
        }
        return s;
    }


    private Document convertToDocument(String s) throws Exception {
        DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
        documentFactory.setNamespaceAware(true);
        DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
        InputSource source = new InputSource(new StringReader(s));
        return documentBuilder.parse(source);
    }
}
