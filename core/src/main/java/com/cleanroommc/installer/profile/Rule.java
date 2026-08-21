package com.cleanroommc.installer.profile;

import com.cleanroommc.platformutils.Platform;

import java.util.Map;
import java.util.regex.Pattern;


public final class Rule {

    public String action;
    public Os os;
    public Map<String, Boolean> features;

    public static final class Os {

        public String name;
        public String arch;
        public String version;

    }

    public boolean allow() {
        return !"disallow".equals(this.action);
    }

    public boolean applies(Platform platform) {
        if (this.features != null && !this.features.isEmpty()) {
            return false;
        }
        if (this.os == null) {
            return true;
        }
        if (this.os.name != null && !this.os.name.equals(osName(platform))) {
            return false;
        }
        if (this.os.arch != null && !this.os.arch.equals(archName(platform))) {
            return false;
        }
        if (this.os.version != null) {
            String version = System.getProperty("os.version", "");
            return Pattern.compile(this.os.version).matcher(version).find();
        }
        return true;
    }

    public static String osName(Platform platform) {
        if (platform.isWindows()) {
            return "windows";
        }
        if (platform.isMacOS()) {
            return "osx";
        }
        return "linux";
    }

    public static String archName(Platform platform) {
        if (platform.isArm()) {
            return platform.is64Bit() ? "arm64" : "arm32";
        }
        return platform.is64Bit() ? "x64" : "x86";
    }

}
