package btvn.medicore.controller;

import btvn.medicore.entity.Appointment;
import btvn.medicore.entity.Medicine;
import btvn.medicore.entity.Prescription;
import btvn.medicore.service.ClinicService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clinic")
@RequiredArgsConstructor
public class ClinicController {

    private final ClinicService clinicService;

    // API 1: Thêm thuốc mới - CHỈ ADMIN ĐƯỢC PHÉP
    @PostMapping("/medicines")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Medicine> addMedicine(@RequestBody Medicine medicine) {
        return ResponseEntity.ok(clinicService.createMedicine(medicine));
    }

    // API 2: Xem danh mục thuốc - TẤT CẢ TÀI KHOẢN ĐÃ ĐĂNG NHẬP ĐỀU XEM ĐƯỢC
    @GetMapping("/medicines")
    public ResponseEntity<List<Medicine>> getAllMedicines() {
        return ResponseEntity.ok(clinicService.getAllMedicines());
    }

    // API 3: Đặt lịch khám mới - CHỈ PATIENT ĐƯỢC PHÉP
    @PostMapping("/appointments")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<Appointment> bookAppointment(@RequestBody Map<String, Object> body) {
        Long patientId = Long.valueOf(body.get("patientId").toString());
        Long doctorId = Long.valueOf(body.get("doctorId").toString());
        LocalDateTime time = LocalDateTime.parse(body.get("appointmentTime").toString());
        String note = body.get("note") != null ? body.get("note").toString() : "";

        return ResponseEntity.ok(clinicService.bookAppointment(patientId, doctorId, time, note));
    }

    // API 4: Xem lịch khám trong ngày của Bác sĩ - CHỈ DOCTOR ĐƯỢC PHÉP CHÍNH MÌNH
    @GetMapping("/doctor/{doctorId}/today")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<List<Appointment>> getDoctorScheduleToday(@PathVariable Long doctorId) {
        return ResponseEntity.ok(clinicService.getDoctorAppointmentsToday(doctorId));
    }

    // API 5: Kê đơn thuốc cho bệnh nhân - CHỈ DOCTOR ĐƯỢC PHÉP
    @PostMapping("/prescriptions")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<Prescription> prescribeMedicine(@RequestBody Map<String, Object> body) {
        Long appointmentId = Long.valueOf(body.get("appointmentId").toString());
        Long medicineId = Long.valueOf(body.get("medicineId").toString());
        int quantity = Integer.parseInt(body.get("quantity").toString());
        String instruction = body.get("instruction").toString();

        return ResponseEntity.ok(clinicService.createPrescription(appointmentId, medicineId, quantity, instruction));
    }
}