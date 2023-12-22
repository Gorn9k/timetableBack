package by.vstu.timetable.services.common;

import by.vstu.dean.enums.ELessonType;
import by.vstu.dean.future.models.lessons.TeacherModel;
import by.vstu.dean.future.models.rooms.ClassroomModel;
import by.vstu.dean.future.models.students.GroupModel;
import by.vstu.timetable.dto.LessonDTO;
import by.vstu.timetable.dto.RoomDTO;
import by.vstu.timetable.enums.ESubGroup;
import by.vstu.timetable.enums.EWeekType;
import by.vstu.timetable.models.LessonModel;
import by.vstu.timetable.models.StreamModel;
import by.vstu.timetable.repo.rest.ClassroomRepo;
import by.vstu.timetable.repo.rest.GroupsRepo;
import by.vstu.timetable.services.LessonService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class RoomService {

    private final LessonService lessonService;
    private final ClassroomRepo classroomRepo;
    private final GroupsRepo groupsRepo;
    private final CacheManager cacheManager;

    public RoomService(LessonService lessonService, ClassroomRepo classroomRepo, GroupsRepo groupsRepo, CacheManager cacheManager) {
        this.lessonService = lessonService;
        this.classroomRepo = classroomRepo;
        this.groupsRepo = groupsRepo;
        this.cacheManager = cacheManager;
    }

    private List<StreamModel> getStreams() {
        Vector<StreamModel> streams = new Vector<>();
        AtomicLong id = new AtomicLong(1);

        lessonService.getAll().stream().filter(p -> p.getSubGroup().equals(ESubGroup.ALL)).forEach(lesson -> {


            streams.stream().filter(p -> p.getStreamCourse() == groupsRepo.getSingle(lesson.getGroupId()).getYearStart().shortValue() && p.getDisciplineId() == lesson.getDisciplineId() && p.getTeacherId().equals(lesson.getTeacherId()) && p.getRoomId() == lesson.getRoomId() && lesson.getDay() == p.getDay() && lesson.getLessonNumber() == p.getLessonNumber() && lesson.getLessonType().equals(p.getLessonType())).forEach(stream -> {
//				System.out.printf("%s %s %s\n", stream.getDisciplineId(), lesson.getGroupId(), stream.getGroups());
                stream.add(lesson.getGroupId());
            });


            if (streams.stream().noneMatch(p -> p.getStreamCourse() == groupsRepo.getSingle(lesson.getGroupId()).getYearStart().shortValue() && p.getDisciplineId() == lesson.getDisciplineId() && p.getTeacherId().equals(lesson.getTeacherId()) && p.getRoomId() == lesson.getRoomId() && lesson.getDay() == p.getDay() && lesson.getLessonNumber() == p.getLessonNumber() && p.getLessonType().equals(lesson.getLessonType()))) {
                StreamModel streamModel = new StreamModel();
                streamModel.setLessonType(lesson.getLessonType());
                streamModel.setDisciplineId(lesson.getDisciplineId());
                streamModel.setDay(lesson.getDay());
                streamModel.setTeacherId(lesson.getTeacherId());
                streamModel.setRoomId(lesson.getRoomId());
                GroupModel group = groupsRepo.getSingle(lesson.getGroupId());
                streamModel.setStreamCourse(group.getYearStart().shortValue());
                streamModel.setLessonNumber(lesson.getLessonNumber());
                streamModel.setGroups(new HashSet<>(Collections.singletonList(lesson.getGroupId())));
                streamModel.setId(id.get());
                id.getAndIncrement();
                streams.add(streamModel);
            }


        });

        return streams.parallelStream().filter(p -> p.getGroups().size() > 1).toList();
    }

    public List<RoomDTO> getAll() {
        System.out.println(LocalDateTime.now().toString() + " [repo] Обращение в БД деканата");
        System.out.println("[repo] system.property access_token: " + System.getProperty("accessToken"));
        System.out.println("[repo] system.property expires_at: " + System.getProperty("expires_at"));
        System.out.println("[repo] expires_at converted: " + LocalDateTime.parse(System.getProperty("expires_at")));

        List<RoomDTO> rooms = new ArrayList<>();
        List<LessonDTO> lessonDTOs = lessonService.toDto(lessonService.getAllActive(true));

        lessonDTOs.forEach(lessonDTO -> rooms.add(this.convertToDto(lessonDTO, null)));

        this.classroomRepo.getAll().stream().distinct().forEach(room -> {
            for (int d = 0; d < 7; d++) {
                RoomDTO roomDTO = createEmptyRoomDTO(room, (short) d);
                rooms.add(roomDTO);
            }
        });


//        return rooms;
        return rooms.stream().sorted(Comparator.comparing(RoomDTO::getRoomNumber).thenComparing(RoomDTO::getDay).thenComparing(RoomDTO::getLessonNumber).thenComparing(RoomDTO::getWeekType)).toList();
    }

    public List<RoomDTO> convert2Rooms() {

        Vector<RoomDTO> rooms = new Vector<>();
        List<StreamModel> streams = this.getStreams();

        List<Thread> threads = new ArrayList<>();

        this.classroomRepo.getAll().forEach(room -> {

//            Thread roomThread = new Thread(() -> {

            List<LessonDTO> lessonDTOs = lessonService.toDto(lessonService.getByRoomId(room.getId()));

            if (!lessonDTOs.isEmpty())
                lessonDTOs.forEach(lessonDTO -> {
                    Optional<StreamModel> stream = streams.stream().filter(p -> p.getStreamCourse() == lessonDTO.getGroup().getYearStart() && p.getDisciplineId().equals(lessonDTO.getDiscipline().getId()) && p.getRoomId().equals(lessonDTO.getClassroom().getId()) && p.getLessonNumber() == lessonDTO.getLessonNumber() && p.getDay() == lessonDTO.getDay()).findFirst();

                    if (stream.isPresent()) {
                        rooms.add(this.convertToDto(lessonDTO, stream.get()));
                    } else
                        rooms.add(this.convertToDto(lessonDTO, null));
                });
//            });
//
//            roomThread.setName("room " + room.getRoomNumber() + " " + room.getFrame().getId());
//            threads.add(roomThread);
        });

//        for (Thread t : threads) t.start();
//
//        while(Thread.getAllStackTraces().keySet().stream().anyMatch(p -> p.getName().contains("room"))) {
//            try {
//                Thread.sleep(500);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//        }

        this.classroomRepo.getAll().stream().distinct().forEach(room -> {
            for (int d = 0; d < 7; d++) {
                RoomDTO roomDTO = createEmptyRoomDTO(room, (short) d);
                rooms.add(roomDTO);
            }
        });

        this.cacheManager.getCacheNames().forEach(name -> {
            Objects.requireNonNull(this.cacheManager.getCache(name)).clear();
        });

        return rooms.stream().sorted(Comparator.comparing(RoomDTO::getRoomNumber).thenComparing(RoomDTO::getDay).thenComparing(RoomDTO::getLessonNumber)).toList();
    }

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
        roomDTO.setLessonType(String.valueOf(ELessonType.UNKNOWN));
        return roomDTO;
    }

    public RoomDTO convertToDto(LessonDTO lessonDTO, StreamModel stream) {
        RoomDTO roomDTO = new RoomDTO();

        roomDTO.setId(lessonDTO.getId());
        roomDTO.setUpdated(LocalDateTime.now());
        roomDTO.setRoomId(lessonDTO.getClassroom().getId());
        roomDTO.setLessonId(lessonDTO.getId());
        roomDTO.setGroupId(lessonDTO.getGroup().getId());
        roomDTO.setTeacherId(lessonDTO.getTeacher().getId());
        roomDTO.setDisciplineId(lessonDTO.getDiscipline().getId());

        roomDTO.setRoomNumber(lessonDTO.getClassroom().getRoomNumber());

        if (stream == null)
            roomDTO.setGroup(lessonDTO.getGroup().getName());
        else {
            StringBuilder groups = new StringBuilder();

            stream.getGroups().forEach(g -> {
                groups.append(this.groupsRepo.getSingle(g).getName()).append(" ");
            });
            roomDTO.setStreamInternalId(stream.getId());
            roomDTO.setStream(groups.toString());
            roomDTO.setGroup(lessonDTO.getGroup().getName());
        }
        roomDTO.setLessonType(String.valueOf(lessonDTO.getLessonType()));
        roomDTO.setWeekType(String.valueOf(lessonDTO.getWeekType()));
        roomDTO.setRoomType(String.valueOf(lessonDTO.getClassroom().getRoomType()));
        roomDTO.setFrame(String.valueOf(lessonDTO.getClassroom().getFrame()));
        roomDTO.setLessonNumber(lessonDTO.getLessonNumber());
        roomDTO.setDisciplineName(lessonDTO.getDiscipline().getName());
        roomDTO.setDay(lessonDTO.getDay());
        //roomDTO.setSubGroup((short) lessonDTO.getSubGroup().ordinal());
        roomDTO.setSubGroup(lessonDTO.getSubGroup());
        roomDTO.setTeacherFullName(String.format("%s %s %s", lessonDTO.getTeacher().getSurname(), lessonDTO.getTeacher().getName(), lessonDTO.getTeacher().getPatronymic()));
        return roomDTO;
    }

    public LessonModel save(RoomDTO roomDTO) {

        if (roomDTO.getId() == null) {
            LessonModel l = this.lessonService.create(roomDTO);

            if (l != null)
                return this.lessonService.save(l);
            else
                return null;
        }

        Optional<LessonModel> o = this.lessonService.getById(roomDTO.getId());

        return o.map(lessonModel -> this.lessonService.save(this.lessonService.update(lessonModel, roomDTO))).orElse(null);

    }


    // Эксель расписания для групп определённого факультета, определённого курса
    // P.S. Какое задание, такой и код =))
    public Workbook getExcel(long facultyId, int course) {

        LocalDate dateNow = LocalDate.now();

        List<LessonDTO> lessons = lessonService.toDto(lessonService.getAllActive(true));
        lessons = lessons.stream()
                .filter(l -> l.getGroup().getFaculty().getId().equals(facultyId) &&
                        dateNow.minusYears(l.getGroup().getYearStart()).minusMonths(9).minusDays(1).getYear() == course - 1
                )
                .toList();

        Set<String> groups = new HashSet<>();
        Map<String, List<LessonDTO>> groupLessons = new HashMap<>();
        AtomicReference<String> faculty = new AtomicReference<>("");

        lessons.forEach(lessonDTO -> {
            faculty.set(lessonDTO.getGroup().getFaculty().getShortName());
            groups.add(lessonDTO.getGroup().getName());
            if (!groupLessons.containsKey(lessonDTO.getGroup().getName()))
                groupLessons.put(lessonDTO.getGroup().getName(), new ArrayList<>(Collections.singleton(lessonDTO)));
            else groupLessons.get(lessonDTO.getGroup().getName()).add(lessonDTO);
        });
        System.out.println(groups);

        Workbook workbook = null;
        try (FileInputStream fileInputStream = new FileInputStream("ras.xlsx")) {
            workbook = new XSSFWorkbook(fileInputStream);
        } catch (FileNotFoundException fileNotFoundException) {
            System.out.println("File ras.xlsx not found!");
        } catch (IOException ioException) {
            System.out.println("Incorrect import excel file!");
        }

        workbook.setSheetName(0, faculty.get() + " " + course + " курс");
        Sheet sheet = workbook.getSheetAt(0);
        Row row = sheet.createRow(0);
        for (int i = 0; i < 30; i++) {
            row.createCell(i);
        }

        int cellIndex = 4;

        for (String group : groups) {
            sheet.getRow(5).getCell(cellIndex).setCellValue(group);
            List<LessonDTO> currentLessons = groupLessons.get(group);

            int rowIndex = 6;

            for (int i = 0; i < 5; i++) {
                int day = i;
                List<LessonDTO> temp = currentLessons.stream().filter(lessonDTO -> lessonDTO.getDay() == day).toList();

                for (int j = 1; j <= 7; j++) {
                    int lessonNumber = j;
                    List<LessonDTO> dayLessons = temp.stream().filter(lessonDTO -> lessonDTO.getLessonNumber() == lessonNumber).sorted(Comparator.comparing(LessonDTO::getSubGroup).reversed()).toList();

                    if (!dayLessons.isEmpty()) {

                        for (LessonDTO lesson : dayLessons) {

                            if (lesson.getWeekType().equals(EWeekType.ALWAYS)) {
                                switch (lesson.getSubGroup()) {
                                    case ALL:
                                        sheet.getRow(rowIndex).getCell(cellIndex - 1).setCellValue(fillClassroomCell(lesson));
                                        sheet.getRow(rowIndex).getCell(cellIndex).setCellValue(fillDisciplineCell(lesson));

                                        // Объеденить ячейки в дисциплине
                                        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex + 1, cellIndex, cellIndex + 1));
                                        // Объеденить ячейки в аудитории
                                        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex + 1, cellIndex - 1, cellIndex - 1));
                                        break;
                                    case FIRST:
                                        sheet.getRow(rowIndex).getCell(cellIndex - 1).setCellValue(fillClassroomCell(lesson));
                                        sheet.getRow(rowIndex).getCell(cellIndex).setCellValue(fillDisciplineCell(lesson));

                                        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex + 1, cellIndex, cellIndex));
                                        break;
                                    case SECOND:
                                        sheet.getRow(rowIndex + 1).getCell(cellIndex - 1).setCellValue(fillClassroomCell(lesson));
                                        sheet.getRow(rowIndex).getCell(cellIndex + 1).setCellValue(fillDisciplineCell(lesson));

                                        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex + 1, cellIndex + 1, cellIndex + 1));
                                        break;
                                    default:
                                        break;
                                }
                            }

                            if (lesson.getWeekType().equals(EWeekType.NUMERATOR)) {
                                switch (lesson.getSubGroup()) {
                                    case ALL:
                                        sheet.getRow(rowIndex).getCell(cellIndex - 1).setCellValue(fillClassroomCell(lesson));
                                        sheet.getRow(rowIndex).getCell(cellIndex).setCellValue(fillDisciplineCell(lesson));

                                        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, cellIndex, cellIndex + 1));
                                        break;
                                    case FIRST:
                                        sheet.getRow(rowIndex).getCell(cellIndex - 1).setCellValue(fillClassroomCell(lesson));
                                        sheet.getRow(rowIndex).getCell(cellIndex).setCellValue(fillDisciplineCell(lesson));
                                        break;
                                    case SECOND:
                                        sheet.getRow(rowIndex).getCell(cellIndex - 1).setCellValue(fillClassroomCell(lesson));
                                        sheet.getRow(rowIndex).getCell(cellIndex + 1).setCellValue(fillDisciplineCell(lesson));
                                        break;
                                    default:
                                        break;
                                }
                            }

                            if (lesson.getWeekType().equals(EWeekType.DENOMINATOR)) {
                                switch (lesson.getSubGroup()) {
                                    case ALL:
                                        sheet.getRow(rowIndex + 1).getCell(cellIndex - 1).setCellValue(fillClassroomCell(lesson));
                                        sheet.getRow(rowIndex + 1).getCell(cellIndex).setCellValue(fillDisciplineCell(lesson));

                                        sheet.addMergedRegion(new CellRangeAddress(rowIndex + 1, rowIndex + 1, cellIndex, cellIndex + 1));
                                        break;
                                    case FIRST:
                                        sheet.getRow(rowIndex + 1).getCell(cellIndex - 1).setCellValue(fillClassroomCell(lesson));
                                        sheet.getRow(rowIndex + 1).getCell(cellIndex).setCellValue(fillDisciplineCell(lesson));
                                        break;
                                    case SECOND:
                                        sheet.getRow(rowIndex + 1).getCell(cellIndex - 1).setCellValue(fillClassroomCell(lesson));
                                        sheet.getRow(rowIndex + 1).getCell(cellIndex + 1).setCellValue(fillDisciplineCell(lesson));
                                        break;
                                    default:
                                        break;
                                }
                            }

                            if (lesson.getWeekType().equals(EWeekType.FIRST)) {
                                switch (lesson.getSubGroup()) {
                                    case ALL:
                                        sheet.getRow(rowIndex).getCell(cellIndex - 1).setCellValue(fillClassroomCell(lesson));
                                        sheet.getRow(rowIndex).getCell(cellIndex).setCellValue(fillDisciplineCell(lesson) + "(1н)");

                                        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, cellIndex, cellIndex + 1));
                                        break;
                                    case FIRST:
                                        sheet.getRow(rowIndex).getCell(cellIndex - 1).setCellValue(fillClassroomCell(lesson));
                                        sheet.getRow(rowIndex).getCell(cellIndex).setCellValue(fillDisciplineCell(lesson) + "(1н)");
                                        break;
                                    case SECOND:
                                        sheet.getRow(rowIndex).getCell(cellIndex - 1).setCellValue(fillClassroomCell(lesson));
                                        sheet.getRow(rowIndex).getCell(cellIndex + 1).setCellValue(fillDisciplineCell(lesson) + "(1н)");
                                        break;
                                    default:
                                        break;
                                }
                            }

                            if (lesson.getWeekType().equals(EWeekType.SECOND)) {
                                switch (lesson.getSubGroup()) {
                                    case ALL:
                                        sheet.getRow(rowIndex + 1).getCell(cellIndex - 1).setCellValue(fillClassroomCell(lesson));
                                        sheet.getRow(rowIndex + 1).getCell(cellIndex).setCellValue(fillDisciplineCell(lesson) + "(2н)");

                                        sheet.addMergedRegion(new CellRangeAddress(rowIndex + 1, rowIndex + 1, cellIndex, cellIndex + 1));
                                        break;
                                    case FIRST:
                                        sheet.getRow(rowIndex + 1).getCell(cellIndex - 1).setCellValue(fillClassroomCell(lesson));
                                        sheet.getRow(rowIndex + 1).getCell(cellIndex).setCellValue(fillDisciplineCell(lesson) + "(2н)");
                                        break;
                                    case SECOND:
                                        sheet.getRow(rowIndex + 1).getCell(cellIndex - 1).setCellValue(fillClassroomCell(lesson));
                                        sheet.getRow(rowIndex + 1).getCell(cellIndex + 1).setCellValue(fillDisciplineCell(lesson) + "(2н)");
                                        break;
                                    default:
                                        break;
                                }
                            }

                            if (lesson.getWeekType().equals(EWeekType.THIRD)) {
                                switch (lesson.getSubGroup()) {
                                    case ALL:
                                        sheet.getRow(rowIndex).getCell(cellIndex - 1).setCellValue(fillClassroomCell(lesson));
                                        sheet.getRow(rowIndex).getCell(cellIndex).setCellValue(fillDisciplineCell(lesson) + "(3н)");

                                        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, cellIndex, cellIndex + 1));
                                        break;
                                    case FIRST:
                                        sheet.getRow(rowIndex).getCell(cellIndex - 1).setCellValue(fillClassroomCell(lesson));
                                        sheet.getRow(rowIndex).getCell(cellIndex).setCellValue(fillDisciplineCell(lesson) + "(3н)");
                                        break;
                                    case SECOND:
                                        sheet.getRow(rowIndex).getCell(cellIndex - 1).setCellValue(fillClassroomCell(lesson));
                                        sheet.getRow(rowIndex).getCell(cellIndex + 1).setCellValue(fillDisciplineCell(lesson) + "(3н)");
                                        break;
                                    default:
                                        break;
                                }
                            }

                            if (lesson.getWeekType().equals(EWeekType.FOURTH)) {
                                switch (lesson.getSubGroup()) {
                                    case ALL:
                                        sheet.getRow(rowIndex + 1).getCell(cellIndex - 1).setCellValue(fillClassroomCell(lesson));
                                        sheet.getRow(rowIndex + 1).getCell(cellIndex).setCellValue(fillDisciplineCell(lesson) + "(4н)");

                                        sheet.addMergedRegion(new CellRangeAddress(rowIndex + 1, rowIndex + 1, cellIndex, cellIndex + 1));
                                        break;
                                    case FIRST:
                                        sheet.getRow(rowIndex + 1).getCell(cellIndex - 1).setCellValue(fillClassroomCell(lesson));
                                        sheet.getRow(rowIndex + 1).getCell(cellIndex).setCellValue(fillDisciplineCell(lesson) + "(4н)");
                                        break;
                                    case SECOND:
                                        sheet.getRow(rowIndex + 1).getCell(cellIndex - 1).setCellValue(fillClassroomCell(lesson));
                                        sheet.getRow(rowIndex + 1).getCell(cellIndex + 1).setCellValue(fillDisciplineCell(lesson) + "(4н)");
                                        break;
                                    default:
                                        break;
                                }
                            }
                        }

                    }
                    rowIndex += 2;
                }

            }
            cellIndex += 3;
        }

        return workbook;
    }

    private String fillDisciplineCell(LessonDTO lesson) {
        return convertLessonType(lesson.getLessonType()) + " " + lesson.getDiscipline().getName() + ", " + getTeacherFio(lesson.getTeacher());
    }

    private String fillClassroomCell(LessonDTO lesson) {
        return lesson.getClassroom().getFrame().getId() + "-" + lesson.getClassroom().getRoomNumber();
    }

    private String convertLessonType(ELessonType lessonType) {
        return switch (lessonType) {
            case LECTURE -> "лк.";
            case PRACTICE -> "пр.";
            case LAB -> "лб.";
            default -> "unknown";
        };
    }

    private String getTeacherFio(TeacherModel teacher) {
        return teacher.getSurname() + " " + teacher.getName().split("")[0] + "." + teacher.getPatronymic().split("")[0] + ".";
    }
}
