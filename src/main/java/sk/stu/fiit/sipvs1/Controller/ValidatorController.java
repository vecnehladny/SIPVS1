package sk.stu.fiit.sipvs1.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import sk.stu.fiit.sipvs1.Service.ValidatorService;
import sk.stu.fiit.sipvs1.wrapper.XadesValidationResult;

import java.util.List;
import java.util.logging.Logger;

@Controller()
public class ValidatorController {

    public static final Logger LOGGER = Logger.getLogger("ValidatorController");

    @Autowired
    ValidatorService validatorService;

    @GetMapping("/validator")
    public String handleFileUpload() {
        return "validator";
    }

    @PostMapping("/validator/upload")
    public ResponseEntity<?> handleFileUpload(@RequestParam("files") List<MultipartFile> files) {

        for (MultipartFile file : files) {
            LOGGER.info("Received file: " + file.getOriginalFilename());
        }

        List<XadesValidationResult> results = validatorService.validateDocuments(files);

        return ResponseEntity.ok(results);
    }
}
