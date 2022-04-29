package dk.halim.Apdu;

import com.fazecast.jSerialComm.SerialPort;
import dk.halim.Controllers.ApduInterface;
import dk.halim.Models.DataString;
import dk.halim.Models.ePassport;
import dk.halim.Views.UpdateStatus;
import javafx.scene.control.TextArea;
import org.bouncycastle.util.encoders.Hex;

public class ExternalVerification {

    /**
     * EXTERNAL VERIFICATION TO CHECK THAT THE SESSION KEY IS CORRECT
     * Expected result: (KSenc, KSmac) and the SSC
     *
     * Note: External verification has been verified using the sample data.
     *       But when tested using physical e-passport, the RAPDU was not correct.
     */

    public static void verify(SerialPort portConnected, ePassport passport, TextArea currentStatus) throws Exception {

        UpdateStatus.append(currentStatus, "\n--------------------------------------------------------");
        UpdateStatus.append(currentStatus, "SEND EXTERNAL AUTHENTICATION (VERIFY SESSION KEYS & SSC)");
        UpdateStatus.append(currentStatus, "--------------------------------------------------------");

        // Concatenate RND.IC, RND.IFD and KIC:
        DataString R = new DataString();
        R.setData(passport.getRndICC() + passport.getRndIFD() + passport.getKeyIC());

        UpdateStatus.append(currentStatus, "\nConcatenate rnd.IC + rnd.IFD + K_IC");
        UpdateStatus.append(currentStatus, "R   = " + R.getData());

        // Encrypt R with 3DES and key KEnc
        R.encrypt(passport.getKeySeedEncByte(), passport.getKeySeedMACByte());

        UpdateStatus.append(currentStatus, "Enc = " + R.getEncrypted());
        UpdateStatus.append(currentStatus, "MAC  = " + R.getMAC());

        // Concatenate Encrypted(R) + MAC(encrypted(R)) + 9000
        String resp_data = R.getEncrypted() + R.getMAC() + "9000";

        // Send resp_data to the chip and get the response
        String RAPDU_extAuth = ApduInterface.SendExternalAuthenticate(portConnected, currentStatus, resp_data);

        // Page 91/150: Inspection system:
        // expected A set of two 16 bytes keys (KSenc, KSmac) and the SSC
        if (RAPDU_extAuth.length() > 4) {
            DataString RAPDU = new DataString();
            RAPDU.setEncrypted(Hex.decode(RAPDU_extAuth));
            RAPDU.decrypt(passport.getKeySeedEncByte());

            UpdateStatus.append(currentStatus, "Decrypted Response = " + RAPDU.getDecrypted());
        } else {
            UpdateStatus.append(currentStatus, "APDU for external authentication is invalid.");
        }

    }

}
