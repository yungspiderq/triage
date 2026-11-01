# Triage, on Fabric

**Download:** [latest release](https://github.com/yungspiderq/triage/releases) · **Bug reports:** [Issues](https://github.com/yungspiderq/triage/issues) · **Main guide:** [main branch](https://github.com/yungspiderq/triage/tree/main)

The same mod as [the NeoForge tree](https://github.com/yungspiderq/triage/tree/main), for
Fabric 1.21.1. Not a rewrite and not a fork: about forty thousand lines of it are the *same
lines*, copied over by a script and copied again whenever the other tree moves.

    python tools/carry.py        # the shared half, plus every resource
    python tools/config_port.py  # three hundred config settings, from the NeoForge spec
    python tools/registries.py   # items, blocks, sounds, particles, components

    ./gradlew build              # builds triage-fabric-1.21.1-<version>.jar
    ./gradlew runClient          # or runServer

## Why the copying works

Everything above the loader is written against Minecraft rather than against NeoForge:
injuries, illnesses, organs, blood, the readings, every minigame, every screen. This tree
is on `loom.officialMojangMappings()` for that one reason - so a file carried over reads
here exactly as it reads there, class for class and method for method. On Yarn the shared
half would have to be retyped line by line, and then maintained twice forever.

What genuinely differs is the seam, and it is small:

| | NeoForge | here |
|---|---|---|
| where a body is kept | attachments | attachments (Fabric grew them too) |
| packets | `PacketDistributor` | `net/PacketDistributor.java`, same method names |
| registries | `DeferredRegister` | straight into `BuiltInRegistries`, generated |
| config | `ModConfigSpec` | a generated table, a reader and a writer |
| events | an event bus | Fabric callbacks, and twelve mixins for the rest |
| a forced pose | on every entity | `compat/Poses.java` and a mixin on `Player` |

## The wrappers

Eight classes in `event/` and four in `client/` are the mod deciding what happens when
somebody is hit, eats, dies, logs in, reaches out to another player, or looks at one -
about seventeen hundred lines, none of it about a loader, all of it written against an
event object with getters on it. Fabric hands those moments over as callback arguments
instead.

So the arguments are wrapped in the shape the handlers already read. `event/Event.java`
says the rest; `event/Events.java` and `client/TriageClient.java` do the wrapping;
`mixin/` supplies the four moments Fabric has no callback for at all - being hit, being
healed, finishing a meal, something choosing its next target - and the client's seven,
where Fabric has fewest of its own.

The wrappers wear NeoForge's names on purpose. That is what keeps those seventeen hundred
lines carried rather than forked, and a fork would mean every future fix landing three
times.

## Two things the loader does not do for you

**The config does not sync itself.** NeoForge sends a server config to its clients;
nothing here does, so `ConfigPayload` hands the server's copy to a joining player before
the first body is sent. Without it every client would read its own
`config/triage-server.toml`, two people in the same world would disagree about what a
broken leg costs, and the one drawing the screens would be the one who was wrong.

**A few vanilla members are not public.** `triage.accesswidener` lists all of them with
the reason beside each: the list of layers a body is drawn with, which of the two player
models a renderer uses, where the inventory window starts, and the two lists a jigsaw pool
keeps.

## What is not here

No ragdoll compat - the physics mod it talks to is a NeoForge mod, so `compat/Ragdolls.java`
answers no to everything and the mod behaves as it does on NeoForge without it installed.
The calls stay where they are, so there is one file to change if something like it arrives.
