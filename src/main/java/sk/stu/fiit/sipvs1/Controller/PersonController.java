package sk.stu.fiit.sipvs1.Controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sk.stu.fiit.sipvs1.Model.Child;
import sk.stu.fiit.sipvs1.Model.Person;
import sk.stu.fiit.sipvs1.Service.PersonService;
import sk.stu.fiit.sipvs1.wrapper.Alert;
import sk.stu.fiit.sipvs1.wrapper.XSDValidationResult;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.net.URLConnection;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

@Controller
public class PersonController {

    public static final Logger LOGGER = Logger.getLogger("PersonController");

    @Autowired
    PersonService personService;

    @GetMapping("/")
    public String showPersonForm(Person person) {
        return "person-form";
    }

    @PostMapping("/process/save-to-xml")
    public String processPersonForm(@Valid Person person, BindingResult result, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return showPersonForm(person);
        }

        redirectAttributes.addFlashAttribute("person", person);
        return "redirect:/process/save-to-xml";
    }

    @GetMapping("/process/save-to-xml")
    @ResponseBody
    public void saveToXml(Person person, HttpServletResponse response) {
        File personAsXmlFile = personService.getPersonAsXmlFile(person);
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

    @PostMapping("/process/validate-against-xsd")
    public String validateAgainstXsd(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        XSDValidationResult validationResult = personService.validateXmlAgainstXsd(file);
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

    @PostMapping("/process/transform-xml-to-html")
    public void transformXmlToHtml(@RequestParam("file") MultipartFile file, HttpServletResponse response) {
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
        } catch (IOException | TransformerException e) {
            LOGGER.log(Level.SEVERE, "Failed send a file", e);
        }
    }

    private Person getSamplePerson() {
        return Person.builder()
                     .personID("1")
                     .name("Viktor")
                     .age(12)
                     .email("email@gmail.com")
                     .birthDate(LocalDate.now())
                     .children(Arrays.asList(Child.builder()
                                                  .firstName("Decko")
                                                  .lastName("1")
                                                  .build(),
                                             Child.builder()
                                                  .firstName("Decko")
                                                  .lastName("2")
                                                  .build()))
                     .build();
    }
}
