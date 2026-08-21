package com.cleanroommc.installer.maven;

import com.cleanroommc.installer.profile.Download;
import com.cleanroommc.installer.profile.Library;
import com.cleanroommc.installer.source.ProfileSource;
import com.cleanroommc.installer.target.action.Action;
import com.cleanroommc.installer.target.action.CopyResourceAction;
import com.cleanroommc.installer.target.action.DownloadAction;
import com.cleanroommc.platformutils.Platform;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Turning library entries into files under {@code libraries/}.
 */
public final class MavenLayout {

    public static final String EMBEDDED_ROOT = "maven/";

    public static Path file(Path librariesDirectory, String coordinate, Download download) {
        return librariesDirectory.resolve(download.path(coordinate).replace('/', File.separatorChar));
    }

    public static List<Action> actions(List<Library> libraries, Path librariesDirectory, ProfileSource source,
                                       Platform platform, boolean nativesForThisPlatformOnly) {
        List<Action> actions = new ArrayList<>();
        for (Library library : libraries) {
            if (!library.allowed(platform)) {
                continue;
            }
            if (nativesForThisPlatformOnly && !nativeMatches(library, platform)) {
                continue;
            }
            Download artifact = library.artifact();
            if (artifact != null) {
                actions.add(action(library.name, artifact, librariesDirectory, source));
            }
            String classifier = library.natives == null ? null : library.nativeClassifier(platform);
            if (classifier != null) {
                Download natives = library.classifier(classifier);
                if (natives != null) {
                    actions.add(action(library.name + ":" + classifier, natives, librariesDirectory, source));
                }
            }
        }
        return actions;
    }

    private static boolean nativeMatches(Library library, Platform platform) {
        String[] parts = library.name == null ? new String[0] : library.name.split(":");
        if (parts.length != 4 || !parts[3].startsWith("natives-")) {
            return true;
        }
        return parts[3].equals(library.nativeClassifier(platform));
    }

    private static Action action(String coordinate, Download download, Path librariesDirectory, ProfileSource source) {
        Path destination = file(librariesDirectory, coordinate, download);
        if (download.embedded()) {
            String entry = EMBEDDED_ROOT + download.path(coordinate);
            return new CopyResourceAction(entry, () -> source.open(entry), destination);
        }
        return new DownloadAction(download.url, destination, download.sha1, download.size);
    }

    private MavenLayout() { }

}
