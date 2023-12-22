package by.vstu.timetable.dto;

import by.vstu.dean.dto.BaseDTO;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PatternDTO extends BaseDTO {

    private String lessonDay;

    private Boolean numerator;

    private Integer weekNumber;

    private Integer lessonNumber;

    private Integer subGroup;

    private String frame;

    private String location;

    private String disciplineName;

    private String typeClassName;

    private String groupName;

    private String teacherFio;

    private String departmentTeacherDisplayName;

    private Long departmentId;

}
