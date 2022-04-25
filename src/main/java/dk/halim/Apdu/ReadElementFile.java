package dk.halim.Apdu;

import com.fazecast.jSerialComm.SerialPort;
import dk.halim.Controllers.*;
import dk.halim.Models.*;
import dk.halim.Views.*;
import javafx.application.Platform;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Hex;

import java.io.File;
import java.math.BigInteger;

public class ReadElementFile {

    static String CLA = "";
    static String INS = "";
    static String P1 = "";
    static String P2 = "";
    static String LC = "";
    static String data = "";
    static String LE = "";

    static RAPDU RAPDU_select = new RAPDU();
    static RAPDU RAPDU_length = new RAPDU();
    static RAPDU RAPDU_content = new RAPDU();

    static String command;
    static byte[] writeBuffer;

    /**
     * This value is based on the ability of the reader to read and return data
     * from the chip.
     *
     * Practically, the value should be 256 so that it can read one row with a single
     * APDU request command. Using value less than 256, in this case 31 increases
     * the reading time.
     *
     * 2022-03-24 The PN-532 is only able to read up to 31 bytes in each request.
     * When increased between 32-39, the returned checksum DO'8E has different value,
     * one byte at the end. From a real test:
     * DO'8E          = 78C6C4D0E2DCBF28
     * Calculated MAC = 78C6C4D0E2DCBFBC
     * Interestingly: the returned D0'87 and DO'99 are correct.
     *
     * When increased from 40 and above, the checksum failed totally
     * */
    static final int APDU_READ_LENGTH = 31;

    public static void read(SerialPort portConnected,
                            ePassport passport,
                            TextArea currentStatus,
                            ProgressBar progressReading,
                            String ReadFileCode)
            throws Exception {

        /**
         * Initialize an object to store the final output
         * */
        ElementFile OutputFile = new ElementFile();

        /**
         * First the EF.COM will be selected, then the first four bytes of this file
         * will be read so that the length of the structure in the file can be determined
         * and after that the remaining bytes are read.
         *
         * STEP 1: SELECT FILE
         * */
        if (ReadFileCode.length() > 0) {

            UpdateStatus.append(currentStatus, "\nSTEP 1: SELECT FILE");

            ApduCommand SelectFile = new ApduCommand(
                    CLA = "0C", // Documentation gave 00
                    INS = "A4",
                    P1 = "02",
                    P2 = "0C",
                    LC = "02",
                    data = "",
                    LE = "00");


            /**
             * a) Mask class byte and pad command header
             * */
            UpdateStatus.append(currentStatus, "\nCommand Header        = " + SelectFile.getCmdHeader());
            UpdateStatus.append(currentStatus, "Padded Command Header = " + SelectFile.getCmdHeaderPadded());

            // Select file code
            SelectFile.setData(ReadFileCode);

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

            // Pass the encrypted data to ApduCommand->data
            SelectFile.setData(DataFieldEncrypt.getEncrypted());

            /**
             * d) Build DO'87'
             * */
            UpdateStatus.append(currentStatus, "\nBuild DO'87");
            UpdateStatus.append(currentStatus, "DO87 = " + SelectFile.getDO87());

            /**
             * e) Concatenate CmdHeader and DO'87'
             * */
            UpdateStatus.append(currentStatus, "\nConcatenate CmdHeaderPadded and DO87");
            UpdateStatus.append(currentStatus, "M = " + SelectFile.getM("DO87"));

            /**
             * f) Compute MAC of M
             *    i) Increment SSC with 1
             *    ii) Concatenate SSC and M and add padding
             *    iii) Compute MAC over N with KSMAC
             * */

            passport.incrementSSC();
            UpdateStatus.append(currentStatus, "\nIncremented SSC = " + passport.getSSC());

            UpdateStatus.append(currentStatus, "\nN = Concatenate SSC and M and add padding");
            UpdateStatus.append(currentStatus, "    (NO PADDING WAS ADDED TO N)");

            DataString N = new DataString();
            N.setData(SelectFile.getN(passport.getSSC(), "DO87"));
            // 2022-03-20 : When padded, the MAC(N) cannot be verified.
            //N.setData(Hex.toHexString(BytePadding.pad(N.getDataByte(), 8)).toUpperCase());
            N.setEncrypted(N.getDataByte());
            N.mac(passport.getKeyICSeedMACByte());

            UpdateStatus.append(currentStatus, "N       = " + N.getEncrypted());
            UpdateStatus.append(currentStatus, "MAC (N) = " + N.getMAC());

            //byte[] check = Operations.calculate_mac(Hex.decode(SelectFile.getN(passport.getSSC(), "DO87")), passport.getKeyICSeedMACByte());
            //UpdateStatus.append(currentStatus, "Check   = " + Hex.toHexString(check).toUpperCase());

            /**
             * g) Build DO'8E'
             * */
            SelectFile.setDO8E(N.getMAC());
            UpdateStatus.append(currentStatus, "\nDO8E = " + SelectFile.getDO8E());

            /**
             * h) Construct and send protected APDU
             * */

            String ProtectedAPDU = SelectFile.getProtectedApdu("DO87");
            UpdateStatus.append(currentStatus, "\nSelect {EF.COM}");
            UpdateStatus.append(currentStatus, "ProtectedAPDU = " + ProtectedAPDU);

            command = "CMD APDU " + ProtectedAPDU + " 24\n";
            writeBuffer = command.getBytes();

            // Send APDU command
            portConnected.writeBytes(writeBuffer, writeBuffer.length);

            /**
             * i) Receive response APDU of eMRTD’s contactless IC
             * */
            RAPDU_select.setResponse(ArduinoResponse.SerialRead(portConnected));

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

                String DO99 = "9902" + "9000";

                DataString K = new DataString();
                K.setData(passport.getSSC() + DO99);
                // 2022-03-20 : When padded, the MAC(K) cannot be verified.
                //K.setData(Hex.toHexString(BytePadding.pad(K.getDataByte(), 8)).toUpperCase());
                K.setEncrypted(K.getDataByte());
                K.mac(passport.getKeyICSeedMACByte());

                UpdateStatus.append(currentStatus, "\nK           = " + K.getData());
                UpdateStatus.append(currentStatus, "MAC (K)     = " + K.getMAC());

                UpdateStatus.append(currentStatus, "RAPDU_DO8E  = " + RAPDU_select.getDO8E());

                if (RAPDU_select.getDO8E().equals(K.getMAC())) {
                    UpdateStatus.append(currentStatus, "RAPDU_DO8E == MAC (K) -> RAPDU is verified.");
                    RAPDU_select.setVerified(true);
                } else {
                    UpdateStatus.append(currentStatus, "RAPDU_DO8E != MAC (K) -> RAPDU is invalid.");
                }

            } else {
                UpdateStatus.append(currentStatus, "RAPDU did not return valid output.");
            }

        }

        /**
         * Read Binary of first four bytes.
         * Only if DAPDU_length is verified.
         * */
        if (RAPDU_select.getVerified()) {

            UpdateStatus.append(currentStatus, "\nSTEP 2: GET FIRST FOUR BYTES FROM THE SELECTED FILE");

            ApduCommand FileLength = new ApduCommand(
                    CLA = "0C",
                    INS = "B0",
                    P1 = "00",
                    P2 = "00",
                    LC = "",
                    data = "",
                    LE = "00");

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

            /**
             * c) Concatenate CmdHeader and DO'97'
             * */
            UpdateStatus.append(currentStatus, "\nConcatenate Padded CmdHeader and DO97");
            UpdateStatus.append(currentStatus, "M = " + FileLength.getM("DO97"));

            /**
             * d) Compute MAC of M
             *  i) Increment SSC with 1
             *  ii) Concatenate SSC and M and add padding
             *  iii) Compute MAC over N with KSMAC
             * */

            passport.incrementSSC();
            UpdateStatus.append(currentStatus, "\nIncremented SSC = " + passport.getSSC());

            DataString N = new DataString();
            N.setData(FileLength.getN(passport.getSSC(), "DO97"));
            // 2022-03-15: It seems another incorrect information from the report. MAC without padding.
            //N.setData(Hex.toHexString(BytePadding.pad(N.getDataByte(), 8)).toUpperCase());
            N.setEncrypted(N.getDataByte());
            N.mac(passport.getKeyICSeedMACByte());

            UpdateStatus.append(currentStatus, "\nN       = Concatenate SSC and M");
            UpdateStatus.append(currentStatus, "N       = " + N.getEncrypted());
            UpdateStatus.append(currentStatus, "MAC (N) = " + N.getMAC());

            /**
             * e) Build DO'8E'
             * */
            FileLength.setDO8E(N.getMAC());
            UpdateStatus.append(currentStatus, "\nDO8E = " + FileLength.getDO8E());

            /**
             * f) Construct and send protected APDU
             * */

            String ProtectedApdu = FileLength.getProtectedApdu("DO97");

            UpdateStatus.append(currentStatus, "\nGet first four bytes");
            UpdateStatus.append(currentStatus, "ProtectedAPDU = " + ProtectedApdu);

            command = "CMD APDU " + ProtectedApdu + " 27\n";
            writeBuffer = command.getBytes();

            // Send APDU command
            portConnected.writeBytes(writeBuffer, writeBuffer.length);

            /**
             * g) Receive response APDU of eMRTD’s contactless IC
             * */
            RAPDU_length.setResponse(ArduinoResponse.SerialRead(portConnected));

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

                DataString K = new DataString();
                K.setData(passport.getSSC() + RAPDU_length.getDO87() + RAPDU_length.getDO99());
                // 2022-03-20 : When unpadded, the RAPDU can be verified.
                // 2022-03-15: It seems another incorrect information from the report. MAC without padding.
                //K.setData(Hex.toHexString(BytePadding.pad(K.getDataByte(), 8)).toUpperCase());
                K.setEncrypted(K.getDataByte());
                K.mac(passport.getKeyICSeedMACByte());

                UpdateStatus.append(currentStatus, "\nK           = SSC + DO87 + DO99");
                UpdateStatus.append(currentStatus, "K           = " + K.getData());
                UpdateStatus.append(currentStatus, "MAC (K)     = " + K.getMAC());

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

                // Commented as not to store the first read 4 bytes
                // the "reading" method will read afresh from position 0
                // OutputFile.append(DO87Decrypt.getData());

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
                    UpdateStatus.append(currentStatus, "7F = " + DG_FileLength + " bytes");
                    ASN1datalength = "7F";
                }

                if (new BigInteger(Hex.toHexString(length), 16).intValue() == new BigInteger("81", 16).intValue()) {
                    byte[] length2 = Arrays.copyOfRange(DO87Decrypt.getDecryptedByte(), 2, 3);
                    DG_FileLength = new BigInteger(Hex.toHexString(length2), 16).intValue() + 3;
                    UpdateStatus.append(currentStatus, "81 = " + DG_FileLength + " bytes");
                    ASN1datalength = "81";
                }

                if (new BigInteger(Hex.toHexString(length), 16).intValue() == new BigInteger("82", 16).intValue()) {
                    byte[] length2 = Arrays.copyOfRange(DO87Decrypt.getDecryptedByte(), 2, 4);
                    DG_FileLength = new BigInteger(Hex.toHexString(length2), 16).intValue() + 4;
                    UpdateStatus.append(currentStatus, "82 = " + DG_FileLength + " bytes");
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

        /**
         * Read the binary from the selected file.
         * */
        progressReading.setProgress(0);
        progressReading.setVisible(true);

        UpdateStatus.append(currentStatus, "\nREADING...");

        UpdateStatus.append(currentStatus, "Total APDU iterations to fetch the file = " +
                ((long) OutputFile.getFileSize() / (long)APDU_READ_LENGTH));
        UpdateStatus.append(currentStatus, "Fetched bytes per iteration = " + 31 + " bytes");

        while (OutputFile.getDataSize() < OutputFile.getFileSize()) {

            long startTime = System.nanoTime();
            fetchFileContent(portConnected, passport, OutputFile, currentStatus);
            long stopTime = System.nanoTime();

            if (OutputFile.getStop()) break;

            float progress = (float) OutputFile.getDataSize() / (float) OutputFile.getFileSize();

            long remaining = ((long) OutputFile.getFileSize() - (long) OutputFile.getDataSize()) / 31 * (stopTime - startTime);
            String finishingIn = Utils.getReadableTime(remaining);

            Platform.runLater(() -> progressReading.setProgress(progress));

            System.out.println("Current progress = " + String.format("%.4f", progress * 100) + "%" +
                             "  Remaining time = " + finishingIn);

        }

        progressReading.setVisible(false);

        UpdateStatus.append(currentStatus, "\n---------------");
        UpdateStatus.append(currentStatus, "FINALIZING FILE");
        UpdateStatus.append(currentStatus, "---------------");

        /**
         * Save the final output to an external file
         * */
        if (RAPDU_select.getVerified() && RAPDU_length.getVerified() && RAPDU_content.getVerified()) {

            if (OutputFile.getData().length() < 100) {
                UpdateStatus.append(currentStatus, "\nData (hex) = " + OutputFile.getData());
                UpdateStatus.append(currentStatus, "Data       = " + Operations.hexToAscii(OutputFile.getData()));
            }

            UpdateStatus.append(currentStatus, "\nSaving to an external file");
            File file = new File("/Users/halim/RUC/2022_S2/Programming for Security (PSec)/ePassport Reader/EFCOM.bin");

            OutputFile.saveOutput(file);

            switch (ReadFileCode) {
                case "0101":
                    String temp = Operations.hexToAscii(OutputFile.getData());
                    EF_DG1 DG1 = new EF_DG1();
                    DG1.setMRZ(temp.substring(temp.length()-88));
                    DG1.parseMRZ();

                    if (DG1.getDocumentNumber().length() > 0){
                        passport.setDG1(DG1);
                    }

                    break;

                case "0102":
                    EF_DG2 DG2 = new EF_DG2();
                    DG2.setImageData(OutputFile.getData());
                    if (OutputFile.getData().length() > 0) {
                        passport.setDG2(DG2);
                    }

                    break;
            }

        } else {
            UpdateStatus.append(currentStatus, "\nError occurred and data has not been successfully retrieved.");
        }

    }

    /**
     * Read binary of the selected file
     * Pg 94/150: The reference reads the binary of remaining bytes from offset 4,
     * but implementation in this program starts at offset 0.
     */
    public static void fetchFileContent(SerialPort portConnected, ePassport passport, ElementFile OutputFile, TextArea currentStatus) throws Exception {

        boolean updateTextFlag = true;
        if (OutputFile.getDataSize() > 0) {
            updateTextFlag = false;
        }

        if (updateTextFlag) UpdateStatus.append(currentStatus, "\nSTEP 3: READ THE REMAINING CONTENT OF EF.COM");

        if (updateTextFlag)
            UpdateStatus.append(currentStatus, "\nLast read byte position = " + OutputFile.getDataSize());

        /**
         * These calculations are inspired from https://stackoverflow.com/questions/11297880/
         * The data on the chip is "arranged" as 256 bytes per row, therefore to get next information,
         * I must iterate to the next row.
         *
         * P1 is the row
         * P2 is the information column
         * */
        int P1_position = Math.floorDiv(OutputFile.getDataSize(), 256);
        int P2_position = OutputFile.getDataSize() % 256;

        ApduCommand FileContent = new ApduCommand(
                CLA = "0C",
                INS = "B0",
                P1 = String.format("%02X", P1_position),
                P2 = String.format("%02X", P2_position),
                LC = "",
                data = "",
                LE = "00");


        /**
         * a) Mask class byte and pad command header
         * */
        if (updateTextFlag)
            UpdateStatus.append(currentStatus, "\nCommand Header        = " + FileContent.getCmdHeader());
        if (updateTextFlag)
            UpdateStatus.append(currentStatus, "Padded Command Header = " + FileContent.getCmdHeaderPadded());

        /**
         * b) Build DO'97'
         * */

        int untilPosition = OutputFile.getFileSize() - OutputFile.getDataSize();
        if (untilPosition > APDU_READ_LENGTH) untilPosition = APDU_READ_LENGTH;

        // Reset the position of the maximum byte location in the row, not to exceed location 255.
        if ((P2_position + APDU_READ_LENGTH) >= 256) untilPosition = 256 - P2_position;

        FileContent.setLE97(String.format("%02X", untilPosition));
        if (updateTextFlag) UpdateStatus.append(currentStatus, "\nDO97 = " + FileContent.getDO97());

        /**
         * c) Concatenate CmdHeader and DO'97'
         * */
        if (updateTextFlag) UpdateStatus.append(currentStatus, "\nConcatenate Padded CmdHeader and DO97");
        if (updateTextFlag) UpdateStatus.append(currentStatus, "M = " + FileContent.getM("DO97"));

        /**
         * d) Compute MAC of M:
         *    i) Increment SSC with 1:
         *    ii) Concatenate SSC and M and add padding:
         *    iii) Compute MAC over N with KSMAC:
         * */

        passport.incrementSSC();
        if (updateTextFlag) UpdateStatus.append(currentStatus, "\nIncremented SSC = " + passport.getSSC());

        DataString N = new DataString();
        N.setData(FileContent.getN(passport.getSSC(), "DO97"));
        // 2022-03-16: It seems another incorrect information from the report. MAC without padding.
        // N3.setData(Hex.toHexString(BytePadding.pad(N3.getDataByte(), 8)).toUpperCase());
        N.setEncrypted(N.getDataByte());
        N.mac(passport.getKeyICSeedMACByte());

        if (updateTextFlag) UpdateStatus.append(currentStatus, "N   = " + N.getEncrypted());
        if (updateTextFlag) UpdateStatus.append(currentStatus, "MAC = " + N.getMAC());

        /**
         * e) Build DO'8E'
         * */
        FileContent.setDO8E(N.getMAC());
        if (updateTextFlag) UpdateStatus.append(currentStatus, "\nDO8E = " + FileContent.getDO8E());

        /**
         * f) Construct and send protected APDU
         * */

        String ProtectedAPDU = FileContent.getProtectedApdu("DO97");

        if (updateTextFlag) UpdateStatus.append(currentStatus, "\nGet the remaining file content");
        if (updateTextFlag) UpdateStatus.append(currentStatus, "ProtectedAPDU = " + ProtectedAPDU);

        command = "CMD APDU " + ProtectedAPDU + " 255\n";
        writeBuffer = command.getBytes();

        // Send APDU command
        portConnected.writeBytes(writeBuffer, writeBuffer.length);

        /**
         * g) Receive response APDU of eMRTD's contactless IC:
         * */

        RAPDU_content.setResponse(ArduinoResponse.SerialRead(portConnected));

        if (updateTextFlag) UpdateStatus.append(currentStatus, "\nRAPDU        = " + RAPDU_content.getResponse());
        if (updateTextFlag)
            UpdateStatus.append(currentStatus, "RAPDU length = " + RAPDU_content.getResponseByte().length);
        if (updateTextFlag) UpdateStatus.append(currentStatus, "RAPDU_DO87   = " + RAPDU_content.getDO87());
        if (updateTextFlag) UpdateStatus.append(currentStatus, "RAPDU_DO99   = " + RAPDU_content.getDO99());
        if (updateTextFlag) UpdateStatus.append(currentStatus, "RAPDU_DO8E   = " + RAPDU_content.getDO8E());

        /**
         * h) Verify RAPDU CC by computing MAC of concatenation DO'87' and DO'99'
         *    i) Increment SSC with 1
         *    ii) Concatenate SSC, DO‘87’ and DO‘99’ and add padding
         *    iii) Compute MAC with KSMAC
         *    iv) Compare CC' [MAC (K)] with data of DO'8E' of RAPDU
         * */
        if (RAPDU_content.getResponse().length() > 4 && RAPDU_content.getDO87() != null && RAPDU_content.getDO87().length() > 0) {

            passport.incrementSSC();
            if (updateTextFlag) UpdateStatus.append(currentStatus, "\nIncremented SSC = " + passport.getSSC());

            DataString K = new DataString();
            K.setData(passport.getSSC() + RAPDU_content.getDO87() + RAPDU_content.getDO99());
            // 2022-03-16 Problem when padded!
            // K.setData(Hex.toHexString(BytePadding.pad(K.getDataByte(), 8)).toUpperCase());
            K.setEncrypted(K.getDataByte());
            K.mac(passport.getKeyICSeedMACByte());

            if (updateTextFlag) UpdateStatus.append(currentStatus, "\nK length    = " + K.getDataByte().length);
            if (updateTextFlag) UpdateStatus.append(currentStatus, "K           = SSC + DO87 + DO99");
            if (updateTextFlag) UpdateStatus.append(currentStatus, "K           = " + K.getData());
            if (updateTextFlag) UpdateStatus.append(currentStatus, "MAC (K)     = " + K.getMAC());

            if (RAPDU_content.getDO8E().equals(K.getMAC())) {
                if (updateTextFlag) UpdateStatus.append(currentStatus, "RAPDU_DO8E == MAC (K) -> RAPDU is verified.");
                RAPDU_content.setVerified(true);
            } else {
                if (updateTextFlag) UpdateStatus.append(currentStatus, "RAPDU_DO8E != MAC (K) -> RAPDU is invalid.");
                RAPDU_content.setVerified(false);
                OutputFile.setStop(true);
            }

            // i) Decrypt data of DO'87' with KSEnc
            DataString DO87Decrypt = new DataString();
            DO87Decrypt.setEncrypted(Arrays.copyOfRange(RAPDU_content.getDO87Byte(), 3, RAPDU_content.getDO87Byte().length));
            DO87Decrypt.decrypt(passport.getKeyICSeedEncByte());
            DO87Decrypt.setData(Hex.toHexString(BytePadding.unpad(DO87Decrypt.getDecryptedByte())).toUpperCase());

            if (updateTextFlag) UpdateStatus.append(currentStatus, "\nDecrypted DO87 = " + DO87Decrypt.getData());
            if (updateTextFlag)
                UpdateStatus.append(currentStatus, "DecryptedData length = " + DO87Decrypt.getDataByte().length);

            OutputFile.append(DO87Decrypt.getData());

        } else {
            System.out.println("Response code = " + RAPDU_content.getSW1SW2() + " and RAPDU is empty.");
            if (updateTextFlag) UpdateStatus.append(currentStatus, "RAPDU is empty.");
            RAPDU_content.setVerified(false);
            OutputFile.setStop(true);
            OutputFile.reset();
        }
    }
}