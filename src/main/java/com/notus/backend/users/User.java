package com.notus.backend.users;



import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Setter
@Getter
@Table(name = "users",
        indexes = {
                @Index(name = "idx_users_clerk_user_id", columnList = "clerkUserId", unique = true),
                @Index(name = "idx_users_email", columnList = "email", unique = true)
        })
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String clerkUserId;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private String indexNumber;

    @Column(name = "student_group")
    private String studentGroup;
}
