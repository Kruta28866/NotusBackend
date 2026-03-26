package com.notus.backend.users;

import com.notus.backend.attendance.group.StudentGroup;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "students",
        indexes = {
                @Index(name = "idx_students_clerk_user_id", columnList = "clerk_user_id", unique = true),
                @Index(name = "idx_students_email", columnList = "email", unique = true)
        })
@Getter
@Setter
@NoArgsConstructor
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "clerk_user_id", nullable = false, unique = true)
    private String clerkUserId;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.STUDENT;

    @Column(name = "index_number")
    private String indexNumber;

    @ManyToMany
    @JoinTable(
            name = "student_groups_assignments",
            joinColumns = @JoinColumn(name = "student_id"),
            inverseJoinColumns = @JoinColumn(name = "group_id")
    )
    private List<StudentGroup> studentGroups;


}
