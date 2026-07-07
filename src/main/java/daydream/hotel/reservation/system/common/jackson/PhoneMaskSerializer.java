package daydream.hotel.reservation.system.common.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import daydream.hotel.reservation.system.common.util.SensitiveUtils;
import java.io.IOException;

public class PhoneMaskSerializer extends JsonSerializer<String> {

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {
        gen.writeString(SensitiveUtils.maskPhone(value));
    }
}
