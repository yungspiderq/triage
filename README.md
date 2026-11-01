# Triage, on Fabric 1.20.1

**Download:** [latest release](https://github.com/yungspiderq/triage/releases) · **Bug reports:** [Issues](https://github.com/yungspiderq/triage/issues) · **Main guide:** [main branch](https://github.com/yungspiderq/triage/tree/main)

The same mod as [the Forge 1.20.1 tree](https://github.com/yungspiderq/triage/tree/forge-1.20.1),
for Fabric. Fourth tree, two ports deep, and almost none of it is written twice.

    python tools/carry.py        # the shared half, plus every resource
    python tools/config_port.py  # three hundred config settings, from the Forge spec
    python tools/registries.py   # items, blocks, sounds, particles

    ./gradlew build              # builds triage-fabric-1.20.1-<version>.jar
    ./gradlew runClient          # or runServer

## Why it is cheap

Two ports stacked on each other, and this one takes the harder half for free. The Forge
1.20.1 tree has already taken the mod down a Minecraft version - data components back to
NBT, a different damage pipeline, a hundred renamed methods - and every one of those
decisions lives in the files this tree copies. What is left is the loader, which is the same
difference as in [triage-fabric](https://github.com/yungspiderq/triage/tree/fabric-1.21.1):

| | Forge 1.20.1 | here |
|---|---|---|
| where a body is kept | a capability, four classes | a field on `Player`, by mixin |
| packets | `SimpleChannel`, one index each | one channel name each |
| registries | `DeferredRegister` | straight into `BuiltInRegistries`, generated |
| config | `ForgeConfigSpec` | a generated table, a reader and a writer |
| events | an event bus | Fabric callbacks, and twelve mixins for the rest |
| a forced pose | on every entity | `compat/Poses.java` and a mixin on `Player` |
| how fast you dig | an event | a mixin on `getDestroySpeed` |

Everything above that line - injuries, illnesses, organs, blood, the readings, every
minigame, every screen - is the Forge tree's own files, copied by `tools/carry.py` and copied
again whenever that tree moves. This tree is on `loom.officialMojangMappings()` for that one
reason.

## Where a body lives

Fabric grew attachments in 1.20.5, which is after this version, so there is nowhere to hang
one. The body is a field added to `Player` by `mixin/PlayerMixin.java`, written out with the
player under the tag `triage:body` through the same `BodyHealth.CODEC` the other three trees
use - so a body written by any build of this mod reads on the others. `data/BodyStore.java`
is the door to it and the only thing above the mixin that knows where it is.

## One place this tree does not copy Forge

Forge fires its hurt event before armour has had a say. A mod that answers it and then sets
the damage to nothing - which is exactly what this one does - has quietly taken armour out of
the game. NeoForge fires after armour and enchantments and before absorption, which is where
the comment at the top of `event/DamageEvents.java` says a blow arrives, and where the mod
spends absorption itself.

So the mixin attaches at the NeoForge point: `mixin/PlayerMixin.java` modifies the damage
local after `getDamageAfterMagicAbsorb` has written to it. On `Player` and not on
`LivingEntity`, which is not a detail - `Player` carries its own copy of `actuallyHurt` and
never calls the one above it, so the same injection a class up is dead code for the only
entity this mod is about. A plated chest is worth
something here. The same is not true of the Forge 1.20.1 build, and that is a bug in it
rather than a difference worth keeping.

## Two things the loader does not do for you

**The config does not sync itself.** Forge sends a server config to its clients; nothing
here does, so `ConfigPayload` hands the server's copy to a joining player before the first
body is sent. Without it every client would read its own `config/triage-server.toml`, and the
one drawing the screens would be the one who was wrong.

**A few vanilla members are not public.** `triage.accesswidener` lists them with the reason
beside each. One is not there and is reached by reflection instead, through `Reach` - the
same as in the Forge tree: an access widener opens a field, and what two of the mod's render
layers need is to replace what a list contains.

## What is not here

No ragdoll compat - the physics mod it talks to is a Forge mod, so `compat/Ragdolls.java`
answers no to everything and the mod behaves as it does on Forge without it installed.
