package sk.stu.fiit.sipvs1.Controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sk.stu.fiit.sipvs1.Model.Person;
import sk.stu.fiit.sipvs1.Service.PersonService;

import java.util.logging.Logger;

@Controller
public class PersonController {

    public static final Logger LOGGER = Logger.getLogger("PersonController");

    @Autowired
    PersonService personService;

    @GetMapping("/")
    public String showPersonForm(Person person, Model model) {
        model.addAttribute("xsdFile", personService.getSampleXsd());
        model.addAttribute("xslFile", personService.getSampleXsl());
        model.addAttribute("pdfFile", personService.getSamplePdf());
        return "person-form";
    }

    @PostMapping("/process/save-to-xml")
    public String processPersonForm(@Valid Person person, BindingResult result, RedirectAttributes redirectAttributes, Model model) {
        if (result.hasErrors()) {
            return showPersonForm(person, model);
        }

        redirectAttributes.addFlashAttribute("person", person);
        return "redirect:/process/save-to-xml";
    }

    @GetMapping("/process/save-to-xml")
    @ResponseBody
    public void saveToXml(Person person, HttpServletResponse response) {
        personService.saveFormToXml(person, response);
    }

    @PostMapping("/process/validate-against-xsd")
    public String validateAgainstXsd(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        return personService.validateXmlAgainstXsd(file, redirectAttributes);
    }

    @PostMapping("/process/transform-xml-to-html")
    public void transformXmlToHtml(@RequestParam("file") MultipartFile file, HttpServletResponse response) {
        personService.transformXmlToHtml(file, response);
    }

    @PostMapping("/process/timestamp-signature")
    public void addTimestampToSignature(@RequestParam("file") MultipartFile file, HttpServletResponse response) {
        personService.addTimestampToSignature(file, response);
    }
}
