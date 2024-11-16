package by.vstu.dean.timetable.dto;

import by.vstu.dean.core.dto.BaseDTO;
import by.vstu.dean.support.models.ClassroomModel;
import by.vstu.dean.support.models.DisciplineModel;
import by.vstu.dean.support.models.GroupModel;
import by.vstu.dean.support.models.TeacherModel;
import by.vstu.dean.timetable.enums.ELessonTypes;
import by.vstu.dean.timetable.enums.ESubGroup;
import by.vstu.dean.timetable.enums.EWeekType;
import by.vstu.dean.timetable.models.LessonModel;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;

/**
 * DTO for {@link LessonModel}
 */
@Getter
@Setter
@ToString
public class LessonDTO extends BaseDTO {

    GroupModel group;
    TeacherModel teacher;
    DisciplineModel discipline;
    ClassroomModel classroom;

    short day;
    short lessonNumber;
    ELessonTypes lessonType;
    EWeekType weekType;
    @NotNull
    ESubGroup subGroup;

    private LocalDate startDate;
    private LocalDate endDate;

    boolean visible;

}
