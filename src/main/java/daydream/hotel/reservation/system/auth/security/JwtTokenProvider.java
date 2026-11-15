package daydream.hotel.reservation.system.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    /** Token 版本号声明名，与 {@link TokenVersionStore} 配套实现"立即失效"。 */
    public static final String CLAIM_TOKEN_VERSION = "ver";

    public static final String CLAIM_PHONE = "phone";
    public static final String CLAIM_ROLE = "role";

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey =
                Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 签发 Token。
     *
     * <p>身份信息（用户 id、手机号、角色）全部写入 claims， 后续请求直接据此构建登录态，无需再查库； {@code ver} 记录签发时的吊销版本，供每个请求比对。
     */
    public String generateToken(Long userId, String phone, String role, long tokenVersion) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getExpiration());

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_PHONE, phone)
                .claim(CLAIM_ROLE, role)
                .claim(CLAIM_TOKEN_VERSION, tokenVersion)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 解析并校验 Token（签名 + 有效期）。
     *
     * <p>只解析一次：调用方据此判断成功或失败，不需要先 validate 再 parse。
     *
     * @throws io.jsonwebtoken.JwtException Token 过期、签名不匹配、结构非法
     * @throws IllegalArgumentException Token 为空或 payload 无法解析
     */
    public Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
    }

    /** 读取 Token 里的吊销版本号；没有该声明（旧 Token）时按 0 处理，保证升级后老 Token 不失效。 */
    public long getTokenVersion(Claims claims) {
        Object raw = claims.get(CLAIM_TOKEN_VERSION);
        return raw instanceof Number number ? number.longValue() : 0L;
    }
}
