package jadex.common;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SBinConv
{
    /**
     *  Convert bytes to a short.
     */
    public static short bytesToShort(byte[] buffer)
    {
        assert buffer.length == 2;

        return bytesToShort(buffer, 0);
    }

    /**
     *  Convert bytes to a short.
     */
    public static short bytesToShort(byte[] buffer, int offset)
    {
        short value = (short)((0xFF & buffer[offset]) << 8);
        value |= (0xFF & buffer[offset + 1]);

        return value;
    }

    /**
     *  Convert a short to bytes.
     */
    public static byte[] shortToBytes(int val)
    {
        byte[] buffer = new byte[2];

        shortIntoBytes(val, buffer, 0);

        return buffer;
    }

    /**
     *  Convert a short into byte array.
     */
    public static void shortIntoBytes(int val, byte[] buffer, int offset)
    {
        buffer[offset] = (byte)((val >>> 8) & 0xFF);
        buffer[offset+1] = (byte)(val & 0xFF);
    }

    /**
     *  Convert bytes to an integer.
     */
    public static int bytesToInt(byte[] buffer)
    {
        assert buffer.length == 4;
//		if(buffer.length != 4)
//		{
//			throw new IllegalArgumentException("buffer length must be 4 bytes!");
//		}

        return bytesToInt(buffer, 0);
    }

    /**
     *  Convert bytes to an integer.
     */
    public static int bytesToInt(byte[] buffer, int offset)
    {
        int value = (0xFF & buffer[offset]) << 24;
        value |= (0xFF & buffer[offset+1]) << 16;
        value |= (0xFF & buffer[offset+2]) << 8;
        value |= (0xFF & buffer[offset+3]);

        return value;
    }

    /**
     *  Convert an integer to bytes.
     */
    public static byte[] intToBytes(int val)
    {
        byte[] buffer = new byte[4];

        intIntoBytes(val, buffer, 0);

        return buffer;
    }

    /**
     *  Convert a long to bytes.
     */
    public static void intIntoBytes(int val, byte[] buffer, int offset)
    {
        buffer[offset++] = (byte)((val >>> 24) & 0xFF);
        buffer[offset++] = (byte)((val >>> 16) & 0xFF);
        buffer[offset++] = (byte)((val >>> 8) & 0xFF);
        buffer[offset++] = (byte)(val & 0xFF);
    }

    /**
     *  Convert bytes to a long.
     */
    public static long bytesToLong(byte[] buffer)
    {
        assert buffer.length == 8;

        long value = (0xFFL & buffer[0]) << 56L;
        value |= (0xFFL & buffer[1]) << 48L;
        value |= (0xFFL & buffer[2]) << 40L;
        value |= (0xFFL & buffer[3]) << 32L;
        value |= (0xFFL & buffer[4]) << 24L;
        value |= (0xFFL & buffer[5]) << 16L;
        value |= (0xFFL & buffer[6]) << 8L;
        value |= (0xFFL & buffer[7]);

        return value;
    }

    /**
     *  Convert bytes to a long.
     */
    public static long bytesToLong(byte[] buffer, int offset)
    {
        long value = (0xFFL & buffer[offset++]) << 56L;
        value |= (0xFFL & buffer[offset++]) << 48L;
        value |= (0xFFL & buffer[offset++]) << 40L;
        value |= (0xFFL & buffer[offset++]) << 32L;
        value |= (0xFFL & buffer[offset++]) << 24L;
        value |= (0xFFL & buffer[offset++]) << 16L;
        value |= (0xFFL & buffer[offset++]) << 8L;
        value |= (0xFFL & buffer[offset++]);

        return value;
    }

    /**
     *  Convert a long to bytes.
     */
    public static byte[] longToBytes(long val)
    {
        byte[] buffer = new byte[8];

        buffer[0] = (byte)((val >>> 56) & 0xFF);
        buffer[1] = (byte)((val >>> 48) & 0xFF);
        buffer[2] = (byte)((val >>> 40) & 0xFF);
        buffer[3] = (byte)((val >>> 32) & 0xFF);
        buffer[4] = (byte)((val >>> 24) & 0xFF);
        buffer[5] = (byte)((val >>> 16) & 0xFF);
        buffer[6] = (byte)((val >>> 8) & 0xFF);
        buffer[7] = (byte)(val & 0xFF);

        return buffer;
    }

    /**
     *  Convert a long to bytes.
     */
    public static void longIntoBytes(long val, byte[] buffer)
    {
        longIntoBytes(val, buffer, 0);
    }

    /**
     *  Convert a long to bytes.
     */
    public static void longIntoBytes(long val, byte[] buffer, int offset)
    {
        buffer[offset++] = (byte)((val >>> 56) & 0xFF);
        buffer[offset++] = (byte)((val >>> 48) & 0xFF);
        buffer[offset++] = (byte)((val >>> 40) & 0xFF);
        buffer[offset++] = (byte)((val >>> 32) & 0xFF);
        buffer[offset++] = (byte)((val >>> 24) & 0xFF);
        buffer[offset++] = (byte)((val >>> 16) & 0xFF);
        buffer[offset++] = (byte)((val >>> 8) & 0xFF);
        buffer[offset++] = (byte)(val & 0xFF);
    }

    /**
     *  Calculates the size of a String if encoded with UTF-8.
     *  Uses an optimized, non-branching approach for speed.
     *
     *  @param string The input string.
     *  @return The number of bytes the String uses if encoded with UTF-8.
     */
    public static final int stringSizeAsUtf8(String string)
    {
        int bytes = string.codePoints().map(codepoint ->
        {
            // Calculate the UTF-8 size using bit manipulation

            int result = 1; // At least one byte.

            // Check each additional range using bitwise operations
            int tmp = codepoint & 0xFFFFFF80; // Check for 2 bytes
            // Normally we would have to user (tmp | -tmp) >>> 31,
            // but all Unicode values are positive, so we can use this
            result += -tmp >>> 31;    // Increment if non-zero

            tmp = codepoint & 0xFFFFF800; // Check for 3 bytes
            result += -tmp >>> 31;    // Increment if non-zero

            tmp = codepoint & 0xFFFF0000; // Check for 4 bytes
            result += -tmp >>> 31;    // Increment if non-zero

            return result;
        }).sum();

        return bytes;
    }

    /**
     *  Converts a String to a byte array prefixed
     *  by a big-endian encoded integer length followed by
     *  the UTF8-encoded string data.
     *
     *  @param string The input String.
     *  @return Byte array prefixed by 4 bytes encoding the length of the string.
     */
    public static byte[] stringToByteArray(String string)
    {
        // Calculate size of the string in UTF8.
        int utf8size = stringSizeAsUtf8(string);

        // Allocate final array of the right size include prefixed length integer.
        byte[] result = new byte[utf8size + 4];

        stringIntoByteArray(string, utf8size, result, 0);

        return result;
    }

    /**
     *  Writes a String to a byte array prefixed
     *  by a big-endian encoded integer length followed by
     *  the UTF8-encoded string data.
     *  The array must be of sufficient size to accomodate the string.
     *  Use stringSizeAsUtf8() to precalculate and add 4 for the length integer,
     *  then consider the public static void stringToByteArray(String string, byte[] output, int offset, int utf8size)
     *  method.
     *
     *  @param string The input String.
     *  @param output The output array.
     *  @param offset Offset in the output array.
     */
    public static void stringIntoByteArray(String string, byte[] output, int offset)
    {
        int utf8size = stringSizeAsUtf8(string);
        stringIntoByteArray(string, utf8size, output, offset);
    }

    /**
     *  Writes a String to a byte array prefixed
     *  by a big-endian encoded integer length followed by
     *  the UTF8-encoded string data.
     *  The array must be of sufficient size to accomodate the string.
     *  User stringSizeAsUtf8() to precalculate and add 4 for the length integer.
     *
     *  @param string The input String.
     *  @param utf8size Size of the string as UTF8.
     *  @param output The output array.
     *  @param offset Offset in the output array.
     *
     */
    public static void stringIntoByteArray(String string, int utf8size, byte[] output, int offset)
    {
        // Encode String length into result
        intIntoBytes(utf8size, output, offset);

        ByteBuffer resultbuffer = ByteBuffer.wrap(output, offset + 4, utf8size);
        CharsetEncoder enc = SUtil.UTF8.newEncoder();
        enc.encode(CharBuffer.wrap(string), resultbuffer, true);
    }

    /**
     *  Converts part of a byte array that was encoded by
     *  stringToByteArray(), or one of the
     *  stringIntoByteArray() back into a String.
     *
     *  @param input The input array.
     *  @param offset Offset in the input array.
     *  @return Decoded String.
     */
    public static String byteArrayToString(byte[] input, int offset)
    {
        int strlen = bytesToInt(input, offset);
        return new String(input, 4, strlen, SUtil.UTF8);
    }

    /**
     *
     */
    protected static void testIntByteConversion()
    {
        Random rnd	= new Random(123);
        for(int i=1; i<10000000; i++)
        {
            int	val	= rnd.nextInt(Integer.MAX_VALUE);
            if(i%2==0)	// Test negative values too.
            {
                val	= -val;
            }
            byte[]	bytes	= SBinConv.intToBytes(val);
            int	val2	= SBinConv.bytesToInt(bytes);
            if(val!=val2)
            {
                throw new RuntimeException("Failed: "+val+", "+val2+", "+SUtil.arrayToString(bytes));
            }
//			System.out.println("Converted: "+val+", "+arrayToString(bytes));
        }
    }

    /**
     *  Primitive encoding approach: Merges multiple byte arrays
     *  into a single one so it can be split later.
     *
     *  @param data The input data.
     *  @return A merged byte array.
     */
    public static byte[] mergeData(byte[]... data)
    {
        int datasize = 0;
        for (int i = 0; i < data.length; ++i)
            datasize += data[i].length;
        byte[] ret = new byte[datasize + (data.length << 2)];
        int offset = 0;
        for (int i = 0; i < data.length; ++i)
        {
            intIntoBytes(data[i].length, ret, offset);
            offset += 4;
            System.arraycopy(data[i], 0, ret, offset, data[i].length);
            offset += data[i].length;
        }
        return ret;
    }

    /**
     *  Primitive encoding approach: Splits a byte array
     *  that was encoded with mergeData().
     *
     *  @param data The input data.
     *  @return A list of byte arrays representing the original set.
     */
    public static List<byte[]> splitData(byte[] data)
    {
        return splitData(data, -1, -1);
    }

    /**
     *  Primitive encoding approach: Splits a byte array
     *  that was encoded with mergeData().
     *
     *  @param data The input data.
     *  @param offset Offset where the data is located.
     *  @param length Length of the data, rest of data used if length < 0.
     *  @return A list of byte arrays representing the original set.
     */
    public static List<byte[]> splitData(byte[] data, int offset, int length)
    {
        List<byte[]> ret = new ArrayList<byte[]>();
        offset = offset < 0 ? 0 : offset;
        length = length < 0 ? data.length - offset : length;
        int endpos = offset + length;
        while (offset < endpos)
        {
            int datalen = bytesToInt(data, offset);
            offset += 4;
            if (offset + datalen > endpos)
                throw new IllegalArgumentException("Invalid encoded data.");
            byte[] datapart = new byte[datalen];
            System.arraycopy(data, offset, datapart, 0, datalen);
            offset += datalen;
            ret.add(datapart);
        }
        return ret;
    }
}
