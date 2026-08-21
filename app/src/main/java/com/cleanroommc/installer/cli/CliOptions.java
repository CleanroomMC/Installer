package com.cleanroommc.installer.cli;

import com.cleanroommc.installer.java.JavaSpec;
import com.cleanroommc.installer.platform.Environment;
import com.cleanroommc.installer.target.ExitCode;
import com.cleanroommc.installer.target.InstallException;
import com.cleanroommc.installer.target.InstallRequest;
import com.cleanroommc.installer.target.client.ClientTarget;
import com.cleanroommc.installer.target.mmc.MmcTarget;
import com.cleanroommc.installer.target.server.ServerTarget;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @see Help Help for documentation on options
 */
public final class CliOptions {

    public String mode;
    public boolean help;
    public boolean showVersion;
    public boolean gui;
    public boolean noGui;
    public boolean json;
    public boolean quiet;
    public boolean verbose;
    public boolean dryRun;
    public boolean offline;
    public boolean force;
    public boolean assumeYes;
    // TODO
    public boolean uninstall;
    public String version;
    public Path directory;
    public Path logFile;
    public String javaPath;
    public int javaTarget = -1;
    public String javaVendor;
    public Boolean provisionJava;
    public List<String> jvmArgs = new ArrayList<>();
    public Map<String, String> extras = new LinkedHashMap<>();

    private static final List<String> BOOLEAN_MODES = Arrays.asList("help", "version", "list-versions", "uninstall");

    public static CliOptions parse(String[] args, Environment environment) throws InstallException {
        CliOptions options = new CliOptions();
        int index = 0;
        while (index < args.length) {
            String arg = args[index++];
            if (!arg.startsWith("-")) {
                if (options.mode == null) {
                    options.mode = arg;
                } else {
                    throw usage("Unexpected argument: " + arg);
                }
                continue;
            }
            switch (arg) {
                case "-h":
                case "--help":
                    options.help = true;
                    break;
                case "--version-info":
                    options.showVersion = true;
                    break;
                case "--gui":
                    options.gui = true;
                    break;
                case "--no-gui":
                    options.noGui = true;
                    break;
                case "--json":
                    options.json = true;
                    options.quiet = true;
                    break;
                case "--quiet":
                    options.quiet = true;
                    break;
                case "--verbose":
                    options.verbose = true;
                    break;
                case "--dry-run":
                    options.dryRun = true;
                    break;
                case "--offline":
                    options.offline = true;
                    break;
                case "--force":
                    options.force = true;
                    break;
                case "-y":
                case "--yes":
                    options.assumeYes = true;
                    break;
                case "-v":
                case "--version":
                    options.version = value(args, index++, arg);
                    break;
                case "-d":
                case "--dir":
                    options.directory = environment.path(value(args, index++, arg)).toAbsolutePath();
                    break;
                case "--log-file":
                    options.logFile = environment.path(value(args, index++, arg)).toAbsolutePath();
                    break;
                case "--java":
                    options.javaPath = value(args, index++, arg);
                    break;
                case "--java-version":
                    options.javaTarget = integer(value(args, index++, arg), arg);
                    break;
                case "--java-vendor":
                    options.javaVendor = value(args, index++, arg);
                    break;
                case "--provision-java":
                    options.provisionJava = Boolean.TRUE;
                    break;
                case "--no-provision-java":
                    options.provisionJava = Boolean.FALSE;
                    break;
                case "--jvm-args":
                    options.jvmArgs = splitArgs(value(args, index++, arg));
                    break;
                // client
                case "--profile-name":
                    options.extras.put(ClientTarget.OPTION_PROFILE_NAME, value(args, index++, arg));
                    break;
                case "--no-profile":
                    options.extras.put(ClientTarget.OPTION_NO_PROFILE, "true");
                    break;
                case "--full":
                    options.extras.put(ClientTarget.OPTION_FULL, "true");
                    break;
                case "--launcher-profiles":
                    options.extras.put(ClientTarget.OPTION_LAUNCHER_PROFILES, value(args, index++, arg));
                    break;
                // server
                case "--no-scripts":
                    options.extras.put(ServerTarget.OPTION_NO_SCRIPTS, "true");
                    break;
                case "--no-server-jar":
                    options.extras.put(ServerTarget.OPTION_NO_SERVER_JAR, "true");
                    break;
                case "--pin-java":
                    options.extras.put(ServerTarget.OPTION_PIN_JAVA, "true");
                    break;
                case "--memory":
                    options.extras.put(ServerTarget.OPTION_MEMORY, value(args, index++, arg));
                    break;
                // mmc
                case "--replace-java-path":
                    options.extras.put(MmcTarget.OPTION_REPLACE_JAVA_PATH, "true");
                    break;
                case "--instance-name":
                    options.extras.put(MmcTarget.OPTION_INSTANCE_NAME, value(args, index++, arg));
                    break;
                case "--launcher":
                    options.extras.put(MmcTarget.OPTION_LAUNCHER, value(args, index++, arg));
                    break;
                default:
                    throw usage("Unknown option: " + arg);
            }
        }
        if (options.gui && options.noGui) {
            throw usage("--gui and --no-gui contradict each other");
        }
        return options;
    }

    public boolean wantsGui(boolean headless) {
        if (this.noGui || this.help || this.showVersion) {
            return false;
        }
        if (this.gui) {
            return true;
        }
        return this.mode == null && !headless;
    }

    public boolean isInstallMode() {
        return this.mode != null && !BOOLEAN_MODES.contains(this.mode);
    }

    public InstallRequest toRequest() {
        JavaSpec java = JavaSpec.defaults()
                .withPath(this.javaPath)
                .withProvision(Boolean.TRUE.equals(this.provisionJava));
        if (this.javaTarget > 0) {
            java = java.withBounds(Math.min(java.minimum(), this.javaTarget),
                    Math.max(java.maximum(), this.javaTarget), this.javaTarget);
        }
        if (this.javaVendor != null) {
            java = java.withDistro(this.javaVendor);
        }
        InstallRequest.Builder builder = InstallRequest.builder(this.mode)
                .version(this.version)
                .directory(this.directory)
                .offline(this.offline)
                .dryRun(this.dryRun)
                .force(this.force)
                .assumeYes(this.assumeYes)
                .java(java)
                .jvmArgs(this.jvmArgs);
        for (Map.Entry<String, String> extra : this.extras.entrySet()) {
            builder.extra(extra.getKey(), extra.getValue());
        }
        return builder.build();
    }

    static List<String> splitArgs(String value) {
        List<String> args = new ArrayList<>();
        for (String part : value.trim().split("\\s+")) {
            if (!part.isEmpty()) {
                args.add(part);
            }
        }
        return args;
    }

    private static String value(String[] args, int index, String flag) throws InstallException {
        if (index >= args.length) {
            throw usage(flag + " needs a value");
        }
        return args[index];
    }

    private static int integer(String value, String flag) throws InstallException {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw usage(flag + " needs a number, got " + value);
        }
    }

    private static InstallException usage(String message) {
        return new InstallException(ExitCode.USAGE, message);
    }

}
