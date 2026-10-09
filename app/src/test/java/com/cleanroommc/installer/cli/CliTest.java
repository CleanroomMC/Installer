/*
 * Copyright (c) 2026 CleanroomMC contributors
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package com.cleanroommc.installer.cli;

import com.cleanroommc.installer.platform.Environment;
import com.cleanroommc.installer.target.ExitCode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class CliTest {

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();

    @TempDir Path home;

    @Test
    void versionInfoNeedsNoMode() {
        assertThat(run("--version-info")).isEqualTo(ExitCode.SUCCESS.code());
        assertThat(this.out.toString()).startsWith("Cleanroom Installer ");
    }

    @Test
    void listVersionsPrintsJsonUnderJson() throws Exception {
        Files.createDirectories(this.home.resolve("installer"));
        Files.write(this.home.resolve("installer/versions.txt"), Arrays.asList("0.2", "0.1"));

        assertThat(run("list-versions", "--offline", "--json")).isEqualTo(ExitCode.SUCCESS.code());
        assertThat(this.out.toString().trim()).isEqualTo("{\"event\":\"versions\",\"versions\":[\"0.2\",\"0.1\"]}");
    }

    @Test
    void anUnknownModeIsAUsageError() {
        assertThat(run("bogus", "--json")).isEqualTo(ExitCode.USAGE.code());
        assertThat(this.out.toString()).contains("\"ok\":false,\"code\":2");
    }

    private int run(String... args) {
        Environment environment = new Environment() {

            @Override
            public Path cleanroomHome() {
                return CliTest.this.home;
            }

        };
        PrintStream stream = new PrintStream(this.out);
        return new Cli(environment, stream, stream).run(args);
    }

}
