package com.cleanroommc.installer.source;

import com.cleanroommc.installer.net.Downloader;
import com.cleanroommc.installer.target.ExitCode;
import com.cleanroommc.installer.target.InstallException;
import com.cleanroommc.installer.util.Log;
import com.cleanroommc.installer.util.ProgressListener;
import com.cleanroommc.installer.version.RemoteVersion;
import com.cleanroommc.installer.version.VersionIndex;
import com.cleanroommc.javautils.JavaUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Sources {

    public static Path ownJar() {
        try {
            Path path = JavaUtils.jarLocationOf(Sources.class).toPath();
            return Files.isRegularFile(path) ? path : null;
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    /**
     * The Cleanroom version this jar carries, or null when it is the generic installer.
     * <p>
     * A pinned jar installs that one version and nothing else, so callers offering a choice — the
     * window's version picker — have nothing to offer and should say which version it is instead.
     */
    public static String pinnedVersion() {
        Path own = ownJar();
        if (own == null) {
            return null;
        }
        JarProfileSource embedded = null;
        try {
            embedded = JarProfileSource.of(own);
            return embedded.pinned() ? embedded.profile().cleanroomVersion() : null;
        } catch (InstallException | RuntimeException e) {
            return null;
        } finally {
            closeQuietly(embedded);
        }
    }

    /**
     * @param requestedVersion the version the user asked for, or null for "whatever this jar carries,
     *                         else the latest release"
     */
    public static ProfileSource open(String requestedVersion, Downloader downloader, Log log, Path cache,
                                     ProgressListener listener) throws InstallException {
        Path own = ownJar();
        if (own != null) {
            JarProfileSource embedded = JarProfileSource.of(own);
            if (embedded.pinned()) {
                String embeddedVersion = embedded.profile().cleanroomVersion();
                if (requestedVersion == null || requestedVersion.equals(embeddedVersion)) {
                    log.info("Installing Cleanroom {} from this installer", embeddedVersion);
                    return embedded;
                }
                log.info("This installer carries Cleanroom {}, but {} was asked for", embeddedVersion, requestedVersion);
            }
            embedded.close();
        }
        return remote(requestedVersion, downloader, log, cache, listener);
    }

    private static ProfileSource remote(String requestedVersion, Downloader downloader, Log log, Path cache,
                                        ProgressListener listener) throws InstallException {
        VersionIndex index = new VersionIndex(downloader, log, cache);
        RemoteVersion version = requestedVersion == null ? index.latest() : index.byId(requestedVersion);
        Path jar = cache.resolve("installers").resolve("cleanroom-" + version.id() + "-installer.jar");
        listener.stage("Fetching Cleanroom " + version.id());
        downloader.download(version.installerUrl(), jar, null, 0L, listener);
        try {
            if (!Files.isRegularFile(jar)) {
                throw new InstallException(ExitCode.NETWORK, "Failed to obtain " + version.installerUrl());
            }
        } catch (RuntimeException e) {
            throw new InstallException(ExitCode.NETWORK, "Failed to obtain " + version.installerUrl(), e);
        }
        return JarProfileSource.of(jar);
    }

    public static ProfileSource ofJar(Path jar) throws InstallException {
        return JarProfileSource.of(jar);
    }

    static void closeQuietly(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception ignored) { }
    }

    private Sources() { }

}
