package daydream.hotel.reservation.system.banner.controller;

import daydream.hotel.reservation.system.banner.dto.BannerVO;
import daydream.hotel.reservation.system.banner.service.BannerService;
import daydream.hotel.reservation.system.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Banner-用户端")
@RestController
@RequestMapping("/banners")
public class BannerController {

    private final BannerService bannerService;

    public BannerController(BannerService bannerService) {
        this.bannerService = bannerService;
    }

    @Operation(summary = "首页Banner")
    @GetMapping
    public Result<List<BannerVO>> list() {
        return Result.ok(bannerService.listActiveBanners());
    }
}
