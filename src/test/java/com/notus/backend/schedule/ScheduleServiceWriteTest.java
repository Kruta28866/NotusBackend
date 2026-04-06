package com.notus.backend.schedule;

import com.notus.backend.attendance.group.StudentGroupRepository;
import com.notus.backend.users.Teacher;
import com.notus.backend.users.TeacherRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceWriteTest {

    @Mock private ScheduleRepository scheduleRepository;
    @Mock private TeacherRepository teacherRepository;
    @Mock private StudentGroupRepository studentGroupRepository;

    @InjectMocks private ScheduleService scheduleService;

    @Test
    void getById_returnsSchedule_whenFound() {
        Schedule s = new Schedule();
        s.setId("abc");
        when(scheduleRepository.findById("abc")).thenReturn(Optional.of(s));

        Schedule result = scheduleService.getById("abc");

        assertThat(result.getId()).isEqualTo("abc");
    }

    @Test
    void getById_throws404_whenNotFound() {
        when(scheduleRepository.findById("x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scheduleService.getById("x"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void createSchedule_savesAndReturnsSchedule() {
        Teacher teacher = new Teacher();
        teacher.setId(1L);
        teacher.setName("Jan Kowalski");
        when(teacherRepository.findByClerkUserId("uid1")).thenReturn(Optional.of(teacher));

        Schedule saved = new Schedule();
        saved.setId("new-id");
        when(scheduleRepository.save(any(Schedule.class))).thenReturn(saved);

        CreateScheduleRequest req = new CreateScheduleRequest(
                "Matematyka", Instant.now(), "08:00 - 09:30", "101", "Wykład", null, null
        );

        Schedule result = scheduleService.createSchedule(req, "uid1");

        assertThat(result.getId()).isEqualTo("new-id");
        verify(scheduleRepository).save(any(Schedule.class));
    }

    @Test
    void createSchedule_throws403_whenTeacherNotFound() {
        when(teacherRepository.findByClerkUserId("unknown")).thenReturn(Optional.empty());

        CreateScheduleRequest req = new CreateScheduleRequest(
                "Matematyka", Instant.now(), "08:00 - 09:30", "101", "Wykład", null, null
        );

        assertThatThrownBy(() -> scheduleService.createSchedule(req, "unknown"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void updateSchedule_updatesFields() {
        Schedule existing = new Schedule();
        existing.setId("id1");
        existing.setSubject("Old Subject");
        when(scheduleRepository.findById("id1")).thenReturn(Optional.of(existing));
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateScheduleRequest req = new CreateScheduleRequest(
                "New Subject", Instant.now(), "10:00 - 11:30", "202", "Ćwiczenia", null, null
        );

        Schedule result = scheduleService.updateSchedule("id1", req);

        assertThat(result.getSubject()).isEqualTo("New Subject");
        assertThat(result.getRoom()).isEqualTo("202");
    }

    @Test
    void updateSchedule_throws404_whenNotFound() {
        when(scheduleRepository.findById("x")).thenReturn(Optional.empty());

        CreateScheduleRequest req = new CreateScheduleRequest(
                "S", Instant.now(), "08:00 - 09:30", "1", "Wykład", null, null
        );

        assertThatThrownBy(() -> scheduleService.updateSchedule("x", req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void updateSchedule_preservesTeacherEntity() {
        Teacher teacher = new Teacher();
        teacher.setId(5L);
        teacher.setName("Original Teacher");

        Schedule existing = new Schedule();
        existing.setId("id2");
        existing.setSubject("Old");
        existing.setTeacherEntity(teacher);
        when(scheduleRepository.findById("id2")).thenReturn(Optional.of(existing));
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateScheduleRequest req = new CreateScheduleRequest(
                "New Subject", Instant.now(), "10:00 - 11:30", "202", "Ćwiczenia", null, null
        );

        Schedule result = scheduleService.updateSchedule("id2", req);

        assertThat(result.getTeacherEntity()).isSameAs(teacher);
    }

    @Test
    void deleteSchedule_deletesWhenFound() {
        when(scheduleRepository.existsById("id1")).thenReturn(true);

        scheduleService.deleteSchedule("id1");

        verify(scheduleRepository).deleteById("id1");
    }

    @Test
    void deleteSchedule_throws404_whenNotFound() {
        when(scheduleRepository.existsById("x")).thenReturn(false);

        assertThatThrownBy(() -> scheduleService.deleteSchedule("x"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }
}
