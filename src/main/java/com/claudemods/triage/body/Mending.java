package com.claudemods.triage.body;

import com.claudemods.triage.Config;
import com.claudemods.triage.med.Theatre;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

/**
 * The days after.
 *
 * <p>Everything in this mod has ended at the moment the treatment did. A surgeon closed
 * somebody up and they stood straight back up and went to work - which is the one thing
 * about surgery that nobody who has ever had any believes.
 *
 * <p>So an operated part is not finished when the screen closes. It is knitting, and
 * knitting is the one process in here that nothing can hurry: there is no item for this
 * and there is not going to be one. What there is instead is a choice about what to do
 * with the next minute and a half, and the correct answer is nothing at all.
 *
 * <p>Nothing is taken away while it lasts. A mending arm swings exactly as well as it did
 * an hour ago, because a penalty would turn this into another debuff to wait out and
 * there are enough of those. What it is instead is a <b>risk</b> attached to the part,
 * and it only exists while somebody is using it - which means the player who lies low is
 * not being slowed down, they are being left alone.
 *
 * <p>And it is the second thing the operating theatre pays for. A job done well in a
 * proper room knits in half the time of one done badly on a floor, because most of what a
 * good theatre buys in life is exactly this and not the hour itself.
 */
public final class Mending {
    private Mending() {
    }

    /**
     * Somebody has been inside this part. It now needs leaving alone.
     *
     * <p>Called by the treatments that open a body rather than by all of them, and the
     * length is decided at the moment it starts: how well it went and where it happened
     * are facts about the operation, not about the week after it.
     */
    public static void begins(ServerPlayer patient, BodyHealth body, BodyPart part, float quality) {
        Config.Values config = Config.values();
        if (!config.mending) {
            return;
        }
        PartState state = body.part(part);
        if (state.gone()) {
            // A stump has its own clocks and does not need another one.
            return;
        }
        // Between half and one and a half of the base, by how well it went. The theatre
        // is already inside the quality by the time this is asked, which is the second
        // thing a good room buys and the one people actually notice.
        float worth = Mth.clamp(quality, 0.0F, 1.0F);
        // Mending is mostly something a body does while it is asleep, so a body that is
        // not sleeping is a body doing less of it.
        int ticks = Math.round(config.mendingSeconds * 20 * Mth.lerp(worth, 1.5F, 0.5F)
                * Vigil.mending(patient));
        state.mending = Math.max(state.mending, ticks);
        body.setDirty(true);
        Body.notify(patient, Component.translatable("triage.msg.mending", part.title(), ticks / 20),
                ChatFormatting.AQUA, null);
    }

    /**
     * One second of it.
     *
     * <p>The clock runs whatever anybody is doing; what working the part changes is
     * whether it survives. Resting is not a faster clock, it is the absence of the thing
     * that resets it - and that difference is the whole reason this is not a debuff.
     */
    public static void tick(ServerPlayer player, BodyHealth body, int interval) {
        Config.Values config = Config.values();
        if (!config.mending) {
            return;
        }
        boolean working = worked(player);
        for (BodyPart part : BodyPart.VALUES) {
            PartState state = body.part(part);
            if (!state.mending()) {
                continue;
            }
            state.mending = Math.max(0, state.mending - interval);
            body.setDirty(true);
            if (state.mending == 0) {
                // Closed, and the only moment in the mod where that is true. What grows
                // across the gap is not skin: it is the place the next infection will
                // settle and a little less of the part than there was.
                Scars.closed(player, body, part);
                Body.notify(player, Component.translatable("triage.msg.mended", part.title()),
                        ChatFormatting.GREEN, null);
                continue;
            }
            if (!working || !usedBy(player, part)) {
                continue;
            }
            if (player.getRandom().nextFloat() < config.mendingTearChance) {
                tears(player, body, state, part, config);
            }
        }
    }

    /** Whether they are doing anything hard enough to matter. */
    private static boolean worked(ServerPlayer player) {
        return player.isSprinting() || player.swinging;
    }

    /**
     * Whether this part is one of the ones doing it.
     *
     * <p>Legs for running, arms for swinging, and the trunk for either - because there is
     * no way to sprint or swing that a belly full of stitches is not part of.
     */
    private static boolean usedBy(ServerPlayer player, BodyPart part) {
        return switch (part) {
            case LEFT_LEG, RIGHT_LEG -> player.isSprinting();
            case LEFT_ARM, RIGHT_ARM -> player.swinging;
            case THORAX, STOMACH -> true;
            case HEAD -> false;
        };
    }

    /**
     * Something jolted it that was not work.
     *
     * <p>A sneeze, at present, and a sneeze is the perfect thing for this: a belly closed
     * up an hour ago is held together by exactly what a sneeze is built to defeat. It is
     * the only way in the mod to come open without doing anything at all.
     */
    public static void jolted(ServerPlayer player, BodyHealth body, PartState state, BodyPart part) {
        if (!Config.values().mending || !state.mending()) {
            return;
        }
        tears(player, body, state, part, Config.values());
    }

    /**
     * It comes open.
     *
     * <p>Everything that was true before the operation is true again: it bleeds, the
     * clock starts over, and whoever closed it has to do it a second time - this time on
     * somebody who has already paid for it once.
     */
    private static void tears(ServerPlayer player, BodyHealth body, PartState state, BodyPart part,
                              Config.Values config) {
        state.startBleeding(PartState.BLEED_HEAVY, 0);
        state.mending = 0;
        body.setDirty(true);
        Body.notify(player, Component.translatable("triage.msg.came_open", part.title()),
                ChatFormatting.DARK_RED, SoundEvents.PLAYER_HURT);
    }
}
