package com.cleanroommc.installer.util.slf4j;

import com.cleanroommc.installer.util.Log;

public final class LogBridge {

    private static volatile Log target;

    public static void attach(Log log) {
        target = log;
    }

    public static void detach() {
        target = null;
    }

    public static Log target() {
        return target;
    }

    private LogBridge() { }

}
