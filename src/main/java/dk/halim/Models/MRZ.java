package dk.halim.Models;

import dk.halim.Controllers.Operations;
import dk.halim.Views.UpdateStatus;
import javafx.scene.control.TextArea;

public class MRZ {

    static String toAppend = "";

    static String finalDateOfBirth = "";
    static String finalDateOfExpiry = "";

    public static void validateInfo(ePassport passport, TextArea currentStatus, String documentNumber, String dateOfBirth, String dateOfExpiry){

        passport.setDocumentNumber(documentNumber);

        UpdateStatus.append(currentStatus, "\nDocument Number = " + passport.getDocumentNumber());

        // Validate and correct the document number, if length is less than 9, append with <
        if (!passport.getDocumentNumber().equals(Operations.fixDocumentNumber(passport.getDocumentNumber()))) {
            toAppend = "Corrected Document Number = " + Operations.fixDocumentNumber(passport.getDocumentNumber());
            UpdateStatus.append(currentStatus, toAppend);
            passport.setDocumentNumber(Operations.fixDocumentNumber(passport.getDocumentNumber()));
        }

        // Convert the date from dd/MM/yyyy to yymmdd
        if (Operations.isValidDate(dateOfBirth)) {
            UpdateStatus.append(currentStatus, "Date of Birth = " + dateOfBirth);

            finalDateOfBirth = Operations.formatDateISO8601(dateOfBirth);
            UpdateStatus.append(currentStatus, "Date of Birth (ISO 8601:2000) = " + finalDateOfBirth);

            passport.setDateOfBirth(finalDateOfBirth);
        } else {
            UpdateStatus.append(currentStatus, "Date of Birth: INVALID");
        }

        if (Operations.isValidDate(dateOfExpiry)) {
            UpdateStatus.append(currentStatus, "Date of Expiry = " + dateOfExpiry);

            finalDateOfExpiry = Operations.formatDateISO8601(dateOfExpiry);
            UpdateStatus.append(currentStatus, toAppend = "Date of Expiry (ISO 8601:2000) = " + finalDateOfExpiry);

            passport.setDateofExpiry(finalDateOfExpiry);
        } else {
            UpdateStatus.append(currentStatus, "Date of Expiry: INVALID");
        }
    }

}
