package by.vstu.timetable.dto;

import by.vstu.dean.dto.BaseDTO;
import by.vstu.dean.enums.ELessonType;
import by.vstu.dean.future.DBBaseModel;
import by.vstu.dean.future.models.lessons.DisciplineModel;
import by.vstu.dean.future.models.lessons.TeacherModel;
import by.vstu.dean.future.models.rooms.ClassroomModel;
import by.vstu.dean.future.models.students.GroupModel;
import by.vstu.timetable.enums.ESubGroup;
import by.vstu.timetable.enums.EWeekType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.time.LocalDate;
import java.util.Calendar;

/**
 * DTO for {@link by.vstu.timetable.models.LessonModel}
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
    ELessonType lessonType;
    EWeekType weekType;
    ESubGroup subGroup;

    private LocalDate startDate;
    private LocalDate endDate;


}
