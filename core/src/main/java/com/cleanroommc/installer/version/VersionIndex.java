package com.cleanroommc.installer.version;

import com.cleanroommc.installer.InstallerMeta;
import com.cleanroommc.installer.net.Downloader;
import com.cleanroommc.installer.target.ExitCode;
import com.cleanroommc.installer.target.InstallException;
import com.cleanroommc.installer.util.Log;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The list of installable Cleanroom versions, for the generic installer jar.
 * <p>
 * Maven metadata first: it is the same host the artifacts come from, so it can never list a version
 * whose files are missing. The rest of the ecosystem reads GitHub releases, so that stays as a
 * fallback for the case where the repository is unreachable.
 */
public final class VersionIndex {

    private static final long CACHE_TTL_MS = 60L * 60L * 1000L;

    private final Downloader downloader;
    private final Log log;
    private final Path cacheFile;

    public VersionIndex(Downloader downloader, Log log, Path cacheDirectory) {
        this.downloader = downloader;
        this.log = log;
        this.cacheFile = cacheDirectory.resolve("versions.txt");
    }

    public List<RemoteVersion> all() throws InstallException {
        return all(true);
    }

    /**
     * Newest first.
     *
     * @param useCache false to ignore a fresh cache and ask the repository again.
     */
    public List<RemoteVersion> all(boolean useCache) throws InstallException {
        List<String> ids = useCache ? cached() : null;
        if (ids == null) {
            try {
                ids = fromMavenMetadata();
                store(ids);
            } catch (InstallException mavenFailure) {
                this.log.warn("Unable to read maven metadata: {}", mavenFailure.getMessage());
                ids = staleCache();
                if (ids == null) {
                    throw mavenFailure;
                }
            }
        }
        List<RemoteVersion> versions = new ArrayList<>();
        for (String id : ids) {
            versions.add(new RemoteVersion(id, installerUrl(id)));
        }
        return versions;
    }

    public RemoteVersion latest() throws InstallException {
        List<RemoteVersion> versions = all();
        if (versions.isEmpty()) {
            throw new InstallException(ExitCode.NETWORK, "No Cleanroom releases were found");
        }
        return versions.get(0);
    }

    public RemoteVersion byId(String id) throws InstallException {
        for (RemoteVersion version : all()) {
            if (version.id().equals(id)) {
                return version;
            }
        }
        throw new InstallException(ExitCode.USAGE,
                "No such Cleanroom version: " + id + ". Run 'list-versions' to see what is available.");
    }

    public static String installerUrl(String version) {
        return InstallerMeta.CLEANROOM_REPO
                + InstallerMeta.CLEANROOM_GROUP.replace('.', '/') + "/"
                + InstallerMeta.CLEANROOM_ARTIFACT + "/" + version + "/"
                + InstallerMeta.CLEANROOM_ARTIFACT + "-" + version + "-installer.jar";
    }

    private List<String> fromMavenMetadata() throws InstallException {
        String url = InstallerMeta.CLEANROOM_REPO
                + InstallerMeta.CLEANROOM_GROUP.replace('.', '/') + "/"
                + InstallerMeta.CLEANROOM_ARTIFACT + "/maven-metadata.xml";
        byte[] xml = this.downloader.fetch(url);
        List<String> versions = parseMavenMetadata(new InputStreamReader(
                new ByteArrayInputStream(xml), StandardCharsets.UTF_8));
        Collections.reverse(versions);
        return versions;
    }

    static List<String> parseMavenMetadata(Reader reader) throws InstallException {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
        List<String> versions = new ArrayList<>();
        try {
            XMLStreamReader xml = factory.createXMLStreamReader(reader);
            boolean inVersions = false;
            while (xml.hasNext()) {
                int event = xml.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    if ("versions".equals(xml.getLocalName())) {
                        inVersions = true;
                    } else if (inVersions && "version".equals(xml.getLocalName())) {
                        String version = xml.getElementText().trim();
                        if (!version.isEmpty()) {
                            versions.add(version);
                        }
                    }
                } else if (event == XMLStreamConstants.END_ELEMENT && "versions".equals(xml.getLocalName())) {
                    inVersions = false;
                }
            }
            xml.close();
        } catch (XMLStreamException e) {
            throw new InstallException(ExitCode.NETWORK, "Unable to parse maven metadata", e);
        }
        return versions;
    }

    private List<String> cached() {
        try {
            if (!Files.isRegularFile(this.cacheFile)) {
                return null;
            }
            long age = System.currentTimeMillis() - Files.getLastModifiedTime(this.cacheFile).toMillis();
            if (age > CACHE_TTL_MS && !this.downloader.offline()) {
                return null;
            }
            return Files.readAllLines(this.cacheFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    private List<String> staleCache() {
        try {
            return Files.isRegularFile(this.cacheFile) ? Files.readAllLines(this.cacheFile, StandardCharsets.UTF_8) : null;
        } catch (IOException e) {
            return null;
        }
    }

    private void store(List<String> versions) {
        try {
            Files.createDirectories(this.cacheFile.getParent());
            Files.write(this.cacheFile, versions, StandardCharsets.UTF_8);
        } catch (IOException e) {
            this.log.debug("Unable to cache the version list: {}", e.toString());
        }
    }

}
