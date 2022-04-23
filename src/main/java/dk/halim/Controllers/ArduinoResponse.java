package dk.halim.Controllers;

import com.fazecast.jSerialComm.SerialPort;
import java.io.*;

public class ArduinoResponse {

    static InputStream inputSerial;

    public static String SerialRead(SerialPort portConnected) throws IOException {

        inputSerial = portConnected.getInputStream();

        while(true) {

            String ArduinoResponse = "";
            String ArduinoResponseCode = "";
            StringBuffer data = new StringBuffer();

            while (inputSerial.available() > 0) {

                int b;
                while ((b = inputSerial.read()) >= 0 && b != '\r' && b != '\n') {
                    data.append((char)b);
                }

            }

            ArduinoResponse = data.toString();

            if (ArduinoResponse.length()>0) {

                if (ArduinoResponse.substring(0, 8).equals("RESPONSE")) {
                    ArduinoResponseCode = ArduinoResponse.replace("RESPONSE ", "");
                }

                return ArduinoResponseCode;

            }

        }
    }
}