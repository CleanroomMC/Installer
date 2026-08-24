package com.cleanroommc.installer.target.server;

import com.cleanroommc.installer.target.action.Action;
import com.cleanroommc.installer.target.action.WriteFileAction;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServerTargetTest {

    @Test
    void universalJarAppearsOnlyOnceOnTheClasspath() {
        Path universal = Paths.get("libraries/com/cleanroommc/cleanroom/1.0.0/cleanroom-1.0.0-universal.jar");
        Path dependency = Paths.get("libraries/example/dependency/1.0/dependency-1.0.jar");
        List<Action> actions = Arrays.asList(
                new WriteFileAction(universal, "universal"),
                new WriteFileAction(dependency, "dependency"));

        assertEquals(Arrays.asList(universal, dependency), ServerTarget.classpathDestinations(actions, universal));
    }

}
