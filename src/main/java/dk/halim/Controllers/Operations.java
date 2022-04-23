package dk.halim.Controllers;

import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Hex;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Operations {

    final static String DATE_FORMAT = "dd/MM/yyyy";

    public Operations() throws NoSuchAlgorithmException, NoSuchProviderException {
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    public static String bytesToHexString(byte[] bytes){
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes){
            sb.append(String.format("%02x", b&0xff));
        }
        return sb.toString();
    }

    public static String hexToAscii(String hexStr) {
        StringBuilder output = new StringBuilder("");

        for (int i = 0; i < hexStr.length(); i += 2) {
            String str = hexStr.substring(i, i + 2);
            output.append((char) Integer.parseInt(str, 16));
        }

        return output.toString();
    }

    /**
     * Returns the document number, including trailing '<' until length 9.
     *
     * @param documentNumber the original document number
     * @return the documentNumber with at least length 9
     * <p>
     * Sourced from
     * https://github.com/tkaczenko/cardreader/blob/master/jmrtd/src/main/java/org/jmrtd/protocol/BACProtocol.java
     */
    public static String fixDocumentNumber(String documentNumber) {
        StringBuilder maxDocumentNumber = new StringBuilder(documentNumber == null ? "" : documentNumber.replace('<', ' ').trim().replace(' ', '<'));
        while (maxDocumentNumber.length() < 9) {
            maxDocumentNumber.append('<');
        }
        return maxDocumentNumber.toString();
    }

    public static int checkDigit(String data) {
        int checkDigit = 0;
        int value;

        int[] MRZ_WEIGHT = {7, 3, 1};

        for (int i = 0; i < data.length(); i++) {
            char temp = data.charAt(i);

            if (Character.isUpperCase(temp)) {
                value = (int) temp - 55;
            } else if (temp == '<') {
                value = 0;
            } else {
                value = Integer.parseInt(Character.toString(temp));
            }

            checkDigit += value * MRZ_WEIGHT[i % 3];
        }

        return (checkDigit % 10);
    }

    public static boolean isValidDate(final String date) {

        try {
            DateFormat df = new SimpleDateFormat(DATE_FORMAT);
            df.setLenient(false);
            df.parse(date);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    // Convert date to truncated representations (ISO 8601:2000) YYMMDD
    // https://en.wikipedia.org/wiki/ISO_8601
    public static String formatDateISO8601(final String date) {

        try {
            DateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
            Date sdfDate = sdf.parse(date);

            DateFormat sdfConversion = new SimpleDateFormat("yyMMdd");
            return sdfConversion.format(sdfDate);
        } catch (ParseException e) {
            return "";
        }
    }

    public static byte[] Calculate_Session_Key(byte[] input, int Mode) throws Exception {

        MessageDigest mda = MessageDigest.getInstance("SHA-1", "BC");

        byte[] add1Byte = {};

        switch (Mode) {
            case 1: add1Byte = Hex.decode("00000001"); break;
            case 2: add1Byte = Hex.decode("00000002"); break;
        }

        byte[] keyEncByte = new byte[input.length + add1Byte.length];
        System.arraycopy(input, 0, keyEncByte, 0, input.length);
        System.arraycopy(add1Byte, 0, keyEncByte, input.length, add1Byte.length);

        byte [] keyEnc = mda.digest(keyEncByte);

        keyEnc = Arrays.copyOfRange(keyEnc, 0, 16);

        byte[] keyEnc_a = Arrays.copyOfRange(keyEnc, 0, 8);
        byte[] keyEnc_b = Arrays.copyOfRange(keyEnc, 8, 16);

        byte[] keyEnc_a_adj = Parity.isDESParityAdjusted(keyEnc_a);
        byte[] keyEnc_b_adj = Parity.isDESParityAdjusted(keyEnc_b);

        String keyEnc_a_adj_hex = Hex.toHexString(keyEnc_a_adj).toUpperCase();
        String keyEnc_b_adj_hex = Hex.toHexString(keyEnc_b_adj).toUpperCase();

        String keyEncHex = keyEnc_a_adj_hex + keyEnc_b_adj_hex;
        byte[] keyEncFinal = Hex.decode(keyEncHex);

        return keyEncFinal;

    }

    /*
     * To do : encrypt using 3DES algorithm
     */
    public static byte[] encrypt(byte[] input, byte[] keyBytes) throws Exception {

        /** Initialization vector */
        IvParameterSpec ZERO_IV_PARAM_SPEC = new IvParameterSpec(new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 });

        SecretKey originalKey = new SecretKeySpec(keyBytes, 0, keyBytes.length, "DESede");

        Cipher cipher = Cipher.getInstance("DESede/CBC/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, originalKey, ZERO_IV_PARAM_SPEC);

        final byte[] encrypted = cipher.doFinal(input);

        return encrypted;

    }

    /*
     * To do : decrypt using 3DES algorithm
     */
    public static byte[] decrypt(byte[] input, byte[] keyBytes) throws Exception {

        /** Initialization vector */
        IvParameterSpec ZERO_IV_PARAM_SPEC = new IvParameterSpec(new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 });

        SecretKey originalKey = new SecretKeySpec(keyBytes, 0, keyBytes.length, "DESede");

        Cipher cipher = Cipher.getInstance("DESede/CBC/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, originalKey, ZERO_IV_PARAM_SPEC);

        final byte[] decrypted = cipher.doFinal(input);

        return decrypted;

    }

    public static byte[] calculate_mac(byte[] enc, byte[] keyMac) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("ISO9797Alg3Mac");

        SecretKey keyMac_ = new SecretKeySpec(keyMac, 0, keyMac.length, "DESede");
        mac.init(keyMac_);
        byte[] macResult = mac.doFinal(BytePadding.pad(enc, 8));

        return macResult;
    }

    /**
     * Computes the initial send sequence counter to use,
     * given the randoms generated by PICC and PCD.
     *
     * @param rndICC the PICC's random
     * @param rndIFD the PCD's random
     */
    public static byte[] computeSendSequenceCounter(byte[] rndICC, byte[] rndIFD) {
        byte[] A = Arrays.copyOfRange(rndICC, 4, 8);
        byte[] B = Arrays.copyOfRange(rndIFD, 4, 8);

        byte[] SSC = new byte[A.length + B.length];
        System.arraycopy(A, 0, SSC, 0, A.length);
        System.arraycopy(B, 0, SSC, A.length, B.length);

        return SSC;
    }

    public static String incrementSSC(String SSC){
        BigInteger value = new BigInteger(SSC, 16);
        value = value.add(BigInteger.ONE);
        return value.toString(16).toUpperCase();
    }

    public static String buildDO87(String data) {
        String cipher = "01" + data;
        byte[] cByte = Hex.decode(cipher);
        int cByteLen = cByte.length;
        String DO87 = "87" + toAsn1Length(cByteLen) + cipher;

        return DO87;
    }

    public static String buildDO8E(String data) {
        byte[] cByte = Hex.decode(data);
        String DO8E = "8E" + String.format("%02X", cByte.length) + data;

        return DO8E;
    }

    public static String buildDO97(String Le) {
        String DO97 = "9701" + Le;
        return DO97;
    }

    /*
     * 2022-03-15 Converted from Python to Java.
     * Inspired from the original code at https://github.com/andrew867/epassportviewer/blob/master/pypassport-2.0/pypassport/asn1.py
     * */
    public static String toAsn1Length(int data) {

        if (data <= Long.parseLong("7F", 16)) {
            return Hex.toHexString(BigInteger.valueOf(data).toByteArray());
        }

        if (data <= Long.parseLong("80", 16) && data <= Long.parseLong("FF", 16)) {
            return "81" + Hex.toHexString(BigInteger.valueOf(data).toByteArray());
        }

        if (data <= Long.parseLong("0100", 16) && data <= Long.parseLong("FFFF", 16)) {
            return "82" + Hex.toHexString(BigInteger.valueOf(data).toByteArray());
        }

        return "";

    }

    /** Return XOR of two byte array of different or same size. */
    // https://stackoverflow.com/questions/13999351/java-create-a-byte-by-xor-2-bytes
    public static byte[] xor(byte[] data1, byte[] data2) {
        // make data2 the largest...
        if (data1.length > data2.length) {
            byte[] tmp = data2;
            data2 = data1;
            data1 = tmp;
        }
        for (int i = 0; i < data1.length; i++) {
            data2[i] ^= data1[i];
        }
        return data2;
    }

}
