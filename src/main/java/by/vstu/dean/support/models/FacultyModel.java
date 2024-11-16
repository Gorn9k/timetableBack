package by.vstu.dean.support.models;

import by.vstu.dean.core.adapters.LocalDateTypeAdapter;
import by.vstu.dean.core.models.DBBaseModel;
import com.google.gson.annotations.JsonAdapter;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FacultyModel extends DBBaseModel {

    private String shortName;
    private String name;
    private String nameGenitive;
    private String nameDative;
    private String rectorName;
    private String dean;
    private String clerkName;
    private String enrollMsgPaid;
    @JsonAdapter(LocalDateTypeAdapter.class)
    private LocalDate enrollDatePaid;
    private String enrollMsgNotPaid;
    @JsonAdapter(LocalDateTypeAdapter.class)
    private LocalDate enrollDateNotPaid;
    private Integer journalType;
    private Integer facultyType;
    private Integer semesterStartYear;
    private Integer semesterEndYear;
    private String semester;
    private String moveMsgNumber;
    @JsonAdapter(LocalDateTypeAdapter.class)
    private LocalDate moveMsgDate;
    @Deprecated
    private Integer educationType;
}
