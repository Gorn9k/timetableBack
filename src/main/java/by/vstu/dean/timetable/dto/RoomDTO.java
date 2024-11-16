package by.vstu.dean.timetable.dto;

import by.vstu.dean.core.dto.BaseDTO;
import by.vstu.dean.timetable.enums.ESubGroup;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;

import java.time.LocalDate;

@Setter
@Getter
public class RoomDTO extends BaseDTO {

    @Nullable
    String roomNumber;
    @Nullable
    ESubGroup subGroup;
    @Nullable
    Short day;
    @Nullable
    Short lessonNumber;
    @Nullable
    String frame;
    @Nullable
    String roomType;
    @Nullable
    String lessonType;
    @Nullable
    String disciplineName;
    @Nullable
    String teacherFullName;
    @Nullable
    String weekType;
    @Nullable
    String group;
    @Nullable
    String stream;
    @Nullable
    Long roomId;
    @Nullable
    Long lessonId;
    @Nullable
    Long disciplineId;
    @Nullable
    Long teacherId;
    @Nullable
    Long groupId;
    @Nullable
    LocalDate startDate;
    @Nullable
    LocalDate endDate;
    @Nullable
    Boolean visible;
    @JsonIgnore
    @Nullable
    Long streamInternalId;
}