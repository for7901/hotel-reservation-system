package daydream.hotel.reservation.system.config;

import daydream.hotel.reservation.system.banner.entity.Banner;
import daydream.hotel.reservation.system.banner.mapper.BannerMapper;
import daydream.hotel.reservation.system.coupon.entity.Coupon;
import daydream.hotel.reservation.system.coupon.mapper.CouponMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
@Order(4)
public class DevEngagementDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevEngagementDataInitializer.class);

    private final BannerMapper bannerMapper;
    private final CouponMapper couponMapper;

    public DevEngagementDataInitializer(BannerMapper bannerMapper, CouponMapper couponMapper) {
        this.bannerMapper = bannerMapper;
        this.couponMapper = couponMapper;
    }

    @Override
    public void run(String... args) {
        if (bannerMapper.selectCount(null) == 0) {
            createBanner("春日出行季", "https://picsum.photos/800/320?random=1", "/", 1);
            createBanner("周末特惠酒店", "https://picsum.photos/800/320?random=2", "/hotels", 2);
            log.info("Seeded demo banners");
        }
        if (couponMapper.selectCount(null) == 0) {
            Coupon coupon = new Coupon();
            coupon.setName("新用户立减50");
            coupon.setCouponType("FIXED");
            coupon.setAmount(BigDecimal.valueOf(50));
            coupon.setMinAmount(BigDecimal.valueOf(200));
            coupon.setTotalCount(1000);
            coupon.setClaimedCount(0);
            coupon.setStartTime(LocalDateTime.now().minusDays(1));
            coupon.setEndTime(LocalDateTime.now().plusMonths(3));
            coupon.setStatus(1);
            couponMapper.insert(coupon);

            Coupon coupon2 = new Coupon();
            coupon2.setName("满500减100");
            coupon2.setCouponType("FIXED");
            coupon2.setAmount(BigDecimal.valueOf(100));
            coupon2.setMinAmount(BigDecimal.valueOf(500));
            coupon2.setTotalCount(500);
            coupon2.setClaimedCount(0);
            coupon2.setStartTime(LocalDateTime.now().minusDays(1));
            coupon2.setEndTime(LocalDateTime.now().plusMonths(2));
            coupon2.setStatus(1);
            couponMapper.insert(coupon2);
            log.info("Seeded demo coupons");
        }
    }

    private void createBanner(String title, String imageUrl, String linkUrl, int sort) {
        Banner banner = new Banner();
        banner.setTitle(title);
        banner.setImageUrl(imageUrl);
        banner.setLinkUrl(linkUrl);
        banner.setSortOrder(sort);
        banner.setStatus(1);
        bannerMapper.insert(banner);
    }
}
