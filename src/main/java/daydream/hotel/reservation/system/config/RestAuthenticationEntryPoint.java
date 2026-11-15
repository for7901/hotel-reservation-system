package daydream.hotel.reservation.system.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import daydream.hotel.reservation.system.common.exception.ErrorCode;
import daydream.hotel.reservation.system.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/** 未登录访问受保护接口时的入口：返回统一 JSON（401），而非默认 HTML 登录页。 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException)
            throws IOException {
        writeError(response, ErrorCode.UNAUTHORIZED);
    }

    static void writeError(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter()
                .write(
                        OBJECT_MAPPER.writeValueAsString(
                                Result.fail(errorCode.getCode(), errorCode.getMessage())));
    }
}

/** 已登录但权限不足时返回统一 JSON（403）。 */
@Component
class RestAccessDeniedHandler implements AccessDeniedHandler {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter()
                .write(
                        OBJECT_MAPPER.writeValueAsString(
                                Result.fail(
                                        ErrorCode.FORBIDDEN.getCode(),
                                        ErrorCode.FORBIDDEN.getMessage())));
    }
}
