package com.notus.backend.users;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Setter
@Getter
@Table(name = "users",
        indexes = {
                @Index(name = "idx_users_firebase_uid", columnList = "firebaseUid", unique = true),
                @Index(name = "idx_users_email", columnList = "email", unique = true)
        })
public class User {

    // getters/setters (na razie ręcznie, później możesz dać Lomboka)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String firebaseUid;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private String indexNumber;

}
