/*
 * Copyright (c) 2026 CleanroomMC contributors
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package com.cleanroommc.installer.target.action;

import com.cleanroommc.installer.target.InstallException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class CopyResourceActionTest {

    @TempDir Path directory;

    @Test
    void aFailedCopyKeepsTheExistingFile() throws Exception {
        Path file = this.directory.resolve("library.jar");
        Files.write(file, "intact".getBytes(StandardCharsets.UTF_8));
        InputStream failing = new InputStream() {

            private int served;

            @Override
            public int read() throws IOException {
                if (this.served++ < 3) {
                    return 'x';
                }
                throw new IOException("source went away");
            }

        };

        InstallException failure = catchThrowableOfType(InstallException.class,
            () -> new CopyResourceAction("library", () -> failing, file).execute(null)
        );

        assertThat(failure).isNotNull();
        assertThat(new String(Files.readAllBytes(file), StandardCharsets.UTF_8)).isEqualTo("intact");
        assertThat(this.directory.toFile().list()).containsExactly("library.jar");
    }

}
