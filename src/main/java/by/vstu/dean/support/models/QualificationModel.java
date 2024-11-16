package by.vstu.dean.support.models;

import by.vstu.dean.core.models.DBBaseModel;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class QualificationModel extends DBBaseModel {

    @NotNull
    private String name;

    @NotNull
    private String nameGenitive;

}
