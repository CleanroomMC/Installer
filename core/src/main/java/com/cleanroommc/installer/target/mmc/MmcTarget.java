package com.cleanroommc.installer.target.mmc;

import com.cleanroommc.installer.InstallerMeta;
import com.cleanroommc.installer.java.JavaResolution;
import com.cleanroommc.installer.platform.DetectedLauncher;
import com.cleanroommc.installer.platform.Environment;
import com.cleanroommc.installer.platform.InstallLocations;
import com.cleanroommc.installer.profile.InstallProfile;
import com.cleanroommc.installer.target.*;
import com.cleanroommc.installer.target.action.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Installs Cleanroom into a MultiMC/Prism/PolyMC launcher, from the pack zip CleanroomGradle's
 * {@code publishMmcPackZip} produces.
 * <p>
 * Which of the two things it does is decided by the directory it is pointed at:
 * <ul>
 *     <li>Root launcher or instances directory: creates a new instance</li>
 *     <li>Existing instance: Cleanroom files are repaired/version changed or upgraded from a Forge one</li>
 * </ul>
 *
 * @see MmcInstance
 */
public final class MmcTarget extends AbstractInstallTarget {

    public static final String ID = "mmc";
    public static final String OPTION_INSTANCE_NAME = "instanceName";
    public static final String OPTION_LAUNCHER = "launcher";
    public static final String OPTION_REPLACE_JAVA_PATH = "replaceJavaPath";
    public static final String EMBEDDED_PACK = "mmc/pack.zip";

    private static final Set<String> INSTANCE_OWNED = Collections.singleton("instance.cfg");

    public static boolean updatesExistingInstance(Path directory) {
        return InstallLocations.looksLikeMmcInstance(directory);
    }

    private static String packFileName(InstallProfile profile) {
        return InstallerMeta.CLEANROOM_ARTIFACT + "-" + profile.cleanroomVersion() + "-mmc.zip";
    }

    private static String packUrl(InstallContext context, InstallProfile profile) {
        String directory = InstallerMeta.CLEANROOM_REPO
                + InstallerMeta.CLEANROOM_GROUP.replace('.', '/') + "/"
                + InstallerMeta.CLEANROOM_ARTIFACT + "/" + profile.cleanroomVersion() + "/";
        String current = directory + packFileName(profile);
        if (context.downloader().exists(current)) {
            return current;
        }
        String legacy = directory + InstallerMeta.CLEANROOM_ARTIFACT + "-" + profile.cleanroomVersion() + ".zip";
        return context.downloader().exists(legacy) ? legacy : current;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "Profile for MultiMC/PolyMC/Prism";
    }

    @Override
    public String description() {
        return "Creates a Prism, PolyMC or MultiMC instance, or updates an existing one.";
    }

    @Override
    public Path defaultDirectory(Environment environment) {
        List<DetectedLauncher> launchers = InstallLocations.multiMcFamily(environment);
        return launchers.isEmpty() ? environment.workingDirectory() : launchers.get(0).instances();
    }

    @Override
    public Set<Capability> capabilities() {
        return capabilitySet(Capability.DRY_RUN, Capability.NEEDS_NETWORK);
    }

    @Override
    public void validate(InstallRequest request, InstallContext context) throws InstallException {
        Path directory = directory(request, context.env());
        if (directory == null) {
            throw new InstallException(ExitCode.TARGET,
                    "No Prism, PolyMC or MultiMC installation was found." + System.lineSeparator()
                            + "Pass --dir <path> to name an instances directory, a launcher folder, "
                            + "or an instance to update.");
        }
        if (updatesExistingInstance(directory)) {
            if (!Files.isWritable(directory)) {
                throw new InstallException(ExitCode.TARGET, "No write permission for " + directory);
            }
            MmcInstance existing = MmcInstance.inspect(directory);
            if (existing.kind() == MmcInstance.Kind.FORGE && !request.assumeYes()) {
                throw new InstallException(ExitCode.TARGET,
                        directory + " is a " + existing.describe() + "." + System.lineSeparator()
                                + "Installing Cleanroom there replaces Forge, and mods built for Forge may "
                                + "not load afterwards." + System.lineSeparator()
                                + "Pass --yes to convert it anyway, or --dir to point elsewhere.");
            }
            return;
        }
        Path instances = instances(request, context.env());
        Path instance = instances.resolve(instanceName(request, context));
        if (Files.exists(instance) && !request.assumeYes()) {
            throw new InstallException(ExitCode.TARGET,
                    "Instance " + instance + " already exists. Pass --yes to overwrite it, "
                            + "or --instance-name to use a different name.");
        }
    }

    @Override
    public InstallPlan plan(InstallRequest request, InstallContext context) throws InstallException {
        InstallProfile profile = context.profile();
        String versionId = context.versionJson().id != null
                ? context.versionJson().id
                : InstallerMeta.versionId(profile.cleanroomVersion());
        Path directory = directory(request, context.env());
        if (directory == null) {
            throw new InstallException(ExitCode.TARGET, "No Prism, PolyMC or MultiMC installation was found");
        }
        boolean updating = updatesExistingInstance(directory);
        Path instance = updating ? directory : instances(request, context.env()).resolve(instanceName(request, context));

        InstallPlan plan = new InstallPlan(ID, versionId, instance);
        Path pack = context.cache().resolve("packs").resolve(packFileName(profile));
        plan.add(packAction(context, profile, pack));
        plan.add(updating ? new ExtractZipAction(pack, instance, INSTANCE_OWNED) : new ExtractZipAction(pack, instance));
        plan.add(new WriteFileAction(instance.resolve(".cleanroom-installer"),
                "Created by the Cleanroom Installer for " + versionId + System.lineSeparator()));

        Map<String, String> values = new LinkedHashMap<>();
        String requestedName = request.extra(OPTION_INSTANCE_NAME);
        if (!updating && requestedName != null && !requestedName.isEmpty()) {
            values.put("name", requestedName);
        }
        if (!updating || request.flag(OPTION_REPLACE_JAVA_PATH)) {
            JavaResolution java = context.javaResolver().resolve(
                    request.java().withBounds(profile.java.minimum, profile.java.maximum, profile.java.recommended),
                    context.listener());
            values.put("OverrideJava", "true");
            values.put("OverrideJavaLocation", "true");
            values.put("JavaPath", java.executable().toString());
            plan.note("Instance java path set to " + java.executable());
        }
        if (!values.isEmpty()) {
            plan.add(new InstanceConfigAction(instance.resolve("instance.cfg"), values));
        }

        if (updating) {
            for (String note : updateNotes(MmcInstance.inspect(directory), profile.cleanroomVersion())) {
                plan.note(note);
            }
        } else {
            plan.note("Restart your launcher if the instance does not appear right away");
        }
        return plan;
    }

    private static List<String> updateNotes(MmcInstance existing, String version) {
        List<String> notes = new ArrayList<>();
        switch (existing.kind()) {
            case CLEANROOM:
                notes.add(existing.isCleanroom(version)
                        ? "Repaired the existing Cleanroom " + version + " instance"
                        : "Changed this instance from Cleanroom " + existing.loaderVersion() + " to " + version);
                break;
            case FORGE:
                notes.add("Replaced Forge " + existing.loaderVersion() + " with Cleanroom " + version);
                notes.add("Mods built for Forge may need updating before they load");
                break;
            default:
                notes.add("Installed Cleanroom " + version + " to the existing instance");
                break;
        }
        notes.add("Kept the instance's own configuration");
        return notes;
    }

    private Action packAction(InstallContext context, InstallProfile profile, Path pack) {
        try (InputStream embedded = context.source().open(EMBEDDED_PACK)) {
            if (embedded != null) {
                return new CopyResourceAction(EMBEDDED_PACK, () -> context.source().open(EMBEDDED_PACK), pack);
            }
        } catch (IOException ignored) { }
        return new DownloadAction(packUrl(context, profile), pack, null, 0L);
    }

    private Path directory(InstallRequest request, Environment environment) {
        if (request.directory() != null) {
            return request.directory();
        }
        String wanted = request.extra(OPTION_LAUNCHER);
        for (DetectedLauncher launcher : InstallLocations.multiMcFamily(environment)) {
            if (wanted == null || launcher.kind().name().equalsIgnoreCase(wanted.replace('-', '_'))) {
                return launcher.instances();
            }
        }
        return null;
    }

    private Path instances(InstallRequest request, Environment environment) {
        Path directory = directory(request, environment);
        Path fromRoot = InstallLocations.instancesOfLauncherRoot(directory);
        return fromRoot != null ? fromRoot : directory;
    }

    private String instanceName(InstallRequest request, InstallContext context) throws InstallException {
        String name = request.extra(OPTION_INSTANCE_NAME);
        if (name != null && !name.isEmpty()) {
            return name;
        }
        InstallProfile profile = context.profile();
        return profile.profileName() + " " + profile.cleanroomVersion();
    }

}
