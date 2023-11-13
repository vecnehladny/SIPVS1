package sk.stu.fiit.sipvs1.Service;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.tomcat.util.codec.binary.Base64;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.tsp.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.xml.sax.SAXException;
import sk.stu.fiit.sipvs1.Model.Person;
import sk.stu.fiit.sipvs1.wrapper.Alert;
import sk.stu.fiit.sipvs1.wrapper.XSDValidationResult;

import javax.xml.XMLConstants;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class PersonService {

    public static final Logger LOGGER = Logger.getLogger("PersonService");
    public static final String DEFAULT_FILE_NAME = "sample_file.xml";
    public static final String SIGNATURE_START = "<ds:SignatureValue Id=\"signatureIdSignatureValue\" xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\" xmlns:xzep=\"http://www.ditec.sk/ep/signature_formats/xades_zep/v2.0\">";
    public static final String SIGNATURE_END = "</ds:SignatureValue>";

    @Value("${tsa.url}")
    String tsaUrl;

    public void saveFormToXml(Person person, HttpServletResponse response) {
        File personAsXmlFile = getPersonAsXmlFile(person);
        if(null != personAsXmlFile) {
            try {
                String mimeType = URLConnection.guessContentTypeFromName(personAsXmlFile.getName());
                String contentDisposition = String.format("attachment; filename=%s", personAsXmlFile.getName());
                int fileSize = Long.valueOf(personAsXmlFile.length())
                                   .intValue();
                response.setContentType(mimeType);
                response.setHeader("Content-Disposition", contentDisposition);
                response.setContentLength(fileSize);
                OutputStream out = response.getOutputStream();
                Files.copy(personAsXmlFile.toPath(), out);
                out.flush();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed send a file", e);
            }
        }
    }

    public File getPersonAsXmlFile(Person person) {
        String fileName = null != person.getName() ? getFileNameFromName(person) : DEFAULT_FILE_NAME;
        try {
            xmlMapper().writeValue(new File(fileName), person);
            return new File(fileName);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to create a file", e);
        }
        return null;
    }

    public String validateXmlAgainstXsd(MultipartFile file, RedirectAttributes redirectAttributes) {
        XSDValidationResult validationResult = validateXmlAgainstXsd(file);
        String message;
        String type;
        Exception exception = null;
        if (validationResult.isValid()) {
            message = "Validation successful for file ".concat(file.getOriginalFilename());
            type = "success";
        } else {
            message = "Validation failed for file ".concat(file.getOriginalFilename());
            type = "danger";
            exception = validationResult.getException();
        }
        redirectAttributes.addFlashAttribute("alerts",
                                             Collections.singletonList(Alert.builder()
                                                                            .message(message)
                                                                            .type(type)
                                                                            .exception(exception)
                                                                            .build()));
        return "redirect:/";
    }

    public XSDValidationResult validateXmlAgainstXsd(MultipartFile file) {
        boolean isValid = false;
        Exception exception = null;
        if (!file.isEmpty()) {
            try {
                SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                Schema schema = factory.newSchema(new File("src/main/resources/sample.xsd"));
                Validator validator = schema.newValidator();
                validator.validate(new StreamSource(file.getInputStream()));
                isValid = true;
            } catch (IOException | SAXException e) {
                LOGGER.log(Level.SEVERE, "Failed to validate XML against XSD", e);
                exception = e;
            }
        }
        return XSDValidationResult.builder()
                                  .isValid(isValid)
                                  .exception(exception)
                                  .build();
    }

    public void transformXmlToHtml(MultipartFile file, HttpServletResponse response) {
        try {
            File xslStylesheet = new File("src/main/resources/sample.xsl");
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer(new StreamSource(xslStylesheet));
            File htmlFile = new File("index.html");
            FileWriter fileWriter = new FileWriter(htmlFile);
            transformer.transform(new StreamSource(file.getInputStream()), new StreamResult(fileWriter));
            fileWriter.close();
            String mimeType = URLConnection.guessContentTypeFromName(htmlFile.getName());
            String contentDisposition = String.format("attachment; filename=%s", htmlFile.getName());
            int fileSize = Long.valueOf(htmlFile.length())
                               .intValue();
            response.setContentType(mimeType);
            response.setHeader("Content-Disposition", contentDisposition);
            response.setContentLength(fileSize);
            OutputStream out = response.getOutputStream();
            Files.copy(htmlFile.toPath(), out);
            out.flush();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to send a file", e);
        }
    }

    public void addTimestampToSignature(MultipartFile file, HttpServletResponse response) {
        try {
            String xmlSignature = new String(file.getInputStream().readAllBytes());
            byte[] timestampToken = getTimestampFromTSA(xmlSignature);
            String unsignedProperties = "<xades:UnsignedProperties><xades:UnsignedSignatureProperties><xades:SignatureTimeStamp Id=\"signatureIdSignatureTimeStamp\"><xades:EncapsulatedTimeStamp>$TIMESTAMP$</xades:EncapsulatedTimeStamp></xades:SignatureTimeStamp></xades:UnsignedSignatureProperties></xades:UnsignedProperties>";
            unsignedProperties = unsignedProperties.replace("$TIMESTAMP$", Base64.encodeBase64String(timestampToken));
            xmlSignature = xmlSignature.replace("</xades:SignedProperties>", "</xades:SignedProperties>".concat(unsignedProperties));

            File timestampedSignatureFile = new File("xades_t_signature.xml");
            if (null != timestampedSignatureFile) {
                String mimeType = URLConnection.guessContentTypeFromName(timestampedSignatureFile.getName());
                String contentDisposition = String.format("attachment; filename=%s",
                                                          timestampedSignatureFile.getName());
                int fileSize = Long.valueOf(timestampedSignatureFile.length())
                                   .intValue();
                response.setContentType(mimeType);
                response.setHeader("Content-Disposition", contentDisposition);
                response.setContentLength(fileSize);
                OutputStream out = response.getOutputStream();
                FileWriter fw = new FileWriter(timestampedSignatureFile.getAbsoluteFile());
                BufferedWriter bw = new BufferedWriter(fw);
                bw.write(xmlSignature);
                bw.close();
                Files.copy(timestampedSignatureFile.toPath(), out);
                out.flush();
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error timestamping the signature", e);
        }
    }


    private byte[] getTimestampFromTSA(String signatureFile) throws IOException, TSPException {
        TimeStampRequestGenerator tsReqGen = new TimeStampRequestGenerator();
        tsReqGen.setCertReq(true);
        TimeStampRequest tsReq = tsReqGen.generate(TSPAlgorithms.SHA256, calculateMessageDigest(signatureFile));
        byte[] requestBytes = tsReq.getEncoded();
        byte[] responseBytes = sendTimeStampRequest(tsaUrl, requestBytes);
        TimeStampResponse tsResp = new TimeStampResponse(new ByteArrayInputStream(responseBytes));
        tsResp.validate(tsReq);
        return tsResp.getTimeStampToken().getEncoded();
    }

    private byte[] sendTimeStampRequest(String tsaUrl, byte[] requestBytes) throws IOException {
        URL url = new URL(tsaUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/timestamp-query");
        connection.setDoOutput(true);

        try (java.io.OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(requestBytes);
        }

        try (java.io.InputStream inputStream = connection.getInputStream()) {
            return inputStream.readAllBytes();
        } finally {
            connection.disconnect();
        }
    }

    private byte[] calculateMessageDigest(String signatureFile) {
        String toStamp = signatureFile;
        toStamp = toStamp.substring(toStamp.indexOf(SIGNATURE_START) + SIGNATURE_START.length());
        toStamp = toStamp.substring(0, toStamp.indexOf(SIGNATURE_END));

        SHA256Digest md = new SHA256Digest();
        byte[] bytes = toStamp.getBytes();
        String fileString = new String(bytes);
        byte[] dataBytes = fileString.getBytes();
        int nread = dataBytes.length;
        md.update(dataBytes, 0, nread);
        byte[] result = new byte[32];
        md.doFinal(result, 0);
        return result;
    }

    private XmlMapper xmlMapper() {
        return XmlMapper.builder()
                        .enable(SerializationFeature.INDENT_OUTPUT)
                        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                        .addModule(new JavaTimeModule())
                        .build();
    }

    private static String getFileNameFromName(Person person) {
        return person.getName()
                     .strip()
                     .toLowerCase()
                     .replace(" ", "")
                     .concat(".xml");
    }

    public String getSamplePdf() {
        try {
            byte[] file = Files.readAllBytes(Path.of("src/main/resources/sample.pdf"));
            byte[] encoded = Base64.encodeBase64(file, false);
            return new String(encoded, StandardCharsets.US_ASCII);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to read sample pdf file");
        }
        return null;
    }

    public String getSampleXsl() {
        return readFileToString("src/main/resources/sample.xsl");
    }

    public String getSampleXsd() {
        return readFileToString("src/main/resources/sample.xsd");
    }

    private String readFileToString(String path) {
        try {
            return Files.readString(Path.of(path));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to read file " + path);
        }
        return null;
    }
}
