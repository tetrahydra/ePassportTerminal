package dk.halim.Models;

import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Hex;

public class RAPDU {

    String response;
    byte[] responseByte;

    byte[] DO8E_Byte;

    byte[] DO87_Byte;

    byte[] DO99_Byte;

    byte[] serialResponseByte;
    byte[] SW1SW2_Byte;

    boolean verified = false;

    public void setResponse(String data) {
        if (data != null && data.length() > 0) {
            this.response = data;
            this.responseByte = Hex.decode(data);
        }
    }

    public String getResponse(){
        return this.response;
    }

    public byte[] getResponseByte(){
        return this.responseByte;
    }

    public String getSW1SW2(){
        this.SW1SW2_Byte = Arrays.copyOfRange(getResponseByte(), getResponseByte().length - 2, getResponseByte().length);
        if (this.SW1SW2_Byte.length > 0) {
            return Hex.toHexString(this.SW1SW2_Byte).toUpperCase();
        }

        return null;
    }

    public String getSerialResponse(){
        this.serialResponseByte = Arrays.copyOfRange(getResponseByte(), 0, getResponseByte().length - 2);
        if (this.serialResponseByte.length > 0) {
            return Hex.toHexString(this.serialResponseByte).toUpperCase();
        }

        return null;
    }

    public String getDO8E(){
        this.DO8E_Byte = Arrays.copyOfRange(getResponseByte(), getResponseByte().length - 10, getResponseByte().length - 2);
        if (this.DO8E_Byte.length > 0) {
            return Hex.toHexString(this.DO8E_Byte).toUpperCase();
        }

        return null;
    }

    public String getDO87(){
        this.DO87_Byte = Arrays.copyOfRange(getResponseByte(), 0, getResponseByte().length - 16);
        if (this.DO87_Byte.length > 0) {
            return Hex.toHexString(this.DO87_Byte).toUpperCase();
        }

        return null;
    }

    public byte[] getDO87Byte(){
        this.DO87_Byte = Arrays.copyOfRange(getResponseByte(), 0, getResponseByte().length - 16);
        if (this.DO87_Byte.length > 0) {
            return this.DO87_Byte;
        }

        return null;
    }

    public String getDO99(){
        this.DO99_Byte = Arrays.copyOfRange(getResponseByte(), getResponseByte().length - 16, getResponseByte().length - 12);
        if (this.DO99_Byte.length > 0) {
            return Hex.toHexString(this.DO99_Byte).toUpperCase();
        }

        return null;
    }

    public void setVerified(boolean data) {
        this.verified = data;
    }

    public boolean getVerified(){
        return this.verified;
    }

}