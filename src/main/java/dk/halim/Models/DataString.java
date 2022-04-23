package dk.halim.Models;

import dk.halim.Controllers.Operations;
import org.bouncycastle.util.encoders.Hex;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class DataString {

    String data;
    byte[] dataByte;

    String encrypted;
    byte[] encryptedByte;

    String decrypted;
    byte[] decryptedByte;

    String MAC;
    byte[] MACByte;

    boolean verified = false;

    public void setData(String data) {
        this.data = data;
        this.dataByte = Hex.decode(this.data);
    }

    public String getData(){
        return this.data;
    }

    public byte[] getDataByte(){
        return this.dataByte;
    }

    public void setVerified(boolean flag){
        this.verified = flag;
    }

    public boolean getVerified(){
        return this.verified;
    }

    public void encrypt(byte[] key, byte[] mac) throws Exception {
        setEncrypted(Operations.encrypt(this.dataByte, key));
        mac(mac);
    }

    public void setEncrypted(byte[] data){
        if (data != null && data.length > 0) {
            this.encryptedByte = data;
            this.encrypted = Hex.toHexString(this.encryptedByte).toUpperCase();
        }
    }

    public String getEncrypted(){
        return this.encrypted;
    }

    public byte[] getEncryptedByte(){
        return this.encryptedByte;
    }

    public void mac(byte[] mac) throws NoSuchAlgorithmException, InvalidKeyException {
        this.MACByte = Operations.calculate_mac(getEncryptedByte(), mac);
        this.MAC = Hex.toHexString(this.MACByte).toUpperCase();
    }

    public String getMAC(){
        return this.MAC;
    }

    public byte[] getMACByte(){
        return this.MACByte;
    }

    /*
    * Decryption
    * */

    public void decrypt(byte[] key) throws Exception {
        this.decryptedByte = Operations.decrypt(getEncryptedByte(), key);
        this.decrypted = Hex.toHexString(this.decryptedByte).toUpperCase();
    }

    public String getDecrypted(){
        return this.decrypted;
    }

    public byte[] getDecryptedByte(){
        return this.decryptedByte;
    }

}
