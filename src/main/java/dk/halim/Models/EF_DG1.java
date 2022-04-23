package dk.halim.Models;

public class EF_DG1 {

    String MRZ = "";
    String MRZCheckDigit;

    String documentCode;
    String issuingOrganization;

    String names;
    String lastName;
    String firstName;
    String sex;
    String nationality;

    String personalNumber;
    String getPersonalNumberCheckDigit;

    String documentNumber;
    String documentNumberCheckDigit;

    String dateOfBirth;
    String getDateOfBirthCheckDigit;

    String dateOfExpiry;
    String getDateOfExpiryCheckDigit;

    public void setMRZ(String data){
        this.MRZ = data;
    }

    public boolean isValid(){
        if (this.MRZ.length() > 0) {
            return true;
        } {
            return false;
        }
    }

    public void parseMRZ(){
        if (this.MRZ.length() > 0){
            if (this.MRZ.length() == 88){

                this.documentCode = this.MRZ.substring(0, 2);
                this.issuingOrganization = this.MRZ.substring(2, 5);
                this.names = this.MRZ.substring(5, 44);
                this.documentNumber = this.MRZ.substring(44, 53);
                this.documentNumberCheckDigit = this.MRZ.substring(53, 54);
                this.nationality = this.MRZ.substring(54, 57);
                this.dateOfBirth = this.MRZ.substring(57, 63);
                this.getDateOfBirthCheckDigit = this.MRZ.substring(63, 64);
                this.sex = this.MRZ.substring(64, 65);
                this.dateOfExpiry = this.MRZ.substring(65, 71);
                this.getDateOfExpiryCheckDigit = this.MRZ.substring(71, 72);
                this.personalNumber = this.MRZ.substring(72, 86);
                this.getPersonalNumberCheckDigit = this.MRZ.substring(86, 87);
                this.MRZCheckDigit = this.MRZ.substring(87, 88);

                String[] namesTemp = this.names.split("<<");
                this.firstName = namesTemp[0];
                this.lastName = namesTemp[1];

            }
        }
    }

    public String getDocumentNumber() {
        return this.documentNumber;
    }

    public String getIssuingOrganization() {
        return this.issuingOrganization;
    }

    public String getLastName() {
        return this.lastName;
    }

    public String getFirstName() {
        return this.firstName;
    }

    public String getSex() {

        switch (this.sex) {
            case "F":
                return "Female";
            case "M":
                return "Male";
            case "X":
                return "Unspecified";
            default:
                return "Unspecified";
        }

    }

    public String getNationality() {
        return this.nationality;
    }

    public String getPersonalNumber() {
        return this.personalNumber;
    }

    public String getDateOfBirth() {
        return this.dateOfBirth;
    }

    public String getDateOfExpiry() {
        return this.dateOfExpiry;
    }

}
