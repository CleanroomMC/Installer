package com.cleanroommc.installer.target.action;

import com.cleanroommc.installer.target.InstallContext;
import com.cleanroommc.installer.target.InstallException;
import com.cleanroommc.installer.target.InstallPlan;

import java.nio.file.Path;

/**
 * Work in an {@link InstallPlan}.
 */
public abstract class Action {

    private final Path destination;

    protected Action(Path destination) {
        this.destination = destination;
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
