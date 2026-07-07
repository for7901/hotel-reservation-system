package daydream.hotel.reservation.system.order.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import daydream.hotel.reservation.system.auth.dto.RegisterRequest;
import daydream.hotel.reservation.system.hotel.entity.City;
import daydream.hotel.reservation.system.hotel.entity.Hotel;
import daydream.hotel.reservation.system.hotel.entity.RoomType;
import daydream.hotel.reservation.system.hotel.enums.HotelStatus;
import daydream.hotel.reservation.system.hotel.mapper.CityMapper;
import daydream.hotel.reservation.system.hotel.mapper.HotelMapper;
import daydream.hotel.reservation.system.hotel.mapper.RoomTypeMapper;
import daydream.hotel.reservation.system.inventory.entity.InventoryCalendar;
import daydream.hotel.reservation.system.inventory.mapper.InventoryCalendarMapper;
import daydream.hotel.reservation.system.order.dto.CreateOrderRequest;
import daydream.hotel.reservation.system.order.dto.OrderGuestRequest;
import daydream.hotel.reservation.system.order.mapper.HotelOrderMapper;
import daydream.hotel.reservation.system.order.mapper.OrderGuestMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderControllerTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private CityMapper cityMapper;

    @Autowired private HotelMapper hotelMapper;

    @Autowired private RoomTypeMapper roomTypeMapper;

    @Autowired private InventoryCalendarMapper inventoryMapper;

    @Autowired private HotelOrderMapper orderMapper;

    @Autowired private OrderGuestMapper orderGuestMapper;

    @Autowired private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private String token;
    private Long hotelId;
    private Long roomTypeId;
    private LocalDate checkIn;
    private LocalDate checkOut;

    @BeforeEach
    void setUp() throws Exception {
        orderMapper.delete(null);
        orderGuestMapper.delete(null);
        inventoryMapper.delete(null);
        roomTypeMapper.delete(null);
        hotelMapper.delete(null);
        cityMapper.delete(null);
        jdbcTemplate.execute("DELETE FROM sys_user");

        City city = new City();
        city.setName("订单测试城");
        city.setCode("order-test");
        cityMapper.insert(city);

        Hotel hotel = new Hotel();
        hotel.setMerchantId(1L);
        hotel.setCityId(city.getId());
        hotel.setName("订单测试酒店");
        hotel.setAddress("测试路99号");
        hotel.setStarRating(4);
        hotel.setStatus(HotelStatus.APPROVED.name());
        hotel.setMinPrice(BigDecimal.valueOf(299));
        hotel.setScore(BigDecimal.valueOf(4.5));
        hotelMapper.insert(hotel);
        hotelId = hotel.getId();

        RoomType roomType = new RoomType();
        roomType.setHotelId(hotelId);
        roomType.setName("大床房");
        roomType.setBedType("大床");
        roomType.setMaxGuests(2);
        roomType.setBasePrice(BigDecimal.valueOf(299));
        roomType.setStatus(1);
        roomTypeMapper.insert(roomType);
        roomTypeId = roomType.getId();

        checkIn = LocalDate.now().plusDays(1);
        checkOut = LocalDate.now().plusDays(2);

        InventoryCalendar inventory = new InventoryCalendar();
        inventory.setRoomTypeId(roomTypeId);
        inventory.setInvDate(checkIn);
        inventory.setPrice(BigDecimal.valueOf(299));
        inventory.setAvailableRooms(5);
        inventoryMapper.insert(inventory);

        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setPhone("13900003333");
        registerRequest.setPassword("123456");
        registerRequest.setNickname("订单用户");

        MvcResult registerResult =
                mockMvc.perform(
                                post("/auth/register")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(registerRequest)))
                        .andExpect(status().isOk())
                        .andReturn();
        token =
                objectMapper
                        .readTree(registerResult.getResponse().getContentAsString())
                        .path("data")
                        .path("token")
                        .asText();
    }

    @Test
    void availabilityShouldWork() throws Exception {
        mockMvc.perform(
                        get("/hotels/" + hotelId + "/availability")
                                .param("roomTypeId", roomTypeId.toString())
                                .param("checkInDate", checkIn.toString())
                                .param("checkOutDate", checkOut.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.available").value(true))
                .andExpect(jsonPath("$.data.totalPrice").value(299));
    }

    @Test
    void createPayAndCancelOrder() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setHotelId(hotelId);
        request.setRoomTypeId(roomTypeId);
        request.setCheckInDate(checkIn);
        request.setCheckOutDate(checkOut);
        request.setGuestCount(1);
        OrderGuestRequest guest = new OrderGuestRequest();
        guest.setName("张三");
        guest.setPhone("13900003333");
        request.setGuests(List.of(guest));

        MvcResult createResult =
                mockMvc.perform(
                                post("/orders")
                                        .header("Authorization", "Bearer " + token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.code").value(0))
                        .andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"))
                        .andExpect(jsonPath("$.data.guestCount").value(1))
                        .andExpect(jsonPath("$.data.guests[0].name").value("张三"))
                        .andExpect(jsonPath("$.data.guestPhone").value("139****3333"))
                        .andReturn();

        long orderId =
                objectMapper
                        .readTree(createResult.getResponse().getContentAsString())
                        .path("data")
                        .path("id")
                        .asLong();

        mockMvc.perform(
                        post("/orders/" + orderId + "/pay")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAID"));

        mockMvc.perform(get("/orders/my").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));

        mockMvc.perform(
                        post("/orders/" + orderId + "/cancel")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2005));
    }

    @Test
    void createOrderShouldRejectGuestCountExceedingMaxGuests() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setHotelId(hotelId);
        request.setRoomTypeId(roomTypeId);
        request.setCheckInDate(checkIn);
        request.setCheckOutDate(checkOut);
        request.setGuestCount(3);
        OrderGuestRequest guest1 = new OrderGuestRequest();
        guest1.setName("张三");
        guest1.setPhone("13900003333");
        OrderGuestRequest guest2 = new OrderGuestRequest();
        guest2.setName("李四");
        guest2.setPhone("13900003334");
        OrderGuestRequest guest3 = new OrderGuestRequest();
        guest3.setName("王五");
        guest3.setPhone("13900003335");
        request.setGuests(List.of(guest1, guest2, guest3));

        mockMvc.perform(
                        post("/orders")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2006));
    }
}
