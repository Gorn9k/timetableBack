package by.vstu.dean.timetable.services.common;

import by.vstu.dean.core.enums.EStatus;
import by.vstu.dean.support.models.ClassroomModel;
import by.vstu.dean.support.models.FacultyModel;
import by.vstu.dean.support.models.GroupModel;
import by.vstu.dean.support.models.TeacherModel;
import by.vstu.dean.timetable.dto.LessonDTO;
import by.vstu.dean.timetable.enums.ELessonTypes;
import by.vstu.dean.timetable.enums.ESubGroup;
import by.vstu.dean.timetable.enums.EWeekType;
import by.vstu.dean.timetable.models.LessonModel;
import by.vstu.dean.timetable.repo.LessonModelRepository;
import by.vstu.dean.timetable.repo.rest.TeacherRepo;
import by.vstu.dean.timetable.services.LessonService;
import by.vstu.dean.timetable.dto.mapper.LessonMapper;
import by.vstu.dean.timetable.repo.rest.ClassroomRepo;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.jetbrains.annotations.NotNull;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTPageMargins;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTWorksheet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Сервис, отвечающий за работу с экселем.
 */
@Service
public class ExcelService {
    private static final Logger log = LoggerFactory.getLogger(ExcelService.class);

    private final LessonService lessonService;
    private final LessonMapper lessonMapper;
    private final LessonModelRepository lessonRepo;

    private final TeacherRepo teacherRepo;
    private final ClassroomRepo classroomRepo;

    private final ResourceLoader resourceLoader;

    public ExcelService(LessonMapper lessonMapper, LessonService lessonService, LessonModelRepository lessonRepo, TeacherRepo teacherRepo, ClassroomRepo classroomRepo, ResourceLoader resourceLoader) {
        this.lessonService = lessonService;
        this.lessonRepo = lessonRepo;
        this.teacherRepo = teacherRepo;
        this.classroomRepo = classroomRepo;
        this.resourceLoader = resourceLoader;
        this.lessonMapper = lessonMapper;
    }

    /**
     * Эксель расписания для групп дневного отделения определённого факультета, определённого курса.<br/>
     * P.S. Какая задача, такой и код =))
     *
     * @param facultyId Идентификатор факультета
     * @param course    Курс групп
     * @param from      Дата начала занятий, которые будут включены в эксель
     * @param to        Дата конца занятий, которые будут включены в эксель
     * @return Сформированный эксель
     * @see #convertSubGroup
     * @see #sortedKeysMap
     * @see #fillSheet
     **/
    public Workbook excelDaytime(long facultyId, int course, String from, String to) {

        LocalDate dateFrom = LocalDate.parse(from);
        LocalDate dateTo = LocalDate.parse(to);

        List<LessonDTO> lessons = this.lessonMapper.toDto(lessonRepo.findByStatusAndBetweenDates(EStatus.ACTIVE, dateFrom, dateTo)).stream()
                .filter(l -> l.getGroup().getFaculty().getId().equals(facultyId) &&
                        (dateTo.minusYears(l.getGroup().getYearStart()).minusMonths(9).minusDays(1).getYear() == (course - 1))
                )
                .sorted(Comparator.comparing(LessonDTO::getGroup, Comparator.comparing(GroupModel::getName)).thenComparing(LessonDTO::getSubGroup, Comparator.reverseOrder()))
                .toList();

        Map<String, List<LessonDTO>> tempGroupLessons = new HashMap<>();

        lessons.forEach(lessonDTO -> {
            String groupName = lessonDTO.getGroup().getName();
            String subGroupName = convertSubGroup(lessonDTO.getSubGroup());

            if (lessons.stream()
                    .filter(l -> l.getGroup().getId().equals(lessonDTO.getGroup().getId()))
                    .anyMatch(l -> Arrays.asList(ESubGroup.SEWING, ESubGroup.SHOE, ESubGroup.TEXTILE, ESubGroup.GRAPHIC, ESubGroup.MULTIMEDIA).contains(l.getSubGroup()))
            ) {
                if (Arrays.asList(ESubGroup.SEWING, ESubGroup.SHOE, ESubGroup.TEXTILE, ESubGroup.GRAPHIC, ESubGroup.MULTIMEDIA).contains(lessonDTO.getSubGroup())) {
                    String key = groupName + subGroupName;
                    tempGroupLessons.computeIfAbsent(key, k -> new ArrayList<>()).add(lessonDTO);
                }
                if (lessonDTO.getSubGroup().equals(ESubGroup.ALL)) {
                    tempGroupLessons.computeIfPresent(groupName + convertSubGroup(ESubGroup.SEWING), (k, v) -> {
                        v.add(lessonDTO);
                        return v;
                    });
                    tempGroupLessons.computeIfPresent(groupName + convertSubGroup(ESubGroup.SHOE), (k, v) -> {
                        v.add(lessonDTO);
                        return v;
                    });
                    tempGroupLessons.computeIfPresent(groupName + convertSubGroup(ESubGroup.TEXTILE), (k, v) -> {
                        v.add(lessonDTO);
                        return v;
                    });
                    tempGroupLessons.computeIfPresent(groupName + convertSubGroup(ESubGroup.GRAPHIC), (k, v) -> {
                        v.add(lessonDTO);
                        return v;
                    });
                    tempGroupLessons.computeIfPresent(groupName + convertSubGroup(ESubGroup.MULTIMEDIA), (k, v) -> {
                        v.add(lessonDTO);
                        return v;
                    });
                }
            } else {
                tempGroupLessons.computeIfAbsent(groupName, k -> new ArrayList<>()).add(lessonDTO);
            }

        });

        Map<String, List<LessonDTO>> groupLessons = sortedKeysMap();
        groupLessons.putAll(tempGroupLessons);
        Set<String> groups = groupLessons.keySet();

        log.info("Сформирован excel дневного отделения: {}", groups);

        Workbook workbook = null;
        int halfYear = dateFrom.getMonthValue() < 7 ? 1 : 2;

        if ((groups.stream().filter(s -> !s.contains("Эк-")).toList().size() > 4 && groups.stream().anyMatch(s -> s.contains("Эк-"))) || groups.size() > 8) {
            try (InputStream fileInputStream = getTemplate("rasSize12.xlsx")) {
                workbook = new XSSFWorkbook(fileInputStream);
            } catch (FileNotFoundException fileNotFoundException) {
                log.error("File ras12.xlsx not found!");
            } catch (IOException ioException) {
                log.error("Incorrect import excel file \"rasSize12.xlsx\"!");
            }

            if (workbook == null || lessons.isEmpty() || groups.isEmpty()) {
                return workbook;
            }

            FacultyModel faculty = lessons.get(0).getGroup().getFaculty();
            Map<String, List<LessonDTO>> firstPart = sortedKeysMap();
            Map<String, List<LessonDTO>> secondPart = sortedKeysMap();
            Map<String, List<LessonDTO>> thirdPart = sortedKeysMap();

            if (groups.stream().anyMatch(s -> s.contains("Эк-"))) {
                firstPart.putAll(groupLessons.entrySet().stream()
                        .filter(stringListEntry -> !stringListEntry.getKey().contains("Эк-"))
                        .limit(4)
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
                secondPart.putAll(groupLessons.entrySet().stream()
                        .filter(stringListEntry -> !stringListEntry.getKey().contains("Эк-"))
                        .skip(4)
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
                thirdPart.putAll(groupLessons.entrySet().stream()
                        .filter(stringListEntry -> stringListEntry.getKey().contains("Эк-"))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
            } else {
                firstPart.putAll(groupLessons.entrySet().stream()
                        .limit(4)
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
                secondPart.putAll(groupLessons.entrySet().stream()
                        .skip(4)
                        .limit(4)
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
                thirdPart.putAll(groupLessons.entrySet().stream()
                        .skip(8)
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
            }

            workbook.setSheetName(0, firstPart.keySet().stream().map(g -> course + g).collect(Collectors.joining(", ")));
            workbook.setSheetName(1, secondPart.keySet().stream().map(g -> course + g).collect(Collectors.joining(", ")));
            workbook.setSheetName(2, thirdPart.keySet().stream().map(g -> course + g).collect(Collectors.joining(", ")));
            Sheet sheet1 = workbook.getSheetAt(0);
            Sheet sheet2 = workbook.getSheetAt(1);
            Sheet sheet3 = workbook.getSheetAt(2);

            sheet1.getRow(1).getCell(8).setCellValue((facultyId == 2 || facultyId == 5 ? "" : "факультет ") + faculty.getName().toLowerCase());
            sheet1.getRow(2).getCell(8).setCellValue((halfYear == 1 ? "весенний" : "осенний") + " семестр, " +
                    (halfYear == 1 ? dateFrom.minusYears(1).getYear() + "/" + dateFrom.getYear() : dateFrom.getYear() + "/" + dateFrom.plusYears(1).getYear())
                    + " учебный год, " + course + " курс");
            sheet1.getRow(78).getCell(0).setCellValue("Декан " + (facultyId == 1 || facultyId == 2 || facultyId == 5 ? "" : "факультета ") + faculty.getNameGenitive().toLowerCase());
            sheet1.getRow(78).getCell(10).setCellValue(faculty.getDean());

            sheet2.getRow(1).getCell(8).setCellValue((facultyId == 2 || facultyId == 5 ? "" : "факультет ") + faculty.getName().toLowerCase());
            sheet2.getRow(2).getCell(8).setCellValue((halfYear == 1 ? "весенний" : "осенний") + " семестр, " +
                    (halfYear == 1 ? dateFrom.minusYears(1).getYear() + "/" + dateFrom.getYear() : dateFrom.getYear() + "/" + dateFrom.plusYears(1).getYear())
                    + " учебный год, " + course + " курс");
            sheet2.getRow(78).getCell(0).setCellValue("Декан " + (facultyId == 1 || facultyId == 2 || facultyId == 5 ? "" : "факультета ") + faculty.getNameGenitive().toLowerCase());
            sheet2.getRow(78).getCell(10).setCellValue(faculty.getDean());

            sheet3.getRow(1).getCell(8).setCellValue((facultyId == 2 || facultyId == 5 ? "" : "факультет ") + faculty.getName().toLowerCase());
            sheet3.getRow(2).getCell(8).setCellValue((halfYear == 1 ? "весенний" : "осенний") + " семестр, " +
                    (halfYear == 1 ? dateFrom.minusYears(1).getYear() + "/" + dateFrom.getYear() : dateFrom.getYear() + "/" + dateFrom.plusYears(1).getYear())
                    + " учебный год, " + course + " курс");
            sheet3.getRow(78).getCell(0).setCellValue("Декан " + (facultyId == 1 || facultyId == 2 || facultyId == 5 ? "" : "факультета ") + faculty.getNameGenitive().toLowerCase());
            sheet3.getRow(78).getCell(10).setCellValue(faculty.getDean());

            fillSheet(sheet1, firstPart, dateFrom, dateTo);
            fillSheet(sheet2, secondPart, dateFrom, dateTo);
            fillSheet(sheet3, thirdPart, dateFrom, dateTo);

        } else if ((groups.stream().filter(s -> !s.contains("Эк-")).toList().size() <= 4 && groups.contains("Эк-")) ||
                (groups.size() > 4 && groups.stream().noneMatch(s -> s.contains("Эк-")))) {

            try (InputStream fileInputStream = getTemplate("rasSize8.xlsx")) {
                workbook = new XSSFWorkbook(fileInputStream);
            } catch (FileNotFoundException fileNotFoundException) {
                log.error("File ras8.xlsx not found!");
            } catch (IOException ioException) {
                log.error("Incorrect import excel file \"rasSize8.xlsx\"!");
            }

            if (workbook == null || lessons.isEmpty() || groups.isEmpty()) {
                return workbook;
            }

            FacultyModel faculty = lessons.get(0).getGroup().getFaculty();
            Map<String, List<LessonDTO>> firstPart = sortedKeysMap();
            Map<String, List<LessonDTO>> secondPart = sortedKeysMap();

            if (groups.stream().anyMatch(s -> s.contains("Эк-"))) {
                firstPart.putAll(groupLessons.entrySet().stream()
                        .filter(stringListEntry -> !stringListEntry.getKey().contains("Эк-"))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
                secondPart.putAll(groupLessons.entrySet().stream()
                        .filter(stringListEntry -> stringListEntry.getKey().contains("Эк-"))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
            } else if (groups.size() == 5) {
                firstPart.putAll(groupLessons.entrySet().stream()
                        .limit(3)
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
                secondPart.putAll(groupLessons.entrySet().stream()
                        .skip(3)
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
            } else {
                firstPart.putAll(groupLessons.entrySet().stream()
                        .limit(4)
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
                secondPart.putAll(groupLessons.entrySet().stream()
                        .skip(4)
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
            }

            workbook.setSheetName(0, firstPart.keySet().stream().map(g -> course + g).collect(Collectors.joining(", ")));
            workbook.setSheetName(1, secondPart.keySet().stream().map(g -> course + g).collect(Collectors.joining(", ")));
            Sheet sheet1 = workbook.getSheetAt(0);
            Sheet sheet2 = workbook.getSheetAt(1);

            sheet1.getRow(1).getCell(8).setCellValue((facultyId == 2 || facultyId == 5 ? "" : "факультет ") + faculty.getName().toLowerCase());
            sheet1.getRow(2).getCell(8).setCellValue((halfYear == 1 ? "весенний" : "осенний") + " семестр, " +
                    (halfYear == 1 ? dateFrom.minusYears(1).getYear() + "/" + dateFrom.getYear() : dateFrom.getYear() + "/" + dateFrom.plusYears(1).getYear())
                    + " учебный год, " + course + " курс");
            sheet1.getRow(78).getCell(0).setCellValue("Декан " + (facultyId == 1 || facultyId == 2 || facultyId == 5 ? "" : "факультета ") + faculty.getNameGenitive().toLowerCase());
            sheet1.getRow(78).getCell(10).setCellValue(faculty.getDean());

            sheet2.getRow(1).getCell(8).setCellValue((facultyId == 2 || facultyId == 5 ? "" : "факультет ") + faculty.getName().toLowerCase());
            sheet2.getRow(2).getCell(8).setCellValue((halfYear == 1 ? "весенний" : "осенний") + " семестр, " +
                    (halfYear == 1 ? dateFrom.minusYears(1).getYear() + "/" + dateFrom.getYear() : dateFrom.getYear() + "/" + dateFrom.plusYears(1).getYear())
                    + " учебный год, " + course + " курс");
            sheet2.getRow(78).getCell(0).setCellValue("Декан " + (facultyId == 1 || facultyId == 2 || facultyId == 5 ? "" : "факультета ") + faculty.getNameGenitive().toLowerCase());
            sheet2.getRow(78).getCell(10).setCellValue(faculty.getDean());

            fillSheet(sheet1, firstPart, dateFrom, dateTo);
            fillSheet(sheet2, secondPart, dateFrom, dateTo);
        } else {
            try (InputStream fileInputStream = getTemplate("rasSize4.xlsx")) {
                workbook = new XSSFWorkbook(fileInputStream);
            } catch (FileNotFoundException fileNotFoundException) {
                log.error("File ras4.xlsx not found!");
            } catch (IOException ioException) {
                log.error("Incorrect import excel file \"rasSize4.xlsx\"!");
            }

            if (workbook == null || lessons.isEmpty() || groups.isEmpty()) {
                return workbook;
            }

            FacultyModel faculty = lessons.get(0).getGroup().getFaculty();
            workbook.setSheetName(0, groups.stream().map(g -> course + g.split("\\(")[0]).distinct().collect(Collectors.joining(", ")));
            Sheet sheet = workbook.getSheetAt(0);

            sheet.getRow(1).getCell(8).setCellValue((facultyId == 2 || facultyId == 5 ? "" : "факультет ") + faculty.getName().toLowerCase());
            sheet.getRow(2).getCell(8).setCellValue((halfYear == 1 ? "весенний" : "осенний") + " семестр, " +
                    (halfYear == 1 ? dateFrom.minusYears(1).getYear() + "/" + dateFrom.getYear() : dateFrom.getYear() + "/" + dateFrom.plusYears(1).getYear())
                    + " учебный год, " + course + " курс");
            sheet.getRow(78).getCell(0).setCellValue("Декан " + (facultyId == 1 || facultyId == 2 || facultyId == 5 ? "" : "факультета ") + faculty.getNameGenitive().toLowerCase());
            sheet.getRow(78).getCell(10).setCellValue(faculty.getDean());

            fillSheet(sheet, groupLessons, dateFrom, dateTo);
        }

        return workbook;
    }

    /**
     * Эксель расписания сессии для групп дневного отделения.
     *
     * @param groupIds Идентификаторы групп, для которых формируется эксель
     * @param from     Дата начала сессии
     * @param to       Дата конца сессии
     * @return Сформированный эксель
     * @see #convertSubGroup
     * @see #sortedKeysMap
     * @see #dateToString
     * @see #convertDayOfWeek
     * @see #convertLessonNumber
     * @see #classroomToString(LessonDTO)
     * @see #convertLessonType
     * @see #sessionCellValue
     */
    public Workbook excelDaytimeSession(long[] groupIds, String from, String to) {
        LocalDate dateFrom = LocalDate.parse(from);
        LocalDate dateTo = LocalDate.parse(to);

        Workbook workbook = null;

        try (InputStream inputStream = getTemplate("session.xlsx")) {
            workbook = new XSSFWorkbook(inputStream);
        } catch (FileNotFoundException fileNotFoundException) {
            log.error("File session.xlsx not found!");
        } catch (IOException ioException) {
            log.error("Incorrect import excel file \"session.xlsx\"!");
        }

        List<LessonDTO> lessons = lessonMapper.toDto(lessonRepo.findByGroupIdInAndStatusAndBetweenDates(groupIds, EStatus.ACTIVE, dateFrom, dateTo));
        Map<String, List<LessonDTO>> tempGroupLessons = new HashMap<>();

        if (workbook == null || groupIds == null || groupIds.length == 0 || lessons == null || lessons.isEmpty()) {
            return null;
        }

        lessons.forEach(lessonDTO -> {
            String groupName = lessonDTO.getGroup().getName();
            String subGroupName = convertSubGroup(lessonDTO.getSubGroup());

            if (lessons.stream()
                    .filter(l -> l.getGroup().getId().equals(lessonDTO.getGroup().getId()))
                    .anyMatch(l -> Arrays.asList(ESubGroup.SEWING, ESubGroup.SHOE, ESubGroup.TEXTILE, ESubGroup.GRAPHIC, ESubGroup.MULTIMEDIA, ESubGroup.WEAVERS, ESubGroup.KNITWEAR).contains(l.getSubGroup()))
            ) {
                if (Arrays.asList(ESubGroup.SEWING, ESubGroup.SHOE, ESubGroup.TEXTILE, ESubGroup.GRAPHIC, ESubGroup.MULTIMEDIA, ESubGroup.WEAVERS, ESubGroup.KNITWEAR).contains(lessonDTO.getSubGroup())) {
                    String key = groupName + subGroupName;
                    tempGroupLessons.computeIfAbsent(key, k -> new ArrayList<>()).add(lessonDTO);
                }
                if (lessonDTO.getSubGroup().equals(ESubGroup.ALL)) {
                    tempGroupLessons.computeIfPresent(groupName + convertSubGroup(ESubGroup.SEWING), (k, v) -> {
                        v.add(lessonDTO);
                        return v;
                    });
                    tempGroupLessons.computeIfPresent(groupName + convertSubGroup(ESubGroup.SHOE), (k, v) -> {
                        v.add(lessonDTO);
                        return v;
                    });
                    tempGroupLessons.computeIfPresent(groupName + convertSubGroup(ESubGroup.TEXTILE), (k, v) -> {
                        v.add(lessonDTO);
                        return v;
                    });
                    tempGroupLessons.computeIfPresent(groupName + convertSubGroup(ESubGroup.GRAPHIC), (k, v) -> {
                        v.add(lessonDTO);
                        return v;
                    });
                    tempGroupLessons.computeIfPresent(groupName + convertSubGroup(ESubGroup.MULTIMEDIA), (k, v) -> {
                        v.add(lessonDTO);
                        return v;
                    });
                    tempGroupLessons.computeIfPresent(groupName + convertSubGroup(ESubGroup.WEAVERS), (k, v) -> {
                        v.add(lessonDTO);
                        return v;
                    });
                    tempGroupLessons.computeIfPresent(groupName + convertSubGroup(ESubGroup.KNITWEAR), (k, v) -> {
                        v.add(lessonDTO);
                        return v;
                    });
                }
            } else {
                tempGroupLessons.computeIfAbsent(groupName, k -> new ArrayList<>()).add(lessonDTO);
            }

        });

        Map<String, List<LessonDTO>> groupLessons = sortedKeysMap();
        groupLessons.putAll(tempGroupLessons);

        Font font6 = workbook.createFont(), font8 = workbook.createFont(), font11 = workbook.createFont(), font12 = workbook.createFont();
        font6.setFontName("Arial Cyr");
        font6.setFontHeightInPoints((short) 6);

        font8.setFontName("Arial Cyr");
        font8.setFontHeightInPoints((short) 8);

        font11.setFontName("Arial Cyr");
        font11.setFontHeightInPoints((short) 11);

        font12.setFontName("Arial Cyr");
        font12.setFontHeightInPoints((short) 12);

        CellStyle topStyle = workbook.createCellStyle(), tableStyle = workbook.createCellStyle(), tableStyleRotated = workbook.createCellStyle(), groupNameStyle = workbook.createCellStyle(), cellStyle = workbook.createCellStyle();
        topStyle.setFont(font11);
        topStyle.setVerticalAlignment(VerticalAlignment.BOTTOM);

        tableStyle.setFont(font8);
        tableStyle.setAlignment(HorizontalAlignment.CENTER);
        tableStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        tableStyle.setBorderTop(BorderStyle.THIN);
        tableStyle.setBorderLeft(BorderStyle.THIN);
        tableStyle.setBorderRight(BorderStyle.THIN);
        tableStyle.setBorderBottom(BorderStyle.THIN);

        tableStyleRotated.cloneStyleFrom(tableStyle);
        tableStyleRotated.setRotation((short) 90);

        groupNameStyle.cloneStyleFrom(tableStyle);
        groupNameStyle.setFont(font12);

        cellStyle.cloneStyleFrom(tableStyle);
        cellStyle.setFont(font6);
        cellStyle.setWrapText(true);

        workbook.setSheetName(0, String.join(", ", groupLessons.keySet()));
        Sheet sheet = workbook.getSheetAt(0);

        Row row = sheet.getRow(2);
        row.createCell(6).setCellValue(dateTo.getYear() - 1 + "/" + dateTo.getYear() + " учебного года");
        row.getCell(6).setCellStyle(topStyle);

        FacultyModel faculty = groupLessons.entrySet().stream()
                .limit(1)
                .map(stringListEntry -> stringListEntry.getValue().get(0).getGroup().getFaculty())
                .findFirst().get();
        String facultyString = (Arrays.asList(3L, 4L, 6L, 7L).contains(faculty.getId()) ?
                "факультета " + faculty.getNameGenitive().split(" ")[0].toLowerCase()
                :
                Arrays.stream(faculty.getNameGenitive().toLowerCase().split(" ")).limit(2).collect(Collectors.joining(" ")));
        row = sheet.getRow(3);
        row.createCell(6).setCellValue(facultyString);
        row.getCell(6).setCellStyle(topStyle);
        if (faculty.getId() == 3 || faculty.getId() == 4) {
            row = sheet.getRow(4);
            row.createCell(6).setCellValue(Arrays.stream(faculty.getNameGenitive().split(" ")).skip(1).collect(Collectors.joining(" ")));
            row.getCell(6).setCellStyle(topStyle);
        }
        if (faculty.getId() == 5) {
            row = sheet.getRow(4);
            row.createCell(6).setCellValue(Arrays.stream(faculty.getNameGenitive().split(" ")).skip(2).collect(Collectors.joining(" ")));
            row.getCell(6).setCellStyle(topStyle);
        }

        row = sheet.getRow(6);
        int cellIndex = 2;
        for (String group : groupLessons.keySet()) {
            row.createCell(cellIndex - 1).setCellStyle(tableStyleRotated);
            row.getCell(cellIndex - 1).setCellValue("Ауд.");
            row.createCell(cellIndex).setCellStyle(groupNameStyle);
            row.getCell(cellIndex).setCellValue(group);
            row.createCell(cellIndex + 1).setCellStyle(groupNameStyle);
            sheet.addMergedRegion(new CellRangeAddress(6, 6, cellIndex, cellIndex + 1));
            cellIndex += 3;
        }

        LocalDate currentDate = dateFrom;
        int rowIndex = 7;
        while (currentDate.isBefore(dateTo.plusDays(1))) {
            row = sheet.createRow(rowIndex);
            row.createCell(0).setCellValue(dateToString(currentDate) + " " + convertDayOfWeek(currentDate.getDayOfWeek().getValue()));
            sheet.createRow(rowIndex + 1).createCell(0).setCellStyle(tableStyle);
            row.getCell(0).setCellStyle(tableStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex + 1, 0, 0));

            cellIndex = 2;
            for (String group : groupLessons.keySet()) {
                row.createCell(cellIndex - 1).setCellStyle(cellStyle);
                sheet.getRow(rowIndex + 1).createCell(cellIndex - 1).setCellStyle(cellStyle);

                row.createCell(cellIndex).setCellStyle(cellStyle);
                row.createCell(cellIndex + 1).setCellStyle(cellStyle);
                sheet.getRow(rowIndex + 1).createCell(cellIndex).setCellStyle(cellStyle);
                sheet.getRow(rowIndex + 1).createCell(cellIndex + 1).setCellStyle(cellStyle);

                LocalDate tempDate = currentDate;
                List<LessonDTO> lessonDTOS = groupLessons.get(group).stream().filter(l -> l.getStartDate().equals(tempDate) && l.getEndDate().equals(tempDate)).toList();
                Map<String, List<LessonDTO>> timeLessons = lessonDTOS.stream().sorted(Comparator.comparing(LessonDTO::getLessonNumber)).collect(Collectors.groupingBy(lessonDTO -> convertLessonNumber(lessonDTO.getLessonNumber())));
                if (lessonDTOS.isEmpty()) {
                    sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex + 1, cellIndex - 1, cellIndex - 1));
                    sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex + 1, cellIndex, cellIndex + 1));
                } else if (timeLessons.keySet().size() == 1) {
                    String classrooms = timeLessons.entrySet().iterator().next().getValue().stream().map(this::classroomToString).distinct().collect(Collectors.joining(", "));
                    row.getCell(cellIndex - 1).setCellValue(timeLessons.keySet().iterator().next() + " ауд." + classrooms);

                    sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex + 1, cellIndex - 1, cellIndex - 1));

                    List<LessonDTO> values = timeLessons.entrySet().iterator().next().getValue();
                    row.getCell(cellIndex).setCellValue(
                            values.stream().allMatch(l -> l.getLessonType().equals(ELessonTypes.CONSULTATION)) ?
                                    "Консультация"
                                    :
                                    (convertLessonType(values.get(0).getLessonType()) + " " + sessionCellValue(values))
                    );

                    sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex + 1, cellIndex, cellIndex + 1));

                } else {
                    Map<String, String> cellValues = timeLessons.entrySet().stream().collect(
                            Collectors.toMap(
                                    entry -> entry.getValue().stream().map(this::classroomToString).distinct().collect(Collectors.joining(", ")),
                                    entry -> sessionCellValue(entry.getValue())
                            )
                    );
                    Iterator<Map.Entry<String, List<LessonDTO>>> timeLessonsIterator = timeLessons.entrySet().iterator();
                    Iterator<Map.Entry<String, String>> cellValuesIterator = cellValues.entrySet().iterator();
                    Map.Entry<String, List<LessonDTO>> currentTimeLessons = timeLessonsIterator.next();
                    Map.Entry<String, String> currentValues = cellValuesIterator.next();
                    row.getCell(cellIndex - 1).setCellValue(currentTimeLessons.getKey() + " ауд." + currentValues.getKey());
                    row.getCell(cellIndex).setCellValue(
                            currentTimeLessons.getValue().stream().allMatch(l -> l.getLessonType().equals(ELessonTypes.CONSULTATION)) ?
                                    "Консультация"
                                    :
                                    (convertLessonType(currentTimeLessons.getValue().get(0).getLessonType()) + " " + sessionCellValue(currentTimeLessons.getValue()))
                    );
                    sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, cellIndex, cellIndex + 1));

                    currentTimeLessons = timeLessonsIterator.next();
                    currentValues = cellValuesIterator.next();
                    sheet.getRow(rowIndex + 1).getCell(cellIndex - 1).setCellValue(currentTimeLessons.getKey() + " ауд." + currentValues.getKey());
                    sheet.getRow(rowIndex + 1).getCell(cellIndex).setCellValue(
                            currentTimeLessons.getValue().stream().allMatch(l -> l.getLessonType().equals(ELessonTypes.CONSULTATION)) ?
                                    "Консультация"
                                    :
                                    (convertLessonType(currentTimeLessons.getValue().get(0).getLessonType()) + " " + sessionCellValue(currentTimeLessons.getValue()))
                    );
                    sheet.addMergedRegion(new CellRangeAddress(rowIndex + 1, rowIndex + 1, cellIndex, cellIndex + 1));
                }

                cellIndex += 3;
            }

            currentDate = currentDate.plusDays(1);
            rowIndex += 2;
        }


        return workbook;
    }

    /**
     * Эксель для расписания аудиторий ЦИТ первого корпуса.<br/>
     * Стал ненужен в связи с созданием сервиса "Расписание занятости в аудиториях(ЦИТ)"
     *
     * @param from Дата, с которой учитываются занятия
     * @param to   Дата, по которую учитываются занятия
     * @return Эксель с занятостью аудиторий ЦИТ 1-ого корпуса(122, 212, 221, 417)
     * @see #citCellValue
     * @since 01.09.2024
     * @deprecated Используйте <a href="https://schedulecit.vstu.by/">Расписание занятости в аудиториях(ЦИТ)</a>
     */
    @Deprecated
    public Workbook excelForCit(String from, String to) {
        Workbook workbook = null;
        try (InputStream fileInputStream = getTemplate("cit.xlsx")) {
            workbook = new XSSFWorkbook(fileInputStream);
        } catch (FileNotFoundException fileNotFoundException) {
            log.error("File cit.xlsx not found!");
        } catch (IOException ioException) {
            log.error("Incorrect import excel file \"cit.xlsx\"!");
        }

        LocalDate dateFrom = LocalDate.parse(from);
        LocalDate dateTo = LocalDate.parse(to);

        List<LessonDTO> lessons = lessonMapper.toDto(lessonRepo.findByStatusAndDate(EStatus.ACTIVE, dateFrom, dateTo))
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

                            if ((lesson.getStartDate().getMonthValue() == 9 && lesson.getStartDate().getDayOfMonth() == 1 && lesson.getEndDate().getMonthValue() == 5 && lesson.getEndDate().getDayOfMonth() == 31) ||
                                    lesson.getStartDate().getMonthValue() == 2 && lesson.getStartDate().getDayOfMonth() == 1 && lesson.getEndDate().getMonthValue() == 12 && lesson.getEndDate().getDayOfMonth() == 31)
                                sheet.getRow(rowIndex).getCell(cellIndex).setCellValue(citCellValue(lesson, dateFrom, dateTo));
                            else
                                sheet.getRow(rowIndex).getCell(cellIndex).setCellValue(citCellValue(lesson, dateFrom, dateTo) + "\n" + lesson.getStartDate().toString() + " - " + lesson.getEndDate().toString());
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

    /**
     * Эксель для заочных групп.
     *
     * @param groupIds    Массив идентификаторов групп
     * @param from        Дата начала занятий, которые будут включены в эксель
     * @param to          Дата конца занятий, которые будут включены в эксель
     * @param sessionType Тип сессии<b><i>("установочная", "онлайн", null)</i></b>
     * @return Сформированный эксель файл
     * @see #convertSubGroup
     * @see #createWeeksMap
     * @see #dateToString
     * @see #fillZaochSheet
     */
    public Workbook excelZaoch(long[] groupIds, String from, String to, String sessionType) {
        Workbook workbook = null;
        if (groupIds == null || groupIds.length == 0) {
            return null;
        }

        int columnWidth = switch (groupIds.length) {
            case 1 -> 76;
            case 2 -> 41;
            default -> 26;
        };

        LocalDate dateFrom = LocalDate.parse(from);
        LocalDate dateTo = LocalDate.parse(to);

        int halfYear = dateFrom.getMonthValue() < 7 ? 1 : 2; // 1 - весна, 2 - осень

        List<LessonDTO> lessons = lessonMapper.toDto(lessonRepo.findByGroupIdInAndStatusAndBetweenDates(groupIds, EStatus.ACTIVE, dateFrom, dateTo));

        Map<String, List<LessonDTO>> tempGroupLessons = new HashMap<>();

        lessons.forEach(lessonDTO -> {
            String groupName = lessonDTO.getGroup().getName();
            String subGroupName = convertSubGroup(lessonDTO.getSubGroup());

            if (lessons.stream()
                    .filter(l -> l.getGroup().getId().equals(lessonDTO.getGroup().getId()))
                    .anyMatch(l -> Arrays.asList(ESubGroup.SEWING, ESubGroup.SHOE, ESubGroup.TEXTILE, ESubGroup.GRAPHIC, ESubGroup.MULTIMEDIA, ESubGroup.WEAVERS, ESubGroup.KNITWEAR).contains(l.getSubGroup()))
            ) {
                if (Arrays.asList(ESubGroup.SEWING, ESubGroup.SHOE, ESubGroup.TEXTILE, ESubGroup.GRAPHIC, ESubGroup.MULTIMEDIA, ESubGroup.WEAVERS, ESubGroup.KNITWEAR).contains(lessonDTO.getSubGroup())) {
                    String key = groupName + subGroupName;
                    tempGroupLessons.computeIfAbsent(key, k -> new ArrayList<>()).add(lessonDTO);
                }
                if (lessonDTO.getSubGroup().equals(ESubGroup.ALL)) {
                    tempGroupLessons.computeIfPresent(groupName + convertSubGroup(ESubGroup.SEWING), (k, v) -> {
                        v.add(lessonDTO);
                        return v;
                    });
                    tempGroupLessons.computeIfPresent(groupName + convertSubGroup(ESubGroup.SHOE), (k, v) -> {
                        v.add(lessonDTO);
                        return v;
                    });
                    tempGroupLessons.computeIfPresent(groupName + convertSubGroup(ESubGroup.TEXTILE), (k, v) -> {
                        v.add(lessonDTO);
                        return v;
                    });
                    tempGroupLessons.computeIfPresent(groupName + convertSubGroup(ESubGroup.GRAPHIC), (k, v) -> {
                        v.add(lessonDTO);
                        return v;
                    });
                    tempGroupLessons.computeIfPresent(groupName + convertSubGroup(ESubGroup.MULTIMEDIA), (k, v) -> {
                        v.add(lessonDTO);
                        return v;
                    });
                    tempGroupLessons.computeIfPresent(groupName + convertSubGroup(ESubGroup.WEAVERS), (k, v) -> {
                        v.add(lessonDTO);
                        return v;
                    });
                    tempGroupLessons.computeIfPresent(groupName + convertSubGroup(ESubGroup.KNITWEAR), (k, v) -> {
                        v.add(lessonDTO);
                        return v;
                    });
                }
            } else {
                tempGroupLessons.computeIfAbsent(groupName, k -> new ArrayList<>()).add(lessonDTO);
            }

        });

        Comparator<String> comparator = (s1, s2) -> {
            boolean hasBrackets1 = s1.contains("(");
            boolean hasBrackets2 = s2.contains("(");

            if (hasBrackets1 && hasBrackets2) {
                return s2.compareTo(s1);
            } else if (hasBrackets1) {
                return 1;
            } else if (hasBrackets2) {
                return -1;
            } else {
                return s1.compareTo(s2);
            }
        };
        Map<String, List<LessonDTO>> groupLessons = new TreeMap<>(comparator);
        groupLessons.putAll(tempGroupLessons);

        try (InputStream fileInputStream = getTemplate("zaochSize" + Math.min(groupLessons.keySet().size(), 4) + ".xlsx")) {
            workbook = new XSSFWorkbook(fileInputStream);
        } catch (FileNotFoundException fileNotFoundException) {
            log.error("File zaoch.xlsx not found!");
        } catch (IOException ioException) {
            log.error("Incorrect import excel file \"zaoch.xlsx\"!");
        }

        if (workbook == null)
            return null;

        log.info("Сформирован excel для заочников: {}", groupLessons.keySet());

        Sheet sheet = workbook.getSheetAt(0);

        Font font14 = workbook.createFont();
        font14.setFontName("Arial Cyr");
        font14.setFontHeightInPoints((short) 14);

        Font font14Bold = workbook.createFont();
        font14Bold.setFontName("Arial Cyr");
        font14Bold.setFontHeightInPoints((short) 14);
        font14Bold.setBold(true);

        CellStyle style14 = workbook.createCellStyle();
        style14.setFont(font14);
        style14.setAlignment(HorizontalAlignment.CENTER);
        style14.setVerticalAlignment(VerticalAlignment.CENTER);

        CellStyle groupHeader = workbook.createCellStyle();
        groupHeader.setFont(font14Bold);
        groupHeader.setAlignment(HorizontalAlignment.CENTER);
        groupHeader.setVerticalAlignment(VerticalAlignment.CENTER);
        groupHeader.setBorderLeft(BorderStyle.MEDIUM);
        groupHeader.setBorderBottom(BorderStyle.MEDIUM);
        groupHeader.setBorderRight(BorderStyle.MEDIUM);
        groupHeader.setBorderTop(BorderStyle.MEDIUM);

        Stream.of(1, 2, 4).forEach(i -> sheet.getRow(i).getCell(0).setCellStyle(style14));

        sheet.getRow(1).getCell(0).setCellValue(sessionType.equals("установочная") ? "установочной сессии" : (((halfYear == 1) ? "весенней " : "осенней ") + sessionType + " сессии"));
        sheet.getRow(2).getCell(0).setCellValue("студентов " + groupLessons.values().stream().toList().get(0).get(0).getGroup().getCurrentCourse() + " курса");
        sheet.getRow(4).getCell(0).setCellValue(String.format("с %s.%s.%s по %s.%s.%s",
                dateFrom.getDayOfMonth(), dateFrom.getMonthValue() < 10 ? "0" + dateFrom.getMonthValue() : dateFrom.getMonthValue(), dateFrom.getYear(),
                dateTo.getDayOfMonth(), dateTo.getMonthValue() < 10 ? "0" + dateTo.getMonthValue() : dateTo.getMonthValue(), dateTo.getYear())
        );
        for (int i = 0; i < groupIds.length * 2; i++) {
            sheet.setColumnWidth(3 + i, columnWidth * 256);
        }

        WeekFields weekFields = WeekFields.of(Locale.getDefault());
        Map<LocalDate, LocalDate> weeks = createWeeksMap(dateFrom, dateTo, weekFields);

        for (int i = 0; i < weeks.keySet().size() - 1; i++) {
            workbook.cloneSheet(0);
        }

        List<Map.Entry<LocalDate, LocalDate>> entries = new ArrayList<>(weeks.entrySet());
        for (int i = 0; i < weeks.keySet().size(); i++) {
            Map.Entry<LocalDate, LocalDate> entry = entries.get(i);
            workbook.setSheetName(i, dateToString(entry.getKey()) + "-" + dateToString(entry.getValue()));
            Map<String, List<LessonDTO>> weekLessons = new TreeMap<>(comparator);
            weekLessons.putAll(
                    groupLessons.entrySet().stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().stream()
                                    .filter(lessonDTO -> (lessonDTO.getStartDate().isEqual(entry.getKey()) || lessonDTO.getStartDate().isAfter(entry.getKey())) &&
                                            (lessonDTO.getEndDate().isBefore(entry.getValue()) || lessonDTO.getEndDate().isEqual(entry.getValue()))
                                    ).toList()
                            ))
            );
            fillZaochSheet(workbook.getSheetAt(i), entry.getKey(), entry.getValue(), weekLessons, groupHeader, columnWidth);
        }


        return workbook;
    }

    /**
     * Эксель для загруженности преподавателей на определённый промежуток дат.
     *
     * @param teacherIds Массив идентификаторов преподавателей
     * @param from       Дата с которой необходимо отобразить загруженность
     * @param to         Дата по которую необходимо отобразить загруженность
     * @return Сформированный эксель файл, где цифры - номер пары, когда занят преподаватель.<br/>
     * Жирные цифры большим шрифтом - пара у дневного отделения.<br/>
     * Цифра маленьким шрифтом - пара у заочной группы. Если подчёркнуто - экзамен<br/>
     * Цифра маленьким шрифтом с подписью "м" - пара у группы магистратуры.
     * @see #teacherFioString
     * @see #dateToString
     * @see #convertDayOfWeek
     * @see #definitionOfWeek
     */
    public Workbook excelTeacherWorkload(long[] teacherIds, String from, String to) {
        LocalDate dateFrom = LocalDate.parse(from);
        LocalDate dateTo = LocalDate.parse(to);

        Map<TeacherModel, List<LessonDTO>> teacherLessonsMap = new LinkedHashMap<>();

        for (long teacherId : teacherIds) {
            TeacherModel t = teacherRepo.getSingle(teacherId);
            if (t != null)
                teacherLessonsMap.put(t, new ArrayList<>());
            List<LessonDTO> lessonDTOS = lessonMapper.toDto(lessonRepo.findByTeacherIdAndStatus(teacherId, EStatus.ACTIVE).stream()
                    .filter(l ->
                            !((l.getStartDate().isBefore(dateFrom) && l.getEndDate().isBefore(dateFrom)) ||
                                    (l.getStartDate().isAfter(dateTo) && l.getEndDate().isAfter(dateTo)))
                    ).toList());
            if (lessonDTOS != null && !lessonDTOS.isEmpty())
                teacherLessonsMap.computeIfPresent(lessonDTOS.get(0).getTeacher(), (k, v) -> {
                    v.addAll(lessonDTOS);
                    return v;
                });
        }
        log.info("Сформирован excel нагрузки преподавателей: {}", teacherLessonsMap.keySet().stream().map(this::teacherFioString).toList());

        Workbook workbook = new XSSFWorkbook();

        Font font12 = workbook.createFont();
        Font font12Underline = workbook.createFont();
        Font font14 = workbook.createFont();
        Font font16bold = workbook.createFont();

        font12.setFontName("Arial");
        font12.setFontHeightInPoints((short) 12);

        font12Underline.setFontName("Arial");
        font12Underline.setFontHeightInPoints((short) 12);
        font12Underline.setUnderline(Font.U_SINGLE);

        font14.setFontName("Arial");
        font14.setFontHeightInPoints((short) 14);

        font16bold.setFontName("Arial");
        font16bold.setFontHeightInPoints((short) 16);
        font16bold.setBold(true);

        CellStyle headerStyle = workbook.createCellStyle(), cellStyle = workbook.createCellStyle(), teacherCellStyle = workbook.createCellStyle();
        headerStyle.setFont(font12);
        headerStyle.setWrapText(true);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);

        cellStyle.cloneStyleFrom(headerStyle);
        cellStyle.setWrapText(false);

        teacherCellStyle.cloneStyleFrom(cellStyle);
        teacherCellStyle.setFont(font14);

        Sheet sheet = workbook.createSheet(dateToString(dateFrom) + "-" + dateToString(dateTo));
        Row row = sheet.createRow(0);
        row.createCell(0).setCellValue("Преподаватель");
        row.getCell(0).setCellStyle(headerStyle);

        List<LocalDate> dateList = dateFrom.datesUntil(dateTo.plusDays(1)).toList();
        for (int i = 0; i < dateList.size(); i++) {
            LocalDate temp = dateList.get(i);
            row.createCell(i + 1).setCellStyle(headerStyle);
            String cellValue = dateToString(temp) + "\n" + convertDayOfWeek(temp.getDayOfWeek().getValue()) + "\n" + (definitionOfWeek(temp) % 2 == 0 ? "зн." : "чис.");
            RichTextString richTextString = new XSSFRichTextString(cellValue);
            richTextString.applyFont(0, dateToString(temp).length(), font14);
            richTextString.applyFont(dateToString(temp).length(), cellValue.length(), font12);
            row.getCell(i + 1).setCellValue(richTextString);
        }

        int rowIndex = 1;
        for (TeacherModel teacher : teacherLessonsMap.keySet()) {
            row = sheet.createRow(rowIndex);
            row.createCell(0).setCellValue(teacherFioString(teacher));
            row.getCell(0).setCellStyle(teacherCellStyle);

            List<LessonDTO> teacherLessons = teacherLessonsMap.get(teacher);
            for (int i = 0; i < dateList.size(); i++) {
                LocalDate current = dateList.get(i);
                int currentWeek = definitionOfWeek(current);
                XSSFRichTextString cellValue = new XSSFRichTextString();
                teacherLessons.stream()
                        .filter(l -> {
                            if (l.getStartDate().isEqual(l.getEndDate()) && l.getStartDate().isEqual(current))
                                return true;
                            else
                                return current.isAfter(l.getStartDate().minusDays(1)) &&
                                        current.isBefore(l.getEndDate().plusDays(1)) &&
                                        l.getDay() == (current.getDayOfWeek().getValue() - 1) &&
                                        (l.getWeekType().equals(EWeekType.ALWAYS) ||
                                                (l.getWeekType().equals(EWeekType.NUMERATOR) && currentWeek % 2 == 1) ||
                                                (l.getWeekType().equals(EWeekType.DENOMINATOR) && currentWeek % 2 == 0) ||
                                                (l.getWeekType().equals(EWeekType.FIRST) && currentWeek == 1) ||
                                                (l.getWeekType().equals(EWeekType.SECOND) && currentWeek == 2) ||
                                                (l.getWeekType().equals(EWeekType.THIRD) && currentWeek == 3) ||
                                                (l.getWeekType().equals(EWeekType.FOURTH) && currentWeek == 4));

                        })
                        .collect(Collectors.toMap(
                                LessonDTO::getLessonNumber,
                                lessonDTO -> lessonDTO,
                                (o, o2) -> o
                        ))
                        .values()
                        .stream()
                        .sorted(Comparator.comparing(LessonDTO::getLessonNumber))
                        .map(l -> {
                            RichTextString res;
                            switch (l.getGroup().getFaculty().getId().intValue()) {
                                case 3, 4, 5, 6 -> {
                                    res = new XSSFRichTextString(String.valueOf(l.getLessonNumber()));
                                    res.applyFont(font16bold);
                                }
                                case 7 -> {
                                    res = new XSSFRichTextString(l.getLessonNumber() + "м");
                                    res.applyFont(font12);
                                }
                                default -> {
                                    res = new XSSFRichTextString(String.valueOf(l.getLessonNumber()));
                                    if (Arrays.asList(ELessonTypes.EXAM, ELessonTypes.EXAM_REVIEW, ELessonTypes.CONSULT_EXAM).contains(l.getLessonType()))
                                        res.applyFont(font12Underline);
                                    else res.applyFont(font12);
                                }
                            }
                            return res;
                        }).forEachOrdered(rts -> cellValue.append(rts.getString(), ((XSSFRichTextString) rts).getFontAtIndex(0)));
                row.createCell(i + 1).setCellStyle(cellStyle);
                row.getCell(i + 1).setCellValue(cellValue);
            }
            rowIndex++;
        }

        row.iterator().forEachRemaining(cell -> {
                    sheet.autoSizeColumn(cell.getColumnIndex());
                    if (sheet.getColumnWidth(cell.getColumnIndex()) < 3200) {
                        sheet.setColumnWidth(cell.getColumnIndex(), 3200);
                    }
                }
        );

        sheet.createFreezePane(1, 1);

        PrintSetup printSetup = sheet.getPrintSetup();
        printSetup.setPaperSize(PrintSetup.A4_PAPERSIZE);
        printSetup.setLandscape(true);
        printSetup.setFitHeight((short) 1);
        printSetup.setFitWidth((short) 1);

        CTWorksheet worksheet = ((XSSFSheet) sheet).getCTWorksheet();
        CTPageMargins ctMargins = worksheet.getPageMargins();
        ctMargins.setTop(0.1875);
        ctMargins.setLeft(0.1875);
        ctMargins.setRight(0.1875);
        ctMargins.setBottom(0.1875);

        return workbook;
    }

    /**
     * Эксель для загруженности аудиторий на определённый промежуток дат.
     *
     * @param classroomIds Массив идентификаторов аудиторий
     * @param from         Дата с которой необходимо отобразить загруженность
     * @param to           Дата по которую необходимо отобразить загруженность
     * @return Сформированный эксель файл, где цифры - номер пары, когда занята аудитория.<br/>
     * * Жирные цифры большим шрифтом - пара у дневного отделения.<br/>
     * * Цифра маленьким шрифтом - пара у заочной группы. Если подчёркнуто - экзамен<br/>
     * * Цифра маленьким шрифтом с подписью "м" - пара у группы магистратуры.
     * @see #classroomToString(ClassroomModel)
     * @see #dateToString
     * @see #convertDayOfWeek
     * @see #definitionOfWeek
     */
    public Workbook excelClassroomWorkload(long[] classroomIds, String from, String to) {
        LocalDate dateFrom = LocalDate.parse(from);
        LocalDate dateTo = LocalDate.parse(to);

        Map<ClassroomModel, List<LessonDTO>> classroomLessonsMap = new LinkedHashMap<>();

        for (long classroomId : classroomIds) {
            ClassroomModel c = classroomRepo.getSingle(classroomId);
            if (c != null)
                classroomLessonsMap.put(c, new ArrayList<>());
            List<LessonDTO> lessonDTOS = lessonMapper.toDto(lessonRepo.findByRoomIdAndStatus(classroomId, EStatus.ACTIVE).stream()
                    .filter(l ->
                            !((l.getStartDate().isBefore(dateFrom) && l.getEndDate().isBefore(dateFrom)) ||
                                    (l.getStartDate().isAfter(dateTo) && l.getEndDate().isAfter(dateTo)))
                    ).toList());
            if (lessonDTOS != null && !lessonDTOS.isEmpty())
                classroomLessonsMap.computeIfPresent(lessonDTOS.get(0).getClassroom(), (k, v) -> {
                    v.addAll(lessonDTOS);
                    return v;
                });
        }
        log.info("Сформирован excel нагрузки аудиторий: {}", classroomLessonsMap.keySet().stream().map(this::classroomToString).toList());

        Workbook workbook = new XSSFWorkbook();

        Font font12 = workbook.createFont();
        Font font12Underline = workbook.createFont();
        Font font14 = workbook.createFont();
        Font font16bold = workbook.createFont();

        font12.setFontName("Arial");
        font12.setFontHeightInPoints((short) 12);

        font12Underline.setFontName("Arial");
        font12Underline.setFontHeightInPoints((short) 12);
        font12Underline.setUnderline(Font.U_SINGLE);

        font14.setFontName("Arial");
        font14.setFontHeightInPoints((short) 14);

        font16bold.setFontName("Arial");
        font16bold.setFontHeightInPoints((short) 16);
        font16bold.setBold(true);

        CellStyle headerStyle = workbook.createCellStyle(), cellStyle = workbook.createCellStyle(), classroomCellStyle = workbook.createCellStyle();
        headerStyle.setFont(font12);
        headerStyle.setWrapText(true);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);

        cellStyle.cloneStyleFrom(headerStyle);
        cellStyle.setWrapText(false);

        classroomCellStyle.cloneStyleFrom(cellStyle);
        classroomCellStyle.setFont(font14);

        Sheet sheet = workbook.createSheet(dateToString(dateFrom) + "-" + dateToString(dateTo));
        Row row = sheet.createRow(0);
        row.createCell(0).setCellValue("Аудитория");
        row.getCell(0).setCellStyle(headerStyle);

        List<LocalDate> dateList = dateFrom.datesUntil(dateTo.plusDays(1)).toList();
        for (int i = 0; i < dateList.size(); i++) {
            LocalDate temp = dateList.get(i);
            row.createCell(i + 1).setCellStyle(headerStyle);
            String cellValue = dateToString(temp) + "\n" + convertDayOfWeek(temp.getDayOfWeek().getValue()) + "\n" + (definitionOfWeek(temp) % 2 == 0 ? "зн." : "чис.");
            RichTextString richTextString = new XSSFRichTextString(cellValue);
            richTextString.applyFont(0, dateToString(temp).length(), font14);
            richTextString.applyFont(dateToString(temp).length(), cellValue.length(), font12);
            row.getCell(i + 1).setCellValue(richTextString);
        }

        int rowIndex = 1;
        for (ClassroomModel classroom : classroomLessonsMap.keySet()) {
            row = sheet.createRow(rowIndex);
            row.createCell(0).setCellValue(classroomToString(classroom));
            row.getCell(0).setCellStyle(classroomCellStyle);

            List<LessonDTO> classroomLessons = classroomLessonsMap.get(classroom);
            for (int i = 0; i < dateList.size(); i++) {
                LocalDate current = dateList.get(i);
                int currentWeek = definitionOfWeek(current);
                XSSFRichTextString cellValue = new XSSFRichTextString();
                classroomLessons.stream()
                        .filter(l -> {
                            if (l.getStartDate().isEqual(l.getEndDate()) && l.getStartDate().isEqual(current))
                                return true;
                            else
                                return current.isAfter(l.getStartDate().minusDays(1)) &&
                                        current.isBefore(l.getEndDate().plusDays(1)) &&
                                        l.getDay() == (current.getDayOfWeek().getValue() - 1) &&
                                        (l.getWeekType().equals(EWeekType.ALWAYS) ||
                                                (l.getWeekType().equals(EWeekType.NUMERATOR) && currentWeek % 2 == 1) ||
                                                (l.getWeekType().equals(EWeekType.DENOMINATOR) && currentWeek % 2 == 0) ||
                                                (l.getWeekType().equals(EWeekType.FIRST) && currentWeek == 1) ||
                                                (l.getWeekType().equals(EWeekType.SECOND) && currentWeek == 2) ||
                                                (l.getWeekType().equals(EWeekType.THIRD) && currentWeek == 3) ||
                                                (l.getWeekType().equals(EWeekType.FOURTH) && currentWeek == 4));

                        })
                        .collect(Collectors.toMap(
                                LessonDTO::getLessonNumber,
                                lessonDTO -> lessonDTO,
                                (o, o2) -> o
                        ))
                        .values()
                        .stream()
                        .sorted(Comparator.comparing(LessonDTO::getLessonNumber))
                        .map(l -> {
                            RichTextString res;
                            switch (l.getGroup().getFaculty().getId().intValue()) {
                                case 3, 4, 5, 6 -> {
                                    res = new XSSFRichTextString(String.valueOf(l.getLessonNumber()));
                                    res.applyFont(font16bold);
                                }
                                case 7 -> {
                                    res = new XSSFRichTextString(l.getLessonNumber() + "м");
                                    res.applyFont(font12);
                                }
                                default -> {
                                    res = new XSSFRichTextString(String.valueOf(l.getLessonNumber()));
                                    if (Arrays.asList(ELessonTypes.EXAM, ELessonTypes.EXAM_REVIEW, ELessonTypes.CONSULT_EXAM).contains(l.getLessonType()))
                                        res.applyFont(font12Underline);
                                    else res.applyFont(font12);
                                }
                            }
                            return res;
                        }).forEachOrdered(rts -> cellValue.append(rts.getString(), ((XSSFRichTextString) rts).getFontAtIndex(0)));
                row.createCell(i + 1).setCellStyle(cellStyle);
                row.getCell(i + 1).setCellValue(cellValue);
            }
            rowIndex++;
        }

        row.iterator().forEachRemaining(cell -> {
                    sheet.autoSizeColumn(cell.getColumnIndex());
                    if (sheet.getColumnWidth(cell.getColumnIndex()) < 3200) {
                        sheet.setColumnWidth(cell.getColumnIndex(), 3200);
                    }
                }
        );

        sheet.createFreezePane(1, 1);

        PrintSetup printSetup = sheet.getPrintSetup();
        printSetup.setPaperSize(PrintSetup.A4_PAPERSIZE);
        printSetup.setLandscape(true);
        printSetup.setFitHeight((short) 1);
        printSetup.setFitWidth((short) 1);

        CTWorksheet worksheet = ((XSSFSheet) sheet).getCTWorksheet();
        CTPageMargins ctMargins = worksheet.getPageMargins();
        ctMargins.setTop(0.1875);
        ctMargins.setLeft(0.1875);
        ctMargins.setRight(0.1875);
        ctMargins.setBottom(0.1875);

        return workbook;
    }

    private InputStream getTemplate(String fileName) throws IOException {
        return resourceLoader.getResource("classpath:templates/" + fileName).getInputStream();
    }

    /**
     * Создаёт объект Map, где из периода дат создаются пары ключ-значение с датой начала недели и датой конца недели.<br/>
     * Пары ключ-значение всегда являются началом и концом недели, даже если концы промежутка дат не являются ими.
     *
     * @param dateFrom   Дата начала промежутка
     * @param dateTo     Дата конца промежутка
     * @param weekFields Класс для работы с днями недели
     * @return Объект Map, где ключ - дата начала недели, значение - дата конца недели.
     * @see #excelZaoch
     */
    private static Map<LocalDate, LocalDate> createWeeksMap(LocalDate dateFrom, LocalDate dateTo, WeekFields weekFields) {
        LocalDate firstMonday = dateFrom.with(weekFields.dayOfWeek(), 1);

        LocalDate lastSunday = dateTo.with(weekFields.dayOfWeek(), 7);

        Map<LocalDate, LocalDate> weeks = new LinkedHashMap<>();
        LocalDate currentMonday = firstMonday;
        while (currentMonday.isBefore(lastSunday) || currentMonday.isEqual(lastSunday)) {
            LocalDate currentSunday = currentMonday.plusDays(6);
            weeks.put(currentMonday, currentSunday);
            currentMonday = currentSunday.plusDays(1);
        }
        return weeks;
    }

    /**
     * Создаёт объект Map с отсортированными ключами.
     *
     * @return Объект {@link Map}, в котором ключи отсортированы определённым образом
     * @see #excelDaytime
     * @see #excelDaytimeSession
     */
    @NotNull
    private static Map<String, List<LessonDTO>> sortedKeysMap() {
        Comparator<String> comparator = (s1, s2) -> {
            boolean hasBrackets1 = s1.contains("(");
            boolean hasBrackets2 = s2.contains("(");

            if (hasBrackets1 && hasBrackets2) {
                return s2.compareTo(s1);
            } else if (hasBrackets1) {
                return 1;
            } else if (hasBrackets2) {
                return -1;
            } else {
                return s1.compareTo(s2);
            }
        };
        return new TreeMap<>(comparator);
    }

    /**
     * Переводит дату {@link LocalDate} в строку.
     *
     * @param date Дата, которую нужно перевести
     * @return Строку вида "{@code ДД.ММ.ГГ}".
     * @see #excelZaoch
     * @see #collectLessonDays
     * @see #excelDaytimeSession
     * @see #excelTeacherWorkload
     */
    @NotNull
    private String dateToString(LocalDate date) {
        return date.getDayOfMonth() + "." + date.getMonthValue() + "." + String.valueOf(date.getYear()).substring(2);
    }

    /**
     * Метод для заполнения листа экселя заочных групп.
     *
     * @param sheet       Лист для заполнения
     * @param dateFrom    Дата, с которой учитываются занятия
     * @param dateTo      Дата, по которую учитываются занятия
     * @param weekLessons Список занятий {@link LessonDTO} на промежуток дат
     * @param headerStyle Стиль оформления шапки таблицы
     * @param columnWidth Ширина одной колонки
     * @see #excelZaoch
     * @see #createCellStyle
     * @see #cellValueZaoch
     */
    private void fillZaochSheet(Sheet sheet, LocalDate dateFrom, LocalDate dateTo, Map<String, List<LessonDTO>> weekLessons, CellStyle headerStyle, int columnWidth) {
        Workbook workbook = sheet.getWorkbook();
        CellStyle borderLeft = createCellStyle(workbook),
                borderRight = createCellStyle(workbook),
                borderTopLeft = createCellStyle(workbook),
                borderTopRight = createCellStyle(workbook),
                borderBottomLeft = createCellStyle(workbook),
                borderBottomRight = createCellStyle(workbook),
                borderLeftRight = createCellStyle(workbook),
                borderTopLeftRight = createCellStyle(workbook),
                borderBottomLeftRight = createCellStyle(workbook);

        XSSFFont fontSize12 = (XSSFFont) workbook.createFont();
        fontSize12.setFontName("Arial Cyr");
        fontSize12.setFontHeightInPoints((short) 12);

        XSSFFont fontSize12Bold = (XSSFFont) workbook.createFont();
        fontSize12Bold.setFontName("Arial Cyr");
        fontSize12Bold.setFontHeightInPoints((short) 12);
        fontSize12Bold.setBold(true);

        borderLeft.setBorderLeft(BorderStyle.MEDIUM);

        borderRight.setBorderRight(BorderStyle.MEDIUM);

        borderTopLeft.setBorderLeft(BorderStyle.MEDIUM);
        borderTopLeft.setBorderTop(BorderStyle.MEDIUM);

        borderTopRight.setBorderTop(BorderStyle.MEDIUM);
        borderTopRight.setBorderRight(BorderStyle.MEDIUM);

        borderBottomLeft.setBorderBottom(BorderStyle.MEDIUM);
        borderBottomLeft.setBorderLeft(BorderStyle.MEDIUM);

        borderBottomRight.setBorderBottom(BorderStyle.MEDIUM);
        borderBottomRight.setBorderRight(BorderStyle.MEDIUM);

        borderLeftRight.setBorderLeft(BorderStyle.MEDIUM);
        borderLeftRight.setBorderRight(BorderStyle.MEDIUM);

        borderTopLeftRight.setBorderTop(BorderStyle.MEDIUM);
        borderTopLeftRight.setBorderLeft(BorderStyle.MEDIUM);
        borderTopLeftRight.setBorderRight(BorderStyle.MEDIUM);

        borderBottomLeftRight.setBorderBottom(BorderStyle.MEDIUM);
        borderBottomLeftRight.setBorderLeft(BorderStyle.MEDIUM);
        borderBottomLeftRight.setBorderRight(BorderStyle.MEDIUM);

        LocalDate currentDay = dateFrom;
        int rowIndex = 10, cellIndex = 3;

        while (currentDay.isBefore(dateTo) || currentDay.isEqual(dateTo)) {
            sheet.getRow(rowIndex).getCell(1).setCellValue(currentDay);
            rowIndex += 7;
            currentDay = currentDay.plusDays(1);
        }

        for (String group : weekLessons.keySet()) {
            sheet.getRow(9).createCell(cellIndex).setCellStyle(headerStyle);
            sheet.getRow(9).createCell(cellIndex + 1).setCellStyle(headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(9, 9, cellIndex, cellIndex + 1));
            sheet.getRow(9).getCell(cellIndex).setCellValue(group);
            cellIndex += 2;
        }

        cellIndex = 3;
        for (String group : weekLessons.keySet()) {
            List<LessonDTO> currentLessons = weekLessons.get(group);

            rowIndex = 10;

            for (int i = 0; i < 7; i++) {
                int day = i;
                List<LessonDTO> dayLessons = currentLessons.stream().filter(lessonDTO -> lessonDTO.getDay() == day).toList();

                for (int j = 1; j <= 7; j++) {
                    int lessonNumber = j;
                    List<LessonDTO> timeLessons = dayLessons.stream().filter(lessonDTO -> lessonDTO.getLessonNumber() == lessonNumber).toList();

                    if (!timeLessons.isEmpty()) {
                        for (LessonDTO lessonDTO : timeLessons) {
                            CellStyle style = createCellStyle(workbook);
                            XSSFCell cell;
                            if (Objects.requireNonNull(lessonDTO.getSubGroup()) == ESubGroup.FIRST) {
                                if (lessonDTO.getLessonNumber() == 1) {
                                    style.cloneStyleFrom(borderTopLeft);
                                } else if (lessonDTO.getLessonNumber() == 7) {
                                    style.cloneStyleFrom(borderBottomLeft);
                                } else {
                                    style.cloneStyleFrom(borderLeft);
                                }
                                cell = (XSSFCell) sheet.getRow(rowIndex).getCell(cellIndex);
                                if (cell == null || cell.getStringCellValue() == null || cell.getCellType() == CellType.BLANK)
                                    cell = (XSSFCell) sheet.getRow(rowIndex).createCell(cellIndex);
                            } else if (lessonDTO.getSubGroup() == ESubGroup.SECOND) {
                                if (lessonDTO.getLessonNumber() == 1) {
                                    style.cloneStyleFrom(borderTopRight);
                                } else if (lessonDTO.getLessonNumber() == 7) {
                                    style.cloneStyleFrom(borderBottomRight);
                                } else {
                                    style.cloneStyleFrom(borderRight);
                                }
                                cell = (XSSFCell) sheet.getRow(rowIndex).getCell(cellIndex + 1);
                                if (cell == null || cell.getStringCellValue() == null || cell.getCellType() == CellType.BLANK)
                                    cell = (XSSFCell) sheet.getRow(rowIndex).createCell(cellIndex + 1);
                            } else {
                                if (lessonDTO.getLessonNumber() == 1) {
                                    style.cloneStyleFrom(borderTopLeftRight);
                                } else if (lessonDTO.getLessonNumber() == 7) {
                                    style.cloneStyleFrom(borderBottomLeftRight);
                                } else {
                                    style.cloneStyleFrom(borderLeftRight);
                                }
                                cell = (XSSFCell) sheet.getRow(rowIndex).getCell(cellIndex);
                                if (cell == null || cell.getStringCellValue() == null || cell.getCellType() == CellType.BLANK)
                                    cell = (XSSFCell) sheet.getRow(rowIndex).createCell(cellIndex);
                            }

                            try {
                                RichTextString richText;
                                if (Objects.requireNonNull(lessonDTO.getSubGroup()) == ESubGroup.FIRST || lessonDTO.getSubGroup() == ESubGroup.SECOND) {
                                    for (int mg = 0; mg < sheet.getNumMergedRegions(); mg++) {
                                        CellRangeAddress range = sheet.getMergedRegion(mg);
                                        if (range.getFirstRow() == rowIndex && range.getFirstColumn() == cellIndex) {
                                            sheet.removeMergedRegion(mg);
                                        }
                                    }

                                    richText = cellValueZaoch(dayLessons, lessonDTO, columnWidth);
                                    cell.setCellStyle(style);
                                    if (!Arrays.asList(ELessonTypes.LAB, ELessonTypes.PRACTICE, ELessonTypes.LECTURE, ELessonTypes.SEMINAR).contains(lessonDTO.getLessonType()))
                                        richText.applyFont(fontSize12Bold);
                                    cell.setCellValue(richText);
                                } else {
                                    richText = cellValueZaoch(dayLessons, lessonDTO, columnWidth);
                                    cell.setCellStyle(style);
                                    if (!Arrays.asList(ELessonTypes.LAB, ELessonTypes.PRACTICE, ELessonTypes.LECTURE, ELessonTypes.SEMINAR).contains(lessonDTO.getLessonType()))
                                        richText.applyFont(fontSize12Bold);
                                    cell.setCellValue(richText);
                                }
                            } catch (IllegalStateException ignored) {
                            }

                        }
                    }
                    rowIndex++;
                }
            }
            cellIndex += 2;
        }

    }

    /**
     * Метод для заполнения листа экселя дневного отделения.
     *
     * @param sheet        Лист для заполнения
     * @param groupLessons Карта, где ключ - название группы, значение - список занятий {@link LessonDTO} этой группы
     * @param dateFrom     Дата, с которой учитываются занятия
     * @param dateTo       Дата, по которую учитываются занятия
     * @see #excelDaytime
     * @see #classroomToString(LessonDTO)
     * @see #disciplineCellValue
     */
    private void fillSheet(Sheet sheet, Map<String, List<LessonDTO>> groupLessons, LocalDate dateFrom, LocalDate dateTo) {
        int cellIndex = 4;

        for (String group : groupLessons.keySet()) {
            sheet.getRow(5).getCell(cellIndex).setCellValue(group);
            List<LessonDTO> currentLessons = groupLessons.get(group);

            int rowIndex = 6;

            for (int i = 0; i < 5; i++) {
                int day = i;
                List<LessonDTO> dayLessons = currentLessons.stream().filter(lessonDTO -> lessonDTO.getDay() == day).toList();

                for (int j = 1; j <= 7; j++) {
                    int lessonNumber = j;
                    List<LessonDTO> timeLessons = dayLessons.stream().filter(lessonDTO -> lessonDTO.getLessonNumber() == lessonNumber).toList();

                    if (!timeLessons.isEmpty()) {

                        for (LessonDTO lesson : timeLessons) {

                            String classrooms;
                            List<LessonDTO> tempList = lessonRepo.findByGroupIdAndDayAndLessonNumberAndStatusAndBetweenDates(
                                    lesson.getGroup().getId(),
                                    lesson.getDay(),
                                    lesson.getLessonNumber(),
                                    EStatus.ACTIVE,
                                    dateFrom,
                                    dateTo).stream().map(lessonMapper::toDto).sorted(Comparator.comparing(LessonDTO::getWeekType).thenComparing(LessonDTO::getSubGroup)).toList();

                            try {

                                if (lesson.getWeekType().equals(EWeekType.ALWAYS)) {
                                    if (Objects.requireNonNull(lesson.getSubGroup()) == ESubGroup.ALL) {
                                        classrooms = tempList.stream().map(this::classroomToString).distinct().sorted().collect(Collectors.joining("\n"));

                                        sheet.getRow(rowIndex).getCell(cellIndex - 1).setCellValue(classrooms);
                                        sheet.getRow(rowIndex).getCell(cellIndex).setCellValue(disciplineCellValue(lesson, dateFrom, dateTo, 123));

                                        // Объединить ячейки в дисциплине
                                        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex + 1, cellIndex, cellIndex + 1));
                                        // Объединить ячейки в аудитории
                                        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex + 1, cellIndex - 1, cellIndex - 1));
                                    } else if (lesson.getSubGroup() == ESubGroup.FIRST) {
                                        if (tempList.stream().anyMatch(l -> l.getSubGroup().equals(ESubGroup.SECOND) && Arrays.asList(EWeekType.NUMERATOR, EWeekType.FIRST, EWeekType.THIRD).contains(l.getWeekType()))) {
                                            classrooms = tempList.stream()
                                                    .sorted(Comparator.comparing(LessonDTO::getSubGroup))
                                                    .filter(l -> l.getSubGroup().equals(ESubGroup.FIRST) ||
                                                            (l.getSubGroup().equals(ESubGroup.SECOND) && Arrays.asList(EWeekType.NUMERATOR, EWeekType.FIRST, EWeekType.THIRD).contains(l.getWeekType())))
                                                    .map(this::classroomToString)
                                                    .distinct().sorted().collect(Collectors.joining("\n"));
                                        } else {
                                            classrooms = tempList.stream()
                                                    .filter(l -> l.getSubGroup().equals(ESubGroup.FIRST))
                                                    .map(this::classroomToString)
                                                    .distinct().sorted().collect(Collectors.joining("\n"));
                                        }

                                        sheet.getRow(rowIndex).getCell(cellIndex - 1).setCellValue(classrooms);
                                        sheet.getRow(rowIndex).getCell(cellIndex).setCellValue(disciplineCellValue(lesson, dateFrom, dateTo, 60));

                                        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex + 1, cellIndex, cellIndex));
                                    } else if (lesson.getSubGroup() == ESubGroup.SECOND) {
                                        if (tempList.stream().anyMatch(l -> l.getSubGroup().equals(ESubGroup.FIRST) && Arrays.asList(EWeekType.NUMERATOR, EWeekType.FIRST, EWeekType.THIRD).contains(l.getWeekType()))) {
                                            classrooms = tempList.stream()
                                                    .sorted(Comparator.comparing(LessonDTO::getSubGroup))
                                                    .filter(l -> l.getSubGroup().equals(ESubGroup.SECOND) ||
                                                            (l.getSubGroup().equals(ESubGroup.FIRST) && Arrays.asList(EWeekType.NUMERATOR, EWeekType.FIRST, EWeekType.THIRD).contains(l.getWeekType())))
                                                    .map(this::classroomToString)
                                                    .distinct().sorted().collect(Collectors.joining("\n"));
                                            sheet.getRow(rowIndex).getCell(cellIndex - 1).setCellValue(classrooms);
                                        } else {
                                            classrooms = tempList.stream()
                                                    .filter(l -> l.getSubGroup().equals(ESubGroup.SECOND))
                                                    .map(this::classroomToString)
                                                    .distinct().sorted().collect(Collectors.joining("\n"));
                                            sheet.getRow(rowIndex + 1).getCell(cellIndex - 1).setCellValue(classrooms);
                                        }

                                        sheet.getRow(rowIndex).getCell(cellIndex + 1).setCellValue(disciplineCellValue(lesson, dateFrom, dateTo, 60));

                                        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex + 1, cellIndex + 1, cellIndex + 1));
                                    } else if (lesson.getSubGroup() == ESubGroup.SEWING || lesson.getSubGroup() == ESubGroup.SHOE || lesson.getSubGroup() == ESubGroup.TEXTILE || lesson.getSubGroup() == ESubGroup.GRAPHIC || lesson.getSubGroup() == ESubGroup.MULTIMEDIA) {
                                        classrooms = tempList.stream()
                                                .filter(l -> l.getSubGroup().equals(lesson.getSubGroup()))
                                                .map(this::classroomToString)
                                                .distinct().sorted().collect(Collectors.joining("\n"));

                                        sheet.getRow(rowIndex).getCell(cellIndex - 1).setCellValue(classrooms);
                                        sheet.getRow(rowIndex).getCell(cellIndex).setCellValue(disciplineCellValue(lesson, dateFrom, dateTo, 123));

                                        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex + 1, cellIndex, cellIndex + 1));
                                        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex + 1, cellIndex - 1, cellIndex - 1));
                                    }
                                }

                                if (lesson.getWeekType().equals(EWeekType.NUMERATOR)) {
                                    if (Objects.requireNonNull(lesson.getSubGroup()) == ESubGroup.ALL) {
                                        classrooms = tempList.stream()
                                                .filter(l -> l.getWeekType().equals(EWeekType.NUMERATOR))
                                                .map(this::classroomToString)
                                                .distinct().sorted().collect(Collectors.joining("\n"));

                                        sheet.getRow(rowIndex).getCell(cellIndex - 1).setCellValue(classrooms);
                                        sheet.getRow(rowIndex).getCell(cellIndex).setCellValue(disciplineCellValue(lesson, dateFrom, dateTo, 72));

                                        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, cellIndex, cellIndex + 1));
                                    } else if (lesson.getSubGroup() == ESubGroup.FIRST) {
                                        if (tempList.stream().anyMatch(l -> l.getSubGroup().equals(ESubGroup.SECOND) && l.getWeekType().equals(EWeekType.ALWAYS))) {
                                            classrooms = tempList.stream()
                                                    .sorted(Comparator.comparing(LessonDTO::getSubGroup))
                                                    .filter(l -> l.getSubGroup().equals(ESubGroup.FIRST) || l.getWeekType().equals(EWeekType.ALWAYS))
                                                    .map(this::classroomToString)
                                                    .distinct().collect(Collectors.joining("\n"));
                                        } else {
                                            classrooms = tempList.stream()
                                                    .filter(l -> l.getWeekType().equals(EWeekType.NUMERATOR))
                                                    .map(this::classroomToString)
                                                    .distinct().collect(Collectors.joining("\n"));
                                        }

                                        sheet.getRow(rowIndex).getCell(cellIndex - 1).setCellValue(classrooms);
                                        sheet.getRow(rowIndex).getCell(cellIndex).setCellValue(disciplineCellValue(lesson, dateFrom, dateTo, 36));
                                    } else if (lesson.getSubGroup() == ESubGroup.SECOND) {
                                        if (tempList.stream().anyMatch(l -> l.getSubGroup().equals(ESubGroup.FIRST) && l.getWeekType().equals(EWeekType.ALWAYS))) {
                                            classrooms = tempList.stream()
                                                    .sorted(Comparator.comparing(LessonDTO::getSubGroup))
                                                    .filter(l -> l.getSubGroup().equals(ESubGroup.SECOND) || l.getWeekType().equals(EWeekType.ALWAYS))
                                                    .map(this::classroomToString)
                                                    .distinct().collect(Collectors.joining("\n"));
                                        } else {
                                            classrooms = tempList.stream()
                                                    .filter(l -> l.getWeekType().equals(EWeekType.NUMERATOR))
                                                    .map(this::classroomToString)
                                                    .distinct().collect(Collectors.joining("\n"));
                                        }

                                        sheet.getRow(rowIndex).getCell(cellIndex - 1).setCellValue(classrooms);
                                        sheet.getRow(rowIndex).getCell(cellIndex + 1).setCellValue(disciplineCellValue(lesson, dateFrom, dateTo, 36));
                                    } else if (lesson.getSubGroup() == ESubGroup.SEWING || lesson.getSubGroup() == ESubGroup.SHOE || lesson.getSubGroup() == ESubGroup.TEXTILE || lesson.getSubGroup() == ESubGroup.GRAPHIC || lesson.getSubGroup() == ESubGroup.MULTIMEDIA) {
                                        classrooms = tempList.stream()
                                                .filter(l -> l.getWeekType().equals(EWeekType.NUMERATOR) && l.getSubGroup().equals(lesson.getSubGroup()))
                                                .map(this::classroomToString)
                                                .distinct().sorted().collect(Collectors.joining("\n"));

                                        sheet.getRow(rowIndex).getCell(cellIndex - 1).setCellValue(classrooms);
                                        sheet.getRow(rowIndex).getCell(cellIndex).setCellValue(disciplineCellValue(lesson, dateFrom, dateTo, 72));

                                        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, cellIndex, cellIndex + 1));
                                    }
                                }

                                if (lesson.getWeekType().equals(EWeekType.DENOMINATOR)) {
                                    if (Objects.requireNonNull(lesson.getSubGroup()) == ESubGroup.ALL) {
                                        classrooms = tempList.stream()
                                                .filter(l -> l.getWeekType().equals(EWeekType.DENOMINATOR))
                                                .map(this::classroomToString)
                                                .distinct().sorted().collect(Collectors.joining("\n"));

                                        sheet.getRow(rowIndex + 1).getCell(cellIndex - 1).setCellValue(classrooms);
                                        sheet.getRow(rowIndex + 1).getCell(cellIndex).setCellValue(disciplineCellValue(lesson, dateFrom, dateTo, 72));

                                        sheet.addMergedRegion(new CellRangeAddress(rowIndex + 1, rowIndex + 1, cellIndex, cellIndex + 1));
                                    } else if (lesson.getSubGroup() == ESubGroup.FIRST) {
                                        if (tempList.stream().anyMatch(l -> l.getSubGroup().equals(ESubGroup.SECOND) && l.getWeekType().equals(EWeekType.ALWAYS))) {
                                            classrooms = tempList.stream()
                                                    .filter(l -> l.getSubGroup().equals(ESubGroup.FIRST))
                                                    .map(this::classroomToString)
                                                    .distinct().collect(Collectors.joining("\n"));
                                        } else {
                                            classrooms = tempList.stream()
                                                    .filter(l -> l.getWeekType().equals(EWeekType.DENOMINATOR))
                                                    .map(this::classroomToString)
                                                    .distinct().collect(Collectors.joining("\n"));
                                        }

                                        sheet.getRow(rowIndex + 1).getCell(cellIndex - 1).setCellValue(classrooms);
                                        sheet.getRow(rowIndex + 1).getCell(cellIndex).setCellValue(disciplineCellValue(lesson, dateFrom, dateTo, 36));
                                    } else if (lesson.getSubGroup() == ESubGroup.SECOND) {
                                        if (tempList.stream().anyMatch(l -> l.getSubGroup().equals(ESubGroup.FIRST) && l.getWeekType().equals(EWeekType.ALWAYS))) {
                                            classrooms = tempList.stream()
                                                    .filter(l -> l.getSubGroup().equals(ESubGroup.SECOND))
                                                    .map(this::classroomToString)
                                                    .distinct().collect(Collectors.joining("\n"));
                                        } else {
                                            classrooms = tempList.stream()
                                                    .filter(l -> l.getWeekType().equals(EWeekType.DENOMINATOR))
                                                    .map(this::classroomToString)
                                                    .distinct().collect(Collectors.joining("\n"));
                                        }
                                        sheet.getRow(rowIndex + 1).getCell(cellIndex - 1).setCellValue(classrooms);
                                        sheet.getRow(rowIndex + 1).getCell(cellIndex + 1).setCellValue(disciplineCellValue(lesson, dateFrom, dateTo, 36));
                                    } else if (lesson.getSubGroup() == ESubGroup.SEWING || lesson.getSubGroup() == ESubGroup.SHOE || lesson.getSubGroup() == ESubGroup.TEXTILE || lesson.getSubGroup() == ESubGroup.GRAPHIC || lesson.getSubGroup() == ESubGroup.MULTIMEDIA) {
                                        classrooms = tempList.stream()
                                                .filter(l -> l.getWeekType().equals(EWeekType.DENOMINATOR) && l.getSubGroup().equals(lesson.getSubGroup()))
                                                .map(this::classroomToString)
                                                .distinct().sorted().collect(Collectors.joining("\n"));

                                        sheet.getRow(rowIndex + 1).getCell(cellIndex - 1).setCellValue(classrooms);
                                        sheet.getRow(rowIndex + 1).getCell(cellIndex).setCellValue(disciplineCellValue(lesson, dateFrom, dateTo, 72));

                                        sheet.addMergedRegion(new CellRangeAddress(rowIndex + 1, rowIndex + 1, cellIndex, cellIndex + 1));
                                    }
                                }

                                if (lesson.getWeekType().equals(EWeekType.FIRST)) {
                                    if (Objects.requireNonNull(lesson.getSubGroup()) == ESubGroup.ALL) {
                                        classrooms = Stream.concat(
                                                        this.lessonRepo.findByGroupIdAndDayAndWeekTypeAndLessonNumberAndSubGroupAndStatusAndDateBetween(
                                                                lesson.getGroup().getId(),
                                                                lesson.getDay(),
                                                                EWeekType.FIRST,
                                                                lesson.getLessonNumber(),
                                                                ESubGroup.ALL,
                                                                EStatus.ACTIVE,
                                                                dateFrom,
                                                                dateTo).stream(),
                                                        this.lessonRepo.findByGroupIdAndDayAndWeekTypeAndLessonNumberAndSubGroupAndStatusAndDateBetween(
                                                                lesson.getGroup().getId(),
                                                                lesson.getDay(),
                                                                EWeekType.THIRD,
                                                                lesson.getLessonNumber(),
                                                                ESubGroup.ALL,
                                                                EStatus.ACTIVE,
                                                                dateFrom,
                                                                dateTo).stream())
                                                .map(lessonMapper::toDto)
                                                .map(this::classroomToString)
                                                .distinct().collect(Collectors.joining("\n"));

                                        sheet.getRow(rowIndex).getCell(cellIndex - 1).setCellValue(classrooms);
                                        sheet.getRow(rowIndex).getCell(cellIndex).setCellValue(disciplineCellValue(lesson, dateFrom, dateTo, 72));

                                        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, cellIndex, cellIndex + 1));
                                    } else if (lesson.getSubGroup() == ESubGroup.FIRST) {
                                        if (tempList.stream().anyMatch(l -> l.getSubGroup().equals(ESubGroup.SECOND) && l.getWeekType().equals(EWeekType.ALWAYS))) {
                                            classrooms = tempList.stream()
                                                    .sorted(Comparator.comparing(LessonDTO::getSubGroup))
                                                    .filter(l -> l.getSubGroup().equals(ESubGroup.FIRST) ||
                                                            (l.getSubGroup().equals(ESubGroup.SECOND) && Arrays.asList(EWeekType.ALWAYS, EWeekType.NUMERATOR, EWeekType.FIRST, EWeekType.THIRD).contains(l.getWeekType())))
                                                    .map(this::classroomToString)
                                                    .distinct().collect(Collectors.joining("\n"));
                                        } else {
                                            classrooms = tempList.stream()
                                                    .filter(l -> Arrays.asList(EWeekType.NUMERATOR, EWeekType.FIRST, EWeekType.THIRD).contains(l.getWeekType()))
                                                    .map(this::classroomToString)
                                                    .distinct().collect(Collectors.joining("\n"));
                                        }

                                        sheet.getRow(rowIndex).getCell(cellIndex - 1).setCellValue(classrooms);
                                        sheet.getRow(rowIndex).getCell(cellIndex).setCellValue(disciplineCellValue(lesson, dateFrom, dateTo, 36));
                                    } else if (lesson.getSubGroup() == ESubGroup.SECOND) {
                                        if (tempList.stream().anyMatch(l -> l.getSubGroup().equals(ESubGroup.FIRST) && l.getWeekType().equals(EWeekType.ALWAYS))) {
                                            classrooms = tempList.stream()
                                                    .sorted(Comparator.comparing(LessonDTO::getSubGroup))
                                                    .filter(l -> l.getSubGroup().equals(ESubGroup.SECOND) ||
                                                            (l.getSubGroup().equals(ESubGroup.FIRST) && Arrays.asList(EWeekType.ALWAYS, EWeekType.NUMERATOR, EWeekType.FIRST, EWeekType.THIRD).contains(l.getWeekType())))
                                                    .map(this::classroomToString)
                                                    .distinct().collect(Collectors.joining("\n"));
                                            sheet.getRow(rowIndex + 1).getCell(cellIndex - 1).setCellValue(classrooms);
                                        } else {
                                            classrooms = tempList.stream()
                                                    .filter(l -> Arrays.asList(EWeekType.NUMERATOR, EWeekType.FIRST, EWeekType.THIRD).contains(l.getWeekType()))
                                                    .map(this::classroomToString)
                                                    .distinct().collect(Collectors.joining("\n"));
                                            sheet.getRow(rowIndex).getCell(cellIndex - 1).setCellValue(classrooms);
                                        }

                                        sheet.getRow(rowIndex).getCell(cellIndex + 1).setCellValue(disciplineCellValue(lesson, dateFrom, dateTo, 36));
                                    } else if (lesson.getSubGroup() == ESubGroup.SEWING || lesson.getSubGroup() == ESubGroup.SHOE || lesson.getSubGroup() == ESubGroup.TEXTILE || lesson.getSubGroup() == ESubGroup.GRAPHIC || lesson.getSubGroup() == ESubGroup.MULTIMEDIA) {
                                        classrooms = Stream.concat(
                                                        this.lessonRepo.findByGroupIdAndDayAndWeekTypeAndLessonNumberAndSubGroupAndStatusAndDateBetween(
                                                                lesson.getGroup().getId(),
                                                                lesson.getDay(),
                                                                EWeekType.FIRST,
                                                                lesson.getLessonNumber(),
                                                                lesson.getSubGroup(),
                                                                EStatus.ACTIVE,
                                                                dateFrom,
                                                                dateTo).stream(),
                                                        this.lessonRepo.findByGroupIdAndDayAndWeekTypeAndLessonNumberAndSubGroupAndStatusAndDateBetween(
                                                                lesson.getGroup().getId(),
                                                                lesson.getDay(),
                                                                EWeekType.THIRD,
                                                                lesson.getLessonNumber(),
                                                                lesson.getSubGroup(),
                                                                EStatus.ACTIVE,
                                                                dateFrom,
                                                                dateTo).stream())
                                                .map(lessonMapper::toDto)
                                                .map(this::classroomToString)
                                                .distinct().sorted().collect(Collectors.joining("\n"));

                                        sheet.getRow(rowIndex).getCell(cellIndex - 1).setCellValue(classrooms);
                                        sheet.getRow(rowIndex).getCell(cellIndex).setCellValue(disciplineCellValue(lesson, dateFrom, dateTo, 72));

                                        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, cellIndex, cellIndex + 1));
                                    }
                                }

                                if (lesson.getWeekType().equals(EWeekType.SECOND)) {
                                    if (Objects.requireNonNull(lesson.getSubGroup()) == ESubGroup.ALL) {
                                        classrooms = Stream.concat(
                                                        this.lessonRepo.findByGroupIdAndDayAndWeekTypeAndLessonNumberAndSubGroupAndStatusAndDateBetween(
                                                                lesson.getGroup().getId(),
                                                                lesson.getDay(),
                                                                EWeekType.SECOND,
                                                                lesson.getLessonNumber(),
                                                                ESubGroup.ALL,
                                                                EStatus.ACTIVE,
                                                                dateFrom,
                                                                dateTo).stream(),
                                                        this.lessonRepo.findByGroupIdAndDayAndWeekTypeAndLessonNumberAndSubGroupAndStatusAndDateBetween(
                                                                lesson.getGroup().getId(),
                                                                lesson.getDay(),
                                                                EWeekType.FOURTH,
                                                                lesson.getLessonNumber(),
                                                                ESubGroup.ALL,
                                                                EStatus.ACTIVE,
                                                                dateFrom,
                                                                dateTo).stream())
                                                .map(lessonMapper::toDto)
                                                .map(this::classroomToString)
                                                .distinct().sorted().collect(Collectors.joining("\n"));

                                        sheet.getRow(rowIndex + 1).getCell(cellIndex - 1).setCellValue(classrooms);
                                        sheet.getRow(rowIndex + 1).getCell(cellIndex).setCellValue(disciplineCellValue(lesson, dateFrom, dateTo, 72));

                                        sheet.addMergedRegion(new CellRangeAddress(rowIndex + 1, rowIndex + 1, cellIndex, cellIndex + 1));
                                    } else if (lesson.getSubGroup() == ESubGroup.FIRST) {
                                        if (tempList.stream().anyMatch(l -> l.getSubGroup().equals(ESubGroup.SECOND) && l.getWeekType().equals(EWeekType.ALWAYS))) {
                                            classrooms = tempList.stream()
                                                    .filter(l -> l.getSubGroup().equals(ESubGroup.FIRST))
                                                    .map(this::classroomToString)
                                                    .distinct().collect(Collectors.joining("\n"));
                                            sheet.getRow(rowIndex).getCell(cellIndex - 1).setCellValue(classrooms);
                                        } else {
                                            classrooms = tempList.stream()
                                                    .filter(l -> Arrays.asList(EWeekType.DENOMINATOR, EWeekType.SECOND, EWeekType.FOURTH).contains(l.getWeekType()))
                                                    .map(this::classroomToString)
                                                    .distinct().collect(Collectors.joining("\n"));
                                            sheet.getRow(rowIndex + 1).getCell(cellIndex - 1).setCellValue(classrooms);
                                        }

                                        sheet.getRow(rowIndex + 1).getCell(cellIndex - 1).setCellValue(classrooms);
                                        sheet.getRow(rowIndex + 1).getCell(cellIndex).setCellValue(disciplineCellValue(lesson, dateFrom, dateTo, 36));
                                    } else if (lesson.getSubGroup() == ESubGroup.SECOND) {
                                        if (tempList.stream().anyMatch(l -> l.getSubGroup().equals(ESubGroup.FIRST) && l.getWeekType().equals(EWeekType.ALWAYS))) {
                                            classrooms = tempList.stream()
                                                    .filter(l -> l.getSubGroup().equals(ESubGroup.SECOND))
                                                    .map(this::classroomToString)
                                                    .distinct().collect(Collectors.joining("\n"));
                                        } else {
                                            classrooms = tempList.stream()
                                                    .filter(l -> Arrays.asList(EWeekType.DENOMINATOR, EWeekType.SECOND, EWeekType.FOURTH).contains(l.getWeekType()))
                                                    .map(this::classroomToString)
                                                    .distinct().collect(Collectors.joining("\n"));
                                        }

                                        sheet.getRow(rowIndex + 1).getCell(cellIndex - 1).setCellValue(classrooms);
                                        sheet.getRow(rowIndex + 1).getCell(cellIndex + 1).setCellValue(disciplineCellValue(lesson, dateFrom, dateTo, 36));
                                    } else if (lesson.getSubGroup() == ESubGroup.SEWING || lesson.getSubGroup() == ESubGroup.SHOE || lesson.getSubGroup() == ESubGroup.TEXTILE || lesson.getSubGroup() == ESubGroup.GRAPHIC || lesson.getSubGroup() == ESubGroup.MULTIMEDIA) {
                                        classrooms = Stream.concat(
                                                        this.lessonRepo.findByGroupIdAndDayAndWeekTypeAndLessonNumberAndSubGroupAndStatusAndDateBetween(
                                                                lesson.getGroup().getId(),
                                                                lesson.getDay(),
                                                                EWeekType.SECOND,
                                                                lesson.getLessonNumber(),
                                                                lesson.getSubGroup(),
                                                                EStatus.ACTIVE,
                                                                dateFrom,
                                                                dateTo).stream(),
                                                        this.lessonRepo.findByGroupIdAndDayAndWeekTypeAndLessonNumberAndSubGroupAndStatusAndDateBetween(
                                                                lesson.getGroup().getId(),
                                                                lesson.getDay(),
                                                                EWeekType.FOURTH,
                                                                lesson.getLessonNumber(),
                                                                lesson.getSubGroup(),
                                                                EStatus.ACTIVE,
                                                                dateFrom,
                                                                dateTo).stream())
                                                .map(lessonMapper::toDto)
                                                .map(this::classroomToString)
                                                .distinct().sorted().collect(Collectors.joining("\n"));

                                        sheet.getRow(rowIndex + 1).getCell(cellIndex - 1).setCellValue(classrooms);
                                        sheet.getRow(rowIndex + 1).getCell(cellIndex).setCellValue(disciplineCellValue(lesson, dateFrom, dateTo, 72));

                                        sheet.addMergedRegion(new CellRangeAddress(rowIndex + 1, rowIndex + 1, cellIndex, cellIndex + 1));
                                    }
                                }

                                if (lesson.getWeekType().equals(EWeekType.THIRD)) {
                                    if (Objects.requireNonNull(lesson.getSubGroup()) == ESubGroup.ALL) {
                                        classrooms = Stream.concat(
                                                        this.lessonRepo.findByGroupIdAndDayAndWeekTypeAndLessonNumberAndSubGroupAndStatusAndDateBetween(
                                                                lesson.getGroup().getId(),
                                                                lesson.getDay(),
                                                                EWeekType.FIRST,
                                                                lesson.getLessonNumber(),
                                                                ESubGroup.ALL,
                                                                EStatus.ACTIVE,
                                                                dateFrom,
                                                                dateTo).stream(),
                                                        this.lessonRepo.findByGroupIdAndDayAndWeekTypeAndLessonNumberAndSubGroupAndStatusAndDateBetween(
                                                                lesson.getGroup().getId(),
                                                                lesson.getDay(),
                                                                EWeekType.THIRD,
                                                                lesson.getLessonNumber(),
                                                                ESubGroup.ALL,
                                                                EStatus.ACTIVE,
                                                                dateFrom,
                                                                dateTo).stream())
                                                .map(lessonMapper::toDto)
                                                .map(this::classroomToString)
                                                .distinct().sorted().collect(Collectors.joining("\n"));

                                        sheet.getRow(rowIndex).getCell(cellIndex - 1).setCellValue(classrooms);
                                        sheet.getRow(rowIndex).getCell(cellIndex).setCellValue(disciplineCellValue(lesson, dateFrom, dateTo, 72));

                                        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, cellIndex, cellIndex + 1));
                                    } else if (lesson.getSubGroup() == ESubGroup.FIRST) {
                                        if (tempList.stream().anyMatch(l -> l.getSubGroup().equals(ESubGroup.SECOND) && l.getWeekType().equals(EWeekType.ALWAYS))) {
                                            classrooms = tempList.stream()
                                                    .sorted(Comparator.comparing(LessonDTO::getSubGroup))
                                                    .filter(l -> l.getSubGroup().equals(ESubGroup.FIRST) ||
                                                            (l.getSubGroup().equals(ESubGroup.SECOND) && Arrays.asList(EWeekType.ALWAYS, EWeekType.NUMERATOR, EWeekType.FIRST, EWeekType.THIRD).contains(l.getWeekType())))
                                                    .map(this::classroomToString)
                                                    .distinct().collect(Collectors.joining("\n"));
                                        } else {
                                            classrooms = tempList.stream()
                                                    .filter(l -> Arrays.asList(EWeekType.NUMERATOR, EWeekType.FIRST, EWeekType.THIRD).contains(l.getWeekType()))
                                                    .map(this::classroomToString)
                                                    .distinct().collect(Collectors.joining("\n"));
                                        }

                                        sheet.getRow(rowIndex).getCell(cellIndex - 1).setCellValue(classrooms);
                                        sheet.getRow(rowIndex).getCell(cellIndex).setCellValue(disciplineCellValue(lesson, dateFrom, dateTo, 36));
                                    } else if (lesson.getSubGroup() == ESubGroup.SECOND) {
                                        if (tempList.stream().anyMatch(l -> l.getSubGroup().equals(ESubGroup.FIRST) && l.getWeekType().equals(EWeekType.ALWAYS))) {
                                            classrooms = tempList.stream()
                                                    .sorted(Comparator.comparing(LessonDTO::getSubGroup))
                                                    .filter(l -> l.getSubGroup().equals(ESubGroup.SECOND) ||
                                                            (l.getSubGroup().equals(ESubGroup.FIRST) && Arrays.asList(EWeekType.ALWAYS, EWeekType.NUMERATOR, EWeekType.FIRST, EWeekType.THIRD).contains(l.getWeekType())))
                                                    .map(this::classroomToString)
                                                    .distinct().collect(Collectors.joining("\n"));
                                        } else {
                                            classrooms = tempList.stream()
                                                    .filter(l -> Arrays.asList(EWeekType.NUMERATOR, EWeekType.FIRST, EWeekType.THIRD).contains(l.getWeekType()))
                                                    .map(this::classroomToString)
                                                    .distinct().collect(Collectors.joining("\n"));
                                        }

                                        sheet.getRow(rowIndex).getCell(cellIndex - 1).setCellValue(classrooms);
                                        sheet.getRow(rowIndex).getCell(cellIndex + 1).setCellValue(disciplineCellValue(lesson, dateFrom, dateTo, 36));
                                    } else if (lesson.getSubGroup() == ESubGroup.SEWING || lesson.getSubGroup() == ESubGroup.SHOE || lesson.getSubGroup() == ESubGroup.TEXTILE || lesson.getSubGroup() == ESubGroup.GRAPHIC || lesson.getSubGroup() == ESubGroup.MULTIMEDIA) {
                                        classrooms = Stream.concat(
                                                        this.lessonRepo.findByGroupIdAndDayAndWeekTypeAndLessonNumberAndSubGroupAndStatusAndDateBetween(
                                                                lesson.getGroup().getId(),
                                                                lesson.getDay(),
                                                                EWeekType.FIRST,
                                                                lesson.getLessonNumber(),
                                                                lesson.getSubGroup(),
                                                                EStatus.ACTIVE,
                                                                dateFrom,
                                                                dateTo).stream(),
                                                        this.lessonRepo.findByGroupIdAndDayAndWeekTypeAndLessonNumberAndSubGroupAndStatusAndDateBetween(
                                                                lesson.getGroup().getId(),
                                                                lesson.getDay(),
                                                                EWeekType.THIRD,
                                                                lesson.getLessonNumber(),
                                                                lesson.getSubGroup(),
                                                                EStatus.ACTIVE,
                                                                dateFrom,
                                                                dateTo).stream())
                                                .map(lessonMapper::toDto)
                                                .map(this::classroomToString)
                                                .distinct().sorted().collect(Collectors.joining("\n"));

                                        sheet.getRow(rowIndex).getCell(cellIndex - 1).setCellValue(classrooms);
                                        sheet.getRow(rowIndex).getCell(cellIndex).setCellValue(disciplineCellValue(lesson, dateFrom, dateTo, 72));

                                        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, cellIndex, cellIndex + 1));
                                    }
                                }

                                if (lesson.getWeekType().equals(EWeekType.FOURTH)) {
                                    if (Objects.requireNonNull(lesson.getSubGroup()) == ESubGroup.ALL) {
                                        classrooms = Stream.concat(
                                                        this.lessonRepo.findByGroupIdAndDayAndWeekTypeAndLessonNumberAndSubGroupAndStatusAndDateBetween(
                                                                lesson.getGroup().getId(),
                                                                lesson.getDay(),
                                                                EWeekType.SECOND,
                                                                lesson.getLessonNumber(),
                                                                ESubGroup.ALL,
                                                                EStatus.ACTIVE,
                                                                dateFrom,
                                                                dateTo).stream(),
                                                        this.lessonRepo.findByGroupIdAndDayAndWeekTypeAndLessonNumberAndSubGroupAndStatusAndDateBetween(
                                                                lesson.getGroup().getId(),
                                                                lesson.getDay(),
                                                                EWeekType.FOURTH,
                                                                lesson.getLessonNumber(),
                                                                ESubGroup.ALL,
                                                                EStatus.ACTIVE,
                                                                dateFrom,
                                                                dateTo).stream())
                                                .map(lessonMapper::toDto)
                                                .map(this::classroomToString)
                                                .distinct().sorted().collect(Collectors.joining("\n"));

                                        sheet.getRow(rowIndex + 1).getCell(cellIndex - 1).setCellValue(classrooms);
                                        sheet.getRow(rowIndex + 1).getCell(cellIndex).setCellValue(disciplineCellValue(lesson, dateFrom, dateTo, 72));

                                        sheet.addMergedRegion(new CellRangeAddress(rowIndex + 1, rowIndex + 1, cellIndex, cellIndex + 1));
                                    } else if (lesson.getSubGroup() == ESubGroup.FIRST) {
                                        if (tempList.stream().anyMatch(l -> l.getSubGroup().equals(ESubGroup.SECOND) && l.getWeekType().equals(EWeekType.ALWAYS))) {
                                            classrooms = tempList.stream()
                                                    .filter(l -> l.getSubGroup().equals(ESubGroup.FIRST))
                                                    .map(this::classroomToString)
                                                    .distinct().collect(Collectors.joining("\n"));
                                        } else {
                                            classrooms = tempList.stream()
                                                    .filter(l -> Arrays.asList(EWeekType.DENOMINATOR, EWeekType.SECOND, EWeekType.FOURTH).contains(l.getWeekType()))
                                                    .map(this::classroomToString)
                                                    .distinct().collect(Collectors.joining("\n"));
                                        }

                                        sheet.getRow(rowIndex + 1).getCell(cellIndex - 1).setCellValue(classrooms);
                                        sheet.getRow(rowIndex + 1).getCell(cellIndex).setCellValue(disciplineCellValue(lesson, dateFrom, dateTo, 36));
                                    } else if (lesson.getSubGroup() == ESubGroup.SECOND) {
                                        if (tempList.stream().anyMatch(l -> l.getSubGroup().equals(ESubGroup.FIRST) && l.getWeekType().equals(EWeekType.ALWAYS))) {
                                            classrooms = tempList.stream()
                                                    .filter(l -> l.getSubGroup().equals(ESubGroup.SECOND))
                                                    .map(this::classroomToString)
                                                    .distinct().collect(Collectors.joining("\n"));
                                        } else {
                                            classrooms = tempList.stream()
                                                    .filter(l -> Arrays.asList(EWeekType.DENOMINATOR, EWeekType.SECOND, EWeekType.FOURTH).contains(l.getWeekType()))
                                                    .map(this::classroomToString)
                                                    .distinct().collect(Collectors.joining("\n"));
                                        }

                                        sheet.getRow(rowIndex + 1).getCell(cellIndex - 1).setCellValue(classrooms);
                                        sheet.getRow(rowIndex + 1).getCell(cellIndex + 1).setCellValue(disciplineCellValue(lesson, dateFrom, dateTo, 36));
                                    } else if (lesson.getSubGroup() == ESubGroup.SEWING || lesson.getSubGroup() == ESubGroup.SHOE || lesson.getSubGroup() == ESubGroup.TEXTILE || lesson.getSubGroup() == ESubGroup.GRAPHIC || lesson.getSubGroup() == ESubGroup.MULTIMEDIA) {
                                        classrooms = Stream.concat(
                                                        this.lessonRepo.findByGroupIdAndDayAndWeekTypeAndLessonNumberAndSubGroupAndStatusAndDateBetween(
                                                                lesson.getGroup().getId(),
                                                                lesson.getDay(),
                                                                EWeekType.SECOND,
                                                                lesson.getLessonNumber(),
                                                                lesson.getSubGroup(),
                                                                EStatus.ACTIVE,
                                                                dateFrom,
                                                                dateTo).stream(),
                                                        this.lessonRepo.findByGroupIdAndDayAndWeekTypeAndLessonNumberAndSubGroupAndStatusAndDateBetween(
                                                                lesson.getGroup().getId(),
                                                                lesson.getDay(),
                                                                EWeekType.FOURTH,
                                                                lesson.getLessonNumber(),
                                                                lesson.getSubGroup(),
                                                                EStatus.ACTIVE,
                                                                dateFrom,
                                                                dateTo).stream())
                                                .map(lessonMapper::toDto)
                                                .map(this::classroomToString)
                                                .distinct().sorted().collect(Collectors.joining("\n"));

                                        sheet.getRow(rowIndex + 1).getCell(cellIndex - 1).setCellValue(classrooms);
                                        sheet.getRow(rowIndex + 1).getCell(cellIndex).setCellValue(disciplineCellValue(lesson, dateFrom, dateTo, 72));

                                        sheet.addMergedRegion(new CellRangeAddress(rowIndex + 1, rowIndex + 1, cellIndex, cellIndex + 1));
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
    }

    /**
     * Метод для получения значения в ячейке с дисциплиной.
     *
     * @param lesson    Занятие {@link LessonDTO}, для которого получается значение
     * @param dateFrom  Дата, с которой учитываются занятия проходящие в той же аудитории
     * @param dateTo    Дата, по которую учитываются занятия проходящие в той же аудитории
     * @param maxLength Максимальное число символов в данной ячейке
     * @return Строку со значением ячейки дисциплины
     * @see #fillSheet
     * @see #convertLessonType
     * @see #teacherFioString
     * @see #convertWeekType
     */
    private String disciplineCellValue(LessonDTO lesson, LocalDate dateFrom, LocalDate dateTo, int maxLength) {
        String lessonType, teachers;
        List<LessonModel> lessons = new ArrayList<>();
        if (Arrays.asList(EWeekType.FIRST, EWeekType.THIRD).contains(lesson.getWeekType())) {
            lessons.addAll(this.lessonRepo.findByGroupIdAndDayAndWeekTypeAndLessonNumberAndSubGroupAndStatusAndDateBetween(
                    lesson.getGroup().getId(),
                    lesson.getDay(),
                    EWeekType.FIRST,
                    lesson.getLessonNumber(),
                    lesson.getSubGroup(),
                    EStatus.ACTIVE,
                    dateFrom,
                    dateTo));
            lessons.addAll(this.lessonRepo.findByGroupIdAndDayAndWeekTypeAndLessonNumberAndSubGroupAndStatusAndDateBetween(
                    lesson.getGroup().getId(),
                    lesson.getDay(),
                    EWeekType.THIRD,
                    lesson.getLessonNumber(),
                    lesson.getSubGroup(),
                    EStatus.ACTIVE,
                    dateFrom,
                    dateTo));
        }
        if (Arrays.asList(EWeekType.SECOND, EWeekType.FOURTH).contains(lesson.getWeekType())) {
            lessons.addAll(this.lessonRepo.findByGroupIdAndDayAndWeekTypeAndLessonNumberAndSubGroupAndStatusAndDateBetween(
                    lesson.getGroup().getId(),
                    lesson.getDay(),
                    EWeekType.SECOND,
                    lesson.getLessonNumber(),
                    lesson.getSubGroup(),
                    EStatus.ACTIVE,
                    dateFrom,
                    dateTo));
            lessons.addAll(this.lessonRepo.findByGroupIdAndDayAndWeekTypeAndLessonNumberAndSubGroupAndStatusAndDateBetween(
                    lesson.getGroup().getId(),
                    lesson.getDay(),
                    EWeekType.FOURTH,
                    lesson.getLessonNumber(),
                    lesson.getSubGroup(),
                    EStatus.ACTIVE,
                    dateFrom,
                    dateTo));
        }
        if (Arrays.asList(EWeekType.ALWAYS, EWeekType.NUMERATOR, EWeekType.DENOMINATOR).contains(lesson.getWeekType()))
            lessons = this.lessonRepo.findByGroupIdAndDayAndWeekTypeAndLessonNumberAndSubGroupAndStatusAndDateBetween(
                    lesson.getGroup().getId(),
                    lesson.getDay(),
                    lesson.getWeekType(),
                    lesson.getLessonNumber(),
                    lesson.getSubGroup(),
                    EStatus.ACTIVE,
                    dateFrom,
                    dateTo);
        lessonType = lessons.stream().sorted(Comparator.comparing(LessonModel::getLessonType)).map(l -> convertLessonType(l.getLessonType())).distinct().collect(Collectors.joining(" / ", "", " "));
        teachers = lessons.stream().map(lessonMapper::toDto).map(l -> teacherFioString(l.getTeacher())).distinct().collect(Collectors.joining(", ", "", ""));

        String cellValue;
        if (Arrays.asList(EWeekType.ALWAYS, EWeekType.NUMERATOR, EWeekType.DENOMINATOR).contains(lesson.getWeekType())) {
            cellValue = lessonType + lesson.getDiscipline().getName() + ", \n" + teachers;
            if (cellValue.length() > maxLength) {
                cellValue = lessonType +
                        (lesson.getDiscipline().getName().split(" \\(")[0].split(" ").length > 1 ?
                                Arrays.stream(lesson.getDiscipline().getName().split(" \\(")[0].split(" "))
                                        .map(word -> word.length() == 1 ?
                                                word
                                                :
                                                word.contains("-") ?
                                                        Arrays.stream(word.split("-")).map(w -> w.substring(0, 1).toUpperCase()).collect(Collectors.joining("-"))
                                                        :
                                                        word.substring(0, 1).toUpperCase())
                                        .collect(Collectors.joining())
                                :
                                lesson.getDiscipline().getName())
                        + ", \n" + teachers;
            }
        } else {
            cellValue = lessonType +
                    lesson.getDiscipline().getName() + ", \n" +
                    teachers + " (" +
                    lessons.stream().map(l -> convertWeekType(l.getWeekType())).distinct().collect(Collectors.joining("   ", "", "")) +
                    ")";
            if (cellValue.length() > maxLength) {
                cellValue = lessonType +
                        (lesson.getDiscipline().getName().split(" \\(")[0].split(" ").length > 1 ?
                                Arrays.stream(lesson.getDiscipline().getName().split(" \\(")[0].split(" "))
                                        .map(word -> word.length() == 1 ?
                                                word
                                                :
                                                word.contains("-") ?
                                                        Arrays.stream(word.split("-")).map(w -> w.substring(0, 1).toUpperCase()).collect(Collectors.joining("-"))
                                                        :
                                                        word.substring(0, 1).toUpperCase())
                                        .collect(Collectors.joining())
                                :
                                lesson.getDiscipline().getName())
                        + ", \n" + teachers + " (" +
                        lessons.stream().map(l -> convertWeekType(l.getWeekType())).distinct().collect(Collectors.joining("   ", "", "")) +
                        ")";
            }
        }
        return cellValue;
    }

    /**
     * Метод для получения строки со значением типа занятия и дисциплины для ячейки экселя заочных групп.
     *
     * @param lesson Занятие {@link LessonDTO}, для которого получается значение
     * @param cut    Параметр для сокращения
     *               <i><p><b>true</b> - сокращает название дисциплины</p>
     *               <p><b>false</b> - название дисциплины записывается полностью</p></i>
     * @return Строку со значением типа занятия и дисциплины
     * @see #cellValueZaoch
     * @see #convertLessonType
     */
    private String zaochDisciplineString(LessonDTO lesson, boolean cut) {
        return convertLessonType(lesson.getLessonType()) + " " +
                (cut && lesson.getDiscipline().getName().split(" \\(")[0].split(" ").length > 1 ?
                        Arrays.stream(lesson.getDiscipline().getName().split(" \\(")[0].split(" "))
                                .map(word -> word.length() == 1 ?
                                        word
                                        :
                                        word.contains("-") ?
                                                Arrays.stream(word.split("-")).map(w -> w.substring(0, 1).toUpperCase()).collect(Collectors.joining("-"))
                                                :
                                                word.substring(0, 1).toUpperCase())
                                .collect(Collectors.joining())
                        :
                        lesson.getDiscipline().getName());
    }

    /**
     * Метод для создания стиля с некоторыми предустановленными значениями.<br/>
     * Подходит преимущественно для экселя заочных групп.
     *
     * @param workbook Книга, для которой создаётся стиль
     * @return Настроенный стиль
     * @see #fillZaochSheet
     */
    private CellStyle createCellStyle(Workbook workbook) {
        Font fontSize12 = workbook.createFont();
        fontSize12.setFontName("Arial Cyr");
        fontSize12.setFontHeightInPoints((short) 12);

        CellStyle style = workbook.createCellStyle();
        style.setFont(fontSize12);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);

        return style;
    }

    /**
     * Метод для получения полного значения ячейки экселя заочных групп. <br/>
     * Учитывает, если одно и то же занятие идёт несколько пар подряд, и в зависимости от этого выдаёт либо частичное, либо полное значение ячейки.
     *
     * @param dayLessons    Список всех занятий {@link LessonDTO} на день
     * @param currentLesson Занятие {@link LessonDTO}, для которого получается значение
     * @param columnWidth   Значение ширины одного столбца
     * @return Строку, для которой можно применить шрифт
     * @see #fillZaochSheet
     * @see #classroomToString(LessonDTO)
     * @see #convertLessonType
     * @see #teacherFioString
     * @see #zaochDisciplineString
     */
    private XSSFRichTextString cellValueZaoch(List<LessonDTO> dayLessons, LessonDTO currentLesson, int columnWidth) {
        XSSFRichTextString cellValue;
        String classrooms, teachers;
        if (dayLessons.stream().anyMatch(
                les -> les.getDiscipline().getId().equals(currentLesson.getDiscipline().getId()) &&
                        les.getLessonType().equals(currentLesson.getLessonType()) &&
                        les.getClassroom().getId().equals(currentLesson.getClassroom().getId()) &&
                        les.getTeacher().getId().equals(currentLesson.getTeacher().getId()) &&
                        !Arrays.asList(ELessonTypes.LECTURE, ELessonTypes.PRACTICE, ELessonTypes.LAB).contains(les.getLessonType()) &&
                        Arrays.asList((short) (currentLesson.getLessonNumber() + 1), (short) (currentLesson.getLessonNumber() + 2)).contains(les.getLessonNumber()))
                &&
                dayLessons.stream().noneMatch(les -> les.getDiscipline().getId().equals(currentLesson.getDiscipline().getId()) &&
                        les.getLessonType().equals(currentLesson.getLessonType()) &&
                        les.getClassroom().getId().equals(currentLesson.getClassroom().getId()) &&
                        les.getTeacher().getId().equals(currentLesson.getTeacher().getId()) &&
                        !Arrays.asList(ELessonTypes.LECTURE, ELessonTypes.PRACTICE, ELessonTypes.LAB).contains(les.getLessonType()) &&
                        les.getLessonNumber() == (short) (currentLesson.getLessonNumber() - 1))
        ) {
            classrooms = lessonMapper.toDto(lessonRepo.findByGroupIdAndDayAndLessonNumberAndSubGroupAndDateAndStatus(currentLesson.getGroup().getId(), currentLesson.getDay(),
                    currentLesson.getLessonNumber(), currentLesson.getSubGroup(), currentLesson.getStartDate(), EStatus.ACTIVE)).stream().map(this::classroomToString).distinct().collect(Collectors.joining(", "));
            cellValue = new XSSFRichTextString(classrooms + " " + convertLessonType(currentLesson.getLessonType()));
        } else if (
                dayLessons.stream().anyMatch(
                        les -> les.getDiscipline().getId().equals(currentLesson.getDiscipline().getId()) &&
                                les.getLessonType().equals(currentLesson.getLessonType()) &&
                                les.getClassroom().getId().equals(currentLesson.getClassroom().getId()) &&
                                les.getTeacher().getId().equals(currentLesson.getTeacher().getId()) &&
                                !Arrays.asList(ELessonTypes.LECTURE, ELessonTypes.PRACTICE, ELessonTypes.LAB).contains(les.getLessonType()) &&
                                les.getLessonNumber() == (short) (currentLesson.getLessonNumber() + 1)) &&
                        dayLessons.stream().anyMatch(
                                les -> les.getDiscipline().getId().equals(currentLesson.getDiscipline().getId()) &&
                                        les.getLessonType().equals(currentLesson.getLessonType()) &&
                                        les.getClassroom().getId().equals(currentLesson.getClassroom().getId()) &&
                                        les.getTeacher().getId().equals(currentLesson.getTeacher().getId()) &&
                                        !Arrays.asList(ELessonTypes.LECTURE, ELessonTypes.PRACTICE, ELessonTypes.LAB).contains(les.getLessonType()) &&
                                        les.getLessonNumber() == (short) (currentLesson.getLessonNumber() - 1))
        )
            cellValue = new XSSFRichTextString(currentLesson.getDiscipline().getName());
        else if (dayLessons.stream().anyMatch(
                les -> les.getDiscipline().getId().equals(currentLesson.getDiscipline().getId()) &&
                        les.getLessonType().equals(currentLesson.getLessonType()) &&
                        les.getClassroom().getId().equals(currentLesson.getClassroom().getId()) &&
                        les.getTeacher().getId().equals(currentLesson.getTeacher().getId()) &&
                        !Arrays.asList(ELessonTypes.LECTURE, ELessonTypes.PRACTICE, ELessonTypes.LAB).contains(les.getLessonType()) &&
                        les.getLessonNumber() == (short) (currentLesson.getLessonNumber() - 1)) &&
                dayLessons.stream().anyMatch(les -> les.getDiscipline().getId().equals(currentLesson.getDiscipline().getId()) &&
                        les.getLessonType().equals(currentLesson.getLessonType()) &&
                        les.getClassroom().getId().equals(currentLesson.getClassroom().getId()) &&
                        les.getTeacher().getId().equals(currentLesson.getTeacher().getId()) &&
                        !Arrays.asList(ELessonTypes.LECTURE, ELessonTypes.PRACTICE, ELessonTypes.LAB).contains(les.getLessonType()) &&
                        les.getLessonNumber() == (short) (currentLesson.getLessonNumber() - 2))
        ) {
            teachers = lessonMapper.toDto(lessonRepo.findByGroupIdAndDayAndLessonNumberAndSubGroupAndDateAndStatus(currentLesson.getGroup().getId(), currentLesson.getDay(),
                            currentLesson.getLessonNumber(), currentLesson.getSubGroup(), currentLesson.getStartDate(), EStatus.ACTIVE)).stream()
                    .map(l -> teacherFioString(l.getTeacher())).distinct().collect(Collectors.joining(", "));
            cellValue = new XSSFRichTextString(teachers);
        } else if (dayLessons.stream().anyMatch(
                les -> les.getDiscipline().getId().equals(currentLesson.getDiscipline().getId()) &&
                        les.getLessonType().equals(currentLesson.getLessonType()) &&
                        les.getClassroom().getId().equals(currentLesson.getClassroom().getId()) &&
                        les.getTeacher().getId().equals(currentLesson.getTeacher().getId()) &&
                        !Arrays.asList(ELessonTypes.LECTURE, ELessonTypes.PRACTICE, ELessonTypes.LAB).contains(les.getLessonType()) &&
                        les.getLessonNumber() == (short) (currentLesson.getLessonNumber() - 1)) &&
                dayLessons.stream().noneMatch(les -> les.getDiscipline().getId().equals(currentLesson.getDiscipline().getId()) &&
                        les.getLessonType().equals(currentLesson.getLessonType()) &&
                        les.getClassroom().getId().equals(currentLesson.getClassroom().getId()) &&
                        les.getTeacher().getId().equals(currentLesson.getTeacher().getId()) &&
                        !Arrays.asList(ELessonTypes.LECTURE, ELessonTypes.PRACTICE, ELessonTypes.LAB).contains(les.getLessonType()) &&
                        les.getLessonNumber() == (short) (currentLesson.getLessonNumber() + 1))) {
            teachers = lessonMapper.toDto(lessonRepo.findByGroupIdAndDayAndLessonNumberAndSubGroupAndDateAndStatus(currentLesson.getGroup().getId(), currentLesson.getDay(),
                            currentLesson.getLessonNumber(), currentLesson.getSubGroup(), currentLesson.getStartDate(), EStatus.ACTIVE)).stream()
                    .map(l -> teacherFioString(l.getTeacher())).distinct().collect(Collectors.joining(", "));
            cellValue = new XSSFRichTextString(currentLesson.getDiscipline().getName() + " " + teachers);
        } else if ((classroomToString(currentLesson) + " " + zaochDisciplineString(currentLesson, false) + " " + teacherFioString(currentLesson.getTeacher())).length() >=
                ((currentLesson.getSubGroup().equals(ESubGroup.ALL) ? 2 : 1) * columnWidth * 280 / 256)
                && !currentLesson.getSubGroup().equals(ESubGroup.ALL)) {
            classrooms = lessonMapper.toDto(lessonRepo.findByGroupIdAndDayAndLessonNumberAndSubGroupAndDateAndStatus(currentLesson.getGroup().getId(), currentLesson.getDay(),
                    currentLesson.getLessonNumber(), currentLesson.getSubGroup(), currentLesson.getStartDate(), EStatus.ACTIVE)).stream().distinct().map(this::classroomToString).collect(Collectors.joining(", "));
            teachers = lessonMapper.toDto(lessonRepo.findByGroupIdAndDayAndLessonNumberAndSubGroupAndDateAndStatus(currentLesson.getGroup().getId(), currentLesson.getDay(),
                            currentLesson.getLessonNumber(), currentLesson.getSubGroup(), currentLesson.getStartDate(), EStatus.ACTIVE)).stream()
                    .map(l -> teacherFioString(l.getTeacher())).distinct().collect(Collectors.joining(", "));
            cellValue = new XSSFRichTextString(classrooms + " " + zaochDisciplineString(currentLesson, true) + " " + teachers);
        } else {
            classrooms = lessonMapper.toDto(lessonRepo.findByGroupIdAndDayAndLessonNumberAndSubGroupAndDateAndStatus(currentLesson.getGroup().getId(), currentLesson.getDay(),
                    currentLesson.getLessonNumber(), currentLesson.getSubGroup(), currentLesson.getStartDate(), EStatus.ACTIVE)).stream().map(this::classroomToString).distinct().collect(Collectors.joining(", "));
            teachers = lessonMapper.toDto(lessonRepo.findByGroupIdAndDayAndLessonNumberAndSubGroupAndDateAndStatus(currentLesson.getGroup().getId(), currentLesson.getDay(),
                            currentLesson.getLessonNumber(), currentLesson.getSubGroup(), currentLesson.getStartDate(), EStatus.ACTIVE)).stream()
                    .map(l -> teacherFioString(l.getTeacher())).distinct().collect(Collectors.joining(", "));
            cellValue = new XSSFRichTextString(classrooms + " " + zaochDisciplineString(currentLesson, false) + " " + teachers);
        }
        return cellValue;
    }

    /**
     * Метод для получения значения ячейки экселя сессии.
     *
     * @param lessons Список занятий {@link LessonDTO}, из которых формируется значение.<br/>
     *                Предполагается заранее, что этот список - занятия определённой группы, в определённый день и определённое время.
     * @return Строку вида "{@code Название_дисциплины ФИО_преподавателей_через_запятую}"
     * @see #excelDaytimeSession
     * @see #teacherFioString
     */
    private String sessionCellValue(List<LessonDTO> lessons) {
        Map<String, String> disciplineTeacherMap = lessons.stream()
                .collect(Collectors.toMap(l -> l.getDiscipline().getName(), l -> teacherFioString(l.getTeacher()), (o, o2) -> {
                            if (o.equals(o2))
                                return o;
                            else return String.join(",", o, o2);
                        }
                ));
        return disciplineTeacherMap.entrySet().stream().map(entry -> entry.getKey() + " " + entry.getValue()).collect(Collectors.joining(", "));
    }

    /**
     * Берёт объект аудитории {@link ClassroomModel} и конвертирует его в строку.
     *
     * @param lesson Занятие {@link LessonDTO}, из которого берётся аудитория
     * @return Строку вида "{@code Номер_корпуса-Номер_аудитории}".<br/>
     * Если аудитория находится в 1-ом корпусе, возвращает только номер аудитории.<br/>
     * Также возвращает сокращённые названия для спортзалов, спорткомплекса и пустой аудитории
     * (используется для онлайн занятий у заочников)
     * @see #fillSheet
     * @see #excelDaytimeSession
     * @see #cellValueZaoch
     * @see #classroomToString(ClassroomModel)
     */
    private String classroomToString(LessonDTO lesson) {
        if (lesson.getClassroom().getId() == 156)
            return "Б с/з";
        if (lesson.getClassroom().getId() == 157)
            return "СК";
        if (lesson.getClassroom().getId() == 158)
            return "М с/з";
        if (lesson.getClassroom().getId() == 172)
            return "";
        if (lesson.getClassroom().getFrame().getId() == -1 || lesson.getClassroom().getFrame().getId() == 1)
            return lesson.getClassroom().getRoomNumber();
        return lesson.getClassroom().getFrame().getId() + "-" + lesson.getClassroom().getRoomNumber();
    }

    /**
     * Конвертирует аудиторию в строку.
     *
     * @param classroomModel Объект аудитории
     * @return Строку вида "{@code Номер_корпуса-Номер_аудитории}".<br/>
     * Если аудитория находится в 1-ом корпусе, возвращает только номер аудитории.<br/>
     * Также возвращает сокращённые названия для спортзалов, спорткомплекса и пустой аудитории
     * (используется для онлайн занятий у заочников)
     * @see #fillSheet
     * @see #excelDaytimeSession
     * @see #cellValueZaoch
     * @see #classroomToString(LessonDTO)
     */
    private String classroomToString(ClassroomModel classroomModel) {
        if (classroomModel.getId() == 156)
            return "Б с/з";
        if (classroomModel.getId() == 157)
            return "СК";
        if (classroomModel.getId() == 158)
            return "М с/з";
        if (classroomModel.getId() == 172)
            return "";
        if (classroomModel.getFrame().getId() == -1 || classroomModel.getFrame().getId() == 1)
            return classroomModel.getRoomNumber();
        return classroomModel.getFrame().getId() + "-" + classroomModel.getRoomNumber();
    }

    /**
     * Метод для получения значения ячейки в экселе с занятостью аудиторий ЦИТ.
     *
     * @param lesson   Занятие, для которого получается значение
     * @param dateFrom Дата, с которой учитываются занятия
     * @param dateTo   Дата, по которую учитываются занятия
     * @return Строку со значением ячейки занятости
     * @see #excelForCit
     * @see #teacherFioString
     * @see #convertWeekType
     * @deprecated Использовалось в {@link #excelForCit}
     */
    @Deprecated
    private String citCellValue(LessonDTO lesson, LocalDate dateFrom, LocalDate dateTo) {
        List<LessonModel> lessons = this.lessonRepo.findByRoomIdAndDayAndLessonNumberAndStatusAndDateBetween(lesson.getClassroom().getId(), lesson.getDay(), lesson.getLessonNumber(), EStatus.ACTIVE, dateFrom, dateTo);
        return lessons.stream().map(lessonMapper::toDto).map(l -> teacherFioString(l.getTeacher()) + " " + convertWeekType(l.getWeekType())).distinct().collect(Collectors.joining(" / ", "", ""));
    }

    /**
     * Метод, который определяет к какой неделе относится дата. <br/>
     * Нужен для определения загруженности преподавателя по дням.
     *
     * @param date Дата, для которой нужно определить неделю
     * @return Число от 1 до 4, соответсвующее номеру недели
     * @see #excelTeacherWorkload
     */
    private int definitionOfWeek(LocalDate date) {
        if (date == null)
            date = LocalDate.now();

        int year = date.getMonthValue() < 7 ? date.getYear() - 1 : date.getYear();
        LocalDate dateStart = LocalDate.parse(year + "-09-01");

        if (dateStart.getDayOfWeek().getValue() == 7 || dateStart.getDayOfWeek().getValue() == 6) {
            if (dateStart.getDayOfWeek().getValue() == 7)
                dateStart = dateStart.plusDays(1L);
            else
                dateStart = dateStart.plusDays(2L);
        } else {
            while (dateStart.getDayOfWeek().getValue() != 1) {
                dateStart = dateStart.minusDays(1L);
            }
        }

        return (int) ((Math.abs(dateStart.toEpochDay() - date.toEpochDay()) / 7) % 4) + 1;
    }

    /**
     * Метод для получения значения ячейки занятости преподавателя.<br/>
     * Использовался для первого варианта экселя загруженности преподавателя.
     *
     * @param currentLesson Занятие {@link LessonDTO}, для которого нужно получить значение
     * @param timeLessons   Список занятий {@link LessonDTO}, которые ведёт преподаватель
     * @return Строку со всеми датами, в которые преподаватель ведёт занятия в определённый день и время
     * @see #excelTeacherWorkload
     * @see #dateToString
     */
    @Deprecated
    private String collectLessonDays(LessonDTO currentLesson, List<LessonDTO> timeLessons) {
        return timeLessons.stream()
                .filter(l -> l.getLessonNumber() == currentLesson.getLessonNumber() && l.getWeekType().equals(currentLesson.getWeekType()))
                .map(lesson -> lesson.getStartDate().equals(lesson.getEndDate()) ?
                        dateToString(lesson.getStartDate()) : (dateToString(lesson.getStartDate()) + "-" + dateToString(lesson.getEndDate())))
                .collect(Collectors.joining(", "));
    }

    /**
     * Конвертирует перечисление типов занятия в строку.
     *
     * @param lessonType Тип занятия
     * @return Строку со значением типа занятия в виде, пригодном для записи в эксель
     * @see #zaochDisciplineString
     * @see #excelDaytimeSession
     * @see #disciplineCellValue
     * @see #cellValueZaoch
     */
    private String convertLessonType(ELessonTypes lessonType) {
        return switch (lessonType) {
            case LECTURE -> "лк.";
            case PRACTICE -> "пр.";
            case LAB -> "лб.";
            case EXAM -> "Экзамен";
            case CONSULTATION -> "Консультация";
            case EXAM_REVIEW -> "Просмотр";
            case CONSULT_EXAM -> "Консультация. Экзамен";
            case SCORE -> "Зачёт";
            case COURSE_WORK_DEFENSE -> "Защита курсовой работы";
            case COURSE_PROJECT_DEFENSE -> "Защита курсового проекта";
            case DIFF_SCORE -> "Диф. зачёт";
            case PRACTICE_DEFENSE -> "Защита отчёта по практике";
            case SEMINAR -> "сем.";
            default -> "unknown";
        };
    }

    /**
     * Конвертирует перечисление типов недели в строку.
     *
     * @param weekType Тип недели
     * @return Строку со значением типа недели в виде, пригодном для записи в эксель
     * @see #disciplineCellValue
     * @see #citCellValue
     */
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

    /**
     * Конвертирует перечисление подгрупп в строку.
     *
     * @param subGroup Подгруппа
     * @return Строку со значением подгруппы в виде, пригодном для записи в эксель
     * @see #excelDaytime
     * @see #excelZaoch
     * @see #excelDaytimeSession
     */
    private String convertSubGroup(ESubGroup subGroup) {
        return switch (subGroup) {
            case SEWING -> "(шв.)";
            case SHOE -> "(об.)";
            case TEXTILE -> "(текст.)";
            case GRAPHIC -> "(граф.)";
            case MULTIMEDIA -> "(мульт.)";
            case WEAVERS -> "(тк.)";
            case KNITWEAR -> "(тр.)";
            default -> "";
        };
    }

    /**
     * Конвертирует номер дня недели в строку.
     *
     * @param day Номер дня недели (<b><i>от 1 до 7</i></b>)
     * @return Строку со значением дня недели в виде, пригодном для записи в эксель
     * @see #excelDaytimeSession
     * @see #excelTeacherWorkload
     */
    private String convertDayOfWeek(int day) {
        return switch (day) {
            case 1 -> "пн.";
            case 2 -> "вт.";
            case 3 -> "ср.";
            case 4 -> "чт.";
            case 5 -> "пт.";
            case 6 -> "сб.";
            case 7 -> "вс.";
            default -> "";
        };
    }

    /**
     * Конвертирует номер пары в строку с временем.
     *
     * @param i Номер пары (<b><i>от 1 до 7</i></b>)
     * @return Строку со значением времени
     * @see #excelDaytimeSession
     */
    private String convertLessonNumber(short i) {
        return switch (i) {
            case 1 -> "8:00";
            case 2 -> "9:50";
            case 3 -> "11:40";
            case 4 -> "14:00";
            case 5 -> "15:45";
            case 6 -> "17:30";
            case 7 -> "19:15";
            default -> "";
        };
    }

    /**
     * Конвертирует модель преподавателя {@link TeacherModel} в строку.
     *
     * @param teacher Модель преподавателя
     * @return Строку вида "{@code Фамилия И.О.}"
     * @see #excelTeacherWorkload
     * @see #sessionCellValue
     * @see #disciplineCellValue
     * @see #cellValueZaoch
     * @see #citCellValue
     */
    private String teacherFioString(TeacherModel teacher) {
        return teacher.getSurname() + " " + teacher.getName().split("")[0] + "." + teacher.getPatronymic().split("")[0] + ".";
    }
}
