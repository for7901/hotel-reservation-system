package daydream.hotel.reservation.system.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import daydream.hotel.reservation.system.user.entity.User;
import daydream.hotel.reservation.system.user.enums.UserRole;
import daydream.hotel.reservation.system.user.enums.UserStatus;
import daydream.hotel.reservation.system.user.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
@Order(1)
public class DevAdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevAdminInitializer.class);

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public DevAdminInitializer(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        initUser("13800000000", "admin123", "系统管理员", UserRole.ADMIN);
    }

    private void initUser(String phone, String password, String nickname, UserRole role) {
        try {
            Long count =
                    userMapper.selectCount(
                            new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
            if (count > 0) {
                return;
            }
            User user = new User();
            user.setPhone(phone);
            user.setPassword(passwordEncoder.encode(password));
            user.setNickname(nickname);
            user.setRole(role.name());
            user.setStatus(UserStatus.ACTIVE.getValue());
            userMapper.insert(user);
            log.info(
                    "Initialized dev account: phone={}, password={}, role={}",
                    phone,
                    password,
                    role.name());
        } catch (Exception ex) {
            log.warn("Skip dev account initialization for {}: {}", phone, ex.getMessage());
        }
    }
}
