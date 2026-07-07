package daydream.hotel.reservation.system.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import daydream.hotel.reservation.system.auth.dto.LoginRequest;
import daydream.hotel.reservation.system.auth.dto.LoginResponse;
import daydream.hotel.reservation.system.auth.dto.RegisterRequest;
import daydream.hotel.reservation.system.auth.security.JwtTokenProvider;
import daydream.hotel.reservation.system.common.exception.BusinessException;
import daydream.hotel.reservation.system.common.exception.ErrorCode;
import daydream.hotel.reservation.system.common.util.SensitiveUtils;
import daydream.hotel.reservation.system.user.entity.User;
import daydream.hotel.reservation.system.user.enums.UserRole;
import daydream.hotel.reservation.system.user.enums.UserStatus;
import daydream.hotel.reservation.system.user.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public LoginResponse register(RegisterRequest request) {
        Long count =
                userMapper.selectCount(
                        new LambdaQueryWrapper<User>().eq(User::getPhone, request.getPhone()));
        if (count > 0) {
            throw new BusinessException(ErrorCode.DUPLICATE_PHONE);
        }

        User user = new User();
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(
                StringUtils.hasText(request.getNickname())
                        ? request.getNickname()
                        : "用户" + request.getPhone().substring(7));
        user.setRole(UserRole.USER.name());
        user.setStatus(UserStatus.ACTIVE.getValue());
        userMapper.insert(user);

        return buildLoginResponse(user);
    }

    public LoginResponse login(LoginRequest request) {
        User user =
                userMapper.selectOne(
                        new LambdaQueryWrapper<User>().eq(User::getPhone, request.getPhone()));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        if (user.getStatus() != UserStatus.ACTIVE.getValue()) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }
        return buildLoginResponse(user);
    }

    private LoginResponse buildLoginResponse(User user) {
        LoginResponse response = new LoginResponse();
        response.setToken(
                jwtTokenProvider.generateToken(user.getId(), user.getPhone(), user.getRole()));
        response.setUserId(user.getId());
        response.setPhone(SensitiveUtils.maskPhone(user.getPhone()));
        response.setNickname(user.getNickname());
        response.setRole(user.getRole());
        return response;
    }
}
