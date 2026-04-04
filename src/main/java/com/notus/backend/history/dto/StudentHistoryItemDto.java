package com.notus.backend.history.dto;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class StudentHistoryItemDto {
    private String scheduleId;
    private String scheduleSubject;
    private Instant scheduleDate;
    private String scheduleTime;

    private boolean attended;
    private Instant checkedInAt;

    private String quizTitle;
    private Long quizAssignmentId;
    private Long quizId;
    private Long submissionId;
    private int score;
    private int total;
    private boolean pendingOpenReview;
}
