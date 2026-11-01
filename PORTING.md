# The 1.20.1 port

A separate tree rather than a branch, and not by choice. The two lines cannot share a
toolchain (17 against 21), a modding platform, a networking layer, a way of hanging data
off a player, or a way of putting data on an item.

What they *can* share is everything above that line — the injuries, the illnesses, the
organs, the readings, the minigames — and that is most of the mod by weight. Around a
hundred of the hundred and twenty-seven files needed nothing but renamed imports.

This file is the list of what actually differs, so that a change made on one side can be
made on the other without rediscovering all of it.

## The three subsystems that were rebuilt

### Where a body lives

`BodyStore` — a capability, a provider, `AttachCapabilitiesEvent` to bolt it on and
`PlayerEvent.Clone` to carry it across a respawn. Four pieces doing what one attachment
did.

The one thing worth not getting wrong: **a respawn through the End portal is not a death.**
Dying is what heals you in this mod, so death drops the body and the portal keeps it. Get
that backwards and every trip through a portal is a free full heal.

Serialisation goes through the same `BodyHealth.CODEC` both lines use, so a body written
by one build reads on the other.

Everything above it still asks `Body.of(player)`, which is still one line. That was the
point of doing the floor first.

### The wire

Thirteen messages on one `SimpleChannel`. Each keeps its record shape and gains
`encode` / `decode` / `handle`.

**The index order in `ModNetwork` is the wire format.** Inserting a message in the middle
renumbers everything after it, and a client on the old numbering will read one packet as
another. New ones go on the end.

A whole body goes over as the tag it saves as (`Buffers`) rather than through a
purpose-built encoder — shorter by about two hundred lines and it cannot drift from the
save format, because it *is* the save format.

### What is on an item

1.20.1 has no data components, so the blood bag's type is a string in the stack's NBT
under `triage_blood`.

## Two fields opened by hand

ForgeGradle did not apply the access transformer on this toolchain, so `Reach` opens two
fields at runtime instead:

- `StructureTemplatePool.templates` / `rawTemplates` — there is no API for appending to a
  village pool; a datapack can only replace one.
- `LivingEntityRenderer.layers` — there is no API for *replacing* a render layer, only for
  adding one, and adding would draw armour over the cape.

A third is found by type rather than by name: `Explosion` keeps its radius private and
Forge adds no getter, so `Reach.soleFloat` takes the only float on the class. By type
because the name differs between compiling and running, and the type does not.

## The smaller differences, in one place

| | 1.21.1 | 1.20.1 |
|:---|:---|:---|
| Tick events | three events | one class with a `phase` — checked in every handler |
| Damage | `LivingDamageEvent.Pre` | `LivingHurtEvent`, `getAmount` / `setAmount` |
| HUD | `LayeredDraw.Layer` | `IGuiOverlay`, registered as an overlay |
| Tinting a model part | packed ARGB int | four floats — unpacked once, in `Tint` |
| A flat particle | `renderRotatedQuad` | the four corners, written out in `StainParticle` |
| Attribute modifiers | `ResourceLocation` | a UUID and a label, derived from the same strings |
| Trade cost | `ItemCost` | a plain `ItemStack` |
| Player model variants | `PlayerSkin.Model` | the strings `"default"` and `"slim"` |
| Food effects | a component on the stack | `getFoodProperties` on the item |
| Advancements | `AdvancementHolder` | `Advancement` |
| Scroll | four arguments | three |
| Config | `ModConfigSpec` | `ForgeConfigSpec`; `defineList` takes no supplier |

## What is genuinely missing here

**Ragdolls.** Sable: Ragdolls has no 1.20.1 build, so `RagdollHooks` is a stub that
answers no. This is already a supported path — most players on either line do not have it
installed, and a player who goes down without a physics mod gets the pose instead. If a
1.20.1 build ever appears, that one file is the only thing to change.

**A broken arm costs swing speed only.** 1.20.1 has no attribute for how fast a block
comes apart. The other line takes both. This is the one place the two builds do different
arithmetic, and it is one line of it.
