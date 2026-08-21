# Installer

Double-click/execute it for a window. Run it with arguments for command-line interaction.

```
java -jar cleanroom-installer.jar                      # Graphical installer
java -jar cleanroom-installer.jar client               # Add a profile to the Minecraft launcher
java -jar cleanroom-installer.jar server -d ./server   # Set up a dedicated server
java -jar cleanroom-installer.jar mmc                  # Create a Prism/PolyMC/MultiMC instance
java -jar cleanroom-installer.jar --help
```

## Layout

| Module | Contains                                                                    |
|--------|-----------------------------------------------------------------------------|
| `core` | Profiles, downloads, targets, Java resolution, directory detection          |
| `ui`   | Swing LaF shared with CleanroomRelauncher, plus the installer's own window. |
| `app`  | Entry point, the command line, and the shaded jar.                          |

## Adding a Mode

- Implement a new `InstallTarget` and add to `core/src/main/resources/META-INF/services/com.cleanroommc.installer.target.InstallTarget`.

## Compatibility

- Two paths inside an installer jar are load-bearing and must not move: `/version.json` and`/maven/com/cleanroommc/cleanroom/<version>/cleanroom-<version>.jar`.
  - CleanroomRelauncher reads both directly out of published installer jars.
- `version.json` is written self-contained, with no `inheritsFrom`.
- Inheriting from `1.12.2` would cause the launcher to merge parent libraries which ends up adding LWJGL 2 onto the classpath. (Previous installer/zip did this.)
