package com.cleanroommc.installer.target;

import com.cleanroommc.installer.target.action.Action;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class InstallPlan {

    private final String targetId;
    private final String versionId;
    private final Path root;
    private final List<Action> actions = new ArrayList<>();
    private final List<String> notes = new ArrayList<>();

    public InstallPlan(String targetId, String versionId, Path root) {
        this.targetId = targetId;
        this.versionId = versionId;
        this.root = root;
    }

    public String targetId() {
        return this.targetId;
    }

    public String versionId() {
        return this.versionId;
    }

    public Path root() {
        return this.root;
    }

    public InstallPlan add(Action action) {
        this.actions.add(action);
        return this;
    }

    public InstallPlan addAll(List<? extends Action> actions) {
        this.actions.addAll(actions);
        return this;
    }

    public InstallPlan note(String note) {
        this.notes.add(note);
        return this;
    }

    public List<Action> actions() {
        return Collections.unmodifiableList(this.actions);
    }

    public List<String> notes() {
        return Collections.unmodifiableList(this.notes);
    }

    public boolean empty() {
        return this.actions.isEmpty();
    }

    public long networkBytes() {
        long total = 0L;
        for (Action action : this.actions) {
            total += action.networkBytes();
        }
        return total;
    }

    public String render() {
        StringBuilder builder = new StringBuilder();
        builder.append(this.targetId).append(' ').append(this.versionId)
                .append(System.lineSeparator())
                .append("  into ").append(this.root)
                .append(System.lineSeparator());
        for (Action action : this.actions) {
            builder.append("  ").append(action.describe()).append(System.lineSeparator());
        }
        builder.append("  ").append(this.actions.size()).append(" action(s), ")
                .append(networkBytes() / 1024L / 1024L).append(" MiB to download")
                .append(System.lineSeparator());
        for (String note : this.notes) {
            builder.append("  note: ").append(note).append(System.lineSeparator());
        }
        return builder.toString();
    }

}
