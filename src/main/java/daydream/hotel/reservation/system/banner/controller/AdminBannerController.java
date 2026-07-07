package daydream.hotel.reservation.system.banner.controller;

import daydream.hotel.reservation.system.banner.dto.BannerSaveRequest;
import daydream.hotel.reservation.system.banner.dto.BannerVO;
import daydream.hotel.reservation.system.banner.service.BannerService;
import daydream.hotel.reservation.system.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Banner-平台端")
@RestController
@RequestMapping("/admin/banners")
@PreAuthorize("hasRole('ADMIN')")
public class AdminBannerController {

    private final BannerService bannerService;

    public AdminBannerController(BannerService bannerService) {
        this.bannerService = bannerService;
    }

    @Operation(summary = "Banner列表")
    @GetMapping
    public Result<List<BannerVO>> list() {
        return Result.ok(bannerService.listAllBanners());
    }

    @Operation(summary = "新增Banner")
    @PostMapping
    public Result<BannerVO> create(@Valid @RequestBody BannerSaveRequest request) {
        return Result.ok(bannerService.create(request));
    }

    @Operation(summary = "更新Banner")
    @PutMapping("/{id}")
    public Result<BannerVO> update(
            @PathVariable Long id, @Valid @RequestBody BannerSaveRequest request) {
        return Result.ok(bannerService.update(id, request));
    }

    @Operation(summary = "删除Banner")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        bannerService.delete(id);
        return Result.ok();
    }
}
