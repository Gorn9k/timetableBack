package by.vstu.timetable.repo;

import by.vstu.dean.enums.ELessonType;
import by.vstu.dean.future.DBBaseModelRepository;
import by.vstu.timetable.enums.ESubGroup;
import by.vstu.timetable.enums.EWeekType;
import by.vstu.timetable.models.LessonModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LessonModelRepository extends DBBaseModelRepository<LessonModel> {
    boolean existsByRoomIdAndTeacherIdAndWeekTypeAndDayAndLessonNumberAndLessonType(long roomId, long teacherId, EWeekType weekType, short day, short lessonNumber, ELessonType lessonType);
    List<LessonModel> findAllByRoomId(Long roomId);
    boolean existsByRoomIdAndLessonNumberAndDayAndWeekType(long roomId, short lessonNumber, short day, EWeekType weekType);

    boolean existsByRoomIdAndLessonNumberAndDay(long roomId, short lessonNumber, short day);

    boolean existsByTeacherIdAndLessonNumberAndDay(long teacherId, short lessonNumber, short day);

    boolean existsByTeacherIdAndLessonNumberAndDayAndWeekType(long teacherId, short lessonNumber, short day, EWeekType weekType);

    boolean existsByRoomIdAndTeacherIdAndDisciplineIdAndGroupIdAndSubGroupAndDayAndLessonNumberAndLessonTypeAndWeekType(long roomId, long teacherId, long disciplineId, long groupId, ESubGroup subGroup, short day, short lessonNumber, ELessonType lessonType, EWeekType weekType);
}