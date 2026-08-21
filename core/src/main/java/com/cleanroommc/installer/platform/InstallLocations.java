package com.cleanroommc.installer.platform;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Default install directory detection.
 */
public final class InstallLocations {

    public static Path minecraft(Environment environment) {
        List<Path> candidates = minecraftCandidates(environment);
        for (Path candidate : candidates) {
            if (looksLikeMinecraft(candidate)) {
                return candidate;
            }
        }
        for (Path candidate : candidates) {
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
        }
        return candidates.get(0);
    }

    public static List<Path> minecraftCandidates(Environment environment) {
        List<Path> candidates = new ArrayList<>();
        if (environment.windows()) {
            addIfNamed(candidates, environment, environment.env("APPDATA"), ".minecraft");
            addIfNamed(candidates, environment, environment.env("USERPROFILE"), "AppData", "Roaming", ".minecraft");
        } else if (environment.macOs()) {
            candidates.add(environment.home().resolve("Library/Application Support/minecraft"));
        } else {
            candidates.add(environment.home().resolve(".minecraft"));
            addIfNamed(candidates, environment, environment.env("XDG_DATA_HOME"), "minecraft");
            candidates.add(environment.home().resolve(".local/share/minecraft"));
            candidates.add(environment.home().resolve(".var/app/com.mojang.Minecraft/.minecraft"));
        }
        return candidates;
    }

    public static boolean looksLikeMinecraft(Path directory) {
        return Files.isRegularFile(directory.resolve("launcher_profiles.json"))
                || Files.isRegularFile(directory.resolve("launcher_profiles_microsoft_store.json"))
                || Files.isDirectory(directory.resolve("versions"));
    }

    public static boolean looksLikeMmcInstance(Path directory) {
        return Files.isRegularFile(directory.resolve("instance.cfg")) || Files.isRegularFile(directory.resolve("mmc-pack.json"));
    }

    public static Path instancesOfLauncherRoot(Path directory) {
        for (DetectedLauncher.Kind kind : DetectedLauncher.Kind.values()) {
            if (Files.isRegularFile(directory.resolve(kind.configName()))) {
                return instancesDirectory(directory, kind);
            }
        }
        return null;
    }

    public static Path serverDefault(Environment environment) {
        return environment.workingDirectory();
    }


    public static List<DetectedLauncher> multiMcFamily(Environment environment) {
        List<DetectedLauncher> found = new ArrayList<>();
        for (DetectedLauncher.Kind kind : DetectedLauncher.Kind.values()) {
            for (Path root : launcherRoots(environment, kind)) {
                if (!Files.isDirectory(root)) {
                    continue;
                }
                Path instances = instancesDirectory(root, kind);
                if (instances != null) {
                    found.add(new DetectedLauncher(kind, root, instances));
                    break;
                }
            }
        }
        return found;
    }

    private static List<Path> launcherRoots(Environment environment, DetectedLauncher.Kind kind) {
        Set<Path> roots = new LinkedHashSet<>();
        String windowsName = kind == DetectedLauncher.Kind.PRISM ? "PrismLauncher"
                : kind == DetectedLauncher.Kind.POLY_MC ? "PolyMC" : "MultiMC";
        if (environment.windows()) {
            addIfNamed(roots, environment, environment.env("APPDATA"), windowsName);
            addIfNamed(roots, environment, environment.env("USERPROFILE"), windowsName);
        } else if (environment.macOs()) {
            roots.add(environment.home().resolve("Library/Application Support").resolve(windowsName));
            if (kind == DetectedLauncher.Kind.MULTI_MC) {
                roots.add(environment.path("/Applications/MultiMC.app/Contents/MacOS"));
            }
        } else {
            String flatpak = kind == DetectedLauncher.Kind.PRISM
                    ? ".var/app/org.prismlauncher.PrismLauncher/data/PrismLauncher"
                    : kind == DetectedLauncher.Kind.POLY_MC
                    ? ".var/app/org.polymc.PolyMC/data/PolyMC"
                    : null;
            addIfNamed(roots, environment, environment.env("XDG_DATA_HOME"), windowsName);
            roots.add(environment.home().resolve(".local/share").resolve(windowsName));
            if (kind == DetectedLauncher.Kind.MULTI_MC) {
                roots.add(environment.home().resolve(".local/share/multimc"));
                roots.add(environment.home().resolve("MultiMC"));
            }
            if (flatpak != null) {
                roots.add(environment.home().resolve(flatpak));
            }
        }
        if (kind == DetectedLauncher.Kind.MULTI_MC) {
            Path here = environment.workingDirectory();
            roots.add(here);
            if (here.getParent() != null) {
                roots.add(here.getParent());
            }
        }
        return new ArrayList<>(roots);
    }

    static Path instancesDirectory(Path root, DetectedLauncher.Kind kind) {
        Path config = root.resolve(kind.configName());
        if (Files.isRegularFile(config)) {
            String configured = readKey(config, "InstanceDir");
            if (configured != null && !configured.isEmpty()) {
                Path resolved = root.resolve(configured).normalize();
                if (Files.isDirectory(resolved)) {
                    return resolved;
                }
            }
        }
        Path instances = root.resolve("instances");
        if (Files.isDirectory(instances)) {
            return instances;
        }
        return Files.isRegularFile(config) ? instances : null;
    }

    private static String readKey(Path config, String key) {
        try {
            for (String line : Files.readAllLines(config, StandardCharsets.UTF_8)) {
                int separator = line.indexOf('=');
                if (separator > 0 && line.substring(0, separator).trim().equals(key)) {
                    return line.substring(separator + 1).trim();
                }
            }
        } catch (IOException ignored) { }
        return null;
    }

    private static void addIfNamed(Collection<Path> target, Environment environment, String base, String... more) {
        if (base == null || base.isEmpty()) {
            return;
        }
        target.add(environment.path(base, more));
    }

    private InstallLocations() { }

}
