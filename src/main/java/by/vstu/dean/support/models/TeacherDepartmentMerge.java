package by.vstu.dean.support.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TeacherDepartmentMerge {

    private TeacherModel teacher;
    @JsonBackReference
    private DepartmentModel department;

}
