package dk.halim.Controllers;

import com.fazecast.jSerialComm.SerialPort;
import dk.halim.Views.UpdateStatus;
import javafx.scene.control.TextArea;

import java.util.concurrent.TimeUnit;

public class ArduinoReset {

    public static void reset(SerialPort portConnected, TextArea currentStatus) throws InterruptedException {

        UpdateStatus.append(currentStatus, "\nResetting Arduino with card reader... ");

        String commandReset = "CMD RESET\n";
        byte[] writeBufferReset = commandReset.getBytes();

        portConnected.writeBytes(writeBufferReset, writeBufferReset.length);

        TimeUnit.SECONDS.sleep(5);

        UpdateStatus.append(currentStatus, "Done.");

    }
}
