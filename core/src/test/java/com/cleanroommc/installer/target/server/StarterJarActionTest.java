/*
 * Copyright (c) 2026 CleanroomMC contributors
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package com.cleanroommc.installer.target.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

// Public, because the server jar runs it as a main class
public class StarterJarActionTest {

    @TempDir Path directory;

    public static void main(String[] args) {
        System.out.println(String.join(" ", args));
        System.out.println(System.getProperty("java.class.path"));
    }

    private static byte[] readAll(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    @Test
    void startsTheMainClassWithTheClasspathAndArguments() throws Exception {
        Path root = this.directory.resolve("my server");
        Path library = root.resolve("libraries/with space/probe.jar");
        Files.createDirectories(library.getParent());
        String entry = StarterJarActionTest.class.getName().replace('.', '/') + ".class";
        try (OutputStream out = Files.newOutputStream(library); JarOutputStream jar = new JarOutputStream(out
        ); InputStream in = StarterJarActionTest.class.getResourceAsStream("/" + entry)) {
            jar.putNextEntry(new JarEntry(entry));
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                jar.write(buffer, 0, read);
            }
        }

        Path serverJar = root.resolve("server.jar");
        StarterJarAction action = new StarterJarAction(serverJar, root, Collections.singletonList(library),
            StarterJarActionTest.class.getName(), Arrays.asList("--tweakClass", "example.Tweaker"),
            Arrays.asList("--add-opens=java.base/java.lang=ALL-UNNAMED", "--add-exports",
                "java.base/jdk.internal.misc=ALL-UNNAMED", "-Dexample=1"
            ),
            8, 0
        );
        action.execute(null);

        assertThat(action.skippedJvmArgs()).containsExactly("-Dexample=1");
        try (JarFile jar = new JarFile(serverJar.toFile())) {
            assertThat(jar.getManifest().getMainAttributes().getValue("Add-Opens")).isEqualTo("java.base/java.lang");
            assertThat(jar.getManifest().getMainAttributes().getValue("Add-Exports")
            ).isEqualTo("java.base/jdk.internal.misc");
        }

        String java = Paths.get(System.getProperty("java.home"), "bin", "java").toString();
        Process process = new ProcessBuilder(java, "-jar", "server.jar", "nogui")
            .directory(root.toFile())
            .redirectErrorStream(true)
            .start();
        byte[] output;
        try (InputStream in = process.getInputStream()) {
            output = readAll(in);
        }
        assertThat(process.waitFor()).as(new String(output, StandardCharsets.UTF_8)).isZero();
        String[] lines = new String(output, StandardCharsets.UTF_8).split("\\R");
        assertThat(lines).hasSize(2);
        assertThat(lines[0]).isEqualTo("--tweakClass example.Tweaker nogui");
        assertThat(Paths.get(lines[1]).toRealPath()).isEqualTo(library.toRealPath());
    }

}
