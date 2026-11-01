package com.claudemods.triage.block;

import com.claudemods.triage.net.ModNetwork;
import com.claudemods.triage.body.Body;
import com.claudemods.triage.body.BodyHealth;
import com.claudemods.triage.body.Findings;
import com.claudemods.triage.body.BodyPart;
import com.claudemods.triage.net.ShowFilmPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * The machine that sees bone.
 *
 * <p>This mod has spent nine versions making injuries that cannot be seen, and then giving
 * players ways to find them that are all the same way: put your hands on somebody and find
 * out. A pulse, a temperature, pressing on a burn, asking where it hurts. Every one of them
 * is a person examining a person, and every one of them can be wrong.
 *
 * <p>This is the other kind of finding out, and it is the only one in the mod: a picture.
 * It does not ask the patient anything, it cannot be talked out of an answer by morphine or
 * a concussion or somebody being unconscious, and it does not care how steady the hands
 * holding it are. <b>What is on the film is what is in the arm.</b>
 *
 * <p>What it shows is exactly what an X-ray shows, which is less than people expect and
 * more than anything else here manages. Bone, in every state the mod has: sound, cracked,
 * broken, out of line, healed crooked - including the cracks nobody can feel and the ribs
 * and skulls that have no outside to look at. And metal, which is the other thing that
 * stops the beam: every fragment still in somebody, counted.
 *
 * <p>And what it will never show is the half of this mod that is soft. No bleeding, no
 * infection, no clot, no torn spleen. A film of a man dying of a ruptured organ is a
 * perfectly normal film, and that is not a limitation anybody had to invent - it is the
 * single most important thing to know about the instrument.
 */
public class XrayMachine extends Block {
    /** Close enough to stand somebody against the plate. */
    private static final double REACH = 4.0;

    public XrayMachine(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, net.minecraft.world.InteractionHand hand,
                                 BlockHitResult hit) {
        if (level.isClientSide || !(player instanceof ServerPlayer shooter)) {
            return InteractionResult.SUCCESS;
        }
        Player subject = nearest(level, pos, shooter);
        if (!(subject instanceof ServerPlayer patient)) {
            shooter.displayClientMessage(
                    Component.translatable("triage.msg.film_nobody").withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.SUCCESS;
        }
        if (!Body.tracks(patient)) {
            shooter.displayClientMessage(
                    Component.translatable("triage.msg.untracked_them").withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.SUCCESS;
        }
        BodyHealth body = Body.of(patient);
        // Written on the notes: who took the film and when, so that the next person to
        // read the chart knows a picture exists and how old it is.
        body.noteFilm(shooter.getGameProfile().getName(), level.getGameTime());
        // And the film lands on the chart, which it never did. A picture that showed the
        // break to the person holding it and left the form saying "not examined" was the
        // one instrument in the mod whose answer went nowhere - you could photograph
        // somebody's shattered femur and the chart would still be a blank row.
        //
        // Bone and metal, on every part, because that is what one film of a person shows
        // and it is exactly what FilmScreen draws. Nothing else: an X-ray of a man dying
        // of a torn spleen is a perfectly normal X-ray, and that limit is the whole
        // character of the machine.
        long now = level.getGameTime();
        for (BodyPart part : BodyPart.VALUES) {
            body.establish(part, Findings.BONE | Findings.METAL, now);
        }
        Body.sync(patient);

        level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.5F, 1.8F);
        // Through the same redaction as everything else. It used to send the body whole,
        // which meant a film carried organs, infection and clots over the wire - none of
        // which a film can show - and the mode's one rule is that the truth does not leave
        // the server. The bones and the metal survive because they were just established
        // above, which is the correct reason for a fact to be on a client.
        ModNetwork.toPlayer(shooter,
                new ShowFilmPayload(patient.getId(), Body.seen(body, shooter == patient)));
        if (patient != shooter) {
            patient.displayClientMessage(Component.translatable("triage.msg.film_taken_you",
                    shooter.getName()).withStyle(ChatFormatting.GRAY), true);
        }
        return InteractionResult.CONSUME;
    }

    /**
     * Whoever is standing at the plate.
     *
     * <p>The person using the machine counts, and counts last: somebody photographing
     * their own arm has to stand where the film is, and if there is anybody else there
     * they are the patient. Nobody has to be told which - they simply stand somewhere.
     */
    private static @Nullable Player nearest(Level level, BlockPos pos, Player shooter) {
        Player best = null;
        double closest = Double.MAX_VALUE;
        for (Player candidate : level.players()) {
            if (candidate == shooter) {
                continue;
            }
            double away = candidate.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            if (away <= REACH * REACH && away < closest) {
                closest = away;
                best = candidate;
            }
        }
        return best != null ? best : shooter;
    }
}
