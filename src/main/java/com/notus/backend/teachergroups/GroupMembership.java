package com.notus.backend.teachergroups;

import com.notus.backend.users.Student;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "group_memberships",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_group_memberships_group_student", columnNames = {"group_id", "student_id"})
        },
        indexes = {
                @Index(name = "idx_group_memberships_group_id", columnList = "group_id"),
                @Index(name = "idx_group_memberships_student_id", columnList = "student_id")
        })
@Getter
@Setter
@NoArgsConstructor
public class GroupMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private TeacherGroup group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "display_name_override")
    private String displayNameOverride;

    @Column(name = "email_override")
    private String emailOverride;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GroupMembershipStatus status = GroupMembershipStatus.ACTIVE;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @Column(name = "removed_at")
    private Instant removedAt;

    @PrePersist
    void prePersist() {
        if (joinedAt == null) {
            joinedAt = Instant.now();
        }
    }
}
