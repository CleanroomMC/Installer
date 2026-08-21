package com.cleanroommc.installer.target.mmc;

import com.cleanroommc.installer.util.Json;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class MmcInstance {

    public enum Kind {

        NONE,
        VANILLA,
        FORGE,
        CLEANROOM;

        public boolean exists() {
            return this != NONE;
        }

    }

    private static final String LOADER_UID = "net.minecraftforge";
    private static final String CLEANROOM_NAME = "Cleanroom";
    private static final String CLEANROOM_LIBRARY = "com.cleanroommc:cleanroom:";

    private static JsonObject readObject(Path path) {
        try {
            return Files.isRegularFile(path) ? Json.readObject(path) : null;
        } catch (IOException | RuntimeException unreadable) {
            return null;
        }
    }

    private static String string(JsonObject object, String key) {
        JsonElement value = object == null ? null : object.get(key);
        return value != null && value.isJsonPrimitive() ? value.getAsString() : null;
    }

    private static String configValue(Path config, String key) {
        if (!Files.isRegularFile(config)) {
            return null;
        }
        try {
            for (String line : Files.readAllLines(config, StandardCharsets.UTF_8)) {
                int separator = line.indexOf('=');
                if (separator > 0 && line.substring(0, separator).trim().equals(key)) {
                    return line.substring(separator + 1).trim();
                }
            }
        } catch (IOException ignored) { }
        return null;
    }

    private final Kind kind;
    private final String name;
    private final String loaderVersion;
    private final String minecraftVersion;

    private MmcInstance(Kind kind, String name, String loaderVersion, String minecraftVersion) {
        this.kind = kind;
        this.name = name;
        this.loaderVersion = loaderVersion;
        this.minecraftVersion = minecraftVersion;
    }

    public static MmcInstance inspect(Path directory) {
        Path pack = directory.resolve("mmc-pack.json");
        if (!Files.isRegularFile(pack) && !Files.isRegularFile(directory.resolve("instance.cfg"))) {
            return new MmcInstance(Kind.NONE, null, null, null);
        }
        String name = configValue(directory.resolve("instance.cfg"), "name");
        JsonObject loader = null;
        String minecraft = null;
        if (Files.isRegularFile(pack)) {
            JsonObject root = readObject(pack);
            JsonArray components = root != null && root.get("components") != null && root.get("components").isJsonArray()
                    ? root.getAsJsonArray("components") : new JsonArray();
            for (JsonElement element : components) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject component = element.getAsJsonObject();
                String uid = string(component, "uid");
                if (LOADER_UID.equals(uid)) {
                    loader = component;
                } else if ("net.minecraft".equals(uid)) {
                    minecraft = string(component, "version");
                }
            }
        }
        if (loader == null) {
            return new MmcInstance(Kind.VANILLA, name, null, minecraft);
        }
        return new MmcInstance(isCleanroom(directory, loader) ? Kind.CLEANROOM : Kind.FORGE, name, string(loader, "version"), minecraft);
    }

    private static boolean isCleanroom(Path directory, JsonObject loader) {
        String cachedName = string(loader, "cachedName");
        if (cachedName != null && !cachedName.isEmpty()) {
            return CLEANROOM_NAME.equalsIgnoreCase(cachedName);
        }
        JsonObject patch = readObject(directory.resolve("patches").resolve(LOADER_UID + ".json"));
        String patchName = string(patch, "name");
        if (patchName != null && !patchName.isEmpty()) {
            return CLEANROOM_NAME.equalsIgnoreCase(patchName);
        }
        return hasCleanroomLibrary(patch);
    }

    private static boolean hasCleanroomLibrary(JsonObject patch) {
        JsonElement libraries = patch == null ? null : patch.get("libraries");
        if (libraries == null || !libraries.isJsonArray()) {
            return false;
        }
        for (JsonElement element : libraries.getAsJsonArray()) {
            if (!element.isJsonObject()) {
                continue;
            }
            String coordinate = string(element.getAsJsonObject(), "name");
            if (coordinate != null && coordinate.startsWith(CLEANROOM_LIBRARY)) {
                return true;
            }
        }
        return false;
    }

    public Kind kind() {
        return this.kind;
    }

    public String name() {
        return this.name;
    }

    public String loaderVersion() {
        return this.loaderVersion;
    }

    public String minecraftVersion() {
        return this.minecraftVersion;
    }

    public boolean isCleanroom(String version) {
        return this.kind == Kind.CLEANROOM && version != null && version.equals(this.loaderVersion);
    }

    public String describe() {
        switch (this.kind) {
            case CLEANROOM:
                return "Cleanroom " + versionOrUnknown() + " instance";
            case FORGE:
                return "Forge " + versionOrUnknown() + " instance";
            case VANILLA:
                return "instance with no mod loader in it";
            default:
                return "no instance";
        }
    }

    private String versionOrUnknown() {
        return this.loaderVersion == null ? "(unknown version)" : this.loaderVersion;
    }

}
