package dk.halim.Controllers;

import com.fazecast.jSerialComm.SerialPort;
import javafx.scene.control.TextArea;

/**
 * The code for connect/disconnect to an Arduino serial is based and improvised from
 * https://forum.arduino.cc/t/trying-to-communicate-to-arduino-with-jserialcomm-and-kotlin/913482
 * */

public class ArduinoPort {

    static SerialPort arduinoPort = null;

    public static SerialPort connectArduino(String port, TextArea currentStatus) {
        System.out.println("Connecting to NFC Reader...");
        currentStatus.setText("\nConnecting to NFC Reader...");

        SerialPort arduinoPort = SerialPort.getCommPort(port);

        // Default connection settings for Arduino
        arduinoPort.setComPortParameters(
                115200,
                8,
                SerialPort.ONE_STOP_BIT,
                SerialPort.NO_PARITY);

        // Block until bytes can be written
        arduinoPort.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING, 0, 0);

        if (arduinoPort.openPort()) {
            System.out.println("Connected at port: " + port);
            currentStatus.setText("Connected at port: " + port);
        } else {
            System.out.println("Connection failed.");
            currentStatus.setText("Connection failed.");
        }

        return arduinoPort;
    }


    public static void disconnectArduino(TextArea currentStatus) {
        System.out.println("Disconnecting NFC Reader...");
        currentStatus.setText("\nConnecting to NFC Reader...");

        if (arduinoPort != null) {
            arduinoPort.closePort();
        }

        System.out.println("Disconnected.");
        currentStatus.setText("Disconnected.");
    }

}
