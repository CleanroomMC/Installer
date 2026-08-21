package com.cleanroommc.installer.java;

import com.cleanroommc.installer.platform.Environment;
import com.cleanroommc.installer.target.ExitCode;
import com.cleanroommc.installer.target.InstallException;
import com.cleanroommc.installer.util.Log;
import com.cleanroommc.installer.util.ProgressListener;
import com.cleanroommc.javautils.JavaUtils;
import com.cleanroommc.javautils.api.JavaDistro;
import com.cleanroommc.javautils.api.JavaInstall;
import com.cleanroommc.javautils.provisioners.FoojayJavaProvisioner;
import com.cleanroommc.javautils.spi.JavaLocator;
import com.cleanroommc.javautils.spi.JavaProvisioner;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Finds, or downloads, a Java new enough to run Cleanroom.
 * <p>
 * The searching, parsing and provisioning are JavaUtils'; this only states which install the
 * installer prefers and turns JavaUtils' callbacks into installer progress. Resolve-then-report
 * rather than the relauncher's interleaved GUI calls, so this is testable and so the GUI and CLI
 * share it.
 */
public final class JavaResolver {

    public static final String CLEANROOM_HOME_PROPERTY = Environment.CLEANROOM_HOME_PROPERTY;

    private final Environment environment;
    private final Log log;

    public JavaResolver(Environment environment, Log log) {
        this.environment = environment;
        this.log = log;
        publishHome();
    }

    /**
     * Tells JavaUtils which Cleanroom home this run provisions into, so its locator finds those
     * installs on the next run. An explicitly given property is left alone.
     */
    private void publishHome() {
        if (System.getProperty(CLEANROOM_HOME_PROPERTY) == null) {
            System.setProperty(CLEANROOM_HOME_PROPERTY, this.environment.cleanroomHome().toString());
        }
    }

    public JavaResolution resolve(JavaSpec spec, ProgressListener listener) throws InstallException {
        if (spec.path() != null && !spec.path().isEmpty()) {
            return explicit(spec);
        }
        JavaResolution located = locate(spec, listener);
        if (located != null) {
            return located;
        }
        if (!spec.allowProvision()) {
            throw new InstallException(ExitCode.JAVA,
                    "No " + spec.requirement() + " was found." + System.lineSeparator()
                            + "Cleanroom cannot run on the Java the Minecraft launcher ships." + System.lineSeparator()
                            + "Pass --java <path> to point at one, or --provision-java to download "
                            + "Java " + spec.target() + " into " + this.environment.javaCache() + ".");
        }
        return provision(spec, listener);
    }

    /**
     * Whether this machine already has a Java the spec accepts. The GUI asks so it only offers to
     * provision one when provisioning would actually happen; the scan touches the disk, so call
     * this off the event thread.
     */
    public boolean hasLocalJava(JavaSpec spec, ProgressListener listener) {
        return locate(spec, listener) != null;
    }

    private JavaResolution explicit(JavaSpec spec) throws InstallException {
        try {
            JavaInstall install = JavaUtils.parseInstall(spec.path());
            int major = install.version().major();
            if (!spec.accepts(major)) {
                throw new InstallException(ExitCode.JAVA,
                        spec.path() + " is Java " + major + ", but Cleanroom needs " + spec.requirement() + ".");
            }
            return new JavaResolution(install, JavaResolution.Origin.EXPLICIT);
        } catch (IOException e) {
            throw new InstallException(ExitCode.JAVA, "Not a usable Java installation: " + spec.path(), e);
        }
    }

    private JavaResolution locate(JavaSpec spec, ProgressListener listener) {
        if (listener != null) {
            listener.stage("Looking for " + spec.requirement());
        }
        List<JavaInstall> candidates = new ArrayList<>();
        for (JavaLocator locator : JavaLocator.locators()) {
            try {
                if (listener != null) {
                    locator.onScan(directory -> listener.detail(directory.toString()));
                }
                candidates.addAll(locator.get(install -> spec.accepts(install.version().major())));
            } catch (RuntimeException e) {
                // A single broken locator (an unreadable registry key, a dangling symlink) must not
                // sink the whole scan.
                this.log.debug("Locator {} failed: {}", locator.getClass().getSimpleName(), e.toString());
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        candidates.sort(preference(spec));
        JavaInstall best = candidates.get(0);
        this.log.info("Using {}", best);
        return new JavaResolution(best, JavaResolution.Origin.LOCATED);
    }

    /**
     * Exactly the target version first, then JDKs, then the requested vendor, then whichever
     * JavaUtils itself ranks highest (newest version, then distro).
     */
    private static Comparator<JavaInstall> preference(JavaSpec spec) {
        JavaDistro wanted = spec.distro() == null ? JavaDistro.UNKNOWN : spec.distro();
        return Comparator
                .comparing((JavaInstall install) -> install.version().major() == spec.target() ? 0 : 1)
                .thenComparing(install -> install.jdk() ? 0 : 1)
                .thenComparing(install -> wanted.equals(install.distro()) ? 0 : 1)
                .thenComparing(Comparator.<JavaInstall>reverseOrder());
    }

    private JavaResolution provision(JavaSpec spec, ProgressListener listener) throws InstallException {
        JavaDistro distro = spec.distro() == null ? JavaDistro.UNKNOWN : spec.distro();
        Path directory = this.environment.javaCache();
        JavaProvisioner provisioner = JavaProvisioner.provisioners().stream()
                .findFirst()
                .orElseGet(FoojayJavaProvisioner::new);
        if (listener != null) {
            listener.stage("Downloading Java " + spec.target() + " (" + distro.name() + ")");
            provisioner.onDownload((done, total, name) -> {
                listener.detail(name);
                listener.progress(done, total);
            });
        }
        try {
            JavaInstall install = provisioner.resolve(spec.target(), distro, directory);
            this.log.info("Provisioned {}", install);
            return new JavaResolution(install, JavaResolution.Origin.PROVISIONED);
        } catch (IOException e) {
            throw new InstallException(ExitCode.JAVA,
                    "Unable to download Java " + spec.target() + " into " + directory, e);
        }
    }

}
