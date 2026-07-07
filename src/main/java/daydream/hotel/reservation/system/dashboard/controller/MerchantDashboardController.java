package daydream.hotel.reservation.system.dashboard.controller;

import daydream.hotel.reservation.system.common.result.Result;
import daydream.hotel.reservation.system.dashboard.dto.DashboardStatsVO;
import daydream.hotel.reservation.system.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "仪表盘-商家端")
@RestController
@RequestMapping("/merchant/dashboard")
@PreAuthorize("hasRole('MERCHANT')")
public class MerchantDashboardController {

    private final DashboardService dashboardService;

    public MerchantDashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Operation(summary = "商家统计")
    @GetMapping("/stats")
    public Result<DashboardStatsVO> stats() {
        return Result.ok(dashboardService.getMerchantStats());
    }
}
