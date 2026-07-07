package daydream.hotel.reservation.system.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class SensitiveUtilsTest {

    @Test
    void maskPhoneShouldHideMiddleFourDigits() {
        assertEquals("138****5678", SensitiveUtils.maskPhone("13812345678"));
    }

    @Test
    void maskPhoneShouldReturnNullForNullInput() {
        assertNull(SensitiveUtils.maskPhone(null));
    }

    @Test
    void maskPhoneShouldReturnOriginalWhenLengthInvalid() {
        assertEquals("1381234", SensitiveUtils.maskPhone("1381234"));
    }
}
