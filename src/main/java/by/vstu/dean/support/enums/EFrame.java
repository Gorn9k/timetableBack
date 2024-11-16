package by.vstu.dean.support.enums;

import by.vstu.dean.core.enums.BaseEnum;

import java.util.Arrays;
import java.util.List;

public enum EFrame implements BaseEnum<EFrame> {
    FIRST(1),
    SECOND(2),
    FOURTH(4),
    FIFTH(5),
    UNKNOWN(-1);

    final int id;

    public int getId() {
        return this.id;
    }

    EFrame(int id) {
        this.id = id;
    }

    public List<EFrame> getValues() {
        return Arrays.<EFrame>stream(values()).toList();
    }
}
