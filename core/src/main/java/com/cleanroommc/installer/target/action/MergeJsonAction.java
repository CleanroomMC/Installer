package com.cleanroommc.installer.target.action;

import com.cleanroommc.installer.target.InstallContext;
import com.cleanroommc.installer.target.InstallException;
import com.google.gson.JsonObject;

import java.nio.file.Path;

/**
 * Mutate an existing JSON in-place, preserving every key the installer does not own, after taking a timestamped backup.
 */
public final class MergeJsonAction extends Action {

    @FunctionalInterface
    public interface Merge {

        void apply(JsonObject document, InstallContext context) throws InstallException;

    }

    private final String description;
    private final Merge merge;

    public MergeJsonAction(Path destination, String description, Merge merge) {
        super(destination);
        this.description = description;
        this.merge = merge;
    }

    @Override
    public String describe() {
        return "MERGE " + destination() + " (" + this.description + ", unknown keys preserved, backup written)";
    }

    @Override
    public void execute(InstallContext context) throws InstallException {
        this.merge.apply(null, context);
    }

}
