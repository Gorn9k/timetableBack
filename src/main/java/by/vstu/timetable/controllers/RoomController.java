package by.vstu.timetable.controllers;


import by.vstu.dean.anotations.ApiSecurity;
import by.vstu.dean.enums.ELessonType;
import by.vstu.timetable.dto.RoomDTO;
import by.vstu.timetable.enums.EWeekType;
import by.vstu.timetable.models.LessonModel;
import by.vstu.timetable.repo.LessonModelRepository;
import by.vstu.timetable.services.LessonService;
import by.vstu.timetable.services.common.RoomService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService service;
    private final LessonService lessonService;

    private final LessonModelRepository lessonRepo;

    @RequestMapping(
            value = {""},
            produces = {"application/json"},
            method = {RequestMethod.GET}
    )
    @PreAuthorize("#oauth2.hasScope('read') AND (hasAnyRole('ROLE_USER', 'ROLE_ADMIN'))")
    public ResponseEntity<List<RoomDTO>> getAll() {
//        return new ResponseEntity<>((this.service).convert2Rooms(), HttpStatus.OK);
        return new ResponseEntity<>((this.service).getAll(), HttpStatus.OK);
    }

    @RequestMapping(
            value = {"getExcel"},
            produces = {"application/xlsx"},
            method = {RequestMethod.GET}
    )
    @PreAuthorize("#oauth2.hasScope('read') AND (hasAnyRole('ROLE_USER', 'ROLE_ADMIN'))")
    public void getExcel(HttpServletResponse response, @RequestParam long facultyId, @RequestParam int course) throws IOException, ParseException {
        response.setHeader("Content-Disposition", "inline;filename=\"" +
                URLEncoder.encode("Ras.xlsx", "UTF-8") + "\"");

        Workbook workbook = this.service.getExcel(facultyId, course);
        OutputStream outputStream = response.getOutputStream();

        workbook.getCreationHelper().createFormulaEvaluator().clearAllCachedResultValues();
        workbook.setForceFormulaRecalculation(true);

        workbook.write(outputStream);
        workbook.close();
        outputStream.flush();
        outputStream.close();
    }

    @RequestMapping(
            value = {"/delete"},
            produces = {"application/json"},
            method = {RequestMethod.DELETE}
    )
    @PreAuthorize("#oauth2.hasScope('read') AND (hasAnyRole('ROLE_USER', 'ROLE_ADMIN'))")
    public ResponseEntity<LessonModel> delete(@RequestParam Long id) {
        LessonModel l = this.lessonService.delete(id);

        if (l == null)
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

        return new ResponseEntity<>(l, HttpStatus.OK);
    }

    @RequestMapping(
            value = {"/put"},
            produces = {"application/json"},
            method = {RequestMethod.PUT}
    )
    @PreAuthorize("#oauth2.hasScope('read') AND (hasAnyRole('ROLE_ADMIN'))")
    public ResponseEntity<?> put(@RequestBody RoomDTO room) {

        if (this.lessonRepo.existsByRoomIdAndTeacherIdAndDisciplineIdAndGroupIdAndSubGroupAndDayAndLessonNumberAndLessonTypeAndWeekType(room.getRoomId(),
                room.getTeacherId(),
                room.getDisciplineId(),
                room.getGroupId(),
                room.getSubGroup(),
                room.getDay(),
                room.getLessonNumber(),
                ELessonType.valueOf(room.getLessonType()),
                EWeekType.valueOf(room.getWeekType())))
            return new ResponseEntity<>("Ошибка: Такая запись уже существует.", HttpStatus.CONFLICT);

        // Проверка на конфликты новой пары и уже добаевленные, проходящие в то же время в том же месте,
        // проверка на конфликт нескольких пар в одно время у одного препода.
        // P.S. Добавил говна, потому что захотел =))

        // Проверка на пток
        if (!this.lessonRepo.existsByRoomIdAndTeacherIdAndWeekTypeAndDayAndLessonNumberAndLessonType(room.getRoomId(),
                room.getTeacherId(),
                EWeekType.valueOf(room.getWeekType()),
                room.getDay(),
                room.getLessonNumber(),
                ELessonType.valueOf(room.getLessonType()))) {
            switch (EWeekType.valueOf(room.getWeekType())) {
                case ALWAYS:
                    if (this.lessonRepo.existsByRoomIdAndLessonNumberAndDay(room.getRoomId(), room.getLessonNumber(), room.getDay()))
                        return new ResponseEntity<>("Конфликт: В аудитории " + room.getRoomNumber() + " уже есть пара на это время.", HttpStatus.CONFLICT);

                    if (this.lessonRepo.existsByTeacherIdAndLessonNumberAndDay(room.getTeacherId(), room.getLessonNumber(), room.getDay()))
                        return new ResponseEntity<>("Конфликт: У преподавателя " + room.getTeacherFullName() + " уже есть пара на это время.", HttpStatus.CONFLICT);

                    break;

                case NUMERATOR:
                    if (this.lessonRepo.existsByRoomIdAndLessonNumberAndDayAndWeekType(room.getRoomId(), room.getLessonNumber(), room.getDay(), EWeekType.ALWAYS) ||
                            this.lessonRepo.existsByRoomIdAndLessonNumberAndDayAndWeekType(room.getRoomId(), room.getLessonNumber(), room.getDay(), EWeekType.NUMERATOR) ||
                            this.lessonRepo.existsByRoomIdAndLessonNumberAndDayAndWeekType(room.getRoomId(), room.getLessonNumber(), room.getDay(), EWeekType.FIRST) ||
                            this.lessonRepo.existsByRoomIdAndLessonNumberAndDayAndWeekType(room.getRoomId(), room.getLessonNumber(), room.getDay(), EWeekType.THIRD))
                        return new ResponseEntity<>("Конфликт: В аудитории " + room.getRoomNumber() + " уже есть пара на это время.", HttpStatus.CONFLICT);

                    if (this.lessonRepo.existsByTeacherIdAndLessonNumberAndDayAndWeekType(room.getTeacherId(), room.getLessonNumber(), room.getDay(), EWeekType.ALWAYS) ||
                            this.lessonRepo.existsByTeacherIdAndLessonNumberAndDayAndWeekType(room.getTeacherId(), room.getLessonNumber(), room.getDay(), EWeekType.NUMERATOR) ||
                            this.lessonRepo.existsByTeacherIdAndLessonNumberAndDayAndWeekType(room.getTeacherId(), room.getLessonNumber(), room.getDay(), EWeekType.FIRST) ||
                            this.lessonRepo.existsByTeacherIdAndLessonNumberAndDayAndWeekType(room.getTeacherId(), room.getLessonNumber(), room.getDay(), EWeekType.THIRD))
                        return new ResponseEntity<>("Конфликт: У преподавателя " + room.getTeacherFullName() + " уже есть пара на это время.", HttpStatus.CONFLICT);

                    break;

                case DENOMINATOR:
                    if (this.lessonRepo.existsByRoomIdAndLessonNumberAndDayAndWeekType(room.getRoomId(), room.getLessonNumber(), room.getDay(), EWeekType.ALWAYS) ||
                            this.lessonRepo.existsByRoomIdAndLessonNumberAndDayAndWeekType(room.getRoomId(), room.getLessonNumber(), room.getDay(), EWeekType.DENOMINATOR) ||
                            this.lessonRepo.existsByRoomIdAndLessonNumberAndDayAndWeekType(room.getRoomId(), room.getLessonNumber(), room.getDay(), EWeekType.SECOND) ||
                            this.lessonRepo.existsByRoomIdAndLessonNumberAndDayAndWeekType(room.getRoomId(), room.getLessonNumber(), room.getDay(), EWeekType.FOURTH))
                        return new ResponseEntity<>("Конфликт: В аудитории " + room.getRoomNumber() + " уже есть пара на это время.", HttpStatus.CONFLICT);


                    if (this.lessonRepo.existsByTeacherIdAndLessonNumberAndDayAndWeekType(room.getTeacherId(), room.getLessonNumber(), room.getDay(), EWeekType.ALWAYS) ||
                            this.lessonRepo.existsByTeacherIdAndLessonNumberAndDayAndWeekType(room.getTeacherId(), room.getLessonNumber(), room.getDay(), EWeekType.DENOMINATOR) ||
                            this.lessonRepo.existsByTeacherIdAndLessonNumberAndDayAndWeekType(room.getTeacherId(), room.getLessonNumber(), room.getDay(), EWeekType.SECOND) ||
                            this.lessonRepo.existsByTeacherIdAndLessonNumberAndDayAndWeekType(room.getTeacherId(), room.getLessonNumber(), room.getDay(), EWeekType.FOURTH))
                        return new ResponseEntity<>("Конфликт: У преподавателя " + room.getTeacherFullName() + " уже есть пара на это время.", HttpStatus.CONFLICT);

                    break;

                case FIRST:
                    if (this.lessonRepo.existsByRoomIdAndLessonNumberAndDayAndWeekType(room.getRoomId(), room.getLessonNumber(), room.getDay(), EWeekType.ALWAYS) ||
                            this.lessonRepo.existsByRoomIdAndLessonNumberAndDayAndWeekType(room.getRoomId(), room.getLessonNumber(), room.getDay(), EWeekType.NUMERATOR) ||
                            this.lessonRepo.existsByRoomIdAndLessonNumberAndDayAndWeekType(room.getRoomId(), room.getLessonNumber(), room.getDay(), EWeekType.FIRST))
                        return new ResponseEntity<>("Конфликт: В аудитории " + room.getRoomNumber() + " уже есть пара на это время.", HttpStatus.CONFLICT);
                    if (this.lessonRepo.existsByTeacherIdAndLessonNumberAndDayAndWeekType(room.getTeacherId(), room.getLessonNumber(), room.getDay(), EWeekType.ALWAYS) ||
                            this.lessonRepo.existsByTeacherIdAndLessonNumberAndDayAndWeekType(room.getTeacherId(), room.getLessonNumber(), room.getDay(), EWeekType.NUMERATOR) ||
                            this.lessonRepo.existsByTeacherIdAndLessonNumberAndDayAndWeekType(room.getTeacherId(), room.getLessonNumber(), room.getDay(), EWeekType.FIRST))
                        return new ResponseEntity<>("Конфликт: У преподавателя " + room.getTeacherFullName() + " уже есть пара на это время.", HttpStatus.CONFLICT);

                    break;

                case SECOND:
                    if (this.lessonRepo.existsByRoomIdAndLessonNumberAndDayAndWeekType(room.getRoomId(), room.getLessonNumber(), room.getDay(), EWeekType.ALWAYS) ||
                            this.lessonRepo.existsByRoomIdAndLessonNumberAndDayAndWeekType(room.getRoomId(), room.getLessonNumber(), room.getDay(), EWeekType.DENOMINATOR) ||
                            this.lessonRepo.existsByRoomIdAndLessonNumberAndDayAndWeekType(room.getRoomId(), room.getLessonNumber(), room.getDay(), EWeekType.SECOND))
                        return new ResponseEntity<>("Конфликт: В аудитории " + room.getRoomNumber() + " уже есть пара на это время.", HttpStatus.CONFLICT);
                    if (this.lessonRepo.existsByTeacherIdAndLessonNumberAndDayAndWeekType(room.getTeacherId(), room.getLessonNumber(), room.getDay(), EWeekType.ALWAYS) ||
                            this.lessonRepo.existsByTeacherIdAndLessonNumberAndDayAndWeekType(room.getTeacherId(), room.getLessonNumber(), room.getDay(), EWeekType.DENOMINATOR) ||
                            this.lessonRepo.existsByTeacherIdAndLessonNumberAndDayAndWeekType(room.getTeacherId(), room.getLessonNumber(), room.getDay(), EWeekType.SECOND))
                        return new ResponseEntity<>("Конфликт: У преподавателя " + room.getTeacherFullName() + " уже есть пара на это время.", HttpStatus.CONFLICT);

                    break;

                case THIRD:
                    if (this.lessonRepo.existsByRoomIdAndLessonNumberAndDayAndWeekType(room.getRoomId(), room.getLessonNumber(), room.getDay(), EWeekType.ALWAYS) ||
                            this.lessonRepo.existsByRoomIdAndLessonNumberAndDayAndWeekType(room.getRoomId(), room.getLessonNumber(), room.getDay(), EWeekType.NUMERATOR) ||
                            this.lessonRepo.existsByRoomIdAndLessonNumberAndDayAndWeekType(room.getRoomId(), room.getLessonNumber(), room.getDay(), EWeekType.THIRD))
                        return new ResponseEntity<>("Конфликт: В аудитории " + room.getRoomNumber() + " уже есть пара на это время.", HttpStatus.CONFLICT);

                    if (this.lessonRepo.existsByTeacherIdAndLessonNumberAndDayAndWeekType(room.getTeacherId(), room.getLessonNumber(), room.getDay(), EWeekType.ALWAYS) ||
                            this.lessonRepo.existsByTeacherIdAndLessonNumberAndDayAndWeekType(room.getTeacherId(), room.getLessonNumber(), room.getDay(), EWeekType.NUMERATOR) ||
                            this.lessonRepo.existsByTeacherIdAndLessonNumberAndDayAndWeekType(room.getTeacherId(), room.getLessonNumber(), room.getDay(), EWeekType.THIRD))
                        return new ResponseEntity<>("Конфликт: У преподавателя " + room.getTeacherFullName() + " уже есть пара на это время.", HttpStatus.CONFLICT);

                    break;

                case FOURTH:
                    if (this.lessonRepo.existsByRoomIdAndLessonNumberAndDayAndWeekType(room.getRoomId(), room.getLessonNumber(), room.getDay(), EWeekType.ALWAYS) ||
                            this.lessonRepo.existsByRoomIdAndLessonNumberAndDayAndWeekType(room.getRoomId(), room.getLessonNumber(), room.getDay(), EWeekType.DENOMINATOR) ||
                            this.lessonRepo.existsByRoomIdAndLessonNumberAndDayAndWeekType(room.getRoomId(), room.getLessonNumber(), room.getDay(), EWeekType.FOURTH))
                        return new ResponseEntity<>("Конфликт: В аудитории " + room.getRoomNumber() + " уже есть пара на это время.", HttpStatus.CONFLICT);

                    if (this.lessonRepo.existsByTeacherIdAndLessonNumberAndDayAndWeekType(room.getTeacherId(), room.getLessonNumber(), room.getDay(), EWeekType.ALWAYS) ||
                            this.lessonRepo.existsByTeacherIdAndLessonNumberAndDayAndWeekType(room.getTeacherId(), room.getLessonNumber(), room.getDay(), EWeekType.DENOMINATOR) ||
                            this.lessonRepo.existsByTeacherIdAndLessonNumberAndDayAndWeekType(room.getTeacherId(), room.getLessonNumber(), room.getDay(), EWeekType.FOURTH))
                        return new ResponseEntity<>("Конфликт: У преподавателя " + room.getTeacherFullName() + " уже есть пара на это время.", HttpStatus.CONFLICT);

                    break;
            }
        }

        LessonModel l = this.service.save(room);

        if (l == null)
            return new ResponseEntity<>("Некорректные данные", HttpStatus.BAD_REQUEST);

        return new ResponseEntity<>(l, HttpStatus.OK);
    }
}
