package by.vstu.timetable.models;

import by.vstu.dean.enums.ELessonType;
import by.vstu.dean.future.DBBaseModel;
import by.vstu.timetable.enums.ESubGroup;
import by.vstu.timetable.enums.EWeekType;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "lessons")
@Setter
@Getter
public class LessonModel extends DBBaseModel {

    long groupId;

    long teacherId;
    long disciplineId;
    long roomId;
    short lessonNumber;
    short day;
    @Enumerated(EnumType.ORDINAL)
    ELessonType lessonType;
    @Enumerated(EnumType.ORDINAL)
    EWeekType weekType;
    @Enumerated(EnumType.ORDINAL)
    ESubGroup subGroup;
    LocalDate startDate;
    LocalDate endDate;

}
