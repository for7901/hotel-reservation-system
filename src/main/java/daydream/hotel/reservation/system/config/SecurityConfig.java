package daydream.hotel.reservation.system.config;

import daydream.hotel.reservation.system.auth.security.JwtAuthenticationFilter;
import daydream.hotel.reservation.system.auth.security.JwtProperties;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Security + JJWT 鉴权配置。
 *
 * <p>无状态 JWT：请求经 {@link JwtAuthenticationFilter} 解析 Token；公开接口放行，其余需登录。 方法级权限由
 * {@code @PreAuthorize} 等注解控制。
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    /** 安全过滤链：CORS、无 Session、白名单、JWT 过滤器。 */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // 前后端分离：不使用服务端 Session，鉴权依赖 JWT
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(
                        ex ->
                                ex.authenticationEntryPoint(authenticationEntryPoint)
                                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(
                        auth ->
                                auth
                                        // 健康检查、登录注册
                                        .requestMatchers(
                                                "/health",
                                                "/api/health",
                                                "/auth/**",
                                                "/api/auth/**")
                                        .permitAll()
                                        // 酒店浏览（公开）
                                        .requestMatchers("/hotels/**", "/api/hotels/**")
                                        .permitAll()
                                        // Banner / 评价只读公开
                                        .requestMatchers(
                                                HttpMethod.GET,
                                                "/banners",
                                                "/api/banners",
                                                "/banners/**",
                                                "/api/banners/**")
                                        .permitAll()
                                        .requestMatchers(
                                                HttpMethod.GET,
                                                "/reviews/hotels/**",
                                                "/api/reviews/hotels/**")
                                        .permitAll()
                                        // Swagger / OpenAPI 文档
                                        .requestMatchers(
                                                "/v3/api-docs/**",
                                                "/api/v3/api-docs/**",
                                                "/swagger-ui/**",
                                                "/api/swagger-ui/**",
                                                "/swagger-ui.html",
                                                "/api/swagger-ui.html")
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                // 在用户名密码过滤器之前解析 Authorization: Bearer <token>
                .addFilterBefore(
                        jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** 密码加密（BCrypt），注册与登录校验共用。 */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** 允许本地前端与部署域名跨域携带凭证访问。 */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(
                List.of(
                        "http://localhost:5173",
                        "http://localhost:5174",
                        "http://localhost:5175",
                        "http://127.0.0.1:5173",
                        "http://127.0.0.1:5174",
                        "http://127.0.0.1:5175",
                        "http://118.31.116.107",
                        "https://118.31.116.107"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
