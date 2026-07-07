package daydream.hotel.reservation.system.favorite.controller;

import daydream.hotel.reservation.system.common.result.Result;
import daydream.hotel.reservation.system.favorite.dto.FavoriteVO;
import daydream.hotel.reservation.system.favorite.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "收藏")
@RestController
@RequestMapping("/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @Operation(summary = "我的收藏")
    @GetMapping
    public Result<List<FavoriteVO>> list() {
        return Result.ok(favoriteService.listMyFavorites());
    }

    @Operation(summary = "是否已收藏")
    @GetMapping("/{hotelId}/status")
    public Result<Boolean> status(@PathVariable Long hotelId) {
        return Result.ok(favoriteService.isFavorited(hotelId));
    }

    @Operation(summary = "添加收藏")
    @PostMapping("/{hotelId}")
    public Result<Void> add(@PathVariable Long hotelId) {
        favoriteService.addFavorite(hotelId);
        return Result.ok();
    }

    @Operation(summary = "取消收藏")
    @DeleteMapping("/{hotelId}")
    public Result<Void> remove(@PathVariable Long hotelId) {
        favoriteService.removeFavorite(hotelId);
        return Result.ok();
    }
}
