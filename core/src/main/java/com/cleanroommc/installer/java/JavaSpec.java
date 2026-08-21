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

    public JavaSpec(String path, int minimum, int maximum, int target, JavaDistro distro, boolean allowProvision) {
        this.path = path;
        this.minimum = minimum;
        this.maximum = maximum;
        this.target = target;
        this.distro = distro;
        this.allowProvision = allowProvision;
    }

    /**
     * Fixme: Cleanroom is pinned to Java 25 for now
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
        return new JavaSpec(newPath, this.minimum, this.maximum, this.target, this.distro, this.allowProvision);
    }

    public JavaSpec withBounds(int newMinimum, int newMaximum, int newTarget) {
        return new JavaSpec(this.path, newMinimum, newMaximum, newTarget, this.distro, this.allowProvision);
    }

    public JavaSpec withDistro(JavaDistro newDistro) {
        return new JavaSpec(this.path, this.minimum, this.maximum, this.target, newDistro, this.allowProvision);
    }

    public JavaSpec withDistro(String newDistro) {
        return withDistro(newDistro == null ? null : JavaDistro.match(newDistro));
    }

    public JavaSpec withProvision(boolean allow) {
        return new JavaSpec(this.path, this.minimum, this.maximum, this.target, this.distro, allow);
    }

}
