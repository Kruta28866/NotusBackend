package com.notus.backend.quiz;

import com.notus.backend.users.Teacher;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "quiz_assignments",
        uniqueConstraints = @UniqueConstraint(columnNames = {"quiz_id", "schedule_id"}))
@Getter
@Setter
@NoArgsConstructor
public class QuizAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(name = "schedule_id", nullable = false)
    private String scheduleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @Column(name = "assigned_at")
    private Instant assignedAt = Instant.now();

    @Column(nullable = false)
    private boolean active = false;

    /** ID of the AttendanceSession this quiz is currently pushed to (null = not activated) */
    @Column(name = "session_id")
    private Long sessionId;
}
