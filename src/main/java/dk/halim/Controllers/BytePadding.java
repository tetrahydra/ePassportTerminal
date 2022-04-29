package dk.halim.Controllers;

import javax.crypto.BadPaddingException;
import java.io.ByteArrayOutputStream;

/**
 * Original code https://github.com/tkaczenko/cardreader/blob/master/jmrtd/src/main/java/org/jmrtd/Util.java
 * */

public class BytePadding {

    /**
     * Pads the input <code>in</code> according to ISO9797-1 padding method 2,
     * using the given block size.
     *
     * @param in input
     * @param blockSize the block size
     *
     * @return padded bytes
     */
    public static byte[] pad(byte[] in, int blockSize) {
        return pad(in, 0, in.length, blockSize);
    }

    /**
     * Pads the input {@code bytes} indicated by {@code offset} and {@code length}
     * according to ISO9797-1 padding method 2, using the given block size in {@code blockSize}.
     *
     * @param bytes input
     * @param offset the offset
     * @param length the length
     * @param blockSize the block size
     *
     * @return padded bytes
     */
    public static byte[] pad(byte[] bytes, int offset, int length, int blockSize) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(bytes, offset, length);
        outputStream.write((byte)0x80);
        while (outputStream.size() % blockSize != 0) {
            outputStream.write((byte)0x00);
        }
        return outputStream.toByteArray();
    }

    /**
     * Unpads the input {@code bytes} according to ISO9797-1 padding method 2.
     *
     * @param bytes the input
     *
     * @return the unpadded bytes
     *
     * @throws BadPaddingException on padding exception
     */
    public static byte[] unpad(byte[] bytes) throws BadPaddingException {
        int i = bytes.length - 1;
        while (i >= 0 && bytes[i] == 0x00) {
            i--;
        }
        if ((bytes[i] & 0xFF) != 0x80) {
            throw new BadPaddingException("Expected constant 0x80, found 0x" + Integer.toHexString((bytes[i] & 0x000000FF)));
        }
        byte[] out = new byte[i];
        System.arraycopy(bytes, 0, out, 0, i);
        return out;
    }

}
