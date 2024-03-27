package by.vstu.timetable.services.common;

import by.vstu.dean.enums.ELessonType;
import by.vstu.dean.enums.EStatus;
import by.vstu.dean.future.models.FacultyModel;
import by.vstu.dean.future.models.lessons.TeacherModel;
import by.vstu.dean.future.models.rooms.ClassroomModel;
import by.vstu.dean.future.models.students.GroupModel;
import by.vstu.timetable.dto.LessonDTO;
import by.vstu.timetable.dto.RoomDTO;
import by.vstu.timetable.enums.ESubGroup;
import by.vstu.timetable.enums.EWeekType;
import by.vstu.timetable.models.LessonModel;
import by.vstu.timetable.models.StreamModel;
import by.vstu.timetable.repo.LessonModelRepository;
import by.vstu.timetable.repo.rest.ClassroomRepo;
import by.vstu.timetable.repo.rest.GroupsRepo;
import by.vstu.timetable.services.LessonService;
import org.apache.poi.ss.usermodel.*;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class RoomService {

    private final LessonService lessonService;
    private final ClassroomRepo classroomRepo;
    private final GroupsRepo groupsRepo;
    private final CacheManager cacheManager;
    private final LessonModelRepository lessonRepo;

    public RoomService(LessonService lessonService, ClassroomRepo classroomRepo, GroupsRepo groupsRepo, CacheManager cacheManager, LessonModelRepository lessonRepo) {
        this.lessonService = lessonService;
        this.classroomRepo = classroomRepo;
        this.groupsRepo = groupsRepo;
        this.cacheManager = cacheManager;
        this.lessonRepo = lessonRepo;
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

        List<RoomDTO> rooms = new ArrayList<>();
        List<LessonDTO> lessonDTOs = lessonService.toDto(lessonService.getAllActive(true));

        lessonDTOs.forEach(lessonDTO -> rooms.add(this.convertToDto(lessonDTO, null)));

        this.classroomRepo.getAll().stream().distinct().forEach(room -> {
            for (int d = 0; d < 7; d++) {
                RoomDTO roomDTO = createEmptyRoomDTO(room, (short) d);
                rooms.add(roomDTO);
            }
        });

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
        roomDTO.setSubGroup(lessonDTO.getSubGroup());
        roomDTO.setTeacherFullName(String.format("%s %s %s", lessonDTO.getTeacher().getSurname(), lessonDTO.getTeacher().getName(), lessonDTO.getTeacher().getPatronymic()));
        roomDTO.setStartDate(lessonDTO.getStartDate());
        roomDTO.setEndDate(lessonDTO.getEndDate());

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

        lessons.forEach(lessonDTO -> {
            groups.add(lessonDTO.getGroup().getName());
            if (!groupLessons.containsKey(lessonDTO.getGroup().getName()))
                groupLessons.put(lessonDTO.getGroup().getName(), new ArrayList<>(Collections.singleton(lessonDTO)));
            else groupLessons.get(lessonDTO.getGroup().getName()).add(lessonDTO);
        });
        System.out.println(groups);

        Workbook workbook = null;
        try (FileInputStream fileInputStream = new FileInputStream("rasSize" + (groups.size() <= 4 ? "4" : groups.size()) + ".xlsx")) {
            workbook = new XSSFWorkbook(fileInputStream);
        } catch (FileNotFoundException fileNotFoundException) {
            System.out.println("File ras.xlsx not found!");
        } catch (IOException ioException) {
            System.out.println("Incorrect import excel file!");
        }

        FacultyModel faculty = lessons.get(0).getGroup().getFaculty();
        workbook.setSheetName(0, faculty.getName() + " " + course + " курс");
        Sheet sheet = workbook.getSheetAt(0);
//        Row row = sheet.createRow(0);
//        for (int i = 0; i < 30; i++) {
//            row.createCell(i);
//        }

        int halfYear = dateNow.getMonthValue() < 7 ? 1 : 2;
        sheet.getRow(1).getCell(8).setCellValue((facultyId == 2 || facultyId == 5 ? "" : "факультет ") + faculty.getName().toLowerCase());
        sheet.getRow(2).getCell(8).setCellValue((halfYear == 1 ? "весенний" : "осенний") + " семестр, " +
                (halfYear == 1 ? dateNow.minusYears(1).getYear() + "/" + dateNow.getYear() : dateNow.getYear() + "/" + dateNow.plusYears(1).getYear())
                + " учебный год");
        sheet.getRow(78).getCell(0).setCellValue("Декан " + (facultyId == 1 || facultyId == 2 || facultyId == 5 ? "" : "факультета ") + faculty.getNameGenitive().toLowerCase());
        sheet.getRow(78).getCell(7).setCellValue(faculty.getDean());

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
                    List<LessonDTO> dayLessons = temp.stream().filter(lessonDTO -> lessonDTO.getLessonNumber() == lessonNumber).toList();

                    if (!dayLessons.isEmpty()) {

                        for (LessonDTO lesson : dayLessons) {

//
                            String classrooms = "";
                            List<LessonDTO> tempList = lessonRepo.findByGroupIdAndDayAndLessonNumberAndStatus(
                                    lesson.getGroup().getId(),
                                    lesson.getDay(),
                                    lesson.getLessonNumber(),
                                    EStatus.ACTIVE).stream().map(lessonService::toDto).sorted(Comparator.comparing(LessonDTO::getWeekType).thenComparing(LessonDTO::getSubGroup)).toList();

                            try {

                                if (lesson.getWeekType().equals(EWeekType.ALWAYS)) {
                                    switch (lesson.getSubGroup()) {
                                        case ALL:
                                            classrooms = tempList.stream().map(this::fillClassroomCell).distinct().collect(Collectors.joining(", ", "", ""));

                                            sheet.getRow(rowIndex).getCell(cellIndex - 1).setCellValue(classrooms);
                                            sheet.getRow(rowIndex).getCell(cellIndex).setCellValue(fillDisciplineCell(lesson));

                                            // Объеденить ячейки в дисциплине
                                            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex + 1, cellIndex, cellIndex + 1));
                                            // Объеденить ячейки в аудитории
                                            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex + 1, cellIndex - 1, cellIndex - 1));
                                            break;
                                        case FIRST:
                                            classrooms = tempList.stream().filter(l -> l.getSubGroup().equals(ESubGroup.FIRST)).map(this::fillClassroomCell).distinct().collect(Collectors.joining(", ", "", ""));

                                            sheet.getRow(rowIndex).getCell(cellIndex - 1).setCellValue(classrooms);
                                            sheet.getRow(rowIndex).getCell(cellIndex).setCellValue(fillDisciplineCell(lesson));

                                            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex + 1, cellIndex, cellIndex));
                                            break;
                                        case SECOND:
                                            classrooms = tempList.stream().filter(l -> l.getSubGroup().equals(ESubGroup.SECOND)).map(this::fillClassroomCell).distinct().collect(Collectors.joining(", ", "", ""));

                                            sheet.getRow(rowIndex + 1).getCell(cellIndex - 1).setCellValue(classrooms);
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
                                            classrooms = tempList.stream().filter(l -> l.getWeekType().equals(EWeekType.NUMERATOR)).map(this::fillClassroomCell).distinct().collect(Collectors.joining(", ", "", ""));

                                            sheet.getRow(rowIndex).getCell(cellIndex - 1).setCellValue(classrooms);
                                            sheet.getRow(rowIndex).getCell(cellIndex).setCellValue(fillDisciplineCell(lesson));

                                            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, cellIndex, cellIndex + 1));
                                            break;
                                        case FIRST:
                                            if (tempList.stream().anyMatch(l -> l.getSubGroup().equals(ESubGroup.SECOND) && l.getWeekType().equals(EWeekType.ALWAYS))) {
                                                classrooms = tempList.stream().filter(l -> l.getSubGroup().equals(ESubGroup.FIRST)).map(this::fillClassroomCell).distinct().collect(Collectors.joining(", ", "", ""));
                                            } else {
                                                classrooms = tempList.stream().filter(l -> l.getWeekType().equals(EWeekType.NUMERATOR)).map(this::fillClassroomCell).distinct().collect(Collectors.joining(", ", "", ""));
                                            }

                                            sheet.getRow(rowIndex).getCell(cellIndex - 1).setCellValue(classrooms);
                                            sheet.getRow(rowIndex).getCell(cellIndex).setCellValue(fillDisciplineCell(lesson));
                                            break;
                                        case SECOND:
                                            if (tempList.stream().anyMatch(l -> l.getSubGroup().equals(ESubGroup.FIRST) && l.getWeekType().equals(EWeekType.ALWAYS))) {
                                                classrooms = tempList.stream().filter(l -> l.getSubGroup().equals(ESubGroup.SECOND)).map(this::fillClassroomCell).distinct().collect(Collectors.joining(", ", "", ""));
                                                sheet.getRow(rowIndex + 1).getCell(cellIndex - 1).setCellValue(classrooms);
                                            } else {
                                                classrooms = tempList.stream().filter(l -> l.getWeekType().equals(EWeekType.NUMERATOR)).map(this::fillClassroomCell).distinct().collect(Collectors.joining(", ", "", ""));
                                                sheet.getRow(rowIndex).getCell(cellIndex - 1).setCellValue(classrooms);
                                            }

                                            sheet.getRow(rowIndex).getCell(cellIndex + 1).setCellValue(fillDisciplineCell(lesson));
                                            break;
                                        default:
                                            break;
                                    }
                                }

                                if (lesson.getWeekType().equals(EWeekType.DENOMINATOR)) {
                                    switch (lesson.getSubGroup()) {
                                        case ALL:
                                            classrooms = tempList.stream().filter(l -> l.getWeekType().equals(EWeekType.DENOMINATOR)).map(this::fillClassroomCell).distinct().collect(Collectors.joining(", ", "", ""));

                                            sheet.getRow(rowIndex + 1).getCell(cellIndex - 1).setCellValue(classrooms);
                                            sheet.getRow(rowIndex + 1).getCell(cellIndex).setCellValue(fillDisciplineCell(lesson));

                                            sheet.addMergedRegion(new CellRangeAddress(rowIndex + 1, rowIndex + 1, cellIndex, cellIndex + 1));
                                            break;
                                        case FIRST:
                                            if (tempList.stream().anyMatch(l -> l.getSubGroup().equals(ESubGroup.SECOND) && l.getWeekType().equals(EWeekType.ALWAYS))) {
                                                classrooms = tempList.stream().filter(l -> l.getSubGroup().equals(ESubGroup.FIRST)).map(this::fillClassroomCell).distinct().collect(Collectors.joining(", ", "", ""));
                                                sheet.getRow(rowIndex).getCell(cellIndex - 1).setCellValue(classrooms);
                                            } else {
                                                classrooms = tempList.stream().filter(l -> l.getWeekType().equals(EWeekType.DENOMINATOR)).map(this::fillClassroomCell).distinct().collect(Collectors.joining(", ", "", ""));
                                                sheet.getRow(rowIndex + 1).getCell(cellIndex - 1).setCellValue(classrooms);
                                            }

                                            sheet.getRow(rowIndex + 1).getCell(cellIndex).setCellValue(fillDisciplineCell(lesson));
                                            break;
                                        case SECOND:
                                            if (tempList.stream().anyMatch(l -> l.getSubGroup().equals(ESubGroup.FIRST) && l.getWeekType().equals(EWeekType.ALWAYS))) {
                                                classrooms = tempList.stream().filter(l -> l.getSubGroup().equals(ESubGroup.SECOND)).map(this::fillClassroomCell).distinct().collect(Collectors.joining(", ", "", ""));
                                            } else {
                                                classrooms = tempList.stream().filter(l -> l.getWeekType().equals(EWeekType.DENOMINATOR)).map(this::fillClassroomCell).distinct().collect(Collectors.joining(", ", "", ""));
                                            }
                                            sheet.getRow(rowIndex + 1).getCell(cellIndex - 1).setCellValue(classrooms);
                                            sheet.getRow(rowIndex + 1).getCell(cellIndex + 1).setCellValue(fillDisciplineCell(lesson));
                                            break;
                                        default:
                                            break;
                                    }
                                }

                                if (lesson.getWeekType().equals(EWeekType.FIRST)) {
                                    switch (lesson.getSubGroup()) {
                                        case ALL:
                                            classrooms = Stream.concat(
                                                            this.lessonRepo.findByGroupIdAndDayAndWeekTypeAndLessonNumberAndSubGroupAndStatus(
                                                                    lesson.getGroup().getId(),
                                                                    lesson.getDay(),
                                                                    EWeekType.FIRST,
                                                                    lesson.getLessonNumber(),
                                                                    ESubGroup.ALL,
                                                                    EStatus.ACTIVE).stream(),
                                                            this.lessonRepo.findByGroupIdAndDayAndWeekTypeAndLessonNumberAndSubGroupAndStatus(
                                                                            lesson.getGroup().getId(),
                                                                            lesson.getDay(),
                                                                            EWeekType.THIRD,
                                                                            lesson.getLessonNumber(),
                                                                            ESubGroup.ALL,
                                                                            EStatus.ACTIVE)
                                                                    .stream()).map(lessonService::toDto)
                                                    .map(this::fillClassroomCell)
                                                    .distinct()
                                                    .collect(Collectors.joining(", ", "", ""));

                                            sheet.getRow(rowIndex).getCell(cellIndex - 1).setCellValue(classrooms);
                                            sheet.getRow(rowIndex).getCell(cellIndex).setCellValue(fillDisciplineCell(lesson));

                                            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, cellIndex, cellIndex + 1));
                                            break;
                                        case FIRST:
                                            if (tempList.stream().anyMatch(l -> l.getSubGroup().equals(ESubGroup.SECOND) && l.getWeekType().equals(EWeekType.ALWAYS))) {
                                                classrooms = tempList.stream().filter(l -> l.getSubGroup().equals(ESubGroup.FIRST)).map(this::fillClassroomCell).distinct().collect(Collectors.joining(", ", "", ""));
                                            } else {
                                                classrooms = tempList.stream().filter(l -> l.getWeekType().equals(EWeekType.NUMERATOR) || l.getWeekType().equals(EWeekType.FIRST) || l.getWeekType().equals(EWeekType.THIRD)).map(this::fillClassroomCell).distinct().collect(Collectors.joining(", ", "", ""));
                                            }

                                            sheet.getRow(rowIndex).getCell(cellIndex - 1).setCellValue(classrooms);
                                            sheet.getRow(rowIndex).getCell(cellIndex).setCellValue(fillDisciplineCell(lesson));
                                            break;
                                        case SECOND:
                                            if (tempList.stream().anyMatch(l -> l.getSubGroup().equals(ESubGroup.FIRST) && l.getWeekType().equals(EWeekType.ALWAYS))) {
                                                classrooms = tempList.stream().filter(l -> l.getSubGroup().equals(ESubGroup.SECOND)).map(this::fillClassroomCell).distinct().collect(Collectors.joining(", ", "", ""));
                                                sheet.getRow(rowIndex + 1).getCell(cellIndex - 1).setCellValue(classrooms);
                                            } else {
                                                classrooms = tempList.stream().filter(l -> l.getWeekType().equals(EWeekType.NUMERATOR) || l.getWeekType().equals(EWeekType.FIRST) || l.getWeekType().equals(EWeekType.THIRD)).map(this::fillClassroomCell).distinct().collect(Collectors.joining(", ", "", ""));
                                                sheet.getRow(rowIndex).getCell(cellIndex - 1).setCellValue(classrooms);
                                            }

                                            sheet.getRow(rowIndex).getCell(cellIndex + 1).setCellValue(fillDisciplineCell(lesson));
                                            break;
                                        default:
                                            break;
                                    }
                                }

                                if (lesson.getWeekType().equals(EWeekType.SECOND)) {
                                    switch (lesson.getSubGroup()) {
                                        case ALL:
                                            classrooms = Stream.concat(
                                                            this.lessonRepo.findByGroupIdAndDayAndWeekTypeAndLessonNumberAndSubGroupAndStatus(
                                                                    lesson.getGroup().getId(),
                                                                    lesson.getDay(),
                                                                    EWeekType.SECOND,
                                                                    lesson.getLessonNumber(),
                                                                    ESubGroup.ALL,
                                                                    EStatus.ACTIVE).stream(),
                                                            this.lessonRepo.findByGroupIdAndDayAndWeekTypeAndLessonNumberAndSubGroupAndStatus(
                                                                            lesson.getGroup().getId(),
                                                                            lesson.getDay(),
                                                                            EWeekType.FOURTH,
                                                                            lesson.getLessonNumber(),
                                                                            ESubGroup.ALL,
                                                                            EStatus.ACTIVE)
                                                                    .stream()).map(lessonService::toDto)
                                                    .map(this::fillClassroomCell)
                                                    .distinct()
                                                    .collect(Collectors.joining(", ", "", ""));

                                            sheet.getRow(rowIndex + 1).getCell(cellIndex - 1).setCellValue(classrooms);
                                            sheet.getRow(rowIndex + 1).getCell(cellIndex).setCellValue(fillDisciplineCell(lesson));

                                            sheet.addMergedRegion(new CellRangeAddress(rowIndex + 1, rowIndex + 1, cellIndex, cellIndex + 1));
                                            break;
                                        case FIRST:
                                            if (tempList.stream().anyMatch(l -> l.getSubGroup().equals(ESubGroup.SECOND) && l.getWeekType().equals(EWeekType.ALWAYS))) {
                                                classrooms = tempList.stream().filter(l -> l.getSubGroup().equals(ESubGroup.FIRST)).map(this::fillClassroomCell).distinct().collect(Collectors.joining(", ", "", ""));
                                                sheet.getRow(rowIndex).getCell(cellIndex - 1).setCellValue(classrooms);
                                            } else {
                                                classrooms = tempList.stream().filter(l -> l.getWeekType().equals(EWeekType.DENOMINATOR) || l.getWeekType().equals(EWeekType.SECOND) || l.getWeekType().equals(EWeekType.FOURTH)).map(this::fillClassroomCell).distinct().collect(Collectors.joining(", ", "", ""));
                                                sheet.getRow(rowIndex + 1).getCell(cellIndex - 1).setCellValue(classrooms);
                                            }

                                            sheet.getRow(rowIndex + 1).getCell(cellIndex - 1).setCellValue(classrooms);
                                            sheet.getRow(rowIndex + 1).getCell(cellIndex).setCellValue(fillDisciplineCell(lesson));
                                            break;
                                        case SECOND:
                                            if (tempList.stream().anyMatch(l -> l.getSubGroup().equals(ESubGroup.FIRST) && l.getWeekType().equals(EWeekType.ALWAYS))) {
                                                classrooms = tempList.stream().filter(l -> l.getSubGroup().equals(ESubGroup.SECOND)).map(this::fillClassroomCell).distinct().collect(Collectors.joining(", ", "", ""));
                                            } else {
                                                classrooms = tempList.stream().filter(l -> l.getWeekType().equals(EWeekType.DENOMINATOR) || l.getWeekType().equals(EWeekType.SECOND) || l.getWeekType().equals(EWeekType.FOURTH)).map(this::fillClassroomCell).distinct().collect(Collectors.joining(", ", "", ""));
                                            }

                                            sheet.getRow(rowIndex + 1).getCell(cellIndex - 1).setCellValue(classrooms);
                                            sheet.getRow(rowIndex + 1).getCell(cellIndex + 1).setCellValue(fillDisciplineCell(lesson));
                                            break;
                                        default:
                                            break;
                                    }
                                }

                                if (lesson.getWeekType().equals(EWeekType.THIRD)) {
                                    switch (lesson.getSubGroup()) {
                                        case ALL:
                                            classrooms = Stream.concat(
                                                            this.lessonRepo.findByGroupIdAndDayAndWeekTypeAndLessonNumberAndSubGroupAndStatus(
                                                                    lesson.getGroup().getId(),
                                                                    lesson.getDay(),
                                                                    EWeekType.FIRST,
                                                                    lesson.getLessonNumber(),
                                                                    ESubGroup.ALL,
                                                                    EStatus.ACTIVE).stream(),
                                                            this.lessonRepo.findByGroupIdAndDayAndWeekTypeAndLessonNumberAndSubGroupAndStatus(
                                                                            lesson.getGroup().getId(),
                                                                            lesson.getDay(),
                                                                            EWeekType.THIRD,
                                                                            lesson.getLessonNumber(),
                                                                            ESubGroup.ALL,
                                                                            EStatus.ACTIVE)
                                                                    .stream()).map(lessonService::toDto)
                                                    .map(this::fillClassroomCell)
                                                    .distinct()
                                                    .collect(Collectors.joining(", ", "", ""));

                                            sheet.getRow(rowIndex).getCell(cellIndex - 1).setCellValue(classrooms);
                                            sheet.getRow(rowIndex).getCell(cellIndex).setCellValue(fillDisciplineCell(lesson));

                                            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, cellIndex, cellIndex + 1));
                                            break;
                                        case FIRST:
                                            if (tempList.stream().anyMatch(l -> l.getSubGroup().equals(ESubGroup.SECOND) && l.getWeekType().equals(EWeekType.ALWAYS))) {
                                                classrooms = tempList.stream().filter(l -> l.getSubGroup().equals(ESubGroup.FIRST)).map(this::fillClassroomCell).distinct().collect(Collectors.joining(", ", "", ""));
                                            } else {
                                                classrooms = tempList.stream().filter(l -> l.getWeekType().equals(EWeekType.NUMERATOR) || l.getWeekType().equals(EWeekType.FIRST) || l.getWeekType().equals(EWeekType.THIRD)).map(this::fillClassroomCell).distinct().collect(Collectors.joining(", ", "", ""));
                                            }

                                            sheet.getRow(rowIndex).getCell(cellIndex - 1).setCellValue(classrooms);
                                            sheet.getRow(rowIndex).getCell(cellIndex).setCellValue(fillDisciplineCell(lesson));
                                            break;
                                        case SECOND:
                                            if (tempList.stream().anyMatch(l -> l.getSubGroup().equals(ESubGroup.FIRST) && l.getWeekType().equals(EWeekType.ALWAYS))) {
                                                classrooms = tempList.stream().filter(l -> l.getSubGroup().equals(ESubGroup.SECOND)).map(this::fillClassroomCell).distinct().collect(Collectors.joining(", ", "", ""));
                                                sheet.getRow(rowIndex + 1).getCell(cellIndex - 1).setCellValue(classrooms);
                                            } else {
                                                classrooms = tempList.stream().filter(l -> l.getWeekType().equals(EWeekType.NUMERATOR) || l.getWeekType().equals(EWeekType.FIRST) || l.getWeekType().equals(EWeekType.THIRD)).map(this::fillClassroomCell).distinct().collect(Collectors.joining(", ", "", ""));
                                                sheet.getRow(rowIndex).getCell(cellIndex - 1).setCellValue(classrooms);
                                            }

                                            sheet.getRow(rowIndex).getCell(cellIndex + 1).setCellValue(fillDisciplineCell(lesson));
                                            break;
                                        default:
                                            break;
                                    }
                                }

                                if (lesson.getWeekType().equals(EWeekType.FOURTH)) {
                                    switch (lesson.getSubGroup()) {
                                        case ALL:
                                            classrooms = Stream.concat(
                                                            this.lessonRepo.findByGroupIdAndDayAndWeekTypeAndLessonNumberAndSubGroupAndStatus(
                                                                    lesson.getGroup().getId(),
                                                                    lesson.getDay(),
                                                                    EWeekType.SECOND,
                                                                    lesson.getLessonNumber(),
                                                                    ESubGroup.ALL,
                                                                    EStatus.ACTIVE).stream(),
                                                            this.lessonRepo.findByGroupIdAndDayAndWeekTypeAndLessonNumberAndSubGroupAndStatus(
                                                                            lesson.getGroup().getId(),
                                                                            lesson.getDay(),
                                                                            EWeekType.FOURTH,
                                                                            lesson.getLessonNumber(),
                                                                            ESubGroup.ALL,
                                                                            EStatus.ACTIVE)
                                                                    .stream())
                                                    .map(lessonService::toDto)
                                                    .map(this::fillClassroomCell)
                                                    .distinct()
                                                    .collect(Collectors.joining(", ", "", ""));

                                            sheet.getRow(rowIndex + 1).getCell(cellIndex - 1).setCellValue(classrooms);
                                            sheet.getRow(rowIndex + 1).getCell(cellIndex).setCellValue(fillDisciplineCell(lesson));

                                            sheet.addMergedRegion(new CellRangeAddress(rowIndex + 1, rowIndex + 1, cellIndex, cellIndex + 1));
                                            break;
                                        case FIRST:
                                            if (tempList.stream().anyMatch(l -> l.getSubGroup().equals(ESubGroup.SECOND) && l.getWeekType().equals(EWeekType.ALWAYS))) {
                                                classrooms = tempList.stream().filter(l -> l.getSubGroup().equals(ESubGroup.FIRST)).map(this::fillClassroomCell).distinct().collect(Collectors.joining(", ", "", ""));
                                                sheet.getRow(rowIndex).getCell(cellIndex - 1).setCellValue(classrooms);
                                            } else {
                                                classrooms = tempList.stream().filter(l -> l.getWeekType().equals(EWeekType.DENOMINATOR) || l.getWeekType().equals(EWeekType.SECOND) || l.getWeekType().equals(EWeekType.FOURTH)).map(this::fillClassroomCell).distinct().collect(Collectors.joining(", ", "", ""));
                                                sheet.getRow(rowIndex + 1).getCell(cellIndex - 1).setCellValue(classrooms);
                                            }

                                            sheet.getRow(rowIndex + 1).getCell(cellIndex).setCellValue(fillDisciplineCell(lesson));
                                            break;
                                        case SECOND:
                                            if (tempList.stream().anyMatch(l -> l.getSubGroup().equals(ESubGroup.FIRST) && l.getWeekType().equals(EWeekType.ALWAYS))) {
                                                classrooms = tempList.stream().filter(l -> l.getSubGroup().equals(ESubGroup.SECOND)).map(this::fillClassroomCell).distinct().collect(Collectors.joining(", ", "", ""));
                                            } else {
                                                classrooms = tempList.stream().filter(l -> l.getWeekType().equals(EWeekType.DENOMINATOR) || l.getWeekType().equals(EWeekType.SECOND) || l.getWeekType().equals(EWeekType.FOURTH)).map(this::fillClassroomCell).distinct().collect(Collectors.joining(", ", "", ""));
                                            }

                                            sheet.getRow(rowIndex + 1).getCell(cellIndex - 1).setCellValue(classrooms);
                                            sheet.getRow(rowIndex + 1).getCell(cellIndex + 1).setCellValue(fillDisciplineCell(lesson));
                                            break;
                                        default:
                                            break;
                                    }
                                }
                            } catch (IllegalStateException ignored) {
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

    public Workbook getExcelForCit() {
        Workbook workbook = null;
        try (FileInputStream fileInputStream = new FileInputStream("cit.xlsx")) {
            workbook = new XSSFWorkbook(fileInputStream);
        } catch (FileNotFoundException fileNotFoundException) {
            System.out.println("File ras.xlsx not found!");
        } catch (IOException ioException) {
            System.out.println("Incorrect import excel file!");
        }

        List<LessonDTO> lessons = lessonService.toDto(lessonRepo.findByStatusAndDate(EStatus.ACTIVE, LocalDate.now()))
                .stream().filter(l -> l.getClassroom().getId() == 49 || // 122
                        l.getClassroom().getId() == 50 || // 212
                        l.getClassroom().getId() == 51 || // 221
                        l.getClassroom().getId() == 52).sorted(Comparator.comparing(o -> o.getClassroom().getId())).toList(); // 417

        Set<String> classrooms = new HashSet<>();
        Map<String, List<LessonDTO>> classroomLessons = new HashMap<>();

        lessons.forEach(lessonDTO -> {
            classrooms.add(lessonDTO.getClassroom().getRoomNumber());
            if (!classroomLessons.containsKey(lessonDTO.getClassroom().getRoomNumber()))
                classroomLessons.put(lessonDTO.getClassroom().getRoomNumber(), new ArrayList<>(Collections.singleton(lessonDTO)));
            else classroomLessons.get(lessonDTO.getClassroom().getRoomNumber()).add(lessonDTO);
        });
        Set<String> finalClassrooms = classrooms.stream().sorted().collect(Collectors.toCollection(LinkedHashSet::new));

        Sheet sheet = workbook.getSheetAt(0);
        workbook.setSheetName(0, "Расписание аудиторий ЦИТ");
        CellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setWrapText(true);
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        cellStyle.setAlignment(HorizontalAlignment.CENTER);
        cellStyle.setBorderBottom(BorderStyle.THIN);
        cellStyle.setBorderTop(BorderStyle.THIN);
        cellStyle.setBorderLeft(BorderStyle.THIN);
        cellStyle.setBorderRight(BorderStyle.THIN);
        int rowIndex = 4, cellIndex = 2;

        for (String classroom : finalClassrooms) {
            List<LessonDTO> currentLessons = classroomLessons.get(classroom);

            for (int i = 0; i < 7; i++) {
                int day = i;
                List<LessonDTO> temp = currentLessons.stream().filter(lessonDTO -> lessonDTO.getDay() == day).toList();

                for (int j = 1; j <= 7; j++) {
                    int lessonNumber = j;
                    List<LessonDTO> dayLessons = temp.stream().filter(lessonDTO -> lessonDTO.getLessonNumber() == lessonNumber).toList();

                    if (!dayLessons.isEmpty()) {

                        for (LessonDTO lesson : dayLessons) {
                            if (sheet.getRow(rowIndex) == null)
                                sheet.createRow(rowIndex);
                            sheet.getRow(rowIndex).createCell(cellIndex).setCellStyle(cellStyle);

                            if (lesson.getStartDate().equals(LocalDate.of(2024, 2, 5)) && lesson.getEndDate().equals(LocalDate.of(2024, 6, 2)))
                                sheet.getRow(rowIndex).getCell(cellIndex).setCellValue(fillCitCell(lesson));
                            else
                                sheet.getRow(rowIndex).getCell(cellIndex).setCellValue(fillCitCell(lesson) + "\n" + lesson.getStartDate().toString() + " - " + lesson.getEndDate().toString());
                        }
                    }
                    rowIndex++;
                }

                rowIndex -= 7;
                cellIndex++;
            }

            rowIndex += 12;
            cellIndex = 2;
        }


        return workbook;
    }

    private String fillDisciplineCell(LessonDTO lesson) {
        String lessonType, teachers;
        List<LessonModel> lessons = new ArrayList<>();
        if (lesson.getWeekType().equals(EWeekType.FIRST) || lesson.getWeekType().equals(EWeekType.THIRD)) {
            lessons.addAll(this.lessonRepo.findByGroupIdAndDayAndWeekTypeAndLessonNumberAndSubGroupAndStatus(
                    lesson.getGroup().getId(),
                    lesson.getDay(),
                    EWeekType.FIRST,
                    lesson.getLessonNumber(),
                    lesson.getSubGroup(),
                    EStatus.ACTIVE));
            lessons.addAll(this.lessonRepo.findByGroupIdAndDayAndWeekTypeAndLessonNumberAndSubGroupAndStatus(
                    lesson.getGroup().getId(),
                    lesson.getDay(),
                    EWeekType.THIRD,
                    lesson.getLessonNumber(),
                    lesson.getSubGroup(),
                    EStatus.ACTIVE));
        }
        if (lesson.getWeekType().equals(EWeekType.SECOND) || lesson.getWeekType().equals(EWeekType.FOURTH)) {
            lessons.addAll(this.lessonRepo.findByGroupIdAndDayAndWeekTypeAndLessonNumberAndSubGroupAndStatus(
                    lesson.getGroup().getId(),
                    lesson.getDay(),
                    EWeekType.SECOND,
                    lesson.getLessonNumber(),
                    lesson.getSubGroup(),
                    EStatus.ACTIVE));
            lessons.addAll(this.lessonRepo.findByGroupIdAndDayAndWeekTypeAndLessonNumberAndSubGroupAndStatus(
                    lesson.getGroup().getId(),
                    lesson.getDay(),
                    EWeekType.FOURTH,
                    lesson.getLessonNumber(),
                    lesson.getSubGroup(),
                    EStatus.ACTIVE));
        }
        if (lesson.getWeekType().equals(EWeekType.ALWAYS) || lesson.getWeekType().equals(EWeekType.NUMERATOR) || lesson.getWeekType().equals(EWeekType.DENOMINATOR))
            lessons = this.lessonRepo.findByGroupIdAndDayAndWeekTypeAndLessonNumberAndSubGroupAndStatus(
                    lesson.getGroup().getId(),
                    lesson.getDay(),
                    lesson.getWeekType(),
                    lesson.getLessonNumber(),
                    lesson.getSubGroup(),
                    EStatus.ACTIVE);
        lessonType = lessons.stream().sorted(Comparator.comparing(LessonModel::getLessonType)).map(l -> convertLessonType(l.getLessonType())).distinct().collect(Collectors.joining(" / ", "", " "));
        teachers = lessons.stream().map(lessonService::toDto).map(l -> getTeacherFio(l.getTeacher())).distinct().collect(Collectors.joining(", ", "", ""));

        if (lesson.getWeekType().equals(EWeekType.ALWAYS) || lesson.getWeekType().equals(EWeekType.NUMERATOR) || lesson.getWeekType().equals(EWeekType.DENOMINATOR)) {
            return lessonType + lesson.getDiscipline().getName() + ", " + teachers;
        } else return lessonType +
                lesson.getDiscipline().getName() + ", " +
                teachers + " (" +
                lessons.stream().map(l -> convertWeekType(l.getWeekType())).distinct().collect(Collectors.joining(", ", "", "")) +
                ")";

    }

    private String fillClassroomCell(LessonDTO lesson) {
        if (lesson.getClassroom().getFrame().getId() == -1)
            return lesson.getClassroom().getRoomNumber();
        else return lesson.getClassroom().getFrame().getId() + "-" + lesson.getClassroom().getRoomNumber();
    }

    private String fillCitCell(LessonDTO lesson) {
        List<LessonModel> lessons = this.lessonRepo.findByRoomIdAndDayAndLessonNumberAndStatus(lesson.getClassroom().getId(), lesson.getDay(), lesson.getLessonNumber(), EStatus.ACTIVE);
        return lessons.stream().map(lessonService::toDto).map(l -> getTeacherFio(l.getTeacher()) + " " + convertWeekType(l.getWeekType())).distinct().collect(Collectors.joining(" / ", "", ""));
    }

    private String convertLessonType(ELessonType lessonType) {
        return switch (lessonType) {
            case LECTURE -> "лк.";
            case PRACTICE -> "пр.";
            case LAB -> "лб.";
            case EXAM -> "Экзамен";
            case CONSULTATION -> "Консультация";
            case SCORE -> "Зачёт";
            default -> "unknown";
        };
    }

    private String convertWeekType(EWeekType weekType) {
        return switch (weekType) {
            case NUMERATOR -> "числ.";
            case DENOMINATOR -> "знам.";
            case FIRST -> "1н";
            case SECOND -> "2н";
            case THIRD -> "3н";
            case FOURTH -> "4н";
            default -> "";
        };
    }

    private String getTeacherFio(TeacherModel teacher) {
        return teacher.getSurname() + " " + teacher.getName().split("")[0] + "." + teacher.getPatronymic().split("")[0] + ".";
    }
}
