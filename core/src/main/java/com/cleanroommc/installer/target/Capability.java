/*
 * Copyright (c) 2026 CleanroomMC contributors
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package com.cleanroommc.installer.target;

/**
 * What a target supports, so the CLI and GUI can offer only what will work.
 */
public enum Capability {

    DRY_RUN,
    UNINSTALL,
    JAVA_PIN,
    NEEDS_NETWORK

}
