package dk.halim.Controllers;

/**
 * Sourced from https://gist.github.com/ar/791674
 * */
public class Parity {

    /**
     * DES Keys use the LSB as the odd parity bit. This method can be used enforce correct parity.
     *
     * @param bytes the byte array to set the odd parity on.
     */
    public static void adjustDESParity (byte[] bytes) {
        for (int i = 0; i < bytes.length; i++) {
            int b = bytes[i];
            bytes[i] = (byte)((b & 0xfe) | ((((b >> 1) ^ (b >> 2) ^ (b >> 3) ^ (b >> 4) ^ (b >> 5) ^ (b >> 6) ^ (b >> 7)) ^ 0x01) & 0x01));
        }
    }

    /**
     * DES Keys use the LSB as the odd parity bit. This method checks whether the parity is adjusted or not
     *
     * @param bytes the byte[] to be checked
     * @return true if parity is adjusted else returns false
     */
    public static byte[] isDESParityAdjusted (byte[] bytes) {
        byte[] correct = (byte[])bytes.clone();
        adjustDESParity(correct);
        // return  Arrays.equals(bytes, correct);
        /*
        * Halim: Corrected over here as I need the corrected value, not only to check whether
        *        the input is correct or not.
        * */
        return correct;
    }
}