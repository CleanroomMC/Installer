package com.cleanroommc.installer.target.mmc;

import com.cleanroommc.installer.target.action.ExtractZipAction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstanceConfigActionTest {

    @TempDir
    Path temp;

    @Test
    void existingKeysAreReplacedAndTheRestSurvives() throws Exception {
        Path config = this.temp.resolve("instance.cfg");
        Files.write(config, String.join("\n", "name=My Pack", "JavaPath=/old/java", "iconKey=default")
                .getBytes(StandardCharsets.UTF_8));

        Map<String, String> values = new LinkedHashMap<>();
        values.put("JavaPath", "/new/java");
        values.put("OverrideJavaLocation", "true");
        new InstanceConfigAction(config, values).execute(null);

        List<String> lines = Files.readAllLines(config, StandardCharsets.UTF_8);
        assertTrue(lines.contains("name=My Pack"));
        assertTrue(lines.contains("iconKey=default"));
        assertTrue(lines.contains("JavaPath=/new/java"));
        assertTrue(lines.contains("OverrideJavaLocation=true"));
        assertEquals(4, lines.size());
    }

    @Test
    void keptNamesAreNotOverwrittenWhenTheyAlreadyExist() throws Exception {
        Path pack = this.temp.resolve("pack.zip");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(pack))) {
            write(zip, "instance.cfg", "name=Cleanroom");
            write(zip, "mmc-pack.json", "{}");
        }
        Path instance = this.temp.resolve("instance");
        Files.createDirectories(instance);
        Files.write(instance.resolve("instance.cfg"), "name=Mine".getBytes(StandardCharsets.UTF_8));

        new ExtractZipAction(pack, instance, Collections.singleton("instance.cfg")).execute(null);

        assertEquals("name=Mine", new String(Files.readAllBytes(instance.resolve("instance.cfg")), StandardCharsets.UTF_8));
        assertEquals("{}", new String(Files.readAllBytes(instance.resolve("mmc-pack.json")), StandardCharsets.UTF_8));
    }

    @Test
    void keptNamesAreStillWrittenWhenAbsent() throws Exception {
        Path pack = this.temp.resolve("pack.zip");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(pack))) {
            write(zip, "instance.cfg", "name=Cleanroom");
        }
        Path instance = this.temp.resolve("fresh");

        new ExtractZipAction(pack, instance, Collections.singleton("instance.cfg")).execute(null);

        assertEquals("name=Cleanroom", new String(Files.readAllBytes(instance.resolve("instance.cfg")), StandardCharsets.UTF_8));
    }

    private static void write(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        OutputStream out = zip;
        out.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

}
