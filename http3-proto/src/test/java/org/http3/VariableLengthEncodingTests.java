package org.http3;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.io.BaseEncoding;
import java.nio.ByteBuffer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public final class VariableLengthEncodingTests {

    @ParameterizedTest
    @CsvSource({
            "00, 0",
            "c2197c5eff14e88c, 151288809941952652",
            "9d7f3e7d, 494878333",
            "7bbd, 15293",
            "25, 37",
            "4025, 37"})
    public void testEncoding(String hex, long value) {
        byte[] hexBytes = BaseEncoding.base16().lowerCase().decode(hex);
        assertThat(VariableLengthEncoding.decode(ByteBuffer.wrap(hexBytes)))
                .isEqualTo(value);
    }

    @ParameterizedTest
    @CsvSource({
            "00, 0",
            "c2197c5eff14e88c, 151288809941952652",
            "9d7f3e7d, 494878333",
            "7bbd, 15293",
            "25, 37"})
    public void assertEncode(String hex, long value) {
        byte[] hexBytes = BaseEncoding.base16().lowerCase().decode(hex);
        ByteBuffer writeBuf = ByteBuffer.allocate(hexBytes.length);
        VariableLengthEncoding.encode(value, writeBuf);

        assertThat(BaseEncoding.base16().lowerCase().encode(writeBuf.array()))
                .isEqualTo(hex);
    }

    private static Object[][] values() {
        return new Object[][] {

        };
    }
}
