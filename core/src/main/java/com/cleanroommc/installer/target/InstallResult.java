package com.cleanroommc.installer.target;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Details of an installation.
 */
public final class InstallResult {

    private final Path root;
    private final List<Path> written = new ArrayList<>();
    private final List<String> notes = new ArrayList<>();
    private final Map<String, String> details = new LinkedHashMap<>();

    private boolean noOp;

    public InstallResult(Path root) {
        this.root = root;
    }

    public Path root() {
        return this.root;
    }

    public InstallResult wrote(Path path) {
        this.written.add(path);
        return this;
    }

    public InstallResult note(String note) {
        this.notes.add(note);
        return this;
    }

    public InstallResult detail(String key, String value) {
        this.details.put(key, value);
        return this;
    }

    public InstallResult noOp(boolean noOp) {
        this.noOp = noOp;
        return this;
    }

    public boolean isNoOp() {
        return this.noOp;
    }

    public List<Path> written() {
        return Collections.unmodifiableList(this.written);
    }

    public List<String> notes() {
        return Collections.unmodifiableList(this.notes);
    }

    public Map<String, String> details() {
        return Collections.unmodifiableMap(this.details);
    }

}
