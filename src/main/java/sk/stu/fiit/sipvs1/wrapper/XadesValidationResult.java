package sk.stu.fiit.sipvs1.wrapper;

import lombok.*;
import sk.stu.fiit.sipvs1.Model.InvalidDocumentException;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class XadesValidationResult {
    private String filename;
    private String fileContent;
    private String resultClass;
    private String message;
    private InvalidDocumentException exception;
}
