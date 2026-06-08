package btvn.medicore.security;

import btvn.medicore.repository.TokenRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final TokenRepository tokenRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        String path = request.getRequestURI();

        // Bỏ qua lọc với các API đăng nhập/refresh công khai
        if (path.contains("/api/auth/login") || path.contains("/api/auth/refresh")) {
            filterChain.doFilter(request, response);
            return;
        }

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);

        try {
            final String username = jwtService.extractUsername(jwt);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                // KIỂM TRA ĐIỀU KIỆN 1: Kiểm tra trạng thái lưu vết của token trong Database
                boolean isTokenValid = tokenRepository.findByTokenValue(jwt)
                        .map(t -> !t.isExpired() && !t.isRevoked())
                        .orElse(false);

                // SỬA ĐỔI: Nếu chữ ký cấu trúc đúng nhưng DB báo đã bị thu hồi/đăng xuất -> Chặn và phản hồi 403 lập tức
                if (!isTokenValid) {
                    log.warn("Cảnh báo an ninh: Token đã bị thu hồi hoặc đăng xuất trước đó!");
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"status\":403,\"message\":\"Token đã bị thu hồi hoặc đăng xuất khỏi hệ thống!\"}");
                    return;
                }

                // KIỂM TRA ĐIỀU KIỆN 2: Kiểm tra tính hợp lệ về mặt thời gian và chữ ký JWT
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            log.error("Lỗi xác thực cấu trúc hoặc thời hạn Token: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"status\":401,\"message\":\"Mã xác thực không hợp lệ hoặc đã hết hạn!\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}