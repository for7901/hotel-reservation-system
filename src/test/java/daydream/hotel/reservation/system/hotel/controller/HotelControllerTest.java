package daydream.hotel.reservation.system.hotel.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import daydream.hotel.reservation.system.hotel.entity.City;
import daydream.hotel.reservation.system.hotel.entity.Hotel;
import daydream.hotel.reservation.system.hotel.enums.HotelStatus;
import daydream.hotel.reservation.system.hotel.mapper.CityMapper;
import daydream.hotel.reservation.system.hotel.mapper.HotelMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HotelControllerTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private CityMapper cityMapper;

    @Autowired private HotelMapper hotelMapper;

    @BeforeEach
    void setUp() {
        hotelMapper.delete(null);
        cityMapper.delete(null);

        City city = new City();
        city.setName("测试城");
        city.setCode("test");
        cityMapper.insert(city);

        Hotel hotel = new Hotel();
        hotel.setMerchantId(1L);
        hotel.setCityId(city.getId());
        hotel.setName("测试酒店");
        hotel.setAddress("测试路1号");
        hotel.setStarRating(4);
        hotel.setStatus(HotelStatus.APPROVED.name());
        hotel.setMinPrice(BigDecimal.valueOf(299));
        hotel.setScore(BigDecimal.valueOf(4.5));
        hotelMapper.insert(hotel);
    }

    @Test
    void searchHotelsShouldReturnApprovedHotel() throws Exception {
        mockMvc.perform(get("/hotels/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list[0].name").value("测试酒店"));
    }

    @Test
    void hotelDetailShouldWork() throws Exception {
        Hotel hotel = hotelMapper.selectList(null).get(0);
        mockMvc.perform(get("/hotels/" + hotel.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("测试酒店"));
    }
}
