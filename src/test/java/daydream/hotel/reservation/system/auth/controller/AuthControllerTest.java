package daydream.hotel.reservation.system.auth.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import daydream.hotel.reservation.system.auth.dto.LoginRequest;
import daydream.hotel.reservation.system.auth.dto.RegisterRequest;
import daydream.hotel.reservation.system.auth.security.JwtProperties;
import daydream.hotel.reservation.system.auth.security.JwtTokenProvider;
import daydream.hotel.reservation.system.user.dto.ChangePasswordRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    private static final String AUTHORIZATION = "Authorization";

    @Autowired private MockMvc mockMvc;

    @Autowired private JwtProperties jwtProperties;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void registerAndLogin() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setPhone("13900001111");
        registerRequest.setPassword("123456");
        registerRequest.setNickname("测试用户");

        mockMvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.phone").value("139****1111"));

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setPhone("13900001111");
        loginRequest.setPassword("123456");

        mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    @Test
    void protectedEndpointRequiresAuth() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    /** Token 非法（结构错误）→ 401，而不是解析异常冒成 500。 */
    @Test
    void malformedTokenIsRejected() throws Exception {
        mockMvc.perform(get("/users/me").header(AUTHORIZATION, "Bearer not-a-valid-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    /** Token 过期 → 401（覆盖 JwtException 分支）。 */
    @Test
    void expiredTokenIsRejected() throws Exception {
        JwtProperties expiredProperties = new JwtProperties();
        expiredProperties.setSecret(jwtProperties.getSecret());
        expiredProperties.setExpiration(-60_000L);
        String expiredToken =
                new JwtTokenProvider(expiredProperties)
                        .generateToken(1L, "13800000000", "ADMIN", 0L);

        mockMvc.perform(get("/users/me").header(AUTHORIZATION, "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    /** 合法 Token 直接构建登录态，能访问需登录接口。 */
    @Test
    void validTokenAuthenticates() throws Exception {
        String token = registerAndGetToken("13900002222");

        mockMvc.perform(get("/users/me").header(AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.phone").value("139****2222"));
    }

    /** 改密码后版本号 +1：旧 Token 下一个请求就 401，新密码可重新登录。 */
    @Test
    void changePasswordRevokesExistingToken() throws Exception {
        String token = registerAndGetToken("13900003333");

        ChangePasswordRequest changePassword = new ChangePasswordRequest();
        changePassword.setOldPassword("123456");
        changePassword.setNewPassword("654321");

        mockMvc.perform(
                        put("/users/me/password")
                                .header(AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(changePassword)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // 旧 Token 立即失效，无需等 24h 过期
        mockMvc.perform(get("/users/me").header(AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        // 新密码能重新登录，拿到的新 Token 版本号与服务端一致
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setPhone("13900003333");
        loginRequest.setPassword("654321");

        mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    private String registerAndGetToken(String phone) throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setPhone(phone);
        registerRequest.setPassword("123456");
        registerRequest.setNickname("测试用户");

        String body =
                mockMvc.perform(
                                post("/auth/register")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(registerRequest)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.token").isNotEmpty())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        return objectMapper.readTree(body).path("data").path("token").asText();
    }
}
