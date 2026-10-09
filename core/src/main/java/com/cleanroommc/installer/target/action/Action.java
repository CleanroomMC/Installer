/*
 * Copyright (c) 2026 CleanroomMC contributors
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package com.cleanroommc.installer.target.action;

import com.cleanroommc.installer.target.InstallContext;
import com.cleanroommc.installer.target.InstallException;
import com.cleanroommc.installer.target.InstallPlan;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Work in an {@link InstallPlan}.
 */
public abstract class Action {

    private final Path destination;

    protected Action(Path destination) {
        this.destination = destination;
    }

    /**
     * Writes through a sibling temporary file, so a failure never leaves a half-written destination.
     */
    protected static void copyAtomically(InputStream in, Path destination) throws IOException {
        Path temp = destination.resolveSibling(destination.getFileName() + ".tmp");
        try {
            Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
            Files.move(temp, destination, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    public Path destination() {
        return this.destination;
    }

    public long networkBytes() {
        return 0L;
    }

    public abstract String describe();

    public abstract void execute(InstallContext context) throws InstallException;

}
