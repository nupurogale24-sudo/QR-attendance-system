package com.example.qrattendance.controller;

import com.example.qrattendance.model.Attendance;
import com.example.qrattendance.model.AttendanceSession;
import com.example.qrattendance.repository.AttendanceRepository;
import com.example.qrattendance.repository.AttendanceSessionRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

@Controller
public class AttendanceController {

    private final AttendanceRepository attendanceRepository;
    private final AttendanceSessionRepository attendanceSessionRepository;

    public AttendanceController(AttendanceRepository attendanceRepository,
                                AttendanceSessionRepository attendanceSessionRepository) {
        this.attendanceRepository = attendanceRepository;
        this.attendanceSessionRepository = attendanceSessionRepository;
    }

    // Legacy default route (temporary backward compatibility)
    @GetMapping("/attendance")
    public String showAttendanceForm() {
        return "attendance";
    }

    // Legacy default POST route
    @PostMapping("/attendance")
    public String markAttendance(@RequestParam String studentId,
                                 @RequestParam String studentName,
                                 Model model) {
        processAttendance(studentId, studentName, null, model);
        return "attendance";
    }

    // New session-specific GET route: /attendance/{sessionId}
    @GetMapping("/attendance/{sessionId}")
    public String showSessionAttendanceForm(@PathVariable Long sessionId, Model model) {
        Optional<AttendanceSession> sessionOpt = attendanceSessionRepository.findById(sessionId);

        if (sessionOpt.isEmpty()) {
            model.addAttribute("sessionError", "Attendance session not found.");
            return "attendance";
        }

        AttendanceSession session = sessionOpt.get();
        if (!session.isActive()) {
            model.addAttribute("sessionError", "This attendance session is closed.");
            model.addAttribute("attendanceSession", session);
            return "attendance";
        }

        model.addAttribute("attendanceSession", session);
        return "attendance";
    }

    // New session-specific POST route: /attendance/{sessionId}
    @PostMapping("/attendance/{sessionId}")
    public String markSessionAttendance(@PathVariable Long sessionId,
                                        @RequestParam String studentId,
                                        @RequestParam String studentName,
                                        Model model) {
        Optional<AttendanceSession> sessionOpt = attendanceSessionRepository.findById(sessionId);

        if (sessionOpt.isEmpty()) {
            model.addAttribute("sessionError", "Attendance session not found.");
            return "attendance";
        }

        AttendanceSession session = sessionOpt.get();
        if (!session.isActive()) {
            model.addAttribute("sessionError", "This attendance session is closed.");
            model.addAttribute("attendanceSession", session);
            return "attendance";
        }

        model.addAttribute("attendanceSession", session);
        processAttendance(studentId, studentName, session, model);
        return "attendance";
    }

    private void processAttendance(String studentId, String studentName, AttendanceSession session, Model model) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        boolean alreadyMarked = attendanceRepository.existsByStudentIdAndAttendanceTimeBetween(studentId, startOfDay, endOfDay);

        if (alreadyMarked) {
            model.addAttribute("message", "Attendance already marked for today.");
            model.addAttribute("isError", true);
        } else {
            Attendance attendance = new Attendance();
            attendance.setStudentId(studentId);
            attendance.setStudentName(studentName);
            attendance.setAttendanceTime(LocalDateTime.now());
            attendance.setSession(session);

            attendanceRepository.save(attendance);

            model.addAttribute("message", "Attendance marked successfully!");
            model.addAttribute("isError", false);
        }
    }
}
