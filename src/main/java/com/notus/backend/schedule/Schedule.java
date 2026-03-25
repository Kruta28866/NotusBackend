package com.notus.backend.schedule;

import com.notus.backend.users.Teacher;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "schedule")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Schedule {
    @Id
    private String id;

    private Instant date;
    private String time;
    private String subject;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "teacher_id")
    private com.notus.backend.users.Teacher teacherEntity;

    @Transient
    public String getTeacher() {
        return teacherEntity != null ? teacherEntity.getName() : null;
    }

    private String type;
    private String room;
    private String color;
}
