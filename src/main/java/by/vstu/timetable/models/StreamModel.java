package by.vstu.timetable.models;

import by.vstu.dean.enums.ELessonType;
import by.vstu.dean.future.DBBaseModel;
import by.vstu.dean.future.models.lessons.DisciplineModel;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Set;


@Setter
@Getter
public class StreamModel extends DBBaseModel {


    ELessonType lessonType;
    short day;
    short lessonNumber;
    short streamCourse;
    Long roomId;
    Long teacherId;
    Long disciplineId;
    Set<Long> groups;

    public void add(Long groupId) {
        this.groups.add(groupId);
    }

}
