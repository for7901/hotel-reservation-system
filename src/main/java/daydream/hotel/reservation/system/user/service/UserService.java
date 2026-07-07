package daydream.hotel.reservation.system.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import daydream.hotel.reservation.system.audit.service.AuditLogService;
import daydream.hotel.reservation.system.auth.security.SecurityUtils;
import daydream.hotel.reservation.system.common.exception.BusinessException;
import daydream.hotel.reservation.system.common.exception.ErrorCode;
import daydream.hotel.reservation.system.common.result.PageResult;
import daydream.hotel.reservation.system.common.util.SensitiveUtils;
import daydream.hotel.reservation.system.user.dto.AdminUserVO;
import daydream.hotel.reservation.system.user.dto.ChangePasswordRequest;
import daydream.hotel.reservation.system.user.dto.UpdateProfileRequest;
import daydream.hotel.reservation.system.user.dto.UserProfileVO;
import daydream.hotel.reservation.system.user.entity.User;
import daydream.hotel.reservation.system.user.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public UserService(
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            AuditLogService auditLogService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    public UserProfileVO getCurrentProfile() {
        return toProfileVO(getCurrentUser());
    }

    @Transactional
    public UserProfileVO updateProfile(UpdateProfileRequest request) {
        User user = getCurrentUser();
        if (StringUtils.hasText(request.getNickname())) {
            user.setNickname(request.getNickname());
        }
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }
        userMapper.updateById(user);
        return toProfileVO(user);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User user = getCurrentUser();
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);
    }

    public PageResult<AdminUserVO> listUsers(
            String role, Integer status, String keyword, long page, long size) {
        Page<User> userPage =
                userMapper.selectPage(
                        new Page<>(page, size),
                        new LambdaQueryWrapper<User>()
                                .eq(StringUtils.hasText(role), User::getRole, role)
                                .eq(status != null, User::getStatus, status)
                                .and(
                                        StringUtils.hasText(keyword),
                                        w ->
                                                w.like(User::getPhone, keyword)
                                                        .or()
                                                        .like(User::getNickname, keyword))
                                .orderByDesc(User::getCreatedAt));
        return new PageResult<>(
                userPage.getRecords().stream().map(this::toAdminUserVO).toList(),
                userPage.getTotal(),
                userPage.getCurrent(),
                userPage.getSize());
    }

    @Transactional
    public AdminUserVO updateUserStatus(Long userId, Integer status) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        user.setStatus(status);
        userMapper.updateById(user);
        auditLogService.record(
                "USER_STATUS",
                "USER",
                user.getId(),
                (status == 1 ? "启用" : "禁用") + "用户 " + user.getPhone());
        return toAdminUserVO(user);
    }

    private User getCurrentUser() {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return user;
    }

    private UserProfileVO toProfileVO(User user) {
        UserProfileVO vo = new UserProfileVO();
        vo.setId(user.getId());
        vo.setPhone(SensitiveUtils.maskPhone(user.getPhone()));
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        vo.setCreatedAt(user.getCreatedAt());
        return vo;
    }

    private AdminUserVO toAdminUserVO(User user) {
        AdminUserVO vo = new AdminUserVO();
        vo.setId(user.getId());
        vo.setPhone(SensitiveUtils.maskPhone(user.getPhone()));
        vo.setNickname(user.getNickname());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        vo.setCreatedAt(user.getCreatedAt());
        return vo;
    }
}
