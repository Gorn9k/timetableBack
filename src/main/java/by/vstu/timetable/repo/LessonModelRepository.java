package by.vstu.timetable.repo;

import by.vstu.dean.enums.ELessonType;
import by.vstu.dean.enums.EStatus;
import by.vstu.dean.future.DBBaseModelRepository;
import by.vstu.timetable.enums.ESubGroup;
import by.vstu.timetable.enums.EWeekType;
import by.vstu.timetable.models.LessonModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface LessonModelRepository extends DBBaseModelRepository<LessonModel> {
    List<LessonModel> findAllByRoomId(Long roomId);


    //    boolean existsByRoomIdAndTeacherIdAndWeekTypeAndDayAndLessonNumberAndLessonTypeAndStatusAndIdNot(long roomId, long teacherId, EWeekType weekType, short day, short lessonNumber, ELessonType lessonType, EStatus status, Long id);
//
//    boolean existsByRoomIdAndLessonNumberAndDayAndWeekTypeAndStatusAndIdNot(long roomId, short lessonNumber, short day, EWeekType weekType, EStatus status, Long id);
//
//    boolean existsByRoomIdAndLessonNumberAndDayAndStatusAndIdNot(long roomId, short lessonNumber, short day, EStatus status, Long id);
//
//    boolean existsByTeacherIdAndLessonNumberAndDayAndStatusAndIdNot(long teacherId, short lessonNumber, short day, EStatus status, Long id);
//
//    boolean existsByTeacherIdAndLessonNumberAndDayAndWeekTypeAndStatusAndIdNot(long teacherId, short lessonNumber, short day, EWeekType weekType, EStatus status, Long id);
//
//    boolean existsByTeacherIdAndDisciplineIdAndWeekTypeAndDayAndLessonNumberAndLessonTypeAndStatusAndRoomIdNotAndIdNot(long teacherId, long disciplineId, EWeekType weekType, short day, short lessonNumber, ELessonType lessonType, EStatus status, long roomId, Long id);
//
//    List<LessonModel> findByTeacherIdAndDisciplineIdAndGroupIdAndDayAndWeekTypeAndLessonNumberAndLessonTypeAndSubGroupAndStatus(long teacherId, long disciplineId, long groupId, short day, EWeekType weekType, short lessonNumber, ELessonType lessonType, ESubGroup subGroup, EStatus status);
//
//    List<LessonModel> findByGroupIdAndTeacherIdAndDisciplineIdAndDayAndLessonNumberAndLessonTypeAndWeekTypeAndSubGroupAndStatus(long groupId, long teacherId, long disciplineId, short day, short lessonNumber, ELessonType lessonType, EWeekType weekType, ESubGroup subGroup, EStatus status);
//
//    List<LessonModel> findByGroupIdAndTeacherIdAndDisciplineIdAndRoomIdAndDayAndLessonNumberAndWeekTypeAndSubGroupAndStatus(long groupId, long teacherId, long disciplineId, long roomId, short day, short lessonNumber, EWeekType weekType, ESubGroup subGroup, EStatus status);
    @Query(value = "SELECT CASE WHEN count(l) > 0 THEN true ELSE false END FROM lessons l " +
            "WHERE room_id = :roomId AND teacher_id = :teacherId AND discipline_id = :disciplineId " +
            "AND group_id = :groupId AND sub_group = :#{#subGroup?.ordinal()} AND day = :day " +
            "AND lesson_number = :lessonNumber AND lesson_type = :#{#lessonType?.ordinal()} AND week_type = :#{#weekType?.ordinal()} " +
            "AND status = :#{#status?.ordinal()} AND ((:startDate BETWEEN start_date AND end_date) OR (:endDate BETWEEN start_date AND end_date))", nativeQuery = true)
    boolean existsByRoomIdAndTeacherIdAndDisciplineIdAndGroupIdAndSubGroupAndDayAndLessonNumberAndLessonTypeAndWeekTypeAndStatusAndStartDateAndEndDate(long roomId, long teacherId, long disciplineId, long groupId, ESubGroup subGroup, short day, short lessonNumber, ELessonType lessonType, EWeekType weekType, EStatus status, LocalDate startDate, LocalDate endDate);

    List<LessonModel> findByGroupIdAndDayAndWeekTypeAndLessonNumberAndStatus(long groupId, short day, EWeekType weekType, short lessonNumber, EStatus status);

    List<LessonModel> findByGroupIdAndDayAndWeekTypeAndLessonNumberAndSubGroupAndStatus(long groupId, short day, EWeekType weekType, short lessonNumber, ESubGroup subGroup, EStatus status);

    List<LessonModel> findByGroupIdAndDayAndLessonNumberAndStatus(long groupId, short day, short lessonNumber, EStatus status);

    List<LessonModel> findByGroupIdAndDisciplineIdAndTeacherIdAndStatus(long groupId, long disciplineId, long teacherId, EStatus status);

    List<LessonModel> findByDayAndStatus(short day, EStatus status);

    @Query(value = "SELECT * FROM lessons WHERE status = :#{#status?.ordinal()} AND :date BETWEEN start_date AND end_date", nativeQuery = true)
    List<LessonModel> findByStatusAndDate(EStatus status, LocalDate date);

    List<LessonModel> findByRoomIdAndDayAndLessonNumberAndStatus(Long roomId, Short day, Short lessonNumber, EStatus status);
}