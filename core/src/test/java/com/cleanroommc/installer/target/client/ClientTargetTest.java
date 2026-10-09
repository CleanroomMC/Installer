/*
 * Copyright (c) 2026 CleanroomMC contributors
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package com.cleanroommc.installer.target.client;

import com.cleanroommc.installer.platform.Environment;
import com.cleanroommc.installer.target.InstallContext;
import com.cleanroommc.installer.target.InstallException;
import com.cleanroommc.installer.target.InstallRequest;
import com.cleanroommc.installer.target.InstallResult;
import com.cleanroommc.installer.util.Log;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClientTargetTest {

    private final InstallContext context = new InstallContext(null, null, null, new Environment(), Log.console());

    @TempDir Path directory;

    @Test
    void uninstallRemovesTheInstalledVersionAndItsProfile() throws Exception {
        Path version = install("0.6.13-alpha");
        Path vanilla = Files.createDirectories(this.directory.resolve("versions/1.12.2"));

        InstallResult removed = new ClientTarget().uninstall(request(null), this.context);

        assertThat(removed.isNoOp()).isFalse();
        assertThat(version).doesNotExist();
        assertThat(vanilla).exists();
        assertThat(new LauncherProfiles(this.directory.resolve(LauncherProfiles.FILE)).alreadyHas(
            "cleanroom-0.6.13-alpha",
            "1.12.2-Cleanroom-0.6.13-alpha"
        )).isFalse();
        assertThat(new ClientTarget().uninstall(request(null), this.context).isNoOp()).isTrue();
    }

    @Test
    void uninstallAsksWhichVersionWhenSeveralAreInstalled() throws Exception {
        Path older = install("0.6.12-alpha");
        Path newer = install("0.6.13-alpha");

        assertThatThrownBy(() -> new ClientTarget().uninstall(request(null), this.context))
            .isInstanceOf(InstallException.class)
            .hasMessageContaining("--version");
        new ClientTarget().uninstall(request("0.6.12-alpha"), this.context);

        assertThat(older).doesNotExist();
        assertThat(newer).exists();
    }

    private Path install(String version) throws Exception {
        String id = "1.12.2-Cleanroom-" + version;
        new LauncherProfiles(this.directory.resolve(LauncherProfiles.FILE)).merge(
            "cleanroom-" + version,
            "Cleanroom",
            id,
            null,
            null,
            null
        );
        Path versionDirectory = Files.createDirectories(this.directory.resolve("versions").resolve(id));
        Files.createFile(versionDirectory.resolve(id + ".json"));
        return versionDirectory;
    }

    private InstallRequest request(String version) {
        return InstallRequest.builder(ClientTarget.ID).directory(this.directory).version(version).build();
    }

}
