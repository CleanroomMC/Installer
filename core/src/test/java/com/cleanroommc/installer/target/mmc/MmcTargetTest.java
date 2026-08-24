package com.cleanroommc.installer.target.mmc;

import com.cleanroommc.installer.platform.Environment;
import com.cleanroommc.installer.profile.InstallProfile;
import com.cleanroommc.installer.profile.VersionJson;
import com.cleanroommc.installer.source.ProfileSource;
import com.cleanroommc.installer.target.InstallContext;
import com.cleanroommc.installer.target.InstallException;
import com.cleanroommc.installer.target.InstallPlan;
import com.cleanroommc.installer.target.InstallRequest;
import com.cleanroommc.installer.target.action.CopyResourceAction;
import com.cleanroommc.installer.util.Log;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MmcTargetTest {

    @TempDir
    Path directory;

    @Test
    void replacingJavaPathIsOptInForEveryInstall() {
        assertFalse(MmcTarget.shouldReplaceJavaPath(InstallRequest.builder(MmcTarget.ID).build()));
        assertTrue(MmcTarget.shouldReplaceJavaPath(InstallRequest.builder(MmcTarget.ID)
                .flag(MmcTarget.OPTION_REPLACE_JAVA_PATH, true)
                .build()));
    }

    @Test
    void embeddedPackReusesTheInstallersUniversalJar() throws Exception {
        InstallProfile profile = new InstallProfile();
        profile.profile = "Cleanroom";
        profile.cleanroomVersion = "1.0.0+build.4";
        profile.path = "com.cleanroommc:cleanroom:1.0.0+build.4:universal";
        VersionJson version = new VersionJson();
        version.id = "Cleanroom-1.0.0+build.4";

        String universalEntry = "maven/com/cleanroommc/cleanroom/1.0.0+build.4/"
                + "cleanroom-1.0.0+build.4-universal.jar";
        Map<String, byte[]> resources = new HashMap<>();
        resources.put(MmcTarget.EMBEDDED_PACK, "thin pack".getBytes(StandardCharsets.UTF_8));
        resources.put(universalEntry, "universal".getBytes(StandardCharsets.UTF_8));
        ProfileSource source = source(profile, version, resources);
        Environment environment = new Environment() {
            @Override
            public Path installerCache() {
                return directory.resolve("cache");
            }
        };
        InstallContext context = new InstallContext(source, null, null, environment, Log.console());
        Path instances = directory.resolve("instances");
        InstallRequest request = InstallRequest.builder(MmcTarget.ID).directory(instances).build();

        InstallPlan plan = new MmcTarget().plan(request, context);

        Path instance = instances.resolve("Cleanroom 1.0.0+build.4");
        Path localUniversal = instance.resolve("libraries/cleanroom-1.0.0+build.4-universal.jar");
        assertEquals(4, plan.actions().size());
        CopyResourceAction copy = assertInstanceOf(CopyResourceAction.class, plan.actions().get(2));
        assertEquals(localUniversal, copy.destination());
        copy.execute(context);
        assertEquals("universal", new String(Files.readAllBytes(localUniversal), StandardCharsets.UTF_8));
    }

    private static ProfileSource source(InstallProfile profile, VersionJson version, Map<String, byte[]> resources) {
        return new ProfileSource() {
            @Override
            public InstallProfile profile() throws InstallException {
                return profile;
            }

            @Override
            public VersionJson versionJson() throws InstallException {
                return version;
            }

            @Override
            public InputStream open(String path) {
                byte[] contents = resources.get(path);
                return contents == null ? null : new ByteArrayInputStream(contents);
            }

            @Override
            public void close() { }
        };
    }

}
