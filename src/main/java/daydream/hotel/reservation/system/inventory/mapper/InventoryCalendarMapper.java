package daydream.hotel.reservation.system.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import daydream.hotel.reservation.system.inventory.entity.InventoryCalendar;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface InventoryCalendarMapper extends BaseMapper<InventoryCalendar> {

    @Update(
            "UPDATE inventory_calendar SET available_rooms = available_rooms - 1, updated_at = NOW() "
                    + "WHERE room_type_id = #{roomTypeId} AND inv_date = #{invDate} AND available_rooms > 0")
    int decrementStock(@Param("roomTypeId") Long roomTypeId, @Param("invDate") String invDate);

    @Update(
            "UPDATE inventory_calendar SET available_rooms = available_rooms + 1, updated_at = NOW() "
                    + "WHERE room_type_id = #{roomTypeId} AND inv_date = #{invDate}")
    int incrementStock(@Param("roomTypeId") Long roomTypeId, @Param("invDate") String invDate);
}
