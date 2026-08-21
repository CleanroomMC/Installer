package com.cleanroommc.installer.target;

import com.cleanroommc.installer.java.JavaSpec;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What the user asked for.
 */
public final class InstallRequest {

    private final String targetId;
    private final String version;
    private final Path directory;
    private final boolean offline;
    private final boolean dryRun;
    private final boolean force;
    private final boolean assumeYes;
    private final JavaSpec java;
    private final List<String> jvmArgs;
    private final Map<String, String> extras;

    private InstallRequest(Builder builder) {
        this.targetId = builder.targetId;
        this.version = builder.version;
        this.directory = builder.directory;
        this.offline = builder.offline;
        this.dryRun = builder.dryRun;
        this.force = builder.force;
        this.assumeYes = builder.assumeYes;
        this.java = builder.java;
        this.jvmArgs = Collections.unmodifiableList(builder.jvmArgs);
        this.extras = Collections.unmodifiableMap(builder.extras);
    }

    public static Builder builder(String targetId) {
        return new Builder(targetId);
    }

    public String targetId() {
        return this.targetId;
    }

    public String version() {
        return this.version;
    }

    public Path directory() {
        return this.directory;
    }

    public boolean offline() {
        return this.offline;
    }

    public boolean dryRun() {
        return this.dryRun;
    }

    public boolean force() {
        return this.force;
    }

    public boolean assumeYes() {
        return this.assumeYes;
    }

    public JavaSpec java() {
        return this.java;
    }

    public List<String> jvmArgs() {
        return this.jvmArgs;
    }

    public String extra(String key) {
        return this.extras.get(key);
    }

    public String extra(String key, String fallback) {
        String value = this.extras.get(key);
        return value == null ? fallback : value;
    }

    public boolean flag(String key) {
        return Boolean.parseBoolean(this.extras.get(key));
    }

    public Map<String, String> extras() {
        return this.extras;
    }

    public Builder toBuilder() {
        Builder builder = new Builder(this.targetId)
                .version(this.version)
                .directory(this.directory)
                .offline(this.offline)
                .dryRun(this.dryRun)
                .force(this.force)
                .assumeYes(this.assumeYes)
                .java(this.java);
        builder.jvmArgs.addAll(this.jvmArgs);
        builder.extras.putAll(this.extras);
        return builder;
    }

    public static final class Builder {

        private final String targetId;
        private String version;
        private Path directory;
        private boolean offline;
        private boolean dryRun;
        private boolean force;
        private boolean assumeYes;
        private JavaSpec java = JavaSpec.defaults();
        private final List<String> jvmArgs = new ArrayList<>();
        private final Map<String, String> extras = new LinkedHashMap<>();

        private Builder(String targetId) {
            this.targetId = targetId;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder directory(Path directory) {
            this.directory = directory;
            return this;
        }

        public Builder offline(boolean offline) {
            this.offline = offline;
            return this;
        }

        public Builder dryRun(boolean dryRun) {
            this.dryRun = dryRun;
            return this;
        }

        public Builder force(boolean force) {
            this.force = force;
            return this;
        }

        public Builder assumeYes(boolean assumeYes) {
            this.assumeYes = assumeYes;
            return this;
        }

        public Builder java(JavaSpec java) {
            this.java = java;
            return this;
        }

        public Builder jvmArgs(List<String> args) {
            this.jvmArgs.clear();
            this.jvmArgs.addAll(args);
            return this;
        }

        public Builder extra(String key, String value) {
            if (value != null) {
                this.extras.put(key, value);
            }
            return this;
        }

        public Builder flag(String key, boolean value) {
            this.extras.put(key, Boolean.toString(value));
            return this;
        }

        public InstallRequest build() {
            return new InstallRequest(this);
        }

    }

}
