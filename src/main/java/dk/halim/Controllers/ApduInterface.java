package dk.halim.Controllers;

import com.fazecast.jSerialComm.SerialPort;
import dk.halim.Models.ApduCommand;
import dk.halim.Models.RAPDU;
import dk.halim.Models.ePassport;
import dk.halim.Views.UpdateStatus;
import javafx.scene.control.TextArea;
import org.bouncycastle.util.encoders.Hex;

import java.io.IOException;

public class ApduInterface {

    public static boolean AuthenticateAppletId(SerialPort portConnected, TextArea currentStatus) throws IOException {

        // LC and data are both empty
        ApduCommand validateApplet = new ApduCommand(
                "00",
                "A4",         // INS = SELECT FILE
                "04",             // P1 = Select By AID
                "0C",             // P2 = Return No Data
                "07",             // LC = length of the AID
                "A0000002471001", // Data = AID
                "");

        String command = "CMD APDU 00A4040C07A0000002471001 2\n";
        byte[] writeBuffer = command.getBytes();

        UpdateStatus.append(currentStatus, command);

        // Send APDU command
        portConnected.writeBytes(writeBuffer, writeBuffer.length);

        /**
         * Receive response APDU of eMRTD's contactless IC
         * */
        RAPDU RAPDU = new RAPDU();
        RAPDU.setResponse(ArduinoResponse.SerialRead(portConnected));

        boolean chipAppletIdIsValid = false;
        String msg = "";

        if (RAPDU.getSW1SW2().equals("9000")) {
            msg = "Applet ID confirmed";
            chipAppletIdIsValid = true;
        }

        if (RAPDU.getSW1SW2().equals("6A86")) {
            msg = "Failed to confirm applet ID";
        }

        if (RAPDU.getSW1SW2().equals("FAILED")) {
            msg = "NFC Failure";
        }

        UpdateStatus.append(currentStatus, "Response Code = " + RAPDU.getSW1SW2());
        UpdateStatus.append(currentStatus, "Status        = " + msg);

        return chipAppletIdIsValid;
    }

    /*
     * Reference:
     * ICAO Doc 9303, Machine Readable Travel Documents, Eleventh Edition, 2021.
     * Part 11 Security Mechanisms for MRTDs.
     *
     * Page 89/150, Appendix D
     * Request an 8 byte random number from the eMRTD's contactless IC
     *
     * The response is 8 byte + 2 bytes for SW1-SW2
     * Successful: RND.IC + SW1-SW2 (9000)
     * */
    public static void GetChipChallenge(ePassport passport, SerialPort portConnected, TextArea currentStatus) throws IOException {

        // LC and data are both empty
        ApduCommand ChipICC = new ApduCommand(
                "00",
                "84",
                "00",
                "00",
                "",
                "",
                "08");

        // 8 bytes for rnd.ICC and 2 bytes for SW1SW2
        String expectedResponseLength = Integer.toString(8 + 2);

        String command = "CMD APDU " + ChipICC.getApdu() + " " + expectedResponseLength + "\n";
        byte[] writeBuffer = command.getBytes();

        UpdateStatus.append(currentStatus, command);

        // Send APDU command
        portConnected.writeBytes(writeBuffer, writeBuffer.length);

        /**
         * Receive response APDU of eMRTD's contactless IC
         * */
        RAPDU RAPDU = new RAPDU();
        RAPDU.setResponse(ArduinoResponse.SerialRead(portConnected));

        if (RAPDU.getResponse().length() > 0 && !RAPDU.getResponse().equals("FAILED")) {
            passport.setRndICC(RAPDU.getSerialResponse());
            passport.setChipAuthenticationSW1SW2(RAPDU.getSW1SW2());
        }

        UpdateStatus.append(currentStatus, "Response Code = " + RAPDU.getSW1SW2());

    }

    /**
     * Page 90/150: Generate a 16-byte random
     * Send the rnd.ICC back to chip
     */
    public static void MutualAuthentication(ePassport passport, SerialPort portConnected, TextArea currentStatus, String cmd) throws IOException {

        // Check CLA against the documentation, 00 or 0C
        ApduCommand ChipAuthentication = new ApduCommand(
                "00",
                "82",
                "00",
                "00",
                String.format("%02X", Hex.decode(cmd).length),
                cmd,
                "28");

        // Send APDU command
        String command = "CMD APDU " + ChipAuthentication.getApdu() + " 255\n";
        byte[] writeBuffer = command.getBytes();

        UpdateStatus.append(currentStatus, command);

        portConnected.writeBytes(writeBuffer, writeBuffer.length);

        /**
         * Receive response APDU of eMRTD's contactless IC
         * */
        RAPDU RAPDU = new RAPDU();
        RAPDU.setResponse(ArduinoResponse.SerialRead(portConnected));

        UpdateStatus.append(currentStatus, "Response Code = " + RAPDU.getSW1SW2());
        UpdateStatus.append(currentStatus, "Mutual Authentication Response = " + RAPDU.getSerialResponse());

        passport.setChipAuthenticationData(RAPDU.getSerialResponse());
        passport.setChipAuthenticationSW1SW2(RAPDU.getSW1SW2());

    }

    public static String SendExternalAuthenticate(SerialPort portConnected, TextArea currentStatus, String cmd) throws IOException {

        // Should add LC?
        ApduCommand ChipExternalAuthentication = new ApduCommand(
                "",
                "",
                "",
                "",
                "",
                cmd,
                "");

        String command = "\nCMD APDU " + ChipExternalAuthentication.getApdu() + " 255\n";
        byte[] writeBuffer = command.getBytes();

        UpdateStatus.append(currentStatus, command);

        // Send APDU command
        portConnected.writeBytes(writeBuffer, writeBuffer.length);

        /**
         * Receive response APDU of eMRTD’s contactless IC
         * */
        RAPDU RAPDU = new RAPDU();
        RAPDU.setResponse(ArduinoResponse.SerialRead(portConnected));

        UpdateStatus.append(currentStatus, "Response Code = " + RAPDU.getSW1SW2());

        if (RAPDU.getResponse().length() > 4) {
            UpdateStatus.append(currentStatus, "External Authenticate Response = " + RAPDU.getSerialResponse());
            return RAPDU.getSerialResponse();
        } else {
            return RAPDU.getSW1SW2();
        }

    }

}