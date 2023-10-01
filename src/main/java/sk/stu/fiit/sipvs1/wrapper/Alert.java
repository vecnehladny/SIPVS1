package sk.stu.fiit.sipvs1.wrapper;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alert {
    private String type;
    private String message;
    private Exception exception;
}
