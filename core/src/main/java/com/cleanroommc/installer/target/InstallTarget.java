package com.cleanroommc.installer.target;

import com.cleanroommc.installer.platform.Environment;
import com.cleanroommc.installer.util.ProgressListener;

import java.nio.file.Path;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * Target for the installer to install into.
 *
 * <p>Implementations are discovered with {@link ServiceLoader}.
 */
public interface InstallTarget {

    String id();

    String displayName();

    String description();

    Path defaultDirectory(Environment environment);

    Set<Capability> capabilities();

    void validate(InstallRequest request, InstallContext context) throws InstallException;

    /**
     * Resolve the request into a concrete list of actions. May use the network.
     *
     * <p>{@code --dry-run} version of {@link #apply}.
     */
    InstallPlan plan(InstallRequest request, InstallContext context) throws InstallException;

    /**
     * Execute a plan. Must be idempotent and must not leave partial files behind on failure.
     */
    InstallResult apply(InstallPlan plan, InstallContext context, ProgressListener listener) throws InstallException;

    default InstallResult uninstall(InstallRequest request, InstallContext context) throws InstallException {
        throw new InstallException(ExitCode.USAGE, id() + " does not support uninstall");
    }

}
