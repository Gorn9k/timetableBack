package by.vstu.dean.support.models;

import by.vstu.dean.core.models.DBBaseModel;
import by.vstu.dean.support.enums.EClassroomType;
import by.vstu.dean.support.enums.EFrame;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ClassroomModel extends DBBaseModel {
    @Enumerated(EnumType.ORDINAL)
    private EFrame frame;
    @Enumerated(EnumType.ORDINAL)
    private EClassroomType roomType;
    private DepartmentModel department;
    private String roomNumber;
    private Integer seatsNumber;
    private Double square;
}
