package com.cleanroommc.installer.util;

import com.cleanroommc.installer.target.ExitCode;
import com.cleanroommc.installer.target.InstallException;

public final class Cancellation {

    public static void check(ProgressListener listener) throws InstallException {
        if (listener != null && listener.cancelled()) {
            throw new InstallException(ExitCode.CANCELLED, "Cancelled");
        }
    }

    private Cancellation() { }

}
