package daydream.hotel.reservation.system.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import daydream.hotel.reservation.system.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {}
