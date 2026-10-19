/*
 * Copyright (c) 2026 CleanroomMC contributors
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package com.cleanroommc.installer.target.client;

import com.cleanroommc.installer.InstallerMeta;
import com.cleanroommc.installer.java.JavaResolution;
import com.cleanroommc.installer.java.JavaSpec;
import com.cleanroommc.installer.maven.Coordinate;
import com.cleanroommc.installer.maven.MavenLayout;
import com.cleanroommc.installer.platform.Environment;
import com.cleanroommc.installer.platform.InstallLocations;
import com.cleanroommc.installer.profile.InstallProfile;
import com.cleanroommc.installer.profile.VersionJson;
import com.cleanroommc.installer.target.AbstractInstallTarget;
import com.cleanroommc.installer.target.ExitCode;
import com.cleanroommc.installer.target.InstallContext;
import com.cleanroommc.installer.target.InstallException;
import com.cleanroommc.installer.target.InstallPlan;
import com.cleanroommc.installer.target.InstallRequest;
import com.cleanroommc.installer.target.InstallResult;
import com.cleanroommc.installer.target.action.Action;
import com.cleanroommc.installer.target.action.CopyResourceAction;
import com.cleanroommc.installer.target.action.DownloadAction;
import com.cleanroommc.installer.target.action.MergeJsonAction;
import com.cleanroommc.installer.target.action.WriteFileAction;
import com.cleanroommc.installer.util.Json;
import com.cleanroommc.javautils.api.JavaDistro;
import com.cleanroommc.platformutils.Platform;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
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

    private static final String VERSION_MARKER = "Cleanroom-";

    static String jvmArgs(InstallRequest request, InstallProfile profile) {
        List<String> args = new ArrayList<>();
        if (!request.jvmArgs().isEmpty()) {
            args.addAll(request.jvmArgs());
        } else {
            args.addAll(profile.jvmArgs());
        }
        // SDL only brings up its Cocoa video driver on the process main thread
        if (Platform.current().isMacOS() && !args.contains("-XstartOnFirstThread")) {
            args.add("-XstartOnFirstThread");
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

    // Version ids end in "Cleanroom-<version>", e.g. 1.12.2-Cleanroom-0.6.13-alpha
    private static List<Path> cleanroomVersions(Path root) throws InstallException {
        Path versions = root.resolve("versions");
        if (!Files.isDirectory(versions)) {
            return Collections.emptyList();
        }
        try (Stream<Path> entries = Files.list(versions)) {
            return entries.filter(entry -> entry.getFileName().toString().contains(VERSION_MARKER))
                .collect(Collectors.toList());
        } catch (IOException e) {
            throw new InstallException(ExitCode.TARGET, "Unable to read " + versions, e);
        }
    }

    private static List<Path> profileFiles(InstallRequest request, Environment environment, Path root) {
        String chosen = request.extra(OPTION_LAUNCHER_PROFILES);
        return chosen != null ? Collections.singletonList(environment.path(chosen)) : LauncherProfiles.locate(root);
    }

    private static String versionOf(Path versionDirectory) {
        String name = versionDirectory.getFileName().toString();
        return name.substring(name.indexOf(VERSION_MARKER) + VERSION_MARKER.length());
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
    public void validate(InstallRequest request, InstallContext context) throws InstallException {
        Path directory = directory(request, context.env());
        if (InstallLocations.looksLikeMmcInstance(directory)) {
            throw new InstallException(ExitCode.TARGET,
                directory + " is a MultiMC/Prism instance, not a Minecraft installation." + System.lineSeparator() +
                    "Use the 'mmc' mode for those."
            );
        }
        if (!Files.isDirectory(directory)) {
            if (!request.assumeYes()) {
                throw new InstallException(ExitCode.TARGET,
                    directory + " does not exist. Pass --yes to create it, or --dir to point somewhere else."
                );
            }
        } else if (!InstallLocations.looksLikeMinecraft(directory) && !request.assumeYes()) {
            throw new InstallException(ExitCode.TARGET,
                directory + " does not look like a Minecraft installation (no launcher_profiles.json, no versions/)." +
                    System.lineSeparator() + "Pass --yes to install there anyway, or --dir to point elsewhere."
            );
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

        if (!request.force() && alreadyInstalled(request, context.env(), root, versionId, profile)) {
            // Re-running an install that is already complete should cost nothing and change nothing
            return plan;
        }

        plan.add(new WriteFileAction(root.resolve("versions").resolve(versionId).resolve(versionId + ".json"),
            Json.GSON.toJson(version) + "\n"
        ));

        plan.addAll(universalActions(profile, libraries, context));

        if (request.flag(OPTION_FULL)) {
            plan.addAll(MavenLayout.actions(version.libraries(), libraries, context.source(), Platform.current(), true
            ));
        }

        if (!request.flag(OPTION_NO_PROFILE)) {
            plan.addAll(profileActions(request, context, profile, versionId, root, plan));
        }
        return plan;
    }

    private boolean alreadyInstalled(InstallRequest request, Environment environment, Path root, String versionId,
        InstallProfile profile
    ) {
        Path versionFile = root.resolve("versions").resolve(versionId).resolve(versionId + ".json");
        if (!Files.isRegularFile(versionFile)) {
            return false;
        }
        Coordinate universal = Coordinate.parse(profile.path);
        if (!Files.isRegularFile(root.resolve("libraries").resolve(universal.path().replace('/', File.separatorChar))
        )) {
            return false;
        }
        if (request.flag(OPTION_NO_PROFILE)) {
            return true;
        }
        for (Path profilesFile : profileFiles(request, environment, root)) {
            if (!new LauncherProfiles(profilesFile).alreadyHas("cleanroom-" + profile.cleanroomVersion(), versionId)) {
                return false;
            }
        }
        return true;
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
            actions.add(new DownloadAction(InstallerMeta.CLEANROOM_REPO + coordinate.path(), destination));
        }
        return actions;
    }

    private List<Action> profileActions(InstallRequest request, InstallContext context, InstallProfile profile,
        String versionId, Path root, InstallPlan plan
    ) throws InstallException {
        JavaSpec spec = request.java()
            .withProfileBounds(profile.java.minimum, profile.java.maximum, profile.java.recommended)
            .withDistro(request.java().distro() == null
                    ? JavaDistro.match(profile.java.distro)
                    : request.java().distro()
            );
        JavaResolution java = resolveJava(request, context, spec, plan);
        if (java != null) {
            plan.note("Java " + java.major() + " at " + java.home() + " (" + java.origin().name().toLowerCase() + ")");
        }

        String name = request.extra(OPTION_PROFILE_NAME, profile.profileName() + " " + profile.cleanroomVersion());
        String key = "cleanroom-" + profile.cleanroomVersion();
        String jvmArgs = jvmArgs(request, profile);
        String javaDir = java == null ? null : java.windowedExecutable().toString();
        String icon = profile.icon;

        List<Action> actions = new ArrayList<>();
        for (Path profilesFile : profileFiles(request, context.env(), root)) {
            actions.add(new MergeJsonAction(profilesFile, "profile '" + name + "'",
                ctx -> new LauncherProfiles(profilesFile).merge(key, name, versionId, javaDir, jvmArgs, icon)
            ));
        }
        return actions;
    }

    @Override
    public InstallResult uninstall(InstallRequest request, InstallContext context) throws InstallException {
        Path root = directory(request, context.env());
        InstallResult result = new InstallResult(root);
        List<Path> installed = cleanroomVersions(root);

        String version = request.version();
        if (version == null) {
            Set<String> versions = new TreeSet<>();
            for (Path directory : installed) {
                versions.add(versionOf(directory));
            }
            if (versions.isEmpty()) {
                return result.noOp(true);
            }
            if (versions.size() > 1) {
                throw new InstallException(ExitCode.USAGE,
                    "Several Cleanroom versions are installed in " + root + ": " + String.join(", ", versions) +
                        System.lineSeparator() + "Pass --version to choose the one to remove."
                );
            }
            version = versions.iterator().next();
        }

        for (Path directory : installed) {
            if (!versionOf(directory).equals(version)) {
                continue;
            }
            try {
                deleteRecursively(directory);
            } catch (IOException e) {
                throw new InstallException(ExitCode.TARGET, "Unable to remove " + directory, e);
            }
            result.note("Removed " + directory);
        }

        for (Path profilesFile : profileFiles(request, context.env(), root)) {
            if (new LauncherProfiles(profilesFile).remove("cleanroom-" + version)) {
                result.note("Removed the launcher profile from " + profilesFile.getFileName());
            }
        }
        if (result.notes().isEmpty()) {
            return result.noOp(true);
        }
        result.note("Libraries under " + root.resolve("libraries") +
                " were left in place as other versions may use them"
        );
        return result;
    }

    private Path directory(InstallRequest request, Environment environment) {
        return request.directory() != null ? request.directory() : defaultDirectory(environment);
    }

}
