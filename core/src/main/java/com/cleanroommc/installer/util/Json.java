package com.cleanroommc.installer.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class Json {

    public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    public static <T> T read(Path path, Class<T> type) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, type);
        }
    }

    public static <T> T read(Reader reader, Class<T> type) {
        return GSON.fromJson(reader, type);
    }

    public static JsonObject readObject(Path path) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement element = JsonParser.parseReader(reader);
            return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
        }
    }

    public static void write(Path path, Object value) throws IOException {
        writeString(path, GSON.toJson(value) + "\n");
    }

    public static void writeString(Path path, String content) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path temp = path.resolveSibling(path.getFileName() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
            writer.write(content);
        }
        try {
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicUnsupported) {
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private Json() { }

}
