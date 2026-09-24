/*
 * Copyright (c) 2026 CleanroomMC contributors
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package com.cleanroommc.installer.cli;

import com.cleanroommc.installer.platform.Environment;
import com.cleanroommc.installer.target.ExitCode;
import com.cleanroommc.installer.target.InstallException;
import com.cleanroommc.installer.target.InstallRequest;
import com.cleanroommc.installer.target.client.ClientTarget;
import com.cleanroommc.installer.target.mmc.MmcTarget;
import com.cleanroommc.javautils.api.JavaDistro;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class CliOptionsTest {

    private final Environment environment = Environment.current();

    @Test
    void parsesAClientInstall() throws Exception {
        CliOptions options = CliOptions.parse(new String[] { "client", "-d", "/tmp/mc", "-v", "0.6.11-alpha", "--full", "--yes" }, this.environment);
        InstallRequest request = options.toRequest();

        assertThat(request.targetId()).isEqualTo("client");
        assertThat(request.version()).isEqualTo("0.6.11-alpha");
        assertThat(request.directory().toString().endsWith("/tmp/mc")).isTrue();
        assertThat(request.flag(ClientTarget.OPTION_FULL)).isTrue();
        assertThat(request.assumeYes()).isTrue();
    }

    @Test
    void unknownFlagsAreAnErrorRatherThanAGuess() {
        InstallException failure = catchThrowableOfType(
            InstallException.class,
            () -> CliOptions.parse(new String[] { "client", "--dirr", "/tmp/mc" }, this.environment)
        );
        assertThat(failure.exitCode()).isEqualTo(ExitCode.USAGE);
        assertThat(failure.getMessage().contains("--dirr")).isTrue();
    }

    @Test
    void flagsThatNeedAValueSaySo() {
        InstallException failure = catchThrowableOfType(InstallException.class, () -> CliOptions.parse(new String[] { "client", "--dir" }, this.environment));
        assertThat(failure.exitCode()).isEqualTo(ExitCode.USAGE);
        assertThat(failure.getMessage().contains("--dir needs a value")).isTrue();
    }

    @Test
    void contradictoryInterfaceFlagsAreRejected() {
        assertThatExceptionOfType(InstallException.class).isThrownBy(() -> CliOptions.parse(new String[] { "--gui", "--no-gui" }, this.environment));
    }

    @Test
    void aSecondPositionalArgumentIsRejected() {
        assertThatExceptionOfType(InstallException.class).isThrownBy(() -> CliOptions.parse(new String[] { "client", "server" }, this.environment));
    }

    @Test
    void jsonImpliesQuietSoTheOutputStaysParseable() throws Exception {
        CliOptions options = CliOptions.parse(new String[] { "client", "--json" }, this.environment);
        assertThat(options.json).isTrue();
        assertThat(options.quiet).isTrue();
    }

    @Test
    void guiOpensOnlyWithoutAModeAndWithADisplay() throws Exception {
        assertThat(CliOptions.parse(new String[0], this.environment).wantsGui(false)).isTrue();
        assertThat(CliOptions.parse(new String[0], this.environment).wantsGui(true)).as("headless must not open a window").isFalse();
        assertThat(CliOptions.parse(new String[] { "client" }, this.environment).wantsGui(false)).as("an explicit mode runs on the command line").isFalse();
        assertThat(CliOptions.parse(new String[] { "client", "--gui" }, this.environment).wantsGui(false)).isTrue();
        assertThat(CliOptions.parse(new String[] { "--help" }, this.environment).wantsGui(false)).isFalse();
    }

    @Test
    void mmcFlagsLandOnTheRightOptions() throws Exception {
        InstallRequest request = CliOptions.parse(new String[] { "mmc", "--replace-java-path", "--instance-name", "Cleanroom Test" }, this.environment)
            .toRequest();
        assertThat(request.flag(MmcTarget.OPTION_REPLACE_JAVA_PATH)).isTrue();
        assertThat(request.extra(MmcTarget.OPTION_INSTANCE_NAME)).isEqualTo("Cleanroom Test");
    }

    @Test
    void javaFlagsBuildTheSpec() throws Exception {
        InstallRequest request = CliOptions.parse(
            new String[] { "client", "--java-version", "21", "--java-vendor", "temurin", "--provision-java" },
            this.environment
        )
            .toRequest();
        assertThat(request.java().target()).isEqualTo(21);
        assertThat(request.java().distro()).isEqualTo(JavaDistro.TEMURIN);
        assertThat(request.java().allowProvision()).isTrue();
    }

    @Test
    void jvmArgumentsSplitOnWhitespace() throws Exception {
        InstallRequest request = CliOptions.parse(new String[] { "client", "--jvm-args", "-Xmx6G  -XX:+UseZGC" }, this.environment).toRequest();
        assertThat(request.jvmArgs()).hasSize(2);
        assertThat(request.jvmArgs().get(0)).isEqualTo("-Xmx6G");
    }

    @Test
    void modesThatDoNotInstallAreRecognised() throws Exception {
        assertThat(CliOptions.parse(new String[] { "list-versions" }, this.environment).isInstallMode()).isFalse();
        assertThat(CliOptions.parse(new String[] { "server" }, this.environment).isInstallMode()).isTrue();
    }

}
