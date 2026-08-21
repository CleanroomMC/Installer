package com.cleanroommc.installer.profile;

import com.cleanroommc.platformutils.Platform;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class VersionJson {

    public String id;
    public String time;
    public String releaseTime;
    public String type;
    public String mainClass;
    public String assets;
    public AssetIndex assetIndex;
    public Map<String, Download> downloads;
    public JavaVersion javaVersion;
    public int complianceLevel;
    public int minimumLauncherVersion;
    public String minecraftArguments;
    public String inheritsFrom;
    public List<Library> libraries;

    public static final class AssetIndex {

        public String id;
        public String sha1;
        public long size;
        public long totalSize;
        public String url;

    }

    public static final class JavaVersion {

        public String component;
        public int majorVersion;

    }

    public List<Library> applicable(Platform platform) {
        List<Library> applicable = new ArrayList<>();
        if (this.libraries == null) {
            return applicable;
        }
        for (Library library : this.libraries) {
            if (library.allowed(platform)) {
                applicable.add(library);
            }
        }
        return applicable;
    }

    public List<Library> libraries() {
        return this.libraries == null ? new ArrayList<Library>() : this.libraries;
    }

}
