package by.vstu.dean.support.models;

import by.vstu.dean.core.models.DBBaseModel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SpecializationModel extends DBBaseModel {

    private String name;
    private String shortName;
    private String spezCode;
    private SpecialityModel spec;
    private QualificationModel qualification;

}
