package com.cleanroommc.installer;

/**
 * Constants shared by every part of the installer.
 */
public final class InstallerMeta {

    public static final String NAME = "Cleanroom Installer";

    public static final String USER_AGENT = "CleanroomInstaller";

    /** Where release artifacts live. {@code repo.cleanroommc.com/releases} mirrors this host. */
    public static final String CLEANROOM_REPO = "https://maven.cleanroommc.com/";

    public static final String MAVEN_CENTRAL = "https://repo.maven.apache.org/maven2/";

    public static final String MOJANG_LIBRARIES = "https://libraries.minecraft.net/";

    public static final String VERSION_MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";

    public static final String GITHUB_RELEASES = "https://api.github.com/repos/CleanroomMC/Cleanroom/releases";

    public static final String CLEANROOM_GROUP = "com.cleanroommc";

    public static final String CLEANROOM_ARTIFACT = "cleanroom";

    /** Cleanroom targets this and only this. Used for vanilla artifact coordinates, not for ids. */
    public static final String MINECRAFT_VERSION = "1.12.2";

    /**
     * The version id written to {@code versions/<id>/}. Cleanroom is always Minecraft
     * {@link #MINECRAFT_VERSION}, so no Minecraft version appears in the id.
     */
    public static String versionId(String cleanroomVersion) {
        return "Cleanroom-" + cleanroomVersion;
    }

    private InstallerMeta() { }

}
