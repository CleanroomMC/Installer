# Changelog

## 0.2.1 - 2026-10-13

## Bug Fix

- **client**: Pass -XstartOnFirstThread in the launcher profile on macOS *[commit by [@Rongmario](https://github.com/Rongmario) in [bbf46c1](https://github.com/CleanroomMC/Installer/commit/bbf46c1f3994acda618cc3fdb1c28e72726d06c0)]*

**Full Changelog**: https://github.com/CleanroomMC/Installer/compare/0.2.0...0.2.1

## 0.2.0 - 2026-10-10

> [!WARNING]
> This version has breaking changes! More details below.

## Breaking Changes

- Keep the installer window as the only public class of the ui module *[commit by [@Rongmario](https://github.com/Rongmario) in [23ac22e](https://github.com/CleanroomMC/Installer/commit/23ac22ea379aac93fa1f869ebba560e754530691)]*
- Drop the -v alias of --version and document --yes and --launcher-profiles *[commit by [@Rongmario](https://github.com/Rongmario) in [516ecd6](https://github.com/CleanroomMC/Installer/commit/516ecd6e07ef4e31b7a7d25e21d23c6b2ce75637)]*

## Bug Fix

- Pin the server scripts to a Java path only when asked or when Java was provisioned *[commit by [@Rongmario](https://github.com/Rongmario) in [b8c8f26](https://github.com/CleanroomMC/Installer/commit/b8c8f268fe0d59da9bc78ed0cc781c06e3c6f9df)]*
- Take the Java version shown in the window and in help from the Java spec *[commit by [@Rongmario](https://github.com/Rongmario) in [5dd0da1](https://github.com/CleanroomMC/Installer/commit/5dd0da150745baa7b85ead2c7421080dab2d418d)]*
- Add the launcher profile to the Microsoft Store profile file too *[commit by [@Rongmario](https://github.com/Rongmario) in [b90ea27](https://github.com/CleanroomMC/Installer/commit/b90ea270eb0f7c6a85d52caa1a6563399feee175)]*
- Write the Java executable, not its home, into the launcher profile *[commit by [@Rongmario](https://github.com/Rongmario) in [0b8297e](https://github.com/CleanroomMC/Installer/commit/0b8297e92fa93d95c1ae5ad1df76333fba261709)]*
- Keep --dry-run from downloading a Java runtime *[commit by [@Rongmario](https://github.com/Rongmario) in [0ba5dcd](https://github.com/CleanroomMC/Installer/commit/0ba5dcd584df3260e6ca90c4097a73474780cf7d)]*
- Print the plan and the version list as JSON under --json *[commit by [@Rongmario](https://github.com/Rongmario) in [704e371](https://github.com/CleanroomMC/Installer/commit/704e3719034ef8f5f64a5cfd1ffa3194c2f9086a)]*
- Write installed files through a temporary file *[commit by [@Rongmario](https://github.com/Rongmario) in [46ca78c](https://github.com/CleanroomMC/Installer/commit/46ca78c7a905bddf5d4c58a132a9978ca84a5a1a)]*
- Stop a download in progress when the install is cancelled *[commit by [@Rongmario](https://github.com/Rongmario) in [d460f1d](https://github.com/CleanroomMC/Installer/commit/d460f1d204f5b24bec16379928c84f5059b2128a)]*
- Resume a partial download only when a checksum can verify it *[commit by [@Rongmario](https://github.com/Rongmario) in [e089a57](https://github.com/CleanroomMC/Installer/commit/e089a577a6677b5cdb771182158db19f14b3879b)]*
- Verify Cleanroom artifacts against their sha1 sidecar *[commit by [@Rongmario](https://github.com/Rongmario) in [7b393c8](https://github.com/CleanroomMC/Installer/commit/7b393c8f0d0e04626251a60701684406ae46b73d)]*
- Uninstall the Cleanroom version found in the game directory *[commit by [@Rongmario](https://github.com/Rongmario) in [6e65eeb](https://github.com/CleanroomMC/Installer/commit/6e65eebcc18e38f5f38de2e98f2f64ae37b5b75d)]*
- Let --java-version override the Java bounds of the install profile *[commit by [@Rongmario](https://github.com/Rongmario) in [fe02749](https://github.com/CleanroomMC/Installer/commit/fe02749156b856b52a860864927d06d5899c614e)]*
- Honor --launcher and fail when no MultiMC-family launcher is found *[commit by [@Rongmario](https://github.com/Rongmario) in [91997ef](https://github.com/CleanroomMC/Installer/commit/91997efeac4020d7c999da643dfb800b23e03b8e)]*
- Print the installer version for --version-info without a mode *[commit by [@Rongmario](https://github.com/Rongmario) in [6f46f61](https://github.com/CleanroomMC/Installer/commit/6f46f615b9d5ae2858676df79510d2946c2cbf60)]*
- Let the JAVA variable override the runtime in server launch scripts *[commit by [@Rongmario](https://github.com/Rongmario) in [2d59deb](https://github.com/CleanroomMC/Installer/commit/2d59deb2f8201c24aa48bc43cd068517b4fa45f1)]*
- Keep an existing user_jvm_args.txt when re-installing a server *[commit by [@Rongmario](https://github.com/Rongmario) in [4d5711f](https://github.com/CleanroomMC/Installer/commit/4d5711f683c937a06b545653ba9aa15d31c3b5f7)]*

## Refactor

- Remove unused code and stale documentation *[commit by [@Rongmario](https://github.com/Rongmario) in [c8994a2](https://github.com/CleanroomMC/Installer/commit/c8994a2ecac511ff8705e8a45f8f6f5741a3ffd1)]*

## Documentation

- Call the jar installer.jar, the name the build produces *[commit by [@Rongmario](https://github.com/Rongmario) in [3b9d2b8](https://github.com/CleanroomMC/Installer/commit/3b9d2b8b7da257960b24d3e5accc35cb1eb4ccb4)]*
- Describe exit codes, JSON output, the cache and the install profile *[commit by [@Rongmario](https://github.com/Rongmario) in [c08c1e5](https://github.com/CleanroomMC/Installer/commit/c08c1e5160ffd89435eb81b9a579308797a2bc69)]*

## Testing

- Cover the command line, the version index and the zip escape guard *[commit by [@Rongmario](https://github.com/Rongmario) in [81656df](https://github.com/CleanroomMC/Installer/commit/81656df54bdfb9682994e44e7230a08621681b71)]*

## Build and Dependencies

- Stop publishing the ui module *[commit by [@Rongmario](https://github.com/Rongmario) in [3c5dd93](https://github.com/CleanroomMC/Installer/commit/3c5dd93e7ea0501698f740c0729ee627407882ab)]*
- Use Cleanroom Conventions 1.3.1 *[commit by [@Rongmario](https://github.com/Rongmario) in [c33650d](https://github.com/CleanroomMC/Installer/commit/c33650db47189c829952749e8687252633720fa6)]*

**Full Changelog**: https://github.com/CleanroomMC/Installer/compare/0.1.4...0.2.0

## 0.1.4 - 2026-09-28

## Feature

- Keep compatibility with previous cleanroom jar spec *[commit by [@Rongmario](https://github.com/Rongmario) in [aa8cb15](https://github.com/CleanroomMC/Installer/commit/aa8cb154ea9c9475fe62bc3dad87ea214107ddc8)]*

## First-time Contributors

- **[@github-actions[bot]](https://github.com/github-actions[bot]) made their first contribution!**

**Full Changelog**: https://github.com/CleanroomMC/Installer/compare/0.1.3...0.1.4

## 0.1.3 - 2026-09-24

## Bug Fix

- Only copy the embedded universal jar when the MMC pack loads it locally *[commit by [@Rongmario](https://github.com/Rongmario) in [b282870](https://github.com/CleanroomMC/Installer/commit/b282870d76ba82d01e73c70854d6a4e530505488)]*

## Build and Dependencies

- Use Cleanroom Conventions 1.1.7 *[commit by [@Rongmario](https://github.com/Rongmario) in [bee1273](https://github.com/CleanroomMC/Installer/commit/bee1273cf928c9a123d05d0e33e25e34b869814b)]*

**Full Changelog**: https://github.com/CleanroomMC/Installer/compare/0.1.2...0.1.3

## 0.1.2 - 2026-08-27

## Other

- Skip client natives but sideless ones - 0.1.2 *[commit by [@Rongmario](https://github.com/Rongmario) in [2649e7e](https://github.com/CleanroomMC/Installer/commit/2649e7e21d8167da5edf5cac3d5fe1c2a79f1919)]*

**Full Changelog**: https://github.com/CleanroomMC/Installer/compare/0.1.1...0.1.2

## 0.1.1 - 2026-08-24

## Other

- Deduplicate universal jars if it was built locally - 0.1.1 *[commit by [@Rongmario](https://github.com/Rongmario) in [ad16de3](https://github.com/CleanroomMC/Installer/commit/ad16de307368edab2fd4dcfeebd7f16826313172)]*
- Respect `replaceJavaPath` flag properly *[commit by [@Rongmario](https://github.com/Rongmario) in [37c1073](https://github.com/CleanroomMC/Installer/commit/37c1073c56ff7bd68e51a52d0cf58c965b6c5e24)]*

**Full Changelog**: https://github.com/CleanroomMC/Installer/compare/0.1.0...0.1.1

## 0.1.0 - 2026-08-21

## Other

- Fix actions again *[commit by [@Rongmario](https://github.com/Rongmario) in [1878b3d](https://github.com/CleanroomMC/Installer/commit/1878b3d0845a7cc1e91234309852deec70866def)]*
- 0.1.0 - First release *[commit by [@Rongmario](https://github.com/Rongmario) in [f70cb1e](https://github.com/CleanroomMC/Installer/commit/f70cb1eff7b96d7d9990827dd8ba5c4ffda4903a)]*


