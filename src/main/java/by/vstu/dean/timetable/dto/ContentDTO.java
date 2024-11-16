package by.vstu.dean.timetable.dto;

import by.vstu.dean.core.dto.BaseDTO;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@RequiredArgsConstructor
public class ContentDTO extends BaseDTO {

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
