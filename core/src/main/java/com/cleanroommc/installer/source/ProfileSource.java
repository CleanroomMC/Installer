package com.cleanroommc.installer.source;

import com.cleanroommc.installer.profile.InstallProfile;
import com.cleanroommc.installer.profile.VersionJson;
import com.cleanroommc.installer.target.InstallException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Where the installer gets what it is installing.
 * <p>
 * A version-pinned jar reads both documents out of itself; the generic jar downloads the pinned jar
 * for the chosen version and reads them out of that. Both end up here, so every target sees one shape.
 */
public interface ProfileSource extends AutoCloseable {

    InstallProfile profile() throws InstallException;

    VersionJson versionJson() throws InstallException;

    /**
     * Opens an artifact the source carries, by its path inside the jar
     * ({@code maven/com/cleanroommc/cleanroom/<v>/cleanroom-<v>.jar}), or null when absent.
     */
    InputStream open(String path) throws IOException;

    @Override
    void close();

}
