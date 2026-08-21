package com.cleanroommc.installer.ui;

import com.cleanroommc.installer.platform.Environment;
import com.cleanroommc.installer.util.Json;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Where the window remembers whether the user prefers the dark palette.
 * <p>
 * The relauncher keeps this in the game's config directory. The installer has no game directory to
 * write into, so it uses its own file under the shared Cleanroom home.
 */
public interface ThemeStore {

    boolean dark();

    void dark(boolean dark);

    static ThemeStore get() {
        return FileThemeStore.INSTANCE;
    }

    final class FileThemeStore implements ThemeStore {

        static final FileThemeStore INSTANCE = new FileThemeStore(Environment.current().installerCache().resolve("ui.json"));

        private final Path file;
        private Boolean cached;

        FileThemeStore(Path file) {
            this.file = file;
        }

        @Override
        public boolean dark() {
            if (this.cached == null) {
                this.cached = read();
            }
            return this.cached;
        }

        @Override
        public void dark(boolean dark) {
            this.cached = dark;
            JsonObject document = new JsonObject();
            document.addProperty("darkMode", dark);
            try {
                Json.writeString(this.file, Json.GSON.toJson(document) + "\n");
            } catch (IOException ignored) { }
        }

        private boolean read() {
            try {
                if (!Files.isRegularFile(this.file)) {
                    return true;
                }
                JsonObject document = Json.readObject(this.file);
                return document == null || !document.has("darkMode") || document.get("darkMode").getAsBoolean();
            } catch (IOException | RuntimeException e) {
                return true;
            }
        }

    }

}
