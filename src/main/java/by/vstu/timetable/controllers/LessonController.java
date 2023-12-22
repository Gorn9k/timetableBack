package by.vstu.timetable.controllers;

import by.vstu.dean.adapters.LocalDateTimeTypeAdapter;
import by.vstu.dean.adapters.LocalDateTypeAdapter;
import by.vstu.dean.adapters.json.LocalDateJsonAdapter;
import by.vstu.dean.adapters.json.LocalDateTimeJsonAdapter;
import by.vstu.dean.anotations.ApiSecurity;
import by.vstu.dean.controllers.common.BaseController;
import by.vstu.dean.future.models.merge.TeacherDepartmentMerge;
import by.vstu.dean.future.models.students.GroupModel;
import by.vstu.dean.requests.BaseRequest;
import by.vstu.timetable.dto.ContentDTO;
import by.vstu.timetable.dto.LessonDTO;
import by.vstu.timetable.dto.PatternDTO;
import by.vstu.timetable.dto.RoomDTO;
import by.vstu.timetable.dto.mapper.LessonMapper;
import by.vstu.timetable.models.LessonModel;
import by.vstu.timetable.repo.LessonModelRepository;
import by.vstu.timetable.repo.rest.GroupsRepo;
import by.vstu.timetable.services.LessonService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/lessons")
public class LessonController extends BaseController<LessonDTO, LessonModel, LessonMapper, LessonModelRepository, LessonService> {
    protected final Gson gson = (new GsonBuilder()).registerTypeAdapter(LocalDateTime.class, new LocalDateTimeJsonAdapter()).registerTypeAdapter(LocalDate.class, new LocalDateJsonAdapter()).registerTypeAdapter(LocalDateTime.class, new LocalDateTimeTypeAdapter()).registerTypeAdapter(LocalDateTime.class, new LocalDateTypeAdapter()).create();


    public LessonController(LessonService service) {
        super(service);
    }

    @RequestMapping(
            value = {"/dto"},
            produces = {"application/json"},
            method = {RequestMethod.GET}
    )
    @PreAuthorize("#oauth2.hasScope('read') AND (hasAnyRole('ROLE_USER', 'ROLE_ADMIN'))")
    public ResponseEntity<List<LessonDTO>> getAllDTos() {
        return new ResponseEntity<>(this.service.toDto(this.service.getAll()), HttpStatus.OK);
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

    @RequestMapping(
            value = {"/add_teachers"},
            produces = {"application/json"},
            method = {RequestMethod.GET}
    )
    @PreAuthorize("#oauth2.hasScope('read') AND (hasAnyRole('ROLE_USER', 'ROLE_ADMIN'))")
    public ResponseEntity<List<TeacherDepartmentMerge>> addTeachers2Departments() {
        return new ResponseEntity<>(this.service.addTeachers2Departments(), HttpStatus.OK);
    }


        @RequestMapping(
            value = {"/old"},
            produces = {"application/json"},
            method = {RequestMethod.GET}
    )
    @PreAuthorize("#oauth2.hasScope('read') AND (hasAnyRole('ROLE_USER', 'ROLE_ADMIN'))")
    public ResponseEntity<List<LessonModel>> getFromOldTimetable() {

        BaseRequest<String> request = new BaseRequest<>("http://192.168.11.252:8083/patterns");
        request.setMethod(HttpMethod.GET);
        request.setMediaType(MediaType.APPLICATION_JSON);
        String json = request.run("");
        List<PatternDTO> patternDTOS = gson.fromJson(json, new TypeToken<List<PatternDTO>>(){}.getType());

        return new ResponseEntity<>(this.service.fromPatterns(patternDTOS), HttpStatus.OK);
    }
}
