package com.integ.serialcontrol;

import com.integ.common.system.AssemblyBase;
import com.integ.common.system.ReleaseInfo;

class AssemblyInfo extends AssemblyBase {

    AssemblyInfo() {
        NAME = "SerialControl";
        VERSION = "7.0./*//*/31";
        BUILD_TIME = "build-time:12/01/20 11:45 -05:00";

        RELEASE_NOTES = new ReleaseInfo[]{
            new ReleaseInfo("6.0", "15 nov 2019")
            .addNote("rewrite"),
            new ReleaseInfo("7.0", "01 dec 2020")
            .addNote("added udp server")
        };
    }

    public static final int APP_ID = 1800;
}

