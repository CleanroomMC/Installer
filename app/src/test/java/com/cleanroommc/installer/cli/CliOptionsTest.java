package com.cleanroommc.installer.cli;

import com.cleanroommc.javautils.api.JavaDistro;

import com.cleanroommc.installer.platform.Environment;
import com.cleanroommc.installer.target.ExitCode;
import com.cleanroommc.installer.target.InstallException;
import com.cleanroommc.installer.target.InstallRequest;
import com.cleanroommc.installer.target.client.ClientTarget;
import com.cleanroommc.installer.target.mmc.MmcTarget;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CliOptionsTest {

    private final Environment environment = Environment.current();

    @Test
    void parsesAClientInstall() throws Exception {
        CliOptions options = CliOptions.parse(
                new String[] {"client", "-d", "/tmp/mc", "-v", "0.6.11-alpha", "--full", "--yes"}, this.environment);
        InstallRequest request = options.toRequest();

        assertEquals("client", request.targetId());
        assertEquals("0.6.11-alpha", request.version());
        assertTrue(request.directory().toString().endsWith("/tmp/mc"));
        assertTrue(request.flag(ClientTarget.OPTION_FULL));
        assertTrue(request.assumeYes());
    }

    @Test
    void unknownFlagsAreAnErrorRatherThanAGuess() {
        InstallException failure = assertThrows(InstallException.class,
                () -> CliOptions.parse(new String[] {"client", "--dirr", "/tmp/mc"}, this.environment));
        assertEquals(ExitCode.USAGE, failure.exitCode());
        assertTrue(failure.getMessage().contains("--dirr"));
    }

    @Test
    void flagsThatNeedAValueSaySo() {
        InstallException failure = assertThrows(InstallException.class,
                () -> CliOptions.parse(new String[] {"client", "--dir"}, this.environment));
        assertEquals(ExitCode.USAGE, failure.exitCode());
        assertTrue(failure.getMessage().contains("--dir needs a value"));
    }

    @Test
    void contradictoryInterfaceFlagsAreRejected() {
        assertThrows(InstallException.class,
                () -> CliOptions.parse(new String[] {"--gui", "--no-gui"}, this.environment));
    }

    @Test
    void aSecondPositionalArgumentIsRejected() {
        assertThrows(InstallException.class,
                () -> CliOptions.parse(new String[] {"client", "server"}, this.environment));
    }

    @Test
    void jsonImpliesQuietSoTheOutputStaysParseable() throws Exception {
        CliOptions options = CliOptions.parse(new String[] {"client", "--json"}, this.environment);
        assertTrue(options.json);
        assertTrue(options.quiet);
    }

    @Test
    void guiOpensOnlyWithoutAModeAndWithADisplay() throws Exception {
        assertTrue(CliOptions.parse(new String[0], this.environment).wantsGui(false));
        assertFalse(CliOptions.parse(new String[0], this.environment).wantsGui(true), "headless must not open a window");
        assertFalse(CliOptions.parse(new String[] {"client"}, this.environment).wantsGui(false),
                "an explicit mode runs on the command line");
        assertTrue(CliOptions.parse(new String[] {"client", "--gui"}, this.environment).wantsGui(false));
        assertFalse(CliOptions.parse(new String[] {"--help"}, this.environment).wantsGui(false));
    }

    @Test
    void mmcFlagsLandOnTheRightOptions() throws Exception {
        InstallRequest request = CliOptions.parse(
                new String[] {"mmc", "--replace-java-path", "--instance-name", "Cleanroom Test"}, this.environment).toRequest();
        assertTrue(request.flag(MmcTarget.OPTION_REPLACE_JAVA_PATH));
        assertEquals("Cleanroom Test", request.extra(MmcTarget.OPTION_INSTANCE_NAME));
    }

    @Test
    void javaFlagsBuildTheSpec() throws Exception {
        InstallRequest request = CliOptions.parse(
                new String[] {"client", "--java-version", "21", "--java-vendor", "temurin", "--provision-java"},
                this.environment).toRequest();
        assertEquals(21, request.java().target());
        assertEquals(JavaDistro.TEMURIN, request.java().distro());
        assertTrue(request.java().allowProvision());
    }

    @Test
    void jvmArgumentsSplitOnWhitespace() throws Exception {
        InstallRequest request = CliOptions.parse(
                new String[] {"client", "--jvm-args", "-Xmx6G  -XX:+UseZGC"}, this.environment).toRequest();
        assertEquals(2, request.jvmArgs().size());
        assertEquals("-Xmx6G", request.jvmArgs().get(0));
    }

    @Test
    void modesThatDoNotInstallAreRecognised() throws Exception {
        assertFalse(CliOptions.parse(new String[] {"list-versions"}, this.environment).isInstallMode());
        assertTrue(CliOptions.parse(new String[] {"server"}, this.environment).isInstallMode());
    }

}
