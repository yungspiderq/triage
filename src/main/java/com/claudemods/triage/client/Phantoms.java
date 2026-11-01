package com.claudemods.triage.client;

import com.claudemods.triage.Config;
import com.claudemods.triage.body.BodyHealth;
import com.claudemods.triage.body.Disease;
import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Somebody who is not there, doing something.
 *
 * <p>Entirely on one client. Nothing is sent to the server, no entity exists in the
 * world, and nobody else can see or hear it - which is not an implementation shortcut
 * but the definition of the thing being modelled. A hallucination other players could
 * see would be a ghost, and this mod does not have ghosts.
 *
 * <p>It is a person rather than a monster and it wears the face of somebody really on
 * the server. Fully formed creatures are what films use; what people describe is a
 * figure at the edge of things, usually familiar, usually busy with something.
 *
 * <p>The first version stood still and vanished the moment you turned towards it, which
 * is the textbook description and was, in a game, invisible. It walks now, and works, and
 * crouches, and follows at a distance it never closes, and sometimes says something with
 * somebody else's name on it. What gives it away is what always gave it away: it does not
 * survive contact. Swing at it and your hand goes through nothing; walk up to it and
 * there is nobody there.
 */
public final class Phantoms {
    /** Ids well outside anything a server will hand out, so nothing collides. */
    private static final int FIRST_ID = -30000;
    /** Close enough to touch, which is close enough to find out. */
    private static final double TOO_NEAR = 4.0;
    /** And the distance a following one will never come inside. */
    private static final double KEEPS_BACK = 8.0;

    private static final int MIN_LIFE = 20 * 25;
    private static final int MAX_LIFE = 20 * 55;
    private static final float WALK_SPEED = 0.052F;
    private static final float FOLLOW_SPEED = 0.075F;
    /** Degrees a body turns in one tick. Snapping round is the thing that reads as fake. */
    private static final float TURN_BODY = 7.0F;
    private static final float TURN_HEAD = 14.0F;

    /** What it is busy with. Only two of them are anything to do with you. */
    private enum Doing {
        STANDING,
        WALKING,
        WORKING,
        LOOKING,
        CROUCHING,
        FOLLOWING,
        WATCHING,
        /**
         * Facing away and not moving, at all, for a long time.
         *
         * <p>The one that does the most with the least. A figure walking about is
         * somebody; a figure standing perfectly still with its back to you for half a
         * minute is the thing people describe when they talk about this, and it costs
         * nothing to draw.
         */
        FACING_AWAY,
        /**
         * Keeping its distance, backwards.
         *
         * <p>It moves when you move and stops when you stop, always the same way off.
         * Walking towards it changes nothing, which is worse than being followed: a
         * thing that follows can at least be got away from.
         */
        BACKING,
        /**
         * Doing what you are doing, a moment late.
         *
         * <p>Stops when you stop, walks when you walk. Nothing about it is aimed at you
         * and that is exactly why it reads - it is not chasing, it is copying.
         */
        MIRRORING
    }

    private static int nextId = FIRST_ID;
    private static @Nullable LivingEntity standing;
    private static @Nullable Component borrowedName;
    private static int life;
    private static int cooldown;
    private static Doing doing = Doing.STANDING;
    private static int untilNext;
    private static Vec3 goal = Vec3.ZERO;
    private static float wantedYaw;
    private static float wantedHeadYaw;
    private static int age;
    private static boolean spoken;

    private Phantoms() {
    }

    /** Whether that is the one that is not there. */
    public static boolean is(Entity entity) {
        return standing != null && entity == standing;
    }

    /** What the player is pointing at, if it happens to be the one that is not there. */
    public static boolean aimedAt(@Nullable HitResult hit) {
        return hit instanceof EntityHitResult entity && is(entity.getEntity());
    }

    /**
     * Somebody swung at it.
     *
     * <p>The one interaction it has, and it is the one that ends it: a hand goes through
     * and there is nothing on the other side. The swing is kept - the arm still moves,
     * because the player really did swing - and the packet that would have told the
     * server about an entity it has never heard of is not sent.
     */
    public static void struck() {
        Minecraft minecraft = Minecraft.getInstance();
        if (standing == null || minecraft.level == null) {
            return;
        }
        // A footstep behind you as it goes, which is the part that stays with people.
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.GRAVEL_STEP, 1.1F, 0.4F));
        remove(minecraft.level);
    }

    /** Once a client tick. */
    public static void tick(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null) {
            forget();
            return;
        }
        if (standing != null) {
            act(minecraft);
            return;
        }
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        BodyHealth body = ClientBody.get();
        if (!body.has(Disease.SCHIZOPHRENIA) || body.medicated()) {
            return;
        }
        int seconds = Config.values().phantomSeconds;
        if (seconds <= 0) {
            return;
        }
        // Twice as often in the stretch after a course has run out. The setting is in
        // seconds between them because that is the number anybody tuning this has an
        // opinion about; a chance per client tick is not.
        float chance = 1.0F / (seconds * 20.0F) * (body.rebounding() ? 2.0F : 1.0F);
        if (minecraft.player.getRandom().nextFloat() < chance) {
            spawn(minecraft);
        }
    }

    /**
     * Whatever it is doing this second.
     *
     * <p>Driven by hand, every tick, because nothing else will do it: a client-side
     * entity has no server behind it deciding where it goes, and mob behaviour never runs
     * on this side at all. None of it is clever - the point is only that it looks
     * occupied, and that it turns its head before its body the way a person does.
     */
    private static void act(Minecraft minecraft) {
        LivingEntity phantom = standing;
        if (phantom == null || minecraft.level == null || minecraft.player == null || !phantom.isAlive()) {
            forget();
            return;
        }
        age++;
        life--;
        if (life <= 0 || phantom.distanceToSqr(minecraft.player) < TOO_NEAR * TOO_NEAR) {
            remove(minecraft.level);
            return;
        }

        RandomSource random = minecraft.player.getRandom();
        if (--untilNext <= 0) {
            decide(minecraft, random);
        }

        float speed = 0.0F;
        switch (doing) {
            case WALKING -> speed = step(minecraft, phantom, goal, WALK_SPEED);
            case BACKING -> speed = backAway(minecraft, phantom);
            case MIRRORING -> speed = mirror(minecraft, phantom);
            // Nothing at all, which is the whole of it. The yaw was set when it was
            // decided and it is not touched again.
            case FACING_AWAY -> speed = 0.0F;
            case FOLLOWING -> {
                Vec3 me = minecraft.player.position();
                wantedHeadYaw = yawTowards(phantom, me);
                if (phantom.distanceToSqr(minecraft.player) > KEEPS_BACK * KEEPS_BACK) {
                    speed = step(minecraft, phantom, me, FOLLOW_SPEED);
                }
            }
            case WORKING -> {
                if (age % 11 == 0) {
                    phantom.swing(InteractionHand.MAIN_HAND);
                    if (random.nextInt(3) == 0) {
                        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(
                                SoundEvents.STONE_HIT, 0.9F + random.nextFloat() * 0.2F, 0.25F));
                    }
                }
            }
            // Looking round at nothing in particular, which is most of what anybody does.
            case LOOKING -> {
                if (age % 25 == 0) {
                    wantedHeadYaw = wantedYaw + (random.nextFloat() - 0.5F) * 150.0F;
                }
            }
            case WATCHING -> wantedHeadYaw = wantedYaw = yawTowards(phantom, minecraft.player.position());
            case CROUCHING, STANDING -> {
                // Nothing. Standing there is a thing people do.
            }
        }

        turn(phantom);
        phantom.walkAnimation.update(speed, 0.4F);
    }

    /**
     * Picks the next thing, and how long to do it for.
     *
     * <p>Weighted away from the two that are about the player. A figure that only ever
     * watches you is a jump scare; one that is mostly getting on with something and
     * occasionally turns round is the thing being described.
     */
    private static void decide(Minecraft minecraft, RandomSource random) {
        untilNext = 50 + random.nextInt(110);
        LivingEntity phantom = standing;
        int roll = random.nextInt(30);
        if (roll < 5) {
            doing = Doing.WALKING;
            double angle = random.nextDouble() * Math.PI * 2.0;
            double distance = 4.0 + random.nextDouble() * 7.0;
            goal = phantom.position().add(Math.cos(angle) * distance, 0.0, Math.sin(angle) * distance);
        } else if (roll < 9) {
            doing = Doing.WORKING;
            wantedYaw = phantom.getYRot() + (random.nextFloat() - 0.5F) * 90.0F;
            wantedHeadYaw = wantedYaw;
        } else if (roll < 12) {
            doing = Doing.LOOKING;
        } else if (roll < 14) {
            doing = Doing.STANDING;
        } else if (roll < 16) {
            doing = Doing.CROUCHING;
        } else if (roll < 18) {
            doing = Doing.FOLLOWING;
        } else if (roll < 23) {
            // Held far longer than anything else. The whole of it is the length.
            doing = Doing.FACING_AWAY;
            untilNext = 140 + random.nextInt(200);
            if (minecraft.player != null) {
                wantedYaw = wantedHeadYaw = yawTowards(phantom, minecraft.player.position()) + 180.0F;
            }
        } else if (roll < 26) {
            doing = Doing.BACKING;
        } else if (roll < 29) {
            doing = Doing.MIRRORING;
        } else {
            doing = Doing.WATCHING;
            // Once, and only if it is a person: a cow does not have anything to say.
            if (!spoken && borrowedName != null && random.nextBoolean()) {
                spoken = true;
                Whispers.saidBy(minecraft, borrowedName);
            }
        }
        crouch(phantom, doing == Doing.CROUCHING);
    }

    /**
     * Keeping the same distance off, whatever anybody does about it.
     *
     * <p>Walking at it does not close the gap. That is the difference between this and
     * being followed, and it is the worse of the two: a thing that follows can be got
     * away from, and a thing that keeps its distance cannot be got to.
     */
    private static float backAway(Minecraft minecraft, LivingEntity phantom) {
        if (minecraft.player == null) {
            return 0.0F;
        }
        Vec3 from = minecraft.player.position();
        Vec3 away = phantom.position().subtract(from);
        double flat = Math.sqrt(away.x * away.x + away.z * away.z);
        if (flat > KEEPS_BACK + 3.0) {
            // Far enough. It waits rather than wandering off over the horizon.
            wantedHeadYaw = wantedYaw = yawTowards(phantom, from) + 180.0F;
            return 0.0F;
        }
        Vec3 target = phantom.position().add(away.x / Math.max(0.1, flat) * 3.0, 0.0,
                away.z / Math.max(0.1, flat) * 3.0);
        float rate = step(minecraft, phantom, target, WALK_SPEED);
        // Its back stays to you the whole time, which is what makes it read as leaving
        // rather than as retreating.
        wantedHeadYaw = wantedYaw = yawTowards(phantom, from) + 180.0F;
        return rate;
    }

    /**
     * Doing what the player is doing, a moment late.
     *
     * <p>Nothing about it is aimed at anybody, and that is why it works: it is not
     * chasing, it is copying, and a copy is harder to explain away than a chase.
     */
    private static float mirror(Minecraft minecraft, LivingEntity phantom) {
        if (minecraft.player == null) {
            return 0.0F;
        }
        Vec3 moving = minecraft.player.getDeltaMovement();
        double speed = Math.sqrt(moving.x * moving.x + moving.z * moving.z);
        if (speed < 0.02) {
            return 0.0F;
        }
        Vec3 target = phantom.position().add(moving.x * 12.0, 0.0, moving.z * 12.0);
        return step(minecraft, phantom, target, WALK_SPEED);
    }

    private static void crouch(LivingEntity phantom, boolean down) {
        if (phantom instanceof RemotePlayer player) {
            player.setShiftKeyDown(down);
            player.setPose(down ? Pose.CROUCHING : Pose.STANDING);
        }
    }

    /**
     * One step towards somewhere, if there is anywhere to put a foot.
     *
     * @return how fast the legs should look like they are going
     */
    private static float step(Minecraft minecraft, LivingEntity phantom, Vec3 target, float rate) {
        Vec3 to = target.subtract(phantom.position());
        double flat = Math.sqrt(to.x * to.x + to.z * to.z);
        if (flat < 0.6) {
            doing = Doing.STANDING;
            return 0.0F;
        }
        double nx = phantom.getX() + to.x / flat * rate;
        double nz = phantom.getZ() + to.z / flat * rate;
        BlockPos ground = footing(minecraft.level, nx, phantom.getY() + 1.0, nz);
        if (ground == null) {
            // Nothing to walk onto. It stops rather than wading into a wall, which is
            // what anybody would look like doing.
            doing = Doing.STANDING;
            return 0.0F;
        }
        phantom.setPos(nx, ground.getY() + 1, nz);
        wantedYaw = wantedHeadYaw = (float) (Mth.atan2(to.z, to.x) * (180.0 / Math.PI)) - 90.0F;
        return 0.85F;
    }

    /**
     * Turns towards where it wants to be looking, a few degrees at a time.
     *
     * <p>The head goes first and faster than the body, which is the one detail that
     * separates a person from a thing being rotated by a program. Snapping straight to
     * an angle was what made the first version read as a prop.
     */
    private static void turn(LivingEntity phantom) {
        float body = Mth.approachDegrees(phantom.getYRot(), wantedYaw, TURN_BODY);
        float head = Mth.approachDegrees(phantom.getYHeadRot(), wantedHeadYaw, TURN_HEAD);
        // A neck only goes so far round before the shoulders have to follow.
        if (Math.abs(Mth.degreesDifference(body, head)) > 55.0F) {
            body = Mth.approachDegrees(body, head, TURN_BODY * 2.0F);
        }
        phantom.setYRot(body);
        phantom.setYBodyRot(body);
        phantom.setYHeadRot(head);
    }

    private static float yawTowards(LivingEntity phantom, Vec3 at) {
        Vec3 to = at.subtract(phantom.position());
        return (float) (Mth.atan2(to.z, to.x) * (180.0 / Math.PI)) - 90.0F;
    }

    private static void spawn(Minecraft minecraft) {
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null || minecraft.getConnection() == null) {
            return;
        }
        Vec3 spot = somewhereOutOfTheWay(minecraft);
        if (spot == null) {
            return;
        }
        LivingEntity phantom = build(minecraft, level);
        if (phantom == null) {
            return;
        }

        phantom.setId(nextId--);
        phantom.setPos(spot.x, spot.y, spot.z);
        phantom.setOldPosAndRot();
        wantedYaw = wantedHeadYaw = yawTowards(phantom, minecraft.player.position());
        phantom.setYRot(wantedYaw);
        phantom.setYBodyRot(wantedYaw);
        phantom.setYHeadRot(wantedYaw);
        level.putNonPlayerEntity(phantom.getId(), phantom);

        standing = phantom;
        age = 0;
        spoken = false;
        untilNext = 1;
        doing = Doing.STANDING;
        life = MIN_LIFE + minecraft.player.getRandom().nextInt(MAX_LIFE - MIN_LIFE);
    }

    /**
     * Usually a person, sometimes whatever else lives out here.
     *
     * <p>Weighted heavily towards people because that is what gets described, and because
     * a familiar face is the version of this that is actually unpleasant. The animals are
     * there so that the thing you thought you saw moving in the trees is sometimes only a
     * cow that was never in the trees.
     */
    private static @Nullable LivingEntity build(Minecraft minecraft, ClientLevel level) {
        RandomSource random = minecraft.player.getRandom();
        borrowedName = null;
        if (random.nextInt(10) < 7) {
            PlayerInfo face = borrowedFace(minecraft);
            if (face == null) {
                return null;
            }
            borrowedName = face.getTabListDisplayName() != null
                    ? face.getTabListDisplayName()
                    : Component.literal(face.getProfile().getName());
            return new Phantom(level, face);
        }
        EntityType<? extends LivingEntity> type = switch (random.nextInt(3)) {
            case 0 -> EntityType.VILLAGER;
            case 1 -> EntityType.COW;
            default -> EntityType.ZOMBIE;
        };
        LivingEntity mob = type.create(level);
        if (mob != null) {
            mob.noPhysics = true;
        }
        return mob;
    }

    /** Somebody who is really here, to borrow a face from. */
    private static @Nullable PlayerInfo borrowedFace(Minecraft minecraft) {
        if (minecraft.getConnection() == null) {
            return null;
        }
        List<PlayerInfo> online = List.copyOf(minecraft.getConnection().getOnlinePlayers());
        return online.isEmpty() ? null
                : online.get(minecraft.player.getRandom().nextInt(online.size()));
    }

    /** A place to appear: out to the side, on something solid, not where you are looking. */
    private static @Nullable Vec3 somewhereOutOfTheWay(Minecraft minecraft) {
        ClientLevel level = minecraft.level;
        Vec3 eye = minecraft.player.getEyePosition();
        for (int attempt = 0; attempt < 16; attempt++) {
            double angle = minecraft.player.getRandom().nextDouble() * Math.PI * 2.0;
            double distance = 12.0 + minecraft.player.getRandom().nextDouble() * 8.0;
            double x = minecraft.player.getX() + Math.cos(angle) * distance;
            double z = minecraft.player.getZ() + Math.sin(angle) * distance;

            // Not in front of the eyes: something that fades in where you are already
            // looking is a rendering fault, not a hallucination.
            Vec3 toward = new Vec3(x - eye.x, 0.0, z - eye.z).normalize();
            if (minecraft.player.getLookAngle().dot(toward) > 0.55) {
                continue;
            }
            BlockPos ground = footing(level, x, minecraft.player.getY(), z);
            if (ground != null) {
                return new Vec3(x, ground.getY() + 1, z);
            }
        }
        return null;
    }

    /**
     * The top of something you could actually stand on.
     *
     * <p>Not merely "not air", which is what this used to ask and is why they turned up
     * standing on the tops of grass tufts with their feet in the air. Grass, flowers and
     * snow layers are all things you walk through, so the question is whether the block
     * would stop you - and then whether there is room above it for somebody to be.
     */
    private static @Nullable BlockPos footing(ClientLevel level, double x, double startY, double z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dy = 2; dy >= -4; dy--) {
            pos.set(Mth.floor(x), Mth.floor(startY) + dy, Mth.floor(z));
            if (!level.isLoaded(pos)) {
                return null;
            }
            if (level.getBlockState(pos).blocksMotion()
                    && !level.getBlockState(pos.above()).blocksMotion()
                    && !level.getBlockState(pos.above(2)).blocksMotion()) {
                return pos.immutable();
            }
        }
        return null;
    }

    private static void remove(ClientLevel level) {
        if (standing != null) {
            level.removeEntity(standing.getId(), Entity.RemovalReason.DISCARDED);
        }
        standing = null;
        borrowedName = null;
        // Long enough that they are a thing that happens rather than a thing that is
        // happening. None of this is meant to be a constant companion.
        cooldown = 20 * 30;
    }

    /** Left the world, or died: nothing to take away and nothing to remember. */
    public static void forget() {
        standing = null;
        borrowedName = null;
        life = 0;
        cooldown = 0;
        nextId = FIRST_ID;
    }

    /**
     * The figure itself: a player that cannot be pushed or bumped into, and can be aimed
     * at exactly once.
     *
     * <p>Pickable on purpose - a swing has to be able to find it, because the swing is
     * how it ends. Not collidable, because walking into somebody who is not there would
     * be the giveaway happening at the wrong moment and for the wrong reason.
     */
    private static final class Phantom extends RemotePlayer {
        private final PlayerInfo face;

        private Phantom(ClientLevel level, PlayerInfo face) {
            super(level, new GameProfile(UUID.randomUUID(), face.getProfile().getName()));
            this.face = face;
            noPhysics = true;
        }

        /**
         * Somebody else's skin, on an id that is nobody's.
         *
         * <p>A client player finds its skin by looking its own uuid up in the player
         * list, so a made-up id gets the default one - which is what the first version
         * did, and it looked like a bug rather than like a person. Reusing the real uuid
         * fixes the skin and breaks something worse: the figure would land in the level's
         * player list under a uuid that already belongs to somebody, and anything asking
         * the world for that player could be handed a hallucination.
         *
         * <p>So the id stays nobody's and the lookup is answered directly. The face is
         * real, the name is real, and there is still no such player.
         */
        @Override
        protected PlayerInfo getPlayerInfo() {
            return face;
        }

        @Override
        public boolean isPushable() {
            return false;
        }

        @Override
        public boolean canBeCollidedWith() {
            return false;
        }
    }
}
