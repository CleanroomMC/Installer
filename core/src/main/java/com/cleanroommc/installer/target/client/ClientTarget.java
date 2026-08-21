package com.cleanroommc.installer.target.client;

import com.cleanroommc.installer.InstallerMeta;
import com.cleanroommc.installer.java.JavaResolution;
import com.cleanroommc.javautils.api.JavaDistro;
import com.cleanroommc.installer.java.JavaSpec;
import com.cleanroommc.installer.maven.Coordinate;
import com.cleanroommc.installer.maven.MavenLayout;
import com.cleanroommc.installer.platform.Environment;
import com.cleanroommc.installer.platform.InstallLocations;
import com.cleanroommc.installer.profile.InstallProfile;
import com.cleanroommc.installer.profile.VersionJson;
import com.cleanroommc.installer.target.AbstractInstallTarget;
import com.cleanroommc.installer.target.action.Action;
import com.cleanroommc.installer.target.Capability;
import com.cleanroommc.installer.target.action.CopyResourceAction;
import com.cleanroommc.installer.target.action.DownloadAction;
import com.cleanroommc.installer.target.ExitCode;
import com.cleanroommc.installer.target.InstallContext;
import com.cleanroommc.installer.target.InstallException;
import com.cleanroommc.installer.target.InstallPlan;
import com.cleanroommc.installer.target.InstallRequest;
import com.cleanroommc.installer.target.InstallResult;
import com.cleanroommc.installer.target.action.MergeJsonAction;
import com.cleanroommc.installer.target.action.WriteFileAction;
import com.cleanroommc.installer.util.Json;
import com.cleanroommc.platformutils.Platform;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Installs into a Mojang-launcher game directory.
 */
public final class ClientTarget extends AbstractInstallTarget {

    public static final String ID = "client";
    public static final String OPTION_PROFILE_NAME = "profileName";
    public static final String OPTION_NO_PROFILE = "noProfile";
    public static final String OPTION_FULL = "full";
    public static final String OPTION_LAUNCHER_PROFILES = "launcherProfiles";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "Client";
    }

    @Override
    public String description() {
        return "Adds a Cleanroom profile to the Minecraft launcher";
    }

    @Override
    public Path defaultDirectory(Environment environment) {
        return InstallLocations.minecraft(environment);
    }

    @Override
    public Set<Capability> capabilities() {
        return capabilitySet(Capability.DRY_RUN, Capability.UNINSTALL, Capability.JAVA_PIN, Capability.NEEDS_NETWORK);
    }

    @Override
    public void validate(InstallRequest request, InstallContext context) throws InstallException {
        Path directory = directory(request, context.env());
        if (InstallLocations.looksLikeMmcInstance(directory)) {
            throw new InstallException(ExitCode.TARGET,
                    directory + " is a MultiMC/Prism instance, not a Minecraft installation."
                            + System.lineSeparator() + "Use the 'mmc' mode for those.");
        }
        if (!Files.isDirectory(directory)) {
            if (!request.assumeYes()) {
                throw new InstallException(ExitCode.TARGET,
                        directory + " does not exist. Pass --yes to create it, or --dir to point somewhere else.");
            }
        } else if (!InstallLocations.looksLikeMinecraft(directory) && !request.assumeYes()) {
            throw new InstallException(ExitCode.TARGET,
                    directory + " does not look like a Minecraft installation (no launcher_profiles.json, no versions/)."
                            + System.lineSeparator() + "Pass --yes to install there anyway, or --dir to point elsewhere.");
        }
        if (Files.isDirectory(directory) && !Files.isWritable(directory)) {
            throw new InstallException(ExitCode.TARGET, "No write permission for " + directory);
        }
    }

    @Override
    public InstallPlan plan(InstallRequest request, InstallContext context) throws InstallException {
        Path root = directory(request, context.env());
        InstallProfile profile = context.profile();
        VersionJson version = context.versionJson();
        String versionId = version.id != null ? version.id : InstallerMeta.versionId(profile.cleanroomVersion());

        InstallPlan plan = new InstallPlan(ID, versionId, root);
        Path libraries = root.resolve("libraries");

        if (!request.force() && alreadyInstalled(request, root, versionId, profile)) {
            // Re-running an install that is already complete should cost nothing and change nothing
            return plan;
        }

        plan.add(new WriteFileAction(
                root.resolve("versions").resolve(versionId).resolve(versionId + ".json"),
                Json.GSON.toJson(version) + "\n"));

        plan.addAll(universalActions(profile, libraries, context));

        if (request.flag(OPTION_FULL)) {
            plan.addAll(MavenLayout.actions(version.libraries(), libraries, context.source(), Platform.current(), true));
        }

        if (!request.flag(OPTION_NO_PROFILE)) {
            plan.addAll(profileActions(request, context, profile, versionId, root, plan));
        }
        return plan;
    }

    private boolean alreadyInstalled(InstallRequest request, Path root, String versionId, InstallProfile profile) {
        Path versionFile = root.resolve("versions").resolve(versionId).resolve(versionId + ".json");
        if (!Files.isRegularFile(versionFile)) {
            return false;
        }
        Coordinate universal = Coordinate.parse(profile.path);
        if (!Files.isRegularFile(root.resolve("libraries").resolve(universal.path().replace('/', File.separatorChar)))) {
            return false;
        }
        if (request.flag(OPTION_NO_PROFILE)) {
            return true;
        }
        Path profilesFile = request.extra(OPTION_LAUNCHER_PROFILES) != null
                ? Paths.get(request.extra(OPTION_LAUNCHER_PROFILES))
                : LauncherProfiles.locate(root);
        return new LauncherProfiles(profilesFile).alreadyHas("cleanroom-" + profile.cleanroomVersion(), versionId);
    }

    private List<Action> universalActions(InstallProfile profile, Path libraries, InstallContext context)
            throws InstallException {
        List<Action> actions = new ArrayList<>();
        if (profile.path == null || profile.path.isEmpty()) {
            throw new InstallException(ExitCode.INTERNAL, "install_profile.json has no 'path' for the universal jar");
        }
        Coordinate coordinate = Coordinate.parse(profile.path);
        Path destination = libraries.resolve(coordinate.path().replace('/', File.separatorChar));
        String entry = MavenLayout.EMBEDDED_ROOT + coordinate.path();
        boolean embedded;
        try {
            embedded = context.source().open(entry) != null;
        } catch (IOException e) {
            embedded = false;
        }
        if (embedded) {
            actions.add(new CopyResourceAction(entry, () -> context.source().open(entry), destination));
        } else {
            actions.add(new DownloadAction(
                    InstallerMeta.CLEANROOM_REPO + coordinate.path(), destination, null, 0L));
        }
        return actions;
    }

    private List<Action> profileActions(InstallRequest request, InstallContext context, InstallProfile profile,
                                        String versionId, Path root, InstallPlan plan) throws InstallException {
        Path profilesFile = request.extra(OPTION_LAUNCHER_PROFILES) != null
                ? context.env().path(request.extra(OPTION_LAUNCHER_PROFILES))
                : LauncherProfiles.locate(root);
        LauncherProfiles profiles = new LauncherProfiles(profilesFile);

        JavaSpec spec = request.java()
                .withBounds(profile.java.minimum, profile.java.maximum, profile.java.recommended)
                .withDistro(request.java().distro() == null ? JavaDistro.match(profile.java.distro) : request.java().distro());
        JavaResolution java = context.javaResolver().resolve(spec, context.listener());
        plan.note("Java " + java.major() + " at " + java.home() + " (" + java.origin().name().toLowerCase() + ")");

        String name = request.extra(OPTION_PROFILE_NAME, profile.profileName() + " " + profile.cleanroomVersion());
        String key = "cleanroom-" + profile.cleanroomVersion();
        String jvmArgs = jvmArgs(request, profile);
        String javaDir = java.home().toString();
        String icon = profile.icon;

        List<Action> actions = new ArrayList<>();
        actions.add(new MergeJsonAction(profilesFile, "profile '" + name + "'",
                (document, ctx) -> profiles.merge(key, name, versionId, javaDir, jvmArgs, icon)));
        return actions;
    }

    static String jvmArgs(InstallRequest request, InstallProfile profile) {
        List<String> args = new ArrayList<>();
        if (!request.jvmArgs().isEmpty()) {
            args.addAll(request.jvmArgs());
        } else {
            args.addAll(profile.jvmArgs());
        }
        StringBuilder builder = new StringBuilder();
        for (String arg : args) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(arg);
        }
        return builder.toString();
    }

    @Override
    public InstallResult uninstall(InstallRequest request, InstallContext context) throws InstallException {
        Path root = directory(request, context.env());
        InstallProfile profile = context.profile();
        String versionId = context.versionJson().id;
        InstallResult result = new InstallResult(root);

        Path versionDirectory = root.resolve("versions").resolve(versionId);
        try {
            if (Files.isDirectory(versionDirectory)) {
                deleteRecursively(versionDirectory);
                result.note("Removed " + versionDirectory);
            }
        } catch (IOException e) {
            throw new InstallException(ExitCode.TARGET, "Unable to remove " + versionDirectory, e);
        }

        LauncherProfiles profiles = new LauncherProfiles(LauncherProfiles.locate(root));
        if (profiles.remove("cleanroom-" + profile.cleanroomVersion())) {
            result.note("Removed the launcher profile");
        }
        result.note("Libraries under " + root.resolve("libraries") + " were left in place as other versions may use them");
        return result;
    }

    private static void deleteRecursively(Path directory) throws IOException {
        List<Path> paths = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(directory)) {
            walk.forEach(paths::add);
        }
        Collections.reverse(paths);
        for (Path path : paths) {
            Files.deleteIfExists(path);
        }
    }

    private Path directory(InstallRequest request, Environment environment) {
        return request.directory() != null ? request.directory() : defaultDirectory(environment);
    }

}
