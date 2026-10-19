/*
 * Copyright (c) 2026 CleanroomMC contributors
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package com.cleanroommc.installer.target.server;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

/**
 * The only class inside the server jar, so it must not touch any other installer class.
 * Everything it needs is in the jar's manifest.
 */
public final class ServerStarter {

    static final String MAIN_CLASS = "Launch-Main-Class";
    static final String ARGUMENTS = "Launch-Arguments";
    static final String JAVA_MINIMUM = "Launch-Java-Minimum";
    static final String JAVA_MAXIMUM = "Launch-Java-Maximum";

    private ServerStarter() { }

    private static int javaVersion() {
        String version = System.getProperty("java.specification.version");
        return Integer.parseInt(version.startsWith("1.") ? version.substring(2) : version);
    }

    public static void main(String[] args) throws Throwable {
        Path jar = Paths.get(ServerStarter.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Attributes attributes;
        try (JarFile file = new JarFile(jar.toFile())) {
            attributes = file.getManifest().getMainAttributes();
        }

        int java = javaVersion();
        int minimum = Integer.parseInt(attributes.getValue(JAVA_MINIMUM));
        String maximum = attributes.getValue(JAVA_MAXIMUM);
        if (java < minimum || maximum != null && java > Integer.parseInt(maximum)) {
            System.err.println("Cleanroom needs Java " + minimum + (maximum == null ? " or newer" : " to " + maximum) +
                    ", but this is Java " + java + ". Pick another Java version in the server host's settings."
            );
            System.exit(1);
        }

        // Launchwrapper and Forge read the classpath from this property, and under -jar it names only this jar
        URI base = jar.toAbsolutePath().getParent().toUri();
        StringBuilder classpath = new StringBuilder();
        for (String entry : attributes.getValue(Attributes.Name.CLASS_PATH).split(" ")) {
            if (classpath.length() > 0) {
                classpath.append(File.pathSeparatorChar);
            }
            classpath.append(Paths.get(base.resolve(entry)));
        }
        System.setProperty("java.class.path", classpath.toString());

        List<String> launchArgs = new ArrayList<>(Arrays.asList(attributes.getValue(ARGUMENTS).split(" ")));
        launchArgs.addAll(Arrays.asList(args));
        String[] launch = launchArgs.toArray(new String[0]);
        MethodHandles.publicLookup()
            .findStatic(Class.forName(attributes.getValue(MAIN_CLASS)), "main",
                MethodType.methodType(void.class, String[].class)
            )
            .invokeExact(launch);
    }

}
