package org.http3;

import com.google.common.base.Preconditions;
import java.nio.ByteBuffer;

/**
 * Variable length coding.
 *
 * @link <a href="https://quicwg.org/base-drafts/draft-ietf-quic-transport.html#rfc.section.16>Spec</a>
 */
public final class VariableLengthEncoding {

    public static final int FIRST_BYTE_MASK = 0x3F;
    public static final int ALL_BYTES = 0xFF;
    public static final int LEN_SHIFT = 6;

    private VariableLengthEncoding() {
    }

    public static void encode(long value, ByteBuffer buf) {
        Preconditions.checkArgument(value >= 0, "Only positive values are allowed: %s", value);
        long usedBits = 64 - Long.numberOfLeadingZeros(value);
        if (usedBits < 7) { // len 1 = 00 = 0
            buf.put(getByte(value, 0));
        } else if (usedBits < 15) {
            buf.put(toFirstByte(value, 1)); // len 2 = 01 = 1
            buf.put(mask(value, ALL_BYTES, 0));
        } else if (usedBits < 31) {
            buf.put(toFirstByte(value, 3)); // len 4 = 10 = 2
            buf.put(mask(value, ALL_BYTES, 2));
            buf.put(mask(value, ALL_BYTES, 1));
            buf.put(mask(value, ALL_BYTES, 0));
        } else if (usedBits < 63) {
            buf.put(toFirstByte(value, 7)); // len 8 = 11 = 3
            buf.put(mask(value, ALL_BYTES, 6));
            buf.put(mask(value, ALL_BYTES, 5));
            buf.put(mask(value, ALL_BYTES, 4));
            buf.put(mask(value, ALL_BYTES, 3));
            buf.put(mask(value, ALL_BYTES, 2));
            buf.put(mask(value, ALL_BYTES, 1));
            buf.put(mask(value, ALL_BYTES, 0));
        } else {
            throw new IllegalArgumentException("Value too large: " + value);
        }
    }

    public static long decode(ByteBuffer buf) {
        byte firstByte = buf.get();
        int len = unsignedByte(firstByte) >>> LEN_SHIFT;
        long firstByteValue = toFirstByte(firstByte);
        switch (len) {
            case 0:
                return firstByte;
            case 1:
                return shiftLeftBytes(firstByteValue, 1) | get(buf);
            case 2:
                return shiftLeftBytes(firstByteValue, 3)
                        | getShifted(buf, 2)
                        | getShifted(buf, 1)
                        | get(buf);
            case 3:
                return shiftLeftBytes(firstByteValue, 7)
                        | getShifted(buf, 6)
                        | getShifted(buf, 5)
                        | getShifted(buf, 4)
                        | getShifted(buf, 3)
                        | getShifted(buf, 2)
                        | getShifted(buf, 1)
                        | get(buf);
            default:
                throw new IllegalStateException("Unknown length: " + len);
        }
    }

    private static long toFirstByte(byte firstByte) {
        return unsignedByte(firstByte) & FIRST_BYTE_MASK;
    }

    private static int unsignedByte(byte byteValue) {
        return (int) byteValue & 0xFF;
    }

    private static long get(ByteBuffer buf) {
        return (long) buf.get() & 0xFF;
    }

    private static long getShifted(ByteBuffer buf, int pos) {
        return shiftLeftBytes(get(buf), pos);
    }

    private static long shiftLeftBytes(long byteValue, int pos) {
        return byteValue << (pos * 8);
    }

    private static byte mask(long value, int mask, int pos) {
        return (byte) ((value & shiftLeftBytes(mask, pos)) >>> pos * 8);
    }

    private static byte toFirstByte(long value, int pos) {
        byte firstByteValue = mask(value, FIRST_BYTE_MASK, pos);
        int len = 32 - Integer.numberOfLeadingZeros(pos);
        return (byte) ((len << LEN_SHIFT) | firstByteValue);
    }

    private static byte getByte(long value, int num) {
        return mask(value, ALL_BYTES, num);
    }
}
