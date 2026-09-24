/*
 * Copyright (c) 2026 CleanroomMC contributors
 * SPDX-License-Identifier: LGPL-3.0-only
 */

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
