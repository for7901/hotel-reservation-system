package daydream.hotel.reservation.system.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import daydream.hotel.reservation.system.hotel.entity.City;
import daydream.hotel.reservation.system.hotel.entity.Hotel;
import daydream.hotel.reservation.system.hotel.entity.HotelFacility;
import daydream.hotel.reservation.system.hotel.entity.RoomType;
import daydream.hotel.reservation.system.hotel.enums.HotelStatus;
import daydream.hotel.reservation.system.hotel.mapper.CityMapper;
import daydream.hotel.reservation.system.hotel.mapper.HotelFacilityMapper;
import daydream.hotel.reservation.system.hotel.mapper.HotelMapper;
import daydream.hotel.reservation.system.hotel.mapper.RoomTypeMapper;
import daydream.hotel.reservation.system.hotel.service.HotelService;
import daydream.hotel.reservation.system.user.entity.User;
import daydream.hotel.reservation.system.user.enums.UserRole;
import daydream.hotel.reservation.system.user.enums.UserStatus;
import daydream.hotel.reservation.system.user.mapper.UserMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
@Order(2)
public class DevMockDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevMockDataInitializer.class);
    private static final String MOCK_MERCHANT_PHONE = "13800100001";
    private static final String MERCHANT_PASSWORD = "merchant123";
    private static final int MERCHANT_COUNT = 10;
    private static final int HOTEL_COUNT = 100;

    private static final String[] MERCHANT_NAMES = {
        "华东旅业", "西南酒店集团", "北方住宿", "华南精选", "中部商旅",
        "沿海度假", "丝路驿站", "江城客栈", "天府旅宿", "关东佳庭"
    };

    private static final String[] HOTEL_PREFIXES = {
        "如家", "汉庭", "全季", "亚朵", "维也纳", "锦江", "格林", "尚客", "桔子", "美居"
    };

    private static final String[] HOTEL_SUFFIXES = {"精选酒店", "商务酒店", "度假酒店", "轻奢酒店", "舒适酒店"};

    private static final String[] DISTRICTS = {"中心", "新区", "高新", "古城", "滨江", "经开", "文化", "商业"};

    private static final String[] STREETS = {
        "解放路", "人民路", "中山路", "建设路", "文化路", "和平路", "迎宾路", "长安街"
    };

    private static final String[][] FACILITY_SETS = {
        {"免费WiFi", "停车场", "早餐"},
        {"免费WiFi", "健身房", "餐厅"},
        {"免费WiFi", "停车场", "会议室", "洗衣服务"},
        {"免费WiFi", "接机服务", "餐厅", "健身房"},
        {"免费WiFi", "停车场", "早餐", "儿童乐园"}
    };

    private static final String[] COVER_IMAGES = {
        "https://images.unsplash.com/photo-1566073771259-6a8506099945?w=800",
        "https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?w=800",
        "https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=800",
        "https://images.unsplash.com/photo-1618773928121-c66442b4e440?w=800",
        "https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?w=800"
    };

    private final UserMapper userMapper;
    private final CityMapper cityMapper;
    private final HotelMapper hotelMapper;
    private final RoomTypeMapper roomTypeMapper;
    private final HotelFacilityMapper hotelFacilityMapper;
    private final HotelService hotelService;
    private final PasswordEncoder passwordEncoder;
    private final Random random = new Random(20260707L);

    public DevMockDataInitializer(
            UserMapper userMapper,
            CityMapper cityMapper,
            HotelMapper hotelMapper,
            RoomTypeMapper roomTypeMapper,
            HotelFacilityMapper hotelFacilityMapper,
            HotelService hotelService,
            PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.cityMapper = cityMapper;
        this.hotelMapper = hotelMapper;
        this.roomTypeMapper = roomTypeMapper;
        this.hotelFacilityMapper = hotelFacilityMapper;
        this.hotelService = hotelService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (isMockDataSeeded()) {
                return;
            }
            List<City> cities =
                    cityMapper.selectList(
                            new LambdaQueryWrapper<City>()
                                    .isNotNull(City::getProvinceId)
                                    .orderByAsc(City::getId));
            if (cities.isEmpty()) {
                log.warn("Skip mock seed: no cities available");
                return;
            }

            List<User> merchants = createMerchants();
            List<City> cityPool = new ArrayList<>(cities);

            for (int i = 0; i < HOTEL_COUNT; i++) {
                User merchant = merchants.get(i % MERCHANT_COUNT);
                City city = cityPool.get(random.nextInt(cityPool.size()));
                createMockHotel(merchant.getId(), city, i);
            }

            log.info(
                    "Initialized mock data: {} merchants, {} hotels (password: {})",
                    MERCHANT_COUNT,
                    HOTEL_COUNT,
                    MERCHANT_PASSWORD);
            log.info(
                    "Mock merchant phones: 13800100001 ~ 13800100010, password: {}",
                    MERCHANT_PASSWORD);
        } catch (Exception ex) {
            log.error("Failed to initialize mock data", ex);
        }
    }

    private boolean isMockDataSeeded() {
        return userMapper.selectCount(
                        new LambdaQueryWrapper<User>().eq(User::getPhone, MOCK_MERCHANT_PHONE))
                > 0;
    }

    private List<User> createMerchants() {
        List<User> merchants = new ArrayList<>(MERCHANT_COUNT);
        String encodedPassword = passwordEncoder.encode(MERCHANT_PASSWORD);
        for (int i = 0; i < MERCHANT_COUNT; i++) {
            String phone = String.format("138001000%02d", i + 1);
            User user = new User();
            user.setPhone(phone);
            user.setPassword(encodedPassword);
            user.setNickname(MERCHANT_NAMES[i]);
            user.setRole(UserRole.MERCHANT.name());
            user.setStatus(UserStatus.ACTIVE.getValue());
            userMapper.insert(user);
            merchants.add(user);
        }
        return merchants;
    }

    private void createMockHotel(Long merchantId, City city, int index) {
        String prefix = HOTEL_PREFIXES[index % HOTEL_PREFIXES.length];
        String suffix = HOTEL_SUFFIXES[random.nextInt(HOTEL_SUFFIXES.length)];
        String hotelName = city.getName() + prefix + suffix;
        int star = 3 + random.nextInt(3);
        String district = city.getName() + DISTRICTS[random.nextInt(DISTRICTS.length)] + "区";
        String street = STREETS[random.nextInt(STREETS.length)];
        int streetNo = 1 + random.nextInt(200);
        String address = district + street + streetNo + "号";
        String description =
                String.format("%s%s，位于%s，提供舒适便捷的住宿体验，适合商务与休闲出行。", city.getName(), suffix, district);

        Hotel hotel = new Hotel();
        hotel.setMerchantId(merchantId);
        hotel.setCityId(city.getId());
        hotel.setName(hotelName);
        hotel.setAddress(address);
        hotel.setStarRating(star);
        hotel.setDescription(description);
        hotel.setStatus(HotelStatus.APPROVED.name());
        hotel.setScore(
                BigDecimal.valueOf(4.0 + random.nextDouble()).setScale(1, RoundingMode.HALF_UP));
        hotel.setCoverImage(COVER_IMAGES[random.nextInt(COVER_IMAGES.length)]);
        hotelMapper.insert(hotel);

        String[] facilities = FACILITY_SETS[random.nextInt(FACILITY_SETS.length)];
        for (String facility : facilities) {
            HotelFacility item = new HotelFacility();
            item.setHotelId(hotel.getId());
            item.setName(facility);
            hotelFacilityMapper.insert(item);
        }

        BigDecimal basePrice = BigDecimal.valueOf(180 + random.nextInt(620));
        createRoomType(
                hotel.getId(),
                "标准大床房",
                "大床",
                28 + random.nextInt(10),
                2,
                basePrice,
                random.nextInt(2));
        createRoomType(
                hotel.getId(),
                "豪华双床房",
                "双床",
                32 + random.nextInt(12),
                2,
                basePrice.add(BigDecimal.valueOf(80 + random.nextInt(120))),
                1);

        hotelService.refreshMinPrice(hotel.getId());
    }

    private void createRoomType(
            Long hotelId,
            String name,
            String bedType,
            int area,
            int guests,
            BigDecimal price,
            int breakfast) {
        RoomType roomType = new RoomType();
        roomType.setHotelId(hotelId);
        roomType.setName(name);
        roomType.setBedType(bedType);
        roomType.setArea(area);
        roomType.setMaxGuests(guests);
        roomType.setBasePrice(price);
        roomType.setBreakfast(breakfast);
        roomType.setStatus(1);
        roomTypeMapper.insert(roomType);
    }
}
