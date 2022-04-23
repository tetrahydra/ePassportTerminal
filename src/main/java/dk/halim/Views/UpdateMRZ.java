package dk.halim.Views;

import dk.halim.Models.ePassport;
import javafx.scene.control.Label;

public class UpdateMRZ {

    public void show(ePassport passport,
                     Label MRZfirstName,
                     Label MRZlastName,
                     Label MRZNationality,
                     Label MRZSex) {

        if (passport.getDG1().isValid()) {
            MRZfirstName.setText(passport.getDG1().getFirstName());
            MRZlastName.setText(passport.getDG1().getLastName());
            MRZNationality.setText(passport.getDG1().getNationality());
            MRZSex.setText(passport.getDG1().getSex());
        }

    }

}
