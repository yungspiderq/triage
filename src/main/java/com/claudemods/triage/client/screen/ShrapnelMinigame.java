package com.claudemods.triage.client.screen;

import java.util.ArrayList;
import java.util.List;
import com.claudemods.triage.body.BulletKinds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * Taking what a wound is holding out of it, through the hole it made getting in.
 *
 * <p>The version before this drew the wound in section and then put a piece of timing on
 * top of it: hold, and let go inside a window nobody could see. That is a rhythm game
 * wearing a cross-section, and none of it was the job.
 *
 * <p>The job is geometry, and it is the same geometry every time. A fragment or a round
 * tore a channel on the way in, and that channel is the only route back out that does not
 * cut anything new. An instrument goes in <b>where the hole already is</b> - it cannot pass
 * through skin that is not open, which is why the forceps here pivot about the wound and
 * everything else is angle and depth. Then it follows the track down, takes hold, and
 * brings the thing out the way it came.
 *
 * <p>The two things are not drawn alike, because they are not the same problem to look at:
 * a fragment is a ragged piece of casing, and a bullet is a round nose-down in the muscle.
 * The word the screen uses follows the thing in the wound, which is the same rule the
 * chart and the chat messages already keep.
 *
 * <p>Leave the track and you are not removing anything any more, you are making a new
 * wound with a pair of forceps. That is the whole of the failure here, it is visible while
 * it is happening, and it is the reason the tracks are drawn at all.
 *
 * <p>Fingers do the same job worse: they reach what is near the surface and nothing else,
 * they take longer to get hold of anything, and they have less room to be off by. That is
 * the difference a pair of jaws buys, and it is why the item exists.
 */
public final class ShrapnelMinigame implements Minigame {
    /**
     * How far into a limb a finger and thumb can work, as a share of what jaws can.
     *
     * <p>A share of the instrument's reach rather than of the panel, because both of them
     * are measured the same way now: as a radius from the mouth of the wound, which is
     * what pivoting about a hole means.
     */
    private static final float FINGER_REACH = 0.45F;
    /** And how much of what the panel allows a pair of jaws actually covers. */
    private static final float JAW_REACH = 0.98F;
    /** Ticks of holding before the jaws are shut on something. */
    private static final float CLOSE_TICKS = 9.0F;
    private static final float FINGER_CLOSE_TICKS = 16.0F;
    /**
     * How much further than its reach a pair of hands can still take hold of something.
     *
     * <p>The largest fragment plus the tolerance {@link #under} allows, which together
     * are the real reach of any grip. Anything meant to be out of reach has to be past
     * this rather than past the tip.
     */
    private static final float GRIP_CLEAR = 22.0F;
    /** How far off the channel is still the channel. */
    private static final float ON_TRACK = 7.0F;
    private static final float FINGER_ON_TRACK = 4.5F;
    /** Past this much tearing, it came out of a wound the rescue made. */
    private static final float SPOILED = 1.0F;
    /** How wide a slice of skin one rectangle covers, away from the wound itself. */
    private static final int SLICE = 3;

    private static final int SKIN = 0xFFC89571;
    private static final int SKIN_LIT = 0xFFDCAA85;
    private static final int SKIN_EDGE = 0xFF8E5F41;
    private static final int TORN = 0xFFD9A184;
    private static final int FAT = 0xFFE3C98A;
    private static final int[] MUSCLE = {0xFF9B3A33, 0xFF8A312C, 0xFF782A26, 0xFF662320};
    private static final int FIBRE = 0x595A1D1A;
    private static final int OPEN = 0xFF4A0D0A;
    private static final int TRACK = 0xFF5E1612;
    private static final int TORE = 0xFF8E1616;
    private static final int METAL = 0xFFC3C9D0;
    private static final int METAL_LIT = 0xFFE8EEF4;
    private static final int METAL_DARK = 0xFF7C838C;
    private static final int JAW = 0xFFA9B6C0;
    private static final int JAW_DARK = 0xFF5A646E;
    private static final int GOOD = 0xFF7FD46B;
    private static final int BAD = 0xFFCC3333;

    /** What the wound is holding, which decides the shape drawn and the words used. */
    private enum Kind {
        FRAGMENT,
        BULLET,
        MIXED
    }

    /** One piece of what used to be a shell, or one round and what kind it is. */
    private static final class Piece {
        float x;
        float y;
        final float homeX;
        final float homeY;
        final int size;
        final float angle;
        /** Null for a fragment; the round's shape for a bullet. */
        final BulletKinds.Kind kind;
        boolean out;
        boolean spoiled;
        float tearing;

        Piece(float x, float y, int size, float angle, BulletKinds.Kind kind) {
            this.x = x;
            this.y = y;
            this.homeX = x;
            this.homeY = y;
            this.size = size;
            this.angle = angle;
            this.kind = kind;
        }
    }

    private final int left;
    private final int top;
    private final int width;
    private final int height;
    private final boolean forceps;
    private final int skinY;
    private final float mouthX;
    private final float mouthY;
    private final float mouthHalf;
    private final int total;
    private final Kind kind;
    private final List<Piece> pieces = new ArrayList<>();

    private float tipX;
    private float tipY;
    private boolean holding;
    private float closing;
    private Piece held;
    private int cleared;
    private int spoiled;
    private int flash;

    public ShrapnelMinigame(int left, int top, int width, int height, int count, boolean forceps) {
        this(left, top, width, height, count, List.of(), forceps);
    }

    /** Rounds with no name yet: the generic round, one entry per count. */
    private static List<String> generic(int bullets) {
        int count = Math.max(0, bullets);
        List<String> ids = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ids.add(BulletKinds.UNKNOWN);
        }
        return ids;
    }

    /**
     * @param fragments fragments and arrowheads, which are the same job with the jaws
     * @param bullets   rounds under the skin, which are the same job with a different shape
     */
    public ShrapnelMinigame(int left, int top, int width, int height,
                            int fragments, int bullets, boolean forceps) {
        this(left, top, width, height, fragments, generic(bullets), forceps);
    }

    /**
     * @param fragments fragments and arrowheads, which are the same job with the jaws
     * @param bullets   the rounds themselves, one id per round; shape and name follow it
     */
    public ShrapnelMinigame(int left, int top, int width, int height,
                            int fragments, List<String> bullets, boolean forceps) {
        this.left = left;
        this.top = top;
        this.width = Math.max(60, width);
        this.height = Math.max(60, height);
        this.forceps = forceps;
        List<String> rounds = bullets == null ? List.of() : bullets;
        this.kind = fragments > 0 && !rounds.isEmpty() ? Kind.MIXED
                : !rounds.isEmpty() ? Kind.BULLET : Kind.FRAGMENT;
        this.skinY = top + Math.round(this.height * 0.22F);
        this.mouthX = left + this.width * 0.5F;
        this.mouthY = skinY + 1;
        this.mouthHalf = Math.max(12.0F, this.width * 0.07F);

        RandomSource random = Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.getRandom() : RandomSource.create();
        // What is in the wound, one entry per piece. The game draws at most five of them,
        // and when both kinds are present it has to show both - a mixed wound drawn as
        // five fragments would be the screen lying about what is in the patient.
        //
        // A null entry is a fragment; a string is the round it is. The round itself is
        // resolved when it is drawn, so the list belongs to the patient rather than to
        // this screen and a save that predates the names still fills in at the end.
        List<String> types = new ArrayList<>();
        int fragmentPieces = Math.min(5, Math.max(0, fragments));
        int bulletPieces = Math.min(5, Math.max(0, rounds.size()));
        for (int i = 0; i < fragmentPieces; i++) {
            types.add(null);
        }
        for (int i = 0; i < bulletPieces; i++) {
            types.add(rounds.get(i));
        }
        if (types.isEmpty()) {
            types.add(null);
        }
        for (int i = types.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            String swap = types.get(i);
            types.set(i, types.get(j));
            types.set(j, swap);
        }
        this.total = Math.max(1, Math.min(5, types.size()));
        if (fragments > 0 && !rounds.isEmpty() && total >= 2) {
            boolean hasBullet = false;
            boolean hasFragment = false;
            for (int i = 0; i < total; i++) {
                if (types.get(i) != null) {
                    hasBullet = true;
                } else {
                    hasFragment = true;
                }
            }
            if (!hasBullet) {
                types.set(0, rounds.get(0));
            }
            if (!hasFragment) {
                types.set(1, null);
            }
        }
        // Placed in the same geometry they are reached in: an angle below the skin and a
        // distance from the mouth. Doing it in x and y instead is what put fragments
        // inside the panel and outside the circle, where no angle of the jaws reaches.
        float jaws = jawReach();
        float fingers = jaws * FINGER_REACH;
        for (int i = 0; i < total; i++) {
            float spread = total == 1 ? 0.0F
                    : (i - (total - 1) / 2.0F) / ((total - 1) / 2.0F);
            // A fan below the mouth rather than a line across the panel.
            float angle = (float) Math.toRadians(90.0F + spread * 46.0F
                    + (random.nextFloat() - 0.5F) * 14.0F);
            // And the deepest one is always past what a finger can get to, so bare hands
            // can pick at the edges of this and never finish it. That is the whole of
            // what the jaws are for.
            //
            // Past the fingers *and past the grip*, which is where this was wrong. The
            // tip stops at the reach, but closing on something does not need the tip to
            // be on it - a fragment within the width of the jaws counts - so a piece
            // sitting a hair beyond the fingers was still being picked up by them. The
            // gap was five pixels and the grip was fifteen.
            float clear = fingers + GRIP_CLEAR;
            float deepest = Math.max(clear + 2.0F, jaws * 0.95F);
            float depth = i == total - 1
                    ? clear + random.nextFloat() * (deepest - clear)
                    : fingers * 0.5F + random.nextFloat() * (jaws * 0.9F - fingers * 0.5F);
            String type = types.get(i);
            boolean isBullet = type != null;
            // A bullet lies along the track it made - nose first, deeper into the muscle.
            // A fragment has no such axis, so it gets whatever angle the casing broke at.
            float pieceAngle = isBullet ? angle : random.nextFloat() * 3.1F;
            int size = isBullet ? 11 + random.nextInt(3) : 8 + random.nextInt(4);
            pieces.add(new Piece(
                    mouthX + Mth.cos(angle) * depth,
                    mouthY + Mth.sin(angle) * depth,
                    size, pieceAngle, isBullet ? BulletKinds.of(type) : null));
        }
        this.tipX = mouthX;
        this.tipY = mouthY + 20;
    }

    // ---- the hand ---------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || complete()) {
            return false;
        }
        aim(mouseX, mouseY);
        holding = true;
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (button != 0 || complete()) {
            return false;
        }
        aim(mouseX, mouseY);
        return true;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        holding = false;
        closing = 0.0F;
        // Let go and it stays where it was dropped, which is usually worse than where it
        // started. Nothing is lost but the work, which is the right price for letting go.
        held = null;
    }

    /**
     * How far anything can get from the mouth of the wound.
     *
     * <p>The panel is wider than it is tall, and an instrument pivoting about a hole
     * sweeps a circle rather than a rectangle - so the radius that fits is set by the
     * smaller of the two. Both the placing and the reaching ask this, which is the point.
     */
    private float jawReach() {
        // The depth of the panel below the skin, and half its width - whichever the
        // circle runs out of first. The margin is small on purpose: what was being
        // reserved here was room the instrument needed.
        return Math.min(height - (skinY - top) - 8.0F, width * 0.46F) * JAW_REACH;
    }

    /** What this particular pair of hands can reach, jaws or fingers. */
    private float reach() {
        return forceps ? jawReach() : jawReach() * FINGER_REACH;
    }

    /** Where the tip is, clamped to what this pair of hands can reach. */
    private void aim(double mouseX, double mouseY) {
        float dx = (float) mouseX - mouthX;
        float dy = Math.max(3.0F, (float) mouseY - mouthY);
        float reach = reach();
        float length = Mth.sqrt(dx * dx + dy * dy);
        if (length > reach) {
            dx *= reach / length;
            dy *= reach / length;
        }
        tipX = mouthX + dx;
        tipY = mouthY + dy;
    }

    @Override
    public void tick() {
        if (flash > 0) {
            flash--;
        }
        if (complete() || !holding) {
            return;
        }
        if (held == null) {
            grip();
            return;
        }
        // Carrying it. The fragment goes where the jaws go, and what decides everything is
        // whether the jaws are going along the channel it made on the way in.
        held.x = tipX;
        held.y = tipY;
        float off = offTrack(held, tipX, tipY);
        if (off > onTrack()) {
            held.tearing += Math.min(0.05F, (off - onTrack()) * 0.004F);
            if (flash <= 0) {
                flash = 4;
                hiss(SoundEvents.PLAYER_HURT, 1.5F);
            }
        }
        float dx = tipX - mouthX;
        float dy = tipY - mouthY;
        if (Mth.sqrt(dx * dx + dy * dy) < 12.0F) {
            out(held);
        }
    }

    /** Closing on whatever the tip is touching. */
    private void grip() {
        Piece under = under();
        if (under == null) {
            closing = 0.0F;
            return;
        }
        closing += 1.0F;
        if (closing < (forceps ? CLOSE_TICKS : FINGER_CLOSE_TICKS)) {
            return;
        }
        held = under;
        closing = 0.0F;
        hiss(SoundEvents.ITEM_PICKUP, 0.7F);
    }

    private Piece under() {
        for (Piece piece : pieces) {
            if (piece.out) {
                continue;
            }
            float dx = piece.x - tipX;
            float dy = piece.y - tipY;
            if (dx * dx + dy * dy <= (piece.size + 4) * (piece.size + 4)) {
                return piece;
            }
        }
        return null;
    }

    /** How far a point is from the channel this fragment came in along. */
    private float offTrack(Piece piece, float x, float y) {
        float ax = piece.homeX - mouthX;
        float ay = piece.homeY - mouthY;
        float len = ax * ax + ay * ay;
        if (len <= 0.0001F) {
            return 0.0F;
        }
        float t = Mth.clamp(((x - mouthX) * ax + (y - mouthY) * ay) / len, 0.0F, 1.0F);
        float cx = mouthX + ax * t;
        float cy = mouthY + ay * t;
        return Mth.sqrt((x - cx) * (x - cx) + (y - cy) * (y - cy));
    }

    private float onTrack() {
        return forceps ? ON_TRACK : FINGER_ON_TRACK;
    }

    private void out(Piece piece) {
        piece.out = true;
        piece.spoiled = piece.tearing >= SPOILED;
        if (piece.spoiled) {
            spoiled++;
            hiss(SoundEvents.PLAYER_HURT, 0.8F);
        } else {
            cleared++;
            hiss(SoundEvents.ITEM_PICKUP, 1.4F);
        }
        held = null;
        holding = false;
    }

    private void hiss(SoundEvent sound, float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, 0.4F));
    }

    // ---- what it looks like -----------------------------------------------

    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) {
        // Aimed here, every frame, from wherever the hand is.
        //
        // It used to move only while a button was held, which is the wrong half of the
        // gesture: a player moves the mouse first, sees an instrument that does not
        // follow it, and concludes the thing is stuck a centimetre from the wound. It was
        // not stuck - it was waiting to be dragged, and nothing said so.
        //
        // Now the mouse aims it and the button closes the jaws, which is the same
        // division of labour every other pointing device in the game uses.
        if (!complete()) {
            aim(mouseX, mouseY);
        }
        flesh(graphics);
        limit(graphics);
        for (Piece piece : pieces) {
            if (!piece.out) {
                track(graphics, piece);
            }
        }
        for (Piece piece : pieces) {
            if (!piece.out) {
                if (piece.kind != null) {
                    bullet(graphics, piece);
                } else {
                    fragment(graphics, piece);
                }
            }
        }
        instrument(graphics);
        say(graphics, font);
    }

    /**
     * How far this pair of hands goes, drawn.
     *
     * <p>A rule that stops the job should never be a surprise, and this one was: the tip
     * simply refused to go further with nothing on screen to say why. It is a faint arc
     * rather than a wall because that is what it is - the sweep of something pivoting
     * about a hole - and with fingers it is visibly, humiliatingly smaller.
     */
    private void limit(GuiGraphics graphics) {
        float reach = reach();
        int colour = forceps ? 0x33FFFFFF : 0x44FF8080;
        for (int i = 0; i <= 26; i++) {
            // Only the half below the skin: there is nothing above it to reach into.
            float a = Mth.PI * (i / 26.0F);
            int x = Math.round(mouthX + Mth.cos(a) * reach);
            int y = Math.round(mouthY + Mth.sin(a) * reach);
            if (y <= skinY || i % 2 == 0) {
                continue;
            }
            graphics.fill(x, y, x + 2, y + 2, colour);
        }
    }

    private void flesh(GuiGraphics graphics) {
        graphics.fill(left, skinY, left + width, skinY + 6, FAT);
        int band = Math.max(1, (top + height - skinY - 6) / MUSCLE.length);
        for (int i = 0; i < MUSCLE.length; i++) {
            int y1 = i == MUSCLE.length - 1 ? top + height : skinY + 6 + (i + 1) * band;
            graphics.fill(left, skinY + 6 + i * band, left + width, y1, MUSCLE[i]);
        }
        // Fibres, as a few short strokes rather than a quad per pixel. The first pass
        // walked every pixel of the wound and cost four thousand draw calls a frame.
        for (int x = left + 4; x < left + width; x += 11) {
            for (int y = skinY + 10; y < top + height; y += 17) {
                int fx = x + ((y / 5) % 4);
                graphics.fill(fx, y, fx + 1, y + 7, FIBRE);
            }
        }

        for (int x = left; x < left + width; x += Math.abs(x - mouthX) < mouthHalf + 3 ? 1 : SLICE) {
            int x1 = Math.min(left + width, x + (Math.abs(x - mouthX) < mouthHalf + 3 ? 1 : SLICE));
            float d = (x - mouthX) / mouthHalf;
            if (Math.abs(d) < 1.0F) {
                // A tear rather than a cut. The lip curls back and nothing about its edge
                // is straight, which is the one thing that tells the two apart.
                int jag = Math.floorMod(x * 7919, 5) - 2;
                int curl = Math.round(6.0F * (float) Math.pow(Math.abs(d), 1.6)) + jag;
                int lipTop = skinY - 8;
                if (curl > 0) {
                    graphics.fill(x, lipTop, x1, lipTop + curl, TORN);
                    graphics.fill(x, lipTop + curl - 1, x1, lipTop + curl + 1, SKIN_EDGE);
                }
                graphics.fill(x, lipTop + Math.max(0, curl), x1, skinY + 5, OPEN);
            } else {
                graphics.fill(x, skinY - 8, x1, skinY, SKIN_LIT);
                graphics.fill(x, skinY, x1, skinY + 4, SKIN);
                graphics.fill(x, skinY + 3, x1, skinY + 5, SKIN_EDGE);
            }
        }
    }

    /** The channel it tore coming in, which is the way back out. */
    private void track(GuiGraphics graphics, Piece piece) {
        float dx = piece.homeX - mouthX;
        float dy = piece.homeY - mouthY;
        int steps = Math.max(1, Math.round(Mth.sqrt(dx * dx + dy * dy)) / 3);
        int colour = piece.tearing > 0.0F ? TORE : TRACK;
        for (int i = 0; i < steps; i++) {
            float t = i / (float) steps;
            int x = Math.round(mouthX + dx * t + Mth.sin(t * 7.0F + piece.homeX) * 1.6F);
            int y = Math.round(mouthY + dy * t);
            graphics.fill(x - 2, y, x + 3, y + 4, colour);
        }
    }

    /**
     * A piece of metal: a ragged outline filled a row at a time, with a lit edge.
     *
     * <p>Nine corners at uneven radii, which is enough to stop it reading as a gem. Drawn
     * as a polygon rather than by walking a diamond pixel by pixel, because that walk was
     * two hundred draw calls per fragment and there can be five of them.
     */
    private void fragment(GuiGraphics graphics, Piece piece) {
        int n = 9;
        float[] xs = new float[n];
        float[] ys = new float[n];
        for (int k = 0; k < n; k++) {
            float a = piece.angle + k * 2.0F * Mth.PI / n;
            float r = piece.size * (0.62F + 0.55F * (Math.floorMod(k * 7 + (int) piece.homeX, 5) / 4.0F));
            xs[k] = piece.x + Mth.cos(a) * r;
            ys[k] = piece.y + Mth.sin(a) * r;
        }
        Shapes.polygon(graphics, xs, ys, METAL);
        // The lit edge, along the two corners facing the light.
        float[] lx = {xs[0], xs[1], xs[2], piece.x};
        float[] ly = {ys[0], ys[1], ys[2], piece.y};
        Shapes.polygon(graphics, lx, ly, METAL_LIT);
        float[] dxs = {xs[4], xs[5], xs[6], piece.x};
        float[] dys = {ys[4], ys[5], ys[6], piece.y};
        Shapes.polygon(graphics, dxs, dys, METAL_DARK);
    }

    /**
     * A round, lying nose-down along the track it made.
     *
     * <p>A bullet is not a wedge. It is full width along the bearing surface, then turns
     * over to the nose, and that profile is what makes it read as a round rather than a
     * pencil. A driving band sits just above the flat base, which is the one bit of
     * colour on it and the thing that separates it from a pill in a wound.
     *
     * <p>Every round is drawn from its own measurements now. A 9mm pistol round and a
     * 7.62 rifle round are both cylinders with a nose, but one ends in a hemisphere and
     * the other in a long ogive, and a shotgun pellet is neither. The shape is filled a
     * scanline at a time like everything else here.
     */
    private void bullet(GuiGraphics graphics, Piece piece) {
        BulletKinds.Kind kind = piece.kind;
        if (kind.nose() == BulletKinds.Nose.PELLET) {
            pellet(graphics, piece, kind);
            return;
        }
        float ax = Mth.cos(piece.angle);
        float ay = Mth.sin(piece.angle);
        float px = -ay;
        float py = ax;
        // Longer than it is wide, unlike a fragment, and nose first into the muscle.
        float length = piece.size * kind.length();
        float half = piece.size * kind.half();
        float baseX = piece.x - ax * length * 0.5F;
        float baseY = piece.y - ay * length * 0.5F;
        // Width at points along the round, base to nose. The straight run is the bearing
        // surface; the shrinking tail is the nose, which is why there are more than two
        // points on each side.
        float[] along = profileAlong(kind.nose());
        float[] width = profileWidth(kind.nose());
        int n = along.length;
        float[] xs = new float[n * 2 - 1];
        float[] ys = new float[n * 2 - 1];
        for (int i = 0; i < n; i++) {
            float x = baseX + ax * length * along[i];
            float y = baseY + ay * length * along[i];
            xs[i] = x + px * half * width[i];
            ys[i] = y + py * half * width[i];
        }
        for (int i = 1; i < n; i++) {
            int at = n - 1 - i;
            float x = baseX + ax * length * along[at];
            float y = baseY + ay * length * along[at];
            xs[n + i - 1] = x - px * half * width[at];
            ys[n + i - 1] = y - py * half * width[at];
        }
        int body = kind.jacket();
        int lit = shade(body, 1.22F);
        int dark = shade(body, 0.62F);
        Shapes.polygon(graphics, xs, ys, body);
        // The lit face and the shadowed one, along the length of the round.
        Shapes.bar(graphics, baseX + ax * 1.5F + px * half * 0.45F,
                baseY + ay * 1.5F + py * half * 0.45F, ax, ay,
                length * 0.80F, half * 0.20F, lit);
        Shapes.bar(graphics, baseX + ax * 1.5F - px * half * 0.48F,
                baseY + ay * 1.5F - py * half * 0.48F, ax, ay,
                length * 0.80F, half * 0.17F, dark);
        // The driving band, close to the base and a hair proud of the jacket. Not every
        // round has one - an AP round is a steel penetrator in a sleeve, and shot is a
        // pellet - so this is the kind's decision rather than the drawing's.
        if (kind.banded()) {
            float bandX = baseX + ax * length * 0.12F - px * half * 1.02F;
            float bandY = baseY + ay * length * 0.12F - py * half * 1.02F;
            Shapes.bar(graphics, bandX, bandY, px, py, half * 2.04F, 0.62F, kind.band());
            Shapes.bar(graphics, bandX + ax * 0.55F, bandY + ay * 0.55F,
                    px, py, half * 1.30F, 0.30F, shade(kind.band(), 1.18F));
        }
        // The nose itself. A flat round leaves a full-width face; a hollow point leaves a
        // rim with a hole in it; a rocket leaves fins and a nozzle.
        if (kind.nose() == BulletKinds.Nose.FLAT) {
            float noseX = baseX + ax * length;
            float noseY = baseY + ay * length;
            Shapes.bar(graphics, noseX - px * half * 0.86F, noseY - py * half * 0.86F,
                    px, py, half * 1.72F, 0.70F, dark);
        } else if (kind.nose() == BulletKinds.Nose.HOLLOW) {
            cavity(graphics, baseX + ax * length * 0.91F, baseY + ay * length * 0.91F,
                    half * 0.34F, dark);
        }
        if (kind.nose() == BulletKinds.Nose.ROCKET) {
            float finX = baseX + ax * length * 0.08F;
            float finY = baseY + ay * length * 0.08F;
            Shapes.bar(graphics, finX + px * half * 0.95F, finY + py * half * 0.95F,
                    ax, ay, length * 0.22F, half * 0.40F, dark);
            Shapes.bar(graphics, finX - px * half * 0.95F, finY - py * half * 0.95F,
                    ax, ay, length * 0.22F, half * 0.40F, dark);
        }
        // And the flat base, in shadow.
        Shapes.bar(graphics, baseX - px * half * 0.98F, baseY - py * half * 0.98F,
                px, py, half * 1.96F, 0.55F, dark);
    }

    /**
     * The profile from base to nose, as fractions of the round's length. The arrays are
     * paired with {@link #profileWidth(BulletKinds.Nose)}; a flat nose ends on a finite
     * width because it has a face, while a pointed one ends at zero.
     */
    private static float[] profileAlong(BulletKinds.Nose nose) {
        return switch (nose) {
            case SPITZER, ROCKET -> new float[]{0.00F, 0.10F, 0.55F, 0.63F, 0.73F, 0.83F, 0.93F, 1.00F};
            case ROUND -> new float[]{0.00F, 0.10F, 0.55F, 0.68F, 0.78F, 0.87F, 0.95F, 1.00F};
            case HOLLOW -> new float[]{0.00F, 0.10F, 0.55F, 0.68F, 0.78F, 0.87F, 0.95F, 1.00F};
            case FLAT -> new float[]{0.00F, 0.10F, 0.72F, 0.88F, 1.00F};
            default -> new float[]{0.00F, 1.00F};
        };
    }

    private static float[] profileWidth(BulletKinds.Nose nose) {
        return switch (nose) {
            case SPITZER, ROCKET -> new float[]{0.92F, 0.99F, 0.99F, 0.94F, 0.84F, 0.66F, 0.38F, 0.00F};
            case ROUND -> new float[]{0.92F, 0.99F, 0.99F, 0.97F, 0.92F, 0.82F, 0.60F, 0.00F};
            case HOLLOW -> new float[]{0.92F, 0.99F, 0.99F, 0.97F, 0.92F, 0.84F, 0.72F, 0.58F};
            case FLAT -> new float[]{0.92F, 0.99F, 0.99F, 0.96F, 0.86F};
            default -> new float[]{0.92F, 0.92F};
        };
    }

    /** One pellet of shot: a sphere, not a little bullet. */
    private void pellet(GuiGraphics graphics, Piece piece, BulletKinds.Kind kind) {
        float radius = Math.max(2.5F, piece.size * kind.half());
        int body = kind.jacket();
        circle(graphics, piece.x, piece.y, radius, body);
        circle(graphics, piece.x - radius * 0.28F, piece.y - radius * 0.28F,
                radius * 0.42F, shade(body, 1.30F));
        circle(graphics, piece.x + radius * 0.22F, piece.y + radius * 0.22F,
                radius * 0.28F, shade(body, 0.62F));
    }

    /** A dark hole at the nose, for a hollow point. */
    private static void cavity(GuiGraphics graphics, float x, float y, float radius, int colour) {
        circle(graphics, x, y, Math.max(1.5F, radius), colour);
    }

    private static void circle(GuiGraphics graphics, float x, float y, float radius, int colour) {
        int n = 10;
        float[] xs = new float[n];
        float[] ys = new float[n];
        for (int i = 0; i < n; i++) {
            float a = (float) (Math.PI * 2.0 * i / n);
            xs[i] = x + Mth.cos(a) * radius;
            ys[i] = y + Mth.sin(a) * radius;
        }
        Shapes.polygon(graphics, xs, ys, colour);
    }

    /** A lighter or darker version of a packed ARGB colour. */
    private static int shade(int argb, float factor) {
        int a = (argb >>> 24) & 0xFF;
        int r = Math.min(255, Math.round(((argb >> 16) & 0xFF) * factor));
        int g = Math.min(255, Math.round(((argb >> 8) & 0xFF) * factor));
        int b = Math.min(255, Math.round((argb & 0xFF) * factor));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /** The jaws, pivoting about the one hole there is to work through. */
    private void instrument(GuiGraphics graphics) {
        float dx = tipX - mouthX;
        float dy = tipY - mouthY;
        float length = Math.max(1.0F, Mth.sqrt(dx * dx + dy * dy));
        float ax = dx / length;
        float ay = dy / length;
        float px = -ay;
        float py = ax;
        float shut = held != null ? 1.0F
                : closing / (forceps ? CLOSE_TICKS : FINGER_CLOSE_TICKS);
        float spread = 5.5F - 3.5F * Mth.clamp(shut, 0.0F, 1.0F);

        for (int side = -1; side <= 1; side += 2) {
            // The shaft, as two straight runs rather than a walk: from the wound to where
            // the splay starts, and from there to the tip.
            float bendAt = length * 0.55F;
            float offEnd = side * spread;
            Shapes.bar(graphics, mouthX, mouthY, ax, ay, bendAt, 1.5F, JAW);
            float bx = mouthX + ax * bendAt;
            float by = mouthY + ay * bendAt;
            float ex = mouthX + ax * length + px * offEnd;
            float ey = mouthY + ay * length + py * offEnd;
            float sx = ex - bx;
            float sy = ey - by;
            float slen = Math.max(1.0F, Mth.sqrt(sx * sx + sy * sy));
            Shapes.bar(graphics, bx, by, sx / slen, sy / slen, slen, 1.5F, JAW);
            // The jaw itself, thicker and darker at the very end.
            Shapes.bar(graphics, ex - sx / slen * 8.0F, ey - sy / slen * 8.0F,
                    sx / slen, sy / slen, 8.0F, 2.0F, JAW_DARK);
            // And the shank going up out of the wound to whoever is holding it.
            float upx = mouthX + side * 4.0F;
            Shapes.bar(graphics, upx, mouthY, side * 0.16F, -1.0F, skinY - top + 8.0F, 1.5F, JAW);
        }
    }

    private void say(GuiGraphics graphics, Font font) {
        Component note;
        int colour;
        if (complete()) {
            note = Component.translatable(key("done"), cleared, total);
            colour = spoiled > 0 ? BAD : GOOD;
        } else if (held != null) {
            boolean off = offTrack(held, tipX, tipY) > onTrack();
            note = Component.translatable(off ? key("tearing") : key("along"));
            colour = off ? BAD : GOOD;
        } else if (closing > 0.0F) {
            note = Component.translatable(key("closing"));
            colour = 0xFFFFD37F;
        } else {
            note = Component.translatable(key("left"), total - cleared - spoiled);
            colour = 0xFFBFC6CC;
        }
        graphics.drawString(font, note, left + 2, top + height - 9, colour, false);
    }

    /** The words follow what is in the wound, not what this game happens to be called. */
    private String key(String leaf) {
        String prefix = switch (kind) {
            case BULLET -> "bullet_";
            case MIXED -> "mixed_";
            case FRAGMENT -> "shrapnel_";
        };
        return "triage.minigame." + prefix + leaf;
    }

    @Override
    public float progress() {
        return (cleared + spoiled) / (float) Math.max(1, total);
    }

    @Override
    public float quality() {
        return Mth.clamp(cleared / (float) Math.max(1, total), 0.0F, 1.0F);
    }

    @Override
    public boolean complete() {
        for (Piece piece : pieces) {
            if (!piece.out) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean failed() {
        return false;
    }

    @Override
    public Component instruction() {
        if (complete()) {
            if (kind == Kind.BULLET && total > 1) {
                return Component.translatable("triage.minigame.bullet_hint_done_many");
            }
            return Component.translatable(key("hint_done"));
        }
        if (held != null) {
            return Component.translatable(key("hint_out"));
        }
        return Component.translatable(forceps ? key("hint") : key("hint_fingers"));
    }
}
