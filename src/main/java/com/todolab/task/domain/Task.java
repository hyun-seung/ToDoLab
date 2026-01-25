package com.todolab.task.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "task")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;

    /***
     * 미정 : startAt == null && endAt == null
     * 단일 : startAt != null && endAt == null
     * 기간 : startAT != null && endAt != null
     */
    private LocalDateTime startAt;
    private LocalDateTime endAt;

    /***
     * 종일 일정 여부
     *  - true 면 시간 입력 개념이 없으며, startAt/endAt은 00:00으로 정규화되어야 함
     */
    private boolean allDay;

    private String category;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Builder
    public Task(String title, String description, LocalDateTime startAt, LocalDateTime endAt, boolean allDay, String category) {
        apply(title, description, startAt, endAt, allDay, category);
    }

    public void update(String title, String description, LocalDateTime startAt, LocalDateTime endAt, boolean allDay, String category) {
        apply(title, description, startAt, endAt, allDay, category);
    }

    public boolean isUnscheduled() {
        return startAt == null;
    }

    public boolean isPeriodTask() {
        return startAt != null && endAt != null;
    }

    private void apply(String title, String description, LocalDateTime startAt, LocalDateTime endAt, boolean allDay, String category) {
        validateSchedule(startAt, endAt, allDay);

        this.title = title;
        this.description = description;
        this.startAt = startAt;
        this.endAt = endAt;
        this.allDay = allDay;
        this.category = category;
    }

    private void validateSchedule(LocalDateTime startAt, LocalDateTime endAt, boolean allDay) {
        // 미정은 (startAt, endAt) 모두 null 만 허용
        if (startAt == null || endAt == null) {
            if (startAt != null && endAt == null) { // 단일 일정 (allDay 는 true/false 모두 가능하지만 true 면 00:00이어야 함)
                if (allDay && !isMidnight(startAt)) {
                    throw new IllegalArgumentException("allDay 일정의 startAt은 00:00 이어야 합니다.");
                }
                return;
            }
            if (startAt == null && endAt == null) { // 미정: allDay 는 의미 없으니 true 금지
                if (allDay) {
                    throw new IllegalArgumentException("미정 일정에는 allDay 를 설정할 수 없습니다.");
                }
                return;
            }
            // endAt만 있는 경우는 금지
            throw new IllegalArgumentException("endAt이 존재하면 startAt은 필수입니다.");
        }

        // 기간 일정
        if (endAt.isBefore(startAt) || endAt.isEqual(startAt)) {
            // [startAt, endAt) 해석을 전제로 endAt == startAt도 금지 (0분/0일 구간 방지)
            throw new IllegalArgumentException("endAt은 startAt 이후여야 합니다.");
        }

        if (allDay) {
            // 종일 일정은 자정 정규화가 전제
            if (!isMidnight(startAt) || !isMidnight(endAt)) {
                throw new IllegalArgumentException("allDay 일정의 startAt/endAt은 00:00 이어야 합니다.");
            }
        }
    }

    private boolean isMidnight(LocalDateTime dt) {
        return dt.toLocalTime().equals(LocalTime.MIDNIGHT);
    }

}
