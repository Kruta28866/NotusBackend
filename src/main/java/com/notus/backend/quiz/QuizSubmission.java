package com.notus.backend.quiz;

import com.notus.backend.users.Student;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "quiz_submissions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"assignment_id", "student_id"}))
@Getter
@Setter
@NoArgsConstructor
public class QuizSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private QuizAssignment assignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(nullable = false)
    private int score;

    @Column(nullable = false)
    private int total;

    /** True while there are OPEN questions not yet graded by the teacher */
    @Column(name = "pending_open_review", nullable = false)
    private boolean pendingOpenReview = false;

    /** Set when teacher finishes reviewing all OPEN answers */
    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    /** True once the student has acknowledged the review notification */
    @Column(name = "notification_seen", nullable = false)
    private boolean notificationSeen = false;
}
