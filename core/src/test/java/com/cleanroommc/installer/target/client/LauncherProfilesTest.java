/*
 * Copyright (c) 2026 CleanroomMC contributors
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package com.cleanroommc.installer.target.client;

import com.cleanroommc.installer.target.InstallException;
import com.cleanroommc.installer.util.Json;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class LauncherProfilesTest {

    @TempDir
    Path directory;

    @Test
    void preservesUnknownKeysAndOtherProfiles() throws Exception {
        Path file = write("{\"profiles\":{\"vanilla\":{\"name\":\"Vanilla\",\"custom\":42}}," + "\"selectedUser\":{\"account\":\"abc\"},\"version\":3}");

        new LauncherProfiles(file).merge("cleanroom-1.0", "Cleanroom 1.0", "Cleanroom-1.0", "/opt/java", "-Xmx4G", null);

        JsonObject document = Json.readObject(file);
        assertThat(document.has("selectedUser")).as("unrelated top-level keys must survive").isTrue();
        assertThat(document.get("version").getAsInt()).isEqualTo(3);
        JsonObject vanilla = document.getAsJsonObject("profiles").getAsJsonObject("vanilla");
        assertThat(vanilla.get("custom").getAsInt()).as("unknown keys inside other profiles must survive").isEqualTo(42);

        JsonObject ours = document.getAsJsonObject("profiles").getAsJsonObject("cleanroom-1.0");
        assertThat(ours.get("name").getAsString()).isEqualTo("Cleanroom 1.0");
        assertThat(ours.get("lastVersionId").getAsString()).isEqualTo("Cleanroom-1.0");
        assertThat(ours.get("javaDir").getAsString()).isEqualTo("/opt/java");
        assertThat(ours.get("javaArgs").getAsString()).isEqualTo("-Xmx4G");
    }

    @Test
    void takesABackupBeforeWriting() throws Exception {
        Path file = write("{\"profiles\":{}}");
        new LauncherProfiles(file).merge("cleanroom-1.0", "Cleanroom", "id", null, null, null);
        assertThat(backups().size()).as("exactly one backup should exist after one merge").isEqualTo(1);
    }

    @Test
    void createsAMinimalFileWhenTheLauncherHasNeverRun() throws Exception {
        Path file = this.directory.resolve("launcher_profiles.json");
        new LauncherProfiles(file).merge("cleanroom-1.0", "Cleanroom", "id", null, null, null);

        JsonObject document = Json.readObject(file);
        assertThat(document.getAsJsonObject("profiles").has("cleanroom-1.0")).isTrue();
        assertThat(document.has("settings")).isTrue();
        assertThat(backups().isEmpty()).as("there was nothing to back up").isTrue();
    }

    @Test
    void updatesInPlaceRatherThanDuplicating() throws Exception {
        Path file = write("{\"profiles\":{}}");
        LauncherProfiles profiles = new LauncherProfiles(file);
        profiles.merge("cleanroom-1.0", "Cleanroom", "old-id", null, null, null);
        profiles.merge("cleanroom-1.0", "Cleanroom", "new-id", null, null, null);

        JsonObject document = Json.readObject(file);
        assertThat(document.getAsJsonObject("profiles").size()).isEqualTo(1);
        assertThat(document.getAsJsonObject("profiles").getAsJsonObject("cleanroom-1.0").get("lastVersionId").getAsString()).isEqualTo("new-id");
    }

    @Test
    void alreadyHasOnlyMatchesTheSameVersion() throws Exception {
        Path file = write("{\"profiles\":{}}");
        LauncherProfiles profiles = new LauncherProfiles(file);
        profiles.merge("cleanroom-1.0", "Cleanroom", "Cleanroom-1.0", null, null, null);

        assertThat(profiles.alreadyHas("cleanroom-1.0", "Cleanroom-1.0")).isTrue();
        assertThat(profiles.alreadyHas("cleanroom-1.0", "Cleanroom-1.1")).isFalse();
        assertThat(profiles.alreadyHas("cleanroom-2.0", "Cleanroom-1.0")).isFalse();
    }

    @Test
    void removeLeavesOtherProfilesAlone() throws Exception {
        Path file = write("{\"profiles\":{\"vanilla\":{\"name\":\"Vanilla\"}}}");
        LauncherProfiles profiles = new LauncherProfiles(file);
        profiles.merge("cleanroom-1.0", "Cleanroom", "id", null, null, null);

        assertThat(profiles.remove("cleanroom-1.0")).isTrue();
        assertThat(profiles.remove("cleanroom-1.0")).as("removing twice is not an error, just a no-op").isFalse();

        JsonObject document = Json.readObject(file);
        assertThat(document.getAsJsonObject("profiles").size()).isEqualTo(1);
        assertThat(document.getAsJsonObject("profiles").has("vanilla")).isTrue();
    }

    @Test
    void aCorruptFileFailsLoudlyInsteadOfBeingOverwritten() throws Exception {
        Path file = write("{ this is not json");
        LauncherProfiles profiles = new LauncherProfiles(file);
        InstallException failure = catchThrowableOfType(InstallException.class, () -> profiles.merge("cleanroom-1.0", "Cleanroom", "id", null, null, null));
        assertThat(failure.getMessage().contains(file.toString())).isTrue();
        assertThat(new String(Files.readAllBytes(file), StandardCharsets.UTF_8)).isEqualTo("{ this is not json");
    }

    private Path write(String content) throws IOException {
        Path file = this.directory.resolve("launcher_profiles.json");
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    private List<Path> backups() throws IOException {
        try (Stream<Path> files = Files.list(this.directory)) {
            return files.filter(path -> path.getFileName().toString().contains(".bak-")).collect(Collectors.toList());
        }
    }

}
