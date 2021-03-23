package com.integ.serialcontrol;

import com.integ.common.iolog.*;
import com.integ.common.logging.AppLog;
import com.integ.common.logging.Logger;
import com.integ.common.logging.RollingFileLog;
import com.integ.common.net.*;
import com.integ.common.system.Application;
import com.integpg.comm.AUXSerialPort;
import com.integpg.comm.COMSerialPort;
import com.integpg.comm.SerialPort;
import com.integpg.system.JANOS;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.Socket;

/**
 * The Serial Control application allows people to connect to the JNIOR via Serial or Ethernet.
 * I/O monitoring and Relay control is available with simple ASCII commands.
 */
public class SerialControlMain
        implements IoChannelLogListener {

    /* a flag indicating whether there was a valid configuration.  If there is not a 
       valid configuration then we will let the application exit */
    private boolean _validConfiguration = false;

    /* IO log monitors for both inputs and outputs.  we use these to send unsolicited io alerts */
    private DigitalInputsIoLogMonitor _digitalInputsIoLogMonitor;
    private RelayOutputsIoLogMonitor _relayOutputsIoLogMonitor;



    public static void main(String[] args) throws Exception {
        Application.init(new AssemblyInfo());

        SerialControlMain serialControlMain = new SerialControlMain();
        serialControlMain.init(args);
        serialControlMain.run();

    }



    public void init(String[] args) throws Exception {
        //
        // Read the configuration.  Configuration is only consumed on application start-up 
        // since we are bringing up tcp listeners and taking over serial ports
        Config.init();

        //
        // set up various features
        setUpSerialPort();
        setUpTcpServer();
        setUpUdpServer();

        setIoLogMonitors();
    }



    private void setUpSerialPort() {
        try {
            SerialPort serialPort = null;

            //
            // get the serial port configuration and open the port if the name is valid.  we will 
            // use whatever serial settings are in the registry in AUXSerial or COMSerial
            String serialPortName = Config.getSerialPortName();

            if ("aux".equalsIgnoreCase(serialPortName)) serialPort = new AUXSerialPort();
            else if ("rs232".equalsIgnoreCase(serialPortName)
                    || "com".equalsIgnoreCase(serialPortName)) serialPort = new COMSerialPort();

            //
            // if a valid serial port was assigned then open it and start the client
            if (null != serialPort) {
                AppLog.info(String.format("opening %s serial port", serialPortName));
                serialPort.open();

                //
                // get a logger for the serial connection
                Logger serialLog = RollingFileLog.getLogger(
                        String.format("%s_Serial.log", Application.getAppName()));

                //
                // create an serial control client four our serial port
                SerialControlClient serialControlClient
                        = new SerialControlClient(serialPortName + " port",
                                serialPort.getInputStream(), serialPort.getOutputStream());
                serialControlClient.setLog(serialLog);

                //
                /// mark that there is a valid configuration
                _validConfiguration = true;
            } else {
                AppLog.info("serial port not configured to be in use");
            }
        } catch (Exception ex) {
            AppLog.error("error opening serial port", ex);
        }
    }



    private void setUpTcpServer() {
        try {
            //
            // get the serial port configuration and open the port if the name is valid.  we will 
            // use whatever serial settings are in the registry in AUXSerial or COMSerial
            int tcpServerPortNumber = Config.getTcpServerPortNumber();

            if (-1 != tcpServerPortNumber) {
                TcpServer tcpServer = new TcpServer("SerialControl-TcpServer", tcpServerPortNumber);
                tcpServer.setLog(AppLog.getLog());
                tcpServer.start();

                //
                // get a logger for the serial connection
                Logger tcpServerLog = RollingFileLog.getLogger(
                        String.format("%s_TcpServer.log", Application.getAppName()));

                tcpServer.setTcpServerListener(new TcpServerListener() {
                    @Override
                    public void clientConnected(TcpServerEvent evt) {
                        Socket socket = evt.getSocket();

                        String clientInfo = String.format("%s:%d",
                                socket.getInetAddress().getHostAddress(), socket.getPort());
                        tcpServerLog.info(String.format("%s is connected", clientInfo));

                        try {
                            //
                            // create an serial control client four our serial port
                            SerialControlClient serialControlClient
                                    = new SerialControlClient(clientInfo,
                                            socket.getInputStream(), socket.getOutputStream());
                            serialControlClient.setLog(tcpServerLog);
                        } catch (IOException ex) {
                            tcpServerLog.error(
                                    String.format("error setting up ascii command client for %s",
                                            clientInfo), ex);
                        }
                    }
                });

                //
                /// mark that there is a valid configuration
                _validConfiguration = true;
            } else {
                AppLog.info("tcp server not configured to be in use");
            }
        } catch (Exception ex) {
            AppLog.error("error setting up tcp server", ex);
        }
    }



    private void setUpUdpServer() {
        try {
            //
            // get the serial port configuration and open the port if the name is valid.  we will 
            // use whatever serial settings are in the registry in AUXSerial or COMSerial
            int udpServerPortNumber = Config.getUdpServerPortNumber();

            if (-1 != udpServerPortNumber) {
                UdpServer udpServer = new UdpServer("SerialControl-UdpServer", udpServerPortNumber);
                udpServer.setLog(AppLog.getLog());
                udpServer.start();

                //
                // get a logger for the serial connection
                Logger udpServerLog = RollingFileLog.getLogger(
                        String.format("%s_UdpServer.log", Application.getAppName()));

                udpServer.setListener(new UdpConnectionListener() {

                    @Override
                    public void processMessage(IClient client, DatagramPacket packet, String message) {
                        udpServerLog.info(String.format("%s is connected", client.getSocketInfo()));

                        try {
                            //
                            // create an serial control client four our serial port
                            UdpControlClient udpControlClient
                                    = new UdpControlClient(client.getSocketInfo());
                            udpControlClient.setLog(udpServerLog);
                            udpControlClient.processMessage(message);
                        } catch (IOException ex) {
                            udpServerLog.error(
                                    String.format("error setting up udp command client for %s",
                                            client.getSocketInfo()), ex);
                        }
                    }
                });

                //
                /// mark that there is a valid configuration
                _validConfiguration = true;
            } else {
                AppLog.info("udp server not configured to be in use");
            }
        } catch (Exception ex) {
            AppLog.error("error setting up udp server", ex);
        }
    }



    private void setIoLogMonitors() {
        _digitalInputsIoLogMonitor = new DigitalInputsIoLogMonitor();
        _digitalInputsIoLogMonitor.addIoChannelLogEventListener(this);
        _digitalInputsIoLogMonitor.start();

        _relayOutputsIoLogMonitor = new RelayOutputsIoLogMonitor();
        _relayOutputsIoLogMonitor.addIoChannelLogEventListener(this);
        _relayOutputsIoLogMonitor.start();
    }



    @Override
    public void onIoChannelEvent(IoChannelEvent ioEvent) {
        //
        // see if unsolicited io alerts is enabled in the configuration 
        boolean sendUnsolicitedIoAlerts = Config.getSendUnsolicitedIoAlerts();
        if (sendUnsolicitedIoAlerts) {
            //
            // build the string to send
            String outputString = String.format("%s%d=%d",
                    ioEvent.AbbrTypeString, ioEvent.Channel, (ioEvent.State ? 1 : 0));

            if (ioEvent instanceof DigitalInputChannelEvent
                    && Config.getSendCounts()) {
                int counter = JANOS.getInputCounter(ioEvent.Channel - 1);
                outputString = String.format("%s,%d", outputString, counter);
            }

            //
            // broadcast to all connected clients
            SerialControlClient.broadcast(outputString, ioEvent.TransitionTime);
        }
    }



    public void run() throws Exception {
        //
        // see if there is any valid configuration.  if there isnt, then we can let this 
        // application exit as a reboot is needed
        if (_validConfiguration) {

            //
            // nothing to do here since the client handlers are being performed in a separate thread.
            Thread.sleep(Integer.MAX_VALUE);
        } else {
            AppLog.warn("there was either not valid configuration or errors occured.  nothing to do, exiting.");
        }
    }

}

