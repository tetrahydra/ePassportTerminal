package dk.halim.Apdu;

import com.fazecast.jSerialComm.SerialPort;
import dk.halim.Controllers.ApduInterface;
import dk.halim.Models.DataString;
import dk.halim.Models.ePassport;
import dk.halim.Views.UpdateStatus;
import javafx.scene.control.TextArea;
import org.bouncycastle.util.encoders.Hex;

public class EstablishKeys {

    /**
     Construct the command data for the mutual authentication.
     - Request an 8 byte random number from the MRTD's chip (rnd.icc)
     - Generate an 8 byte random (rnd.ifd) and a 16 byte random (kifd)
     - Concatenate rnd.ifd, rnd.icc and kifd (s = rnd.ifd + rnd.icc + kifd)
     - Encrypt it with TDES and the Kenc key (eifd = TDES(s, Kenc))
     - Compute the MAC over eifd with TDES and the Kmax key (mifd = mac(pad(eifd))
     - Construct the APDU data for the mutualAuthenticate command (cmd_data = eifd + mifd)

     * Encrypt S using 3DES with key keyEnc
     * eIFD(DesEdeEngine/CBC/NoPadding)
     * Encrypt it with TDES and the Kenc key (eifd = TDES(s, Kenc))
     * */

    public static void keys(SerialPort portConnected, ePassport passport, TextArea currentStatus) throws Exception {

        UpdateStatus.append(currentStatus, "\n-----------------------------------------------------");
        UpdateStatus.append(currentStatus, "MUTUAL AUTHENTICATION & ESTABLISHMENT OF SESSION KEYS");
        UpdateStatus.append(currentStatus, "-----------------------------------------------------");

        passport.generateRandomIFD();
        UpdateStatus.append(currentStatus, "\nRandom rnd.IFD = " + passport.getRndIFD());
        UpdateStatus.append(currentStatus, "Random key.IFD = " + passport.getRndKeyIFD());

        DataString S = new DataString();
        S.setData(passport.getRndIFD() + passport.getRndICC() + passport.getRndKeyIFD());
        S.encrypt(passport.getKeySeedEncByte(), passport.getKeySeedMACByte());

        UpdateStatus.append(currentStatus, "\nS   = rnd.IFD + rnd.ICC + key.IFD");
        UpdateStatus.append(currentStatus, "S   = " + S.getData());

        UpdateStatus.append(currentStatus, "Enc = " + S.getEncrypted());
        UpdateStatus.append(currentStatus, "MAC = " + S.getMAC());

        // Construct command data for EXTERNAL AUTHENTICATE and send command APDU to the eMRTD's contactless IC
        String cmd_data = S.getEncrypted() + S.getMAC();
        UpdateStatus.append(currentStatus, "\ncmd_data = " + cmd_data);

        // Send the rnd.ICC back to the chip
        ApduInterface.MutualAuthentication(passport, portConnected, currentStatus, cmd_data);

        DataString DecryptChipAuthentication = new DataString();
        DecryptChipAuthentication.setEncrypted(passport.getChipAuthenticationDataByte());
        DecryptChipAuthentication.decrypt(passport.getKeySeedEncByte());
        DecryptChipAuthentication.mac(passport.getKeySeedMACByte());

        UpdateStatus.append(currentStatus, "\nDecrypted Chip Authentication = " + DecryptChipAuthentication.getDecrypted());

        /**
         * Check that the decrypted equal to RND.IC
         * */

        String dec_rndICC = DecryptChipAuthentication.getDecrypted().substring(0, 16);
        String dec_rndIFD = DecryptChipAuthentication.getDecrypted().substring(16, 32);
        String dec_kICC = DecryptChipAuthentication.getDecrypted().substring(32);

        UpdateStatus.append(currentStatus, "\ndec_rndICC     = " + dec_rndICC);
        UpdateStatus.append(currentStatus,   "dec_rndIFD     = " + dec_rndIFD);
        UpdateStatus.append(currentStatus,   "dec_kICC       = " + dec_kICC);

        UpdateStatus.append(currentStatus,   "MAC (BAC Chal) = " + DecryptChipAuthentication.getMAC());
        UpdateStatus.append(currentStatus,   "MAC (Expected) = " + passport.getChipAuthenticationMAC());

        if (dec_rndICC.equals(passport.getRndICC())) {
            UpdateStatus.append(currentStatus, "\nDecrypted data has rndICC substring.");
            UpdateStatus.append(currentStatus, "BAC Challenge is successful.");
        } else {
            UpdateStatus.append(currentStatus, "\nBAC Challenge failed.");
        }


        /**
         * Generate K_IC
         * K_IC is produced by the chip
         * */

        passport.generateKeyIC(dec_kICC);
        UpdateStatus.append(currentStatus, "\nK_IC   = " + passport.getKeyIC());

        passport.generateKeyICSeed();
        UpdateStatus.append(currentStatus,"K_seed = " + passport.getKeyICSeed());

        // Calculate session keys (KSEnc) according to Section 9.7.1/Appendix D.1:
        passport.generateKeyICSeedEnc();
        UpdateStatus.append(currentStatus,"KSEnc  = " + passport.getKeyICSeedEnc());

        // Calculate session keys (KSMAC) according to Section 9.7.1/Appendix D.1:
        passport.generateKeyICSeedMAC();
        UpdateStatus.append(currentStatus,"KSMAC  = " + passport.getKeyICSeedMAC());

        UpdateStatus.append(currentStatus, "\nSESSION KEY HAS BEEN ESTABLISHED SUCCESSFULLY");

    }

}
