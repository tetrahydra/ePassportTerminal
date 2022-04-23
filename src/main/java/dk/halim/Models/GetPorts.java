package dk.halim.Models;

import jssc.SerialPortList;

import java.util.regex.Pattern;

public class GetPorts {

    /**
     * Get all available serial ports
     *
     * @return a list of port names
     * Sourced and edited from https://www.tabnine.com/code/java/methods/jssc.SerialPortList/getPortNames
     */
    public static String[] listConnections() {
        String OS = System.getProperty("os.name").toLowerCase();
        String[] portsDetected = {};

        if (OS.indexOf("mac") >= 0) {
            portsDetected = SerialPortList.getPortNames("/dev/", Pattern.compile("tty\\..*"));
        } else if (OS.indexOf("win") >= 0) {
            portsDetected = SerialPortList.getPortNames("COM");
        } else if (OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0) {
            portsDetected = SerialPortList.getPortNames("/dev/");
        }

        return portsDetected;
    }

}
