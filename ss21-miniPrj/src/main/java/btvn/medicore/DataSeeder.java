package btvn.medicore;

import btvn.medicore.entity.Role;
import btvn.medicore.entity.User;
import btvn.medicore.repository.RoleRepository;
import btvn.medicore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) return;

        // 1. Khởi tạo 3 vai trò hệ thống y tế công khám
        Role adminRole = roleRepository.save(new Role(null, "ROLE_ADMIN"));
        Role doctorRole = roleRepository.save(new Role(null, "ROLE_DOCTOR"));
        Role patientRole = roleRepository.save(new Role(null, "ROLE_PATIENT"));

        // 2. Tạo tài khoản Quản trị viên
        User admin = User.builder()
                .username("admin")
                .password(passwordEncoder.encode("123456"))
                .fullName("Nguyễn Quản Trị")
                .active(true)
                .roles(List.of(adminRole))
                .build();

        // 3. Tạo tài khoản Bác sĩ
        User doctor = User.builder()
                .username("doctor")
                .password(passwordEncoder.encode("123456"))
                .fullName("Bác sĩ Phong Đào")
                .active(true)
                .roles(List.of(doctorRole))
                .build();

        // 4. Tạo tài khoản Bệnh nhân (Thành)
        User patient = User.builder()
                .username("thanh")
                .password(passwordEncoder.encode("123456"))
                .fullName("Nguyễn Tiến Thành")
                .active(true)
                .roles(List.of(patientRole))
                .build();

        userRepository.saveAll(List.of(admin, doctor, patient));

        System.out.println("=============================================================");
        System.out.println("HỆ THỐNG MEDICORE API ĐÃ TỰ ĐỘNG BƠM TÀI KHOẢN MẪU:");
        System.out.println("Tài khoản ADMIN  : admin  | pass: 123456");
        System.out.println("Tài khoản DOCTOR : doctor | pass: 123456");
        System.out.println("Tài khoản PATIENT: thanh  | pass: 123456");
        System.out.println("=============================================================");
    }
}