package dk.halim.Controllers;

import com.fazecast.jSerialComm.SerialPort;
import java.io.*;

/**
 * The code is based on the samples from
 * https://mschoeffler.com/2017/12/29/tutorial-serial-connection-between-java-application-and-arduino-uno/
 * https://xiaozhon.github.io/course_tutorials/Arduino_and_Java_Serial.pdf
 * https://stackoverflow.com/questions/15996345/java-arduino-read-data-from-the-serial-port
 *
 * But I improvised in terms of "incoming data termination" (\r and \n) as I implemented
 * the string termination in Arduino to indicate that the data has been reveived in its entirety.
 * */

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