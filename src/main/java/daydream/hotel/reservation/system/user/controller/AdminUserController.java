package daydream.hotel.reservation.system.user.controller;

import daydream.hotel.reservation.system.common.result.PageResult;
import daydream.hotel.reservation.system.common.result.Result;
import daydream.hotel.reservation.system.user.dto.AdminUserVO;
import daydream.hotel.reservation.system.user.dto.UpdateUserStatusRequest;
import daydream.hotel.reservation.system.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户-平台端")
@RestController
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "用户列表")
    @GetMapping
    public Result<PageResult<AdminUserVO>> list(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(userService.listUsers(role, status, keyword, page, size));
    }

    @Operation(summary = "更新用户状态")
    @PutMapping("/{id}/status")
    public Result<AdminUserVO> updateStatus(
            @PathVariable Long id, @Valid @RequestBody UpdateUserStatusRequest request) {
        return Result.ok(userService.updateUserStatus(id, request.getStatus()));
    }
}
