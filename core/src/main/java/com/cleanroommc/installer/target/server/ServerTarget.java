package com.cleanroommc.installer.target.server;

import com.cleanroommc.installer.InstallerMeta;
import com.cleanroommc.installer.java.JavaResolution;
import com.cleanroommc.installer.maven.Coordinate;
import com.cleanroommc.installer.maven.MavenLayout;
import com.cleanroommc.installer.platform.Environment;
import com.cleanroommc.installer.platform.InstallLocations;
import com.cleanroommc.installer.profile.Download;
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
import com.cleanroommc.installer.target.action.WriteFileAction;
import com.cleanroommc.installer.util.Json;
import com.cleanroommc.platformutils.Platform;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Installs a dedicated server: libraries, argument files, and launch scripts.
 */
public final class ServerTarget extends AbstractInstallTarget {

    public static final String ID = "server";
    public static final String OPTION_NO_SCRIPTS = "noScripts";
    public static final String OPTION_NO_SERVER_JAR = "noServerJar";
    public static final String OPTION_PIN_JAVA = "pinJava";
    public static final String OPTION_MEMORY = "memory";

    private static final String DEFAULT_MEMORY = "3G";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "Server";
    }

    @Override
    public String description() {
        return "Sets up a dedicated Cleanroom server in a directory.";
    }

    @Override
    public Path defaultDirectory(Environment environment) {
        return InstallLocations.serverDefault(environment);
    }

    @Override
    public Set<Capability> capabilities() {
        return capabilitySet(Capability.DRY_RUN, Capability.NEEDS_NETWORK);
    }

    @Override
    public void validate(InstallRequest request, InstallContext context) throws InstallException {
        Path directory = directory(request, context.env());
        if (Files.isDirectory(directory) && !Files.isWritable(directory)) {
            throw new InstallException(ExitCode.TARGET, "No write permission for " + directory);
        }
        if (InstallLocations.looksLikeMinecraft(directory) && !request.assumeYes()) {
            throw new InstallException(ExitCode.TARGET,
                    directory + " looks like a Minecraft client installation." + System.lineSeparator()
                            + "Pass --yes to install a server there anyway, or --dir to point elsewhere.");
        }
        if (busyDirectory(directory)) {
            context.log().warn("{} is not empty and has no server.properties or eula.txt in it; installing there anyway.", directory);
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

        List<Action> libraryActions = MavenLayout.actions(version.libraries(), libraries, context.source(), Platform.current(), true);
        plan.addAll(libraryActions);

        Coordinate universal = Coordinate.parse(profile.path);
        Path universalJar = libraries.resolve(universal.path().replace('/', File.separatorChar));
        plan.add(universalAction(context, universal, universalJar));

        List<Path> classpath = new ArrayList<>();
        for (Action action : libraryActions) {
            classpath.add(action.destination());
        }
        classpath.add(0, universalJar);

        if (!request.flag(OPTION_NO_SERVER_JAR)) {
            Download server = version.downloads == null ? null : version.downloads.get("server");
            if (server != null && !server.embedded()) {
                Path serverJar = libraries.resolve("net").resolve("minecraft").resolve("server")
                        .resolve(InstallerMeta.MINECRAFT_VERSION)
                        .resolve("server-" + InstallerMeta.MINECRAFT_VERSION + ".jar");
                plan.add(new DownloadAction(server.url, serverJar, server.sha1, server.size));
                classpath.add(serverJar);
            } else {
                plan.note("This version json carries no vanilla server download; skipping the server jar");
            }
        }

        String mainClass = profile.serverMainClass != null ? profile.serverMainClass : version.mainClass;
        List<String> gameArgs = new ArrayList<>();
        for (String tweaker : profile.serverTweakers == null ? profile.tweakers : profile.serverTweakers) {
            gameArgs.add("--tweakClass");
            gameArgs.add(tweaker);
        }

        Path argsDirectory = libraries.resolve(universal.group().replace('.', File.separatorChar))
                .resolve(universal.artifact()).resolve(universal.version());
        plan.add(new WriteFileAction(argsDirectory.resolve("unix_args.txt"),
                ArgFiles.argsFile(profile.jvmArgs(), join(classpath, root, ":"), mainClass, gameArgs)));
        plan.add(new WriteFileAction(argsDirectory.resolve("win_args.txt"),
                ArgFiles.argsFile(profile.jvmArgs(), join(classpath, root, ";"), mainClass, gameArgs)));
        plan.add(new WriteFileAction(root.resolve("user_jvm_args.txt"),
                ArgFiles.userJvmArgs(request.extra(OPTION_MEMORY, DEFAULT_MEMORY))));

        if (!request.flag(OPTION_NO_SCRIPTS)) {
            String javaCommand = "java";
            if (request.flag(OPTION_PIN_JAVA)) {
                JavaResolution java = context.javaResolver().resolve(
                        request.java().withBounds(profile.java.minimum, profile.java.maximum, profile.java.recommended),
                        context.listener()
                );
                javaCommand = java.executable().toString();
                plan.note("Scripts pinned to " + javaCommand);
            }
            // Only the script this machine can actually run: a Linux box has no use for run.bat.
            if (context.env().windows()) {
                String winArgs = root.relativize(argsDirectory.resolve("win_args.txt")).toString().replace('/', '\\');
                plan.add(new WriteFileAction(root.resolve("run.bat"),
                        ArgFiles.runBat(javaCommand, winArgs, profile.java.minimum, profile.java.maximum)));
            } else {
                String unixArgs = root.relativize(argsDirectory.resolve("unix_args.txt")).toString().replace(File.separatorChar, '/');
                plan.add(new WriteFileAction(root.resolve("run.sh"),
                        ArgFiles.runSh(javaCommand, unixArgs, profile.java.minimum, profile.java.maximum), true));
            }
        }

        plan.add(new WriteFileAction(root.resolve("cleanroom-server.json"), Json.GSON.toJson(version) + "\n"));
        return plan;
    }

    private Action universalAction(InstallContext context, Coordinate universal, Path destination) {
        String entry = MavenLayout.EMBEDDED_ROOT + universal.path();
        boolean embedded;
        try {
            embedded = context.source().open(entry) != null;
        } catch (IOException e) {
            embedded = false;
        }
        if (embedded) {
            return new CopyResourceAction(entry, () -> context.source().open(entry), destination);
        }
        return new DownloadAction(InstallerMeta.CLEANROOM_REPO + universal.path(), destination, null, 0L);
    }

    private static String join(List<Path> paths, Path root, String separator) {
        StringBuilder builder = new StringBuilder();
        for (Path path : paths) {
            if (builder.length() > 0) {
                builder.append(separator);
            }
            Path relative = path.startsWith(root) ? root.relativize(path) : path;
            builder.append(relative.toString().replace(File.separatorChar, separator.equals(";") ? '\\' : '/'));
        }
        return builder.toString();
    }

    /**
     * Whether the directory already holds files that are not part of a server installation.
     */
    public static boolean busyDirectory(Path directory) {
        if (!Files.isDirectory(directory)) {
            return false;
        }
        if (Files.isRegularFile(directory.resolve("server.properties"))
                || Files.isRegularFile(directory.resolve("eula.txt"))
                || Files.isDirectory(directory.resolve("libraries"))) {
            return false;
        }
        try (Stream<Path> entries = Files.list(directory)) {
            return entries.findAny().isPresent();
        } catch (IOException e) {
            return false;
        }
    }

    private Path directory(InstallRequest request, Environment environment) {
        return request.directory() != null ? request.directory() : defaultDirectory(environment);
    }

    static List<String> defaultTweakers() {
        return Arrays.asList("net.minecraftforge.fml.common.launcher.FMLServerTweaker");
    }

}
