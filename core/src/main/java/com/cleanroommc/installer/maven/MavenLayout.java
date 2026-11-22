/*
 * Copyright (c) 2026 CleanroomMC contributors
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package com.cleanroommc.installer.maven;

import com.cleanroommc.installer.source.ProfileSource;
import com.cleanroommc.installer.target.action.Action;
import com.cleanroommc.installer.target.action.CopyResourceAction;
import com.cleanroommc.installer.target.action.DownloadAction;
import com.cleanroommc.launcherutils.Coordinate;
import com.cleanroommc.launcherutils.LauncherNames;
import com.cleanroommc.launcherutils.rule.RuleContext;
import com.cleanroommc.launcherutils.version.Download;
import com.cleanroommc.launcherutils.version.Library;
import com.cleanroommc.launcherutils.version.VersionJson;
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

    private MavenLayout() { }

    public static Path file(Path librariesDirectory, Coordinate coordinate, Download download) {
        return librariesDirectory.resolve(download.path(coordinate).replace('/', File.separatorChar));
    }

    public static List<Action> actions(List<Library> libraries, Path librariesDirectory, ProfileSource source,
        Platform platform, boolean nativesForThisPlatformOnly
    ) {
        return actions(libraries, librariesDirectory, source, platform, nativesForThisPlatformOnly, true);
    }

    public static List<Action> actions(List<Library> libraries, Path librariesDirectory, ProfileSource source,
        Platform platform, boolean nativesForThisPlatformOnly, boolean includeClientNatives
    ) {
        RuleContext context = RuleContext.of(platform);
        List<Action> actions = new ArrayList<>();
        for (Library library : libraries) {
            if (!library.allowed(context)) {
                continue;
            }
            if (!includeClientNatives && VersionJson.CLIENT.equals(library.side())) {
                continue;
            }
            if (nativesForThisPlatformOnly && !nativeMatches(library, context)) {
                continue;
            }
            Coordinate coordinate = library.coordinate();
            Download artifact = library.artifact();
            if (artifact != null) {
                actions.add(action(coordinate, artifact, librariesDirectory, source));
            }
            String classifier = library.nativeClassifier(context);
            if (classifier != null) {
                Download natives = library.classifier(classifier);
                if (natives != null) {
                    actions.add(action(coordinate.withClassifier(classifier), natives, librariesDirectory, source));
                }
            }
        }
        return actions;
    }

    // A natives jar named in its coordinate must be the one its natives map picks for this platform
    private static boolean nativeMatches(Library library, RuleContext context) {
        String classifier = library.coordinate().classifier();
        if (library.natives().isEmpty() || classifier == null || !classifier.startsWith(LauncherNames.NATIVES_PREFIX)) {
            return true;
        }
        return classifier.equals(library.nativeClassifier(context));
    }

    private static Action action(Coordinate coordinate, Download download, Path librariesDirectory, ProfileSource source
    ) {
        Path destination = file(librariesDirectory, coordinate, download);
        if (download.isEmbedded()) {
            String entry = EMBEDDED_ROOT + download.path(coordinate);
            return new CopyResourceAction(entry, () -> source.open(entry), destination);
        }
        return new DownloadAction(download.url(), destination, download.sha1(), download.size());
    }

}
