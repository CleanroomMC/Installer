/*
 * Copyright (c) 2026 CleanroomMC contributors
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package com.cleanroommc.installer.target.server;

import com.cleanroommc.installer.maven.MavenLayout;
import com.cleanroommc.installer.platform.Environment;
import com.cleanroommc.installer.profile.InstallProfile;
import com.cleanroommc.installer.source.ProfileSource;
import com.cleanroommc.installer.target.InstallContext;
import com.cleanroommc.installer.target.InstallRequest;
import com.cleanroommc.installer.target.action.Action;
import com.cleanroommc.installer.target.action.WriteFileAction;
import com.cleanroommc.installer.util.Log;
import com.cleanroommc.launcherutils.LauncherNames;
import com.cleanroommc.launcherutils.version.Download;
import com.cleanroommc.launcherutils.version.Library;
import com.cleanroommc.launcherutils.version.VersionJson;
import com.cleanroommc.platformutils.Architecture;
import com.cleanroommc.platformutils.Platform;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ServerTargetTest {

    @TempDir Path directory;

    private static InstallContext context() {
        InstallProfile profile = new InstallProfile();
        profile.path = "com.cleanroommc:cleanroom:1.0.0:universal";
        VersionJson version = new VersionJson().id("Cleanroom-1.0.0");
        ProfileSource source = new ProfileSource() {

            @Override
            public InstallProfile profile() {
                return profile;
            }

            @Override
            public VersionJson versionJson() {
                return version;
            }

            @Override
            public InputStream open(String path) {
                return null;
            }

            @Override
            public void close() { }

        };
        return new InstallContext(source, null, null, new Environment(), Log.console());
    }

    private static Download download(String url) {
        return new Download().url(url);
    }

    @Test
    void universalJarAppearsOnlyOnceOnTheClasspath() {
        Path universal = Paths.get("libraries/com/cleanroommc/cleanroom/1.0.0/cleanroom-1.0.0-universal.jar");
        Path dependency = Paths.get("libraries/example/dependency/1.0/dependency-1.0.jar");
        List<Action> actions = Arrays.asList(new WriteFileAction(universal, "universal"),
            new WriteFileAction(dependency, "dependency")
        );

        assertThat(ServerTarget.classpathDestinations(actions, universal)).isEqualTo(Arrays.asList(universal, dependency
        ));
    }

    @Test
    void skipsClientNativeMappingsButKeepsSidelessNativeArtifacts() {
        Library clientNative = Library.of("org.lwjgl:lwjgl:3.4.3")
            .side(VersionJson.CLIENT)
            .natives("linux", "natives-linux")
            .classifier("natives-linux", download("https://example.invalid/lwjgl-natives-linux.jar"));

        String os = LauncherNames.os(Platform.current());
        String sidelessClassifier = LauncherNames.NATIVES_PREFIX + os;
        Library sidelessMappedNative = Library.of("example:sideless-native:1.0")
            .natives(os, sidelessClassifier)
            .classifier(sidelessClassifier, download("https://example.invalid/sideless-native.jar"));

        Library sidelessNative = Library.of("io.netty:netty-transport-native-epoll:4.2.16.Final:linux-x86_64")
            .artifact(download("https://example.invalid/netty-native-epoll.jar"));

        List<Action> actions = MavenLayout.actions(Arrays.asList(clientNative, sidelessMappedNative, sidelessNative),
            this.directory, null, Platform.current(), true, false
        );

        assertThat(Arrays.asList(actions.get(0).destination(), actions.get(1).destination())).isEqualTo(
            Arrays.asList(this.directory.resolve("example/sideless-native/1.0/sideless-native-1.0-" +
                        sidelessClassifier + ".jar"
                ),
                this.directory.resolve("io/netty/netty-transport-native-epoll/4.2.16.Final/netty-transport-native-epoll-4.2.16.Final-linux-x86_64.jar"
                )
            ));
    }

    @Test
    void reinstallKeepsAnEditedUserJvmArgs() throws Exception {
        Path userJvmArgs = this.directory.resolve("user_jvm_args.txt");
        InstallRequest request = InstallRequest.builder(ServerTarget.ID)
            .directory(this.directory)
            .flag(ServerTarget.OPTION_NO_SCRIPTS, true)
            .build();

        assertThat(new ServerTarget().plan(request, context()).actions()).anyMatch(action -> action.destination()
            .equals(userJvmArgs));
        Files.write(userJvmArgs, "-Xmx12G".getBytes(StandardCharsets.UTF_8));
        assertThat(new ServerTarget().plan(request, context()).actions()).noneMatch(action -> action.destination()
            .equals(userJvmArgs));
    }

    @Test
    void launchPlatformFollowsThePinnedJavasArchitecture() throws Exception {
        assertThat(ServerTarget.platformOf(this.directory)).isEqualTo(Platform.current());
        Files.write(this.directory.resolve("release"), Arrays.asList("JAVA_VERSION=\"21.0.5\"", "OS_ARCH=\"riscv64\""),
            StandardCharsets.UTF_8
        );
        Platform platform = ServerTarget.platformOf(this.directory);
        assertThat(platform.architecture()).isEqualTo(Architecture.RISCV64);
        assertThat(platform.operatingSystem()).isEqualTo(Platform.current().operatingSystem());
    }

}
