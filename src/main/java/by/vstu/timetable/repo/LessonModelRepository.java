package by.vstu.timetable.repo;

import by.vstu.dean.enums.ELessonType;
import by.vstu.dean.enums.EStatus;
import by.vstu.dean.future.DBBaseModelRepository;
import by.vstu.timetable.enums.ESubGroup;
import by.vstu.timetable.enums.EWeekType;
import by.vstu.timetable.models.LessonModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LessonModelRepository extends DBBaseModelRepository<LessonModel> {
    List<LessonModel> findAllByRoomId(Long roomId);


    boolean existsByRoomIdAndTeacherIdAndWeekTypeAndDayAndLessonNumberAndLessonTypeAndStatusAndIdNot(long roomId, long teacherId, EWeekType weekType, short day, short lessonNumber, ELessonType lessonType, EStatus status, Long id);

    boolean existsByRoomIdAndLessonNumberAndDayAndWeekTypeAndStatusAndIdNot(long roomId, short lessonNumber, short day, EWeekType weekType, EStatus status, Long id);

    boolean existsByRoomIdAndLessonNumberAndDayAndStatusAndIdNot(long roomId, short lessonNumber, short day, EStatus status, Long id);

    boolean existsByTeacherIdAndLessonNumberAndDayAndStatusAndIdNot(long teacherId, short lessonNumber, short day, EStatus status, Long id);

    boolean existsByTeacherIdAndLessonNumberAndDayAndWeekTypeAndStatusAndIdNot(long teacherId, short lessonNumber, short day, EWeekType weekType, EStatus status, Long id);

    boolean existsByRoomIdAndTeacherIdAndDisciplineIdAndGroupIdAndSubGroupAndDayAndLessonNumberAndLessonTypeAndWeekTypeAndStatusAndIdNot(long roomId, long teacherId, long disciplineId, long groupId, ESubGroup subGroup, short day, short lessonNumber, ELessonType lessonType, EWeekType weekType, EStatus status, Long id);

    boolean existsByTeacherIdAndDisciplineIdAndWeekTypeAndDayAndLessonNumberAndLessonTypeAndStatusAndRoomIdNotAndIdNot(long teacherId, long disciplineId, EWeekType weekType, short day, short lessonNumber, ELessonType lessonType, EStatus status, long roomId, Long id);

    List<LessonModel> findByTeacherIdAndDisciplineIdAndGroupIdAndDayAndWeekTypeAndLessonNumberAndLessonTypeAndSubGroupAndStatus(long teacherId, long disciplineId, long groupId, short day, EWeekType weekType, short lessonNumber, ELessonType lessonType, ESubGroup subGroup, EStatus status);

    List<LessonModel> findByGroupIdAndDayAndWeekTypeAndLessonNumberAndStatus(long groupId, short day, EWeekType weekType, short lessonNumber, EStatus status);
}