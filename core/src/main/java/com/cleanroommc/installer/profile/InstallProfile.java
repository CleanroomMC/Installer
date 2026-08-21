package com.cleanroommc.installer.profile;

import com.cleanroommc.installer.maven.Coordinate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code install_profile.json}. Spec 1 extends the spec 0 file previous Cleanroom installers
 * shipped: same keys, same meanings, plus the blocks under "Cleanroom additions".
 * <p>
 * {@code processors} and {@code data} stay in the model and stay empty. Forge needs them to patch a
 * client jar it may not redistribute; Cleanroom ships a universal jar and applies its binpatches at
 * runtime. Keeping the keys means adding processors later would not be a spec bump.
 */
public final class InstallProfile {

    public static final int SPEC = 1;

    public int spec = SPEC;
    public String profile;
    /** The version id, e.g. {@code Cleanroom-0.6.11-alpha}. */
    public String version;
    public String minecraft;
    public String cleanroomVersion;
    /** Path inside this jar to the version json, conventionally {@code /version.json}. */
    public String json = "/version.json";
    /** Coordinate of the universal jar, unpacked from {@code /maven}. */
    public String path;
    public String icon;
    public String logo;
    public String welcome;
    public String mirrorList;
    public boolean hideClient;
    public boolean hideServer;
    public boolean hideExtract;

    public List<Library> libraries = new ArrayList<>();
    public List<Object> processors = new ArrayList<>();
    public Map<String, Object> data = new LinkedHashMap<>();

    // Cleanroom additions.
    public Java java = new Java();
    public List<String> jvmArgs = new ArrayList<>();
    public String mainClass;
    public String serverMainClass;
    public List<String> tweakers = new ArrayList<>();
    public List<String> serverTweakers = new ArrayList<>();
    public String serverJarPath;
    public Map<String, String> repositories = new LinkedHashMap<>();

    public static final class Java {

        public int minimum = 25;
        // Cleanroom does not support feature releases past this one yet.
        public int maximum = 25;
        public int recommended = 25;
        public String distro = "zulu";

    }

    /**
     * The Cleanroom version. Profiles written before spec 1 have no explicit field for it, so it is
     * derived from the universal jar's coordinate.
     */
    public String cleanroomVersion() {
        if (this.cleanroomVersion != null && !this.cleanroomVersion.isEmpty()) {
            return this.cleanroomVersion;
        }
        if (this.path != null && !this.path.isEmpty()) {
            return Coordinate.parse(this.path).version();
        }
        return this.version;
    }

    public String profileName() {
        return this.profile == null || this.profile.isEmpty() ? "Cleanroom" : this.profile;
    }

    public List<Library> libraries() {
        return this.libraries == null ? new ArrayList<Library>() : this.libraries;
    }

    public List<String> jvmArgs() {
        return this.jvmArgs == null ? new ArrayList<String>() : this.jvmArgs;
    }

}
