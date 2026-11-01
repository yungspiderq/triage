package com.claudemods.triage.body;

import com.claudemods.triage.Config;
import com.claudemods.triage.block.ModBlocks;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

/**
 * Somebody beside a table, being watched.
 *
 * <p>A ward is not a place with better medicine in it - it is a place where somebody
 * writes a number on a page every so often. That is the whole difference between a
 * hospital and a room with a bed in it, and it is the one part of medicine this mod has
 * never had a way to do.
 *
 * <p>So a patient within reach of a medical table gets their pulse and their temperature
 * taken, on a clock, by nobody. It is quiet on purpose: nothing is said in chat and
 * nothing interrupts, the lines simply appear on the observation sheet. The first time a
 * player opens that sheet after ten minutes in a clinic they will find a column of numbers
 * going the wrong way, and that is the mechanic.
 *
 * <p>It is also the first reason in eleven versions to <b>carry somebody somewhere</b>.
 * Until now a clinic was a building with supplies in it, and supplies travel. A place that
 * measures for you does not.
 */
public final class Bedside {
    /** When each body was last written up, by the world clock. */
    private static final Map<UUID, Long> LAST = new HashMap<>();
    /** Whose name goes in the author column. Not a name anybody can have. */
    public static final String BY = "@table";

    private Bedside() {
    }

    public static void tick(ServerPlayer player, BodyHealth body, Config.Values config) {
        if (!config.bedsideObs || config.bedsideSeconds <= 0) {
            return;
        }
        long now = player.level().getGameTime();
        Long last = LAST.get(player.getUUID());
        if (last != null && now - last < config.bedsideSeconds * 20L) {
            return;
        }
        if (!beside(player, config.bedsideRange)) {
            // Not written down and not remembered: somebody who walks past a table has
            // not been observed, and the clock starts when they stop.
            return;
        }
        LAST.put(player.getUUID(), now);
        if (config.pulse) {
            body.notePulse(Pulse.of(body), BY, now);
        }
        if (config.temperature) {
            body.noteTemp(Temperature.of(body).tenths(), BY, now);
        }
        // Nothing is said. A room that announces every observation is a room nobody can
        // think in, and the point of a sheet is that it is there when you look at it.
        Body.sync(player);
    }

    /** Whether there is a table close enough to be standing over them. */
    private static boolean beside(ServerPlayer player, int range) {
        BlockPos at = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(at.offset(-range, -range, -range),
                at.offset(range, range, range))) {
            if (player.level().getBlockState(pos).is(ModBlocks.MEDICAL_TABLE.get())) {
                return true;
            }
        }
        return false;
    }

    public static void forget(UUID playerId) {
        LAST.remove(playerId);
    }
}
