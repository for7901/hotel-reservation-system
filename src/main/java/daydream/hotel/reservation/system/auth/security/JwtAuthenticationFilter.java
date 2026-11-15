package daydream.hotel.reservation.system.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 解析 {@code Authorization: Bearer <token>}，把登录态放进 SecurityContext。
 *
 * <p><b>零查库</b>：用户 id、手机号、角色都来自 Token 声明，每个请求不再 {@code select} 一次 {@code sys_user}； 只读一份 Redis
 * 版本号（{@link TokenVersionStore}）来支持"立即失效"。 改密码、禁用账号时版本号 +1，旧 Token 下一个请求就失效，不必等 24h 过期。
 *
 * <p>Token 非法（过期、签名错、格式乱）或已吊销时只记 debug 日志并放行， 由 Spring Security 的 {@code
 * RestAuthenticationEntryPoint} 统一返回 401， 不会把解析异常抛成 500。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenVersionStore tokenVersionStore;

    public JwtAuthenticationFilter(
            JwtTokenProvider jwtTokenProvider, TokenVersionStore tokenVersionStore) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.tokenVersionStore = tokenVersionStore;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        authenticate(request);
        filterChain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request) {
        String token = resolveToken(request);
        if (!StringUtils.hasText(token)) {
            return;
        }
        // 已经认证过就不再覆盖（兼容其他认证方式 / 测试里手动注入的上下文）
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            return;
        }

        Claims claims;
        try {
            claims = jwtTokenProvider.parseClaims(token);
        } catch (JwtException | IllegalArgumentException ex) {
            // 过期、签名不匹配、结构非法：视为未登录，交给 401 入口处理
            log.debug("JWT 解析失败，按未登录处理: {}", ex.getMessage());
            return;
        }

        LoginUser loginUser = toLoginUser(claims);
        if (loginUser == null || isRevoked(loginUser.getUserId(), claims)) {
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        loginUser, null, loginUser.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /** Token 里的 {@code ver} 与 Redis 中的当前版本不一致 → 已被吊销。存储不可用时放行。 */
    private boolean isRevoked(Long userId, Claims claims) {
        Long expectedVersion = tokenVersionStore.currentVersion(userId);
        if (expectedVersion == null) {
            return false;
        }
        if (jwtTokenProvider.getTokenVersion(claims) != expectedVersion) {
            log.debug("token 已被吊销: userId={}", userId);
            return true;
        }
        return false;
    }

    /** claims → 登录态。缺少必需声明（老 Token / 手工构造）时返回 {@code null}，按未登录处理。 */
    private LoginUser toLoginUser(Claims claims) {
        Long userId;
        try {
            userId = Long.valueOf(claims.getSubject());
        } catch (NumberFormatException ex) {
            log.debug("JWT 缺少合法 subject，按未登录处理");
            return null;
        }

        String phone = claims.get(JwtTokenProvider.CLAIM_PHONE, String.class);
        String role = claims.get(JwtTokenProvider.CLAIM_ROLE, String.class);
        if (!StringUtils.hasText(phone) || !StringUtils.hasText(role)) {
            log.debug("JWT 缺少 phone / role 声明，按未登录处理");
            return null;
        }
        return new LoginUser(userId, phone, role, true);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
