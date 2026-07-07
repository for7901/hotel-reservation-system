package daydream.hotel.reservation.system.banner.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import daydream.hotel.reservation.system.auth.security.SecurityUtils;
import daydream.hotel.reservation.system.banner.dto.BannerSaveRequest;
import daydream.hotel.reservation.system.banner.dto.BannerVO;
import daydream.hotel.reservation.system.banner.entity.Banner;
import daydream.hotel.reservation.system.banner.mapper.BannerMapper;
import daydream.hotel.reservation.system.common.cache.CacheKeys;
import daydream.hotel.reservation.system.common.cache.CacheService;
import daydream.hotel.reservation.system.common.exception.BusinessException;
import daydream.hotel.reservation.system.common.exception.ErrorCode;
import daydream.hotel.reservation.system.config.AppProperties;
import daydream.hotel.reservation.system.user.enums.UserRole;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BannerService {

    private final BannerMapper bannerMapper;
    private final CacheService cacheService;
    private final AppProperties appProperties;

    public BannerService(
            BannerMapper bannerMapper, CacheService cacheService, AppProperties appProperties) {
        this.bannerMapper = bannerMapper;
        this.cacheService = cacheService;
        this.appProperties = appProperties;
    }

    public List<BannerVO> listActiveBanners() {
        return cacheService.get(
                CacheKeys.BANNER_ACTIVE,
                new TypeReference<>() {},
                this::loadActiveBanners,
                appProperties.getCache().getBannerTtlSeconds());
    }

    private List<BannerVO> loadActiveBanners() {
        return bannerMapper
                .selectList(
                        new LambdaQueryWrapper<Banner>()
                                .eq(Banner::getStatus, 1)
                                .orderByAsc(Banner::getSortOrder)
                                .orderByDesc(Banner::getId))
                .stream()
                .map(this::toVO)
                .toList();
    }

    public List<BannerVO> listAllBanners() {
        requireAdmin();
        return bannerMapper
                .selectList(
                        new LambdaQueryWrapper<Banner>()
                                .orderByAsc(Banner::getSortOrder)
                                .orderByDesc(Banner::getId))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Transactional
    public BannerVO create(BannerSaveRequest request) {
        requireAdmin();
        Banner banner = new Banner();
        applyFields(banner, request);
        if (banner.getStatus() == null) {
            banner.setStatus(1);
        }
        if (banner.getSortOrder() == null) {
            banner.setSortOrder(0);
        }
        bannerMapper.insert(banner);
        evictBannerCache();
        return toVO(banner);
    }

    @Transactional
    public BannerVO update(Long id, BannerSaveRequest request) {
        requireAdmin();
        Banner banner = getOrThrow(id);
        applyFields(banner, request);
        bannerMapper.updateById(banner);
        evictBannerCache();
        return toVO(banner);
    }

    @Transactional
    public void delete(Long id) {
        requireAdmin();
        bannerMapper.deleteById(getOrThrow(id).getId());
        evictBannerCache();
    }

    private void evictBannerCache() {
        cacheService.evict(CacheKeys.BANNER_ACTIVE);
    }

    private Banner getOrThrow(Long id) {
        Banner banner = bannerMapper.selectById(id);
        if (banner == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return banner;
    }

    private void applyFields(Banner banner, BannerSaveRequest request) {
        banner.setTitle(request.getTitle());
        banner.setImageUrl(request.getImageUrl());
        banner.setLinkUrl(request.getLinkUrl());
        if (request.getSortOrder() != null) {
            banner.setSortOrder(request.getSortOrder());
        }
        if (request.getStatus() != null) {
            banner.setStatus(request.getStatus());
        }
    }

    private void requireAdmin() {
        if (!UserRole.ADMIN.name().equals(SecurityUtils.getLoginUser().getRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private BannerVO toVO(Banner banner) {
        BannerVO vo = new BannerVO();
        vo.setId(banner.getId());
        vo.setTitle(banner.getTitle());
        vo.setImageUrl(banner.getImageUrl());
        vo.setLinkUrl(banner.getLinkUrl());
        vo.setSortOrder(banner.getSortOrder());
        vo.setStatus(banner.getStatus());
        return vo;
    }
}
