package com.cleanroommc.installer.profile;

import com.cleanroommc.platformutils.Platform;

import java.util.List;
import java.util.Map;

/**
 * A Mojang version-json library entry.
 */
public final class Library {

    public String name;
    public Downloads downloads;
    public Map<String, String> natives;
    public List<Rule> rules;
    public Extract extract;

    public static final class Downloads {

        public Download artifact;
        public Map<String, Download> classifiers;

    }

    public static final class Extract {

        public List<String> exclude;

    }

    public boolean allowed(Platform platform) {
        if (this.rules == null || this.rules.isEmpty()) {
            return true;
        }
        boolean allowed = false;
        for (Rule rule : this.rules) {
            if (rule.applies(platform)) {
                allowed = rule.allow();
            }
        }
        return allowed;
    }

    public String nativeClassifier(Platform platform) {
        if (this.natives != null) {
            String classifier = this.natives.get(Rule.osName(platform));
            if (classifier != null) {
                return classifier.replace("${arch}", platform.is64Bit() ? "64" : "32");
            }
            return null;
        }
        String[] parts = this.name == null ? new String[0] : this.name.split(":");
        if (parts.length == 4 && parts[3].startsWith("natives-")) {
            return parts[3];
        }
        return null;
    }

    public Download artifact() {
        return this.downloads == null ? null : this.downloads.artifact;
    }

    public Download classifier(String classifier) {
        if (this.downloads == null || this.downloads.classifiers == null) {
            return null;
        }
        return this.downloads.classifiers.get(classifier);
    }

}
