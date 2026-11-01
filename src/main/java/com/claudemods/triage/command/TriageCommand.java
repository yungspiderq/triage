package com.claudemods.triage.command;

import com.claudemods.triage.Config;
import com.claudemods.triage.TriageMod;
import com.claudemods.triage.body.BloodType;
import com.claudemods.triage.body.Allergy;
import com.claudemods.triage.body.Body;
import com.claudemods.triage.body.BodyHealth;
import com.claudemods.triage.body.BodyPart;
import com.claudemods.triage.body.Complaint;
import com.claudemods.triage.body.Disease;
import com.claudemods.triage.body.Vigil;
import com.claudemods.triage.body.Adrenaline;
import com.claudemods.triage.body.Fatigue;
import com.claudemods.triage.body.Downed;
import com.claudemods.triage.body.Faint;
import com.claudemods.triage.body.Injuries;
import com.claudemods.triage.body.ModDamageTypes;
import com.claudemods.triage.body.Organ;
import com.claudemods.triage.body.PartEffects;
import com.claudemods.triage.body.Burns;
import com.claudemods.triage.body.Pain;
import com.claudemods.triage.body.PartState;
import net.minecraft.world.item.ItemStack;
import com.claudemods.triage.body.Stuck;
import com.claudemods.triage.body.Breathing;
import com.claudemods.triage.body.BulletKinds;
import com.claudemods.triage.body.Pulse;
import com.claudemods.triage.body.Temperature;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.Arrays;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import org.jetbrains.annotations.Nullable;

/** {@code /triage} - reading a body, and breaking one on purpose to test with. */
public final class TriageCommand {
    private TriageCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Looking at your own body is not a cheat; everything that changes one is.
        LiteralCommandNode<CommandSourceStack> root = dispatcher.register(Commands.literal("triage")
                .then(Commands.literal("info")
                        .executes(context -> info(context.getSource())))
                .then(Commands.literal("hurt")
                        .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(part().then(Commands.argument("amount", FloatArgumentType.floatArg(0.0F, 10000.0F))
                                .executes(context -> hurt(context.getSource(),
                                        StringArgumentType.getString(context, "part"),
                                        FloatArgumentType.getFloat(context, "amount"))))))
                .then(Commands.literal("heal")
                        .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(part().then(Commands.argument("amount", FloatArgumentType.floatArg(0.0F, 10000.0F))
                                .executes(context -> heal(context.getSource(),
                                        StringArgumentType.getString(context, "part"),
                                        FloatArgumentType.getFloat(context, "amount"))))))
                .then(Commands.literal("bleed")
                        .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(part().then(Commands.argument("level", IntegerArgumentType.integer(0, 3))
                                .executes(context -> bleed(context.getSource(),
                                        StringArgumentType.getString(context, "part"),
                                        IntegerArgumentType.getInteger(context, "level"), -1))
                                .then(Commands.argument("seconds", IntegerArgumentType.integer(0, 100000))
                                        .executes(context -> bleed(context.getSource(),
                                                StringArgumentType.getString(context, "part"),
                                                IntegerArgumentType.getInteger(context, "level"),
                                                IntegerArgumentType.getInteger(context, "seconds")))))))
                .then(Commands.literal("fracture")
                        .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(part().then(Commands.argument("breaks", IntegerArgumentType.integer(0, 8))
                                .executes(context -> fracture(context.getSource(),
                                        StringArgumentType.getString(context, "part"),
                                        IntegerArgumentType.getInteger(context, "breaks"))))))
                .then(Commands.literal("crack")
                        .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(part().then(Commands.argument("cracked", BoolArgumentType.bool())
                                .executes(context -> crack(context.getSource(),
                                        StringArgumentType.getString(context, "part"),
                                        BoolArgumentType.getBool(context, "cracked"))))))
                .then(Commands.literal("infect")
                        .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(part().then(Commands.argument("level", IntegerArgumentType.integer(0, 2))
                                .executes(context -> infect(context.getSource(),
                                        StringArgumentType.getString(context, "part"),
                                        IntegerArgumentType.getInteger(context, "level"))))))
                .then(Commands.literal("burn")
                        .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(part().then(Commands.argument("depth", IntegerArgumentType.integer(0, 3))
                                .executes(context -> burn(context.getSource(),
                                        StringArgumentType.getString(context, "part"),
                                        IntegerArgumentType.getInteger(context, "depth"))))))
                .then(Commands.literal("pneumo")
                        .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(part().then(Commands.argument("crushed", BoolArgumentType.bool())
                                .executes(context -> pneumo(context.getSource(),
                                        StringArgumentType.getString(context, "part"),
                                        BoolArgumentType.getBool(context, "crushed"))))))
                .then(Commands.literal("disease")
                        .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("disease", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        Arrays.stream(Disease.VALUES).map(Disease::id), builder))
                                .then(Commands.argument("ill", BoolArgumentType.bool())
                                        .executes(context -> disease(context.getSource(),
                                                StringArgumentType.getString(context, "disease"),
                                                BoolArgumentType.getBool(context, "ill"))))))
                .then(Commands.literal("painkiller")
                        .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(0, 100000))
                                .executes(context -> painkiller(context.getSource(),
                                        IntegerArgumentType.getInteger(context, "seconds")))))
                .then(Commands.literal("arrow")
                        .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(part().then(Commands.argument("count", IntegerArgumentType.integer(0, 8))
                                .executes(context -> arrow(context.getSource(),
                                        StringArgumentType.getString(context, "part"),
                                        IntegerArgumentType.getInteger(context, "count"), false))
                                // There are two kinds of thing that stay in somebody now,
                                // and they are a different job to take out. A readout
                                // that can only make one of them can only test one.
                                .then(Commands.argument("shrapnel", BoolArgumentType.bool())
                                        .executes(context -> arrow(context.getSource(),
                                                StringArgumentType.getString(context, "part"),
                                                IntegerArgumentType.getInteger(context, "count"),
                                                BoolArgumentType.getBool(context, "shrapnel")))))))
                // The other thing that stays in, and the one that needs forceps rather
                // than a steady pull. Kept apart from the arrow command because the
                // whole point of the mechanic is that the two are not the same job.
                .then(Commands.literal("bullet")
                        .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(part().then(Commands.argument("count", IntegerArgumentType.integer(0, 8))
                                .executes(context -> bullet(context.getSource(),
                                        StringArgumentType.getString(context, "part"),
                                        IntegerArgumentType.getInteger(context, "count"), null))
                                // Which round it was is a property of the wound, not of the
                                // count, so the type is optional and comes after it: an old
                                // command line still parses and still means the generic round.
                                .then(Commands.argument("kind", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                BulletKinds.suggestions(), builder))
                                        .executes(context -> bullet(context.getSource(),
                                                StringArgumentType.getString(context, "part"),
                                                IntegerArgumentType.getInteger(context, "count"),
                                                StringArgumentType.getString(context, "kind")))))))
                // What is in your hand, into the part you name. The odds on this in a
                // fight are deliberately long, which makes it the one injury in the mod
                // nobody could reliably see happen without a way to ask for it.
                .then(Commands.literal("blade")
                        .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(part().executes(context -> blade(context.getSource(),
                                StringArgumentType.getString(context, "part")))))
                .then(Commands.literal("press")
                        .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(part().then(Commands.argument("seconds", IntegerArgumentType.integer(0, 100000))
                                .executes(context -> press(context.getSource(),
                                        StringArgumentType.getString(context, "part"),
                                        IntegerArgumentType.getInteger(context, "seconds"))))))
                .then(Commands.literal("airway")
                        .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("blocked", BoolArgumentType.bool())
                                .executes(context -> airway(context.getSource(),
                                        BoolArgumentType.getBool(context, "blocked")))))
                .then(Commands.literal("arrest")
                        .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(0, 100000))
                                .executes(context -> arrest(context.getSource(),
                                        IntegerArgumentType.getInteger(context, "seconds")))))
                .then(Commands.literal("splint")
                        .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(part().then(Commands.argument("seconds", IntegerArgumentType.integer(0, 100000))
                                .executes(context -> splint(context.getSource(),
                                        StringArgumentType.getString(context, "part"),
                                        IntegerArgumentType.getInteger(context, "seconds"))))))
                .then(Commands.literal("stiff")
                        .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(part().then(Commands.argument("percent", IntegerArgumentType.integer(0, 100))
                                .executes(context -> stiff(context.getSource(),
                                        StringArgumentType.getString(context, "part"),
                                        IntegerArgumentType.getInteger(context, "percent"))))))
                .then(Commands.literal("knit")
                        .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(part().then(Commands.argument("times", IntegerArgumentType.integer(0, 20))
                                .executes(context -> knit(context.getSource(),
                                        StringArgumentType.getString(context, "part"),
                                        IntegerArgumentType.getInteger(context, "times"))))))
                .then(Commands.literal("allergy")
                        .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("kind", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        Arrays.stream(Allergy.VALUES).map(Allergy::id), builder))
                                .executes(context -> allergy(context.getSource(),
                                        StringArgumentType.getString(context, "kind")))))
                .then(Commands.literal("clot")
                        .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(part().then(Commands.argument("on", BoolArgumentType.bool())
                                .executes(context -> clot(context.getSource(),
                                        StringArgumentType.getString(context, "part"),
                                        BoolArgumentType.getBool(context, "on"))))))
                .then(Commands.literal("displace")
                        .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(part().then(Commands.argument("grade", IntegerArgumentType.integer(0, 2))
                                .executes(context -> displace(context.getSource(),
                                        StringArgumentType.getString(context, "part"),
                                        IntegerArgumentType.getInteger(context, "grade"))))))
                .then(Commands.literal("crooked")
                        .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(part().then(Commands.argument("times", IntegerArgumentType.integer(0, 5))
                                .executes(context -> crooked(context.getSource(),
                                        StringArgumentType.getString(context, "part"),
                                        IntegerArgumentType.getInteger(context, "times"))))))
                .then(Commands.literal("faint")
                        .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(context -> faint(context.getSource())))
                .then(Commands.literal("reset")
                        .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(context -> reset(context.getSource()))));

        dispatcher.register(Commands.literal("tri").redirect(root));
    }



    /** Which of them, by name, so a test can read the inside of somebody out of chat. */
    private static String organs(PartState state, BodyPart part) {
        StringBuilder out = new StringBuilder();
        for (Organ organ : Organ.in(part)) {
            int level = state.organ(organ);
            if (level <= PartState.ORGAN_NONE) {
                continue;
            }
            if (out.length() > 0) {
                out.append(", ");
            }
            out.append(organ.id());
            if (level < PartState.ORGAN_TORN) {
                out.append('?');
            }
        }
        return out.toString();
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> part() {
        return Commands.argument("part", StringArgumentType.word())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                        Arrays.stream(BodyPart.VALUES).map(BodyPart::id), builder));
    }

    private static int info(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("triage.command.player_only"));
            return 0;
        }
        BodyHealth body = Body.of(player);
        source.sendSuccess(() -> Component.translatable("triage.command.header")
                .withStyle(ChatFormatting.GOLD), false);
        // Which of the two this world is playing, because every line under here means a
        // different thing depending on the answer and nothing else on screen says it.
        source.sendSuccess(() -> Component.translatable(Config.values().realistic
                ? "triage.command.realistic" : "triage.command.arcade")
                .withStyle(ChatFormatting.GRAY), false);

        for (BodyPart part : BodyPart.VALUES) {
            PartState state = body.part(part);
            Component line = Component.literal(" ")
                    .append(part.title())
                    .append(Component.literal(": %.1f / %.1f".formatted(state.health, state.max)))
                    .append(state.bleeding()
                            ? Component.translatable(state.arterial() ? "triage.command.tag_artery"
                                    : state.bleed == PartState.BLEED_HEAVY
                                    ? "triage.command.tag_heavy_bleed" : "triage.command.tag_bleed")
                                    .withStyle(state.arterial() ? ChatFormatting.DARK_RED : ChatFormatting.RED)
                            : Component.empty())
                    .append(state.fractured()
                            ? Component.translatable(state.openFracture() ? "triage.command.tag_open"
                                    : state.outOfLine() ? "triage.command.tag_displaced"
                                    : "triage.command.tag_fractured")
                                    .withStyle(state.fractured() && (state.openFracture() || state.outOfLine())
                                            ? ChatFormatting.DARK_RED : ChatFormatting.GOLD)
                            : Component.empty())
                    .append(state.bent()
                            ? Component.translatable("triage.command.tag_crooked").withStyle(ChatFormatting.DARK_GRAY)
                            : Component.empty())
                    .append(state.clotted()
                            ? Component.translatable("triage.command.tag_clot").withStyle(ChatFormatting.DARK_PURPLE)
                            : Component.empty())
                    .append(state.cracked
                            ? Component.translatable("triage.command.tag_cracked").withStyle(ChatFormatting.YELLOW)
                            : Component.empty())
                    .append(state.infected()
                            ? Component.translatable(state.infection >= PartState.INFECTION_SEVERE
                                    ? "triage.command.tag_infected_severe" : "triage.command.tag_infected")
                                    .withStyle(ChatFormatting.DARK_GREEN)
                            : Component.empty())
                    .append(state.burned
                            ? Component.translatable("triage.command.tag_burned",
                                            Burns.depth(state.burn))
                                    .withStyle(Burns.colour(state.burn))
                            : Component.empty())
                    .append(state.tourniquet
                            ? Component.translatable("triage.command.tag_tourniquet").withStyle(ChatFormatting.AQUA)
                            : Component.empty())
                    .append(state.organHurt()
                            ? Component.translatable(state.organTorn()
                                            ? "triage.command.tag_organ" : "triage.command.tag_organ_bruise",
                                    organs(state, part))
                                    .withStyle(ChatFormatting.DARK_RED)
                            : Component.empty())
                    .append(state.pneumo
                            ? Component.translatable("triage.command.tag_pneumo")
                                    .withStyle(ChatFormatting.LIGHT_PURPLE)
                            : Component.empty())
                    .append(state.arrows > 0
                            ? Component.translatable(state.shrapnel
                                    ? "triage.command.tag_shrapnel" : "triage.command.tag_arrow",
                                    state.arrows)
                                    .withStyle(ChatFormatting.YELLOW)
                            : Component.empty())
                    .append(state.bullets > 0
                            ? Component.translatable("triage.command.tag_bullet", state.bullets,
                                    BulletKinds.summary(state.bulletTypes))
                                    .withStyle(ChatFormatting.YELLOW)
                            : Component.empty())
                    .append(state.pressed()
                            ? Component.translatable("triage.command.tag_pressed", state.pressedTicks / 20)
                                    .withStyle(ChatFormatting.WHITE)
                            : Component.empty())
                    // Neither of these is a wound and neither has a number beside it, so
                    // a test that reads the chart out of chat has to be able to see them.
                    .append(state.dressed
                            ? Component.translatable(
                                    state.soiled(Config.values().dressingCleanSeconds * 20)
                                            ? "triage.command.tag_soiled" : "triage.command.tag_dressed",
                                    state.dressedTicks / 20)
                                    .withStyle(ChatFormatting.DARK_GREEN)
                            : Component.empty())
                    .append(state.necrotic()
                            ? Component.translatable("triage.command.tag_dead")
                                    .withStyle(ChatFormatting.DARK_PURPLE)
                            : Component.empty())
                    .append(state.gone()
                            ? Component.translatable(state.prosthetic
                                    ? "triage.command.tag_prosthetic" : "triage.command.tag_missing")
                                    .withStyle(ChatFormatting.GRAY)
                            : Component.empty())
                    // How loose it has gone, next to the fact that there is one on there.
                    // A worn fitting is the one thing about a stump that changes by itself,
                    // so the readout that exists for reading a body has to carry it.
                    .append(state.mending()
                            ? Component.translatable("triage.command.tag_mending", state.mending / 20)
                                    .withStyle(ChatFormatting.AQUA)
                            : Component.empty())
                    .append(state.prosthetic
                            ? Component.translatable("triage.command.tag_worn",
                                    Math.round(state.wornShare(
                                            Config.values().prostheticWearSeconds * 20) * 100.0F))
                                    .withStyle(ChatFormatting.GRAY)
                            : Component.empty())
                    .append(!state.gone() && !state.necrotic() && state.deadTicks > 0
                            ? Component.translatable("triage.command.tag_dying", state.deadTicks / 20)
                                    .withStyle(ChatFormatting.DARK_PURPLE)
                            : Component.empty())
                    .withStyle(state.blacked() ? ChatFormatting.DARK_RED : ChatFormatting.GRAY);
            source.sendSuccess(() -> line, false);
        }
        // Everything a part keeps, on its own line and only when there is any. By this
        // command's own rule - if it is not drawn on the figure it has to be printable -
        // none of the permanent state would be testable otherwise.
        for (BodyPart part : BodyPart.VALUES) {
            PartState state = body.part(part);
            if (!state.marked() && !state.splinted() && !state.displaced && !state.openBreak) {
                continue;
            }
            source.sendSuccess(() -> Component.literal("  ")
                    .append(part.title())
                    .append(Component.literal((" knits=%d crooked=%d cuts=%d burnt=%d septic=%s"
                            + " stiff=%d%% worked=%d splint=%s/%ds displaced=%s open=%s")
                            .formatted(state.knits, state.crooked, state.cuts, state.burnt, state.septic,
                                    Math.round(state.stiff * 100.0F), state.worked,
                                    state.splinted(), state.splintTicks / 20,
                                    state.displaced, state.openBreak)))
                    .withStyle(ChatFormatting.DARK_GRAY), false);
        }
        if (body.fainted()) {
            source.sendSuccess(() -> Component.literal(" ")
                    .append(Component.literal("out cold for %ds".formatted(body.faintTicks() / 20)))
                    .withStyle(ChatFormatting.DARK_RED), false);
        }
        BloodType type = body.bloodType();
        if (type != null) {
            source.sendSuccess(() -> Component.literal(" ")
                    .append(Component.translatable("triage.screen.blood_type", type.title()))
                    .withStyle(ChatFormatting.GRAY), false);
        }
        // The volume and the heart, on one line, because they are one reading: a body
        // holds its pressure up by beating faster as it loses, so half the volume at a
        // steady seventy is a body that has stopped compensating - and that is worse
        // news than the fast one. Neither is drawn on the figure, and by this command's
        // own rule that means a test cannot check either without them being here.
        Config.Values vitalsConfig = Config.values();
        if (vitalsConfig.pulse) {
            // The truth, for the command, and the notes beside it - because this is the
            // one readout in the mod that exists so that a test can see what a screen
            // cannot, and a test that could only see the notes could not check them.
            Pulse reading = Pulse.of(body);
            Pulse noted = body.notedPulse();
            source.sendSuccess(() -> Component.literal(" ")
                    .append(Component.translatable("triage.command.vitals",
                            Math.round(body.bloodLoss() * 100.0F),
                            reading.character() == Pulse.ABSENT
                                    ? Component.translatable("triage.pulse.absent")
                                    : Component.translatable("triage.screen.pulse_value",
                                            reading.rate(), reading.characterTitle())))
                    .append(Component.literal(noted == null ? "  [notes: never taken]"
                            : "  [notes: %d by %s, %ds ago]".formatted(noted.rate(), body.pulseBy(),
                                    Math.max(0L, player.level().getGameTime() - body.pulseAt()) / 20L)))
                    .withStyle(reading.colour()), false);
        }
        if (vitalsConfig.breathing) {
            Breathing air = Breathing.of(body, vitalsConfig.airwaySeconds);
            source.sendSuccess(() -> Component.literal(" ")
                    .append(Component.translatable("triage.command.breath",
                            air.character() == Breathing.ABSENT
                                    ? Component.translatable("triage.breathing.absent")
                                    : Component.translatable("triage.screen.breath_value",
                                            air.rate(), air.characterTitle())))
                    .withStyle(air.colour()), false);
        }
        if (vitalsConfig.temperature) {
            // The word, not the figure. A hot forehead is something a hand reports; a
            // number is something an instrument does, and the mod used to say one anyway.
            Temperature warmth = Temperature.of(body);
            source.sendSuccess(() -> Component.literal(" ")
                    .append(vitalsConfig.thermometer
                            ? Component.translatable("triage.command.temp_felt", warmth.bandTitle())
                            : Component.translatable("triage.command.temp",
                                    warmth.readout(), warmth.bandTitle()))
                    .withStyle(warmth.colour()), false);
        }
        // Only once it is worth saying. Fresh arms print nothing, by the same rule the
        // chest and the forehead follow: a word on this line is always news.
        float tired = Fatigue.of(body);
        if (Fatigue.worthSaying(tired)) {
            source.sendSuccess(() -> Component.literal(" ")
                    .append(Component.translatable("triage.command.arms",
                            Component.translatable("triage.arms." + Fatigue.word(tired))))
                    .withStyle(Fatigue.colour(tired)), false);
        }
        // What they would say if asked, which is the one line here that is an opinion
        // rather than a reading. On your own chart because you know your own: what it
        // costs on somebody else is walking over and asking them.
        int said = Pain.said(body, player.getRandom());
        if (said > 0) {
            source.sendSuccess(() -> Component.literal(" ")
                    .append(Component.translatable("triage.command.pain", said, Pain.WORST,
                            Pain.title(said)))
                    .withStyle(Pain.colour(said)), false);
        }
        // How much of them is burnt, which is the one figure that decides what happens
        // to a burnt person - far more than how deep any single patch went. Printed as a
        // percentage because that is how it is said out loud, and only once there is
        // enough of it to say.
        float burnt = Burns.extent(body);
        if (burnt > 0.0F) {
            int worst = Burns.worst(body);
            source.sendSuccess(() -> Component.literal(" ")
                    .append(Component.translatable("triage.command.burnt",
                            Math.round(burnt * 100.0F), Burns.depth(worst)))
                    .withStyle(Burns.colour(worst)), false);
        }
        // How long since they slept. Not on the figure and not an illness, so the same
        // rule as the arms: nothing at all until it is worth saying.
        float vigil = Vigil.share(player);
        if (Vigil.worthSaying(vigil)) {
            source.sendSuccess(() -> Component.literal(" ")
                    .append(Component.translatable("triage.command.awake", Vigil.days(player)))
                    .append(Component.literal(" (%d%% gone, lids from %d%%)".formatted(
                            Math.round(body.tired() * 100.0F),
                            Math.round(Config.values().sleepyFrom * 100.0F))))
                    .withStyle(Vigil.colour(vigil)), false);
        }

        // What they would say if somebody asked. Not drawn on the figure either, and by
        // this command's own rule that means it has to be readable here or nothing can
        // check it - which matters most for the one answer that points somewhere the
        // injury is not.
        Complaint.Answer complaint = Complaint.ask(body, player.getRandom());
        if (complaint.part() != null) {
            boolean tip = complaint.kind() == Complaint.REFERRED;
            source.sendSuccess(() -> Component.literal(" ")
                    .append(Component.translatable(switch (complaint.severity()) {
                        case Complaint.AGONY -> tip ? "triage.msg.ask_agony_tip" : "triage.msg.ask_agony";
                        case Complaint.SORE -> tip ? "triage.msg.ask_sore_tip" : "triage.msg.ask_sore";
                        default -> tip ? "triage.msg.ask_ache_tip" : "triage.msg.ask_ache";
                    }, complaint.part().title()))
                    .withStyle(ChatFormatting.YELLOW), false);
        }

        // Neither of these is an illness and neither is drawn on the figure, so the same
        // rule applies as to the pulse: if a test cannot read it out of a line here, it
        // cannot check it at all.
        if (Adrenaline.up(body)) {
            source.sendSuccess(() -> Component.literal(" ")
                    .append(Component.translatable("triage.command.adrenaline",
                            body.adrenalineTicks / 20))
                    .withStyle(ChatFormatting.YELLOW), false);
        } else if (Adrenaline.crashing(body)) {
            source.sendSuccess(() -> Component.literal(" ")
                    .append(Component.translatable("triage.command.crash", body.crashTicks / 20))
                    .withStyle(ChatFormatting.DARK_RED), false);
        }
        for (Disease disease : Disease.VALUES) {
            if (body.has(disease)) {
                source.sendSuccess(() -> Component.literal(" ").append(disease.title()).withStyle(disease.colour()), false);
            }
        }
        if (body.under()) {
            source.sendSuccess(() -> Component.translatable("triage.screen.under", body.underTicks() / 20)
                    .withStyle(ChatFormatting.AQUA), false);
        }
        if (body.painkilled()) {
            source.sendSuccess(() -> Component.translatable("triage.hud.painkiller", body.painkillerTicks() / 20)
                    .withStyle(ChatFormatting.AQUA), false);
        }
        // Neither of these is a wound and neither is drawn on the figure, so a test that
        // cannot read them out of a chat line cannot check them at all.
        if (body.downed() && !body.arrested()) {
            source.sendSuccess(() -> Component.translatable("triage.screen.downed", body.downedTicks() / 20)
                    .withStyle(ChatFormatting.RED), false);
        }
        if (body.arrested()) {
            source.sendSuccess(() -> Component.translatable("triage.hud.arrest", body.arrestTicks() / 20)
                    .withStyle(ChatFormatting.DARK_RED), false);
        }
        if (body.downed()) {
            source.sendSuccess(() -> Component.translatable("triage.command.airway", body.airwayTicks() / 20)
                    .withStyle(Config.values().airway && body.airwayBlocked(Config.values().airwaySeconds * 20)
                            ? ChatFormatting.RED : ChatFormatting.GRAY), false);
        }
        return 1;
    }

    private static int hurt(CommandSourceStack source, String partId, float amount) {
        return withPart(source, partId, (player, body, part) -> {
            Injuries.apply(player, body, part, amount, true);
            if (!Body.killIfEmpty(player, ModDamageTypes.PAIN)) {
                after(player, body);
            }
        });
    }

    private static int heal(CommandSourceStack source, String partId, float amount) {
        return withPart(source, partId, (player, body, part) -> {
            // Straight assignment rather than heal(): the command has to be able to bring
            // a blacked-out part back, which ordinary healing deliberately cannot.
            PartState state = body.part(part);
            state.health = Math.min(state.max, state.health + amount);
            body.setDirty(true);
            after(player, body);
        });
    }

    /**
     * @param seconds how long a light bleed runs before it clots, or -1 for one that
     *                never does
     *
     * <p>The duration is worth an argument. Without one this command could only make
     * bleeds that run forever, which meant the one thing about bleeding that has a clock
     * on it - clotting, and everything that interferes with clotting - could not be put
     * in front of a tester at all.
     */
    private static int bleed(CommandSourceStack source, String partId, int level, int seconds) {
        return withPart(source, partId, (player, body, part) -> {
            PartState state = body.part(part);
            state.stopBleeding();
            if (level > 0) {
                state.startBleeding(level, seconds < 0 ? PartState.BLEED_FOREVER : seconds * 20);
            }
            body.setDirty(true);
            after(player, body);
        });
    }

    /**
     * Takes a number rather than a yes or no now, because a chest can be broken in more
     * than one place and a command that could only say "broken" could not put that in
     * front of anybody. Asking for more breaks than the bone has stops at the ceiling.
     */
    private static int fracture(CommandSourceStack source, String partId, int breaks) {
        return withPart(source, partId, (player, body, part) -> {
            PartState state = body.part(part);
            state.mend();
            for (int i = 0; i < breaks; i++) {
                state.fracture(part.bone().count());
            }
            body.setDirty(true);
            after(player, body);
        });
    }

    private static int crack(CommandSourceStack source, String partId, boolean cracked) {
        return withPart(source, partId, (player, body, part) -> {
            if (cracked) {
                body.part(part).crack();
            } else {
                body.part(part).mend();
            }
            body.setDirty(true);
            after(player, body);
        });
    }

    private static int infect(CommandSourceStack source, String partId, int level) {
        return withPart(source, partId, (player, body, part) -> {
            PartState state = body.part(part);
            state.disinfect();
            if (level > 0) {
                state.infect(level);
            }
            body.setDirty(true);
            after(player, body);
        });
    }

    private static int burn(CommandSourceStack source, String partId, int depth) {
        return withPart(source, partId, (player, body, part) -> {
            PartState state = body.part(part);
            if (depth <= PartState.BURN_NONE) {
                state.clearBurn();
            } else {
                state.clearBurn();
                state.burn(depth, 0);
            }
            body.setDirty(true);
            after(player, body);
        });
    }

    /**
     * Takes a part, like the rest of these, even though only a chest can really have it.
     * A testing command that argued with the tester about which limb has a lung in it
     * would be a worse command.
     */
    private static int pneumo(CommandSourceStack source, String partId, boolean crushed) {
        return withPart(source, partId, (player, body, part) -> {
            PartState state = body.part(part);
            if (crushed) {
                state.pneumo = true;
                state.pneumoTicks = 0;
            } else {
                state.decompress();
            }
            body.setDirty(true);
            after(player, body);
        });
    }

    private static int disease(CommandSourceStack source, String diseaseId, boolean ill) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("triage.command.player_only"));
            return 0;
        }
        Disease disease = Disease.byId(diseaseId);
        if (disease == null) {
            source.sendFailure(Component.translatable("triage.command.unknown_disease", diseaseId));
            return 0;
        }
        BodyHealth body = Body.of(player);
        if (ill) {
            body.catchDisease(disease);
        } else {
            body.cure(disease);
        }
        after(player, body);
        return 1;
    }

    private static int painkiller(CommandSourceStack source, int seconds) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("triage.command.player_only"));
            return 0;
        }
        BodyHealth body = Body.of(player);
        body.setPainkillerTicks(seconds * 20);
        after(player, body);
        return 1;
    }

    /** Leaves arrows in a part, or takes them out. */
    private static int arrow(CommandSourceStack source, String partId, int count, boolean shrapnel) {
        return withPart(source, partId, (player, body, part) -> {
            PartState state = body.part(part);
            state.arrows = count;
            state.shrapnel = shrapnel && count > 0;
            body.setDirty(true);
            after(player, body);
        });
    }

    /**
     * Leaves bullets in a part, or takes them out.
     *
     * <p>A named kind is stored as given; an unknown one keeps its id and is drawn as the
     * nearest shape, because a gun pack's calibre is still a calibre. Leaving the type out
     * is the old command and means the generic round.
     */
    private static int bullet(CommandSourceStack source, String partId, int count, @Nullable String kind) {
        return withPart(source, partId, (player, body, part) -> {
            body.part(part).setBullets(count, kind == null ? BulletKinds.UNKNOWN : kind);
            body.setDirty(true);
            after(player, body);
        });
    }

    /**
     * Puts whatever is in your hand into the part you name, or takes back out what is
     * already there.
     */
    private static int blade(CommandSourceStack source, String partId) {
        return withPart(source, partId, (player, body, part) -> {
            PartState state = body.part(part);
            if (state.bladeIn()) {
                Stuck.hand(player, Stuck.take(player, body, part, PartState.BLEED_LIGHT,
                        false, Config.values()));
                after(player, body);
                return;
            }
            ItemStack held = player.getMainHandItem();
            if (held.isEmpty()) {
                return;
            }
            ItemStack lodged = held.copy();
            lodged.setCount(1);
            state.blade = Stuck.pack(lodged, player.server.registryAccess());
            held.shrink(1);
            state.stopBleeding();
            body.setDirty(true);
            after(player, body);
        });
    }

    /** Puts a hand on a wound, or takes it off. */
    private static int press(CommandSourceStack source, String partId, int seconds) {
        return withPart(source, partId, (player, body, part) -> {
            body.part(part).pressedTicks = seconds * 20;
            body.setDirty(true);
            after(player, body);
        });
    }

    /** Blocks or clears an airway, for testing the one condition with nothing to see. */
    private static int airway(CommandSourceStack source, boolean blocked) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("triage.command.player_only"));
            return 0;
        }
        BodyHealth body = Body.of(player);
        if (blocked) {
            body.openAirway();
            body.tickAirway(Config.values().airwaySeconds * 20 + 20);
        } else {
            body.openAirway();
        }
        after(player, body);
        return 1;
    }

    /** Stops the heart, or lets the window run out. 0 clears it. */
    private static int arrest(CommandSourceStack source, int seconds) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("triage.command.player_only"));
            return 0;
        }
        BodyHealth body = Body.of(player);
        body.setArrestTicks(seconds * 20);
        if (seconds == 0) {
            body.clearDowned();
        }
        after(player, body);
        return 1;
    }

    private static int splint(CommandSourceStack source, String partId, int seconds) {
        return withPart(source, partId, (player, body, part) -> {
            PartState state = body.part(part);
            if (seconds <= 0) {
                state.unsplint();
            } else {
                state.setSplint(seconds * 20);
            }
            after(player, body);
        });
    }

    private static int stiff(CommandSourceStack source, String partId, int percent) {
        return withPart(source, partId, (player, body, part) -> {
            body.part(part).stiff = percent / 100.0F;
            after(player, body);
        });
    }

    private static int knit(CommandSourceStack source, String partId, int times) {
        return withPart(source, partId, (player, body, part) -> {
            body.part(part).knits = times;
            after(player, body);
        });
    }

    /** The one fact about a body that nothing else can set, and the tests need it. */
    private static int allergy(CommandSourceStack source, String id) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        Allergy allergy = Allergy.byId(id);
        BodyHealth body = Body.of(player);
        body.setAllergy(allergy);
        Body.sync(player);
        source.sendSuccess(() -> Component.literal("allergy: " + allergy.id()), false);
        return 1;
    }

    /** 0 in line, 1 out of line, 2 through the skin - on a bone that is already broken. */
    private static int clot(CommandSourceStack source, String partId, boolean on) {
        return withPart(source, partId, (player, body, part) -> {
            PartState state = body.part(part);
            if (on) {
                state.clot = true;
                state.clotTicks = 0;
            } else {
                state.clearClot();
            }
            after(player, body);
        });
    }

    private static int displace(CommandSourceStack source, String partId, int grade) {
        return withPart(source, partId, (player, body, part) -> {
            PartState state = body.part(part);
            if (!state.fractured()) {
                state.fracture(part.bone().count());
            }
            state.displaced = grade >= 1;
            state.openBreak = grade >= 2;
            after(player, body);
        });
    }

    private static int crooked(CommandSourceStack source, String partId, int times) {
        return withPart(source, partId, (player, body, part) -> {
            body.part(part).crooked = times;
            // The maximum moves with it, and every path that rebuilds one agrees here.
            body.reconcile();
            after(player, body);
        });
    }

    private static int faint(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        BodyHealth body = Body.of(player);
        Faint.drop(player, body, Config.values());
        source.sendSuccess(() -> Component.literal("out cold"), false);
        return 1;
    }

    private static int reset(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("triage.command.player_only"));
            return 0;
        }
        BodyHealth body = Body.of(player);
        body.resetAll();
        PartEffects.clear(player);
        // The one path that could clear a downing without ending it. Everything else
        // that stands somebody up goes through Downed, and Downed is what lets go of
        // whatever is holding them - a shoulder, or a ragdoll they are not allowed to
        // climb off. Without this, resetting a downed player left them lying in a heap
        // they could not get out of and the mod no longer had a reason to release.
        Downed.release(player);
        after(player, body);
        source.sendSuccess(() -> Component.translatable("triage.command.reset").withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int withPart(CommandSourceStack source, String partId, PartAction action) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("triage.command.player_only"));
            return 0;
        }
        BodyPart part = BodyPart.byId(partId);
        if (part == null) {
            source.sendFailure(Component.translatable("triage.command.unknown_part", partId));
            return 0;
        }
        action.run(player, Body.of(player), part);
        return 1;
    }

    private static void after(ServerPlayer player, BodyHealth body) {
        PartEffects.apply(player, body);
        Body.mirror(player);
        Body.sync(player);
    }

    @FunctionalInterface
    private interface PartAction {
        void run(ServerPlayer player, BodyHealth body, BodyPart part);
    }
}
