package btvn.medicore.service;

import btvn.medicore.entity.Appointment;
import btvn.medicore.entity.Medicine;
import btvn.medicore.entity.Prescription;
import btvn.medicore.entity.User;
import btvn.medicore.repository.AppointmentRepository;
import btvn.medicore.repository.MedicineRepository;
import btvn.medicore.repository.PrescriptionRepository;
import btvn.medicore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClinicService {

    private final UserRepository userRepository;
    private final MedicineRepository medicineRepository;
    private final AppointmentRepository appointmentRepository;
    private final PrescriptionRepository prescriptionRepository;

    // --- NGHIỆP VỤ DANH MỤC THUỐC (Chỉ ADMIN được quản lý) ---
    public Medicine createMedicine(Medicine medicine) {
        log.info("ADMIN thực hiện thêm thuốc mới: {}", medicine.getName());
        return medicineRepository.save(medicine);
    }

    public List<Medicine> getAllMedicines() {
        return medicineRepository.findAll();
    }

    // --- NGHIỆP VỤ ĐẶT LỊCH KHÁM (PATIENT thực hiện) ---
    public Appointment bookAppointment(Long patientId, Long doctorId, LocalDateTime time, String note) {
        User patient = userRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin bệnh nhân"));
        User doctor = userRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin bác sĩ"));

        Appointment appointment = Appointment.builder()
                .patient(patient)
                .doctor(doctor)
                .appointmentTime(time)
                .status("PENDING")
                .note(note)
                .build();

        log.info("Bệnh nhân [{}] đã đặt lịch khám với Bác sĩ [{}] vào lúc {}", patient.getUsername(), doctor.getUsername(), time);
        return appointmentRepository.save(appointment);
    }

    // --- NGHIỆP VỤ LỌC LỊCH KHÁM TRONG NGÀY (ÁP DỤNG STREAM API THEO SRS) ---
    public List<Appointment> getDoctorAppointmentsToday(Long doctorId) {
        List<Appointment> allAppointments = appointmentRepository.findByDoctorId(doctorId);
        LocalDate today = LocalDate.now();

        // Sử dụng Stream API để lọc danh sách lịch khám thuộc ngày hôm nay theo yêu cầu SRS
        return allAppointments.stream()
                .filter(app -> app.getAppointmentTime().toLocalDate().equals(today))
                .collect(Collectors.toList());
    }

    // --- NGHIỆP VỤ KÊ ĐƠN THUỐC (DOCTOR thực hiện) ---
    @Transactional
    public Prescription createPrescription(Long appointmentId, Long medicineId, int quantity, String instruction) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch khám"));
        Medicine medicine = medicineRepository.findById(medicineId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thuốc trong danh mục"));

        // Kiểm tra số lượng thuốc tồn kho theo nghiệp vụ thực tế
        if (medicine.getStockQuantity() < quantity) {
            throw new RuntimeException("Số lượng thuốc trong kho không đủ để kê đơn! Hiện còn: " + medicine.getStockQuantity());
        }

        // Trừ bớt số lượng thuốc trong kho
        medicine.setStockQuantity(medicine.getStockQuantity() - quantity);
        medicineRepository.save(medicine);

        Prescription prescription = Prescription.builder()
                .appointment(appointment)
                .medicine(medicine)
                .quantity(quantity)
                .dosageInstruction(instruction)
                .createdAt(LocalDateTime.now())
                .build();

        log.info("Bác sĩ [{}] đã kê đơn thuốc [{}] cho bệnh nhân [{}]",
                appointment.getDoctor().getUsername(), medicine.getName(), appointment.getPatient().getUsername());

        return prescriptionRepository.save(prescription);
    }
}