package sk.stu.fiit.sipvs1.Model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JacksonXmlRootElement(namespace = "http://www.example.com")
public class Person {


    @NotEmpty(message = "can't be empty")
    @Size(min = 8, max = 8, message = "needs to be 8 characters long")
    @Pattern(regexp = "[a-zA-Z0-9]{8}", message = "must include only alphanumerical characters")
    @JacksonXmlProperty(isAttribute = true)
    private String personID;

    @NotEmpty(message = "can't be empty")
    @Size(min = 1, max = 50, message = "should have at least 1 character and no more than 50")
    private String name;

    @NotNull(message = "can't be empty")
    @Min(value = 18, message = "must be between 18 and 99")
    @Max(value = 99)
    private Integer age;

    @NotEmpty(message = "can't be empty")
    @Email
    private String email;

    @NotNull(message = "can't be empty")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    @JacksonXmlElementWrapper(useWrapping=false)
    private List<Child> children;

//    @JacksonXmlProperty(isAttribute = true)
//    private final String xmlns = "http://example.com";
}
