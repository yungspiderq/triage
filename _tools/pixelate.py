#!/usr/bin/env python3
"""Turns a generated pixel-art image into a real 16x16 Minecraft item texture.

An image model does not draw a 16x16 PNG. It draws a big picture *of* one: a
1024-pixel canvas of large squares, soft edges between them, sometimes a grid
ruled on top, and a background that is nearly - but not exactly - one colour.
Scaling that down the ordinary way averages neighbouring cells together and
produces mud.

So this votes instead of scaling. It measures the pitch the model actually drew
at, splits the canvas into sixteen bands each way, takes the most common colour
in the middle of each band, keys out everything close to the background, and
snaps the rest onto the palette the mod already uses.

    python _tools/pixelate.py gen/bandage.png bandage --dry
    python _tools/pixelate.py gen/bandage.png bandage

The report is the point. A model asked for 16x16 will cheerfully hand back 24x24
with a buckle on it, and that only shows up as a number: native pitch, colour
count, stray blobs. Anything it complains about is worth regenerating rather than
repairing by hand - the prompt is cheaper than the pixels.

Options:
    --grid N        force the source grid instead of measuring it
    --keep-colours  do not snap to the mod's palette
    --dry           report only, write nothing
    --crop L,T,R,B  cut the source first, for sheets or stray marks
"""

import argparse
import collections
import os
import sys

try:
    from PIL import Image
except ImportError:
    sys.exit("needs Pillow:  pip install pillow")

SIZE = 16
HERE = os.path.dirname(os.path.abspath(__file__))
OUT_DIR = os.path.join(HERE, "..", "src", "main", "resources", "assets",
                       "triage", "textures", "item")

# Every colour the existing item textures use eight times or more - the ones the
# mod has actually committed to, as opposed to the stray one-offs. A new item
# lands in the same box of pencils as the old ones rather than beside it.
PALETTE = [
    (0x12, 0x19, 0x1E), (0xB9, 0xCB, 0xD6), (0x7C, 0x84, 0x92), (0x3A, 0x32, 0x28),
    (0x2A, 0x1C, 0x10), (0x3E, 0x32, 0x26), (0x24, 0x29, 0x32), (0xF2, 0xEC, 0xE0),
    (0x5C, 0x53, 0x47), (0xC4, 0x28, 0x20), (0x3A, 0x24, 0x12), (0x3E, 0x43, 0x4D),
    (0x16, 0x32, 0x3A), (0xD8, 0xCD, 0xB6), (0x8B, 0x9B, 0xA8), (0x35, 0x2E, 0x24),
    (0x7F, 0xD0, 0xDC), (0x6B, 0x44, 0x23), (0x4A, 0x30, 0x18), (0xE4, 0xDC, 0xC8),
    (0x5C, 0x68, 0x78), (0xC6, 0xBC, 0xA6), (0x8E, 0x1F, 0x31), (0xA7, 0xD4, 0xE4),
    (0xC0, 0xB6, 0xA2), (0x3E, 0x55, 0x60), (0xF2, 0xEB, 0xD9), (0x3E, 0x7A, 0x86),
    (0xEF, 0xE7, 0xD4), (0x52, 0x6B, 0x78), (0x6B, 0x4A, 0x28), (0xE1, 0xD8, 0xC1),
    (0xF7, 0xF2, 0xE6), (0x7E, 0x78, 0x90), (0xFB, 0xF8, 0xF0), (0x88, 0x94, 0xA4),
    (0x9C, 0x91, 0x7D), (0xC0, 0xB5, 0xA0), (0xF7, 0xD0, 0x89), (0x8F, 0x85, 0x74),
    (0x96, 0x68, 0x3C), (0x26, 0x34, 0x3C), (0xD9, 0xA9, 0x4F), (0x5E, 0x12, 0x20),
    (0xAF, 0xB7, 0xC4), (0x7F, 0xB4, 0xCA), (0x9A, 0x9E, 0xA6), (0xA9, 0x9E, 0x84),
    (0xDD, 0xE4, 0xEA), (0x8E, 0x1F, 0x1A), (0xCA, 0xBF, 0xA5), (0xD9, 0xB1, 0x2A),
    (0x4E, 0x9E, 0x3E), (0x3A, 0x3A, 0x42), (0x8A, 0x7F, 0x68),
]


def nearest(colour):
    r, g, b = colour
    # Weighted the way an eye is, so "closest" means closest to look at.
    return min(PALETTE, key=lambda p: 2 * (p[0] - r) ** 2 + 4 * (p[1] - g) ** 2 + 3 * (p[2] - b) ** 2)


def apart(a, b, tolerance):
    return sum(abs(x - y) for x, y in zip(a, b)) > tolerance


def edges_along(image, horizontal):
    """Where colour changes, sampled across a spread of rows or columns."""
    w, h = image.size
    length, across = (w, h) if horizontal else (h, w)
    found = []
    for line in range(across // 8, across, max(1, across // 12)):
        previous = None
        for i in range(length):
            pixel = image.getpixel((i, line) if horizontal else (line, i))
            if previous is not None and apart(pixel, previous, 40):
                found.append(i)
            previous = pixel
    return found, length


def native_grid(image):
    """How many cells across the model actually drew, measured rather than assumed.

    Asking for 16 and getting 24 is the commonest way one of these comes back
    unusable, and it is invisible until the sprite is in an inventory slot looking
    like porridge.

    Measured by asking which grid explains the picture. On the grid the model
    really used, every colour change falls on a cell boundary; on any other, they
    scatter. So score each candidate by the share of edges that land on one of its
    boundaries and take the smallest grid that explains nearly all of them - the
    smallest, because every multiple of the true grid explains it just as well.
    """
    scores = {}
    for horizontal in (True, False):
        found, length = edges_along(image, horizontal)
        if not found:
            continue
        for candidate in range(8, 49):
            pitch = length / candidate
            on = sum(1 for e in found if min(e % pitch, pitch - (e % pitch)) <= max(2.0, pitch * 0.12))
            scores[candidate] = scores.get(candidate, 0.0) + on / len(found) / 2.0
    if not scores:
        return SIZE
    best = max(scores.values())
    for candidate in sorted(scores):
        if scores[candidate] >= min(0.92, best - 0.02):
            return candidate
    return SIZE


def band_colour(image, x0, y0, x1, y1):
    """Most common colour in the middle of one band, ignoring the ruled edges."""
    inset_x = max(1, (x1 - x0) // 4)
    inset_y = max(1, (y1 - y0) // 4)
    counts = collections.Counter()
    for y in range(y0 + inset_y, max(y0 + inset_y + 1, y1 - inset_y)):
        for x in range(x0 + inset_x, max(x0 + inset_x + 1, x1 - inset_x)):
            counts[image.getpixel((x, y))] += 1
    return counts.most_common(1)[0][0]


def paper_colour(image):
    """The colour of the empty canvas, whatever the model decided that was.

    Asked for #FF00FF and given something else is the rule rather than the
    exception: a darker pink, a checkerboard of two, JPEG mottling that makes
    every corner slightly different. So this reads the corners and takes the
    average of the commonest, and everything else compares against that with a
    wide tolerance rather than against the magenta that was asked for.
    """
    w, h = image.size
    band = max(2, min(w, h) // 20)
    edge = collections.Counter()
    for x in range(0, w, max(1, w // 200)):
        for y in list(range(band)) + list(range(h - band, h)):
            edge[image.getpixel((x, y))] += 1
    for y in range(0, h, max(1, h // 200)):
        for x in list(range(band)) + list(range(w - band, w)):
            edge[image.getpixel((x, y))] += 1
    common = edge.most_common(40)
    top = common[0][0]
    near = [(c, n) for c, n in common if not apart(c, top, 150)]
    weight = sum(n for _, n in near)
    return tuple(round(sum(c[i] * n for c, n in near) / weight) for i in range(3))


def subject_box(image, paper):
    """The drawn object on its own, with the empty canvas trimmed off.

    A model given a wide canvas draws a small sprite in the middle of it, so the
    picture is mostly nothing. Sixteen bands across all that nothing would spend
    most of the sprite's cells on background and squeeze the object into four.
    """
    w, h = image.size
    step = max(1, min(w, h) // 400)
    x0, y0, x1, y1 = w, h, 0, 0
    for y in range(0, h, step):
        for x in range(0, w, step):
            if apart(image.getpixel((x, y)), paper, 150):
                x0, y0 = min(x0, x), min(y0, y)
                x1, y1 = max(x1, x), max(y1, y)
    if x1 <= x0 or y1 <= y0:
        return None
    pad = step + 1
    return (max(0, x0 - pad), max(0, y0 - pad), min(w, x1 + pad), min(h, y1 + pad))


def largest_blob(filled):
    """Size of the biggest connected group, to catch swatches left floating about."""
    seen, best = set(), 0
    for start in filled:
        if start in seen:
            continue
        stack, size = [start], 0
        seen.add(start)
        while stack:
            x, y = stack.pop()
            size += 1
            for step in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nxt = (x + step[0], y + step[1])
                if nxt in filled and nxt not in seen:
                    seen.add(nxt)
                    stack.append(nxt)
        best = max(best, size)
    return best


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("source")
    parser.add_argument("name", help="texture name, e.g. bandage")
    parser.add_argument("--grid", type=int, help="force the source grid size")
    parser.add_argument("--crop", help="L,T,R,B in pixels, applied first")
    parser.add_argument("--keep-colours", action="store_true")
    parser.add_argument("--dry", action="store_true")
    args = parser.parse_args()

    image = Image.open(args.source).convert("RGB")
    if args.crop:
        image = image.crop(tuple(int(v) for v in args.crop.split(",")))
    print("source               : %dx%d" % image.size)

    paper = paper_colour(image)
    box = subject_box(image, paper)
    if box is None:
        sys.exit("nothing but background in this image")
    image = image.crop(box)
    w, h = image.size
    print("background           : #%02X%02X%02X" % paper)
    print("subject found at     : %dx%d  (was %s in the canvas)" % (w, h, str(box)))

    across = args.grid or native_grid(image)
    down = max(1, round(across * h / w))
    print("subject grid         : about %d x %d cells" % (across, down))
    if across > SIZE or down > SIZE:
        print("  ! the subject alone is bigger than the whole 16x16 canvas. Detail below")
        print("    one cell cannot survive; it will read as mush in an inventory slot.")
        print("    Regenerate with the grid rule restated, or force it with --grid.")
    across, down = min(across, SIZE), min(down, SIZE)

    left, top = (SIZE - across) // 2, (SIZE - down) // 2
    out = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    used, filled = collections.Counter(), set()
    for y in range(down):
        for x in range(across):
            colour = band_colour(image, int(x * w / across), int(y * h / down),
                                 int((x + 1) * w / across), int((y + 1) * h / down))
            if not apart(colour, paper, 110):
                continue
            if not args.keep_colours:
                colour = nearest(colour)
            out.putpixel((left + x, top + y), colour + (255,))
            used[colour] += 1
            filled.add((left + x, top + y))

    print("filled cells         : %d of 256   (existing items sit at 117-154)" % len(filled))
    print("colours              : %d          (existing items use 4-8)" % len(used))
    for colour, count in used.most_common():
        print("    #%02X%02X%02X x%d" % (colour + (count,)))

    if len(used) > 8:
        print("  ! more colours than any existing item: the model shaded inside its own")
        print("    cells. Restate 'flat colour only, at most 6 colours'.")
    if filled:
        biggest = largest_blob(filled)
        strays = len(filled) - biggest
        if strays > 4:
            print("  ! %d cells are not attached to the main shape. Loose swatches or a"
                  % strays)
            print("    grid ruled outside the sprite. Crop with --crop, or regenerate.")
        box = out.getchannel("A").getbbox()
        print("silhouette           : %d x %d at %s" % (box[2] - box[0], box[3] - box[1], box[:2]))
        if box[2] - box[0] > 14 or box[3] - box[1] > 16:
            print("  ! wider or taller than any existing item; it will crowd the slot.")

    if args.dry:
        print("\n--dry: nothing written")
        return
    target = os.path.normpath(os.path.join(OUT_DIR, args.name + ".png"))
    out.save(target)
    print("\nwritten: %s" % target)


if __name__ == "__main__":
    main()
