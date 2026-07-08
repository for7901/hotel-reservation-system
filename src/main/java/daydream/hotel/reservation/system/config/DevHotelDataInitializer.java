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
import daydream.hotel.reservation.system.user.mapper.UserMapper;
import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
@Order(3)
public class DevHotelDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevHotelDataInitializer.class);

    private final CityMapper cityMapper;
    private final HotelMapper hotelMapper;
    private final RoomTypeMapper roomTypeMapper;
    private final HotelFacilityMapper hotelFacilityMapper;
    private final UserMapper userMapper;
    private final HotelService hotelService;

    public DevHotelDataInitializer(
            CityMapper cityMapper,
            HotelMapper hotelMapper,
            RoomTypeMapper roomTypeMapper,
            HotelFacilityMapper hotelFacilityMapper,
            UserMapper userMapper,
            HotelService hotelService) {
        this.cityMapper = cityMapper;
        this.hotelMapper = hotelMapper;
        this.roomTypeMapper = roomTypeMapper;
        this.hotelFacilityMapper = hotelFacilityMapper;
        this.userMapper = userMapper;
        this.hotelService = hotelService;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (userMapper.selectCount(
                            new LambdaQueryWrapper<User>().eq(User::getPhone, "13800100001"))
                    > 0) {
                return;
            }
            if (hotelMapper.selectCount(null) > 0) {
                return;
            }
            User merchant =
                    userMapper.selectOne(
                            new LambdaQueryWrapper<User>()
                                    .eq(User::getRole, UserRole.MERCHANT.name())
                                    .last("LIMIT 1"));
            if (merchant == null) {
                log.warn("Skip hotel seed: merchant account not found");
                return;
            }

            City hangzhou = findCity("杭州");
            City shanghai = findCity("上海");
            City beijing = findCity("北京");
            if (hangzhou == null || shanghai == null || beijing == null) {
                log.warn("Skip hotel seed: demo cities not found, wait for region initializer");
                return;
            }

            Hotel h1 =
                    createHotel(
                            merchant.getId(),
                            hangzhou.getId(),
                            "西湖假日酒店",
                            "杭州市西湖区北山街88号",
                            4,
                            "毗邻西湖，交通便利，适合休闲度假。",
                            HotelStatus.APPROVED,
                            List.of("免费WiFi", "停车场", "早餐", "健身房"));
            createRoomType(h1.getId(), "高级大床房", "大床", 35, 2, new BigDecimal("399"), 1);
            createRoomType(h1.getId(), "湖景双床房", "双床", 40, 2, new BigDecimal("459"), 1);

            Hotel h2 =
                    createHotel(
                            merchant.getId(),
                            shanghai.getId(),
                            "外滩精品酒店",
                            "上海市黄浦区中山东一路18号",
                            5,
                            "尽览黄浦江夜景，商务出行首选。",
                            HotelStatus.APPROVED,
                            List.of("免费WiFi", "接机服务", "餐厅", "会议室"));
            createRoomType(h2.getId(), "江景套房", "大床", 55, 2, new BigDecimal("899"), 1);
            createRoomType(h2.getId(), "商务标准间", "双床", 30, 2, new BigDecimal("599"), 0);

            createHotel(
                    merchant.getId(),
                    beijing.getId(),
                    "国贸商务酒店",
                    "北京市朝阳区建国门外大街1号",
                    4,
                    "CBD核心地段，待审核演示数据。",
                    HotelStatus.PENDING,
                    List.of("免费WiFi", "健身房"));

            hotelService.refreshMinPrice(h1.getId());
            hotelService.refreshMinPrice(h2.getId());
            log.info("Initialized demo hotel data");
        } catch (Exception ex) {
            log.warn("Skip hotel seed: {}", ex.getMessage());
        }
    }

    private City findCity(String name) {
        return cityMapper.selectOne(
                new LambdaQueryWrapper<City>().eq(City::getName, name).last("LIMIT 1"));
    }

    private Hotel createHotel(
            Long merchantId,
            Long cityId,
            String name,
            String address,
            int star,
            String description,
            HotelStatus status,
            List<String> facilities) {
        Hotel hotel = new Hotel();
        hotel.setMerchantId(merchantId);
        hotel.setCityId(cityId);
        hotel.setName(name);
        hotel.setAddress(address);
        hotel.setStarRating(star);
        hotel.setDescription(description);
        hotel.setStatus(status.name());
        hotel.setScore(BigDecimal.valueOf(4.6));
        hotel.setCoverImage("https://images.unsplash.com/photo-1566073771259-6a8506099945?w=800");
        hotelMapper.insert(hotel);
        for (String facility : facilities) {
            HotelFacility item = new HotelFacility();
            item.setHotelId(hotel.getId());
            item.setName(facility);
            hotelFacilityMapper.insert(item);
        }
        return hotel;
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
