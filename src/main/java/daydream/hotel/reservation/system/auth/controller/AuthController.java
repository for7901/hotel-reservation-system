package daydream.hotel.reservation.system.auth.controller;

import daydream.hotel.reservation.system.auth.dto.LoginRequest;
import daydream.hotel.reservation.system.auth.dto.LoginResponse;
import daydream.hotel.reservation.system.auth.dto.RegisterRequest;
import daydream.hotel.reservation.system.auth.ratelimit.LoginRateLimiter;
import daydream.hotel.reservation.system.auth.service.AuthService;
import daydream.hotel.reservation.system.common.result.Result;
import daydream.hotel.reservation.system.common.util.ClientIpUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "认证")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final LoginRateLimiter loginRateLimiter;

    public AuthController(AuthService authService, LoginRateLimiter loginRateLimiter) {
        this.authService = authService;
        this.loginRateLimiter = loginRateLimiter;
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return Result.ok(authService.register(request));
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginResponse> login(
            @Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String rateKey = ClientIpUtils.resolve(httpRequest) + ":" + request.getPhone();
        loginRateLimiter.check(rateKey);
        return Result.ok(authService.login(request));
    }
}
