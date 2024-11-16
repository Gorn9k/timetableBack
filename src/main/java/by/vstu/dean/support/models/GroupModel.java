package by.vstu.dean.support.models;

import by.vstu.dean.core.adapters.LocalDateTypeAdapter;
import by.vstu.dean.core.models.DBBaseModel;
import by.vstu.dean.support.enums.ESemester;
import com.google.gson.annotations.JsonAdapter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GroupModel extends DBBaseModel {
    private String name;
    private SpecialityModel spec;
    private FacultyModel faculty;
    private Integer yearStart;
    private ESemester endSemester;
    private Integer yearEnd;
    @JsonAdapter(LocalDateTypeAdapter.class)
    private LocalDate dateStart;
    @JsonAdapter(LocalDateTypeAdapter.class)
    private LocalDate dateEnd;
    private Integer currentCourse;
    private Double score;
}
