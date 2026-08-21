package com.cleanroommc.installer.util.slf4j;

import com.cleanroommc.installer.util.Log;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.LegacyAbstractLogger;

final class LogAdapter extends LegacyAbstractLogger {

    private static String withThrown(String message, Throwable thrown) {
        return thrown == null ? message : message + ": {}";
    }

    private static Object[] append(Object[] arguments, Throwable thrown) {
        if (thrown == null) {
            return arguments;
        }
        Object[] extended = new Object[(arguments == null ? 0 : arguments.length) + 1];
        if (arguments != null) {
            System.arraycopy(arguments, 0, extended, 0, arguments.length);
        }
        extended[extended.length - 1] = thrown;
        return extended;
    }

    LogAdapter(String name) {
        this.name = name;
    }

    @Override
    protected String getFullyQualifiedCallerName() {
        return null;
    }

    @Override
    protected void handleNormalizedLoggingCall(Level level, Marker marker, String message, Object[] arguments, Throwable thrown) {
        Log log = LogBridge.target();
        if (log == null) {
            return;
        }
        String prefixed = "[" + this.name + "] " + message;
        switch (level) {
            case ERROR:
                if (thrown != null) {
                    log.error(thrown, prefixed, arguments);
                } else {
                    log.error(prefixed, arguments);
                }
                break;
            case WARN:
                log.warn(withThrown(prefixed, thrown), append(arguments, thrown));
                break;
            case INFO:
                log.info(withThrown(prefixed, thrown), append(arguments, thrown));
                break;
            default:
                log.debug(withThrown(prefixed, thrown), append(arguments, thrown));
        }
    }

    @Override
    public boolean isTraceEnabled() {
        return LogBridge.target() != null;
    }

    @Override
    public boolean isDebugEnabled() {
        return LogBridge.target() != null;
    }

    @Override
    public boolean isInfoEnabled() {
        return LogBridge.target() != null;
    }

    @Override
    public boolean isWarnEnabled() {
        return LogBridge.target() != null;
    }

    @Override
    public boolean isErrorEnabled() {
        return LogBridge.target() != null;
    }

}
