package com.cleanroommc.installer.target;

import com.cleanroommc.installer.java.JavaResolver;
import com.cleanroommc.installer.net.Downloader;
import com.cleanroommc.installer.platform.Environment;
import com.cleanroommc.installer.profile.InstallProfile;
import com.cleanroommc.installer.profile.VersionJson;
import com.cleanroommc.installer.source.ProfileSource;
import com.cleanroommc.installer.util.Log;
import com.cleanroommc.installer.util.ProgressListener;

import java.nio.file.Path;

/**
 * Everything a target needs that is not the user's request.
 */
public final class InstallContext {

    private final ProfileSource source;
    private final Downloader downloader;
    private final JavaResolver javaResolver;
    private final Environment environment;
    private final Log log;

    private ProgressListener listener = ProgressListener.NONE;

    public InstallContext(ProfileSource source, Downloader downloader, JavaResolver javaResolver, Environment environment, Log log) {
        this.source = source;
        this.downloader = downloader;
        this.javaResolver = javaResolver;
        this.environment = environment;
        this.log = log;
    }

    public ProfileSource source() {
        return this.source;
    }

    public InstallProfile profile() throws InstallException {
        return this.source.profile();
    }

    public VersionJson versionJson() throws InstallException {
        return this.source.versionJson();
    }

    public Downloader downloader() {
        return this.downloader;
    }

    public JavaResolver javaResolver() {
        return this.javaResolver;
    }

    public Environment env() {
        return this.environment;
    }

    public Log log() {
        return this.log;
    }

    public Path cache() {
        return this.environment.installerCache();
    }

    public ProgressListener listener() {
        return this.listener;
    }

    public InstallContext listener(ProgressListener listener) {
        this.listener = listener == null ? ProgressListener.NONE : listener;
        return this;
    }

}
