package com.notus.backend.attendance;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Entity
@Table(name = "attendance_record",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_att_record_session_student", columnNames = {"session_id", "student_uid"})
        },
        indexes = {
                @Index(name = "idx_att_record_session", columnList = "session_id"),
                @Index(name = "idx_att_record_student", columnList = "student_uid")
        })
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Setter
    @Column(name = "student_uid", nullable = false)
    private String studentUid;

    @Setter
    @Column(name = "checked_in_at", nullable = false)
    private Instant checkedInAt;

    @PrePersist
    void prePersist() {
        if (checkedInAt == null) checkedInAt = Instant.now();
    }

}
