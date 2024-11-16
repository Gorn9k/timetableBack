package by.vstu.dean.timetable.models;


import by.vstu.dean.core.models.DBBaseModel;
import by.vstu.dean.support.enums.ELessonType;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Set;


@Setter
@Getter
@Deprecated
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
