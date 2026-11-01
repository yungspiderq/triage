# Building Triage

Each loader and Minecraft version is a complete Gradle project on its own branch. Check out
the branch you need, then build it like any other mod.

| Branch | Loader | Minecraft | Gradle JVM | Target Java |
|:---|:---|:---|:---|:---|
| `main` | NeoForge | 1.21.1 | JDK 25 | 21 |
| `forge-1.20.1` | Forge | 1.20.1 | JDK 17 | 17 |
| `fabric-1.21.1` | Fabric | 1.21.1 | JDK 21 | 21 |
| `fabric-1.20.1` | Fabric | 1.20.1 | JDK 21 | 17 |

The NeoForge build needs JDK 25 only because the CurseForge publishing plugin refuses to
resolve on an older JVM. The mod itself is compiled for Java 21 by the toolchain.

## NeoForge 1.21.1 (`main`)

The build compiles against two Sable jars that are not redistributed in this repository.
Put them in `libs/` first:

- `sable-neoforge-1.21.1-2.0.3.jar`
- `sable_player_ragdoll-1.21.1-0.7.5.jar`

Then:

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot"
.\gradlew.bat build
```

The jar appears in `build/libs/`.

## Forge 1.20.1

```powershell
git switch forge-1.20.1
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
.\gradlew.bat build
```

## Fabric 1.21.1

```powershell
git switch fabric-1.21.1
.\gradlew.bat build
```

Use JDK 21. Fabric API is pulled by Loom for the development environment and is required at
runtime.

## Fabric 1.20.1

```powershell
git switch fabric-1.20.1
.\gradlew.bat build
```

The wrapper is configured for JDK 21; the mod targets Java 17. Fabric API is required at
runtime.

## Running a development client

```powershell
.\gradlew.bat runClient
```

A dedicated server is `.\gradlew.bat runServer`. Do not pass `--args` to `runClient` on the
NeoForge tree; run it plain.

## Reporting a bug

Open an [issue](https://github.com/yungspiderq/triage/issues/new/choose). Include the
loader, Minecraft version, Triage version, the other mods you run, and `latest.log` or the
crash report.
