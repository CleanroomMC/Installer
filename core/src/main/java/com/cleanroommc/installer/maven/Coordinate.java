package com.cleanroommc.installer.maven;

import java.util.Objects;

/**
 * A maven coordinate in {@code group:artifact:version[:classifier][@extension]} form.
 * Ported from CleanroomGradle's {@code PublishMmcPackZip.Coordinate} so both sides agree on paths.
 */
public final class Coordinate {

    private final String group;
    private final String artifact;
    private final String version;
    private final String classifier;
    private final String extension;

    public static Coordinate parse(String notation) {
        String remainder = notation;
        String extension = "jar";
        int at = remainder.indexOf('@');
        if (at >= 0) {
            extension = remainder.substring(at + 1);
            remainder = remainder.substring(0, at);
        }
        String[] parts = remainder.split(":");
        if (parts.length < 3 || parts.length > 4) {
            throw new IllegalArgumentException("Not a maven coordinate: " + notation);
        }
        return new Coordinate(parts[0], parts[1], parts[2], parts.length == 4 ? parts[3] : null, extension);
    }

    public Coordinate(String group, String artifact, String version, String classifier, String extension) {
        this.group = group;
        this.artifact = artifact;
        this.version = version;
        this.classifier = classifier == null || classifier.isEmpty() ? null : classifier;
        this.extension = extension == null || extension.isEmpty() ? "jar" : extension;
    }

    public String group() {
        return this.group;
    }

    public String artifact() {
        return this.artifact;
    }

    public String version() {
        return this.version;
    }

    public String classifier() {
        return this.classifier;
    }

    public String extension() {
        return this.extension;
    }

    public Coordinate withoutClassifier() {
        return new Coordinate(this.group, this.artifact, this.version, null, this.extension);
    }

    public String path() {
        StringBuilder builder = new StringBuilder();
        builder.append(this.group.replace('.', '/')).append('/')
                .append(this.artifact).append('/')
                .append(this.version).append('/')
                .append(this.artifact).append('-').append(this.version);
        if (this.classifier != null) {
            builder.append('-').append(this.classifier);
        }
        return builder.append('.').append(this.extension).toString();
    }

    public String fileName() {
        String path = path();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder()
                .append(this.group).append(':')
                .append(this.artifact).append(':')
                .append(this.version);
        if (this.classifier != null) {
            builder.append(':').append(this.classifier);
        }
        if (!"jar".equals(this.extension)) {
            builder.append('@').append(this.extension);
        }
        return builder.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Coordinate)) {
            return false;
        }
        Coordinate that = (Coordinate) other;
        return this.group.equals(that.group)
                && this.artifact.equals(that.artifact)
                && this.version.equals(that.version)
                && Objects.equals(this.classifier, that.classifier)
                && this.extension.equals(that.extension);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.group, this.artifact, this.version, this.classifier, this.extension);
    }

}
