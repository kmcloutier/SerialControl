package com.integ.serialcontrol;

import com.integ.common.logging.Logger;
import com.integ.common.logging.SystemOutLog;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UdpControlClient {

//    private static final QuickDateFormat QUICK_DATE_FORMAT = new QuickDateFormat("MM/dd/yy HH:mm:ss.fff");
    private static final Pattern IO_CONTROL_PATTERN = Pattern.compile("([copt\\+\\d\\*]+(=\\d+)?)$", Pattern.CASE_INSENSITIVE);

    // initially we will log to the system.out stream unless setLog is called
    private Logger _log = SystemOutLog.getLogger();

    private final String _clientNameString;



    public UdpControlClient(String clientNameString) {
        _clientNameString = clientNameString;
    }



    public void setLog(Logger log) {
        _log = log;
    }



    public void processMessage(String message) throws IOException {
        Matcher matcher;
        if ((matcher = IO_CONTROL_PATTERN.matcher(message)).find()) {
//            System.out.println("IO_CONTROL_PATTERN.groupCount(): " + matcher.groupCount());
//            for (int i = 0; i < matcher.groupCount(); i++) {
//                System.out.println("  IO_CONTROL_PATTERN.group[" + i + "]: " + matcher.group(i));
//            }

            if (1 < matcher.groupCount()) {
                Jrmon.parseCommand(matcher.group(1));
            }

        }
    }

}

