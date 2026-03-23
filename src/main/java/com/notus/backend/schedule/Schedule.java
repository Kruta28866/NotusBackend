package com.notus.backend.schedule;

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
    private String teacher;
    private String type;
    private String room;
    private String color;
}
