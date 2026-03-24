package com.notus.backend.attendance;

import com.notus.backend.users.Student;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Entity
@Table(name = "attendance_record",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_att_record_session_student", columnNames = {"session_id", "student_id"})
        },
        indexes = {
                @Index(name = "idx_att_record_session", columnList = "session_id"),
                @Index(name = "idx_att_record_student", columnList = "student_id")
        })
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Setter
    @Column(name = "checked_in_at", nullable = false)
    private Instant checkedInAt;

    @PrePersist
    void prePersist() {
        if (checkedInAt == null) checkedInAt = Instant.now();
    }

}
