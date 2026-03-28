package com.notus.backend.attendance;

import com.notus.backend.attendance.dto.CreateSessionRequest;
import com.notus.backend.attendance.dto.CreateSessionResponse;
import com.notus.backend.schedule.Schedule;
import com.notus.backend.schedule.ScheduleRepository;
import com.notus.backend.users.StudentRepository;
import com.notus.backend.users.Teacher;
import com.notus.backend.users.TeacherRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private AttendanceSessionRepository sessionRepo;

    @Mock
    private AttendanceRecordRepository recordRepo;

    @Mock
    private QrTokenService qrTokenService;

    @Mock
    private QrImageService qrImageService;

    @Mock
    private StudentRepository studentRepo;

    @Mock
    private TeacherRepository teacherRepo;

    @Mock
    private ScheduleRepository scheduleRepository;

    @InjectMocks
    private AttendanceService attendanceService;
    @Test
    void shouldCreateSession() {
        // given
        String teacherUid = "teacher-123";
        String scheduleId = "schedule-1";

        CreateSessionRequest request = new CreateSessionRequest(scheduleId);

        Teacher teacher = new Teacher();
        teacher.setId(10L);

        Schedule schedule = new Schedule();
        schedule.setId(scheduleId);
        schedule.setTeacherEntity(teacher);
        schedule.setSubject("Matematyka");
        schedule.setRoom("101");
        schedule.setTime("08:00");
        schedule.setDate(Instant.parse("2025-01-10T08:00:00Z"));

        AttendanceSession savedSession = new AttendanceSession();
        savedSession.setId(100L);
        savedSession.setTeacher(teacher);
        savedSession.setSchedule(schedule);
        savedSession.setCreatedAt(schedule.getDate());
        savedSession.setActive(true);
        savedSession.setShortCode("ABC123");

        when(teacherRepo.findByClerkUserId(teacherUid))
                .thenReturn(Optional.of(teacher));

        when(scheduleRepository.findById(scheduleId))
                .thenReturn(Optional.of(schedule));

        when(sessionRepo.save(any(AttendanceSession.class)))
                .thenReturn(savedSession);

        // when
        CreateSessionResponse result = attendanceService.createSession(teacherUid, request);

        // then
        assertNotNull(result);
        assertEquals(100L, result.sessionId());
        assertEquals(scheduleId, result.scheduleId());
        assertEquals("Matematyka", result.title());
        assertEquals("101", result.room());
        assertEquals("08:00", result.time());
        assertEquals(schedule.getDate(), result.createdAt());
        assertTrue(result.active());

        verify(teacherRepo).findByClerkUserId(teacherUid);
        verify(scheduleRepository).findById(scheduleId);
        verify(sessionRepo).save(any(AttendanceSession.class));
    }
}