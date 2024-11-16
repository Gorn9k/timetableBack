package by.vstu.dean.timetable.repo;

import by.vstu.dean.core.enums.EStatus;
import by.vstu.dean.core.repo.DBBaseModelRepository;
import by.vstu.dean.timetable.enums.ELessonTypes;
import by.vstu.dean.timetable.enums.ESubGroup;
import by.vstu.dean.timetable.enums.EWeekType;
import by.vstu.dean.timetable.models.LessonModel;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface LessonModelRepository extends DBBaseModelRepository<LessonModel> {
    List<LessonModel> findAllByRoomId(Long roomId);

    List<LessonModel> findByStatus(EStatus status);

    List<LessonModel> findByGroupIdAndStatus(Long groupId, EStatus status);

    @Query("""
            select l from LessonModel l
            where l.groupId = ?1 and l.status = ?2 and l.startDate between ?3 and ?4 and l.endDate between ?3 and ?4""")
    List<LessonModel> findByGroupIdAndStatusAndStartDateBetweenAndEndDateBetween(Long groupId, EStatus status, LocalDate dateFrom, LocalDate dateTo);

    @Query(value = "SELECT * FROM lessons WHERE group_id = :groupId AND status = :#{#status?.ordinal()} AND " +
            "start_date = :date and end_date = :date", nativeQuery = true)
    List<LessonModel> findByGroupIdAndStatusAndDate(long groupId, EStatus status, LocalDate date);

//    boolean existsByRoomIdAndTeacherIdAndWeekTypeAndDayAndLessonNumberAndLessonTypeAndStatusAndIdNot(long roomId, long teacherId, EWeekType weekType, short day, short lessonNumber, ELessonType lessonType, EStatus status, Long id);

    //    boolean existsByRoomIdAndLessonNumberAndDayAndStatusAndIdNot(long roomId, short lessonNumber, short day, EStatus status, Long id);

    @Query(value = "SELECT CASE WHEN count(l) > 0 THEN true ELSE false END FROM lessons l WHERE " +
            "room_id = :roomId AND lesson_number = :lessonNumber AND day = :day AND status = :#{#status?.ordinal()} AND " +
            "((:dateFrom BETWEEN start_date AND end_date) OR (:dateTo BETWEEN start_date AND end_date))", nativeQuery = true)
    boolean existsByRoomIdAndLessonNumberAndDayAndStatusAndBetweenDates(long roomId, short lessonNumber, short day, EStatus status, LocalDate dateFrom, LocalDate dateTo);

//    boolean existsByRoomIdAndLessonNumberAndDayAndWeekTypeAndStatusAndIdNot(long roomId, short lessonNumber, short day, EWeekType weekType, EStatus status, Long id);

    @Query(value = "SELECT CASE WHEN count(l) > 0 THEN true ELSE false END FROM lessons l WHERE " +
            "room_id = :roomId AND lesson_number = :lessonNumber AND day = :day AND week_type = :#{#weekType?.ordinal()} AND status = :#{#status?.ordinal()} AND " +
            "((:dateFrom BETWEEN start_date AND end_date) OR (:dateTo BETWEEN start_date AND end_date))", nativeQuery = true)
    boolean existsByRoomIdAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(long roomId, short lessonNumber, short day, EWeekType weekType, EStatus status, LocalDate dateFrom, LocalDate dateTo);

//    boolean existsByTeacherIdAndLessonNumberAndDayAndStatusAndIdNot(long teacherId, short lessonNumber, short day, EStatus status, Long id);

    @Query(value = "SELECT CASE WHEN count(l) > 0 THEN true ELSE false END FROM lessons l WHERE " +
            "teacher_id = :teacherId AND lesson_number = :lessonNumber AND day = :day AND status = :#{#status?.ordinal()} AND " +
            "((:dateFrom BETWEEN start_date AND end_date) OR (:dateTo BETWEEN start_date AND end_date))", nativeQuery = true)
    boolean existsByTeacherIdAndLessonNumberAndDayAndStatusAndBetweenDates(long teacherId, short lessonNumber, short day, EStatus status, LocalDate dateFrom, LocalDate dateTo);

//    boolean existsByTeacherIdAndLessonNumberAndDayAndWeekTypeAndStatusAndIdNot(long teacherId, short lessonNumber, short day, EWeekType weekType, EStatus status, Long id);

    @Query(value = "SELECT CASE WHEN count(l) > 0 THEN true ELSE false END FROM lessons l WHERE " +
            "teacher_id = :teacherId AND lesson_number = :lessonNumber AND day = :day AND week_type = :#{#weekType?.ordinal()} AND status = :#{#status?.ordinal()} AND " +
            "((:dateFrom BETWEEN start_date AND end_date) OR (:dateTo BETWEEN start_date AND end_date))", nativeQuery = true)
    boolean existsByTeacherIdAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(long teacherId, short lessonNumber, short day, EWeekType weekType, EStatus status, LocalDate dateFrom, LocalDate dateTo);

    @Query(value = "SELECT CASE WHEN count(l) > 0 THEN true ELSE false END FROM lessons l WHERE " +
            "group_id = :groupId AND sub_group = :#{#subGroup?.ordinal()} AND lesson_number = :lessonNumber AND day = :day AND status = :#{#status?.ordinal()} AND " +
            "((:dateFrom BETWEEN start_date AND end_date) OR (:dateTo BETWEEN start_date AND end_date))", nativeQuery = true)
    boolean existsByGroupIdAndSubGroupAndLessonNumberAndDayAndStatusAndBetweenDates(long groupId, ESubGroup subGroup, short lessonNumber, short day, EStatus status, LocalDate dateFrom, LocalDate dateTo);

    @Query(value = "SELECT CASE WHEN count(l) > 0 THEN true ELSE false END FROM lessons l WHERE " +
            "group_id = :groupId AND lesson_number = :lessonNumber AND day = :day AND week_type = :#{#weekType?.ordinal()} AND status = :#{#status?.ordinal()} AND " +
            "((:dateFrom BETWEEN start_date AND end_date) OR (:dateTo BETWEEN start_date AND end_date))", nativeQuery = true)
    boolean existsByGroupIdAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(long groupId, short lessonNumber, short day, EWeekType weekType, EStatus status, LocalDate dateFrom, LocalDate dateTo);

    @Query(value = "SELECT CASE WHEN count(l) > 0 THEN true ELSE false END FROM lessons l WHERE " +
            "group_id = :groupId AND sub_group = :#{#subGroup?.ordinal()} AND lesson_number = :lessonNumber AND day = :day AND week_type = :#{#weekType?.ordinal()} AND status = :#{#status?.ordinal()} AND " +
            "((:dateFrom BETWEEN start_date AND end_date) OR (:dateTo BETWEEN start_date AND end_date))", nativeQuery = true)
    boolean existsByGroupIdAndSubGroupAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(long groupId, ESubGroup subGroup, short lessonNumber, short day, EWeekType weekType, EStatus status, LocalDate dateFrom, LocalDate dateTo);

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
            "AND status = :#{#status?.ordinal()} AND ((start_date BETWEEN :startDate AND :endDate) OR (end_date BETWEEN :startDate AND :endDate))", nativeQuery = true)
    boolean existsByRoomIdAndTeacherIdAndDisciplineIdAndGroupIdAndSubGroupAndDayAndLessonNumberAndLessonTypeAndWeekTypeAndStatusAndStartDateAndEndDate(long roomId, long teacherId, long disciplineId, long groupId, ESubGroup subGroup, short day, short lessonNumber, ELessonTypes lessonType, EWeekType weekType, EStatus status, LocalDate startDate, LocalDate endDate);


    @Query(value = "SELECT CASE WHEN count(l) > 0 THEN true ELSE false END FROM lessons l " +
            "WHERE room_id = :roomId AND teacher_id = :teacherId AND discipline_id = :disciplineId " +
            "AND group_id = :groupId AND sub_group = :#{#subGroup?.ordinal()} AND day = :day AND id <> :lessonId " +
            "AND lesson_number = :lessonNumber AND lesson_type = :#{#lessonType?.ordinal()} AND week_type = :#{#weekType?.ordinal()} " +
            "AND status = :#{#status?.ordinal()} AND ((start_date BETWEEN :startDate AND :endDate) OR (end_date BETWEEN :startDate AND :endDate))", nativeQuery = true)
    boolean existsByRoomIdAndTeacherIdAndDisciplineIdAndGroupIdAndSubGroupAndDayAndLessonNumberAndLessonTypeAndWeekTypeAndStatusAndStartDateAndEndDateAndLessonIdNot(long roomId, long teacherId, long disciplineId, long groupId, ESubGroup subGroup, short day, short lessonNumber, ELessonTypes lessonType, EWeekType weekType, EStatus status, LocalDate startDate, LocalDate endDate, long lessonId);

    List<LessonModel> findByGroupIdAndDayAndWeekTypeAndLessonNumberAndStatus(long groupId, short day, EWeekType weekType, short lessonNumber, EStatus status);

    @Query(value = "SELECT * FROM lessons WHERE group_id = :groupId AND day = :day AND week_type = :#{#weekType?.ordinal()} AND " +
            "lesson_number = :lessonNumber AND sub_group = :#{#subGroup?.ordinal()} AND status = :#{#status?.ordinal()} AND " +
            "((start_date BETWEEN :dateFrom AND :dateTo) OR (end_date BETWEEN :dateFrom AND :dateTo))", nativeQuery = true)
    List<LessonModel> findByGroupIdAndDayAndWeekTypeAndLessonNumberAndSubGroupAndStatusAndDateBetween(long groupId, short day, EWeekType weekType, short lessonNumber, ESubGroup subGroup, EStatus status, LocalDate dateFrom, LocalDate dateTo);

    @Query(value = "SELECT * FROM lessons WHERE group_id = :groupId AND day = :day AND lesson_number = :lessonNumber AND status = :#{#status?.ordinal()} AND " +
            "((start_date BETWEEN :dateFrom AND :dateTo) OR (end_date BETWEEN :dateFrom AND :dateTo))", nativeQuery = true)
    List<LessonModel> findByGroupIdAndDayAndLessonNumberAndStatusAndBetweenDates(long groupId, short day, short lessonNumber, EStatus status, LocalDate dateFrom, LocalDate dateTo);

    @Query(value = "SELECT * FROM lessons WHERE group_id = :groupId AND day = :day AND lesson_number = :lessonNumber AND sub_group = :#{#subGroup?.ordinal()} AND " +
            "start_date = :date AND end_date = :date AND status = :#{#status?.ordinal()}", nativeQuery = true)
    List<LessonModel> findByGroupIdAndDayAndLessonNumberAndSubGroupAndDateAndStatus(long groupId, short day, short lessonNumber, ESubGroup subGroup, LocalDate date, EStatus status);

    List<LessonModel> findByGroupIdAndDisciplineIdAndTeacherIdAndStatus(long groupId, long disciplineId, long teacherId, EStatus status);

    List<LessonModel> findByDayAndStatus(short day, EStatus status);

    @Query(value = "SELECT * FROM lessons WHERE status = :#{#status?.ordinal()} AND ((start_date BETWEEN :date1 AND :date2) OR (end_date BETWEEN :date1 AND :date2))", nativeQuery = true)
    List<LessonModel> findByStatusAndDate(EStatus status, LocalDate date1, LocalDate date2);

    @Query(value = "SELECT * FROM lessons WHERE status = :#{#status?.ordinal()} AND ((start_date BETWEEN :date1 AND :date2) OR (end_date BETWEEN :date1 AND :date2))", nativeQuery = true)
    List<LessonModel> findByStatusAndBetweenDates(EStatus status, LocalDate date1, LocalDate date2);

    @Query(value = "SELECT * FROM lessons WHERE teacher_id = :teacherId AND status = :#{#status?.ordinal()} AND ((:date1 BETWEEN start_date AND end_date) OR (:date2 BETWEEN start_date AND end_date))", nativeQuery = true)
    List<LessonModel> findByTeacherIdAndStatusAndDate(long teacherId, EStatus status, LocalDate date1, LocalDate date2);

    List<LessonModel> findByTeacherIdAndStatus(long teacherId, EStatus status);

    List<LessonModel> findByRoomIdAndStatus(long roomId, EStatus status);

    @Query(value = "SELECT * FROM lessons WHERE status = :#{#status?.ordinal()} AND group_id IN (:groupsIds) AND " +
            "((start_date BETWEEN :dateFrom AND :dateTo) OR (end_date BETWEEN :dateFrom AND :dateTo))", nativeQuery = true)
    List<LessonModel> findByGroupIdInAndStatusAndBetweenDates(long[] groupsIds, EStatus status, LocalDate dateFrom, LocalDate dateTo);

    @Query(value = "SELECT * FROM lessons WHERE group_id = :groupId AND status = :#{#status?.ordinal()} AND" +
            "((start_date BETWEEN :dateFrom AND :dateTo) OR (end_date BETWEEN :dateFrom AND :dateTo))", nativeQuery = true)
    List<LessonModel> findByGroupIdAndStatusAndBetweenDates(Long groupId, EStatus status, LocalDate dateFrom, LocalDate dateTo);

    @Query(value = "SELECT * FROM lessons WHERE status = :#{#status?.ordinal()} AND week_type IN (:weekType) AND ((start_date BETWEEN :startDate AND :endDate) OR (end_date BETWEEN :startDate AND :endDate)) AND room_id IN :roomId", nativeQuery = true)
    List<LessonModel> findByStatusAndRoomAndWeekTypeAndDate(EStatus status, Long[] roomId, Integer[] weekType, LocalDate startDate, LocalDate endDate);

    List<LessonModel> findByRoomIdInAndWeekTypeInAndStatus(Collection<Long> roomIds, Collection<EWeekType> weekTypes, EStatus status);

    @Query(value = "SELECT * FROM lessons WHERE room_id = :roomId AND day = :day AND lesson_number = :lessonNumber AND status = :#{#status?.ordinal()} AND " +
            "((start_date BETWEEN :dateFrom AND :dateTo) OR (end_date BETWEEN :dateFrom AND :dateTo))", nativeQuery = true)
    List<LessonModel> findByRoomIdAndDayAndLessonNumberAndStatusAndDateBetween(Long roomId, Short day, Short lessonNumber, EStatus status, LocalDate dateFrom, LocalDate dateTo);

    List<LessonModel> findByStatusAndVisible(EStatus status, boolean visible);
}