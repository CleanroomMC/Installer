# Installer

Double-click/execute it for a window. Run it with arguments for command-line interaction.

```
java -jar installer.jar                      # Graphical installer
java -jar installer.jar client               # Add a profile to the Minecraft launcher
java -jar installer.jar server -d ./server   # Set up a dedicated server
java -jar installer.jar mmc                  # Create a Prism/PolyMC/MultiMC instance
java -jar installer.jar --help
```

## Exit Codes

| Code | Meaning                                         |
|------|-------------------------------------------------|
| 0    | Success                                         |
| 1    | Internal error                                  |
| 2    | Usage error                                     |
| 3    | Network failure                                 |
| 4    | A download failed its size or SHA-1 check       |
| 5    | The install directory is unusable               |
| 6    | No suitable Java was found or could be fetched  |
| 7    | Cancelled                                       |

## JSON Output

`--json` prints one JSON object per line on standard output. Log lines go to standard error.

| Event      | Fields                                         | Printed                             |
|------------|------------------------------------------------|-------------------------------------|
| `stage`    | `name`                                         | When a phase starts                 |
| `progress` | `stage`, `detail`, `done`, `total`             | During a phase, `total` -1 if unknown |
| `versions` | `versions`                                     | By `list-versions`, newest first    |
| `plan`     | `target`, `version`, `root`, `actions`, `notes` | By `--dry-run`, in place of `result` |
| `result`   | `ok` true, `root`, `files`, `notes`            | After a successful run              |
| `result`   | `ok` false, `code`, `message`                  | After a failed run, `code` is the exit code |

## Cache

Everything the installer keeps lives in `~/.cleanroom`. Set `-Dcleanroom.homeDir=<path>` to move it.

| Path                     | Contains                                      |
|--------------------------|-----------------------------------------------|
| `installer/installers/`  | Downloaded Cleanroom installer jars           |
| `installer/packs/`       | Downloaded MultiMC packs                      |
| `installer/versions.txt` | The version list, refreshed after an hour     |
| `installer/logs/`        | Run logs                                      |
| `java/`                  | Java runtimes fetched by `--provision-java`   |

`--dry-run` still downloads the installer jar into the cache, because the plan is read from it.

## Install Profile

An installer jar carries `install_profile.json`. Spec 1 keeps every spec 0 key and adds the ones below. A profile without them gets the defaults.

| Key                | Type     | Meaning                                                        |
|--------------------|----------|----------------------------------------------------------------|
| `spec`             | number   | `1`                                                            |
| `cleanroomVersion` | string   | Cleanroom version, derived from `path` when absent             |
| `java.minimum`     | number   | Lowest Java feature version accepted, default 25               |
| `java.maximum`     | number   | Highest Java feature version accepted, default 25              |
| `java.recommended` | number   | Version to look for first and to provision, default 25         |
| `java.distro`      | string   | Preferred vendor when provisioning, default `zulu`             |
| `jvmArgs`          | string[] | Default JVM arguments for the launcher profile                 |
| `serverMainClass`  | string   | Server main class, default is the one in `version.json`        |
| `serverTweakers`   | string[] | Server tweak classes, default is the built-in list             |

The installer does not read `mainClass`, `tweakers`, `serverJarPath` and `repositories` yet.

## Layout

| Module | Contains                                                                    |
|--------|-----------------------------------------------------------------------------|
| `core` | Profiles, downloads, targets, Java resolution, directory detection          |
| `ui`   | Swing LaF shared with CleanroomRelauncher, plus the installer's own window. |
| `app`  | Entry point, the command line, and the shaded jar.                          |

## Adding a Mode

- Implement a new `InstallTarget` and add to `core/src/main/resources/META-INF/services/com.cleanroommc.installer.target.InstallTarget`.

## Compatibility

- Two paths inside an installer jar are load-bearing and must not move: `/version.json` and `/maven/com/cleanroommc/cleanroom/<version>/cleanroom-<version>-universal.jar`.
  - CleanroomRelauncher reads both directly out of published installer jars.
- `version.json` is written self-contained, with no `inheritsFrom`.
- Inheriting from `1.12.2` would cause the launcher to merge parent libraries which ends up adding LWJGL 2 onto the classpath. (Previous installer/zip did this.)
