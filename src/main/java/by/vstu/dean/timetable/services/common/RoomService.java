package by.vstu.dean.timetable.services.common;

import by.vstu.dean.core.enums.EStatus;
import by.vstu.dean.support.models.ClassroomModel;
import by.vstu.dean.timetable.dto.LessonDTO;
import by.vstu.dean.timetable.dto.RoomDTO;
import by.vstu.dean.timetable.dto.mapper.LessonMapper;
import by.vstu.dean.timetable.enums.ELessonTypes;
import by.vstu.dean.timetable.enums.EWeekType;
import by.vstu.dean.timetable.models.LessonModel;
import by.vstu.dean.timetable.repo.LessonModelRepository;
import by.vstu.dean.timetable.repo.rest.ClassroomRepo;
import by.vstu.dean.timetable.repo.rest.GroupsRepo;
import by.vstu.dean.timetable.services.LessonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class RoomService {

    private static final Logger log = LoggerFactory.getLogger(RoomService.class);
    private final LessonService lessonService;
    private final ClassroomRepo classroomRepo;
    private final LessonMapper lessonMapper;
    private final LessonModelRepository lessonRepo;

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${api.journal}")
    private String journalUrl;

    public RoomService(LessonMapper lessonMapper, LessonService lessonService, ClassroomRepo classroomRepo, GroupsRepo groupsRepo, CacheManager cacheManager, LessonModelRepository lessonRepo) {
        this.lessonService = lessonService;
        this.classroomRepo = classroomRepo;
        this.lessonRepo = lessonRepo;
        this.lessonMapper = lessonMapper;
    }

//    @Deprecated
//    private List<StreamModel> getStreams() {
//        Vector<StreamModel> streams = new Vector<>();
//        AtomicLong id = new AtomicLong(1);
//
//        lessonService.getAll().stream().filter(p -> p.getSubGroup().equals(ESubGroup.ALL)).forEach(lesson -> {
//
//
//            streams.stream().filter(p -> p.getStreamCourse() == groupsRepo.getSingle(lesson.getGroupId()).getYearStart().shortValue() && p.getDisciplineId() == lesson.getDisciplineId() && p.getTeacherId().equals(lesson.getTeacherId()) && p.getRoomId() == lesson.getRoomId() && lesson.getDay() == p.getDay() && lesson.getLessonNumber() == p.getLessonNumber() && lesson.getLessonType().equals(p.getLessonType())).forEach(stream -> {
////				System.out.printf("%s %s %s\n", stream.getDisciplineId(), lesson.getGroupId(), stream.getGroups());
//                stream.add(lesson.getGroupId());
//            });
//
//
//            if (streams.stream().noneMatch(p -> p.getStreamCourse() == groupsRepo.getSingle(lesson.getGroupId()).getYearStart().shortValue() && p.getDisciplineId() == lesson.getDisciplineId() && p.getTeacherId().equals(lesson.getTeacherId()) && p.getRoomId() == lesson.getRoomId() && lesson.getDay() == p.getDay() && lesson.getLessonNumber() == p.getLessonNumber() && p.getLessonType().equals(lesson.getLessonType()))) {
//                StreamModel streamModel = new StreamModel();
//                streamModel.setLessonType(lesson.getLessonType());
//                streamModel.setDisciplineId(lesson.getDisciplineId());
//                streamModel.setDay(lesson.getDay());
//                streamModel.setTeacherId(lesson.getTeacherId());
//                streamModel.setRoomId(lesson.getRoomId());
//                GroupModel group = groupsRepo.getSingle(lesson.getGroupId());
//                streamModel.setStreamCourse(group.getYearStart().shortValue());
//                streamModel.setLessonNumber(lesson.getLessonNumber());
//                streamModel.setGroups(new HashSet<>(Collections.singletonList(lesson.getGroupId())));
//                streamModel.setId(id.get());
//                id.getAndIncrement();
//                streams.add(streamModel);
//            }
//
//
//        });
//
//        return streams.parallelStream().filter(p -> p.getGroups().size() > 1).toList();
//    }

    public List<RoomDTO> getAll() {

        List<RoomDTO> rooms = new ArrayList<>();
        List<LessonDTO> lessonDTOs = this.lessonMapper.toDto(lessonService.getAllActive(true));

        lessonDTOs.forEach(lessonDTO -> rooms.add(this.convertToDto(lessonDTO)));

        this.classroomRepo.getAll().stream().filter(c -> c.getStatus().equals(EStatus.ACTIVE)).distinct().forEach(room -> {
            for (int d = 0; d < 7; d++) {
                RoomDTO roomDTO = createEmptyRoomDTO(room, (short) d);
                rooms.add(roomDTO);
            }
        });

        return rooms.stream().sorted(Comparator.comparing(RoomDTO::getRoomNumber).thenComparing(RoomDTO::getDay).thenComparing(RoomDTO::getLessonNumber).thenComparing(RoomDTO::getWeekType)).toList();
    }

//    @Deprecated
//    public List<RoomDTO> convert2Rooms() {
//
//        Vector<RoomDTO> rooms = new Vector<>();
//        List<StreamModel> streams = this.getStreams();
//
//        List<Thread> threads = new ArrayList<>();
//
//        this.classroomRepo.getAll().forEach(room -> {
//
////            Thread roomThread = new Thread(() -> {
//
//            List<LessonDTO> lessonDTOs = lessonService.toDto(lessonService.getByRoomId(room.getId()));
//
//            if (!lessonDTOs.isEmpty())
//                lessonDTOs.forEach(lessonDTO -> {
//                    Optional<StreamModel> stream = streams.stream().filter(p -> p.getStreamCourse() == lessonDTO.getGroup().getYearStart() && p.getDisciplineId().equals(lessonDTO.getDiscipline().getId()) && p.getRoomId().equals(lessonDTO.getClassroom().getId()) && p.getLessonNumber() == lessonDTO.getLessonNumber() && p.getDay() == lessonDTO.getDay()).findFirst();
//
//                    if (stream.isPresent()) {
//                        rooms.add(this.convertToDto(lessonDTO, stream.get()));
//                    } else
//                        rooms.add(this.convertToDto(lessonDTO, null));
//                });
////            });
////
////            roomThread.setName("room " + room.getRoomNumber() + " " + room.getFrame().getId());
////            threads.add(roomThread);
//        });
//
////        for (Thread t : threads) t.start();
////
////        while(Thread.getAllStackTraces().keySet().stream().anyMatch(p -> p.getName().contains("room"))) {
////            try {
////                Thread.sleep(500);
////            } catch (InterruptedException e) {
////                throw new RuntimeException(e);
////            }
////        }
//
//        this.classroomRepo.getAll().stream().distinct().forEach(room -> {
//            for (int d = 0; d < 7; d++) {
//                RoomDTO roomDTO = createEmptyRoomDTO(room, (short) d);
//                rooms.add(roomDTO);
//            }
//        });
//
//        this.cacheManager.getCacheNames().forEach(name -> {
//            Objects.requireNonNull(this.cacheManager.getCache(name)).clear();
//        });
//
//        return rooms.stream().sorted(Comparator.comparing(RoomDTO::getRoomNumber).thenComparing(RoomDTO::getDay).thenComparing(RoomDTO::getLessonNumber)).toList();
//    }

    private RoomDTO createEmptyRoomDTO(ClassroomModel room, short day) {
        RoomDTO roomDTO = new RoomDTO();

        roomDTO.setRoomNumber(room.getRoomNumber());
        roomDTO.setRoomId(room.getId());
        roomDTO.setFrame(String.valueOf(room.getFrame()));
        roomDTO.setRoomType(String.valueOf(room.getRoomType()));
        roomDTO.setDay(day);
        roomDTO.setRoomId(room.getId());
        roomDTO.setWeekType(String.valueOf(EWeekType.ALWAYS));
        roomDTO.setLessonNumber((short) 20);
        roomDTO.setLessonType(String.valueOf(ELessonTypes.UNKNOWN));
        return roomDTO;
    }

    public RoomDTO convertToDto(LessonDTO lessonDTO) {
        RoomDTO roomDTO = new RoomDTO();

        roomDTO.setId(lessonDTO.getId());
        roomDTO.setUpdated(lessonDTO.getUpdated());
        roomDTO.setStatus(lessonDTO.getStatus());
        roomDTO.setRoomId(lessonDTO.getClassroom().getId());
        roomDTO.setLessonId(lessonDTO.getId());
        roomDTO.setGroupId(lessonDTO.getGroup().getId());
        roomDTO.setTeacherId(lessonDTO.getTeacher().getId());
        roomDTO.setDisciplineId(lessonDTO.getDiscipline().getId());

        roomDTO.setRoomNumber(lessonDTO.getClassroom().getRoomNumber());

        roomDTO.setGroup(lessonDTO.getGroup().getName());

//        if (stream == null) {
//            roomDTO.setGroup(lessonDTO.getGroup().getName());
//        }
//        else {
//            StringBuilder groups = new StringBuilder();
//
//            stream.getGroups().forEach(g -> {
//                groups.append(this.groupsRepo.getSingle(g).getName()).append(" ");
//            });
//            roomDTO.setStreamInternalId(stream.getId());
//            roomDTO.setStream(groups.toString());
//            roomDTO.setGroup(lessonDTO.getGroup().getName());
//        }


        roomDTO.setLessonType(String.valueOf(lessonDTO.getLessonType()));
        roomDTO.setWeekType(String.valueOf(lessonDTO.getWeekType()));
        roomDTO.setRoomType(String.valueOf(lessonDTO.getClassroom().getRoomType()));
        roomDTO.setFrame(String.valueOf(lessonDTO.getClassroom().getFrame()));
        roomDTO.setLessonNumber(lessonDTO.getLessonNumber());
        roomDTO.setDisciplineName(lessonDTO.getDiscipline().getName());
        roomDTO.setDay(lessonDTO.getDay());
        roomDTO.setSubGroup(lessonDTO.getSubGroup());
        roomDTO.setTeacherFullName(String.format("%s %s %s", lessonDTO.getTeacher().getSurname(), lessonDTO.getTeacher().getName(), lessonDTO.getTeacher().getPatronymic()));
        roomDTO.setStartDate(lessonDTO.getStartDate());
        roomDTO.setEndDate(lessonDTO.getEndDate());

        return roomDTO;
    }

    public RoomDTO save(RoomDTO roomDTO) {

        if (roomDTO.getId() == null) {
            LessonModel l = this.lessonService.create(roomDTO);

            if (l != null) {
                LessonModel lessonModel = this.lessonService.save(l);
                HttpEntity<Object> request = lessonService.newRequest(List.of(lessonModel));

                restTemplate.exchange(journalUrl + "utils", HttpMethod.POST, request, new ParameterizedTypeReference<List<LessonModel>>() {
                });
                return convertToDto(this.lessonMapper.toDto(lessonModel));
            } else
                return null;
        }

        Optional<LessonModel> o = this.lessonService.getById(roomDTO.getId());

        return o.map(lessonModel -> convertToDto(
                        this.lessonMapper.toDto(this.lessonService.save(this.lessonService.update(lessonModel, roomDTO))))
                )
                .orElse(null);
    }

    public ResponseEntity<?> checkIfExist(RoomDTO roomDTO) {
        if (roomDTO.getId() == null) {
            if (this.lessonRepo.existsByRoomIdAndTeacherIdAndDisciplineIdAndGroupIdAndSubGroupAndDayAndLessonNumberAndLessonTypeAndWeekTypeAndStatusAndStartDateAndEndDate(
                    roomDTO.getRoomId(),
                    roomDTO.getTeacherId(),
                    roomDTO.getDisciplineId(),
                    roomDTO.getGroupId(),
                    roomDTO.getSubGroup(),
                    roomDTO.getDay(),
                    roomDTO.getLessonNumber(),
                    ELessonTypes.valueOf(roomDTO.getLessonType()),
                    EWeekType.valueOf(roomDTO.getWeekType()),
                    EStatus.ACTIVE,
                    roomDTO.getStartDate(),
                    roomDTO.getEndDate()))
                return new ResponseEntity<>("Ошибка: Такая запись уже существует.", HttpStatus.CONFLICT);
            else return new ResponseEntity<>(HttpStatus.OK);
        }

        LessonModel lesson = lessonRepo.findById(roomDTO.getId()).get();

        if (roomDTO.getGroupId() != null)
            lesson.setGroupId(roomDTO.getGroupId());
        if (roomDTO.getTeacherId() != null)
            lesson.setTeacherId(roomDTO.getTeacherId());
        if (roomDTO.getDisciplineId() != null)
            lesson.setDisciplineId(roomDTO.getDisciplineId());
        if (roomDTO.getRoomId() != null)
            lesson.setRoomId(roomDTO.getRoomId());
        if (roomDTO.getLessonNumber() != null)
            lesson.setLessonNumber(roomDTO.getLessonNumber());
        if (roomDTO.getDay() != null)
            lesson.setDay(roomDTO.getDay());
        if (roomDTO.getLessonType() != null)
            lesson.setLessonType(ELessonTypes.valueOf(roomDTO.getLessonType()));
        if (roomDTO.getWeekType() != null)
            lesson.setWeekType(EWeekType.valueOf(roomDTO.getWeekType()));
        if (roomDTO.getSubGroup() != null)
            lesson.setSubGroup(roomDTO.getSubGroup());
        if (roomDTO.getStartDate() != null)
            lesson.setStartDate(roomDTO.getStartDate());
        if (roomDTO.getEndDate() != null)
            lesson.setEndDate(roomDTO.getEndDate());

        if (this.lessonRepo.existsByRoomIdAndTeacherIdAndDisciplineIdAndGroupIdAndSubGroupAndDayAndLessonNumberAndLessonTypeAndWeekTypeAndStatusAndStartDateAndEndDateAndLessonIdNot(
                lesson.getRoomId(),
                lesson.getTeacherId(),
                lesson.getDisciplineId(),
                lesson.getGroupId(),
                lesson.getSubGroup(),
                lesson.getDay(),
                lesson.getLessonNumber(),
                lesson.getLessonType(),
                lesson.getWeekType(),
                EStatus.ACTIVE,
                lesson.getStartDate(),
                lesson.getEndDate(),
                lesson.getId()))
            return new ResponseEntity<>("Ошибка: Такая запись уже существует.", HttpStatus.CONFLICT);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Проверка на конфликты новой пары и уже добавленные, проходящие в то же время в том же месте,
     * проверка на конфликт нескольких пар в одно время у одного препода.<br/>
     * P.S. Добавил говна, потому что сказали =))
     *
     * @param room Новое занятие
     * @return {@link ResponseEntity} со статусом {@link HttpStatus#CONFLICT}, если новая запись конфликтует с какими-то существующими, и
     * {@link HttpStatus#OK}, если конфликтов нет.
     */
    public ResponseEntity<?> checkForConflicts(RoomDTO room) {
        switch (EWeekType.valueOf(room.getWeekType())) {
            case ALWAYS:
                if (this.lessonRepo.existsByRoomIdAndLessonNumberAndDayAndStatusAndBetweenDates(room.getRoomId(), room.getLessonNumber(), room.getDay(), EStatus.ACTIVE, room.getStartDate(), room.getEndDate()))
                    return new ResponseEntity<>("Конфликт: В аудитории " + room.getRoomNumber() + " уже есть пара на это время.", HttpStatus.CONFLICT);

                if (this.lessonRepo.existsByTeacherIdAndLessonNumberAndDayAndStatusAndBetweenDates(room.getTeacherId(), room.getLessonNumber(), room.getDay(), EStatus.ACTIVE, room.getStartDate(), room.getEndDate()))
                    return new ResponseEntity<>("Конфликт: У преподавателя " + room.getTeacherFullName() + " уже есть пара на это время.", HttpStatus.CONFLICT);

                if (this.lessonRepo.existsByGroupIdAndSubGroupAndLessonNumberAndDayAndStatusAndBetweenDates(room.getGroupId(), room.getSubGroup(), room.getLessonNumber(), room.getDay(), EStatus.ACTIVE, room.getStartDate(), room.getEndDate()))
                    return new ResponseEntity<>("Конфликт: У группы " + room.getGroup() + " уже есть пара на это время.", HttpStatus.CONFLICT);


                break;

            case NUMERATOR:
                if (this.lessonRepo.existsByRoomIdAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(room.getRoomId(), room.getLessonNumber(), room.getDay(), EWeekType.ALWAYS, EStatus.ACTIVE, room.getStartDate(), room.getEndDate()) ||
                        this.lessonRepo.existsByRoomIdAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(room.getRoomId(), room.getLessonNumber(), room.getDay(), EWeekType.NUMERATOR, EStatus.ACTIVE, room.getStartDate(), room.getEndDate()) ||
                        this.lessonRepo.existsByRoomIdAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(room.getRoomId(), room.getLessonNumber(), room.getDay(), EWeekType.FIRST, EStatus.ACTIVE, room.getStartDate(), room.getEndDate()) ||
                        this.lessonRepo.existsByRoomIdAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(room.getRoomId(), room.getLessonNumber(), room.getDay(), EWeekType.THIRD, EStatus.ACTIVE, room.getStartDate(), room.getEndDate()))
                    return new ResponseEntity<>("Конфликт: В аудитории " + room.getRoomNumber() + " уже есть пара на это время.", HttpStatus.CONFLICT);

                if (this.lessonRepo.existsByTeacherIdAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(room.getTeacherId(), room.getLessonNumber(), room.getDay(), EWeekType.ALWAYS, EStatus.ACTIVE, room.getStartDate(), room.getEndDate()) ||
                        this.lessonRepo.existsByTeacherIdAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(room.getTeacherId(), room.getLessonNumber(), room.getDay(), EWeekType.NUMERATOR, EStatus.ACTIVE, room.getStartDate(), room.getEndDate()) ||
                        this.lessonRepo.existsByTeacherIdAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(room.getTeacherId(), room.getLessonNumber(), room.getDay(), EWeekType.FIRST, EStatus.ACTIVE, room.getStartDate(), room.getEndDate()) ||
                        this.lessonRepo.existsByTeacherIdAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(room.getTeacherId(), room.getLessonNumber(), room.getDay(), EWeekType.THIRD, EStatus.ACTIVE, room.getStartDate(), room.getEndDate()))
                    return new ResponseEntity<>("Конфликт: У преподавателя " + room.getTeacherFullName() + " уже есть пара на это время.", HttpStatus.CONFLICT);

                if (this.lessonRepo.existsByGroupIdAndSubGroupAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(room.getGroupId(), room.getSubGroup(), room.getLessonNumber(), room.getDay(), EWeekType.ALWAYS, EStatus.ACTIVE, room.getStartDate(), room.getEndDate()) ||
                        this.lessonRepo.existsByGroupIdAndSubGroupAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(room.getGroupId(), room.getSubGroup(), room.getLessonNumber(), room.getDay(), EWeekType.NUMERATOR, EStatus.ACTIVE, room.getStartDate(), room.getEndDate()) ||
                        this.lessonRepo.existsByGroupIdAndSubGroupAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(room.getGroupId(), room.getSubGroup(), room.getLessonNumber(), room.getDay(), EWeekType.FIRST, EStatus.ACTIVE, room.getStartDate(), room.getEndDate()) ||
                        this.lessonRepo.existsByGroupIdAndSubGroupAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(room.getGroupId(), room.getSubGroup(), room.getLessonNumber(), room.getDay(), EWeekType.THIRD, EStatus.ACTIVE, room.getStartDate(), room.getEndDate()))
                    return new ResponseEntity<>("Конфликт: У группы " + room.getGroup() + " уже есть пара на это время.", HttpStatus.CONFLICT);

                break;

            case DENOMINATOR:
                if (this.lessonRepo.existsByRoomIdAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(room.getRoomId(), room.getLessonNumber(), room.getDay(), EWeekType.ALWAYS, EStatus.ACTIVE, room.getStartDate(), room.getEndDate()) ||
                        this.lessonRepo.existsByRoomIdAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(room.getRoomId(), room.getLessonNumber(), room.getDay(), EWeekType.DENOMINATOR, EStatus.ACTIVE, room.getStartDate(), room.getEndDate()) ||
                        this.lessonRepo.existsByRoomIdAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(room.getRoomId(), room.getLessonNumber(), room.getDay(), EWeekType.SECOND, EStatus.ACTIVE, room.getStartDate(), room.getEndDate()) ||
                        this.lessonRepo.existsByRoomIdAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(room.getRoomId(), room.getLessonNumber(), room.getDay(), EWeekType.FOURTH, EStatus.ACTIVE, room.getStartDate(), room.getEndDate()))
                    return new ResponseEntity<>("Конфликт: В аудитории " + room.getRoomNumber() + " уже есть пара на это время.", HttpStatus.CONFLICT);

                if (this.lessonRepo.existsByTeacherIdAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(room.getTeacherId(), room.getLessonNumber(), room.getDay(), EWeekType.ALWAYS, EStatus.ACTIVE, room.getStartDate(), room.getEndDate()) ||
                        this.lessonRepo.existsByTeacherIdAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(room.getTeacherId(), room.getLessonNumber(), room.getDay(), EWeekType.DENOMINATOR, EStatus.ACTIVE, room.getStartDate(), room.getEndDate()) ||
                        this.lessonRepo.existsByTeacherIdAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(room.getTeacherId(), room.getLessonNumber(), room.getDay(), EWeekType.SECOND, EStatus.ACTIVE, room.getStartDate(), room.getEndDate()) ||
                        this.lessonRepo.existsByTeacherIdAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(room.getTeacherId(), room.getLessonNumber(), room.getDay(), EWeekType.FOURTH, EStatus.ACTIVE, room.getStartDate(), room.getEndDate()))
                    return new ResponseEntity<>("Конфликт: У преподавателя " + room.getTeacherFullName() + " уже есть пара на это время.", HttpStatus.CONFLICT);

                if (this.lessonRepo.existsByGroupIdAndSubGroupAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(room.getGroupId(), room.getSubGroup(), room.getLessonNumber(), room.getDay(), EWeekType.ALWAYS, EStatus.ACTIVE, room.getStartDate(), room.getEndDate()) ||
                        this.lessonRepo.existsByGroupIdAndSubGroupAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(room.getGroupId(), room.getSubGroup(), room.getLessonNumber(), room.getDay(), EWeekType.DENOMINATOR, EStatus.ACTIVE, room.getStartDate(), room.getEndDate()) ||
                        this.lessonRepo.existsByGroupIdAndSubGroupAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(room.getGroupId(), room.getSubGroup(), room.getLessonNumber(), room.getDay(), EWeekType.SECOND, EStatus.ACTIVE, room.getStartDate(), room.getEndDate()) ||
                        this.lessonRepo.existsByGroupIdAndSubGroupAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(room.getGroupId(), room.getSubGroup(), room.getLessonNumber(), room.getDay(), EWeekType.FOURTH, EStatus.ACTIVE, room.getStartDate(), room.getEndDate()))
                    return new ResponseEntity<>("Конфликт: У группы " + room.getGroup() + " уже есть пара на это время.", HttpStatus.CONFLICT);

                break;

            case FIRST:
                if (this.lessonRepo.existsByRoomIdAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(room.getRoomId(), room.getLessonNumber(), room.getDay(), EWeekType.ALWAYS, EStatus.ACTIVE, room.getStartDate(), room.getEndDate()) ||
                        this.lessonRepo.existsByRoomIdAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(room.getRoomId(), room.getLessonNumber(), room.getDay(), EWeekType.NUMERATOR, EStatus.ACTIVE, room.getStartDate(), room.getEndDate()) ||
                        this.lessonRepo.existsByRoomIdAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(room.getRoomId(), room.getLessonNumber(), room.getDay(), EWeekType.FIRST, EStatus.ACTIVE, room.getStartDate(), room.getEndDate()))
                    return new ResponseEntity<>("Конфликт: В аудитории " + room.getRoomNumber() + " уже есть пара на это время.", HttpStatus.CONFLICT);

                if (this.lessonRepo.existsByTeacherIdAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(room.getTeacherId(), room.getLessonNumber(), room.getDay(), EWeekType.ALWAYS, EStatus.ACTIVE, room.getStartDate(), room.getEndDate()) ||
                        this.lessonRepo.existsByTeacherIdAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(room.getTeacherId(), room.getLessonNumber(), room.getDay(), EWeekType.NUMERATOR, EStatus.ACTIVE, room.getStartDate(), room.getEndDate()) ||
                        this.lessonRepo.existsByTeacherIdAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(room.getTeacherId(), room.getLessonNumber(), room.getDay(), EWeekType.FIRST, EStatus.ACTIVE, room.getStartDate(), room.getEndDate()))
                    return new ResponseEntity<>("Конфликт: У преподавателя " + room.getTeacherFullName() + " уже есть пара на это время.", HttpStatus.CONFLICT);

                if (this.lessonRepo.existsByGroupIdAndSubGroupAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(room.getGroupId(), room.getSubGroup(), room.getLessonNumber(), room.getDay(), EWeekType.ALWAYS, EStatus.ACTIVE, room.getStartDate(), room.getEndDate()) ||
                        this.lessonRepo.existsByGroupIdAndSubGroupAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(room.getGroupId(), room.getSubGroup(), room.getLessonNumber(), room.getDay(), EWeekType.NUMERATOR, EStatus.ACTIVE, room.getStartDate(), room.getEndDate()) ||
                        this.lessonRepo.existsByGroupIdAndSubGroupAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(room.getGroupId(), room.getSubGroup(), room.getLessonNumber(), room.getDay(), EWeekType.FIRST, EStatus.ACTIVE, room.getStartDate(), room.getEndDate()))
                    return new ResponseEntity<>("Конфликт: У группы " + room.getGroup() + " уже есть пара на это время.", HttpStatus.CONFLICT);

                break;

            case SECOND:
                if (this.lessonRepo.existsByRoomIdAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(room.getRoomId(), room.getLessonNumber(), room.getDay(), EWeekType.ALWAYS, EStatus.ACTIVE, room.getStartDate(), room.getEndDate()) ||
                        this.lessonRepo.existsByRoomIdAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(room.getRoomId(), room.getLessonNumber(), room.getDay(), EWeekType.DENOMINATOR, EStatus.ACTIVE, room.getStartDate(), room.getEndDate()) ||
                        this.lessonRepo.existsByRoomIdAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(room.getRoomId(), room.getLessonNumber(), room.getDay(), EWeekType.SECOND, EStatus.ACTIVE, room.getStartDate(), room.getEndDate()))
                    return new ResponseEntity<>("Конфликт: В аудитории " + room.getRoomNumber() + " уже есть пара на это время.", HttpStatus.CONFLICT);

                if (this.lessonRepo.existsByTeacherIdAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(room.getTeacherId(), room.getLessonNumber(), room.getDay(), EWeekType.ALWAYS, EStatus.ACTIVE, room.getStartDate(), room.getEndDate()) ||
                        this.lessonRepo.existsByTeacherIdAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(room.getTeacherId(), room.getLessonNumber(), room.getDay(), EWeekType.DENOMINATOR, EStatus.ACTIVE, room.getStartDate(), room.getEndDate()) ||
                        this.lessonRepo.existsByTeacherIdAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(room.getTeacherId(), room.getLessonNumber(), room.getDay(), EWeekType.SECOND, EStatus.ACTIVE, room.getStartDate(), room.getEndDate()))
                    return new ResponseEntity<>("Конфликт: У преподавателя " + room.getTeacherFullName() + " уже есть пара на это время.", HttpStatus.CONFLICT);

                if (this.lessonRepo.existsByGroupIdAndSubGroupAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(room.getGroupId(), room.getSubGroup(), room.getLessonNumber(), room.getDay(), EWeekType.ALWAYS, EStatus.ACTIVE, room.getStartDate(), room.getEndDate()) ||
                        this.lessonRepo.existsByGroupIdAndSubGroupAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(room.getGroupId(), room.getSubGroup(), room.getLessonNumber(), room.getDay(), EWeekType.DENOMINATOR, EStatus.ACTIVE, room.getStartDate(), room.getEndDate()) ||
                        this.lessonRepo.existsByGroupIdAndSubGroupAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(room.getGroupId(), room.getSubGroup(), room.getLessonNumber(), room.getDay(), EWeekType.SECOND, EStatus.ACTIVE, room.getStartDate(), room.getEndDate()))
                    return new ResponseEntity<>("Конфликт: У группы " + room.getGroup() + " уже есть пара на это время.", HttpStatus.CONFLICT);

                break;

            case THIRD:
                if (this.lessonRepo.existsByRoomIdAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(room.getRoomId(), room.getLessonNumber(), room.getDay(), EWeekType.ALWAYS, EStatus.ACTIVE, room.getStartDate(), room.getEndDate()) ||
                        this.lessonRepo.existsByRoomIdAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(room.getRoomId(), room.getLessonNumber(), room.getDay(), EWeekType.NUMERATOR, EStatus.ACTIVE, room.getStartDate(), room.getEndDate()) ||
                        this.lessonRepo.existsByRoomIdAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(room.getRoomId(), room.getLessonNumber(), room.getDay(), EWeekType.THIRD, EStatus.ACTIVE, room.getStartDate(), room.getEndDate()))
                    return new ResponseEntity<>("Конфликт: В аудитории " + room.getRoomNumber() + " уже есть пара на это время.", HttpStatus.CONFLICT);

                if (this.lessonRepo.existsByTeacherIdAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(room.getTeacherId(), room.getLessonNumber(), room.getDay(), EWeekType.ALWAYS, EStatus.ACTIVE, room.getStartDate(), room.getEndDate()) ||
                        this.lessonRepo.existsByTeacherIdAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(room.getTeacherId(), room.getLessonNumber(), room.getDay(), EWeekType.NUMERATOR, EStatus.ACTIVE, room.getStartDate(), room.getEndDate()) ||
                        this.lessonRepo.existsByTeacherIdAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(room.getTeacherId(), room.getLessonNumber(), room.getDay(), EWeekType.THIRD, EStatus.ACTIVE, room.getStartDate(), room.getEndDate()))
                    return new ResponseEntity<>("Конфликт: У преподавателя " + room.getTeacherFullName() + " уже есть пара на это время.", HttpStatus.CONFLICT);

                if (this.lessonRepo.existsByGroupIdAndSubGroupAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(room.getGroupId(), room.getSubGroup(), room.getLessonNumber(), room.getDay(), EWeekType.ALWAYS, EStatus.ACTIVE, room.getStartDate(), room.getEndDate()) ||
                        this.lessonRepo.existsByGroupIdAndSubGroupAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(room.getGroupId(), room.getSubGroup(), room.getLessonNumber(), room.getDay(), EWeekType.NUMERATOR, EStatus.ACTIVE, room.getStartDate(), room.getEndDate()) ||
                        this.lessonRepo.existsByGroupIdAndSubGroupAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(room.getGroupId(), room.getSubGroup(), room.getLessonNumber(), room.getDay(), EWeekType.THIRD, EStatus.ACTIVE, room.getStartDate(), room.getEndDate()))
                    return new ResponseEntity<>("Конфликт: У группы " + room.getGroup() + " уже есть пара на это время.", HttpStatus.CONFLICT);

                break;

            case FOURTH:
                if (this.lessonRepo.existsByRoomIdAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(room.getRoomId(), room.getLessonNumber(), room.getDay(), EWeekType.ALWAYS, EStatus.ACTIVE, room.getStartDate(), room.getEndDate()) ||
                        this.lessonRepo.existsByRoomIdAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(room.getRoomId(), room.getLessonNumber(), room.getDay(), EWeekType.DENOMINATOR, EStatus.ACTIVE, room.getStartDate(), room.getEndDate()) ||
                        this.lessonRepo.existsByRoomIdAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(room.getRoomId(), room.getLessonNumber(), room.getDay(), EWeekType.FOURTH, EStatus.ACTIVE, room.getStartDate(), room.getEndDate()))
                    return new ResponseEntity<>("Конфликт: В аудитории " + room.getRoomNumber() + " уже есть пара на это время.", HttpStatus.CONFLICT);

                if (this.lessonRepo.existsByTeacherIdAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(room.getTeacherId(), room.getLessonNumber(), room.getDay(), EWeekType.ALWAYS, EStatus.ACTIVE, room.getStartDate(), room.getEndDate()) ||
                        this.lessonRepo.existsByTeacherIdAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(room.getTeacherId(), room.getLessonNumber(), room.getDay(), EWeekType.DENOMINATOR, EStatus.ACTIVE, room.getStartDate(), room.getEndDate()) ||
                        this.lessonRepo.existsByTeacherIdAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(room.getTeacherId(), room.getLessonNumber(), room.getDay(), EWeekType.FOURTH, EStatus.ACTIVE, room.getStartDate(), room.getEndDate()))
                    return new ResponseEntity<>("Конфликт: У преподавателя " + room.getTeacherFullName() + " уже есть пара на это время.", HttpStatus.CONFLICT);

                if (this.lessonRepo.existsByGroupIdAndSubGroupAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(room.getGroupId(), room.getSubGroup(), room.getLessonNumber(), room.getDay(), EWeekType.ALWAYS, EStatus.ACTIVE, room.getStartDate(), room.getEndDate()) ||
                        this.lessonRepo.existsByGroupIdAndSubGroupAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(room.getGroupId(), room.getSubGroup(), room.getLessonNumber(), room.getDay(), EWeekType.DENOMINATOR, EStatus.ACTIVE, room.getStartDate(), room.getEndDate()) ||
                        this.lessonRepo.existsByGroupIdAndSubGroupAndLessonNumberAndDayAndWeekTypeAndStatusAndBetweenDates(room.getGroupId(), room.getSubGroup(), room.getLessonNumber(), room.getDay(), EWeekType.FOURTH, EStatus.ACTIVE, room.getStartDate(), room.getEndDate()))
                    return new ResponseEntity<>("Конфликт: У группы " + room.getGroup() + " уже есть пара на это время.", HttpStatus.CONFLICT);

                break;
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
