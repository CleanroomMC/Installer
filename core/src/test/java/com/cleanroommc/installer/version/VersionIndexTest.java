/*
 * Copyright (c) 2026 CleanroomMC contributors
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package com.cleanroommc.installer.version;

import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.assertj.core.api.Assertions.assertThat;

class VersionIndexTest {

    @Test
    void readsOnlyTheVersionsListOfMavenMetadata() throws Exception {
        String xml = "<metadata><versioning><latest>0.2</latest><versions>" +
            "<version>0.1</version><version> 0.2 </version></versions></versioning></metadata>";
        assertThat(VersionIndex.parseMavenMetadata(new StringReader(xml))).containsExactly("0.1", "0.2");
    }

}
