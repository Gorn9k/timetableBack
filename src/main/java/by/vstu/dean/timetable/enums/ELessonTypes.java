package by.vstu.dean.timetable.enums;


import by.vstu.dean.core.enums.BaseEnum;

import java.util.Arrays;
import java.util.List;

public enum ELessonTypes implements BaseEnum<ELessonTypes> {

    LECTURE(0),
    PRACTICE(1),
    LAB(2),
    EXAM(3),
    CONSULTATION(4),
    SCORE(5),
    COURSE_WORK_DEFENSE(6),
    COURSE_PROJECT_DEFENSE(7),
    EXAM_REVIEW(8),
    CONSULT_EXAM(9),
    DIFF_SCORE(10),
    PRACTICE_DEFENSE(11),
    SEMINAR(12),

    UNKNOWN(-1);

    final int id;

    ELessonTypes(int id) {
        this.id = id;
    }

    public static ELessonTypes valueOf(int id) {
        return Arrays.stream(values()).filter(p -> p.id == id).findFirst().orElse(UNKNOWN);
    }

    @Override
    public List<ELessonTypes> getValues() {
        return Arrays.stream(ELessonTypes.values()).toList();
    }
}
