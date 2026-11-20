/*
 * Copyright (c) 2026 CleanroomMC contributors
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package com.cleanroommc.installer.profile;

import com.cleanroommc.platformutils.Architecture;
import com.cleanroommc.platformutils.OperatingSystem;
import com.cleanroommc.platformutils.Platform;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RuleTest {

    private static String names(OperatingSystem os, Architecture arch) {
        Platform platform = Platform.of(os, arch);
        return Rule.osName(platform) + " " + Rule.archName(platform);
    }

    private static Rule rule(String name, String arch) {
        Rule rule = new Rule();
        rule.action = "allow";
        rule.os = new Rule.Os();
        rule.os.name = name;
        rule.os.arch = arch;
        return rule;
    }

    @Test
    void namesMatchWhatCleanroomGradleWrites() {
        assertThat(names(OperatingSystem.WINDOWS, Architecture.ARM64)).isEqualTo("windows arm64");
        assertThat(names(OperatingSystem.MAC_OS, Architecture.X64)).isEqualTo("osx x64");
        assertThat(names(OperatingSystem.LINUX, Architecture.PPC64LE)).isEqualTo("linux ppc64le");
        assertThat(names(OperatingSystem.LINUX, Architecture.RISCV64)).isEqualTo("linux riscv64");
        assertThat(names(OperatingSystem.FREE_BSD, Architecture.X64)).isEqualTo("freebsd x64");
    }

    @Test
    void picksOnlyTheMatchingPlatform() {
        Rule rule = rule("linux", "riscv64");
        assertThat(rule.applies(Platform.of(OperatingSystem.LINUX, Architecture.RISCV64))).isTrue();
        assertThat(rule.applies(Platform.of(OperatingSystem.LINUX, Architecture.X64))).isFalse();
        assertThat(rule("linux", "x64").applies(Platform.of(OperatingSystem.FREE_BSD, Architecture.X64))).isFalse();
    }

}
