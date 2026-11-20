/*
 * Copyright (c) 2026 CleanroomMC contributors
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package com.cleanroommc.installer.profile;

import com.cleanroommc.platformutils.Platform;

import java.util.Map;
import java.util.regex.Pattern;

public final class Rule {

    public String action;
    public Os os;
    public Map<String, Boolean> features;

    public static String osName(Platform platform) {
        return platform.isMacOS() ? "osx" : platform.operatingSystem().id();
    }

    public static String archName(Platform platform) {
        return platform.architecture().id();
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

    public static final class Os {

        public String name;
        public String arch;
        public String version;

    }

}
