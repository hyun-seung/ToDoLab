package com.todolab.task.domain.query;

import lombok.Getter;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;

@Getter
public class DateRange {
    private final LocalDate start;
    private final LocalDate end;

    private DateRange(LocalDate start, LocalDate end) {
        this.start = start;
        this.end = end;
    }

    public static DateRange ofDay(String date) {
        LocalDate d = LocalDate.parse(date);
        return new DateRange(d, d);
    }

    public static DateRange ofWeek(String date) {
        LocalDate d = LocalDate.parse(date);
        LocalDate start = d.with(DayOfWeek.MONDAY);
        LocalDate end = d.with(DayOfWeek.SUNDAY);
        return new DateRange(start, end);
    }

    public static DateRange ofMonth(String date) {
        YearMonth ym = YearMonth.parse(date);
        return new DateRange(ym.atDay(1), ym.atEndOfMonth());
    }
}
