package com.todolab.task.domain.query;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class DateRangeTest {

    @Test
    @DisplayName("DAY 계산 - 시작과 끝이 동일하다")
    void calculate_DAY() {
        DateRange range = DateRange.ofDay("2025-11-27");

        assertThat(range.getStart()).isEqualTo(LocalDate.of(2025, 11, 27));
        assertThat(range.getEnd()).isEqualTo(LocalDate.of(2025, 11, 27));
    }

    @Test
    @DisplayName("WEEK 계산 - 시작은 월요일, 끝은 일요일이다")
    void calculate_WEEK() {
        DateRange range = DateRange.ofWeek("2025-11-27");

        assertThat(range.getStart()).isEqualTo(LocalDate.of(2025, 11, 24));
        assertThat(range.getEnd()).isEqualTo(LocalDate.of(2025, 11, 30));
    }

    @Test
    @DisplayName("MONTH 계산 - 월의 시작은 1일, 끝은 월말이다")
    void calculate_MONTH() {
        DateRange range = DateRange.ofMonth("2025-11");

        assertThat(range.getStart()).isEqualTo(LocalDate.of(2025, 11, 1));
        assertThat(range.getEnd()).isEqualTo(LocalDate.of(2025, 11, 30));
    }

}