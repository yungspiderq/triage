# -*- coding: utf-8 -*-
"""Builds the medic's house, five times, from one plan.

The first clinic was a nine-by-nine box: cobble plinth, plank walls, a stepped pyramid of
planks for a roof, and inside it one bed, one cauldron and the table. It worked - a medic
spawned, the table was their job site, the piece placed in every village style - and it read
like a shed. Nothing in it said what the building was for, which for the one building this
mod adds to the world is the whole job.

So this is the same idea built properly, and built by a script for a reason: there are five
biome variants and they were five separate files that had already drifted from each other in
small ways. Here the plan is written once and the materials are a table, so a change to the
layout is a change to all five.

What changed, and why each thing is there:

  * Two beds, not one. A clinic with one bed is a bedroom. Two say that people are brought
    here, and the aisle between them is what the room is arranged around.
  * A work bench along the south wall: water to wash in, the table to work on, a stand for
    brewing, a barrel of supplies. That is the order somebody would actually use them in,
    left to right, and it is the order they stand in.
  * A stone floor under that bench and planks everywhere else. One surface to spill on and
    one to walk on, which is the oldest thing a clinic does to a room.
  * A real gabled roof of stairs instead of a stepped pyramid, with eaves that overhang -
    the difference between a building and a box. Desert villages have flat roofs, so the
    desert clinic gets a parapet instead; a pitched sandstone roof in a desert village looks
    like somebody else's house dropped in the street.
  * Windows at head height on both long walls, with a post between them, so the facade has
    a middle. Glass panes rather than blocks, because a pane reads as a window and a block
    of glass reads as a hole.

    python _tools/clinic.py

Writes into this tree and into the Forge 1.20.1 tree; the two Fabric trees pick it up from
those with their own tools/carry.py.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import nbt

HERE = os.path.dirname(os.path.abspath(__file__))
TREE = os.path.dirname(HERE)
FORGE_1201 = r"C:\Users\yungspider\IdeaProjects\triage-1201"

# 1.21.1 and 1.20.1. The block states used here exist in both, so the only difference
# between the two sets of files is the number that says which game wrote them.
DATA_VERSION_121 = 3955
DATA_VERSION_120 = 3465

WIDTH = 11          # x, west to east. The door is in the west gable.
DEPTH = 9           # z, north to south.
WALL_TOP = 4        # the interior is y=1..4, so four blocks of headroom.
CEILING = 5
RIDGE = 9           # gable roofs only; the desert one stops at the parapet.

# Which materials play which part, per village style. The five sets are the ones the old
# files already used - this only adds the parts the old plan had no need of.
STYLES = {
    "plains": dict(wall="oak_planks", base="cobblestone", post="oak_log", stairs="oak_stairs",
                   slab="oak_slab", door="oak_door", pane="glass_pane", tile="smooth_stone",
                   fence="oak_fence", flat=False),
    "desert": dict(wall="cut_sandstone", base="sandstone", post="chiseled_sandstone",
                   stairs="sandstone_stairs", slab="sandstone_slab", door="acacia_door",
                   pane="glass_pane", tile="smooth_sandstone", fence="acacia_fence", flat=True),
    "savanna": dict(wall="acacia_planks", base="terracotta", post="acacia_log",
                    stairs="acacia_stairs", slab="acacia_slab", door="acacia_door",
                    pane="glass_pane", tile="smooth_stone", fence="acacia_fence", flat=False),
    "snowy": dict(wall="spruce_planks", base="cobblestone", post="spruce_log",
                  stairs="spruce_stairs", slab="spruce_slab", door="spruce_door",
                  pane="glass_pane", tile="smooth_stone", fence="spruce_fence", flat=False),
    "taiga": dict(wall="spruce_planks", base="cobblestone", post="spruce_log",
                  stairs="spruce_stairs", slab="spruce_slab", door="spruce_door",
                  pane="glass_pane", tile="smooth_stone", fence="spruce_fence", flat=False),
}


class Build(object):
    """Blocks by position, with the palette worked out at the end."""

    def __init__(self):
        self.blocks = {}
        self.entities = {}

    def put(self, x, y, z, name, props=None, entity=None):
        self.blocks[(x, y, z)] = ("minecraft:" + name if ":" not in name else name,
                                  tuple(sorted((props or {}).items())))
        if entity is not None:
            self.entities[(x, y, z)] = entity

    def fill(self, x0, x1, y0, y1, z0, z1, name, props=None):
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                for z in range(z0, z1 + 1):
                    self.put(x, y, z, name, props)

    def size(self):
        return (max(p[0] for p in self.blocks) + 1,
                max(p[1] for p in self.blocks) + 1,
                max(p[2] for p in self.blocks) + 1)


def roof_top(z):
    """Height of the roof over this row. One block of rise per block toward the ridge."""
    return RIDGE - abs(z - 4)


def clinic(style):
    m = STYLES[style]
    b = Build()

    # ---- the ground it stands on ----------------------------------------
    # Planks under everything, then stone where the work happens. The seam between the
    # two is the only thing in the room that says which half is which.
    b.fill(1, 9, 0, 0, 1, 7, m["wall"])
    b.fill(2, 8, 0, 0, 5, 6, m["tile"])

    # ---- walls -----------------------------------------------------------
    # A plinth of stone, planks above it. Wet floors and rain are what the bottom course
    # of a real wall is about, and it also stops the building looking poured in one piece.
    for x in range(1, 10):
        for z in (1, 7):
            b.put(x, 1, z, m["base"])
            b.fill(x, x, 2, WALL_TOP, z, z, m["wall"])
    for z in range(1, 8):
        for x in (1, 9):
            b.put(x, 1, z, m["base"])
            b.fill(x, x, 2, WALL_TOP, z, z, m["wall"])
    # Corner posts, and one in the middle of each long wall so the facade has a centre to
    # hang the windows either side of.
    for x, z in ((1, 1), (9, 1), (1, 7), (9, 7), (5, 1), (5, 7)):
        b.fill(x, x, 1, WALL_TOP, z, z, m["post"], {"axis": "y"})

    # ---- windows ---------------------------------------------------------
    # Head height, both long walls, and two in the east gable. Panes rather than blocks:
    # a pane reads as a window, a block of glass reads as a hole in the wall.
    for x in (3, 7):
        for z in (1, 7):
            for y in (2, 3):
                b.put(x, y, z, m["pane"], {"east": "true", "west": "true", "north": "false",
                                           "south": "false", "waterlogged": "false"})
    for z in (3, 5):
        for y in (2, 3):
            b.put(9, y, z, m["pane"], {"north": "true", "south": "true", "east": "false",
                                       "west": "false", "waterlogged": "false"})

    # ---- the door, in the middle of the west gable ------------------------
    b.put(1, 1, 4, m["door"], {"facing": "east", "half": "lower", "hinge": "left",
                               "open": "false", "powered": "false"})
    b.put(1, 2, 4, m["door"], {"facing": "east", "half": "upper", "hinge": "left",
                               "open": "false", "powered": "false"})

    # ---- two beds, heads to the north wall --------------------------------
    # The aisle between them is where somebody stands to work, which is why they are two
    # apart rather than side by side.
    for x in (3, 7):
        b.put(x, 1, 2, "white_bed", {"facing": "north", "occupied": "false", "part": "head"})
        b.put(x, 1, 3, "white_bed", {"facing": "north", "occupied": "false", "part": "foot"})
    # A post between the beds with a lamp on it - the one light in the room that is at the
    # height of the person being looked at rather than over the whole floor.
    b.put(5, 1, 2, m["fence"], {"east": "false", "north": "false", "south": "false",
                                "west": "false", "waterlogged": "false"})
    b.put(5, 2, 2, "lantern", {"hanging": "false", "waterlogged": "false"})

    # ---- the bench, along the south wall ---------------------------------
    # Left to right in the order somebody uses them: wash, work, brew, store.
    b.put(3, 1, 6, "water_cauldron", {"level": "3"})
    b.put(4, 1, 6, "triage:medical_table")
    b.put(5, 1, 6, "brewing_stand", {"has_bottle_0": "false", "has_bottle_1": "false",
                                     "has_bottle_2": "false"})
    b.put(6, 1, 6, "barrel", {"facing": "up", "open": "false"},
          entity={"id": nbt.string("minecraft:barrel"),
                  "LootTable": nbt.string("triage:chests/clinic")})
    b.put(7, 1, 6, "crafting_table")

    # ---- ceiling and roof -------------------------------------------------
    b.fill(1, 9, CEILING, CEILING, 1, 7, m["wall"])
    # Two lanterns hung from it, over the aisle rather than over the beds: light on the
    # floor you are working from, not in the eyes of somebody lying down.
    for x in (3, 7):
        b.put(x, WALL_TOP, 4, "lantern", {"hanging": "true", "waterlogged": "false"})

    if m["flat"]:
        # A desert village has flat roofs and a parapet, and this is one.
        b.fill(1, 9, CEILING + 1, CEILING + 1, 1, 1, m["base"])
        b.fill(1, 9, CEILING + 1, CEILING + 1, 7, 7, m["base"])
        b.fill(1, 1, CEILING + 1, CEILING + 1, 2, 6, m["base"])
        b.fill(9, 9, CEILING + 1, CEILING + 1, 2, 6, m["base"])
        # And a shade over the door, because a doorway in a desert without one is a
        # doorway nobody stands in.
        for z in (3, 4, 5):
            b.put(0, CEILING, z, m["slab"], {"type": "bottom", "waterlogged": "false"})
    else:
        # A gable, ridge running east to west. Stairs face toward the ridge, which is the
        # way vanilla lays its own roofs - see the note in the changelog.
        for z in range(0, DEPTH):
            y = roof_top(z)
            for x in range(0, WIDTH):
                if z == 4:
                    b.put(x, y, z, m["wall"])
                else:
                    b.put(x, y, z, m["stairs"],
                          {"facing": "south" if z < 4 else "north", "half": "bottom",
                           "shape": "straight", "waterlogged": "false"})
        # The gable ends themselves, filled in under the slope.
        for x in (1, 9):
            for z in range(1, 8):
                for y in range(CEILING + 1, roof_top(z)):
                    b.put(x, y, z, m["wall"])

    # ---- the two jigsaws, exactly where they were -------------------------
    # The one the street connects to, and the one that puts a villager inside. Both are
    # kept at the positions and orientations the old piece used, because those are what
    # the village generator matches on.
    b.put(0, 0, 4, "jigsaw", {"orientation": "west_up"},
          entity={"id": nbt.string("minecraft:jigsaw"),
                  "name": nbt.string("minecraft:building_entrance"),
                  "target": nbt.string("minecraft:building_entrance"),
                  "pool": nbt.string("minecraft:village/%s/streets" % style),
                  "joint": nbt.string("aligned"),
                  "final_state": nbt.string(
                      "minecraft:%s[facing=east,half=bottom,shape=straight,waterlogged=false]"
                      % m["stairs"])})
    b.put(5, 0, 4, "jigsaw", {"orientation": "up_north"},
          entity={"id": nbt.string("minecraft:jigsaw"),
                  "name": nbt.string("minecraft:bottom"),
                  "target": nbt.string("minecraft:bottom"),
                  "pool": nbt.string("minecraft:village/%s/villagers" % style),
                  "joint": nbt.string("rollable"),
                  "final_state": nbt.string("minecraft:" + m["wall"])})
    return b


def hollow(build):
    """Fills the rest of the box with air, the way vanilla's own houses do.

    Not decoration and not optional: a structure only replaces the blocks it stores, so a
    piece that stores no air is a piece that keeps whatever hillside it landed in - dirt
    through the walls, a boulder where the second bed goes. Vanilla village houses store
    air across their whole box for exactly this reason.

    The ground course is left alone, though. The box is wider than the building by the
    overhang of the eaves, and clearing that at ground level would dig a trench round the
    house instead of letting the street run up to it.
    """
    size = build.size()
    for x in range(size[0]):
        for y in range(1, size[1]):
            for z in range(size[2]):
                if (x, y, z) not in build.blocks:
                    build.put(x, y, z, "air")


def to_nbt(build, data_version):
    palette, index = [], {}
    blocks = []
    for pos in sorted(build.blocks, key=lambda p: (p[1], p[2], p[0])):
        key = build.blocks[pos]
        if key not in index:
            index[key] = len(palette)
            palette.append(key)
        entry = {"pos": nbt.lst(nbt.TAG_INT, [nbt.integer(v) for v in pos]),
                 "state": nbt.integer(index[key])}
        if pos in build.entities:
            entry["nbt"] = nbt.compound(build.entities[pos])
        blocks.append(nbt.compound(entry))

    entries = []
    for name, props in palette:
        value = {"Name": nbt.string(name)}
        if props:
            value["Properties"] = nbt.compound({k: nbt.string(v) for k, v in props})
        entries.append(nbt.compound(value))

    size = build.size()
    return nbt.compound({
        "size": nbt.lst(nbt.TAG_INT, [nbt.integer(v) for v in size]),
        "entities": nbt.lst(nbt.TAG_COMPOUND, []),
        "blocks": nbt.lst(nbt.TAG_COMPOUND, blocks),
        "palette": nbt.lst(nbt.TAG_COMPOUND, entries),
        "DataVersion": nbt.integer(data_version),
    })


def preview(build, style):
    size = build.size()
    print("--- %s  %dx%dx%d, %d blocks" % ((style,) + size + (len(build.blocks),)))
    short = {}
    for y in range(size[1]):
        rows = []
        for z in range(size[2]):
            row = ""
            for x in range(size[0]):
                key = build.blocks.get((x, y, z))
                if key is None:
                    row += "."
                    continue
                name = key[0].split(":")[-1]
                letter = {"white_bed": "B", "medical_table": "T", "water_cauldron": "c",
                          "brewing_stand": "b", "barrel": "u", "crafting_table": "k",
                          "lantern": "*", "jigsaw": "J", "glass_pane": "o",
                          "smooth_stone": "s", "smooth_sandstone": "s"}.get(name)
                if letter is None:
                    letter = ("/" if "stairs" in name else
                              "_" if "slab" in name else
                              "|" if "log" in name or "chiseled" in name else
                              "D" if "door" in name else
                              "f" if "fence" in name else
                              "=" if "planks" in name or "sandstone" in name else "#")
                row += letter
                short[letter] = name
            rows.append(row)
        if any(r.strip(".") for r in rows):
            print("  y=%d" % y)
            for r in rows:
                print("    " + r)
    print("  " + "  ".join("%s=%s" % (k, v) for k, v in sorted(short.items())))


def main():
    for style in STYLES:
        build = clinic(style)
        hollow(build)
        if "--preview" in sys.argv:
            preview(build, style)
        for tree, folder, version in (
                (TREE, "structure", DATA_VERSION_121),
                (FORGE_1201, "structures", DATA_VERSION_120)):
            path = os.path.join(tree, "src", "main", "resources", "data", "triage", folder,
                                "village", "houses", "clinic_%s.nbt" % style)
            os.makedirs(os.path.dirname(path), exist_ok=True)
            nbt.write(path, "", to_nbt(build, version))
        print("clinic_%s: %dx%dx%d, %d blocks" % ((style,) + build.size() + (len(build.blocks),)))


if __name__ == "__main__":
    main()
