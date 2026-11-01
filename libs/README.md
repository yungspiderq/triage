# Local compile-only dependencies

The NeoForge 1.21.1 tree compiles against two Sable jars:

- `sable-neoforge-1.21.1-2.0.3.jar`
- `sable_player_ragdoll-1.21.1-0.7.5.jar`

They are **not** committed here. Sable is PolyForm Shield and the ragdoll add-on is All
Rights Reserved, so this repository does not redistribute them.

To build from source, put both jars into this `libs/` directory and run:

```powershell
.\gradlew.bat build
```

Without them the NeoForge build fails at compile time. The released mod does not need them
at runtime — if Sable is absent, the mod behaves as it does without ragdolls installed.

The Forge and Fabric trees do not use these jars.
