package com.cleanroommc.installer.target.mmc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MmcInstanceTest {

    @TempDir
    Path temp;

    @Test
    void anEmptyDirectoryIsNoInstance() {
        assertEquals(MmcInstance.Kind.NONE, MmcInstance.inspect(this.temp).kind());
        assertFalse(MmcInstance.inspect(this.temp).kind().exists());
    }

    @Test
    void cleanroomIsToldApartFromForgeByTheComponentName() throws Exception {
        Path instance = instance("Cleanroom", "0.6.11-alpha");
        MmcInstance found = MmcInstance.inspect(instance);
        assertEquals(MmcInstance.Kind.CLEANROOM, found.kind());
        assertEquals("0.6.11-alpha", found.loaderVersion());
        assertEquals("1.12.2", found.minecraftVersion());
        assertTrue(found.isCleanroom("0.6.11-alpha"));
        assertFalse(found.isCleanroom("0.6.12-alpha"), "another version is an upgrade, not a repair");
    }

    @Test
    void aForgeInstanceKeepsTheSameComponentUid() throws Exception {
        MmcInstance found = MmcInstance.inspect(instance("Forge", "14.23.5.2860"));
        assertEquals(MmcInstance.Kind.FORGE, found.kind());
        assertEquals("14.23.5.2860", found.loaderVersion());
        assertFalse(found.isCleanroom("14.23.5.2860"));
    }

    @Test
    void withoutACachedNameThePatchNameDecides() throws Exception {
        Path instance = instance(null, "0.6.11-alpha");
        patch(instance, "{\"uid\":\"net.minecraftforge\",\"name\":\"Cleanroom\"}");
        assertEquals(MmcInstance.Kind.CLEANROOM, MmcInstance.inspect(instance).kind());
    }

    @Test
    void withoutANameAnywhereTheLibrariesDecide() throws Exception {
        Path instance = instance(null, "0.5.0-alpha");
        patch(instance, "{\"uid\":\"net.minecraftforge\",\"libraries\":["
                + "{\"name\":\"com.paulscode:codecjorbis:20101023\"},"
                + "{\"name\":\"com.cleanroommc:cleanroom:0.5.0-alpha\"}]}");
        assertEquals(MmcInstance.Kind.CLEANROOM, MmcInstance.inspect(instance).kind(),
                "the oldest pack zips are only recognisable by this library");
    }

    @Test
    void aPatchWithNeitherANameNorTheLibraryIsForge() throws Exception {
        Path instance = instance(null, "14.23.5.2860");
        patch(instance, "{\"uid\":\"net.minecraftforge\",\"libraries\":["
                + "{\"name\":\"net.minecraftforge:forge:1.12.2-14.23.5.2860\"}]}");
        assertEquals(MmcInstance.Kind.FORGE, MmcInstance.inspect(instance).kind());
    }

    @Test
    void aCachedNameIsNotSecondGuessed() throws Exception {
        Path instance = instance("Forge", "14.23.5.2860");
        patch(instance, "{\"uid\":\"net.minecraftforge\",\"name\":\"Cleanroom\"}");
        assertEquals(MmcInstance.Kind.FORGE, MmcInstance.inspect(instance).kind(),
                "a pack that names itself Forge is Forge");
    }

    @Test
    void anInstanceWithoutALoaderIsVanilla() throws Exception {
        Path instance = this.temp.resolve("vanilla");
        Files.createDirectories(instance);
        write(instance.resolve("instance.cfg"), "name=Plain\n");
        write(instance.resolve("mmc-pack.json"),
                "{\"components\":[{\"uid\":\"net.minecraft\",\"version\":\"1.12.2\"}]}");
        MmcInstance found = MmcInstance.inspect(instance);
        assertEquals(MmcInstance.Kind.VANILLA, found.kind());
        assertEquals("Plain", found.name());
    }

    @Test
    void anUnparseablePackStillCountsAsAnInstance() throws Exception {
        Path instance = this.temp.resolve("broken");
        Files.createDirectories(instance);
        write(instance.resolve("mmc-pack.json"), "{not json");
        assertEquals(MmcInstance.Kind.VANILLA, MmcInstance.inspect(instance).kind());
    }

    private Path instance(String cachedName, String version) throws IOException {
        Path instance = this.temp.resolve(cachedName + "-" + version);
        Files.createDirectories(instance);
        write(instance.resolve("instance.cfg"), "name=" + cachedName + " Pack\n");
        write(instance.resolve("mmc-pack.json"), "{\"components\":["
                + "{\"uid\":\"net.minecraft\",\"version\":\"1.12.2\"},"
                + "{\"uid\":\"net.minecraftforge\","
                + (cachedName == null ? "" : "\"cachedName\":\"" + cachedName + "\",")
                + "\"version\":\"" + version + "\"}"
                + "]}");
        return instance;
    }

    private void patch(Path instance, String content) throws IOException {
        Files.createDirectories(instance.resolve("patches"));
        write(instance.resolve("patches/net.minecraftforge.json"), content);
    }

    private static void write(Path path, String content) throws IOException {
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    }

}
