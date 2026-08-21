package com.cleanroommc.installer.platform;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Directory detection for all three platforms, on whichever one happens to be running the tests.
 */
class InstallLocationsTest {

    @TempDir
    Path home;

    @Test
    void findsTheLinuxGameDirectory() throws IOException {
        Path minecraft = this.home.resolve(".minecraft");
        Files.createDirectories(minecraft.resolve("versions"));
        assertEquals(minecraft, InstallLocations.minecraft(env("linux")));
    }

    @Test
    void findsTheMacGameDirectory() throws IOException {
        Path minecraft = this.home.resolve("Library/Application Support/minecraft");
        Files.createDirectories(minecraft);
        Files.write(minecraft.resolve("launcher_profiles.json"), "{}".getBytes(StandardCharsets.UTF_8));
        assertEquals(minecraft, InstallLocations.minecraft(env("macos")));
    }

    @Test
    void prefersAnExistingInstallationOverTheConventionalPath() throws IOException {
        Path xdg = this.home.resolve("xdg/minecraft");
        Files.createDirectories(xdg.resolve("versions"));
        FakeEnvironment env = env("linux");
        env.variables.put("XDG_DATA_HOME", this.home.resolve("xdg").toString());
        assertEquals(xdg, InstallLocations.minecraft(env));
    }

    @Test
    void fallsBackToTheConventionalPathWhenNothingExists() {
        assertEquals(this.home.resolve(".minecraft"), InstallLocations.minecraft(env("linux")));
    }

    @Test
    void recognisesAnMmcInstanceAndDoesNotConfuseItForAGameDirectory() throws IOException {
        Path instance = this.home.resolve("instance");
        Files.createDirectories(instance);
        Files.write(instance.resolve("instance.cfg"), "InstanceType=OneSix".getBytes(StandardCharsets.UTF_8));
        assertTrue(InstallLocations.looksLikeMmcInstance(instance));
        assertFalse(InstallLocations.looksLikeMinecraft(instance));
    }

    @Test
    void honoursAConfiguredInstanceDirectory() throws IOException {
        Path root = this.home.resolve(".local/share/PrismLauncher");
        Path instances = this.home.resolve("elsewhere/instances");
        Files.createDirectories(instances);
        Files.createDirectories(root);
        Files.write(root.resolve("prismlauncher.cfg"),
                ("InstanceDir=" + root.relativize(instances) + "\n").getBytes(StandardCharsets.UTF_8));

        List<DetectedLauncher> found = InstallLocations.multiMcFamily(env("linux"));
        assertEquals(1, found.size());
        assertEquals(DetectedLauncher.Kind.PRISM, found.get(0).kind());
        assertEquals(instances, found.get(0).instances());
    }

    @Test
    void fallsBackToTheDefaultInstancesDirectory() throws IOException {
        Path root = this.home.resolve(".local/share/PrismLauncher");
        Files.createDirectories(root.resolve("instances"));
        List<DetectedLauncher> found = InstallLocations.multiMcFamily(env("linux"));
        assertEquals(1, found.size());
        assertEquals(root.resolve("instances"), found.get(0).instances());
    }

    private FakeEnvironment env(String os) {
        return new FakeEnvironment(this.home, os);
    }

    /** An {@link Environment} that reports whatever platform the test asks for. */
    private static final class FakeEnvironment extends Environment {

        final Map<String, String> variables = new HashMap<>();
        private final Path home;
        private final String os;

        FakeEnvironment(Path home, String os) {
            this.home = home;
            this.os = os;
        }

        @Override
        public String env(String name) {
            return this.variables.get(name);
        }

        @Override
        public Path home() {
            return this.home;
        }

        @Override
        public Path workingDirectory() {
            return this.home.resolve("cwd");
        }

        @Override
        public boolean windows() {
            return "windows".equals(this.os);
        }

        @Override
        public boolean macOs() {
            return "macos".equals(this.os);
        }

        @Override
        public boolean linux() {
            return "linux".equals(this.os);
        }

    }

}
