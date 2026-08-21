package com.cleanroommc.installer.platform;

import com.cleanroommc.platformutils.Platform;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;

public class Environment {

    /**
     * Property JavaUtils reads for the Cleanroom home, honoured here so both agree on one directory.
     */
    public static final String CLEANROOM_HOME_PROPERTY = "cleanroom.homeDir";

    private static final Environment CURRENT = new Environment();

    public static Environment current() {
        return CURRENT;
    }

    public String env(String name) {
        return System.getenv(name);
    }

    public String property(String name) {
        return System.getProperty(name);
    }

    public Path home() {
        return path(property("user.home"));
    }

    public Path workingDirectory() {
        return path(property("user.dir"));
    }

    public FileSystem fileSystem() {
        return FileSystems.getDefault();
    }

    public Path path(String first, String... more) {
        return fileSystem().getPath(first, more);
    }

    public boolean windows() {
        return Platform.current().isWindows();
    }

    public boolean macOs() {
        return Platform.current().isMacOS();
    }

    public boolean linux() {
        return Platform.current().isLinux();
    }

    public Path cleanroomHome() {
        String override = property(CLEANROOM_HOME_PROPERTY);
        if (override != null && !override.trim().isEmpty()) {
            return path(override.trim());
        }
        return home().resolve(".cleanroom");
    }

    public Path installerCache() {
        return cleanroomHome().resolve("installer");
    }

    public Path javaCache() {
        return cleanroomHome().resolve("java");
    }

}
