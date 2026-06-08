package btvn.medicore.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "prescriptions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Prescription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment; // Đơn thuốc gắn liền với 1 buổi khám

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicine_id", nullable = false)
    private Medicine medicine; // Thuốc được kê

    private int quantity; // Số lượng thuốc
    private String dosageInstruction; // Hướng dẫn sử dụng (VD: Ngày uống 2 lần)
    private LocalDateTime createdAt;
}