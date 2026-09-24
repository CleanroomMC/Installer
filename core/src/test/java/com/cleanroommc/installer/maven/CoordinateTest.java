/*
 * Copyright (c) 2026 CleanroomMC contributors
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package com.cleanroommc.installer.maven;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class CoordinateTest {

    @Test
    void parsesGroupArtifactVersion() {
        Coordinate coordinate = Coordinate.parse("com.cleanroommc:cleanroom:0.6.11-alpha");
        assertThat(coordinate.group()).isEqualTo("com.cleanroommc");
        assertThat(coordinate.artifact()).isEqualTo("cleanroom");
        assertThat(coordinate.version()).isEqualTo("0.6.11-alpha");
        assertThat(coordinate.classifier()).isNull();
        assertThat(coordinate.extension()).isEqualTo("jar");
        assertThat(coordinate.path()).isEqualTo("com/cleanroommc/cleanroom/0.6.11-alpha/cleanroom-0.6.11-alpha.jar");
    }

    @Test
    void parsesClassifierAndExtension() {
        Coordinate coordinate = Coordinate.parse("de.oceanlabs.mcp:mcp_config:1.12.2-2026@zip");
        assertThat(coordinate.extension()).isEqualTo("zip");
        assertThat(coordinate.path()).isEqualTo("de/oceanlabs/mcp/mcp_config/1.12.2-2026/mcp_config-1.12.2-2026.zip");

        Coordinate natives = Coordinate.parse("org.lwjgl:lwjgl:3.4.1:natives-linux");
        assertThat(natives.classifier()).isEqualTo("natives-linux");
        assertThat(natives.path()).isEqualTo("org/lwjgl/lwjgl/3.4.1/lwjgl-3.4.1-natives-linux.jar");
        assertThat(natives.withoutClassifier().toString()).isEqualTo("org.lwjgl:lwjgl:3.4.1");
    }

    @Test
    void roundTripsThroughToString() {
        for (String notation : new String[] {
            "com.cleanroommc:cleanroom:0.6.11-alpha",
            "org.lwjgl:lwjgl:3.4.1:natives-macos-arm64",
            "de.oceanlabs.mcp:mcp_config:1.12.2@zip"
        }) {
            assertThat(Coordinate.parse(notation).toString()).isEqualTo(notation);
        }
    }

    @Test
    void rejectsNonsense() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Coordinate.parse("cleanroom"));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Coordinate.parse("a:b:c:d:e"));
    }

}
