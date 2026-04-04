package com.notus.backend.history.dto;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class TeacherHistoryItemDto {
    private String scheduleId;
    private String scheduleSubject;
    private Instant scheduleDate;
    private String scheduleTime;

    private Long sessionId;
    private int attendanceCount;

    private Long quizAssignmentId;
    private Long quizId;
    private String quizTitle;
    private int quizSubmissionCount;
    private int quizAvgScore;
}
