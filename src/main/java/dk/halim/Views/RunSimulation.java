package dk.halim.Views;

import dk.halim.Apdu.ReadElementFile;
import dk.halim.Controllers.BytePadding;
import dk.halim.Models.*;
import javafx.scene.control.TextArea;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Hex;

/**
 * Run Simulation
 *
 * The simulation is equivalent to a test as it uses the example input/output from
 * Doc 9303 Machine Readable Travel Documents, Eighth Edition 2021
 * Part 11: Security Mechanisms for MRTDs
 * Appendix D (pp. 87/150 - 95/150)
 *
 * Required to be input the field:
 * Document number: L898902C
 * Date of birth: 06/08/1969
 * Date of expiry: 23/06/1994
 * */

public class RunSimulation {

    static RAPDU RAPDU_select = new RAPDU();
    static RAPDU RAPDU_length = new RAPDU();
    static RAPDU RAPDU_content = new RAPDU();

    static ElementFile OutputFile = new ElementFile();

    public static void Run(ePassport passport,
                           TextArea currentStatus,
                           String documentNumber,
                           String dateOfBirth,
                           String dateOfExpiry) throws Exception {

        Security.addProvider(new BouncyCastleProvider());

        String refDoc9303 = "";
        boolean correct = false;
        String concatString = "";

        /**
         * Validate document number's length and the convert the format for the dates
         * */
        MRZ.validateInfo(passport, currentStatus, documentNumber, dateOfBirth, dateOfExpiry);

        /**
         * Generate MRZ
         * */
        passport.generateMRZ();
        UpdateStatus.append(currentStatus, "\nGenerated MRZ = " + passport.getMRZ());

        refDoc9303 = "L898902C<369080619406236";
        UpdateStatus.append(currentStatus, "\nExpected value = " + refDoc9303);
        correct = refDoc9303.equalsIgnoreCase(passport.getMRZ());
        UpdateStatus.append(currentStatus, "Is value correct? " + correct);
        if (!correct) return;

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

        refDoc9303 = "239AB9CB282DAF66231DC5A4DF6BFBAE";
        UpdateStatus.append(currentStatus, "\nExpected value = " + refDoc9303);
        correct = refDoc9303.equalsIgnoreCase(passport.getKeySeed());
        UpdateStatus.append(currentStatus, "Is value correct? " + correct);
        if (!correct) return;

        passport.generateKeySeedEnc();
        UpdateStatus.append(currentStatus, "\nkeySeedEnc = " + passport.getKeySeedEnc());

        refDoc9303 = "AB94FDECF2674FDFB9B391F85D7F76F2";
        UpdateStatus.append(currentStatus, "\nExpected value = " + refDoc9303);
        correct = refDoc9303.equalsIgnoreCase(passport.getKeySeedEnc());
        UpdateStatus.append(currentStatus, "Is value correct? " + correct);
        if (!correct) return;

        passport.generateKeySeedMAC();
        UpdateStatus.append(currentStatus, "\nkeySeedMAC = " + passport.getKeySeedMAC());

        refDoc9303 = "7962D9ECE03D1ACD4C76089DCE131543";
        UpdateStatus.append(currentStatus, "\nExpected value = " + refDoc9303);
        correct = refDoc9303.equalsIgnoreCase(passport.getKeySeedMAC());
        UpdateStatus.append(currentStatus, "Is value correct? " + correct);
        if (!correct) return;

        UpdateStatus.append(currentStatus, "\n-----------------------------------------------------");
        UpdateStatus.append(currentStatus, "MUTUAL AUTHENTICATION & ESTABLISHMENT OF SESSION KEYS");
        UpdateStatus.append(currentStatus, "-----------------------------------------------------");

        passport.setRndICC("4608F91988702212");
        passport.setRndIFD("781723860C06C226");
        passport.setRndKeyIFD("0B795240CB7049B01C19B33E32804F0B");

        UpdateStatus.append(currentStatus, "\nConcatenate RND.IFD, RND.IC and keyIFD");

        DataString S = new DataString();
        S.setData(passport.getRndIFD() + passport.getRndICC() + passport.getRndKeyIFD());

        UpdateStatus.append(currentStatus, "= " + S.getData());

        refDoc9303 = "781723860C06C2264608F919887022120B795240CB7049B01C19B33E32804F0B";
        UpdateStatus.append(currentStatus, "\nExpected value = " + refDoc9303);
        correct = refDoc9303.equalsIgnoreCase(S.getData());
        UpdateStatus.append(currentStatus, "Is value correct? " + correct);
        if (!correct) return;

        UpdateStatus.append(currentStatus, "\nEncrypt the string");
        S.encrypt(passport.getKeySeedEncByte(), passport.getKeySeedMACByte());
        UpdateStatus.append(currentStatus, "= " + S.getEncrypted());

        refDoc9303 = "72C29C2371CC9BDB65B779B8E8D37B29ECC154AA56A8799FAE2F498F76ED92F2";
        UpdateStatus.append(currentStatus, "\nExpected value = " + refDoc9303);
        correct = refDoc9303.equalsIgnoreCase(S.getEncrypted());
        UpdateStatus.append(currentStatus, "Is value correct? " + correct);
        if (!correct) return;

        UpdateStatus.append(currentStatus, "\nMAC = " + S.getMAC());

        refDoc9303 = "5F1448EEA8AD90A7";
        UpdateStatus.append(currentStatus, "\nExpected value = " + refDoc9303);
        correct = refDoc9303.equalsIgnoreCase(S.getMAC());
        UpdateStatus.append(currentStatus, "Is value correct? " + correct);
        if (!correct) return;

        // Construct command data for EXTERNAL AUTHENTICATE and send command APDU to the eMRTD's contactless IC
        String cmd_data = S.getEncrypted() + S.getMAC();
        UpdateStatus.append(currentStatus, "\ncmd_data = " + cmd_data);

        refDoc9303 = "72C29C2371CC9BDB65B779B8E8D37B29ECC154AA56A8799FAE2F498F76ED92F25F1448EEA8AD90A7";
        UpdateStatus.append(currentStatus, "\nExpected value = " + refDoc9303);
        correct = refDoc9303.equalsIgnoreCase(cmd_data);
        UpdateStatus.append(currentStatus, "Is value correct? " + correct);
        if (!correct) return;

        /**
         * Generate K_IC
         * K_IC is produced by the chip
         * */

        passport.setKeyIC("0B4F80323EB3191CB04970CB4052790B");

        UpdateStatus.append(currentStatus, "\nK_IC   = " + passport.getKeyIC());

        passport.generateKeyICSeed();
        UpdateStatus.append(currentStatus,"\nK_seed = " + passport.getKeyICSeed());

        refDoc9303 = "0036D272F5C350ACAC50C3F572D23600";
        UpdateStatus.append(currentStatus, "\nExpected value = " + refDoc9303);
        correct = refDoc9303.equalsIgnoreCase(passport.getKeyICSeed());
        UpdateStatus.append(currentStatus, "Is value correct? " + correct);
        if (!correct) return;

        // Calculate session keys (KSEnc) according to Section 9.7.1/Appendix D.1:
        passport.generateKeyICSeedEnc();
        UpdateStatus.append(currentStatus,"\nKSEnc  = " + passport.getKeyICSeedEnc());

        refDoc9303 = "979EC13B1CBFE9DCD01AB0FED307EAE5";
        UpdateStatus.append(currentStatus, "\nExpected value = " + refDoc9303);
        correct = refDoc9303.equalsIgnoreCase(passport.getKeyICSeedEnc());
        UpdateStatus.append(currentStatus, "Is value correct? " + correct);
        if (!correct) return;

        // Calculate session keys (KSMAC) according to Section 9.7.1/Appendix D.1:
        passport.generateKeyICSeedMAC();
        UpdateStatus.append(currentStatus,"\nKSMAC  = " + passport.getKeyICSeedMAC());

        refDoc9303 = "F1CB1F1FB5ADF208806B89DC579DC1F8";
        UpdateStatus.append(currentStatus, "\nExpected value = " + refDoc9303);
        correct = refDoc9303.equalsIgnoreCase(passport.getKeyICSeedMAC());
        UpdateStatus.append(currentStatus, "Is value correct? " + correct);
        if (!correct) return;

        UpdateStatus.append(currentStatus, "\nSESSION KEY HAS BEEN ESTABLISHED SUCCESSFULLY");

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
        UpdateStatus.append(currentStatus,"Last 4 from rnd.IFC = " + "XXXXXXXX-" + passport.getRndIFD().substring(8));

        UpdateStatus.append(currentStatus,"SSC = " + passport.getSSC());

        refDoc9303 = "887022120C06C226";
        UpdateStatus.append(currentStatus, "\nExpected value = " + refDoc9303);
        correct = refDoc9303.equalsIgnoreCase(passport.getSSC());
        UpdateStatus.append(currentStatus, "Is value correct? " + correct);
        if (!correct) return;

        UpdateStatus.append(currentStatus, "\n----------------------------");
        UpdateStatus.append(currentStatus, "USING SESSION TO READ EF.COM");
        UpdateStatus.append(currentStatus, "----------------------------");

        UpdateStatus.append(currentStatus, "\nSTEP 1: SELECT FILE");

        // EF.COM
        String FileCode = "011E";
        ReadElementFile Read_DG2 = new ReadElementFile();

        ApduCommand SelectFile = new ApduCommand(
                "0C",
                "A4",
                "02",
                "0C",
                "",
                "",
                "00");

        /**
         * a) Mask class byte and pad command header
         * */
        UpdateStatus.append(currentStatus, "\nCommand Header        = " + SelectFile.getCmdHeader());
        UpdateStatus.append(currentStatus, "Padded Command Header = " + SelectFile.getCmdHeaderPadded());

        // Select file code
        SelectFile.setData(FileCode);

        /**
         * b) Pad data
         * */
        UpdateStatus.append(currentStatus, "\nData Field           = " + SelectFile.getData());
        UpdateStatus.append(currentStatus, "Padded Data Field    = " + SelectFile.getDataPadded());

        /**
         * c) Encrypt data with KSEnc
         * */
        DataString DataFieldEncrypt = new DataString();
        DataFieldEncrypt.setData(SelectFile.getDataPadded());
        DataFieldEncrypt.encrypt(passport.getKeyICSeedEncByte(), passport.getKeyICSeedMACByte());

        UpdateStatus.append(currentStatus, "Encrypted Data Field = " + DataFieldEncrypt.getEncrypted());

        SelectFile.setData(DataFieldEncrypt.getEncrypted());

        refDoc9303 = "6375432908C044F6";
        UpdateStatus.append(currentStatus, "\nExpected value = " + refDoc9303);
        correct = refDoc9303.equalsIgnoreCase(DataFieldEncrypt.getEncrypted());
        UpdateStatus.append(currentStatus, "Is value correct? " + correct);
        if (!correct) return;

        /**
         * d) Build DO'87'
         * */
        UpdateStatus.append(currentStatus, "\nBuild DO'87");
        UpdateStatus.append(currentStatus, "DO87 = " + SelectFile.getDO87());

        refDoc9303 = "8709016375432908C044F6";
        UpdateStatus.append(currentStatus, "\nExpected value = " + refDoc9303);
        correct = refDoc9303.equalsIgnoreCase(SelectFile.getDO87());
        UpdateStatus.append(currentStatus, "Is value correct? " + correct);
        if (!correct) return;

        /**
         * e) Concatenate CmdHeader and DO'87'
         * */
        UpdateStatus.append(currentStatus, "\nConcatenate CmdHeaderPadded and DO87");
        UpdateStatus.append(currentStatus, "M = " + SelectFile.getM("DO87"));

        refDoc9303 = "0CA4020C800000008709016375432908C044F6";
        UpdateStatus.append(currentStatus, "\nExpected value = " + refDoc9303);
        correct = refDoc9303.equalsIgnoreCase(SelectFile.getM("DO87"));
        UpdateStatus.append(currentStatus, "Is value correct? " + correct);
        if (!correct) return;

        /**
         * f) Compute MAC of M
         *    i) Increment SSC with 1
         *    ii) Concatenate SSC and M and add padding
         *    iii) Compute MAC over N with KSMAC
         * */

        passport.incrementSSC();
        UpdateStatus.append(currentStatus, "\nIncremented SSC = " + passport.getSSC());

        refDoc9303 = "887022120C06C227";
        UpdateStatus.append(currentStatus, "\nExpected value = " + refDoc9303);
        correct = refDoc9303.equalsIgnoreCase(passport.getSSC());
        UpdateStatus.append(currentStatus, "Is value correct? " + correct);
        if (!correct) return;

        UpdateStatus.append(currentStatus, "\nN = Concatenate SSC and M and add padding");

        DataString N = new DataString();
        N.setData(SelectFile.getN(passport.getSSC(), "DO87"));
        // 2022-03-20 : When padded, the MAC(N) cannot be verified.
        N.setData(Hex.toHexString(BytePadding.pad(N.getDataByte(), 8)).toUpperCase());
        N.setEncrypted(N.getDataByte());
        N.mac(passport.getKeyICSeedMACByte());

        UpdateStatus.append(currentStatus, "N       = " + N.getEncrypted());

        refDoc9303 = "887022120C06C2270CA4020C800000008709016375432908C044F68000000000";
        UpdateStatus.append(currentStatus, "\nExpected value = " + refDoc9303);
        correct = refDoc9303.equalsIgnoreCase(N.getEncrypted());
        UpdateStatus.append(currentStatus, "Is value correct? " + correct);
        if (!correct) return;

        UpdateStatus.append(currentStatus, "\nIMPORTANT TO NOTE");
        UpdateStatus.append(currentStatus, "When N was padded, MAC(N) is not correct.");

        // Unpadding and recalculate the MAC
        N.setData(Hex.toHexString(BytePadding.unpad(N.getDataByte())).toUpperCase());
        N.setEncrypted(N.getDataByte());
        N.mac(passport.getKeyICSeedMACByte());

        UpdateStatus.append(currentStatus, "\nMAC (N) = " + N.getMAC());

        refDoc9303 = "BF8B92D635FF24F8";
        UpdateStatus.append(currentStatus, "\nExpected value = " + refDoc9303);
        correct = refDoc9303.equalsIgnoreCase(N.getMAC());
        UpdateStatus.append(currentStatus, "Is value correct? " + correct);
        if (!correct) return;

        /**
         * g) Build DO'8E'
         * */
        SelectFile.setDO8E(N.getMAC());
        UpdateStatus.append(currentStatus, "\nDO8E = " + SelectFile.getDO8E());

        refDoc9303 = "8E08BF8B92D635FF24F8";
        UpdateStatus.append(currentStatus, "\nExpected value = " + refDoc9303);
        correct = refDoc9303.equalsIgnoreCase(SelectFile.getDO8E());
        UpdateStatus.append(currentStatus, "Is value correct? " + correct);
        if (!correct) return;

        /**
         * h) Construct and send protected APDU
         * */

        String ProtectedAPDU = SelectFile.getProtectedApdu("DO87");
        UpdateStatus.append(currentStatus, "\nSelect {EF.COM}");
        UpdateStatus.append(currentStatus, "ProtectedAPDU = " + ProtectedAPDU);

        refDoc9303 = "0CA4020C158709016375432908C044F68E08BF8B92D635FF24F800";
        UpdateStatus.append(currentStatus, "\nExpected value = " + refDoc9303);
        correct = refDoc9303.equalsIgnoreCase(ProtectedAPDU);
        UpdateStatus.append(currentStatus, "Is value correct? " + correct);
        if (!correct) return;

        /**
         * i) Receive response APDU of eMRTD’s contactless IC
         * */
        RAPDU_select.setResponse("990290008E08FA855A5D4C50A8ED9000");

        UpdateStatus.append(currentStatus, "\nRAPDU = " + RAPDU_select.getResponse());

        if (RAPDU_select.getResponse().length() > 4) {

            /**
             * j) Verify RAPDU CC by computing MAC of DO'99'
             *    i) Increment SSC with 1
             *    ii) Concatenate SSC and DO'99' and add padding
             *    iii) Compute MAC with KSMAC
             *    iv) Compare CC’ with data of DO'8E' of RAPDU.
             * */

            UpdateStatus.append(currentStatus, "\nVerifying RAPDU");

            UpdateStatus.append(currentStatus, "RAPDU_DO99  = " + RAPDU_select.getDO99());
            UpdateStatus.append(currentStatus, "RAPDU_DO8E  = " + RAPDU_select.getDO8E());

            passport.incrementSSC();
            UpdateStatus.append(currentStatus, "\nIncremented SSC = " + passport.getSSC());

            refDoc9303 = "887022120C06C228";
            UpdateStatus.append(currentStatus, "\nExpected value = " + refDoc9303);
            correct = refDoc9303.equalsIgnoreCase(passport.getSSC());
            UpdateStatus.append(currentStatus, "Is value correct? " + correct);
            if (!correct) return;

            String DO99 = "9902" + "9000";

            DataString K = new DataString();
            K.setData(passport.getSSC() + DO99);
            // 2022-03-20 : When padded, the MAC(K) cannot be verified.
            K.setData(Hex.toHexString(BytePadding.pad(K.getDataByte(), 8)).toUpperCase());
            K.setEncrypted(K.getDataByte());
            K.mac(passport.getKeyICSeedMACByte());

            UpdateStatus.append(currentStatus, "\nK           = " + K.getData());

            refDoc9303 = "887022120C06C2289902900080000000";
            UpdateStatus.append(currentStatus, "\nExpected value = " + refDoc9303);
            correct = refDoc9303.equalsIgnoreCase(K.getData());
            UpdateStatus.append(currentStatus, "Is value correct? " + correct);
            if (!correct) return;

            UpdateStatus.append(currentStatus, "\nIMPORTANT TO NOTE");
            UpdateStatus.append(currentStatus, "When K was padded, MAC(K) is not correct.");

            // Unpadding and recalculate the MAC
            K.setData(Hex.toHexString(BytePadding.unpad(K.getDataByte())).toUpperCase());
            K.setEncrypted(K.getDataByte());
            K.mac(passport.getKeyICSeedMACByte());

            UpdateStatus.append(currentStatus, "\nMAC (K)     = " + K.getMAC());

            refDoc9303 = "FA855A5D4C50A8ED";
            UpdateStatus.append(currentStatus, "\nExpected value = " + refDoc9303);
            correct = refDoc9303.equalsIgnoreCase(K.getMAC());
            UpdateStatus.append(currentStatus, "Is value correct? " + correct);
            if (!correct) return;

            UpdateStatus.append(currentStatus, "\nRAPDU_DO8E  = " + RAPDU_select.getDO8E());

            if (RAPDU_select.getDO8E().equals(K.getMAC())) {
                UpdateStatus.append(currentStatus, "RAPDU_DO8E == MAC (K) -> RAPDU is verified.");
                RAPDU_select.setVerified(true);
            } else {
                UpdateStatus.append(currentStatus, "RAPDU_DO8E != MAC (K) -> RAPDU is invalid.");
            }

        } else {
            UpdateStatus.append(currentStatus, "RAPDU did not return valid output.");
        }

        /**
         * Read Binary of first four bytes.
         * Only if DAPDU_length is verified.
         * */
        if (RAPDU_select.getVerified()) {

            UpdateStatus.append(currentStatus, "\nSTEP 2: GET FIRST FOUR BYTES FROM THE SELECTED FILE");

            ApduCommand FileLength = new ApduCommand(
                    "0C",
                    "B0",
                    "00",
                    "00",
                    "",
                    "",
                    "00");

            /**
             * a) Mask class byte and pad command header
             * */
            UpdateStatus.append(currentStatus, "\nCommand Header        = " + FileLength.getCmdHeader());
            UpdateStatus.append(currentStatus, "Padded Command Header = " + FileLength.getCmdHeaderPadded());

            /**
             * b) Build DO'97'
             * LE97 = 04 because the request to APDU is to fetch only first 4 bytes.
             * LE97 reflects the number of bytes to fetch
             * */
            FileLength.setLE97("04");
            UpdateStatus.append(currentStatus, "\nBuild DO'97");
            UpdateStatus.append(currentStatus, "DO97 = " + FileLength.getDO97());

            refDoc9303 = "970104";
            UpdateStatus.append(currentStatus, "\nExpected value = " + refDoc9303);
            correct = refDoc9303.equalsIgnoreCase(FileLength.getDO97());
            UpdateStatus.append(currentStatus, "Is value correct? " + correct);
            if (!correct) return;

            /**
             * c) Concatenate CmdHeader and DO'97'
             * */
            UpdateStatus.append(currentStatus, "\nConcatenate Padded CmdHeader and DO97");
            UpdateStatus.append(currentStatus, "M = " + FileLength.getM("DO97"));

            refDoc9303 = "0CB0000080000000970104";
            UpdateStatus.append(currentStatus, "\nExpected value = " + refDoc9303);
            correct = refDoc9303.equalsIgnoreCase(FileLength.getM("DO97"));
            UpdateStatus.append(currentStatus, "Is value correct? " + correct);
            if (!correct) return;

            /**
             * d) Compute MAC of M
             *  i) Increment SSC with 1
             *  ii) Concatenate SSC and M and add padding
             *  iii) Compute MAC over N with KSMAC
             * */

            passport.incrementSSC();
            UpdateStatus.append(currentStatus, "\nIncremented SSC = " + passport.getSSC());

            refDoc9303 = "887022120C06C229";
            UpdateStatus.append(currentStatus, "\nExpected value = " + refDoc9303);
            correct = refDoc9303.equalsIgnoreCase(passport.getSSC());
            UpdateStatus.append(currentStatus, "Is value correct? " + correct);
            if (!correct) return;

            N = new DataString();
            N.setData(FileLength.getN(passport.getSSC(), "DO97"));
            // 2022-03-15: If N is padded, then MAC(K) cannot be verified.
            N.setData(Hex.toHexString(BytePadding.pad(N.getDataByte(), 8)).toUpperCase());
            N.setEncrypted(N.getDataByte());
            N.mac(passport.getKeyICSeedMACByte());

            UpdateStatus.append(currentStatus, "\nN   = Concatenate SSC and M");
            UpdateStatus.append(currentStatus, "N   = " + N.getEncrypted());

            refDoc9303 = "887022120C06C2290CB00000800000009701048000000000";
            UpdateStatus.append(currentStatus, "\nExpected value = " + refDoc9303);
            correct = refDoc9303.equalsIgnoreCase(N.getEncrypted());
            UpdateStatus.append(currentStatus, "Is value correct? " + correct);
            if (!correct) return;

            UpdateStatus.append(currentStatus, "\nIMPORTANT TO NOTE");
            UpdateStatus.append(currentStatus, "When N was padded, MAC(N) is not correct.");

            // Unpadding and recalculate the MAC
            N.setData(Hex.toHexString(BytePadding.unpad(N.getDataByte())).toUpperCase());
            N.setEncrypted(N.getDataByte());
            N.mac(passport.getKeyICSeedMACByte());

            UpdateStatus.append(currentStatus, "\nMAC (N) = " + N.getMAC());

            refDoc9303 = "ED6705417E96BA55";
            UpdateStatus.append(currentStatus, "\nExpected value = " + refDoc9303);
            correct = refDoc9303.equalsIgnoreCase(N.getMAC());
            UpdateStatus.append(currentStatus, "Is value correct? " + correct);
            if (!correct) return;

            /**
             * e) Build DO'8E'
             * */
            FileLength.setDO8E(N.getMAC());
            UpdateStatus.append(currentStatus, "\nDO8E = " + FileLength.getDO8E());

            refDoc9303 = "8E08ED6705417E96BA55";
            UpdateStatus.append(currentStatus, "\nExpected value = " + refDoc9303);
            correct = refDoc9303.equalsIgnoreCase(FileLength.getDO8E());
            UpdateStatus.append(currentStatus, "Is value correct? " + correct);
            if (!correct) return;

            /**
             * f) Construct and send protected APDU
             * */

            String ProtectedApdu = FileLength.getProtectedApdu("DO97");

            UpdateStatus.append(currentStatus, "\nGet first four bytes");
            UpdateStatus.append(currentStatus, "ProtectedAPDU = " + ProtectedApdu);

            refDoc9303 = "0CB000000D9701048E08ED6705417E96BA5500";
            UpdateStatus.append(currentStatus, "\nExpected value = " + refDoc9303);
            correct = refDoc9303.equalsIgnoreCase(ProtectedApdu);
            UpdateStatus.append(currentStatus, "Is value correct? " + correct);
            if (!correct) return;

            /**
             * g) Receive response APDU of eMRTD’s contactless IC
             * */
            RAPDU_length.setResponse("8709019FF0EC34F9922651990290008E08AD55CC17140B2DED9000");

            UpdateStatus.append(currentStatus, "\nRAPDU = " + RAPDU_length.getResponse());

            UpdateStatus.append(currentStatus, "RAPDU_DO87  = " + RAPDU_length.getDO87());
            UpdateStatus.append(currentStatus, "RAPDU_DO99  = " + RAPDU_length.getDO99());
            UpdateStatus.append(currentStatus, "RAPDU_DO8E  = " + RAPDU_length.getDO8E());

            /**
             * h) Verify RAPDU CC by computing MAC of concatenation DO'87' and DO'99'
             *    i) Increment SSC with 1
             *    ii) Concatenate SSC, DO‘87’ and DO‘99’ and add padding
             *    iii) Compute MAC with KSMAC
             *    iv) Compare CC’ with data of DO‘8E’ of RAPDU
             * */

            if (RAPDU_select.getResponse().length() > 4) {
                passport.incrementSSC();
                UpdateStatus.append(currentStatus, "\nIncremented SSC = " + passport.getSSC());

                refDoc9303 = "887022120C06C22A";
                UpdateStatus.append(currentStatus, "\nExpected value = " + refDoc9303);
                correct = refDoc9303.equalsIgnoreCase(passport.getSSC());
                UpdateStatus.append(currentStatus, "Is value correct? " + correct);
                if (!correct) return;

                DataString K = new DataString();
                K.setData(passport.getSSC() + RAPDU_length.getDO87() + RAPDU_length.getDO99());
                // 2022-03-20 : When K was padded, MAC(K) was incorrect.
                K.setData(Hex.toHexString(BytePadding.pad(K.getDataByte(), 8)).toUpperCase());
                K.setEncrypted(K.getDataByte());
                K.mac(passport.getKeyICSeedMACByte());

                UpdateStatus.append(currentStatus, "\nK           = SSC + DO87 + DO99");
                UpdateStatus.append(currentStatus, "K           = " + K.getData());

                refDoc9303 = "887022120C06C22A8709019FF0EC34F99226519902900080";
                UpdateStatus.append(currentStatus, "\nExpected value = " + refDoc9303);
                correct = refDoc9303.equalsIgnoreCase(K.getData());
                UpdateStatus.append(currentStatus, "Is value correct? " + correct);
                if (!correct) return;

                UpdateStatus.append(currentStatus, "\nIMPORTANT TO NOTE");
                UpdateStatus.append(currentStatus, "When K was padded, MAC(K) is not correct.");

                // Unpadding and recalculate the MAC
                K.setData(Hex.toHexString(BytePadding.unpad(K.getDataByte())).toUpperCase());
                K.setEncrypted(K.getDataByte());
                K.mac(passport.getKeyICSeedMACByte());

                UpdateStatus.append(currentStatus, "\nMAC (K)     = " + K.getMAC());

                refDoc9303 = "AD55CC17140B2DED";
                UpdateStatus.append(currentStatus, "\nExpected value = " + refDoc9303);
                correct = refDoc9303.equalsIgnoreCase(K.getMAC());
                UpdateStatus.append(currentStatus, "Is value correct? " + correct);
                if (!correct) return;

                if (RAPDU_length.getDO8E().equals(K.getMAC())) {
                    UpdateStatus.append(currentStatus, "RAPDU_DO8E == MAC (K) -> RAPDU is verified.");
                    RAPDU_length.setVerified(true);
                } else {
                    UpdateStatus.append(currentStatus, "RAPDU_DO8E != MAC (K) -> RAPDU is invalid.");
                }


                /**
                 * i) Decrypt data of DO'87' with KSEnc
                 * */
                DataString DO87Decrypt = new DataString();
                DO87Decrypt.setEncrypted(Arrays.copyOfRange(RAPDU_length.getDO87Byte(), 3, RAPDU_length.getDO87Byte().length));
                DO87Decrypt.decrypt(passport.getKeyICSeedEncByte());
                DO87Decrypt.setData(Hex.toHexString(BytePadding.unpad(DO87Decrypt.getDecryptedByte())).toUpperCase());

                UpdateStatus.append(currentStatus, "\nDecrypted = " + DO87Decrypt.getData());

                refDoc9303 = "60145F01";
                UpdateStatus.append(currentStatus, "\nExpected value = " + refDoc9303);
                correct = refDoc9303.equalsIgnoreCase(DO87Decrypt.getData());
                UpdateStatus.append(currentStatus, "Is value correct? " + correct);
                if (!correct) return;

                /**
                 * j) Determine length of structure
                 * Pg 93/150 states: L = ‘14’ + 2 = 22 bytes
                 * */

                // asn1datalength

                byte[] length = Arrays.copyOfRange(DO87Decrypt.getDecryptedByte(), 1, 2);
                int DG_FileLength = 0;
                String ASN1datalength = "";

                if (new BigInteger(Hex.toHexString(length), 16).intValue() <= new BigInteger("7F", 16).intValue()) {
                    DG_FileLength = new BigInteger(Hex.toHexString(length), 16).intValue() + 2;
                    UpdateStatus.append(currentStatus, "\n7F = " + DG_FileLength + " bytes");
                    ASN1datalength = "7F";
                }

                if (new BigInteger(Hex.toHexString(length), 16).intValue() == new BigInteger("81", 16).intValue()) {
                    byte[] length2 = Arrays.copyOfRange(DO87Decrypt.getDecryptedByte(), 2, 3);
                    DG_FileLength = new BigInteger(Hex.toHexString(length2), 16).intValue() + 3;
                    UpdateStatus.append(currentStatus, "\n81 = " + DG_FileLength + " bytes");
                    ASN1datalength = "81";
                }

                if (new BigInteger(Hex.toHexString(length), 16).intValue() == new BigInteger("82", 16).intValue()) {
                    byte[] length2 = Arrays.copyOfRange(DO87Decrypt.getDecryptedByte(), 2, 4);
                    DG_FileLength = new BigInteger(Hex.toHexString(length2), 16).intValue() + 4;
                    UpdateStatus.append(currentStatus, "\n82 = " + DG_FileLength + " bytes");
                    ASN1datalength = "82";
                }

                UpdateStatus.append(currentStatus, "\nDecrypted DO87          = " + DO87Decrypt.getData());
                UpdateStatus.append(currentStatus, "ASN1datalength code     = " + ASN1datalength);
                UpdateStatus.append(currentStatus, "File length in bytes    = " + DG_FileLength);
                UpdateStatus.append(currentStatus, "Decrypted DO87 in bytes = " + DO87Decrypt.getDecryptedByte().length);

                OutputFile.setFileSize(DG_FileLength);

            } else {
                UpdateStatus.append(currentStatus, "RAPDU is empty.");
            }
        }

        UpdateStatus.append(currentStatus,"\n--------");
        UpdateStatus.append(currentStatus,"FINISHED");
        UpdateStatus.append(currentStatus,"--------");

        System.out.println("Simulation Completed");

    }

}
