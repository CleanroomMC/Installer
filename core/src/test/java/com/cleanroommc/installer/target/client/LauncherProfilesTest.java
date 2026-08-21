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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


class LauncherProfilesTest {

    @TempDir
    Path directory;

    @Test
    void preservesUnknownKeysAndOtherProfiles() throws Exception {
        Path file = write("{\"profiles\":{\"vanilla\":{\"name\":\"Vanilla\",\"custom\":42}},"
                + "\"selectedUser\":{\"account\":\"abc\"},\"version\":3}");

        new LauncherProfiles(file).merge("cleanroom-1.0", "Cleanroom 1.0", "Cleanroom-1.0", "/opt/java", "-Xmx4G", null);

        JsonObject document = Json.readObject(file);
        assertTrue(document.has("selectedUser"), "unrelated top-level keys must survive");
        assertEquals(3, document.get("version").getAsInt());
        JsonObject vanilla = document.getAsJsonObject("profiles").getAsJsonObject("vanilla");
        assertEquals(42, vanilla.get("custom").getAsInt(), "unknown keys inside other profiles must survive");

        JsonObject ours = document.getAsJsonObject("profiles").getAsJsonObject("cleanroom-1.0");
        assertEquals("Cleanroom 1.0", ours.get("name").getAsString());
        assertEquals("Cleanroom-1.0", ours.get("lastVersionId").getAsString());
        assertEquals("/opt/java", ours.get("javaDir").getAsString());
        assertEquals("-Xmx4G", ours.get("javaArgs").getAsString());
    }

    @Test
    void takesABackupBeforeWriting() throws Exception {
        Path file = write("{\"profiles\":{}}");
        new LauncherProfiles(file).merge("cleanroom-1.0", "Cleanroom", "id", null, null, null);
        assertEquals(1, backups().size(), "exactly one backup should exist after one merge");
    }

    @Test
    void createsAMinimalFileWhenTheLauncherHasNeverRun() throws Exception {
        Path file = this.directory.resolve("launcher_profiles.json");
        new LauncherProfiles(file).merge("cleanroom-1.0", "Cleanroom", "id", null, null, null);

        JsonObject document = Json.readObject(file);
        assertTrue(document.getAsJsonObject("profiles").has("cleanroom-1.0"));
        assertTrue(document.has("settings"));
        assertTrue(backups().isEmpty(), "there was nothing to back up");
    }

    @Test
    void updatesInPlaceRatherThanDuplicating() throws Exception {
        Path file = write("{\"profiles\":{}}");
        LauncherProfiles profiles = new LauncherProfiles(file);
        profiles.merge("cleanroom-1.0", "Cleanroom", "old-id", null, null, null);
        profiles.merge("cleanroom-1.0", "Cleanroom", "new-id", null, null, null);

        JsonObject document = Json.readObject(file);
        assertEquals(1, document.getAsJsonObject("profiles").size());
        assertEquals("new-id",
                document.getAsJsonObject("profiles").getAsJsonObject("cleanroom-1.0").get("lastVersionId").getAsString());
    }

    @Test
    void alreadyHasOnlyMatchesTheSameVersion() throws Exception {
        Path file = write("{\"profiles\":{}}");
        LauncherProfiles profiles = new LauncherProfiles(file);
        profiles.merge("cleanroom-1.0", "Cleanroom", "Cleanroom-1.0", null, null, null);

        assertTrue(profiles.alreadyHas("cleanroom-1.0", "Cleanroom-1.0"));
        assertFalse(profiles.alreadyHas("cleanroom-1.0", "Cleanroom-1.1"));
        assertFalse(profiles.alreadyHas("cleanroom-2.0", "Cleanroom-1.0"));
    }

    @Test
    void removeLeavesOtherProfilesAlone() throws Exception {
        Path file = write("{\"profiles\":{\"vanilla\":{\"name\":\"Vanilla\"}}}");
        LauncherProfiles profiles = new LauncherProfiles(file);
        profiles.merge("cleanroom-1.0", "Cleanroom", "id", null, null, null);

        assertTrue(profiles.remove("cleanroom-1.0"));
        assertFalse(profiles.remove("cleanroom-1.0"), "removing twice is not an error, just a no-op");

        JsonObject document = Json.readObject(file);
        assertEquals(1, document.getAsJsonObject("profiles").size());
        assertTrue(document.getAsJsonObject("profiles").has("vanilla"));
    }

    @Test
    void aCorruptFileFailsLoudlyInsteadOfBeingOverwritten() throws Exception {
        Path file = write("{ this is not json");
        LauncherProfiles profiles = new LauncherProfiles(file);
        InstallException failure = assertThrows(InstallException.class,
                () -> profiles.merge("cleanroom-1.0", "Cleanroom", "id", null, null, null));
        assertTrue(failure.getMessage().contains(file.toString()));
        assertEquals("{ this is not json", new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
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
