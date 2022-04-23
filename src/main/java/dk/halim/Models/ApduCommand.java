package dk.halim.Models;

import dk.halim.Controllers.BytePadding;
import dk.halim.Controllers.Operations;
import org.bouncycastle.util.encoders.Hex;

public class ApduCommand {

    /**
     * From: https://stackoverflow.com/questions/30550899/what-is-the-structure-of-an-application-protocol-data-unit-apdu-command-and-re
     *
     * APDU commands are a queue of binary numbers in the following form:
     *
     * CLA | INS | P1 | P2 | Lc | CData | Le
     *
     * The first four sections, i.e CLA , INS , P1 and P2 are mandatory in all APDU commands and each one has one byte length. These one-byte-length sections stand for Class, Instruction, Parameter1 and Parameter2 respectively.
     *
     * The last three sections, i.e Lc , CData and Le are optional.Lc is the encoding of Nc, which is the encoding of the length of the CDATA field. Le is the encoding of Ne, then encoding of the maximum response data that may be send. Based on presence or absence of these sections, we have 4 case for APDU commands, as below:
     *
     * Case1: CLA | INS | P1 | P2
     * Case2: CLA | INS | P1 | P2 | Le
     * Case3: CLA | INS | P1 | P2 | Lc | Data
     * Case4: CLA | INS | P1 | P2 | Lc | Data | Le
     * The length of CData is different for different commands and different applets. based on the length of CData (i.e Lc) and the length of maximum response data that may send (i.e Le), we have to type of APDU commands:
     *
     * Normal/Short APDU commands, when Lc and Le are smaller than 0xFF
     * Extended length APDU commands, when Lc and/or Le are greater than 0xFF.
     * So for the length of these sections we have:
     *
     * Lc : 1 byte for Short APDU commands and 3 byte (they specify this length, because its enough) for Extended APDU commands.
     *
     * Data : Different lengths.
     *
     * Le : Same as Lc.
     * */

    String CLA = "";
    String INS = "";
    String P1 = "";
    String P2 = "";
    String LC = "";
    String data = "";
    String LE = "";
    String LE97 = "";

    String DO8E = "";

    public ApduCommand(String CLA, String INS, String P1, String P2, String LC, String data, String LE) {
        if (CLA != null || CLA.length() > 0){
            this.CLA = CLA;
        }

        if (INS != null || INS.length() > 0){
            this.INS = INS;
        }

        if (P1 != null || P1.length() > 0){
            this.P1 = P1;
        }

        if (P2 != null || P2.length() > 0){
            this.P2 = P2;
        }

        if (LC != null || LC.length() > 0){
            this.LC = LC;
        }

        if (data != null || data.length() > 0){
            this.data = data;
        }

        if (LE != null || LE.length() > 0){
            this.LE = LE;
        }
    }

    public void setLE97(String data){
        this.LE97 = data;
    }

    public String getCmdHeader(){
        return this.CLA + this.INS + this.P1 + this.P2;
    }

    public String getCmdHeaderPadded(){
        byte[] CmdHeaderByte = Hex.decode(getCmdHeader());
        CmdHeaderByte = BytePadding.pad(CmdHeaderByte, 8);
        return Hex.toHexString(CmdHeaderByte).toUpperCase();
    }

    public void setData(String data){
        this.data = data;
    }

    public String getData(){
        return this.data;
    }

    public String getDataPadded(){
        byte[] DataFieldByte = Hex.decode(getData());
        DataFieldByte = BytePadding.pad(DataFieldByte, 8);
        return Hex.toHexString(DataFieldByte).toUpperCase();
    }

    public void setDO8E(String data){
        this.DO8E = Operations.buildDO8E(data);
    }

    public String getDO8E(){
        return this.DO8E;
    }

    public String getDO87(){
        return Operations.buildDO87(getData());
    }

    public String getDO97(){
        return Operations.buildDO97(this.LE97);
    }

    public String getM(String DO){
        switch (DO) {
            case "DO97":
                return getCmdHeaderPadded() + getDO97();
            case "DO87":
                return getCmdHeaderPadded() + getDO87();
        }
        return "";
    }

    /**
     * In the reference (pg 92/150), 1.f.ii -> Concatenate SSC and M and add padding.
     * 2022-03-20 But tested using real document, it failed but when NOT PADDED, it works.
     *
     * It has also been discussed over at
     * https://stackoverflow.com/questions/30827140/
     *
     * byte[] concat_SSC_M = BytePadding.pad(Hex.decode(passport.getSSC() + M), 8);
     * */
    public String getN(String SSC, String DO){
        //byte[] concat = BytePadding.pad(Hex.decode(SSC + getM(DO)), 8);
        byte[] concat = Hex.decode(SSC + getM(DO));
        return Hex.toHexString(concat).toUpperCase();
    }

    public String getLCLength(){
        if (Hex.decode(this.data).length > 0) {
            return String.format("%02X", Hex.decode(this.data).length);
        }

        return "";
    }

    public String getProtectedApdu(String DO){

        String thisDO = "";

        switch (DO){
            case "DO97":
                thisDO = getDO97();
                break;
            case "DO87":
                thisDO = getDO87();
                break;
        }

        setData(thisDO + getDO8E());
        return getCmdHeader() + getLCLength() + thisDO + getDO8E() + this.LE;
    }

    public String getApdu(){
        return this.CLA + this.INS + this.P1 + this.P2 + getLCLength() + this.data + this.LE;
    }
}
