package com.cleanroommc.installer;

import com.cleanroommc.installer.cli.Cli;
import com.cleanroommc.installer.cli.CliOptions;
import com.cleanroommc.installer.platform.Environment;
import com.cleanroommc.installer.target.ExitCode;
import com.cleanroommc.installer.target.InstallRequest;
import com.cleanroommc.installer.ui.InstallerWindow;

import java.awt.GraphicsEnvironment;

/**
 * Entry point. No arguments and a display present opens the graphical installer.
 * Anything else runs on the command line.
 * Headless with no arguments prints help and fails rather than guess.
 */
public final class Main {

    public static void main(String[] args) {
        Environment environment = Environment.current();
        boolean headless = GraphicsEnvironment.isHeadless();
        boolean wantsGui;
        CliOptions options = null;
        try {
            options = CliOptions.parse(args, environment);
            wantsGui = options.wantsGui(headless);
        } catch (Exception parseFailure) {
            wantsGui = false;
        }

        if (wantsGui) {
            int code = launchGui(environment, options);
            if (code != Integer.MIN_VALUE) {
                System.exit(code);
            }
        }

        System.exit(new Cli(environment, System.out, System.err).run(args));
    }

    private static int launchGui(Environment environment, CliOptions options) {
        try {
            InstallRequest seed = options != null && options.isInstallMode() ? options.toRequest() : null;
            return InstallerWindow.launch(environment, seed);
        } catch (Exception e) {
            System.err.println("The graphical installer failed to start: " + e);
            System.err.println("Falling back to the command line. Run with --help for usage.");
            return Integer.MIN_VALUE;
        }
    }

    private Main() { }

}
