package com.cleanroommc.installer.source;

import com.cleanroommc.installer.profile.InstallProfile;
import com.cleanroommc.installer.profile.VersionJson;
import com.cleanroommc.installer.target.ExitCode;
import com.cleanroommc.installer.target.InstallException;
import com.cleanroommc.installer.util.Json;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Reads a profile out of an installer jar on disk — either the jar this code is running from, or a
 * pinned jar the generic installer just downloaded.
 */
public final class JarProfileSource implements ProfileSource {

    public static final String PROFILE_ENTRY = "install_profile.json";
    public static final String VERSION_ENTRY = "version.json";

    private final ZipFile zip;
    private final Path location;
    private InstallProfile profile;
    private VersionJson versionJson;

    public static JarProfileSource of(Path jar) throws InstallException {
        try {
            return new JarProfileSource(new ZipFile(jar.toFile()), jar);
        } catch (IOException e) {
            throw new InstallException(ExitCode.INTERNAL, "Unable to read installer jar " + jar, e);
        }
    }

    private JarProfileSource(ZipFile zip, Path location) {
        this.zip = zip;
        this.location = location;
    }

    public Path location() {
        return this.location;
    }

    /** Whether this jar carries an embedded profile at all — the generic jar does not. */
    public boolean pinned() {
        return this.zip.getEntry(PROFILE_ENTRY) != null;
    }

    @Override
    public InstallProfile profile() throws InstallException {
        if (this.profile == null) {
            this.profile = read(PROFILE_ENTRY, InstallProfile.class);
        }
        return this.profile;
    }

    @Override
    public VersionJson versionJson() throws InstallException {
        if (this.versionJson == null) {
            String entry = VERSION_ENTRY;
            InstallProfile loaded = profile();
            if (loaded != null && loaded.json != null && !loaded.json.isEmpty()) {
                entry = loaded.json.startsWith("/") ? loaded.json.substring(1) : loaded.json;
            }
            this.versionJson = read(entry, VersionJson.class);
        }
        return this.versionJson;
    }

    @Override
    public InputStream open(String path) throws IOException {
        ZipEntry entry = this.zip.getEntry(path.startsWith("/") ? path.substring(1) : path);
        return entry == null ? null : this.zip.getInputStream(entry);
    }

    private <T> T read(String entryName, Class<T> type) throws InstallException {
        try (InputStream in = open(entryName)) {
            if (in == null) {
                throw new InstallException(ExitCode.INTERNAL,
                        this.location + " has no " + entryName + "; it is not a version-pinned installer jar");
            }
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return Json.read(reader, type);
            }
        } catch (IOException e) {
            throw new InstallException(ExitCode.INTERNAL, "Unable to parse " + entryName + " in " + this.location, e);
        }
    }

    @Override
    public void close() {
        try {
            this.zip.close();
        } catch (IOException ignored) {
        }
    }

}
