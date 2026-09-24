/*
 * Copyright (c) 2026 CleanroomMC contributors
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package com.cleanroommc.installer.target.mmc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class MmcInstanceTest {

    @TempDir
    Path temp;

    @Test
    void anEmptyDirectoryIsNoInstance() {
        assertThat(MmcInstance.inspect(this.temp).kind()).isEqualTo(MmcInstance.Kind.NONE);
        assertThat(MmcInstance.inspect(this.temp).kind().exists()).isFalse();
    }

    @Test
    void cleanroomIsToldApartFromForgeByTheComponentName() throws Exception {
        Path instance = instance("Cleanroom", "0.6.11-alpha");
        MmcInstance found = MmcInstance.inspect(instance);
        assertThat(found.kind()).isEqualTo(MmcInstance.Kind.CLEANROOM);
        assertThat(found.loaderVersion()).isEqualTo("0.6.11-alpha");
        assertThat(found.minecraftVersion()).isEqualTo("1.12.2");
        assertThat(found.isCleanroom("0.6.11-alpha")).isTrue();
        assertThat(found.isCleanroom("0.6.12-alpha")).as("another version is an upgrade, not a repair").isFalse();
    }

    @Test
    void aForgeInstanceKeepsTheSameComponentUid() throws Exception {
        MmcInstance found = MmcInstance.inspect(instance("Forge", "14.23.5.2860"));
        assertThat(found.kind()).isEqualTo(MmcInstance.Kind.FORGE);
        assertThat(found.loaderVersion()).isEqualTo("14.23.5.2860");
        assertThat(found.isCleanroom("14.23.5.2860")).isFalse();
    }

    @Test
    void withoutACachedNameThePatchNameDecides() throws Exception {
        Path instance = instance(null, "0.6.11-alpha");
        patch(instance, "{\"uid\":\"net.minecraftforge\",\"name\":\"Cleanroom\"}");
        assertThat(MmcInstance.inspect(instance).kind()).isEqualTo(MmcInstance.Kind.CLEANROOM);
    }

    @Test
    void withoutANameAnywhereTheLibrariesDecide() throws Exception {
        Path instance = instance(null, "0.5.0-alpha");
        patch(
            instance,
            "{\"uid\":\"net.minecraftforge\",\"libraries\":[" + "{\"name\":\"com.paulscode:codecjorbis:20101023\"}," +
                "{\"name\":\"com.cleanroommc:cleanroom:0.5.0-alpha\"}]}"
        );
        assertThat(MmcInstance.inspect(instance).kind()).as("the oldest pack zips are only recognisable by this library").isEqualTo(MmcInstance.Kind.CLEANROOM);
    }

    @Test
    void aPatchWithNeitherANameNorTheLibraryIsForge() throws Exception {
        Path instance = instance(null, "14.23.5.2860");
        patch(instance, "{\"uid\":\"net.minecraftforge\",\"libraries\":[" + "{\"name\":\"net.minecraftforge:forge:1.12.2-14.23.5.2860\"}]}");
        assertThat(MmcInstance.inspect(instance).kind()).isEqualTo(MmcInstance.Kind.FORGE);
    }

    @Test
    void aCachedNameIsNotSecondGuessed() throws Exception {
        Path instance = instance("Forge", "14.23.5.2860");
        patch(instance, "{\"uid\":\"net.minecraftforge\",\"name\":\"Cleanroom\"}");
        assertThat(MmcInstance.inspect(instance).kind()).as("a pack that names itself Forge is Forge").isEqualTo(MmcInstance.Kind.FORGE);
    }

    @Test
    void anInstanceWithoutALoaderIsVanilla() throws Exception {
        Path instance = this.temp.resolve("vanilla");
        Files.createDirectories(instance);
        write(instance.resolve("instance.cfg"), "name=Plain\n");
        write(instance.resolve("mmc-pack.json"), "{\"components\":[{\"uid\":\"net.minecraft\",\"version\":\"1.12.2\"}]}");
        MmcInstance found = MmcInstance.inspect(instance);
        assertThat(found.kind()).isEqualTo(MmcInstance.Kind.VANILLA);
        assertThat(found.name()).isEqualTo("Plain");
    }

    @Test
    void anUnparseablePackStillCountsAsAnInstance() throws Exception {
        Path instance = this.temp.resolve("broken");
        Files.createDirectories(instance);
        write(instance.resolve("mmc-pack.json"), "{not json");
        assertThat(MmcInstance.inspect(instance).kind()).isEqualTo(MmcInstance.Kind.VANILLA);
    }

    private Path instance(String cachedName, String version) throws IOException {
        Path instance = this.temp.resolve(cachedName + "-" + version);
        Files.createDirectories(instance);
        write(instance.resolve("instance.cfg"), "name=" + cachedName + " Pack\n");
        write(
            instance.resolve("mmc-pack.json"),
            "{\"components\":[" + "{\"uid\":\"net.minecraft\",\"version\":\"1.12.2\"}," + "{\"uid\":\"net.minecraftforge\"," + (cachedName == null
                    ? ""
                    : "\"cachedName\":\"" + cachedName + "\",") + "\"version\":\"" + version + "\"}" + "]}"
        );
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
