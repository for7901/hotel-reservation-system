package daydream.hotel.reservation.system.auth.security;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * 登录态主体，完全由 JWT 中的声明构建，不再回查数据库。
 *
 * <p>因为身份信息直接来自 Token，这里<b>不保存密码</b>：{@link #getPassword()} 恒为 {@code null}。 从源头杜绝密码哈希随 {@code
 * Authentication#toString()} 被写进日志。
 */
public class LoginUser implements UserDetails {

    private final Long userId;
    private final String phone;
    private final String role;
    private final boolean enabled;

    public LoginUser(Long userId, String phone, String role, boolean enabled) {
        this.userId = userId;
        this.phone = phone;
        this.role = role;
        this.enabled = enabled;
    }

    public Long getUserId() {
        return userId;
    }

    public String getPhone() {
        return phone;
    }

    public String getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    /**
     * JWT 鉴权不依赖密码比对，恒返回 {@code null}。
     *
     * <p>需要校验密码的场景（登录、修改密码）都从数据库读取最新密码， 见 {@code AuthService#login}、 {@code
     * UserService#changePassword}。
     */
    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return phone;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /** 只输出非敏感字段：日志打印 {@code Authentication} 时不会泄露手机号。 */
    @Override
    public String toString() {
        return "LoginUser(userId=" + userId + ", role=" + role + ", enabled=" + enabled + ")";
    }
}
