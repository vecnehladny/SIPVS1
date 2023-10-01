package sk.stu.fiit.sipvs1.Service;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;
import sk.stu.fiit.sipvs1.Model.Person;
import sk.stu.fiit.sipvs1.wrapper.XSDValidationResult;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class PersonService {

    public static final Logger LOGGER = Logger.getLogger("PersonService");
    public static final String DEFAULT_FILE_NAME = "sample_file.xml";

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


}
