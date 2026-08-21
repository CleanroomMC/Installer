package com.cleanroommc.installer.java;

import com.cleanroommc.javautils.api.JavaDistro;
import com.cleanroommc.javautils.api.JavaInstall;
import com.cleanroommc.javautils.api.JavaVersion;

import java.nio.file.Path;

/**
 * A Java runtime the installer settled on.
 */
public final class JavaResolution implements JavaInstall {

    public enum Origin {

        EXPLICIT,
        LOCATED,
        PROVISIONED

    }

    private final JavaInstall install;
    private final Origin origin;

    public JavaResolution(JavaInstall install, Origin origin) {
        this.install = install;
        this.origin = origin;
    }

    public JavaInstall install() {
        return this.install;
    }

    @Override
    public Path home() {
        return this.install.home();
    }

    @Override
    public Path executable(boolean windowed) {
        return this.install.executable(windowed);
    }

    public Path executable() {
        return this.install.executable(false);
    }

    public Path windowedExecutable() {
        return this.install.executable(true);
    }

    @Override
    public JavaVersion version() {
        return this.install.version();
    }

    @Override
    public JavaDistro distro() {
        return this.install.distro();
    }

    @Override
    public boolean jdk() {
        return this.install.jdk();
    }

    public int major() {
        return this.install.version().major();
    }

    public Origin origin() {
        return this.origin;
    }

    @Override
    public int compareTo(JavaInstall other) {
        return this.install.compareTo(other instanceof JavaResolution ? ((JavaResolution) other).install : other);
    }

    @Override
    public String toString() {
        return "Java " + major() + " at " + home() + " (" + this.origin.name().toLowerCase() + ")";
    }

}
