# Installer

Double-click/execute it for a window. Run it with arguments for command-line interaction.

```
java -jar installer.jar                      # Graphical installer
java -jar installer.jar client               # Add a profile to the Minecraft launcher
java -jar installer.jar server -d ./server   # Set up a dedicated server
java -jar installer.jar server --starter-jar # Also write server.jar, for hosts that only take a jar path
java -jar installer.jar mmc                  # Create a Prism/PolyMC/MultiMC instance
java -jar installer.jar --help
```

## Usage

### Modes

| Mode            | Does                                                                                    |
|-----------------|-----------------------------------------------------------------------------------------|
| `client`        | Writes the Cleanroom version into `.minecraft` and adds a launcher profile              |
| `server`        | Sets up a dedicated server in a directory                                               |
| `mmc`           | Creates a MultiMC based (PolyMC/PrismLauncher etc) instance, or updates an existing one |
| `list-versions` | Prints the installable Cleanroom versions, newest first                                 |
| `uninstall`     | Removes a Cleanroom version installed by `client`                                       |
| `version`       | Prints the installer's own version                                                      |

Every mode installs the newest Cleanroom version unless `--version <id>` names one. `--dir` overrides the detected install directory.

### Client

- Writes `versions/<id>/<id>.json` and the Cleanroom universal jar.
- Leaves the other libraries for the launcher to download. `--full` downloads them all now, for an offline-ready install.
- Adds or updates one profile in `launcher_profiles.json`. Every other profile and key is kept, and a timestamped backup is taken first.
- Re-running a complete install changes nothing. `--force` reinstalls anyway.
- `uninstall` removes one installed version. Pass `--version` when more than one is installed.

| Option                       | Effect                                                 |
|------------------------------|--------------------------------------------------------|
| `--profile-name <s>`         | Launcher profile name                                  |
| `--no-profile`               | Leaves `launcher_profiles.json` alone                  |
| `--launcher-profiles <path>` | Profile file to write, when it is not in `--dir`       |
| `--jvm-args "<args>"`        | JVM arguments for the launcher profile                 |
| `--full`                     | Downloads every library now                            |

### Server

- Downloads the libraries and the vanilla server jar.
- Writes `unix_args.txt` and `win_args.txt`, and the launch script this machine can run (`run.sh` or `run.bat`).
- Writes `user_jvm_args.txt` once. A reinstall keeps the existing file and ignores `--memory`.

| Option            | Effect                                                                    |
|-------------------|---------------------------------------------------------------------------|
| `--memory <size>` | Heap size written to a new `user_jvm_args.txt`, e.g. `6G`                 |
| `--no-scripts`    | Skips the launch script                                                   |
| `--no-server-jar` | Skips the vanilla server jar                                              |
| `--starter-jar`   | Writes `server.jar`, for hosts that only take a jar path                  |
| `--pin-java`      | Writes the Java path into the scripts, always done for a provisioned Java |

### MMC

`--dir` decides what happens.

| `--dir` points at                      | Result                                                           |
|----------------------------------------|------------------------------------------------------------------|
| A launcher or its instances directory  | Creates a new instance                                           |
| An existing Cleanroom instance         | Repairs it, or moves it to the requested version                 |
| An existing Forge instance             | Replaces Forge with Cleanroom, after a prompt unless `--yes`     |
| An existing vanilla instance           | Installs Cleanroom into it                                       |

An update keeps the instance's `instance.cfg` settings, except the keys the installer sets.

| Option                  | Effect                                              |
|-------------------------|-----------------------------------------------------|
| `--instance-name <s>`   | Name for a new instance                             |
| `--launcher <name>`     | `prism`, `polymc` or `multimc`                      |
| `--replace-java-path`   | Points the instance at the Java the installer found |

### Java

The installer looks for a Java that matches the version's install profile.

| Option                 | Effect                                                    |
|------------------------|-----------------------------------------------------------|
| `--java <path>`        | Uses this Java installation                               |
| `--java-version <n>`   | Java feature version to use                               |
| `--java-vendor <name>` | Preferred vendor, e.g. `zulu`, `temurin`                  |
| `--provision-java`     | Downloads a Java runtime when none is found               |
| `--no-provision-java`  | Never downloads one                                       |

### General

| Option                   | Effect                                                     |
|--------------------------|------------------------------------------------------------|
| `--gui` / `--no-gui`     | Forces the window or the command line                      |
| `--offline`              | Uses only what is already downloaded                       |
| `--dry-run`              | Prints the plan, writes nothing but the installer cache    |
| `--force`                | Reinstalls even when everything is in place                |
| `-y`, `--yes`            | Skips the safety checks on the install directory           |
| `--json`                 | Machine-readable output, see [JSON Output](#json-output)   |
| `--quiet` / `--verbose`  | Less or more logging                                       |
| `--log-file <path>`      | Where to write the log                                     |

The installer does not update itself, update mods, or check for newer Cleanroom versions on its own.

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
| `ui`   | The installer's window and its Swing look and feel                          |
| `app`  | Entry point, the command line, and the shaded jar.                          |

## Adding a Mode

- Implement a new `InstallTarget` and add to `core/src/main/resources/META-INF/services/com.cleanroommc.installer.target.InstallTarget`.

## Compatibility

- Two paths inside the installer jar are set in stone: `/version.json` and `/maven/com/cleanroommc/cleanroom/<version>/cleanroom-<version>-universal.jar`.
  - CleanroomRelauncher reads both directly out of published installer jars.
- `version.json` is written self-contained, with no `inheritsFrom`.
- Inheriting from `1.12.2` would cause the launcher to merge parent libraries which ends up adding LWJGL 2 onto the classpath. (Previous installer/zip did this.)
