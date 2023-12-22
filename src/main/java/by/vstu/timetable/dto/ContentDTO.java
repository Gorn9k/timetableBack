package by.vstu.timetable.dto;

import com.sun.istack.NotNull;
import lombok.*;

import javax.validation.constraints.NotEmpty;
import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@RequiredArgsConstructor
@NoArgsConstructor
public class ContentDTO {

    @NotNull
    private LocalDate lessonDate;

    @NotNull
    private Integer lessonNumber;

    @NotNull
    private Integer subGroup;

    @NotNull
    private String frame;

    @NotNull
    private String location;

    @NotEmpty
    private String disciplineName;

    @NotEmpty
    private String typeClassName;

    @NotEmpty
    private String groupName;

    @NotEmpty
    private String teacherFio;

    private Long departmentId;

}
