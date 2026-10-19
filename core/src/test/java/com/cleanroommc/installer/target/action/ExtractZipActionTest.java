/*
 * Copyright (c) 2026 CleanroomMC contributors
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package com.cleanroommc.installer.target.action;

import com.cleanroommc.installer.target.ExitCode;
import com.cleanroommc.installer.target.InstallException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class ExtractZipActionTest {

    @TempDir Path directory;

    @Test
    void anEntryThatEscapesTheDestinationIsRefused() throws Exception {
        Path archive = this.directory.resolve("pack.zip");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            zip.putNextEntry(new ZipEntry("../escaped.txt"));
            zip.write('x');
        }

        InstallException failure = catchThrowableOfType(InstallException.class,
            () -> new ExtractZipAction(archive, this.directory.resolve("instance")).execute(null)
        );

        assertThat(failure.exitCode()).isEqualTo(ExitCode.VERIFICATION);
        assertThat(this.directory.resolve("escaped.txt")).doesNotExist();
    }

}
