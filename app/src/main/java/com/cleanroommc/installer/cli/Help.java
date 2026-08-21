package com.cleanroommc.installer.cli;

import com.cleanroommc.installer.target.InstallTarget;
import com.cleanroommc.installer.target.InstallTargets;

/**
 * The help text. Modes are listed from the target registry, so a new target documents itself.
 */
public final class Help {

    public static String text() {
        StringBuilder builder = new StringBuilder();
        builder.append("Cleanroom Installer").append(nl())
                .append(nl())
                .append("Usage:").append(nl())
                .append("  java -jar cleanroom-installer.jar                  open the graphical installer").append(nl())
                .append("  java -jar cleanroom-installer.jar <mode> [options]").append(nl())
                .append(nl())
                .append("Modes:").append(nl());
        for (InstallTarget target : InstallTargets.all()) {
            builder.append("  ").append(pad(target.id())).append(target.description()).append(nl());
        }
        builder.append("  ").append(pad("list-versions")).append("print the installable Cleanroom versions").append(nl())
                .append("  ").append(pad("uninstall")).append("undo a previous install (client only for now)").append(nl())
                .append(nl())
                .append("Options:").append(nl())
                .append("  -v, --version <id>       Cleanroom version to install (default: newest)").append(nl())
                .append("  -d, --dir <path>         where to install (default: detected per mode)").append(nl())
                .append("                           mmc: an instances directory, a launcher folder, or").append(nl())
                .append("                           an existing instance to update").append(nl())
                .append("      --gui | --no-gui     force the graphical or the text interface").append(nl())
                .append("      --offline            use only what is already downloaded").append(nl())
                .append("      --dry-run            print what would happen, write nothing").append(nl())
                .append("      --force              reinstall even if everything is already in place").append(nl())
                .append("  -y, --yes                answer yes to every prompt").append(nl())
                .append("      --json               machine-readable NDJSON output").append(nl())
                .append("      --quiet | --verbose  less or more logging").append(nl())
                .append("      --log-file <path>    where to write the log").append(nl())
                .append("  -h, --help               this text").append(nl())
                .append(nl())
                .append("Java:").append(nl())
                .append("      --java <path>        use this Java installation").append(nl())
                .append("      --java-version <n>   Java feature version to target (default 25)").append(nl())
                .append("      --java-vendor <name> preferred vendor, e.g. zulu, temurin").append(nl())
                .append("      --provision-java     download a Java runtime when none is found").append(nl())
                .append("      --no-provision-java  never download one").append(nl())
                .append(nl())
                .append("client:").append(nl())
                .append("      --profile-name <s>   launcher profile name").append(nl())
                .append("      --no-profile         do not touch launcher_profiles.json").append(nl())
                .append("      --full               download every library now, for an offline-ready install").append(nl())
                .append("      --jvm-args \"<args>\"  JVM arguments for the launcher profile").append(nl())
                .append(nl())
                .append("server:").append(nl())
                .append("      --memory <size>      seeds user_jvm_args.txt, e.g. 6G").append(nl())
                .append("      --no-scripts         skip the launch script (run.bat on Windows, run.sh elsewhere)").append(nl())
                .append("      --no-server-jar      skip the vanilla server jar").append(nl())
                .append("      --pin-java           write the resolved Java path into the scripts").append(nl())
                .append(nl())
                .append("mmc:").append(nl())
                .append("      --instance-name <s>  name for the new instance").append(nl())
                .append("      --replace-java-path  point the instance at the Java the installer found").append(nl())
                .append("      --launcher <name>    prism, polymc or multimc").append(nl())
                .append(nl())
                .append("Exit codes: 0 ok, 1 internal, 2 usage, 3 network, 4 verification, 5 target, 6 java, 7 cancelled")
                .append(nl());
        return builder.toString();
    }

    private static String pad(String value) {
        StringBuilder builder = new StringBuilder(value);
        while (builder.length() < 18) {
            builder.append(' ');
        }
        return builder.toString();
    }

    private static String nl() {
        return System.lineSeparator();
    }

    private Help() { }

}
