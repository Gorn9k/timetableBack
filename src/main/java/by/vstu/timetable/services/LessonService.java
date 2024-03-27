package by.vstu.timetable.services;

import by.vstu.dean.adapters.LocalDateTimeTypeAdapter;
import by.vstu.dean.adapters.LocalDateTypeAdapter;
import by.vstu.dean.adapters.json.LocalDateJsonAdapter;
import by.vstu.dean.adapters.json.LocalDateTimeJsonAdapter;
import by.vstu.dean.enums.ELessonType;
import by.vstu.dean.enums.EStatus;
import by.vstu.dean.future.models.lessons.DepartmentModel;
import by.vstu.dean.future.models.lessons.DisciplineModel;
import by.vstu.dean.future.models.lessons.TeacherModel;
import by.vstu.dean.future.models.merge.TeacherDepartmentMerge;
import by.vstu.dean.future.models.rooms.ClassroomModel;
import by.vstu.dean.future.models.students.GroupModel;
import by.vstu.dean.requests.BaseRequest;
import by.vstu.dean.services.BaseService;
import by.vstu.timetable.dto.ContentDTO;
import by.vstu.timetable.dto.LessonDTO;
import by.vstu.timetable.dto.PatternDTO;
import by.vstu.timetable.dto.RoomDTO;
import by.vstu.timetable.dto.mapper.LessonMapper;
import by.vstu.timetable.enums.ESubGroup;
import by.vstu.timetable.enums.EWeekType;
import by.vstu.timetable.models.LessonModel;
import by.vstu.timetable.repo.LessonModelRepository;
import by.vstu.timetable.repo.rest.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.modelmapper.ModelMapper;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class LessonService extends BaseService<LessonDTO, LessonModel, LessonMapper, LessonModelRepository> {
    protected final Gson gson = (new GsonBuilder()).registerTypeAdapter(LocalDateTime.class, new LocalDateTimeJsonAdapter()).registerTypeAdapter(LocalDate.class, new LocalDateJsonAdapter()).registerTypeAdapter(LocalDateTime.class, new LocalDateTimeTypeAdapter()).registerTypeAdapter(LocalDateTime.class, new LocalDateTypeAdapter()).create();
    private final CacheManager cacheManager;
    protected final ModelMapper mapper;
    protected final GroupsRepo groupsRepo;
    protected final TeacherRepo teacherRepo;
    protected final DisciplinesRepo disciplinesRepo;
    protected final DepartmentsRepo departmentsRepo;
    protected final ClassroomRepo classroomRepo;

    public LessonService(LessonMapper mapper, LessonModelRepository repo, GroupsRepo groupsRepo, TeacherRepo teacherRepo, DisciplinesRepo disciplinesRepo, ClassroomRepo classroomRepo, CacheManager cacheManager, DepartmentsRepo departmentsRepo) {
        super(repo, mapper);
        this.classroomRepo = classroomRepo;
        this.departmentsRepo = departmentsRepo;
        this.mapper = new ModelMapper();
        this.groupsRepo = groupsRepo;
        this.teacherRepo = teacherRepo;
        this.disciplinesRepo = disciplinesRepo;
        this.cacheManager = cacheManager;
    }

    @Cacheable(value = "lessonModels", key = "#roomId")
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

        this.toDto(this.getAll()).stream().filter(p -> p.getDay() == date.getDayOfWeek().getValue() - 1).forEach(lessonModel -> {


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

    public List<TeacherDepartmentMerge> addTeachers2Departments() {
        BaseRequest<String> request = new BaseRequest<>("http://localhost:18076/api/teachers/merges/");
        request.setMethod(HttpMethod.GET);
        request.setToken("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOlsiZGVhbiIsInZzdHUtZWxlY3Ryb25pY2pvdXJuYWwiXSwidXNlcl9uYW1lIjoiYWRtaW5AZ21haWwuY29tIiwic2NvcGUiOlsicmVhZCIsInJzcWwiLCJ3cml0ZSIsImV4cG9ydCJdLCJyb2xlcyI6WyJTVFVERU5UIiwiQURNSU4iLCJVU0VSIiwiUkVDVE9SIl0sImV4cCI6MTY5ODM0MzU5OCwiZmlvIjoi0J_QuNCy0L4g0JwuINCcLiIsImF1dGhvcml0aWVzIjpbIlJPTEVfU1RVREVOVCIsIlJPTEVfQURNSU4iLCJST0xFX1VTRVIiLCJST0xFX1JFQ1RPUiJdLCJqdGkiOiJJa09VOFBjVHU1UVc3QlFpSWdLakVhSS1nSXMiLCJlbWFpbCI6ImFkbWluQGdtYWlsLmNvbSIsImlkX2Zyb21fc291cmNlIjoxNjI4MywiY2xpZW50X2lkIjoiREVBTl9SU1FMIn0.jCldOMRj51hNEq8DBD4v0kS6xdyjpGsALyHU5GwaCFs");
        String json = request.run("");
        Gson gson = (new GsonBuilder()).registerTypeAdapter(LocalDateTime.class, new LocalDateTimeJsonAdapter()).registerTypeAdapter(LocalDateTime.class, new LocalDateTimeTypeAdapter()).create();
        List<TeacherDepartmentMerge> old = gson.fromJson(json, new TypeToken<List<TeacherDepartmentMerge>>() {
        }.getType());


        List<TeacherDepartmentMerge> out = new ArrayList<>();

        this.repo.findAll().forEach(lessonModel -> {
            DisciplineModel disciplineModel = this.disciplinesRepo.getSingle(lessonModel.getDisciplineId());

            if (disciplineModel.getDepartment() == null)
                System.err.println("Got discipline without department: " + disciplineModel.getId());

            if (disciplineModel.getDepartment() != null
                    && old.stream().noneMatch(p -> p.getTeacher().getId().equals(lessonModel.getTeacherId()) && p.getDepartment().getId().equals(disciplineModel.getId()))
                    && out.stream().noneMatch(p -> p.getTeacher().getId().equals(lessonModel.getTeacherId()) && p.getDepartment().getId().equals(disciplineModel.getId()))
            ) {
                DepartmentModel departmentModel = this.departmentsRepo.getSingle(disciplineModel.getDepartment().getId());

                if (departmentModel == null)
                    System.err.println("Can't get department for discipline with id = " + disciplineModel.getId());

                TeacherModel teacherModel = this.teacherRepo.getSingle(lessonModel.getTeacherId());

                if (departmentModel == null)
                    System.err.println("Can't get teacher with id = " + lessonModel.getTeacherId());

                if (departmentModel != null && teacherModel != null) {
                    TeacherDepartmentMerge teacherDepartmentMerge = new TeacherDepartmentMerge();
                    teacherDepartmentMerge.setTeacher(teacherModel);
                    teacherDepartmentMerge.setDepartment(departmentModel);
                    teacherDepartmentMerge.setSourceId(0L);
                    teacherDepartmentMerge.setCreated(LocalDateTime.now());
                    teacherDepartmentMerge.setUpdated(LocalDateTime.now());
                    teacherDepartmentMerge.setStatus(EStatus.ACTIVE);

                    out.add(teacherDepartmentMerge);
                }

            }
        });

        this.cacheManager.getCacheNames().forEach(name -> {
            Objects.requireNonNull(this.cacheManager.getCache(name)).clear();
        });

        BaseRequest<String> request1 = new BaseRequest<>("http://localhost:18076/api/teachers/merges/put");
        request1.setMethod(HttpMethod.PUT);
        request1.setMediaType(MediaType.APPLICATION_JSON);
        request1.setToken("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOlsiZGVhbiIsInZzdHUtZWxlY3Ryb25pY2pvdXJuYWwiXSwidXNlcl9uYW1lIjoiYWRtaW5AZ21haWwuY29tIiwic2NvcGUiOlsicmVhZCIsInJzcWwiLCJ3cml0ZSIsImV4cG9ydCJdLCJyb2xlcyI6WyJTVFVERU5UIiwiQURNSU4iLCJVU0VSIiwiUkVDVE9SIl0sImV4cCI6MTY5ODM0MzU5OCwiZmlvIjoi0J_QuNCy0L4g0JwuINCcLiIsImF1dGhvcml0aWVzIjpbIlJPTEVfU1RVREVOVCIsIlJPTEVfQURNSU4iLCJST0xFX1VTRVIiLCJST0xFX1JFQ1RPUiJdLCJqdGkiOiJJa09VOFBjVHU1UVc3QlFpSWdLakVhSS1nSXMiLCJlbWFpbCI6ImFkbWluQGdtYWlsLmNvbSIsImlkX2Zyb21fc291cmNlIjoxNjI4MywiY2xpZW50X2lkIjoiREVBTl9SU1FMIn0.jCldOMRj51hNEq8DBD4v0kS6xdyjpGsALyHU5GwaCFs");
        String json1 = request1.run(gson.toJson(out));
        System.out.println(json1);

        return out;
    }


    public List<LessonModel> fromPatterns(List<PatternDTO> patterns) {
        List<LessonModel> lessons = new ArrayList<>();

        patterns.forEach(patternDTO -> {

            String rawGroups = this.groupsRepo.rawRSQl("name==" + patternDTO.getGroupName());
            String rawTeacherModels = this.teacherRepo.rawRSQl(String.format("surname==\"%s\";name==\"%s\";patronymic==\"%s\"", patternDTO.getTeacherFio().split(" ")[0].replace("ё", "*").replace("е", "*"), patternDTO.getTeacherFio().split(" ")[1].replace("ё", "*").replace("е", "*"), patternDTO.getTeacherFio().split(" ")[2]));
            String rawDisciplineModels = this.disciplinesRepo.rawRSQl(String.format("name==\"%s\" and department.id==%s", patternDTO.getDisciplineName().replace("ё", "*").replace("е", "*"), this.convertFromOldDean(patternDTO.getDepartmentId())));
            String rawRooms = this.classroomRepo.rawRSQl(String.format("roomNumber==%s;frame==%s", patternDTO.getLocation(), Long.parseLong(patternDTO.getFrame())));

            List<GroupModel> groups = this.gson.fromJson(rawGroups, new TypeToken<List<GroupModel>>() {
            }.getType());
            List<DisciplineModel> disciplineModels = this.gson.fromJson(rawDisciplineModels, new TypeToken<List<DisciplineModel>>() {
            }.getType());
            List<TeacherModel> teacherModels = this.gson.fromJson(rawTeacherModels, new TypeToken<List<TeacherModel>>() {
            }.getType());
            List<ClassroomModel> rooms = this.gson.fromJson(rawRooms, new TypeToken<List<ClassroomModel>>() {
            }.getType());

            if (disciplineModels.isEmpty()) {
                rawDisciplineModels = this.disciplinesRepo.rawRSQl(String.format("name==\"%s\"", patternDTO.getDisciplineName().replace(" (экономики)", "").replace("ё", "*").replace("е", "*")));
                disciplineModels = this.gson.fromJson(rawDisciplineModels, new TypeToken<List<DisciplineModel>>() {
                }.getType());
            }

            if (groups.isEmpty())
                throw new RuntimeException("Cannot find any group with name = " + patternDTO.getGroupName());

            if (teacherModels.isEmpty())
                throw new RuntimeException("Cannot find any teacher with name = " + patternDTO.getTeacherFio());

            if (disciplineModels.isEmpty())
                throw new RuntimeException("Cannot find any discipline with name = " + patternDTO.getDisciplineName());

            if (rooms.isEmpty())
                throw new RuntimeException("Cannot find any classroom with name = " + patternDTO.getLocation() + " and frame = " + patternDTO.getFrame());

            EWeekType weekType = this.convertWeekType(patternDTO.getWeekNumber(), patternDTO.getNumerator());
            ESubGroup subGroup = this.convertSubGroup(patternDTO.getSubGroup());
            Short day = this.convertDay(patternDTO.getLessonDay());
            ELessonType lessonType = this.convertLessonType(patternDTO.getTypeClassName());

            LessonModel lesson = new LessonModel();
            lesson.setSourceId(patternDTO.getId());
            lesson.setStatus(EStatus.ACTIVE);
            lesson.setDay(day);
            lesson.setWeekType(weekType);
            lesson.setSubGroup(subGroup);
            lesson.setLessonType(lessonType);
            lesson.setLessonNumber(patternDTO.getLessonNumber().shortValue());

            lesson.setDisciplineId(disciplineModels.get(0).getId());
            lesson.setGroupId(groups.get(0).getId());
            lesson.setTeacherId(teacherModels.get(0).getId());
            lesson.setRoomId(rooms.get(0).getId());

            lessons.add(lesson);
        });
        return this.saveAll(lessons);
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
            old.setLessonType(ELessonType.valueOf(dto.getLessonType()));

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
        lessonModel.setLessonType(ELessonType.valueOf(dto.getLessonType()));
        lessonModel.setLessonNumber(dto.getLessonNumber());
        lessonModel.setDay(dto.getDay());

        //FIXME: Уточнить даты семестра
        if (dto.getStartDate() == null && dto.getEndDate() == null) {
            int halfYear = LocalDate.now().getMonthValue() < 7 ? 1 : 2;
            if (halfYear == 1) {
                lessonModel.setStartDate(LocalDate.of(LocalDate.now().getYear(), 2, 1));
                lessonModel.setEndDate(LocalDate.of(LocalDate.now().getYear(), 6, 30));
            } else {
                lessonModel.setStartDate(LocalDate.of(LocalDate.now().getYear(), 9, 1));
                lessonModel.setEndDate(LocalDate.of(LocalDate.now().getYear() + 1, 1, 31));
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

        lessonModel.setStatus(EStatus.ACTIVE);

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

    private ELessonType convertLessonType(String lessonType) {
        return switch (lessonType) {
            case "Лабораторная работа" -> ELessonType.LAB;
            case "Практическая работа" -> ELessonType.PRACTICE;
            case "Лекция" -> ELessonType.LECTURE;
            default -> ELessonType.UNKNOWN;
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
}
