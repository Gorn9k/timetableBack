package by.vstu.dean.timetable.services;

import by.vstu.dean.core.adapters.LocalDateTimeTypeAdapter;
import by.vstu.dean.core.adapters.LocalDateTypeAdapter;
import by.vstu.dean.core.adapters.json.LocalDateJsonAdapter;
import by.vstu.dean.core.adapters.json.LocalDateTimeJsonAdapter;
import by.vstu.dean.core.enums.EStatus;
import by.vstu.dean.core.services.BaseService;
import by.vstu.dean.core.websocket.WSControllerManager;
import by.vstu.dean.timetable.dto.ContentDTO;
import by.vstu.dean.timetable.dto.LessonDTO;
import by.vstu.dean.timetable.dto.RoomDTO;
import by.vstu.dean.timetable.enums.ELessonTypes;
import by.vstu.dean.timetable.enums.ESubGroup;
import by.vstu.dean.timetable.enums.EWeekType;
import by.vstu.dean.timetable.models.LessonModel;
import by.vstu.dean.timetable.repo.LessonModelRepository;
import by.vstu.dean.timetable.repo.rest.*;
import by.vstu.dean.timetable.dto.mapper.LessonMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.javers.core.Javers;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class LessonService extends BaseService<LessonModel, LessonModelRepository> {
    protected final Gson gson = (new GsonBuilder()).registerTypeAdapter(LocalDateTime.class, new LocalDateTimeJsonAdapter()).registerTypeAdapter(LocalDate.class, new LocalDateJsonAdapter()).registerTypeAdapter(LocalDateTime.class, new LocalDateTimeTypeAdapter()).registerTypeAdapter(LocalDateTime.class, new LocalDateTypeAdapter()).create();
    private final CacheManager cacheManager;
    protected final ModelMapper mapper;
    protected final GroupsRepo groupsRepo;
    protected final TeacherRepo teacherRepo;
    protected final DisciplinesRepo disciplinesRepo;
    protected final DepartmentsRepo departmentsRepo;
    protected final ClassroomRepo classroomRepo;
    protected final LessonMapper lessonMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${api.auth}")
    private String authUrl;

    public LessonService(LessonMapper lessonMapper, WSControllerManager ws, Javers javers, LessonModelRepository repo, GroupsRepo groupsRepo, TeacherRepo teacherRepo, DisciplinesRepo disciplinesRepo, ClassroomRepo classroomRepo, CacheManager cacheManager, DepartmentsRepo departmentsRepo) {
        super(repo, javers, ws);
        this.classroomRepo = classroomRepo;
        this.lessonMapper = lessonMapper;
        this.departmentsRepo = departmentsRepo;
        this.mapper = new ModelMapper();
        this.groupsRepo = groupsRepo;
        this.teacherRepo = teacherRepo;
        this.disciplinesRepo = disciplinesRepo;
        this.cacheManager = cacheManager;
    }

//    @Override
//    public List<LessonModel> getAllActive(Boolean is) {
//        if (is == null || is)
//            return repo.findAllByStatus(EStatus.ACTIVE);
//        else
//            return Stream.concat(repo.findAllByStatus(EStatus.DELETED).stream(), repo.findAllByStatus(EStatus.INACTIVE).stream()).toList();
//    }


    @Override
    public LessonModel delete(Long id) {
        Optional<LessonModel> l = this.repo.findById(id);

        if (l.isEmpty())
            return null;

        LessonModel lesson = l.get();
        lesson.setStatus(EStatus.DELETED);
        lesson.setVisible(false);

        return this.repo.saveAndFlush(lesson);
    }

    public List<LessonModel> getByRoomId(Long roomId) {
        return this.repo.findAllByRoomId(roomId);
    }

    public List<LessonModel> getByGroupDisciplineTeacher(long groupId, long disciplineId, long teacherId) {
        return this.repo.findByGroupIdAndDisciplineIdAndTeacherIdAndStatus(groupId, disciplineId, teacherId, EStatus.ACTIVE);
    }

    public List<LessonModel> getByDay(short day) {
        return this.repo.findByDayAndStatus(day, EStatus.ACTIVE);
    }

    public List<ContentDTO> toContent(LocalDate date) {
        List<ContentDTO> out = new ArrayList<>();

        this.lessonMapper.toDto(this.getAll()).stream().filter(p -> p.getDay() == date.getDayOfWeek().getValue() - 1).forEach(lessonModel -> {


            ContentDTO contentDTO = new ContentDTO();

            contentDTO.setLessonNumber((int) lessonModel.getLessonNumber());
            contentDTO.setFrame(String.valueOf(lessonModel.getClassroom().getFrame()));
            contentDTO.setTypeClassName(String.valueOf(lessonModel.getClassroom().getRoomType()));
            contentDTO.setLocation(lessonModel.getClassroom().getRoomNumber());
            contentDTO.setDisciplineName(lessonModel.getDiscipline().getName());
            contentDTO.setTeacherFio(String.format("%s %s %s", lessonModel.getTeacher().getSurname(), lessonModel.getTeacher().getName(), lessonModel.getTeacher().getPatronymic()));
            contentDTO.setLessonDate(date);
            contentDTO.setGroupName(lessonModel.getGroup().getName());
            out.add(contentDTO);
        });

        return out;
    }

    public List<LessonModel> setVisibility(Long groupId, Boolean visible, String date1, String date2) {
        LocalDate dateFrom = LocalDate.parse(date1);
        LocalDate dateTo = LocalDate.parse(date2);
        List<LessonModel> lessons = this.repo.findByGroupIdAndStatusAndStartDateBetweenAndEndDateBetween(groupId, EStatus.ACTIVE, dateFrom, dateTo);
        lessons.forEach(l -> l.setVisible(visible));

        return this.saveAll(lessons);
    }

    public List<String> getHiddenGroups() {
        List<LessonDTO> lessons = this.lessonMapper.toDto(this.repo.findByStatusAndVisible(EStatus.ACTIVE, false));
        return lessons.stream().map(l -> l.getGroup().getName()).distinct().toList();
    }

    public LessonModel update(LessonModel old, RoomDTO dto) {

        if (dto.getRoomId() != null)
            old.setRoomId(dto.getRoomId());

        if (dto.getDay() != null)
            old.setDay(dto.getDay());

        if (dto.getTeacherId() != null)
            old.setTeacherId(dto.getTeacherId());

        if (dto.getWeekType() != null)
            old.setWeekType(EWeekType.valueOf(dto.getWeekType()));

        if (dto.getSubGroup() != null)
            old.setSubGroup(dto.getSubGroup());

        if (dto.getLessonType() != null)
            old.setLessonType(ELessonTypes.valueOf(dto.getLessonType()));

        if (dto.getLessonNumber() != null)
            old.setLessonNumber(dto.getLessonNumber());

        if (dto.getGroupId() != null)
            old.setGroupId(dto.getGroupId());

        if (dto.getDisciplineId() != null)
            old.setDisciplineId(dto.getDisciplineId());

        if (dto.getStartDate() != null)
            old.setStartDate(dto.getStartDate());

        if (dto.getEndDate() != null)
            old.setEndDate(dto.getEndDate());

        if (old.getStartDate().equals(old.getEndDate()) && (old.getStartDate().getDayOfWeek().getValue() - 1 != old.getDay())) {
            old.setDay((short) (old.getStartDate().getDayOfWeek().getValue() - 1));
        }

        if (dto.getVisible() != null)
            old.setVisible(dto.getVisible());

        old.setUpdated(LocalDateTime.now());

        return old;
    }

    public LessonModel create(RoomDTO dto) {

        LessonModel lessonModel = new LessonModel();

        if (dto.getDay() == null
                || dto.getLessonNumber() == null
                || dto.getGroupId() == null
                || dto.getLessonType() == null
                || dto.getDisciplineId() == null
                || dto.getTeacherId() == null
                || dto.getWeekType() == null
                || dto.getSubGroup() == null
                || dto.getRoomId() == null
        )
            return null;

        lessonModel.setUpdated(LocalDateTime.now());
        lessonModel.setCreated(LocalDateTime.now());
        lessonModel.setSourceId(null);

        lessonModel.setGroupId(dto.getGroupId());
        lessonModel.setTeacherId(dto.getTeacherId());
        lessonModel.setDisciplineId(dto.getDisciplineId());
        lessonModel.setRoomId(dto.getRoomId());

        lessonModel.setWeekType(EWeekType.valueOf(dto.getWeekType()));
        lessonModel.setSubGroup(dto.getSubGroup());
        lessonModel.setLessonType(ELessonTypes.valueOf(dto.getLessonType()));
        lessonModel.setLessonNumber(dto.getLessonNumber());
        lessonModel.setDay(dto.getDay());

        if (dto.getStartDate() == null && dto.getEndDate() == null) {
            int halfYear = LocalDate.now().getMonthValue() < 7 ? 1 : 2;
            if (halfYear == 1) {
                lessonModel.setStartDate(LocalDate.of(LocalDate.now().getYear(), 2, 1));
                lessonModel.setEndDate(LocalDate.of(LocalDate.now().getYear(), 5, 31));
            } else {
                lessonModel.setStartDate(LocalDate.of(LocalDate.now().getYear(), 9, 1));
                lessonModel.setEndDate(LocalDate.of(LocalDate.now().getYear(), 12, 31));
            }
        } else if (dto.getStartDate() == null) {
            lessonModel.setStartDate(dto.getEndDate());
            lessonModel.setEndDate(dto.getEndDate());
        } else if (dto.getEndDate() == null) {
            lessonModel.setStartDate(dto.getStartDate());
            lessonModel.setEndDate(dto.getStartDate());
        } else {
            lessonModel.setStartDate(dto.getStartDate());
            lessonModel.setEndDate(dto.getEndDate());
        }
        if (lessonModel.getStartDate().isEqual(lessonModel.getEndDate()) &&
                lessonModel.getStartDate().getDayOfWeek().getValue() != (lessonModel.getDay() + 1)) {
            lessonModel.setDay((short) (lessonModel.getStartDate().getDayOfWeek().getValue() - 1));
        }

        lessonModel.setStatus(EStatus.ACTIVE);
        lessonModel.setVisible(false);

        return lessonModel;
    }

    private Long convertFromOldDean(long id) {

        return switch ((int) id) {
            case 1 -> 1L;
            case 2 -> 2L;
            case 4 -> 3L;
            case 7 -> 4L;
            case 8 -> 5L;
            case 9 -> 6L;
            case 10 -> 7L;
            case 11 -> 8L;
            case 15 -> 9L;
            case 18 -> 10L;
            case 22 -> 11L;
            case 23 -> 12L;
            case 25 -> 13L;
            case 26 -> 14L;
            case 27 -> 15L;
            case 29 -> 16L;
            case 30 -> 17L;
            case 31 -> 18L;
            case 32 -> 19L;
            case 33 -> 20L;
            case 34 -> 21L;
            case 35 -> 21L;
            case 999413999 -> 1L;
            default -> throw new IllegalStateException("Unexpected value: " + (int) id);
        };
    }

    private ELessonTypes convertLessonType(String lessonType) {
        return switch (lessonType) {
            case "Лабораторная работа" -> ELessonTypes.LAB;
            case "Практическая работа" -> ELessonTypes.PRACTICE;
            case "Лекция" -> ELessonTypes.LECTURE;
            default -> ELessonTypes.UNKNOWN;
        };
    }

    private ESubGroup convertSubGroup(int subGroup) {
        return switch (subGroup) {
            case 0 -> ESubGroup.ALL;
            case 1 -> ESubGroup.FIRST;
            case 2 -> ESubGroup.SECOND;
            default -> ESubGroup.UNKNOWN;
        };
    }

    private EWeekType convertWeekType(Integer weekType, Boolean numerator) {
        if (numerator != null)
            return numerator ? EWeekType.NUMERATOR : EWeekType.DENOMINATOR;

        if (weekType == null)
            return EWeekType.ALWAYS;

        return switch (weekType) {
            case 1 -> EWeekType.FIRST;
            case 2 -> EWeekType.SECOND;
            case 3 -> EWeekType.THIRD;
            case 4 -> EWeekType.FOURTH;
            default -> EWeekType.ALWAYS;
        };
    }

    private Short convertDay(String day) {
        return switch (day) {
            case "MONDAY" -> 0;
            case "TUESDAY" -> 1;
            case "WEDNESDAY" -> 2;
            case "THURSDAY" -> 3;
            case "FRIDAY" -> 4;
            case "SATURDAY" -> 5;
            case "SUNDAY" -> 6;
            default -> -1;
        };
    }

    public HttpEntity<MultiValueMap<String, String>> newRequest() {
        HttpHeaders headers = new HttpHeaders();
        String token = getTokenFromAuthService();
        headers.add("Authorization", "Bearer " + token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(headers);
    }

    public HttpEntity<Object> newRequest(Object body) {
        HttpHeaders headers = new HttpHeaders();
        String token = getTokenFromAuthService();
        headers.add("Authorization", "Bearer " + token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private String getTokenFromAuthService() {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("username", "admin@gmail.com");
        body.add("password", "admin");
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Basic " + "VlNUVV9FTEVDVFJPTklDSk9VUk5BTF9DTElFTlQ6VlNUVV9FTEVDVFJPTklDSk9VUk5BTF9DTElFTlQ=");
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(authUrl + "token?grant_type=password",
                HttpMethod.POST, request, new ParameterizedTypeReference<>() {
                });
        return Objects.requireNonNull(response.getBody()).split("\"")[3];
    }
}
