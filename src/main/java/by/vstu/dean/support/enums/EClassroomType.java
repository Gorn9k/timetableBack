package by.vstu.dean.support.enums;


import by.vstu.dean.core.enums.BaseEnum;

import java.util.Arrays;
import java.util.List;

public enum EClassroomType implements BaseEnum<EClassroomType> {
    LECTURE(0, ""),
    LAB(1, ""),
    SPEC_LAB(2, ""),
    COMPUTER_CLASS(3, ""),
    PICTURE(4, ""),
    PICTURE_ART(5, ""),
    ART(6, ""),
    EMPTY(7, ""),
    STUDY_CLASS(8, ""),
    GYM(9, ""),
    UNKNOWN(-1, "UNKNOWN");
    final int id;

    final String name;

    public String getName() {
        return this.name;
    }

    EClassroomType(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public static EClassroomType byName(String name) {
        return Arrays.<EClassroomType>stream(values()).filter(p -> p.name.equalsIgnoreCase(name)).findFirst().orElse(UNKNOWN);
    }

    public List<EClassroomType> getValues() {
        return Arrays.<EClassroomType>stream(values()).toList();
    }
}
