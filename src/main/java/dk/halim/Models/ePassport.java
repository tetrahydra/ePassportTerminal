package dk.halim.Models;

import dk.halim.Controllers.*;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Hex;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;

public class ePassport {

    String documentNumber;
    String dateOfBirth;
    String dateofExpiry;

    String MRZ;

    String keySeed;
    byte[] keySeedByte;

    String keySeedEnc;
    byte[] keySeedEncByte;

    String keySeedMAC;
    byte[] keySeedMACByte;

    String rndIFD;
    byte[] rndIFDByte;

    String rndKeyIFD;
    byte[] rndKeyIFDByte;

    String rndICC;
    byte[] rndICCByte;

    String ChipAuthenticationData;
    byte[] ChipAuthenticationDataByte;
    String ChipAuthenticationMAC;
    byte[] ChipAuthenticationMACByte;
    String ChipAuthenticationSW1SW2;

    String keyIC;
    byte[] keyICByte;

    String keyICSeed;
    byte[] keyICSeedByte;

    String keyICSeedEnc;
    byte[] keyICSeedEncByte;

    String keyICSeedMAC;
    byte[] keyICSeedMACByte;

    String SSC;
    byte[] SSCByte;

    EF_DG1 DG1 = new EF_DG1();
    EF_DG2 DG2 = new EF_DG2();

    public void setDocumentNumber(String data){
        this.documentNumber = data;
    }

    public String getDocumentNumber(){
        return this.documentNumber;
    }

    public void setDateOfBirth(String data){
        this.dateOfBirth = data;
    }

    public String getDateOfBirth(){
        return this.dateOfBirth;
    }

    public void setDateofExpiry(String data){
        this.dateofExpiry = data;
    }

    public String getDateofExpiry(){
        return this.dateofExpiry;
    }

    public void setMRZ(String data){
        this.MRZ = data;
    }

    public String getMRZ(){
        return this.MRZ;
    }

    /*
    * MRZ = documentNumber + checkDigit + finalDateOfBirth + checkDigit + finalDateOfExpiry + checkDigit
    * */
    public void generateMRZ(){
        setMRZ(documentNumber + Operations.checkDigit(documentNumber)
                + getDateOfBirth() + Operations.checkDigit(getDateOfBirth())
                + getDateofExpiry() + Operations.checkDigit(getDateofExpiry()));
    }

    public void setKeySeed(byte[] data){
        this.keySeedByte = data;

        // Take first 16 bytes
        this.keySeedByte = Arrays.copyOfRange(this.keySeedByte, 0, 16);
        this.keySeed = Hex.toHexString(this.keySeedByte).toUpperCase();
    }

    public void generateKeySeed() throws NoSuchAlgorithmException, NoSuchProviderException {
        MessageDigest mda = MessageDigest.getInstance("SHA-1", "BC");
        byte[] keySeedByte = mda.digest(getMRZ().getBytes());
        setKeySeed(keySeedByte);
    }

    public String getKeySeed(){
        return this.keySeed;
    }

    public byte[] getKeySeedByte(){
        return this.keySeedByte;
    }

    // Calculate session keys (KSEnc) according to Section 9.7.1/Appendix D.1:
    public void generateKeySeedEnc() throws Exception {
        this.keySeedEncByte = Operations.Calculate_Session_Key(getKeySeedByte(), 1);
        this.keySeedEnc = Hex.toHexString(this.keySeedEncByte).toUpperCase();
    }

    public String getKeySeedEnc(){
        return this.keySeedEnc;
    }

    public byte[] getKeySeedEncByte(){
        return this.keySeedEncByte;
    }

    // Calculate session keys (KSMAC) according to Section 9.7.1/Appendix D.1:
    public void generateKeySeedMAC() throws Exception {
        this.keySeedMACByte = Operations.Calculate_Session_Key(getKeySeedByte(), 2);
        this.keySeedMAC = Hex.toHexString(this.keySeedMACByte).toUpperCase();
    }

    public String getKeySeedMAC(){
        return this.keySeedMAC;
    }

    public byte[] getKeySeedMACByte(){
        return this.keySeedMACByte;
    }

    /*
     * rnd.IFD is random 8 bits
     * Pg 89/150, D.3(2)
     * Generate an 8 byte random (rnd.ifd) and a 16 byte random (kifd)
     * */

    public void generateRandomIFD(){
        this.rndIFDByte = new SecureRandom().generateSeed(8);
        this.rndIFD = Hex.toHexString(this.rndIFDByte).toUpperCase();

        this.rndKeyIFDByte = new SecureRandom().generateSeed(16);
        this.rndKeyIFD = Hex.toHexString(this.rndKeyIFDByte).toUpperCase();
    }

    public void setRndIFD(String data){
        this.rndIFD = data;
        this.rndIFDByte = Hex.decode(this.rndIFD);
    }

    public void setRndKeyIFD(String data){
        this.rndKeyIFD = data;
        this.rndKeyIFDByte = Hex.decode((this.rndKeyIFD));
    }

    public String getRndIFD(){
        return this.rndIFD;
    }

    public byte[] getRndIFDByte(){
        return this.rndIFDByte;
    }

    public String getRndKeyIFD(){
        return this.rndKeyIFD;
    }

    public byte[] getRndKeyIFDByte(){
        return this.rndKeyIFDByte;
    }

    public void setRndICC(String data){
        if (data.length() > 0) {
            this.rndICC = data;
            this.rndICCByte = Hex.decode(this.rndICC);
        }
    }

    public String getRndICC(){
        return this.rndICC;
    }

    public byte[] getRndICCByte(){
        return this.rndICCByte;
    }

    public void setChipAuthenticationData(String data) {

        if (data != null) {
            byte[] data_temp = Hex.decode(data);
            byte[] temp = Arrays.copyOfRange(data_temp, 0, 32);
            byte[] expected_MAC = Arrays.copyOfRange(data_temp, 32, data_temp.length);

            this.ChipAuthenticationData = Hex.toHexString(temp).toUpperCase();
            this.ChipAuthenticationDataByte = temp;

            this.ChipAuthenticationMAC = Hex.toHexString(expected_MAC).toUpperCase();
            this.ChipAuthenticationMACByte = expected_MAC;
        }

    }

    public String getChipAuthenticationData(){
        return this.ChipAuthenticationData;
    }

    public byte[] getChipAuthenticationDataByte(){
        return this.ChipAuthenticationDataByte;
    }

    public String getChipAuthenticationMAC(){
        return this.ChipAuthenticationMAC;
    }

    public byte[] getChipAuthenticationMACByte(){
        return this.ChipAuthenticationMACByte;
    }

    public void setChipAuthenticationSW1SW2(String data){
        this.ChipAuthenticationSW1SW2 = data;
    }

    public String getChipAuthenticationSW1SW2(){
        return  this.ChipAuthenticationSW1SW2;
    }

    // Page 90/150: Generate a 16-byte random
    public void generateKeyIC(String data){
        if (data.length() > 0) {
            this.keyICByte = Arrays.copyOfRange(Hex.decode(data), 0, 16);;
            this.keyIC = Hex.toHexString(this.keyICByte).toUpperCase();
        } else {
            this.keyICByte = new SecureRandom().generateSeed(16);
            this.keyIC = Hex.toHexString(this.keyICByte).toUpperCase();
        }
    }

    public void setKeyIC(String data) {
        this.keyIC = data;
        this.keyICByte = Hex.decode(this.keyIC);
    }

    public String getKeyIC(){
        return this.keyIC;
    }

    public byte[] getKeyICByte(){
        return this.keyICByte;
    }

    // Calculate XOR of keyIFD and K_IC:
    public void generateKeyICSeed(){
        this.keyICSeedByte = Operations.xor(getRndKeyIFDByte(), getKeyICByte());
        this.keyICSeed = Hex.toHexString(this.keyICSeedByte).toUpperCase();
    }

    // Take first 16 bytes
    public void updateKeyICSeed(){
        this.keyICSeedByte = Arrays.copyOfRange(getKeyICSeedByte(), 0, 16);
        this.keyICSeed = Hex.toHexString(this.keyICSeedByte).toUpperCase();
    }

    public String getKeyICSeed(){
        return this.keyICSeed;
    }

    public byte[] getKeyICSeedByte(){
        return this.keyICSeedByte;
    }

    // Calculate session keys (KSEnc) according to Section 9.7.1/Appendix D.1:
    public void generateKeyICSeedEnc() throws Exception {
        this.keyICSeedEncByte = Operations.Calculate_Session_Key(getKeyICSeedByte(), 1);
        this.keyICSeedEnc = Hex.toHexString(this.keyICSeedEncByte).toUpperCase();
    }

    public String getKeyICSeedEnc(){
        return this.keyICSeedEnc;
    }

    public byte[] getKeyICSeedEncByte(){
        return this.keyICSeedEncByte;
    }

    public void generateKeyICSeedMAC() throws Exception {
        this.keyICSeedMACByte = Operations.Calculate_Session_Key(getKeyICSeedByte(), 2);
        this.keyICSeedMAC = Hex.toHexString(this.keyICSeedMACByte).toUpperCase();
    }

    public String getKeyICSeedMAC(){
        return this.keyICSeedMAC;
    }

    public byte[] getKeyICSeedMACByte(){
        return this.keyICSeedMACByte;
    }

    /*
    * Page 76/150
    * 9.8.6.3 Send Sequence Counter
      For Secure Messaging following BAC, the Send Sequence Counter SHALL be initialized by concatenating the four least
      significant bytes of RND.IC and RND.IFD, respectively:
    * */
    public void initializeSSC(){
        this.SSCByte = Operations.computeSendSequenceCounter(getRndICCByte(), getRndIFDByte());
        this.SSC = Hex.toHexString(this.SSCByte).toUpperCase();
    }

    public void incrementSSC(){
        this.SSC = Operations.incrementSSC(getSSC());
        this.SSCByte = Hex.decode(this.SSC);
    }

    public String getSSC(){
        return this.SSC;
    }

    public byte[] getSSCByte(){
        return this.SSCByte;
    }

    public void setDG1(EF_DG1 DG1) {
        this.DG1 = DG1;
    }

    public EF_DG1 getDG1() {
        return this.DG1;
    }

    public void setDG2(EF_DG2 DG2) {
        this.DG2 = DG2;
    }

    public EF_DG2 getDG2(){
        return this.DG2;
    }

}
