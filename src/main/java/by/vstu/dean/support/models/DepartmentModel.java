package by.vstu.dean.support.models;

import by.vstu.dean.core.models.DBBaseModel;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DepartmentModel extends DBBaseModel {

    @NotNull
    private String name;

    @NotNull
    private String shortName;

    @NotNull
    private String room;

    private FacultyModel faculty;


    @JsonManagedReference
    private Set<TeacherModel> teachers;

}
