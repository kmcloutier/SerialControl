package com.integ.serialcontrol;

import com.integ.common.system.Application;
import com.integ.common.utils.RegistryUtils;

public class Config {

    private static final String APPDATA_ROOT = "AppData/" + Application.getAppName();



    static void init() {
        getSerialPortName();
        getTcpServerPortNumber();
        getUdpServerPortNumber();

        getIncomingTerminationString();
        getOutgoingTerminationString();
        getSendUnsolicitedIoAlerts();
        getSendDateStamp();
        getSendCounts();
    }



    public static String getSerialPortName() {
        return RegistryUtils.getRegistryKey(String.format("%s/SerialPort", APPDATA_ROOT), "none");
    }



    public static String getIncomingTerminationString() {
        String incomingTerminationString = RegistryUtils.getRegistryKey(
                String.format("%s/IncomingTerminationString", APPDATA_ROOT), "\\n");
        return new String(getTerminationBytes(incomingTerminationString));
    }



    public static String getOutgoingTerminationString() {
        String outgoingTerminationString = RegistryUtils.getRegistryKey(
                String.format("%s/OutgoingTerminationString", APPDATA_ROOT), "\\n");
        return new String(getTerminationBytes(outgoingTerminationString));
    }



    public static int getTcpServerPortNumber() {
        return RegistryUtils.getRegistryKey(
                String.format("%s/TcpServerPortNumber", APPDATA_ROOT), -1);
    }



    public static int getUdpServerPortNumber() {
        return RegistryUtils.getRegistryKey(
                String.format("%s/UdpServerPortNumber", APPDATA_ROOT), -1);
    }



    private static byte[] getTerminationBytes(String terminationString) {
        String newTermString = "";
        boolean backslashFound = false;
        for (int i = 0; i < terminationString.length(); i++) {
            if (terminationString.charAt(i) == '\\' && !backslashFound) {
                backslashFound = true;
            } else {

                if (backslashFound) {
                    switch (terminationString.charAt(i)) {
                        case 'r':
                            newTermString += '\r';
                            break;
                        case 'n':
                            newTermString += '\n';
                            break;
                        case 't':
                            newTermString += '\r';
                            break;
                        case 'f':
                            newTermString += '\f';
                            break;
                        case 'b':
                            newTermString += '\b';
                            break;
                        case '0':
                            newTermString += '\0';
                            break;
                    }
                } else {
                    newTermString += terminationString.charAt(i);
                }
                backslashFound = false;
            }
        }
        return newTermString.getBytes();
    }



    public static boolean getSendUnsolicitedIoAlerts() {
        return RegistryUtils.getRegistryKey(
                String.format("%s/SendUnsolicitedIoAlerts", APPDATA_ROOT), true);
    }



    public static boolean getSendDateStamp() {
        return RegistryUtils.getRegistryKey(
                String.format("%s/SendDateStamp", APPDATA_ROOT), true);
    }



    public static boolean getSendCounts() {
        return RegistryUtils.getRegistryKey(
                String.format("%s/SendCounts", APPDATA_ROOT), false);
    }

}

