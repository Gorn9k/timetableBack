package by.vstu.dean.timetable.controllers;

import by.vstu.dean.core.adapters.LocalDateTimeTypeAdapter;
import by.vstu.dean.core.adapters.LocalDateTypeAdapter;
import by.vstu.dean.core.adapters.json.LocalDateJsonAdapter;
import by.vstu.dean.core.adapters.json.LocalDateTimeJsonAdapter;
import by.vstu.dean.core.controllers.BaseController;
import by.vstu.dean.timetable.dto.ContentDTO;
import by.vstu.dean.timetable.dto.LessonDTO;
import by.vstu.dean.timetable.dto.mapper.LessonMapper;
import by.vstu.dean.timetable.models.LessonModel;
import by.vstu.dean.timetable.repo.LessonModelRepository;
import by.vstu.dean.timetable.services.LessonService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/lessons/")
public class LessonController extends BaseController<LessonDTO, LessonModel, LessonMapper, LessonModelRepository, LessonService> {
    protected final Gson gson = (new GsonBuilder()).registerTypeAdapter(LocalDateTime.class, new LocalDateTimeJsonAdapter()).registerTypeAdapter(LocalDate.class, new LocalDateJsonAdapter()).registerTypeAdapter(LocalDateTime.class, new LocalDateTimeTypeAdapter()).registerTypeAdapter(LocalDateTime.class, new LocalDateTypeAdapter()).create();

    public LessonController(LessonService service, LessonMapper mapper) {
        super(service, mapper);
    }

    @RequestMapping(
            value = {"/dto"},
            produces = {"application/json"},
            method = {RequestMethod.GET}
    )
    @PreAuthorize("#oauth2.hasScope('read') AND (hasAnyRole('ROLE_USER', 'ROLE_ADMIN'))")
    public ResponseEntity<List<LessonDTO>> getAllDTos() {
        return new ResponseEntity<>(this.mapper.toDto(this.service.getAll()), HttpStatus.OK);
    }

    @RequestMapping(
            value = {"/entity"},
            produces = {"application/json"},
            method = {RequestMethod.GET}
    )
    @PreAuthorize("#oauth2.hasScope('read') AND (hasAnyRole('ROLE_USER', 'ROLE_ADMIN'))")
    public ResponseEntity<List<LessonModel>> getAllEntities() {
        return new ResponseEntity<>(this.service.getAll(), HttpStatus.OK);
    }

    @RequestMapping(
            value = {"/search"},
            produces = {"application/json"},
            method = {RequestMethod.GET}
    )
    @PreAuthorize("#oauth2.hasScope('read') AND (hasAnyRole('ROLE_USER', 'ROLE_ADMIN'))")
    public ResponseEntity<List<LessonModel>> getByGroupAndDisciplineAndTeacher(@RequestParam(name = "gId") Long groupId, @RequestParam(name = "dId") Long disciplineId, @RequestParam(name = "tId") Long teacherId) {
        return new ResponseEntity<>(this.service.getByGroupDisciplineTeacher(groupId, disciplineId, teacherId), HttpStatus.OK);
    }

    @RequestMapping(
            value = {"/byDay"},
            produces = {"application/json"},
            method = {RequestMethod.GET}
    )
    @PreAuthorize("#oauth2.hasScope('read') AND (hasAnyRole('ROLE_USER', 'ROLE_ADMIN'))")
    public ResponseEntity<List<LessonModel>> getByDay(@RequestParam Short day) {
        return new ResponseEntity<>(this.service.getByDay(day), HttpStatus.OK);
    }

    @RequestMapping(
            value = {"/content"},
            produces = {"application/json"},
            method = {RequestMethod.GET}
    )
    @PreAuthorize("#oauth2.hasScope('read') AND (hasAnyRole('ROLE_USER', 'ROLE_ADMIN'))")
    public ResponseEntity<List<ContentDTO>> convert2ContentDTO(@RequestParam String date, @RequestParam(required = false, defaultValue = "") String room) {
        return new ResponseEntity<>(this.service.toContent(LocalDate.parse(date)).stream().sorted(Comparator.comparing(ContentDTO::getLessonNumber)).filter(p -> p.getLocation().contains(room)).toList(), HttpStatus.OK);
    }
}
