package daydream.hotel.reservation.system.hotel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import daydream.hotel.reservation.system.audit.service.AuditLogService;
import daydream.hotel.reservation.system.auth.security.LoginUser;
import daydream.hotel.reservation.system.auth.security.SecurityUtils;
import daydream.hotel.reservation.system.common.cache.CacheKeys;
import daydream.hotel.reservation.system.common.cache.CacheService;
import daydream.hotel.reservation.system.common.exception.BusinessException;
import daydream.hotel.reservation.system.common.exception.ErrorCode;
import daydream.hotel.reservation.system.common.result.PageResult;
import daydream.hotel.reservation.system.config.AppProperties;
import daydream.hotel.reservation.system.hotel.dto.AdminHotelVO;
import daydream.hotel.reservation.system.hotel.dto.CityVO;
import daydream.hotel.reservation.system.hotel.dto.HotelAuditRequest;
import daydream.hotel.reservation.system.hotel.dto.HotelDetailVO;
import daydream.hotel.reservation.system.hotel.dto.HotelListItemVO;
import daydream.hotel.reservation.system.hotel.dto.HotelSaveRequest;
import daydream.hotel.reservation.system.hotel.dto.ProvinceVO;
import daydream.hotel.reservation.system.hotel.dto.RoomTypeSaveRequest;
import daydream.hotel.reservation.system.hotel.dto.RoomTypeVO;
import daydream.hotel.reservation.system.hotel.entity.City;
import daydream.hotel.reservation.system.hotel.entity.Hotel;
import daydream.hotel.reservation.system.hotel.entity.HotelFacility;
import daydream.hotel.reservation.system.hotel.entity.Province;
import daydream.hotel.reservation.system.hotel.entity.RoomType;
import daydream.hotel.reservation.system.hotel.enums.HotelStatus;
import daydream.hotel.reservation.system.hotel.mapper.CityMapper;
import daydream.hotel.reservation.system.hotel.mapper.HotelFacilityMapper;
import daydream.hotel.reservation.system.hotel.mapper.HotelMapper;
import daydream.hotel.reservation.system.hotel.mapper.ProvinceMapper;
import daydream.hotel.reservation.system.hotel.mapper.RoomTypeMapper;
import daydream.hotel.reservation.system.user.entity.User;
import daydream.hotel.reservation.system.user.enums.UserRole;
import daydream.hotel.reservation.system.user.mapper.UserMapper;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class HotelService {

    private final ProvinceMapper provinceMapper;
    private final CityMapper cityMapper;
    private final HotelMapper hotelMapper;
    private final RoomTypeMapper roomTypeMapper;
    private final HotelFacilityMapper hotelFacilityMapper;
    private final UserMapper userMapper;
    private final AuditLogService auditLogService;
    private final CacheService cacheService;
    private final AppProperties appProperties;

    public HotelService(
            ProvinceMapper provinceMapper,
            CityMapper cityMapper,
            HotelMapper hotelMapper,
            RoomTypeMapper roomTypeMapper,
            HotelFacilityMapper hotelFacilityMapper,
            UserMapper userMapper,
            AuditLogService auditLogService,
            CacheService cacheService,
            AppProperties appProperties) {
        this.provinceMapper = provinceMapper;
        this.cityMapper = cityMapper;
        this.hotelMapper = hotelMapper;
        this.roomTypeMapper = roomTypeMapper;
        this.hotelFacilityMapper = hotelFacilityMapper;
        this.userMapper = userMapper;
        this.auditLogService = auditLogService;
        this.cacheService = cacheService;
        this.appProperties = appProperties;
    }

    public List<ProvinceVO> listProvinces() {
        return cacheService.get(
                CacheKeys.PROVINCE_LIST,
                new TypeReference<>() {},
                () ->
                        provinceMapper
                                .selectList(
                                        new LambdaQueryWrapper<Province>().orderByAsc(Province::getCode))
                                .stream()
                                .map(this::toProvinceVO)
                                .toList(),
                appProperties.getCache().getCityTtlSeconds());
    }

    public List<CityVO> listCities(Long provinceId) {
        if (provinceId == null) {
            return listCities();
        }
        String cacheKey = CacheKeys.CITY_LIST + ":" + provinceId;
        return cacheService.get(
                cacheKey,
                new TypeReference<>() {},
                () ->
                        cityMapper
                                .selectList(
                                        new LambdaQueryWrapper<City>()
                                                .eq(City::getProvinceId, provinceId)
                                                .orderByAsc(City::getName))
                                .stream()
                                .map(this::toCityVO)
                                .toList(),
                appProperties.getCache().getCityTtlSeconds());
    }

    public List<CityVO> listCities() {
        return cacheService.get(
                CacheKeys.CITY_LIST,
                new TypeReference<>() {},
                () ->
                        cityMapper
                                .selectList(
                                        new LambdaQueryWrapper<City>().orderByAsc(City::getName))
                                .stream()
                                .map(this::toCityVO)
                                .toList(),
                appProperties.getCache().getCityTtlSeconds());
    }

    public PageResult<HotelListItemVO> searchHotels(
            Long provinceId,
            Long cityId,
            String keyword,
            Integer starRating,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            BigDecimal minScore,
            String sortBy,
            long page,
            long size) {
        LambdaQueryWrapper<Hotel> wrapper =
                new LambdaQueryWrapper<Hotel>()
                        .eq(Hotel::getStatus, HotelStatus.APPROVED.name())
                        .eq(starRating != null, Hotel::getStarRating, starRating)
                        .ge(minPrice != null, Hotel::getMinPrice, minPrice)
                        .le(maxPrice != null, Hotel::getMinPrice, maxPrice)
                        .ge(minScore != null, Hotel::getScore, minScore)
                        .and(
                                StringUtils.hasText(keyword),
                                w ->
                                        w.like(Hotel::getName, keyword)
                                                .or()
                                                .like(Hotel::getAddress, keyword));
        if (cityId != null) {
            wrapper.eq(Hotel::getCityId, cityId);
        } else if (provinceId != null) {
            List<Long> provinceCityIds = resolveCityIdsByProvince(provinceId);
            if (provinceCityIds.isEmpty()) {
                return new PageResult<>(List.of(), 0L, page, size);
            }
            wrapper.in(Hotel::getCityId, provinceCityIds);
        }

        if ("PRICE_ASC".equals(sortBy)) {
            wrapper.orderByAsc(Hotel::getMinPrice);
        } else if ("PRICE_DESC".equals(sortBy)) {
            wrapper.orderByDesc(Hotel::getMinPrice);
        } else {
            wrapper.orderByDesc(Hotel::getScore).orderByAsc(Hotel::getMinPrice);
        }

        Page<Hotel> hotelPage = hotelMapper.selectPage(new Page<>(page, size), wrapper);
        Map<Long, String> cityMap = loadCityNameMap(hotelPage.getRecords());
        List<HotelListItemVO> list =
                hotelPage.getRecords().stream()
                        .map(hotel -> toListItem(hotel, cityMap.get(hotel.getCityId())))
                        .toList();
        return new PageResult<>(
                list, hotelPage.getTotal(), hotelPage.getCurrent(), hotelPage.getSize());
    }

    public HotelDetailVO getHotelDetail(Long id) {
        Hotel hotel = getApprovedHotel(id);
        City city = cityMapper.selectById(hotel.getCityId());
        HotelDetailVO vo = new HotelDetailVO();
        vo.setId(hotel.getId());
        vo.setName(hotel.getName());
        vo.setCityName(city != null ? city.getName() : "");
        vo.setAddress(hotel.getAddress());
        vo.setStarRating(hotel.getStarRating());
        vo.setCoverImage(hotel.getCoverImage());
        vo.setDescription(hotel.getDescription());
        vo.setMinPrice(hotel.getMinPrice());
        vo.setScore(hotel.getScore());
        vo.setFacilities(loadFacilityNames(hotel.getId()));
        vo.setRoomTypes(listActiveRoomTypes(hotel.getId()));
        return vo;
    }

    public PageResult<AdminHotelVO> adminListHotels(
            String status, String keyword, long page, long size) {
        requireAdmin();
        LambdaQueryWrapper<Hotel> wrapper =
                new LambdaQueryWrapper<Hotel>()
                        .eq(StringUtils.hasText(status), Hotel::getStatus, status)
                        .and(
                                StringUtils.hasText(keyword),
                                w ->
                                        w.like(Hotel::getName, keyword)
                                                .or()
                                                .like(Hotel::getAddress, keyword))
                        .orderByDesc(Hotel::getCreatedAt);
        Page<Hotel> hotelPage = hotelMapper.selectPage(new Page<>(page, size), wrapper);
        return toAdminPage(hotelPage);
    }

    @Transactional
    public void auditHotel(Long id, HotelAuditRequest request) {
        requireAdmin();
        Hotel hotel = getHotelOrThrow(id);
        if (!HotelStatus.PENDING.name().equals(hotel.getStatus())
                && !HotelStatus.REJECTED.name().equals(hotel.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "当前状态不可审核");
        }
        String status = request.getStatus();
        if (!HotelStatus.APPROVED.name().equals(status)
                && !HotelStatus.REJECTED.name().equals(status)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "审核状态无效");
        }
        hotel.setStatus(status);
        hotel.setRejectReason(
                HotelStatus.REJECTED.name().equals(status) ? request.getRejectReason() : null);
        hotelMapper.updateById(hotel);
        auditLogService.record(
                "HOTEL_AUDIT", "HOTEL", hotel.getId(), "审核酒店「" + hotel.getName() + "」为 " + status);
    }

    public PageResult<AdminHotelVO> merchantListHotels(long page, long size) {
        Long merchantId = getCurrentMerchantId();
        Page<Hotel> hotelPage =
                hotelMapper.selectPage(
                        new Page<>(page, size),
                        new LambdaQueryWrapper<Hotel>()
                                .eq(Hotel::getMerchantId, merchantId)
                                .orderByDesc(Hotel::getCreatedAt));
        return toAdminPage(hotelPage);
    }

    @Transactional
    public AdminHotelVO createHotel(HotelSaveRequest request) {
        Long merchantId = getCurrentMerchantId();
        validateCity(request.getCityId());
        Hotel hotel = new Hotel();
        hotel.setMerchantId(merchantId);
        applyHotelFields(hotel, request);
        hotel.setStatus(HotelStatus.PENDING.name());
        hotel.setScore(BigDecimal.valueOf(4.5));
        hotelMapper.insert(hotel);
        saveFacilities(hotel.getId(), request.getFacilities());
        return toAdminVO(hotel);
    }

    @Transactional
    public AdminHotelVO updateHotel(Long id, HotelSaveRequest request) {
        Hotel hotel = getOwnedHotel(id);
        validateCity(request.getCityId());
        applyHotelFields(hotel, request);
        if (HotelStatus.REJECTED.name().equals(hotel.getStatus())) {
            hotel.setStatus(HotelStatus.PENDING.name());
            hotel.setRejectReason(null);
        }
        hotelMapper.updateById(hotel);
        replaceFacilities(hotel.getId(), request.getFacilities());
        refreshMinPrice(hotel.getId());
        return toAdminVO(hotel);
    }

    @Transactional
    public void deleteHotel(Long id) {
        Hotel hotel = getOwnedHotel(id);
        hotelMapper.deleteById(hotel.getId());
    }

    public List<RoomTypeVO> listRoomTypes(Long hotelId) {
        getOwnedHotel(hotelId);
        return roomTypeMapper
                .selectList(
                        new LambdaQueryWrapper<RoomType>()
                                .eq(RoomType::getHotelId, hotelId)
                                .orderByAsc(RoomType::getBasePrice))
                .stream()
                .map(this::toRoomTypeVO)
                .toList();
    }

    @Transactional
    public RoomTypeVO createRoomType(Long hotelId, RoomTypeSaveRequest request) {
        getOwnedHotel(hotelId);
        RoomType roomType = new RoomType();
        roomType.setHotelId(hotelId);
        applyRoomTypeFields(roomType, request);
        roomType.setStatus(1);
        roomTypeMapper.insert(roomType);
        refreshMinPrice(hotelId);
        return toRoomTypeVO(roomType);
    }

    @Transactional
    public RoomTypeVO updateRoomType(Long hotelId, Long roomTypeId, RoomTypeSaveRequest request) {
        getOwnedHotel(hotelId);
        RoomType roomType = getRoomTypeOrThrow(hotelId, roomTypeId);
        applyRoomTypeFields(roomType, request);
        roomTypeMapper.updateById(roomType);
        refreshMinPrice(hotelId);
        return toRoomTypeVO(roomType);
    }

    @Transactional
    public void deleteRoomType(Long hotelId, Long roomTypeId) {
        getOwnedHotel(hotelId);
        roomTypeMapper.deleteById(getRoomTypeOrThrow(hotelId, roomTypeId).getId());
        refreshMinPrice(hotelId);
    }

    public void refreshMinPrice(Long hotelId) {
        List<RoomType> roomTypes =
                roomTypeMapper.selectList(
                        new LambdaQueryWrapper<RoomType>()
                                .eq(RoomType::getHotelId, hotelId)
                                .eq(RoomType::getStatus, 1));
        BigDecimal minPrice =
                roomTypes.stream()
                        .map(RoomType::getBasePrice)
                        .filter(Objects::nonNull)
                        .min(BigDecimal::compareTo)
                        .orElse(null);
        Hotel hotel = hotelMapper.selectById(hotelId);
        if (hotel != null) {
            hotel.setMinPrice(minPrice);
            hotelMapper.updateById(hotel);
        }
    }

    private Hotel getApprovedHotel(Long id) {
        Hotel hotel = hotelMapper.selectById(id);
        if (hotel == null || !HotelStatus.APPROVED.name().equals(hotel.getStatus())) {
            throw new BusinessException(ErrorCode.HOTEL_NOT_FOUND);
        }
        return hotel;
    }

    private Hotel getHotelOrThrow(Long id) {
        Hotel hotel = hotelMapper.selectById(id);
        if (hotel == null) {
            throw new BusinessException(ErrorCode.HOTEL_NOT_FOUND);
        }
        return hotel;
    }

    private Hotel getOwnedHotel(Long id) {
        Hotel hotel = getHotelOrThrow(id);
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (!hotel.getMerchantId().equals(loginUser.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return hotel;
    }

    private RoomType getRoomTypeOrThrow(Long hotelId, Long roomTypeId) {
        RoomType roomType =
                roomTypeMapper.selectOne(
                        new LambdaQueryWrapper<RoomType>()
                                .eq(RoomType::getId, roomTypeId)
                                .eq(RoomType::getHotelId, hotelId));
        if (roomType == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return roomType;
    }

    private void requireAdmin() {
        if (!UserRole.ADMIN.name().equals(SecurityUtils.getLoginUser().getRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private Long getCurrentMerchantId() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (!UserRole.MERCHANT.name().equals(loginUser.getRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return loginUser.getUserId();
    }

    private void validateCity(Long cityId) {
        if (cityMapper.selectById(cityId) == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "城市不存在");
        }
    }

    private void applyHotelFields(Hotel hotel, HotelSaveRequest request) {
        hotel.setCityId(request.getCityId());
        hotel.setName(request.getName());
        hotel.setAddress(request.getAddress());
        hotel.setStarRating(request.getStarRating());
        hotel.setCoverImage(request.getCoverImage());
        hotel.setDescription(request.getDescription());
    }

    private void applyRoomTypeFields(RoomType roomType, RoomTypeSaveRequest request) {
        roomType.setName(request.getName());
        roomType.setBedType(request.getBedType());
        roomType.setArea(request.getArea());
        roomType.setMaxGuests(request.getMaxGuests());
        roomType.setBasePrice(request.getBasePrice());
        roomType.setCoverImage(request.getCoverImage());
        roomType.setDescription(request.getDescription());
        roomType.setBreakfast(request.getBreakfast() != null ? request.getBreakfast() : 0);
    }

    private void saveFacilities(Long hotelId, List<String> facilities) {
        if (facilities == null) {
            return;
        }
        for (String name : facilities) {
            if (!StringUtils.hasText(name)) {
                continue;
            }
            HotelFacility facility = new HotelFacility();
            facility.setHotelId(hotelId);
            facility.setName(name.trim());
            hotelFacilityMapper.insert(facility);
        }
    }

    private void replaceFacilities(Long hotelId, List<String> facilities) {
        hotelFacilityMapper.delete(
                new LambdaQueryWrapper<HotelFacility>().eq(HotelFacility::getHotelId, hotelId));
        saveFacilities(hotelId, facilities);
    }

    private List<String> loadFacilityNames(Long hotelId) {
        return hotelFacilityMapper
                .selectList(
                        new LambdaQueryWrapper<HotelFacility>()
                                .eq(HotelFacility::getHotelId, hotelId))
                .stream()
                .map(HotelFacility::getName)
                .toList();
    }

    private List<RoomTypeVO> listActiveRoomTypes(Long hotelId) {
        return roomTypeMapper
                .selectList(
                        new LambdaQueryWrapper<RoomType>()
                                .eq(RoomType::getHotelId, hotelId)
                                .eq(RoomType::getStatus, 1)
                                .orderByAsc(RoomType::getBasePrice))
                .stream()
                .map(this::toRoomTypeVO)
                .toList();
    }

    private Map<Long, City> loadCityMap(List<Hotel> hotels) {
        if (hotels.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> cityIds = hotels.stream().map(Hotel::getCityId).distinct().toList();
        return cityMapper.selectBatchIds(cityIds).stream()
                .collect(Collectors.toMap(City::getId, city -> city));
    }

    private Map<Long, String> loadCityNameMap(List<Hotel> hotels) {
        return loadCityMap(hotels).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getName()));
    }

    private PageResult<AdminHotelVO> toAdminPage(Page<Hotel> hotelPage) {
        Map<Long, City> cityMap = loadCityMap(hotelPage.getRecords());
        Map<Long, String> merchantMap = loadMerchantNameMap(hotelPage.getRecords());
        List<AdminHotelVO> list =
                hotelPage.getRecords().stream()
                        .map(
                                hotel -> {
                                    AdminHotelVO vo = toAdminVO(hotel);
                                    City city = cityMap.get(hotel.getCityId());
                                    vo.setCityName(city != null ? city.getName() : "");
                                    vo.setProvinceId(city != null ? city.getProvinceId() : null);
                                    vo.setMerchantName(merchantMap.get(hotel.getMerchantId()));
                                    return vo;
                                })
                        .toList();
        return new PageResult<>(
                list, hotelPage.getTotal(), hotelPage.getCurrent(), hotelPage.getSize());
    }

    private Map<Long, String> loadMerchantNameMap(List<Hotel> hotels) {
        if (hotels.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> merchantIds = hotels.stream().map(Hotel::getMerchantId).distinct().toList();
        return userMapper.selectBatchIds(merchantIds).stream()
                .collect(
                        Collectors.toMap(
                                User::getId,
                                u ->
                                        StringUtils.hasText(u.getNickname())
                                                ? u.getNickname()
                                                : u.getPhone()));
    }

    private List<Long> resolveCityIdsByProvince(Long provinceId) {
        return cityMapper
                .selectList(
                        new LambdaQueryWrapper<City>()
                                .eq(City::getProvinceId, provinceId)
                                .select(City::getId))
                .stream()
                .map(City::getId)
                .toList();
    }

    private ProvinceVO toProvinceVO(Province province) {
        ProvinceVO vo = new ProvinceVO();
        vo.setId(province.getId());
        vo.setName(province.getName());
        vo.setCode(province.getCode());
        return vo;
    }

    private CityVO toCityVO(City city) {
        CityVO vo = new CityVO();
        vo.setId(city.getId());
        vo.setProvinceId(city.getProvinceId());
        vo.setName(city.getName());
        vo.setCode(city.getCode());
        return vo;
    }

    private HotelListItemVO toListItem(Hotel hotel, String cityName) {
        HotelListItemVO vo = new HotelListItemVO();
        vo.setId(hotel.getId());
        vo.setName(hotel.getName());
        vo.setCityName(cityName);
        vo.setAddress(hotel.getAddress());
        vo.setStarRating(hotel.getStarRating());
        vo.setCoverImage(hotel.getCoverImage());
        vo.setMinPrice(hotel.getMinPrice());
        vo.setScore(hotel.getScore());
        return vo;
    }

    private AdminHotelVO toAdminVO(Hotel hotel) {
        AdminHotelVO vo = new AdminHotelVO();
        vo.setId(hotel.getId());
        vo.setMerchantId(hotel.getMerchantId());
        vo.setCityId(hotel.getCityId());
        vo.setName(hotel.getName());
        vo.setAddress(hotel.getAddress());
        vo.setStarRating(hotel.getStarRating());
        vo.setCoverImage(hotel.getCoverImage());
        vo.setDescription(hotel.getDescription());
        vo.setStatus(hotel.getStatus());
        vo.setRejectReason(hotel.getRejectReason());
        vo.setMinPrice(hotel.getMinPrice());
        vo.setScore(hotel.getScore());
        vo.setFacilities(loadFacilityNames(hotel.getId()));
        vo.setCreatedAt(hotel.getCreatedAt());
        City city = cityMapper.selectById(hotel.getCityId());
        if (city != null) {
            vo.setCityName(city.getName());
        }
        User merchant = userMapper.selectById(hotel.getMerchantId());
        if (merchant != null) {
            vo.setMerchantName(
                    StringUtils.hasText(merchant.getNickname())
                            ? merchant.getNickname()
                            : merchant.getPhone());
        }
        return vo;
    }

    private RoomTypeVO toRoomTypeVO(RoomType roomType) {
        RoomTypeVO vo = new RoomTypeVO();
        vo.setId(roomType.getId());
        vo.setHotelId(roomType.getHotelId());
        vo.setName(roomType.getName());
        vo.setBedType(roomType.getBedType());
        vo.setArea(roomType.getArea());
        vo.setMaxGuests(roomType.getMaxGuests());
        vo.setBasePrice(roomType.getBasePrice());
        vo.setCoverImage(roomType.getCoverImage());
        vo.setDescription(roomType.getDescription());
        vo.setBreakfast(roomType.getBreakfast());
        vo.setStatus(roomType.getStatus());
        return vo;
    }
}
