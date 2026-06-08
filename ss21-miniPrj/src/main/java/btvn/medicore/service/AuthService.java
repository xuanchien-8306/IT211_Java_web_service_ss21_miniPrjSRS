package btvn.medicore.service;

import btvn.medicore.dto.AuthRequest;
import btvn.medicore.dto.AuthResponse;
import btvn.medicore.entity.Token;
import btvn.medicore.entity.User;
import btvn.medicore.repository.TokenRepository;
import btvn.medicore.repository.UserRepository;
import btvn.medicore.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse login(AuthRequest request) {
        // 1. Xác thực thông tin tài khoản qua AuthenticationManager
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại trên hệ thống"));

        // 2. Sinh cặp token đa tầng (Access Token vòng đời ngắn & Refresh Token đăng nhập lâu dài)
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        // 3. Quét sạch và vô hiệu hóa toàn bộ các phiên làm việc cũ của User này
        revokeAllUserTokens(user);

        // 4. SỬA ĐỔI: Ghi nhận và lưu vết CẢ HAI token vào database để kiểm soát tính an toàn
        saveUserToken(user, accessToken, "ACCESS");
        saveUserToken(user, refreshToken, "REFRESH");

        log.info("Cấp quyền thành công: Tài khoản [{}] đã đăng nhập vào hệ thống MediCore.", user.getUsername());
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public AuthResponse refresh(String refreshToken) {
        String username = jwtService.extractUsername(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Người dùng không hợp lệ"));

        // Đảm bảo Refresh Token sử dụng để gia hạn tồn tại và chưa bị thu hồi
        Token tokenInDb = tokenRepository.findByTokenValue(refreshToken)
                .orElseThrow(() -> new RuntimeException("Phiên làm việc không tồn tại hoặc đã đăng xuất"));

        if (tokenInDb.isExpired() || tokenInDb.isRevoked()) {
            throw new RuntimeException("Phiên đăng nhập đã hết hiệu lực, vui lòng đăng nhập lại!");
        }

        // Cấp phát một chuỗi Access Token mới tinh để client tiếp tục tương tác nghiệp vụ
        String newAccessToken = jwtService.generateAccessToken(user);

        // Lưu Access Token mới này vào database để tiếp tục quản lý trạng thái hủy bỏ
        saveUserToken(user, newAccessToken, "ACCESS");

        log.info("Hệ thống tự động tái cấp Access Token mới thành công cho tài khoản [{}].", username);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken) // Duy trì sử dụng lại Refresh Token dài hạn cũ
                .build();
    }

    public void logout(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Yêu cầu không hợp lệ!");
        }
        String jwt = authHeader.substring(7);

        // SỬA ĐỔI: Khi thực hiện đăng xuất, tìm kiếm token tương ứng và chuyển trạng thái thu hồi
        Token token = tokenRepository.findByTokenValue(jwt).orElse(null);

        if (token != null) {
            // Thu hồi token hiện tại dùng để gọi API đăng xuất
            token.setExpired(true);
            token.setRevoked(true);
            tokenRepository.save(token);

            // Tìm và hủy bỏ luôn toàn bộ token đồng bộ (bao gồm cả refresh token) của phiên làm việc đó
            User user = token.getUser();
            if (user != null) {
                revokeAllUserTokens(user);
            }
            log.info("Đã xử lý đăng xuất an toàn. Toàn bộ phiên token liên quan đã bị thu hồi hiệu lực.");
        }
    }

    // SỬA ĐỔI: Hàm lưu trữ bổ sung thêm tham số phân loại cấu trúc token (ACCESS / REFRESH)
    private void saveUserToken(User user, String jwtToken, String tokenType) {
        Token token = Token.builder()
                .user(user)
                .tokenValue(jwtToken)
                .tokenType(tokenType)
                .expired(false)
                .revoked(false)
                .build();
        tokenRepository.save(token);
    }

    private void revokeAllUserTokens(User user) {
        List<Token> validUserTokens = tokenRepository.findAllValidTokenByUser(user.getId());
        if (validUserTokens.isEmpty()) return;
        validUserTokens.forEach(token -> {
            token.setExpired(true);
            token.setRevoked(true);
        });
        tokenRepository.saveAll(validUserTokens);
    }
}