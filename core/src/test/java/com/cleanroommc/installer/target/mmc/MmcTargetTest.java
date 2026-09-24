/*
 * Copyright (c) 2026 CleanroomMC contributors
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package com.cleanroommc.installer.target.mmc;

import com.cleanroommc.installer.platform.Environment;
import com.cleanroommc.installer.profile.InstallProfile;
import com.cleanroommc.installer.profile.VersionJson;
import com.cleanroommc.installer.source.ProfileSource;
import com.cleanroommc.installer.target.InstallContext;
import com.cleanroommc.installer.target.InstallException;
import com.cleanroommc.installer.target.InstallPlan;
import com.cleanroommc.installer.target.InstallRequest;
import com.cleanroommc.installer.target.action.CopyResourceAction;
import com.cleanroommc.installer.util.Log;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class MmcTargetTest {

    private static final String UNIVERSAL = "com.cleanroommc:cleanroom:1.0.0+build.4:universal";

    @TempDir
    Path directory;

    @Test
    void replacingJavaPathIsOptInForEveryInstall() {
        assertThat(MmcTarget.shouldReplaceJavaPath(InstallRequest.builder(MmcTarget.ID).build())).isFalse();
        assertThat(MmcTarget.shouldReplaceJavaPath(InstallRequest.builder(MmcTarget.ID).flag(MmcTarget.OPTION_REPLACE_JAVA_PATH, true).build())).isTrue();
    }

    @Test
    void embeddedPackReusesTheInstallersUniversalJar() throws Exception {
        InstallContext context = context("{\"name\":\"" + UNIVERSAL + "\",\"MMC-hint\":\"local\"}");

        InstallPlan plan = new MmcTarget().plan(request(), context);

        Path localUniversal = directory.resolve("instances/Cleanroom 1.0.0+build.4/libraries/cleanroom-1.0.0+build.4-universal.jar");
        assertThat(plan.actions()).hasSize(4);
        assertThat(plan.actions().get(2)).isInstanceOf(CopyResourceAction.class);
        CopyResourceAction copy = (CopyResourceAction) plan.actions().get(2);
        assertThat(copy.destination()).isEqualTo(localUniversal);
        copy.execute(context);
        assertThat(new String(Files.readAllBytes(localUniversal), StandardCharsets.UTF_8)).isEqualTo("universal");
    }

    @Test
    void embeddedPackWithDownloadedUniversalSkipsTheCopy() throws Exception {
        InstallContext context = context("{\"name\":\"" + UNIVERSAL + "\",\"downloads\":{}}");

        InstallPlan plan = new MmcTarget().plan(request(), context);

        assertThat(plan.actions()).hasSize(3);
        assertThat(plan.actions().stream().noneMatch(action -> action.destination().toString().contains("libraries"))).isTrue();
    }

    private InstallRequest request() {
        return InstallRequest.builder(MmcTarget.ID).directory(directory.resolve("instances")).build();
    }

    private InstallContext context(String universalLibrary) throws Exception {
        InstallProfile profile = new InstallProfile();
        profile.profile = "Cleanroom";
        profile.cleanroomVersion = "1.0.0+build.4";
        profile.path = UNIVERSAL;
        VersionJson version = new VersionJson();
        version.id = "Cleanroom-1.0.0+build.4";

        ByteArrayOutputStream pack = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(pack)) {
            zip.putNextEntry(new ZipEntry("patches/net.minecraftforge.json"));
            zip.write(("{\"libraries\":[" + universalLibrary + "]}").getBytes(StandardCharsets.UTF_8));
        }
        Map<String, byte[]> resources = new HashMap<>();
        resources.put(MmcTarget.EMBEDDED_PACK, pack.toByteArray());
        resources.put("maven/com/cleanroommc/cleanroom/1.0.0+build.4/cleanroom-1.0.0+build.4-universal.jar", "universal".getBytes(StandardCharsets.UTF_8));
        Environment environment = new Environment() {

            @Override
            public Path installerCache() {
                return directory.resolve("cache");
            }

        };
        return new InstallContext(source(profile, version, resources), null, null, environment, Log.console());
    }

    private static ProfileSource source(InstallProfile profile, VersionJson version, Map<String, byte[]> resources) {
        return new ProfileSource() {

            @Override
            public InstallProfile profile() throws InstallException {
                return profile;
            }

            @Override
            public VersionJson versionJson() throws InstallException {
                return version;
            }

            @Override
            public InputStream open(String path) {
                byte[] contents = resources.get(path);
                return contents == null ? null : new ByteArrayInputStream(contents);
            }

            @Override
            public void close() { }

        };
    }

}
