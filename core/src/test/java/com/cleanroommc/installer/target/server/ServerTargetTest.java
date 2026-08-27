package com.cleanroommc.installer.target.server;

import com.cleanroommc.installer.maven.MavenLayout;
import com.cleanroommc.installer.profile.Download;
import com.cleanroommc.installer.profile.Library;
import com.cleanroommc.installer.profile.Rule;
import com.cleanroommc.installer.target.action.Action;
import com.cleanroommc.installer.target.action.WriteFileAction;
import com.cleanroommc.platformutils.Platform;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServerTargetTest {

    @TempDir
    Path directory;

    @Test
    void universalJarAppearsOnlyOnceOnTheClasspath() {
        Path universal = Paths.get("libraries/com/cleanroommc/cleanroom/1.0.0/cleanroom-1.0.0-universal.jar");
        Path dependency = Paths.get("libraries/example/dependency/1.0/dependency-1.0.jar");
        List<Action> actions = Arrays.asList(
                new WriteFileAction(universal, "universal"),
                new WriteFileAction(dependency, "dependency"));

        assertEquals(Arrays.asList(universal, dependency), ServerTarget.classpathDestinations(actions, universal));
    }

    @Test
    void skipsClientNativeMappingsButKeepsSidelessNativeArtifacts() {
        Library clientNative = new Library();
        clientNative.name = "org.lwjgl:lwjgl:3.4.3";
        clientNative.side = "client";
        clientNative.natives = Collections.singletonMap("linux", "natives-linux");
        clientNative.downloads = new Library.Downloads();
        clientNative.downloads.classifiers = Collections.singletonMap("natives-linux",
                download("https://example.invalid/lwjgl-natives-linux.jar"));

        String os = Rule.osName(Platform.current());
        String sidelessClassifier = "natives-" + os;
        Library sidelessMappedNative = new Library();
        sidelessMappedNative.name = "example:sideless-native:1.0";
        sidelessMappedNative.natives = Collections.singletonMap(os, sidelessClassifier);
        sidelessMappedNative.downloads = new Library.Downloads();
        sidelessMappedNative.downloads.classifiers = Collections.singletonMap(sidelessClassifier,
                download("https://example.invalid/sideless-native.jar"));

        Library sidelessNative = new Library();
        sidelessNative.name = "io.netty:netty-transport-native-epoll:4.2.16.Final:linux-x86_64";
        sidelessNative.downloads = new Library.Downloads();
        sidelessNative.downloads.artifact = download("https://example.invalid/netty-native-epoll.jar");

        List<Action> actions = MavenLayout.actions(
                Arrays.asList(clientNative, sidelessMappedNative, sidelessNative), this.directory,
                null, Platform.current(), true, false);

        assertEquals(Arrays.asList(
                        this.directory.resolve("example/sideless-native/1.0/sideless-native-1.0-" + sidelessClassifier + ".jar"),
                        this.directory.resolve(
                                "io/netty/netty-transport-native-epoll/4.2.16.Final/netty-transport-native-epoll-4.2.16.Final-linux-x86_64.jar")),
                Arrays.asList(actions.get(0).destination(), actions.get(1).destination()));
    }

    private static Download download(String url) {
        Download download = new Download();
        download.url = url;
        return download;
    }

}
