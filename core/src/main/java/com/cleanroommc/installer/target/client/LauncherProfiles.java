package com.cleanroommc.installer.target.client;

import com.cleanroommc.installer.target.ExitCode;
import com.cleanroommc.installer.target.InstallException;
import com.cleanroommc.installer.util.Json;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class LauncherProfiles {

    public static final String FILE = "launcher_profiles.json";
    public static final String MICROSOFT_STORE_FILE = "launcher_profiles_microsoft_store.json";

    private static final String ISO = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    private final Path file;

    public LauncherProfiles(Path file) {
        this.file = file;
    }

    public static Path locate(Path gameDirectory) {
        Path standard = gameDirectory.resolve(FILE);
        if (Files.isRegularFile(standard)) {
            return standard;
        }
        Path store = gameDirectory.resolve(MICROSOFT_STORE_FILE);
        if (Files.isRegularFile(store)) {
            return store;
        }
        return standard;
    }

    public Path file() {
        return this.file;
    }

    public boolean alreadyHas(String key, String versionId) {
        try {
            JsonObject document = load();
            JsonObject profiles = document.getAsJsonObject("profiles");
            if (profiles == null || !profiles.has(key)) {
                return false;
            }
            JsonObject profile = profiles.getAsJsonObject(key);
            return profile.has("lastVersionId")
                    && versionId.equals(profile.get("lastVersionId").getAsString());
        } catch (InstallException e) {
            return false;
        }
    }

    /**
     * Inserts or updates one profile, preserving everything else in the file.
     */
    public void merge(String key, String name, String versionId, String javaDir, String javaArgs, String icon) throws InstallException {
        JsonObject document = load();
        backup();

        JsonObject profiles = document.getAsJsonObject("profiles");
        if (profiles == null) {
            profiles = new JsonObject();
            document.add("profiles", profiles);
        }

        String now = new SimpleDateFormat(ISO).format(new Date());
        JsonObject profile = profiles.has(key) && profiles.get(key).isJsonObject() ? profiles.getAsJsonObject(key) : new JsonObject();

        if (!profile.has("created")) {
            profile.addProperty("created", now);
        }
        profile.addProperty("lastUsed", profile.has("lastUsed") ? profile.get("lastUsed").getAsString() : now);
        profile.addProperty("name", name);
        profile.addProperty("type", "custom");
        profile.addProperty("lastVersionId", versionId);
        if (icon != null) {
            profile.addProperty("icon", icon);
        }
        if (javaDir != null) {
            profile.addProperty("javaDir", javaDir);
        }
        if (javaArgs != null && !javaArgs.isEmpty()) {
            profile.addProperty("javaArgs", javaArgs);
        }
        profiles.add(key, profile);

        write(document);
    }

    public boolean remove(String key) throws InstallException {
        if (!Files.isRegularFile(this.file)) {
            return false;
        }
        JsonObject document = load();
        JsonObject profiles = document.getAsJsonObject("profiles");
        if (profiles == null || !profiles.has(key)) {
            return false;
        }
        backup();
        profiles.remove(key);
        write(document);
        return true;
    }

    JsonObject load() throws InstallException {
        if (!Files.isRegularFile(this.file)) {
            return skeleton();
        }
        try {
            JsonObject document = Json.readObject(this.file);
            return document == null ? skeleton() : document;
        } catch (JsonSyntaxException | IOException e) {
            throw new InstallException(ExitCode.TARGET,
                    "Unable to read " + this.file + ". Move it aside and re-run to start from a fresh one.", e);
        }
    }

    private void backup() throws InstallException {
        if (!Files.isRegularFile(this.file)) {
            return;
        }
        String stem = this.file.getFileName() + ".bak-" + System.currentTimeMillis();
        try {
            Path backup = this.file.resolveSibling(stem);
            for (int suffix = 2; Files.exists(backup); suffix++) {
                backup = this.file.resolveSibling(stem + "-" + suffix);
            }
            Files.copy(this.file, backup, StandardCopyOption.COPY_ATTRIBUTES);
        } catch (IOException e) {
            throw new InstallException(ExitCode.TARGET, "Unable to back up " + this.file, e);
        }
    }

    private void write(JsonObject document) throws InstallException {
        try {
            Json.writeString(this.file, Json.GSON.toJson(document) + "\n");
        } catch (IOException e) {
            throw new InstallException(ExitCode.TARGET, "Unable to write " + this.file, e);
        }
    }

    private static JsonObject skeleton() {
        JsonObject document = new JsonObject();
        document.add("profiles", new JsonObject());
        JsonObject settings = new JsonObject();
        settings.addProperty("enableSnapshots", false);
        settings.addProperty("enableAdvanced", false);
        settings.addProperty("keepLauncherOpen", false);
        document.add("settings", settings);
        document.addProperty("version", 3);
        return document;
    }

    static String readAll(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

}
