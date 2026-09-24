/*
 * Copyright (c) 2026 CleanroomMC contributors
 * SPDX-License-Identifier: LGPL-3.0-only
 */

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

/**
 * Directory detection for all three platforms, on whichever one happens to be running the tests.
 */
import static org.assertj.core.api.Assertions.assertThat;

class InstallLocationsTest {

    @TempDir
    Path home;

    @Test
    void findsTheLinuxGameDirectory() throws IOException {
        Path minecraft = this.home.resolve(".minecraft");
        Files.createDirectories(minecraft.resolve("versions"));
        assertThat(InstallLocations.minecraft(env("linux"))).isEqualTo(minecraft);
    }

    @Test
    void findsTheMacGameDirectory() throws IOException {
        Path minecraft = this.home.resolve("Library/Application Support/minecraft");
        Files.createDirectories(minecraft);
        Files.write(minecraft.resolve("launcher_profiles.json"), "{}".getBytes(StandardCharsets.UTF_8));
        assertThat(InstallLocations.minecraft(env("macos"))).isEqualTo(minecraft);
    }

    @Test
    void prefersAnExistingInstallationOverTheConventionalPath() throws IOException {
        Path xdg = this.home.resolve("xdg/minecraft");
        Files.createDirectories(xdg.resolve("versions"));
        FakeEnvironment env = env("linux");
        env.variables.put("XDG_DATA_HOME", this.home.resolve("xdg").toString());
        assertThat(InstallLocations.minecraft(env)).isEqualTo(xdg);
    }

    @Test
    void fallsBackToTheConventionalPathWhenNothingExists() {
        assertThat(InstallLocations.minecraft(env("linux"))).isEqualTo(this.home.resolve(".minecraft"));
    }

    @Test
    void recognisesAnMmcInstanceAndDoesNotConfuseItForAGameDirectory() throws IOException {
        Path instance = this.home.resolve("instance");
        Files.createDirectories(instance);
        Files.write(instance.resolve("instance.cfg"), "InstanceType=OneSix".getBytes(StandardCharsets.UTF_8));
        assertThat(InstallLocations.looksLikeMmcInstance(instance)).isTrue();
        assertThat(InstallLocations.looksLikeMinecraft(instance)).isFalse();
    }

    @Test
    void honoursAConfiguredInstanceDirectory() throws IOException {
        Path root = this.home.resolve(".local/share/PrismLauncher");
        Path instances = this.home.resolve("elsewhere/instances");
        Files.createDirectories(instances);
        Files.createDirectories(root);
        Files.write(root.resolve("prismlauncher.cfg"), ("InstanceDir=" + root.relativize(instances) + "\n").getBytes(StandardCharsets.UTF_8));

        List<DetectedLauncher> found = InstallLocations.multiMcFamily(env("linux"));
        assertThat(found).hasSize(1);
        assertThat(found.get(0).kind()).isEqualTo(DetectedLauncher.Kind.PRISM);
        assertThat(found.get(0).instances()).isEqualTo(instances);
    }

    @Test
    void fallsBackToTheDefaultInstancesDirectory() throws IOException {
        Path root = this.home.resolve(".local/share/PrismLauncher");
        Files.createDirectories(root.resolve("instances"));
        List<DetectedLauncher> found = InstallLocations.multiMcFamily(env("linux"));
        assertThat(found).hasSize(1);
        assertThat(found.get(0).instances()).isEqualTo(root.resolve("instances"));
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
