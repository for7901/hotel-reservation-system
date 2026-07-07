package daydream.hotel.reservation.system.controller;

import daydream.hotel.reservation.system.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "健康检查")
@RestController
@RequestMapping("/health")
public class HealthController {

    @Value("${spring.application.name}")
    private String appName;

    @Value("${spring.profiles.active:default}")
    private String profile;

    @Operation(summary = "服务健康检查")
    @GetMapping
    public Result<Map<String, Object>> health() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("app", appName);
        data.put("profile", profile);
        data.put("status", "UP");
        data.put("timestamp", LocalDateTime.now().toString());
        return Result.ok(data);
    }
}
