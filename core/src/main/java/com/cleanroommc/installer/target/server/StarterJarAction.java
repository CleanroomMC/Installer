/*
 * Copyright (c) 2026 CleanroomMC contributors
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package com.cleanroommc.installer.target.server;

import com.cleanroommc.installer.target.ExitCode;
import com.cleanroommc.installer.target.InstallContext;
import com.cleanroommc.installer.target.InstallException;
import com.cleanroommc.installer.target.action.Action;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * Write a jar that starts the server through {@code java -jar}, for hosts that only take a jar path.
 */
public final class StarterJarAction extends Action {

    private static final String STARTER_ENTRY = ServerStarter.class.getName().replace('.', '/') + ".class";
    private static final String ALL_UNNAMED = "=ALL-UNNAMED";

    private final Manifest manifest = new Manifest();
    private final List<String> skippedJvmArgs = new ArrayList<>();

    public StarterJarAction(Path destination, Path root, List<Path> classpath, String mainClass, List<String> gameArgs,
        List<String> jvmArgs, int minimumJava, int maximumJava
    ) throws InstallException {
        super(destination);
        Attributes attributes = this.manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attributes.put(Attributes.Name.MAIN_CLASS, ServerStarter.class.getName());
        attributes.put(Attributes.Name.CLASS_PATH, classpath(root, classpath));
        attributes.putValue(ServerStarter.MAIN_CLASS, mainClass);
        attributes.putValue(ServerStarter.ARGUMENTS, String.join(" ", gameArgs));
        attributes.putValue(ServerStarter.JAVA_MINIMUM, Integer.toString(minimumJava));
        if (maximumJava > 0) {
            attributes.putValue(ServerStarter.JAVA_MAXIMUM, Integer.toString(maximumJava));
        }

        // An executable jar can carry only these JVM options, and only towards the unnamed module
        List<String> opens = new ArrayList<>();
        List<String> exports = new ArrayList<>();
        for (int i = 0; i < jvmArgs.size(); i++) {
            String arg = jvmArgs.get(i);
            String option = arg;
            String value = null;
            int equals = arg.indexOf('=');
            if (arg.startsWith("--") && equals > 0) {
                option = arg.substring(0, equals);
                value = arg.substring(equals + 1);
            } else if ((arg.equals("--add-opens") || arg.equals("--add-exports")) && i + 1 < jvmArgs.size()) {
                value = jvmArgs.get(++i);
                arg += " " + value;
            }
            if (value != null && value.endsWith(ALL_UNNAMED) && option.equals("--add-opens")) {
                opens.add(value.substring(0, value.length() - ALL_UNNAMED.length()));
            } else if (value != null && value.endsWith(ALL_UNNAMED) && option.equals("--add-exports")) {
                exports.add(value.substring(0, value.length() - ALL_UNNAMED.length()));
            } else if (arg.equals("--enable-native-access=ALL-UNNAMED")) {
                attributes.putValue("Enable-Native-Access", "ALL-UNNAMED");
            } else {
                this.skippedJvmArgs.add(arg);
            }
        }
        if (!opens.isEmpty()) {
            attributes.putValue("Add-Opens", String.join(" ", opens));
        }
        if (!exports.isEmpty()) {
            attributes.putValue("Add-Exports", String.join(" ", exports));
        }
    }

    private static String classpath(Path root, List<Path> classpath) throws InstallException {
        StringBuilder builder = new StringBuilder();
        for (Path path : classpath) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            if (path.startsWith(root)) {
                String relative = root.relativize(path).toString().replace(path.getFileSystem().getSeparator(), "/");
                try {
                    builder.append(new URI(null, null, relative, null).toASCIIString());
                } catch (URISyntaxException e) {
                    throw new InstallException(ExitCode.TARGET, "Unable to put " + path + " on the classpath", e);
                }
            } else {
                builder.append(path.toUri().toASCIIString());
            }
        }
        return builder.toString();
    }

    /**
     * JVM arguments from the profile that the jar cannot apply on its own.
     */
    public List<String> skippedJvmArgs() {
        return this.skippedJvmArgs;
    }

    @Override
    public String describe() {
        return "WRITE " + destination() + " (starter jar)";
    }

    @Override
    public void execute(InstallContext context) throws InstallException {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (JarOutputStream jar = new JarOutputStream(bytes, this.manifest
            ); InputStream starter = ServerStarter.class.getResourceAsStream("/" + STARTER_ENTRY)) {
                if (starter == null) {
                    throw new IOException("missing: " + STARTER_ENTRY);
                }
                jar.putNextEntry(new JarEntry(STARTER_ENTRY));
                byte[] buffer = new byte[8192];
                int read;
                while ((read = starter.read(buffer)) != -1) {
                    jar.write(buffer, 0, read);
                }
                jar.closeEntry();
            }
            Path parent = destination().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            copyAtomically(new ByteArrayInputStream(bytes.toByteArray()), destination());
        } catch (IOException e) {
            throw new InstallException(ExitCode.TARGET, "Unable to write " + destination(), e);
        }
    }

}
