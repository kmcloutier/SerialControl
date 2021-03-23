package com.integ.serialcontrol;

import com.integ.common.logging.AppLog;
import com.integ.common.logging.Logger;
import com.integ.common.logging.SystemOutLog;
import com.integ.common.net.AsciiCommandClient;
import com.integ.common.net.BytesReceivedEvent;
import com.integ.common.net.ClientListener;
import com.integpg.system.JANOS;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.QuickDateFormat;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SerialControlClient
        implements ClientListener {

    private static final QuickDateFormat QUICK_DATE_FORMAT = new QuickDateFormat("MM/dd/yy HH:mm:ss.fff");

    private static final Pattern IO_CONTROL_PATTERN = Pattern.compile("([copt\\+\\d\\*]+(=\\d+)?)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern DIN_QUERY_PATTERN = Pattern.compile("din(\\d+)\\?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern ROUT_QUERY_PATTERN = Pattern.compile("rout(\\d+)\\?$", Pattern.CASE_INSENSITIVE);

    private static final ArrayList<SerialControlClient> CLIENTS = new ArrayList<>();

    // initially we will log to the system.out stream unless setLog is called
    private Logger _log = SystemOutLog.getLogger();

    private final String _clientNameString;
    private final OutputStream _outputStream;
    private final ByteArrayOutputStream _byteArrayOutputStream = new ByteArrayOutputStream();

    private final AsciiCommandClient _asciiCommandClient;



    public SerialControlClient(String clientNameString, InputStream inputStream, OutputStream outputStream) {
        _clientNameString = clientNameString;
        _outputStream = outputStream;

        //
        // create an ascii command client four our serial port
        _asciiCommandClient = new AsciiCommandClient(clientNameString, inputStream);
        _asciiCommandClient.setTerminationBytes(Config.getIncomingTerminationString());
        _asciiCommandClient.setClientListener(this);
        _asciiCommandClient.start();
    }



    public void setLog(Logger log) {
        _log = log;
        _asciiCommandClient.setLog(_log);
    }



    public static void broadcast(String s, long timestamp) {
        synchronized (CLIENTS) {
            for (SerialControlClient serialControlClient : CLIENTS) {
                try {
                    // send the output string with the acutal transition time from the iolog
                    serialControlClient.send(s, timestamp);
                } catch (Exception ex) {
                    // do nothing
                }
            }
        }
    }



    public synchronized void send(String s) {
        send(s, System.currentTimeMillis());
    }



    public synchronized void send(String s, long timestamp) {
        try {
            // reset the byte output array
            _byteArrayOutputStream.reset();

            // if client should respond with datestamp
            if (Config.getSendDateStamp()) {
                // first send the datestamp
                _byteArrayOutputStream.write(QUICK_DATE_FORMAT.format(timestamp).getBytes());
                _byteArrayOutputStream.write(' ');
            }

            // append text
            _byteArrayOutputStream.write(s.getBytes());

            // append the termination characters
            _byteArrayOutputStream.write(Config.getOutgoingTerminationString().getBytes());

            if (_outputStream != null) {
                _outputStream.write(_byteArrayOutputStream.toByteArray());
                // flush the buffer to make sure it went
                _outputStream.flush();
                // log what was sent. since we are using the timestamp obtained at
                // the beginning of this method we could potentially log out of order
                // with the recieve client.  we can synchronize on a log lock to make
                // sure this doesnt happen or we can let it happen and let it be known
                // that it can happen
                _log.info(_clientNameString + " sent: " + s);
            }
        } catch (IOException ex) {
            _log.error(ex);
            AppLog.error(ex);
        }

    }



    @Override
    public void bytesReceived(BytesReceivedEvent evt) {
        byte[] bytes = evt.getBytes();
        String message = new String(bytes);

        _log.info(String.format("%s received: %s", _clientNameString, message));

        try {
            processMessage(message);
        } catch (IOException ex) {
            _log.error(ex);
            AppLog.error(ex);
        }
    }



    @Override
    public void clientStarted(EventObject evt) {
        _log.info(_clientNameString + " client started");

        //
        // add this serial client to the clients list
        synchronized (CLIENTS) {
            CLIENTS.add(this);
        }
    }



    @Override
    public void clientFinished(EventObject evt) {
        _log.info(_clientNameString + " client finished");

        synchronized (CLIENTS) {
            if (CLIENTS.contains(this)) {
                CLIENTS.remove(this);
            }
        }
    }



    private void processMessage(String message) throws IOException {
        Matcher matcher;
        if ((matcher = IO_CONTROL_PATTERN.matcher(message)).find()) {
//            System.out.println("IO_CONTROL_PATTERN.groupCount(): " + matcher.groupCount());
//            for (int i = 0; i < matcher.groupCount(); i++) {
//                System.out.println("  IO_CONTROL_PATTERN.group[" + i + "]: " + matcher.group(i));
//            }

            if (1 < matcher.groupCount()) {
                Jrmon.parseCommand(matcher.group(1));
            }

        } //
        // is it a query for input status?
        else if ((matcher = DIN_QUERY_PATTERN.matcher(message)).find()) {
//            System.out.println("DIN_QUERY_PATTERN.groupCount(): " + matcher.groupCount());
//            for (int i = 0; i < matcher.groupCount(); i++) {
//                System.out.println("  DIN_QUERY_PATTERN.group[" + i + "]: " + matcher.group(i));
//            }

            int channelNumber = Integer.parseInt(matcher.group(1));
            int state = ((JANOS.getInputStates() >> (channelNumber - 1)) & 1);
            String response = String.format("din%d=%d", channelNumber, state);
            if (Config.getSendCounts()) {
                int counter = JANOS.getInputCounter(channelNumber - 1);
                response = String.format("%s,%d", response, counter);
            }
            send(response);

        } //
        // is it a query for output status?
        else if ((matcher = ROUT_QUERY_PATTERN.matcher(message)).find()) {
//            System.out.println("ROUT_QUERY_PATTERN.groupCount(): " + matcher.groupCount());
//            for (int i = 0; i < matcher.groupCount(); i++) {
//                System.out.println("  ROUT_QUERY_PATTERN.group[" + i + "]: " + matcher.group(i));
//            }

            int channelNumber = Integer.parseInt(matcher.group(1));
            int state = ((JANOS.getOutputStates() >> (channelNumber - 1)) & 1);
            String response = String.format("rout%d=%d", channelNumber, state);
            send(response);


        } else {
            send("unknown command: '" + message + "'");

        }
    }

}

