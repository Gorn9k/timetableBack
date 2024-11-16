package by.vstu.timetable;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateTimeTest {

    public static void main(String[] args) {

        LocalDate dateStart = LocalDate.of(2024, 9, 1);
        LocalDate dateFrom = LocalDate.of(2024, 10, 5);

        System.out.println(dateFrom.minusYears(dateStart.getYear()).minusMonths(dateStart.getMonthValue()));

    }
}
