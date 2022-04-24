package dk.halim.Controllers;
import dk.halim.Apdu.EstablishKeys;
import dk.halim.Apdu.ReadElementFile;
import dk.halim.Models.MRZ;
import dk.halim.Models.ePassport;
import dk.halim.Views.UpdateStatus;

import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;

import java.security.MessageDigest;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;

import com.fazecast.jSerialComm.SerialPort;

public class PerformNFC {

    public static void read(ePassport passport,
                            SerialPort portConnected,
                            TextArea currentStatus,
                            ProgressBar progressReading,
                            String documentNumber,
                            String dateOfBirth,
                            String dateOfExpiry)
            throws Exception {

        Security.addProvider(new BouncyCastleProvider());

        /**
         * Authenticate the applet id
         * This is to confirm that the card conform to ISO 9303
         * */

        UpdateStatus.append(currentStatus, "\n------------------------------------------");
        UpdateStatus.append(currentStatus, "AUTHENTICATING CHIP USING Select Applet ID");
        UpdateStatus.append(currentStatus, "------------------------------------------");

        boolean isAppletID = ApduInterface.AuthenticateAppletId(portConnected, currentStatus);

        if (!isAppletID) {
            return;
        }

        /**
         * Validate document number's length and the convert the format for the dates
         * */
        MRZ.validateInfo(passport, currentStatus, documentNumber, dateOfBirth, dateOfExpiry);

        /**
         * Generate MRZ
         * */
        passport.generateMRZ();
        UpdateStatus.append(currentStatus, "\nGenerated MRZ = " + passport.getMRZ());

        /**
         * Generate keySeed from MRZ_BAC
         * */

        UpdateStatus.append(currentStatus, "\n---------------------------");
        UpdateStatus.append(currentStatus, "GENERATING BASIC ACCESS KEY");
        UpdateStatus.append(currentStatus, "---------------------------");

        passport.generateKeySeed();

        MessageDigest mda = MessageDigest.getInstance("SHA-1", "BC");
        byte[] keySeedByte = mda.digest(passport.getMRZ().getBytes());
        UpdateStatus.append(currentStatus, "\nkeySeed = SHA-1(MRZ)      = " + Hex.toHexString(keySeedByte).toUpperCase());

        UpdateStatus.append(currentStatus, "keySeed 16-byte Truncated = " + passport.getKeySeed());

        passport.generateKeySeedEnc();
        UpdateStatus.append(currentStatus, "keySeedEnc = " + passport.getKeySeedEnc());

        passport.generateKeySeedMAC();
        UpdateStatus.append(currentStatus, "keySeedMAC = " + passport.getKeySeedMAC());

        /**
         * RND.IC is the response returned by card (get challenge), 8 bits
         * Request an 8 byte random number from the MRTD's chip (rnd.icc)
         * rnd_icc: The challenge received from the ICC.
         * */

        UpdateStatus.append(currentStatus, "\n---------------------------");
        UpdateStatus.append(currentStatus, "GET CHIP IC / BAC CHALLENGE");
        UpdateStatus.append(currentStatus, "---------------------------");

        ApduInterface.GetChipChallenge(passport, portConnected, currentStatus);
        UpdateStatus.append(currentStatus, "Chip IC (rnd.ICC) = " + passport.getRndICC());

        if (!(passport.getRndICC() == null) && !passport.getRndICC().isEmpty() && passport.getRndICC().length() == 16) {

            /**
             * Chip authentication and establishment of session keys
             * */
            EstablishKeys.keys(portConnected, passport, currentStatus);

            /**
             * Initialize the Send Sequence Counter (SSC)
             * Concatenation of the four least significant bytes of rnd.IC and rnd.IFD
             * */
            UpdateStatus.append(currentStatus, "\n--------------------------------------");
            UpdateStatus.append(currentStatus, "INITIALIZE SEND SEQUENCE COUNTER (SSC)");
            UpdateStatus.append(currentStatus, "--------------------------------------");
            passport.initializeSSC();
            UpdateStatus.append(currentStatus,"\nConcatenate the least 4 bytes from rnd.ICC and rnd.IFD");

            UpdateStatus.append(currentStatus,"Last 4 from rnd.ICC = " + "XXXXXXXX-" + passport.getRndICC().substring(8));
            UpdateStatus.append(currentStatus,"Last 4 from rnd.IFD = " + "XXXXXXXX-" + passport.getRndIFD().substring(8));

            UpdateStatus.append(currentStatus,"SSC = " + passport.getSSC());

            /**
             * Verify the generated KeyICSeedEnc, KeyICSeedMAC and SSC against the chip
             */

//            UpdateStatus.append(currentStatus, "\n----------------------------------------");
//            UpdateStatus.append(currentStatus, "MUTUAL AUTHENTICATION (TERMINAL TO CARD)");
//            UpdateStatus.append(currentStatus, "----------------------------------------");
            //ExternalVerification.verify(portConnected, passport, currentStatus);

            /**
             * Read file
             * 011E 0101 0102
             *
             * 0102 expected 16450
             * */

            UpdateStatus.append(currentStatus, "\n-----------------------");
            UpdateStatus.append(currentStatus, "READ FILE FROM THE CHIP");
            UpdateStatus.append(currentStatus, "-----------------------");

            String FileCode = "0101";
            ReadElementFile Read_DG1 = new ReadElementFile();
            Read_DG1.read(portConnected, passport, currentStatus, progressReading, FileCode);

            FileCode = "0102";
            ReadElementFile Read_DG2 = new ReadElementFile();
            Read_DG2.read(portConnected, passport, currentStatus, progressReading, FileCode);

        }

        UpdateStatus.append(currentStatus,"\n--------");
        UpdateStatus.append(currentStatus,"FINISHED");
        UpdateStatus.append(currentStatus,"--------");

        System.out.println("PerformNFC Completed");

    }

}