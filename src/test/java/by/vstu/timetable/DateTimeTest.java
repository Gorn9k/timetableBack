package by.vstu.timetable;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateTimeTest {

    public static void main(String[] args) {

        System.out.println(LocalDateTime.now().toString());
        System.out.println("2023-10-12T15:48:20.758998");

        System.out.println(LocalDateTime.parse(LocalDateTime.now().toString()));
        System.out.println(LocalDateTime.parse("2023-10-12T15:48:20"));
        System.out.println(Instant.parse("2023-10-12T15:48:20"));

    }
}
