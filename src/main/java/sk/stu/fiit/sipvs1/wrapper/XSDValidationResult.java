package sk.stu.fiit.sipvs1.wrapper;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class XSDValidationResult {
    private boolean isValid;
    private Exception exception = null;
}
