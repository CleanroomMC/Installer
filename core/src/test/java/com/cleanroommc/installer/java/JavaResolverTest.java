/*
 * Copyright (c) 2026 CleanroomMC contributors
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package com.cleanroommc.installer.java;

import com.cleanroommc.installer.platform.Environment;
import com.cleanroommc.installer.util.Log;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class JavaResolverTest {

    private String previous;

    @BeforeEach
    void remember() {
        this.previous = System.getProperty(JavaResolver.CLEANROOM_HOME_PROPERTY);
        System.clearProperty(JavaResolver.CLEANROOM_HOME_PROPERTY);
    }

    @AfterEach
    void restore() {
        if (this.previous == null) {
            System.clearProperty(JavaResolver.CLEANROOM_HOME_PROPERTY);
        } else {
            System.setProperty(JavaResolver.CLEANROOM_HOME_PROPERTY, this.previous);
        }
    }

    @Test
    void publishesTheCleanroomHomeSoJavaUtilsLocatesWhatWeProvision() {
        Path home = Paths.get("/tmp/somewhere/.cleanroom");
        new JavaResolver(
            new Environment() {

                @Override
                public Path cleanroomHome() {
                    return home;
                }

            },
            Log.console()
        );
        assertThat(System.getProperty(JavaResolver.CLEANROOM_HOME_PROPERTY)).isEqualTo(home.toString());
    }

    @Test
    void leavesAnExplicitlyGivenHomeAlone() {
        System.setProperty(JavaResolver.CLEANROOM_HOME_PROPERTY, "/opt/cleanroom");
        new JavaResolver(Environment.current(), Log.console());
        assertThat(System.getProperty(JavaResolver.CLEANROOM_HOME_PROPERTY)).isEqualTo("/opt/cleanroom");
    }

}
