module dk.halim.epassportterminal {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires jssc;
    requires jSerialComm;
    requires org.bouncycastle.provider;
    requires javafx.graphics;

    opens dk.halim.ePassportTerminal to javafx.fxml;
    exports dk.halim.ePassportTerminal;
}