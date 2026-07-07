package daydream.hotel.reservation.system.favorite.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import daydream.hotel.reservation.system.auth.security.SecurityUtils;
import daydream.hotel.reservation.system.common.exception.BusinessException;
import daydream.hotel.reservation.system.common.exception.ErrorCode;
import daydream.hotel.reservation.system.favorite.dto.FavoriteVO;
import daydream.hotel.reservation.system.favorite.entity.UserFavorite;
import daydream.hotel.reservation.system.favorite.mapper.UserFavoriteMapper;
import daydream.hotel.reservation.system.hotel.entity.City;
import daydream.hotel.reservation.system.hotel.entity.Hotel;
import daydream.hotel.reservation.system.hotel.enums.HotelStatus;
import daydream.hotel.reservation.system.hotel.mapper.CityMapper;
import daydream.hotel.reservation.system.hotel.mapper.HotelMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FavoriteService {

    private final UserFavoriteMapper favoriteMapper;
    private final HotelMapper hotelMapper;
    private final CityMapper cityMapper;

    public FavoriteService(
            UserFavoriteMapper favoriteMapper, HotelMapper hotelMapper, CityMapper cityMapper) {
        this.favoriteMapper = favoriteMapper;
        this.hotelMapper = hotelMapper;
        this.cityMapper = cityMapper;
    }

    public List<FavoriteVO> listMyFavorites() {
        Long userId = SecurityUtils.getCurrentUserId();
        return favoriteMapper
                .selectList(
                        new LambdaQueryWrapper<UserFavorite>()
                                .eq(UserFavorite::getUserId, userId)
                                .orderByDesc(UserFavorite::getCreatedAt))
                .stream()
                .map(this::toVO)
                .toList();
    }

    public boolean isFavorited(Long hotelId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return favoriteMapper.selectCount(
                        new LambdaQueryWrapper<UserFavorite>()
                                .eq(UserFavorite::getUserId, userId)
                                .eq(UserFavorite::getHotelId, hotelId))
                > 0;
    }

    @Transactional
    public void addFavorite(Long hotelId) {
        Long userId = SecurityUtils.getCurrentUserId();
        Hotel hotel = hotelMapper.selectById(hotelId);
        if (hotel == null || !HotelStatus.APPROVED.name().equals(hotel.getStatus())) {
            throw new BusinessException(ErrorCode.HOTEL_NOT_FOUND);
        }
        Long exists =
                favoriteMapper.selectCount(
                        new LambdaQueryWrapper<UserFavorite>()
                                .eq(UserFavorite::getUserId, userId)
                                .eq(UserFavorite::getHotelId, hotelId));
        if (exists > 0) {
            return;
        }
        UserFavorite favorite = new UserFavorite();
        favorite.setUserId(userId);
        favorite.setHotelId(hotelId);
        favoriteMapper.insert(favorite);
    }

    @Transactional
    public void removeFavorite(Long hotelId) {
        Long userId = SecurityUtils.getCurrentUserId();
        favoriteMapper.delete(
                new LambdaQueryWrapper<UserFavorite>()
                        .eq(UserFavorite::getUserId, userId)
                        .eq(UserFavorite::getHotelId, hotelId));
    }

    private FavoriteVO toVO(UserFavorite favorite) {
        FavoriteVO vo = new FavoriteVO();
        vo.setId(favorite.getId());
        vo.setHotelId(favorite.getHotelId());
        vo.setCreatedAt(favorite.getCreatedAt());
        Hotel hotel = hotelMapper.selectById(favorite.getHotelId());
        if (hotel != null) {
            vo.setHotelName(hotel.getName());
            vo.setCoverImage(hotel.getCoverImage());
            vo.setMinPrice(hotel.getMinPrice());
            vo.setScore(hotel.getScore());
            City city = cityMapper.selectById(hotel.getCityId());
            if (city != null) {
                vo.setCityName(city.getName());
            }
        }
        return vo;
    }
}
