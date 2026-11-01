#!/usr/bin/env python3
"""Draws the mod's item textures as explicit pixel maps.

Generating these turned out not to work. Three rounds of prompting produced
sprites drawn at nineteen to forty-four cells across when the whole canvas is
sixteen, and no wording fixed it: an image model does not count cells, it draws
something pixel-art-flavoured at whatever density it likes. Downsampled, most of
them lost the one thing a 16x16 icon has to do, which is read at a glance in a
hotbar slot.

So the pixels are written down instead. Each sprite is sixteen strings of sixteen
characters and a legend, which is slower to author and completely predictable:
the silhouette is whatever is typed, the palette cannot drift because the letters
resolve to the same colours the rest of the mod uses, and two sprites that must
match - the blood bag full and empty - are literally the same map.

NOTE: the mod currently ships the original hand-drawn textures, not these. Running this
replaces them; the drawn set is kept as PNGs in _tools/textures-redrawn/ so either set
can be put back without redrawing anything.

    python _tools/draw_items.py            # write them all
    python _tools/draw_items.py bandage    # just one
    python _tools/draw_items.py --check    # measure, write nothing

The house rules, measured off the textures that already existed: the object sits
in 10 to 14 cells across and 10 to 16 down, with a one-cell outline in a very
dark version of its own colour, shading in two or three steps light-to-dark down
the sprite, and no more than eight colours.
"""

import collections
import os
import sys

try:
    from PIL import Image
except ImportError:
    sys.exit("needs Pillow:  pip install pillow")

HERE = os.path.dirname(os.path.abspath(__file__))
OUT_DIR = os.path.join(HERE, "..", "src", "main", "resources", "assets",
                       "triage", "textures", "item")

# The mod's colours, named rather than repeated. Outlines are deliberately a dark
# version of the thing they surround - cloth is outlined in brown, steel in blue -
# because a set of sprites all outlined in the same black reads as clip art.
C = {
    "cloth_out": "#5C5347", "cloth_hi": "#F7F2E6", "cloth": "#E4DCC8",
    "cloth_mid": "#C6BCA6", "cloth_low": "#9C917D",
    "canvas_out": "#3E3226", "canvas_hi": "#EFE7D4", "canvas": "#D8CDB6",
    "canvas_mid": "#C0B5A0", "canvas_low": "#8F8574",
    "wood_out": "#3A2412", "wood_hi": "#96683C", "wood": "#6B4423",
    "wood_low": "#4A3018",
    "leather_out": "#2A1C10", "leather_hi": "#6B4A28", "leather": "#4A3018",
    "steel_out": "#12191E", "steel_hi": "#DDE4EA", "steel": "#B9CBD6",
    "steel_mid": "#8B9BA8", "steel_low": "#526B78", "steel_deep": "#3E5560",
    "slate_out": "#242932", "slate_hi": "#AFB7C4", "slate": "#7C8492",
    "slate_low": "#3E434D",
    "glass_out": "#16323A", "glass_hi": "#A7D4E4", "glass": "#7FD0DC",
    "glass_low": "#3E7A86",
    "amber_hi": "#F7D089", "amber": "#D9A94F",
    "blood_hi": "#C42820", "blood": "#8E1F31", "blood_low": "#5E1220",
    "paper_out": "#352E24", "paper_hi": "#F2EBD9", "paper": "#E1D8C1",
    "paper_low": "#CABFA5", "string": "#8A7F68",
    "red": "#C42820", "amber_tag": "#D9B12A", "green": "#4E9E3E", "black": "#3A3A42",
}


def rgb(name):
    v = C[name].lstrip("#")
    return tuple(int(v[i:i + 2], 16) for i in (0, 2, 4))


SPRITES = {}


def sprite(name, legend, rows):
    assert len(rows) == 16, "%s has %d rows" % (name, len(rows))
    for i, r in enumerate(rows):
        assert len(r) == 16, "%s row %d is %d wide" % (name, i, len(r))
    SPRITES[name] = (legend, rows)


# --- cloth and wood -------------------------------------------------------

# A roll seen end on, with the loose end hanging off it. The old one was a
# rectangle with two stripes, which read as a crate.
sprite("bandage", {"o": "cloth_out", "a": "cloth_hi", "b": "cloth",
                   "c": "cloth_mid", "d": "cloth_low"}, [
    "................",
    "....oooooooo....",
    "..ooaaaaaaaaoo..",
    ".oaaaoaaaaoaaao.",
    ".oaaaoaaaaoaaao.",
    ".obbbobbbbobbbo.",
    ".obbbobbbbobbbo.",
    ".occcoccccoccco.",
    ".occcoccccoccco.",
    ".odddoddddodddo.",
    "..ooddddddddoo..",
    "....ooooooooo...",
    "........oaao....",
    "........obbo....",
    "........occo....",
    "........oooo....",
])

# A strap in a flat loop with the windlass rod through it. The rod has to touch
# the strap at both ends or it reads as two separate objects.
sprite("tourniquet", {"o": "leather_out", "l": "leather", "h": "leather_hi",
                      "w": "wood", "W": "wood_hi"}, [
    "................",
    "................",
    ".....oooooo.....",
    "...oohhhhhhoo...",
    "..ohhloooolhho..",
    "..ohlo....olho..",
    ".oohlo....olhoo.",
    ".oWWWWWWWWWWWWo.",
    ".oohlo....olhoo.",
    "..ohlo....olho..",
    "..ohhloooolhho..",
    "...oohhhhhhoo...",
    ".....oooooo.....",
    "................",
    "................",
    "................",
])

# Two sticks and two bindings. Kept vertical so it cannot be confused with the
# bandage roll at a glance.
sprite("splint", {"o": "wood_out", "w": "wood", "h": "wood_hi", "l": "wood_low",
                  "c": "cloth_hi", "C": "cloth_mid"}, [
    "................",
    "...oo.....oo....",
    "..ohwo...ohwo...",
    "..ohwo...ohwo...",
    ".occccccccccco..",
    ".oCCCCCCCCCCCo..",
    "..ohwo...ohwo...",
    "..ohwo...ohwo...",
    "..ohwo...ohwo...",
    "..ohwo...ohwo...",
    ".occccccccccco..",
    ".oCCCCCCCCCCCo..",
    "..ohwo...ohwo...",
    "..ohwo...ohwo...",
    "..olwo...olwo...",
    "...oo.....oo....",
])

# A satchel with a flap and a buckle. The red cross stays: the old medkit had
# one, and it is the fastest thing to recognise in a full hotbar.
sprite("medkit", {"o": "canvas_out", "h": "canvas_hi", "c": "canvas",
                  "m": "canvas_mid", "l": "canvas_low", "r": "red"}, [
    "................",
    "................",
    "....oo....oo....",
    "...ohco..ocho...",
    "...oco....oco...",
    ".oooooooooooooo.",
    ".ohhhhhhhhhhhho.",
    ".ohhhhhrrhhhhho.",
    ".occccorrcoccco.",
    ".occcorrrroccco.",
    ".occccorrcoccco.",
    ".ommmmmrrmmmmmo.",
    ".ommmmmmmmmmmmo.",
    ".ollllllllllllo.",
    ".oooooooooooooo.",
    "................",
])

# An unrolled instrument pouch. Three tools stand out of it; the tips are the
# only steel in the sprite, so they carry the read.
sprite("surgical_kit", {"o": "steel_out", "h": "steel_hi", "m": "steel_mid",
                        "C": "steel_low", "c": "steel_deep", "r": "red"}, [
    "................",
    "....h..h..h.....",
    "....h..h..r.....",
    "....m..m..r.....",
    ".oooooooooooooo.",
    ".oCCCCCCCCCCCCo.",
    ".oCCCCCCCCCCCCo.",
    ".orrrrrrrrrrrro.",
    ".occcccccccccco.",
    ".occcccccccccco.",
    ".occcccccccccco.",
    ".ommmmmmmmmmmmo.",
    ".oooooooooooooo.",
    "................",
    "................",
    "................",
])

# A blister card. Square pills rather than round ones: a circle inside one cell
# is three cells of grey mush, which is what the generated one turned into.
sprite("painkillers", {"o": "slate_out", "h": "slate_hi", "s": "slate",
                       "l": "slate_low", "w": "cloth_hi"}, [
    "................",
    "................",
    "..oooooooooooo..",
    "..ohhhhhhhhhho..",
    "..ohwwohwwohwo..",
    "..ohwwohwwohwo..",
    "..ohhhhhhhhhho..",
    "..osssssssssso..",
    "..oswwoswwoswo..",
    "..oswwoswwoswo..",
    "..ossssssssslo..",
    "..ollllllllllo..",
    "..oooooooooooo..",
    "................",
    "................",
    "................",
])


# --- bottles and bags -----------------------------------------------------

# A corked bottle, straight sided so the cork reads as a separate block.
sprite("antiseptic", {"o": "glass_out", "h": "glass_hi", "g": "glass",
                      "l": "glass_low", "w": "wood", "W": "wood_hi"}, [
    "................",
    "......oooo......",
    "......oWWo......",
    "......owwo......",
    "......owwo......",
    ".....oohhoo.....",
    "....ohhgggho....",
    "...ohgggggggo...",
    "...ohgggggggo...",
    "...ohgggggggo...",
    "...oggggggglo...",
    "...ollllllllo...",
    "...ollllllllo...",
    "...oollllloo....",
    ".....ooooo......",
    "................",
])

# A squat vial with a flat metal cap. Deliberately shorter and fatter than the
# antiseptic bottle, or the two are the same icon in different colours.
sprite("antibiotics", {"o": "steel_out", "s": "steel", "m": "steel_mid",
                       "h": "cloth_hi", "c": "cloth", "d": "cloth_low"}, [
    "................",
    "................",
    "................",
    "....oooooooo....",
    "....osssssso....",
    "....ommmmmmo....",
    "...oohhhhhhoo...",
    "...ohhhhhhhho...",
    "...ohhhhhhhho...",
    "...occcccccdo...",
    "...occcccccdo...",
    "...oddddddddo...",
    "...oddddddddo...",
    "...oodddddoo....",
    "....oooooo......",
    "................",
])

# A shallow tin with the lid set behind it. Wide and round, so it cannot be
# mistaken for the vial.
sprite("burn_salve", {"o": "steel_out", "s": "steel", "m": "steel_mid",
                      "l": "steel_low", "a": "amber", "A": "amber_hi"}, [
    "................",
    "................",
    "....oooooooo....",
    "...ommmmmmmmo...",
    "..ooooooooooo...",
    ".osssssssssso...",
    "oAAAAAAAAAAAAo..",
    "oAAAAAAAAAAAAo..",
    "oaaaaaaaaaaaao..",
    ".osssssssssso...",
    ".ommmmmmmmmmo...",
    ".ollllllllllo...",
    ".ooooooooooo....",
    "................",
    "................",
    "................",
])


def bag(fill_hi, fill, fill_low, name, outline="steel_out"):
    """The transfusion bag, drawn once and coloured twice.

    Empty and full are the same item with a component swapped, and the model
    swaps under the icon. If the two silhouettes disagree by even a cell the
    thing twitches in the slot as it fills, so they cannot be two drawings.
    """
    sprite(name, {"o": outline, "h": "steel_hi", "s": "steel", "m": "steel_mid",
                  "A": fill_hi, "B": fill, "C": fill_low}, [
        "......oooo......",
        "......o..o......",
        "....ooooooooo...",
        "...ohhhhhhhhho..",
        "...ohhhhhhhhho..",
        "...oAAAAAAAAAo..",
        "...oAAAAAAAAAo..",
        "...oBBBBBBBBBo..",
        "...oBBBBBBBBBo..",
        "...oBBBBBBBBBo..",
        "...oCCCCCCCCCo..",
        "...oCCCCCCCCCo..",
        "...ooCCCCCCCoo..",
        ".....ooosooo....",
        ".......oso......",
        ".......ooo......",
    ])


bag("steel_hi", "steel", "steel_mid", "blood_bag")
bag("blood_hi", "blood", "blood_low", "blood_bag_filled")
# Saline is the same bag again: it is the same object, only what is in it differs.
bag("glass_hi", "glass", "glass_low", "saline", outline="glass_out")

# The tag, unchanged in spirit from the one drawn for 1.2.0: a card on a string
# with the four colours along the tear-off strip.
sprite("triage_tag", {"o": "paper_out", "h": "paper_hi", "p": "paper",
                      "t": "string",
                      "r": "red", "y": "amber_tag", "g": "green", "k": "black"}, [
    "......t.........",
    "......t.........",
    "......t.........",
    ".....oooooo.....",
    "....ohhhhhho....",
    "...ohhhohhhho...",
    "...ohhhhhhhho...",
    "...ohhhhhhhho...",
    "...oppppppppo...",
    "...oppppppppo...",
    "...orrrryyyyo...",
    "...orrrryyyyo...",
    "...oggggkkkko...",
    "...oggggkkkko...",
    "...oppppppppo...",
    "....oooooooo....",
])


# The one item that is not a dressing, a pill or a bag: a large-bore needle with a
# flange to push against. Thin and all metal, so it cannot be mistaken at a glance
# for the vial or either of the bags.
sprite("decompression_needle", {"o": "steel_out", "h": "steel_hi", "s": "steel",
                                "g": "steel_low", "m": "steel_mid"}, [
    "......oooo......",
    "......ohho......",
    ".......oo.......",
    "..oooooooooo....",
    "..ohhhhhhhho....",
    "...ohssssho.....",
    "...osggggso.....",
    "...osggggso.....",
    "...osggggso.....",
    "...osggggso.....",
    "...osggggso.....",
    "...omggggmo.....",
    "...oooooooo.....",
    "......oo........",
    "......hh........",
    "......hh........",
])


def render(name):
    legend, rows = SPRITES[name]
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                image.putpixel((x, y), rgb(legend[ch]) + (255,))
    return image


def report(name, image):
    used = collections.Counter()
    for y in range(16):
        for x in range(16):
            p = image.getpixel((x, y))
            if p[3]:
                used[p[:3]] += 1
    box = image.getchannel("A").getbbox()
    w, h = (box[2] - box[0], box[3] - box[1]) if box else (0, 0)
    flags = []
    if not 4 <= len(used) <= 8:
        flags.append("colours=%d outside 4-8" % len(used))
    if not 100 <= sum(used.values()) <= 165:
        flags.append("fill=%d outside 100-165" % sum(used.values()))
    if w > 14 or h > 16:
        flags.append("silhouette %dx%d too big" % (w, h))
    print("%-18s %2d colours  %3d filled  %2dx%2d  %s"
          % (name, len(used), sum(used.values()), w, h,
             "  <-- " + "; ".join(flags) if flags else "ok"))


def main():
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    check = "--check" in sys.argv
    names = args or list(SPRITES)
    for name in names:
        image = render(name)
        report(name, image)
        if not check:
            image.save(os.path.normpath(os.path.join(OUT_DIR, name + ".png")))
    if not check:
        print("\nwritten to assets/triage/textures/item/")


if __name__ == "__main__":
    main()
