/*
 * Copyright (c) 2026 CleanroomMC contributors
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package com.cleanroommc.installer.cli;

import com.cleanroommc.installer.InstallerMeta;
import com.cleanroommc.installer.java.JavaResolver;
import com.cleanroommc.installer.net.Downloader;
import com.cleanroommc.installer.platform.Environment;
import com.cleanroommc.installer.source.ProfileSource;
import com.cleanroommc.installer.source.Sources;
import com.cleanroommc.installer.target.ExitCode;
import com.cleanroommc.installer.target.InstallContext;
import com.cleanroommc.installer.target.InstallException;
import com.cleanroommc.installer.target.InstallPlan;
import com.cleanroommc.installer.target.InstallRequest;
import com.cleanroommc.installer.target.InstallResult;
import com.cleanroommc.installer.target.InstallTarget;
import com.cleanroommc.installer.target.InstallTargets;
import com.cleanroommc.installer.target.client.ClientTarget;
import com.cleanroommc.installer.util.Log;
import com.cleanroommc.installer.util.slf4j.LogBridge;
import com.cleanroommc.installer.version.RemoteVersion;
import com.cleanroommc.installer.version.VersionIndex;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Map;

/**
 * The text interface.
 * The graphical one builds the same {@link InstallRequest} and runs the same target.
 */
public final class Cli {

    private final Environment environment;
    private final PrintStream out;
    private final PrintStream err;

    public Cli(Environment environment, PrintStream out, PrintStream err) {
        this.environment = environment;
        this.out = out;
        this.err = err;
    }

    static String implementationVersion() {
        String version = Cli.class.getPackage().getImplementationVersion();
        return version == null ? "(dev)" : version;
    }

    /** Where a run's log goes when the user did not choose. */
    public static Path defaultLogFile(Environment environment) {
        return environment.installerCache().resolve("logs").resolve("installer-" + System.currentTimeMillis() + ".log");
    }

    public int run(String[] args) {
        CliOptions options;
        try {
            options = CliOptions.parse(args, this.environment);
        } catch (InstallException e) {
            this.err.println(e.getMessage());
            this.err.println("Run with --help for usage.");
            return e.exitCode().code();
        }

        if (options.help || "help".equals(options.mode)) {
            this.out.print(Help.text());
            return ExitCode.SUCCESS.code();
        }
        if (options.showVersion || "version".equals(options.mode)) {
            this.out.println(InstallerMeta.NAME + " " + implementationVersion());
            return ExitCode.SUCCESS.code();
        }
        if (options.mode == null) {
            this.out.print(Help.text());
            return ExitCode.USAGE.code();
        }

        Log log = options.logFile != null ? Log.toFile(options.logFile) : Log.console();
        LogBridge.attach(log);
        if (options.verbose) {
            log.consoleLevel(Log.Level.DEBUG);
        } else if (options.quiet) {
            log.consoleLevel(Log.Level.WARN);
        }
        CliProgress progress = new CliProgress(this.out, options.json, options.quiet);
        Downloader downloader = new Downloader(log, options.offline);

        try {
            if ("list-versions".equals(options.mode)) {
                return listVersions(downloader, log);
            }
            return install(options, downloader, log, progress);
        } catch (InstallException e) {
            progress.done();
            report(e, options.json);
            log.error(e, "Install failed");
            return e.exitCode().code();
        } catch (RuntimeException e) {
            progress.done();
            log.error(e, "Unexpected failure");
            this.err.println("Unexpected failure: " + e);
            if (log.file() != null) {
                this.err.println("Full log: " + log.file());
            }
            return ExitCode.INTERNAL.code();
        } finally {
            LogBridge.detach();
            log.close();
        }
    }

    private int listVersions(Downloader downloader, Log log) throws InstallException {
        VersionIndex index = new VersionIndex(downloader, log, this.environment.installerCache());
        for (RemoteVersion version : index.all()) {
            this.out.println(version.id());
        }
        return ExitCode.SUCCESS.code();
    }

    private int install(CliOptions options, Downloader downloader, Log log, CliProgress progress)
        throws InstallException {
        if ("uninstall".equals(options.mode)) {
            return uninstall(options, downloader, log, progress);
        }
        InstallTarget target = InstallTargets.byId(options.mode);
        try (ProfileSource source = Sources.open(
            options.version,
            downloader,
            log,
            this.environment.installerCache(),
            progress
        )) {
            InstallContext context = new InstallContext(
                source,
                downloader,
                new JavaResolver(this.environment, log),
                this.environment,
                log
            ).listener(progress);
            InstallRequest request = options.toRequest();

            target.validate(request, context);
            InstallPlan plan = target.plan(request, context);
            if (options.dryRun) {
                progress.done();
                this.out.print(plan.render());
                return ExitCode.SUCCESS.code();
            }

            InstallResult result = target.apply(plan, context, progress);
            progress.done();
            printResult(result, options.json, false);
            if (log.file() != null && !options.json) {
                this.out.println("Log: " + log.file());
            }
            return ExitCode.SUCCESS.code();
        }
    }

    private int uninstall(CliOptions options, Downloader downloader, Log log, CliProgress progress)
        throws InstallException {
        InstallRequest request = options.toRequest();
        if (request.version() == null) {
            request = request.toBuilder().version(Sources.pinnedVersion()).build();
        }
        // No profile source: what to remove is read from the game directory, never from a download
        InstallContext context = new InstallContext(
            null,
            downloader,
            new JavaResolver(this.environment, log),
            this.environment,
            log
        ).listener(progress);
        InstallResult result = InstallTargets.byId(ClientTarget.ID).uninstall(request, context);
        progress.done();
        printResult(result, options.json, true);
        return ExitCode.SUCCESS.code();
    }

    private void printResult(InstallResult result, boolean json, boolean removing) {
        if (json) {
            StringBuilder builder = new StringBuilder("{\"event\":\"result\",\"ok\":true,\"root\":")
                .append(CliProgress.quote(result.root().toString()))
                .append(",\"files\":[");
            for (int i = 0; i < result.written().size(); i++) {
                if (i > 0) {
                    builder.append(',');
                }
                builder.append(CliProgress.quote(result.written().get(i).toString()));
            }
            this.out.println(builder.append("]}").toString());
            return;
        }
        if (removing) {
            this.out.println(
                result.isNoOp()
                    ? "No Cleanroom installation was found in " + result.root() + "."
                    : "Removed Cleanroom from " + result.root() + "."
            );
        } else if (result.isNoOp()) {
            this.out.println("Already installed; nothing to do.");
        } else {
            this.out.println("Installed into " + result.root() + " (" + result.written().size() + " files).");
        }
        for (Map.Entry<String, String> detail : result.details().entrySet()) {
            this.out.println("  " + detail.getKey() + ": " + detail.getValue());
        }
        for (String note : result.notes()) {
            this.out.println("  " + note);
        }
    }

    private void report(InstallException e, boolean json) {
        if (json) {
            this.out.println(
                "{\"event\":\"result\",\"ok\":false,\"code\":" + e.exitCode().code() + ",\"message\":" +
                    CliProgress.quote(e.getMessage()) + "}"
            );
            return;
        }
        this.err.println(e.getMessage());
        Throwable cause = e.getCause();
        if (cause != null && cause.getMessage() != null) {
            this.err.println("  caused by: " + cause.getMessage());
        }
    }

}
