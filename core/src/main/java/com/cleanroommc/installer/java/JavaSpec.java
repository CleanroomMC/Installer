/*
 * Copyright (c) 2026 CleanroomMC contributors
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package com.cleanroommc.installer.java;

import com.cleanroommc.javautils.api.JavaDistro;

/**
 * What the user asked for in terms of a Java runtime.
 */
public final class JavaSpec {

    private final String path;
    private final int minimum;
    private final int maximum;
    private final int target;
    private final JavaDistro distro;
    private final boolean allowProvision;
    private final boolean versionChosen;

    public JavaSpec(String path, int minimum, int maximum, int target, JavaDistro distro, boolean allowProvision) {
        this(path, minimum, maximum, target, distro, allowProvision, false);
    }

    private JavaSpec(
        String path,
        int minimum,
        int maximum,
        int target,
        JavaDistro distro,
        boolean allowProvision,
        boolean versionChosen
    ) {
        this.path = path;
        this.minimum = minimum;
        this.maximum = maximum;
        this.target = target;
        this.distro = distro;
        this.allowProvision = allowProvision;
        this.versionChosen = versionChosen;
    }

    /**
     * The bounds used until an install profile is read. The profile then replaces them.
     */
    public static JavaSpec defaults() {
        return new JavaSpec(null, 25, 25, 25, JavaDistro.ZULU, false);
    }

    public String path() {
        return this.path;
    }

    public int minimum() {
        return this.minimum;
    }

    public int maximum() {
        return this.maximum;
    }

    public int target() {
        return this.target;
    }

    public JavaDistro distro() {
        return this.distro;
    }

    public boolean allowProvision() {
        return this.allowProvision;
    }

    public boolean accepts(int major) {
        return major >= this.minimum && (this.maximum <= 0 || major <= this.maximum);
    }

    public String requirement() {
        if (this.maximum <= 0) {
            return "Java " + this.minimum + " or newer";
        }
        if (this.maximum == this.minimum) {
            return "Java " + this.minimum;
        }
        return "Java " + this.minimum + " to " + this.maximum;
    }

    public JavaSpec withPath(String newPath) {
        return new JavaSpec(
            newPath,
            this.minimum,
            this.maximum,
            this.target,
            this.distro,
            this.allowProvision,
            this.versionChosen
        );
    }

    public JavaSpec withBounds(int newMinimum, int newMaximum, int newTarget) {
        return new JavaSpec(
            this.path,
            newMinimum,
            newMaximum,
            newTarget,
            this.distro,
            this.allowProvision,
            this.versionChosen
        );
    }

    /**
     * Pins the spec to one feature version the user asked for. {@link #withProfileBounds} leaves it alone afterwards.
     */
    public JavaSpec withVersion(int version) {
        return new JavaSpec(this.path, version, version, version, this.distro, this.allowProvision, true);
    }

    public JavaSpec withProfileBounds(int profileMinimum, int profileMaximum, int profileRecommended) {
        return this.versionChosen ? this : withBounds(profileMinimum, profileMaximum, profileRecommended);
    }

    public JavaSpec withDistro(JavaDistro newDistro) {
        return new JavaSpec(
            this.path,
            this.minimum,
            this.maximum,
            this.target,
            newDistro,
            this.allowProvision,
            this.versionChosen
        );
    }

    public JavaSpec withDistro(String newDistro) {
        return withDistro(newDistro == null ? null : JavaDistro.match(newDistro));
    }

    public JavaSpec withProvision(boolean allow) {
        return new JavaSpec(this.path, this.minimum, this.maximum, this.target, this.distro, allow, this.versionChosen);
    }

}
