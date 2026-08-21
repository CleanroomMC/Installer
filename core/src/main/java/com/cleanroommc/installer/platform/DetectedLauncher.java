package com.cleanroommc.installer.platform;

import java.nio.file.Path;

/**
 * A MultiMC-family launcher found on this machine.
 */
public final class DetectedLauncher {

    public enum Kind {

        PRISM("Prism Launcher", "prismlauncher.cfg"),
        POLY_MC("PolyMC", "polymc.cfg"),
        MULTI_MC("MultiMC", "multimc.cfg");

        private final String displayName;
        private final String configName;

        Kind(String displayName, String configName) {
            this.displayName = displayName;
            this.configName = configName;
        }

        public String displayName() {
            return this.displayName;
        }

        public String configName() {
            return this.configName;
        }

    }

    private final Kind kind;
    private final Path root;
    private final Path instances;

    public DetectedLauncher(Kind kind, Path root, Path instances) {
        this.kind = kind;
        this.root = root;
        this.instances = instances;
    }

    public Kind kind() {
        return this.kind;
    }

    public Path root() {
        return this.root;
    }

    public Path instances() {
        return this.instances;
    }

    @Override
    public String toString() {
        return this.kind.displayName() + " (" + this.instances + ")";
    }

}
