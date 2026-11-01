package com.claudemods.triage.event;

import com.claudemods.triage.Config;
import com.claudemods.triage.TriageMod;
import com.claudemods.triage.body.Body;
import com.claudemods.triage.body.BodyHealth;
import com.claudemods.triage.body.PartEffects;
import net.minecraft.world.entity.player.Player;
import com.claudemods.triage.event.PlayerEvent;

/**
 * A broken arm swings a pickaxe slowly.
 *
 * <p>1.20.1 has no attribute for how fast a block comes apart, and the first pass at this
 * port took that as the answer: the arm cost swing speed twice and the difference was
 * written off as one line the two builds would never agree on.
 *
 * <p>That was giving up early. Forge asks, every time anybody starts digging, how fast it
 * should go - and answering a question is a better place to put this than an attribute
 * was, because it can look at the body as it is at that moment rather than at a number
 * somebody remembered to keep up to date.
 *
 * <p>So this build does not have a worse version of that rule. It has the rule, arrived
 * at from the other end.
 */
public final class BreakSpeed {
    private BreakSpeed() {
    }

    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        if (!Config.values().effects || !Body.tracks(player)) {
            return;
        }
        BodyHealth body = Body.of(player);
        // The same reading the other side takes off attack speed, and deliberately the
        // same one. This used to be a second opinion written only for this build - it
        // averaged the two arms, where every other rule in the mod takes the worse of
        // them - so one dead arm cost half here and all of it on 1.21. Two builds of one
        // mod disagreeing about what a broken arm is worth is exactly what the port is
        // meant not to do.
        float arms = PartEffects.arms(body);
        if (arms <= 0.0F) {
            return;
        }
        // The same 0.6 the other line gives the attribute, so a broken arm is worth the
        // same on both sides of the port.
        event.setNewSpeed(event.getOriginalSpeed() * (1.0F - 0.6F * arms));
    }
}
