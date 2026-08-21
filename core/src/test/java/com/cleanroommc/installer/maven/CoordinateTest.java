package com.cleanroommc.installer.maven;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CoordinateTest {

    @Test
    void parsesGroupArtifactVersion() {
        Coordinate coordinate = Coordinate.parse("com.cleanroommc:cleanroom:0.6.11-alpha");
        assertEquals("com.cleanroommc", coordinate.group());
        assertEquals("cleanroom", coordinate.artifact());
        assertEquals("0.6.11-alpha", coordinate.version());
        assertNull(coordinate.classifier());
        assertEquals("jar", coordinate.extension());
        assertEquals("com/cleanroommc/cleanroom/0.6.11-alpha/cleanroom-0.6.11-alpha.jar", coordinate.path());
    }

    @Test
    void parsesClassifierAndExtension() {
        Coordinate coordinate = Coordinate.parse("de.oceanlabs.mcp:mcp_config:1.12.2-2026@zip");
        assertEquals("zip", coordinate.extension());
        assertEquals("de/oceanlabs/mcp/mcp_config/1.12.2-2026/mcp_config-1.12.2-2026.zip", coordinate.path());

        Coordinate natives = Coordinate.parse("org.lwjgl:lwjgl:3.4.1:natives-linux");
        assertEquals("natives-linux", natives.classifier());
        assertEquals("org/lwjgl/lwjgl/3.4.1/lwjgl-3.4.1-natives-linux.jar", natives.path());
        assertEquals("org.lwjgl:lwjgl:3.4.1", natives.withoutClassifier().toString());
    }

    @Test
    void roundTripsThroughToString() {
        for (String notation : new String[] {
                "com.cleanroommc:cleanroom:0.6.11-alpha",
                "org.lwjgl:lwjgl:3.4.1:natives-macos-arm64",
                "de.oceanlabs.mcp:mcp_config:1.12.2@zip"
        }) {
            assertEquals(notation, Coordinate.parse(notation).toString());
        }
    }

    @Test
    void rejectsNonsense() {
        assertThrows(IllegalArgumentException.class, () -> Coordinate.parse("cleanroom"));
        assertThrows(IllegalArgumentException.class, () -> Coordinate.parse("a:b:c:d:e"));
    }

}
