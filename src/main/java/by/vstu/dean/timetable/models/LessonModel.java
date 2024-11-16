package by.vstu.dean.timetable.models;

import by.vstu.dean.core.models.DBBaseModel;
import by.vstu.dean.timetable.enums.ELessonTypes;
import by.vstu.dean.timetable.enums.ESubGroup;
import by.vstu.dean.timetable.enums.EWeekType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "lessons")
@Setter
@Getter
public class LessonModel extends DBBaseModel {

    Long groupId;
    Long teacherId;
    Long disciplineId;
    Long roomId;
    Short lessonNumber;
    Short day;
    @Enumerated(EnumType.ORDINAL)
    ELessonTypes lessonType;
    @Enumerated(EnumType.ORDINAL)
    EWeekType weekType;
    @Enumerated(EnumType.ORDINAL)
    ESubGroup subGroup;
    LocalDate startDate;
    LocalDate endDate;
    boolean visible;

}
