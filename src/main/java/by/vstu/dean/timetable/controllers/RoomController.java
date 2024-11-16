package by.vstu.dean.timetable.controllers;



import by.vstu.dean.core.enums.EStatus;
import by.vstu.dean.core.models.DBBaseModel;
import by.vstu.dean.support.enums.EFrame;
import by.vstu.dean.support.models.ClassroomModel;
import by.vstu.dean.timetable.dto.RoomDTO;
import by.vstu.dean.timetable.dto.mapper.LessonMapper;
import by.vstu.dean.timetable.enums.EWeekType;
import by.vstu.dean.timetable.models.LessonModel;
import by.vstu.dean.timetable.repo.LessonModelRepository;
import by.vstu.dean.timetable.repo.rest.ClassroomRepo;
import by.vstu.dean.timetable.services.LessonService;
import by.vstu.dean.timetable.services.common.ExcelService;
import by.vstu.dean.timetable.services.common.RoomService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService service;
    private final LessonService lessonService;
    private final LessonMapper mapper;
    private final ExcelService excelService;

    private final LessonModelRepository lessonRepo;
    private final ClassroomRepo classroomRepo;

    @RequestMapping(value = {""}, produces = {"application/json"}, method = {RequestMethod.GET})
    @PreAuthorize("#oauth2.hasScope('read') AND (hasAnyRole('ROLE_USER', 'ROLE_ADMIN'))")
    public ResponseEntity<List<RoomDTO>> getAll() {
        return new ResponseEntity<>((this.service).getAll(), HttpStatus.OK);
    }

    @RequestMapping(value = {"visibility"}, produces = {"application/json"}, method = {RequestMethod.POST})
    @PreAuthorize("#oauth2.hasScope('read') AND (hasAnyRole('ROLE_USER', 'ROLE_ADMIN'))")
    @Transactional
    public ResponseEntity<?> changeVisibility(@RequestParam long groupId, @RequestParam boolean visible, @RequestParam String date1, @RequestParam String date2) {
        List<LessonModel> lessons = this.lessonService.setVisibility(groupId, visible, date1, date2);

        if (lessons == null || lessons.isEmpty())
            return new ResponseEntity<>("Расписание для данной группы не найдено", HttpStatus.NOT_FOUND);

        return new ResponseEntity<>(this.mapper.toDto(lessons).stream().map(this.service::convertToDto).toList(), HttpStatus.OK);
    }

    @RequestMapping(value = {"hiddenGroups"}, produces = {"application/json"}, method = {RequestMethod.GET})
    @PreAuthorize("#oauth2.hasScope('read') AND (hasAnyRole('ROLE_USER', 'ROLE_ADMIN'))")
    public ResponseEntity<?> getHiddenGroups() {
        return new ResponseEntity<>(this.lessonService.getHiddenGroups(), HttpStatus.OK);
    }

    @RequestMapping(value = {"currentDate"}, produces = {"application/json"}, method = {RequestMethod.GET})
    @PreAuthorize("#oauth2.hasScope('read') AND (hasAnyRole('ROLE_USER', 'ROLE_ADMIN'))")
    @Transactional
    public ResponseEntity<List<RoomDTO>> getAllOnCurrentDate() {
        return new ResponseEntity<>(this.mapper.toDto(this.lessonRepo.findByStatusAndDate(EStatus.ACTIVE, LocalDate.now(), LocalDate.now())).stream().map(this.service::convertToDto).toList(), HttpStatus.OK);
    }

    @RequestMapping(value = {"byRoomAndDate"}, produces = {"application/json"}, method = {RequestMethod.GET})
    @PreAuthorize("#oauth2.hasScope('read') AND (hasAnyRole('ROLE_USER', 'ROLE_ADMIN'))")
    @Transactional
    public ResponseEntity<List<RoomDTO>> getAllOnCurrentDateAndClassroom(@RequestParam EFrame frame, @RequestParam String[] roomNumbers, @RequestParam EWeekType[] weekType, @RequestParam String startDate, @RequestParam String endDate) {
        List<ClassroomModel> rooms = classroomRepo.rsql(String.format("frame==%s;roomNumber=in=(%s)", frame, String.join(",", roomNumbers)));

        List<LessonModel> lessons = this.lessonRepo.findByRoomIdInAndWeekTypeInAndStatus(rooms.stream().map(DBBaseModel::getId).toList(), Arrays.stream(weekType).toList(), EStatus.ACTIVE).stream().filter(l -> !(l.getEndDate().isBefore(LocalDate.parse(startDate)) || l.getStartDate().isAfter(LocalDate.parse(endDate)) || (l.getStartDate().equals(LocalDate.parse(endDate)) && !l.getEndDate().equals(LocalDate.parse(endDate))))).toList();

        return new ResponseEntity<>(this.mapper.toDto(lessons).stream().map(this.service::convertToDto).toList(), HttpStatus.OK);
    }

    @RequestMapping(value = {"getExcel"}, produces = {"application/xlsx"}, method = {RequestMethod.GET})
    @PreAuthorize("#oauth2.hasScope('read') AND (hasAnyRole('ROLE_USER', 'ROLE_ADMIN'))")
    public void getExcel(HttpServletResponse response, @RequestParam long facultyId, @RequestParam int course, @RequestParam String date1, @RequestParam String date2) throws IOException {
        response.setHeader("Content-Disposition", "inline;filename=\"" + URLEncoder.encode("Ras.xlsx", StandardCharsets.UTF_8) + "\"");

        Workbook workbook = this.excelService.excelDaytime(facultyId, course, date1, date2);
        OutputStream outputStream = response.getOutputStream();

        workbook.getCreationHelper().createFormulaEvaluator().clearAllCachedResultValues();
        workbook.setForceFormulaRecalculation(true);

        workbook.write(outputStream);
        workbook.close();
        outputStream.flush();
        outputStream.close();
    }

    @RequestMapping(value = {"daytimeSessionExcel"}, produces = {"application/xlsx"}, method = {RequestMethod.GET})
    @PreAuthorize("#oauth2.hasScope('read') AND (hasAnyRole('ROLE_USER', 'ROLE_ADMIN'))")
    public void daytimeSessionExcel(HttpServletResponse response, @RequestParam long[] groupIds, @RequestParam String date1, @RequestParam String date2) throws IOException {
        response.setHeader("Content-Disposition", "inline;filename=\"" + URLEncoder.encode("session.xlsx", StandardCharsets.UTF_8) + "\"");

        Workbook workbook = this.excelService.excelDaytimeSession(groupIds, date1, date2);
        OutputStream outputStream = response.getOutputStream();

        workbook.getCreationHelper().createFormulaEvaluator().clearAllCachedResultValues();
        workbook.setForceFormulaRecalculation(true);

        workbook.write(outputStream);
        workbook.close();
        outputStream.flush();
        outputStream.close();
    }

    @Deprecated
    @RequestMapping(value = {"citExcel"}, produces = {"application/xlsx"}, method = {RequestMethod.GET})
    @PreAuthorize("#oauth2.hasScope('read') AND (hasAnyRole('ROLE_USER', 'ROLE_ADMIN'))")
    public void citExcel(HttpServletResponse response, @RequestParam String date1, @RequestParam String date2) throws IOException {
        response.setHeader("Content-Disposition", "inline;filename=\"" + URLEncoder.encode("Cit.xlsx", StandardCharsets.UTF_8) + "\"");

        Workbook workbook = this.excelService.excelForCit(date1, date2);
        OutputStream outputStream = response.getOutputStream();

        workbook.getCreationHelper().createFormulaEvaluator().clearAllCachedResultValues();
        workbook.setForceFormulaRecalculation(true);

        workbook.write(outputStream);
        workbook.close();
        outputStream.flush();
        outputStream.close();
    }

    @RequestMapping(value = {"teacherExcel"}, produces = {"application/xlsx"}, method = {RequestMethod.GET})
    @PreAuthorize("#oauth2.hasScope('read') AND (hasAnyRole('ROLE_USER', 'ROLE_ADMIN'))")
    public void teacherExcel(HttpServletResponse response, @RequestParam long[] teacherIds, @RequestParam String date1, @RequestParam String date2) throws IOException {
        response.setHeader("Content-Disposition", "inline;filename=\"" + URLEncoder.encode("Teacher.xlsx", StandardCharsets.UTF_8) + "\"");

        Workbook workbook = this.excelService.excelTeacherWorkload(teacherIds, date1, date2);
        OutputStream outputStream = response.getOutputStream();

        workbook.getCreationHelper().createFormulaEvaluator().clearAllCachedResultValues();
        workbook.setForceFormulaRecalculation(true);

        workbook.write(outputStream);
        workbook.close();
        outputStream.flush();
        outputStream.close();
    }

    @RequestMapping(value = {"classroomExcel"}, produces = {"application/xlsx"}, method = {RequestMethod.GET})
    @PreAuthorize("#oauth2.hasScope('read') AND (hasAnyRole('ROLE_USER', 'ROLE_ADMIN'))")
    public void classroomExcel(HttpServletResponse response, @RequestParam long[] classroomIds, @RequestParam String date1, @RequestParam String date2) throws IOException {
        response.setHeader("Content-Disposition", "inline;filename=\"" + URLEncoder.encode("classroom.xlsx", StandardCharsets.UTF_8) + "\"");

        Workbook workbook = this.excelService.excelClassroomWorkload(classroomIds, date1, date2);
        OutputStream outputStream = response.getOutputStream();

        workbook.getCreationHelper().createFormulaEvaluator().clearAllCachedResultValues();
        workbook.setForceFormulaRecalculation(true);

        workbook.write(outputStream);
        workbook.close();
        outputStream.flush();
        outputStream.close();
    }

    @RequestMapping(value = {"zaochnoeExcel"}, produces = {"application/xlsx"}, method = {RequestMethod.GET})
    @PreAuthorize("#oauth2.hasScope('read') AND (hasAnyRole('ROLE_USER', 'ROLE_ADMIN'))")
    public void zaochnoeExcel(HttpServletResponse response, @RequestParam long[] groupsIds, @RequestParam String from, @RequestParam String to, @RequestParam(required = false, defaultValue = "") String sessionType) throws IOException {
        response.setHeader("Content-Disposition", "inline;filename=\"" + URLEncoder.encode("Zaochnoe.xlsx", StandardCharsets.UTF_8) + "\"");

        Workbook workbook = this.excelService.excelZaoch(groupsIds, from, to, sessionType);
        OutputStream outputStream = response.getOutputStream();

        if (workbook == null) {
            return;
        }

        workbook.getCreationHelper().createFormulaEvaluator().clearAllCachedResultValues();
        workbook.setForceFormulaRecalculation(true);

        workbook.write(outputStream);
        workbook.close();
        outputStream.flush();
        outputStream.close();
    }

    @RequestMapping(value = {"/delete"}, produces = {"application/json"}, method = {RequestMethod.DELETE})
    @PreAuthorize("#oauth2.hasScope('read') AND (hasAnyRole('ROLE_USER', 'ROLE_ADMIN'))")
    @Transactional
    public ResponseEntity<LessonModel> delete(@RequestParam Long id) {
        LessonModel l = this.lessonService.delete(id);

        if (l == null) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

        return new ResponseEntity<>(l, HttpStatus.OK);
    }

    @RequestMapping(value = {"/put"}, produces = {"application/json"}, method = {RequestMethod.PUT})
    @PreAuthorize("#oauth2.hasScope('read') AND (hasAnyRole('ROLE_ADMIN'))")
    public ResponseEntity<?> put(@RequestBody RoomDTO room, @RequestParam(required = false, defaultValue = "false") boolean check) {
        if (room.getStartDate() == null && room.getEndDate() != null) room.setStartDate(room.getEndDate());
        if (room.getEndDate() == null && room.getStartDate() != null) room.setEndDate(room.getStartDate());

        if (room.getLessonId() != null) {
            ResponseEntity<?> checkedIfExist = this.service.checkIfExist(room);
            if (checkedIfExist.getStatusCode().equals(HttpStatus.CONFLICT)) return checkedIfExist;
        }

        if (check) {
            ResponseEntity<?> checkedForConflicts = this.service.checkForConflicts(room);
            if (checkedForConflicts.getStatusCode().equals(HttpStatus.CONFLICT)) return checkedForConflicts;
        }

        RoomDTO r = this.service.save(room);

        if (r == null) return new ResponseEntity<>("Некорректные данные", HttpStatus.BAD_REQUEST);

        return new ResponseEntity<>(r, HttpStatus.OK);
    }
}
