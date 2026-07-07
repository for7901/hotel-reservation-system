package daydream.hotel.reservation.system.user.controller;

import daydream.hotel.reservation.system.common.result.Result;
import daydream.hotel.reservation.system.user.dto.ChangePasswordRequest;
import daydream.hotel.reservation.system.user.dto.UpdateProfileRequest;
import daydream.hotel.reservation.system.user.dto.UserProfileVO;
import daydream.hotel.reservation.system.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户")
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "获取当前用户资料")
    @GetMapping("/me")
    public Result<UserProfileVO> getProfile() {
        return Result.ok(userService.getCurrentProfile());
    }

    @Operation(summary = "更新当前用户资料")
    @PutMapping("/me")
    public Result<UserProfileVO> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return Result.ok(userService.updateProfile(request));
    }

    @Operation(summary = "修改密码")
    @PutMapping("/me/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
        return Result.ok();
    }
}
