package com.claudemods.triage;

import com.claudemods.triage.med.MedicineKind;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.Nullable;

/**
 * Server-side tuning for the whole mod.
 *
 * <p>Every value is read through {@link #values()} rather than off the spec directly.
 * The spec throws when it is asked for a value before the config has loaded, and this
 * runs in the damage path and once a second per player, so the numbers are baked into
 * plain fields when the config loads and read from there afterwards.
 */
public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.IntValue TETANUS_SECONDS;
    private static final ModConfigSpec.DoubleValue TETANUS_CHANCE;
    private static final ModConfigSpec.IntValue CRUSH_SECONDS;
    private static final ModConfigSpec.IntValue CRUSH_STRAP_SECONDS;
    private static final ModConfigSpec.DoubleValue RELAPSE_CHANCE;

    // ---- difficulty -------------------------------------------------------
    private static final ModConfigSpec.BooleanValue REALISTIC;

    // ---- health -----------------------------------------------------------
    private static final ModConfigSpec.DoubleValue HEAD_MAX;
    private static final ModConfigSpec.DoubleValue THORAX_MAX;
    private static final ModConfigSpec.DoubleValue STOMACH_MAX;
    private static final ModConfigSpec.DoubleValue ARM_MAX;
    private static final ModConfigSpec.DoubleValue LEG_MAX;
    private static final ModConfigSpec.DoubleValue DAMAGE_SCALE;
    private static final ModConfigSpec.DoubleValue HEAL_SCALE;
    private static final ModConfigSpec.BooleanValue REGENERATION;
    private static final ModConfigSpec.DoubleValue OVERFLOW_TO_THORAX;
    private static final ModConfigSpec.DoubleValue BLACKED_DRAIN;
    private static final ModConfigSpec.DoubleValue ONE_SHOT_ABOVE;
    private static final ModConfigSpec.DoubleValue ONE_SHOT_FLOOR;

    // ---- where hits land --------------------------------------------------
    private static final ModConfigSpec.IntValue HEAD_WEIGHT;
    private static final ModConfigSpec.IntValue THORAX_WEIGHT;
    private static final ModConfigSpec.IntValue STOMACH_WEIGHT;
    private static final ModConfigSpec.IntValue ARM_WEIGHT;
    private static final ModConfigSpec.IntValue LEG_WEIGHT;

    // ---- bleeding ---------------------------------------------------------
    private static final ModConfigSpec.DoubleValue LIGHT_BLEED_RATE;
    private static final ModConfigSpec.DoubleValue HEAVY_BLEED_RATE;
    private static final ModConfigSpec.DoubleValue BLEED_CHANCE_PER_DAMAGE;
    private static final ModConfigSpec.DoubleValue HEAVY_BLEED_THRESHOLD;
    private static final ModConfigSpec.IntValue LIGHT_BLEED_SECONDS;
    private static final ModConfigSpec.DoubleValue BLOOD_PER_LIGHT;
    private static final ModConfigSpec.DoubleValue BLOOD_PER_HEAVY;
    private static final ModConfigSpec.DoubleValue BLOOD_RECOVERY;
    private static final ModConfigSpec.IntValue BLOOD_RECOVERY_HUNGER;
    private static final ModConfigSpec.BooleanValue BLOOD_LOSS_KILLS;
    private static final ModConfigSpec.BooleanValue BLOOD_PARTICLES;
    private static final ModConfigSpec.IntValue BLOOD_STAIN_SECONDS;
    private static final ModConfigSpec.DoubleValue TOURNIQUET_DAMAGE;

    // ---- armour -----------------------------------------------------------
    private static final ModConfigSpec.DoubleValue UNCOVERED_MULTIPLIER;
    private static final ModConfigSpec.BooleanValue AIMED_HITS;

    // ---- fractures --------------------------------------------------------
    private static final ModConfigSpec.BooleanValue FRACTURES;
    private static final ModConfigSpec.DoubleValue FRACTURE_FALL;
    private static final ModConfigSpec.DoubleValue FRACTURE_BLOW;
    private static final ModConfigSpec.DoubleValue FRACTURE_CHANCE;
    private static final ModConfigSpec.DoubleValue FRACTURE_PAIN;
    private static final ModConfigSpec.DoubleValue CRACK_BLOW;
    private static final ModConfigSpec.DoubleValue CRACK_CHANCE;
    private static final ModConfigSpec.IntValue CRACK_HEAL_SECONDS;
    private static final ModConfigSpec.DoubleValue CRACK_BREAK_CHANCE;

    // ---- infection --------------------------------------------------------
    private static final ModConfigSpec.BooleanValue INFECTION;
    private static final ModConfigSpec.IntValue BLEED_SECONDS_BEFORE_RISK;
    private static final ModConfigSpec.IntValue BLACKED_SECONDS_BEFORE_RISK;
    private static final ModConfigSpec.DoubleValue INFECTION_RISK;
    private static final ModConfigSpec.IntValue INFECTION_WORSEN_SECONDS;
    private static final ModConfigSpec.IntValue INFECTION_FEVER_SECONDS;
    private static final ModConfigSpec.DoubleValue INFECTION_MILD_DAMAGE;
    private static final ModConfigSpec.DoubleValue INFECTION_SEVERE_DAMAGE;

    // ---- burns ------------------------------------------------------------
    private static final ModConfigSpec.BooleanValue BURNS;
    private static final ModConfigSpec.DoubleValue BURN_CHANCE;
    private static final ModConfigSpec.IntValue BURN_COOK_SECONDS;
    private static final ModConfigSpec.DoubleValue BURN_DEEPEN;
    private static final ModConfigSpec.IntValue BURN_FADE_SECONDS;
    private static final ModConfigSpec.DoubleValue BURN_COOL_BACK;
    private static final ModConfigSpec.DoubleValue BURN_ICE;
    private static final ModConfigSpec.DoubleValue BURN_FLUID_FLOOR;
    private static final ModConfigSpec.DoubleValue BURN_FLUID_DRAIN;
    private static final ModConfigSpec.DoubleValue BURN_CHILL;
    private static final ModConfigSpec.DoubleValue SMOKE_CHANCE;
    private static final ModConfigSpec.IntValue SMOKE_SECONDS;
    private static final ModConfigSpec.DoubleValue SMOKE_BREATH;
    private static final ModConfigSpec.BooleanValue REACTIONS;
    private static final ModConfigSpec.BooleanValue ALLERGIES;
    private static final ModConfigSpec.DoubleValue ALLERGY_CHANCE;
    private static final ModConfigSpec.BooleanValue GRAFTS;
    private static final ModConfigSpec.IntValue GRAFT_SECONDS;
    private static final ModConfigSpec.DoubleValue GRAFT_COLD;
    private static final ModConfigSpec.DoubleValue GRAFT_RESTORE;
    private static final ModConfigSpec.DoubleValue PARASITE_CHANCE;
    private static final ModConfigSpec.DoubleValue PARASITE_HUNGER;
    private static final ModConfigSpec.DoubleValue PARASITE_DRAIN;

    // ---- diseases ---------------------------------------------------------
    private static final ModConfigSpec.BooleanValue DISEASES;
    private static final ModConfigSpec.IntValue COLD_SECONDS;
    private static final ModConfigSpec.IntValue POISONING_SECONDS;
    private static final ModConfigSpec.IntValue FEVER_SECONDS;
    private static final ModConfigSpec.DoubleValue SEPSIS_DAMAGE;
    private static final ModConfigSpec.DoubleValue POISONING_DAMAGE;
    private static final ModConfigSpec.DoubleValue COLD_RISK;
    private static final ModConfigSpec.DoubleValue FOOD_POISON_CHANCE;
    private static final ModConfigSpec.IntValue CONCUSSION_SECONDS;
    private static final ModConfigSpec.DoubleValue CONCUSSION_BLOW;
    private static final ModConfigSpec.DoubleValue CONCUSSION_CHANCE;
    private static final ModConfigSpec.BooleanValue SHOCK;
    private static final ModConfigSpec.DoubleValue SHOCK_BLOOD_LOSS;
    private static final ModConfigSpec.DoubleValue SHOCK_CLEAR_BLOOD_LOSS;
    private static final ModConfigSpec.IntValue SHOCK_REST_SECONDS;
    private static final ModConfigSpec.IntValue SHOCK_BLACKED_PARTS;
    private static final ModConfigSpec.DoubleValue SHOCK_DOWNED_RATE;
    private static final ModConfigSpec.BooleanValue HYPOTHERMIA;
    private static final ModConfigSpec.DoubleValue HYPOTHERMIA_RISK;
    private static final ModConfigSpec.IntValue HYPOTHERMIA_WARM_SECONDS;
    private static final ModConfigSpec.BooleanValue PNEUMOTHORAX;
    private static final ModConfigSpec.DoubleValue PNEUMO_BLOW;
    private static final ModConfigSpec.DoubleValue PNEUMO_CHANCE;
    private static final ModConfigSpec.DoubleValue PNEUMO_DAMAGE;
    private static final ModConfigSpec.IntValue PNEUMO_RAMP_SECONDS;
    private static final ModConfigSpec.IntValue REACTION_SECONDS;
    private static final ModConfigSpec.DoubleValue REACTION_DAMAGE;
    private static final ModConfigSpec.DoubleValue REACTION_BLOOD_LOSS;

    // ---- what being hurt does to you --------------------------------------
    private static final ModConfigSpec.BooleanValue EFFECTS;
    private static final ModConfigSpec.DoubleValue REGEN_PENALTY;
    private static final ModConfigSpec.IntValue PAINKILLER_SECONDS;
    private static final ModConfigSpec.DoubleValue PAINKILLER_FALLOFF;
    private static final ModConfigSpec.IntValue PAINKILLER_TOLERANCE_SECONDS;
    private static final ModConfigSpec.BooleanValue HEARTBEAT;
    private static final ModConfigSpec.BooleanValue ASSESS;
    private static final ModConfigSpec.BooleanValue TAGS;
    private static final ModConfigSpec.IntValue TAG_STALE_SECONDS;
    private static final ModConfigSpec.BooleanValue MINIGAME;
    private static final ModConfigSpec.DoubleValue INTERRUPT_DAMAGE;
    private static final ModConfigSpec.BooleanValue DOWNED;
    private static final ModConfigSpec.IntValue DOWNED_SECONDS;
    private static final ModConfigSpec.BooleanValue DOWNED_IGNORED;
    private static final ModConfigSpec.DoubleValue DOWNED_DAMAGE_SECONDS;
    private static final ModConfigSpec.IntValue STABILISE_SECONDS;
    private static final ModConfigSpec.DoubleValue STABILISE_FALLOFF;
    private static final ModConfigSpec.BooleanValue RAGDOLL;
    private static final ModConfigSpec.BooleanValue CARRY;
    private static final ModConfigSpec.DoubleValue CARRY_SLOW;
    private static final ModConfigSpec.DoubleValue DOWNED_CARRIED_RATE;
    private static final ModConfigSpec.BooleanValue AIRWAY;
    private static final ModConfigSpec.IntValue AIRWAY_SECONDS;
    private static final ModConfigSpec.DoubleValue AIRWAY_DOWNED_RATE;
    private static final ModConfigSpec.BooleanValue CPR;
    private static final ModConfigSpec.IntValue ARREST_SECONDS;
    private static final ModConfigSpec.DoubleValue CPR_FALLOFF;
    private static final ModConfigSpec.DoubleValue CPR_RIB_CHANCE;
    private static final ModConfigSpec.BooleanValue PRESSURE;
    private static final ModConfigSpec.IntValue PRESSURE_SECONDS;
    private static final ModConfigSpec.BooleanValue SCHIZOPHRENIA;
    private static final ModConfigSpec.DoubleValue SCHIZOPHRENIA_CHANCE;
    private static final ModConfigSpec.IntValue ANTIPSYCHOTIC_SECONDS;
    private static final ModConfigSpec.IntValue REBOUND_SECONDS;
    private static final ModConfigSpec.BooleanValue DELIRIUM;
    private static final ModConfigSpec.DoubleValue DELIRIUM_BLOOD_LOSS;
    private static final ModConfigSpec.IntValue DELIRIUM_CLEAR_SECONDS;
    private static final ModConfigSpec.IntValue WHISPER_SECONDS;
    private static final ModConfigSpec.IntValue PHANTOM_SECONDS;
    private static final ModConfigSpec.DoubleValue CATATONIA_CHANCE;
    private static final ModConfigSpec.IntValue CATATONIA_SECONDS;
    private static final ModConfigSpec.DoubleValue NEUROLEPTIC_CHANCE;
    private static final ModConfigSpec.DoubleValue NEUROLEPTIC_DAMAGE;
    private static final ModConfigSpec.IntValue NEUROLEPTIC_COOL_SECONDS;
    private static final ModConfigSpec.BooleanValue ORGANS;
    private static final ModConfigSpec.DoubleValue ORGAN_BLOW;
    private static final ModConfigSpec.DoubleValue ORGAN_CHANCE;
    private static final ModConfigSpec.DoubleValue ORGAN_HEART_STOP_CHANCE;
    private static final ModConfigSpec.DoubleValue ORGAN_BLEED;
    private static final ModConfigSpec.DoubleValue ORGAN_DAMAGE;
    private static final ModConfigSpec.DoubleValue ORGAN_SEPSIS;
    private static final ModConfigSpec.IntValue ORGAN_HEAL_SECONDS;
    private static final ModConfigSpec.BooleanValue REPERFUSION;
    private static final ModConfigSpec.DoubleValue REPERFUSION_DAMAGE;
    private static final ModConfigSpec.IntValue DRESSING_STUCK_SECONDS;
    private static final ModConfigSpec.BooleanValue ANAESTHESIA;
    private static final ModConfigSpec.IntValue ANAESTHESIA_SECONDS;
    private static final ModConfigSpec.DoubleValue ANAESTHESIA_ARREST;
    private static final ModConfigSpec.DoubleValue AWAKE_QUALITY_CAP;
    private static final ModConfigSpec.DoubleValue AWAKE_DAMAGE;
    private static final ModConfigSpec.BooleanValue DRESSINGS;
    private static final ModConfigSpec.IntValue DRESSING_CLEAN_SECONDS;
    private static final ModConfigSpec.BooleanValue NECROSIS;
    private static final ModConfigSpec.IntValue STRAP_DEAD_SECONDS;
    private static final ModConfigSpec.IntValue ROT_DEAD_SECONDS;
    private static final ModConfigSpec.DoubleValue DEAD_LIMB_DRAIN;
    private static final ModConfigSpec.DoubleValue DEAD_LIMB_SEPSIS;
    private static final ModConfigSpec.BooleanValue AMPUTATION;
    private static final ModConfigSpec.DoubleValue AMPUTATION_BLOOD_LOSS;
    private static final ModConfigSpec.DoubleValue AMPUTATION_OPEN_BLOOD_LOSS;
    private static final ModConfigSpec.BooleanValue PROSTHETICS;
    private static final ModConfigSpec.DoubleValue PROSTHETIC_RATIO;
    private static final ModConfigSpec.DoubleValue PROSTHETIC_RATIO_FINE;
    private static final ModConfigSpec.BooleanValue RABIES;
    private static final ModConfigSpec.DoubleValue RABIES_CHANCE;
    private static final ModConfigSpec.IntValue RABIES_SECONDS;
    private static final ModConfigSpec.IntValue FRENZY_SECONDS;
    private static final ModConfigSpec.DoubleValue FRENZY_DAMAGE;
    private static final ModConfigSpec.IntValue DEAFNESS_SECONDS;
    private static final ModConfigSpec.BooleanValue BLAST;
    private static final ModConfigSpec.DoubleValue TEAR_OFF_BLOW;
    private static final ModConfigSpec.DoubleValue TEAR_OFF_CHANCE;
    private static final ModConfigSpec.BooleanValue BLAST_TEAR_OFF;
    private static final ModConfigSpec.DoubleValue BLAST_TEAR_CHANCE;
    private static final ModConfigSpec.BooleanValue BLAST_FRAGMENTS;
    private static final ModConfigSpec.BooleanValue BLAST_WAVE;
    private static final ModConfigSpec.DoubleValue BLAST_ARMOUR_STOP;
    private static final ModConfigSpec.DoubleValue BLAST_LUNG_CHANCE;
    private static final ModConfigSpec.DoubleValue BLAST_GUT_CHANCE;
    private static final ModConfigSpec.DoubleValue BLAST_BURN_CHANCE;
    private static final ModConfigSpec.BooleanValue MENDING;
    private static final ModConfigSpec.IntValue MENDING_SECONDS;
    private static final ModConfigSpec.DoubleValue MENDING_TEAR_CHANCE;
    private static final ModConfigSpec.BooleanValue SLEEPLESS;
    private static final ModConfigSpec.IntValue SLEEPLESS_DAYS;
    private static final ModConfigSpec.DoubleValue SLEEPLESS_HANDS;
    private static final ModConfigSpec.DoubleValue SLEEPLESS_ILLNESS;
    private static final ModConfigSpec.DoubleValue SLEEPLESS_MENDING;
    private static final ModConfigSpec.IntValue EPINEPHRINE_ROUNDS;
    private static final ModConfigSpec.DoubleValue EPINEPHRINE_BLEED;
    private static final ModConfigSpec.DoubleValue EPINEPHRINE_ARREST;
    private static final ModConfigSpec.IntValue MORPHINE_SECONDS;
    private static final ModConfigSpec.BooleanValue MORPHINE_DEPRESSES;
    private static final ModConfigSpec.DoubleValue MORPHINE_STOPS;
    private static final ModConfigSpec.IntValue NALOXONE_SHOCK_PAIN;
    private static final ModConfigSpec.DoubleValue NALOXONE_SHOCK_CHANCE;
    private static final ModConfigSpec.IntValue FLU_SECONDS;
    private static final ModConfigSpec.DoubleValue FLU_RISK;
    private static final ModConfigSpec.IntValue FLU_CROWD;
    private static final ModConfigSpec.DoubleValue FLU_PNEUMONIA;
    private static final ModConfigSpec.IntValue STOMACH_SECONDS;
    private static final ModConfigSpec.DoubleValue STOMACH_CHANCE;
    private static final ModConfigSpec.DoubleValue STOMACH_DRAIN;
    private static final ModConfigSpec.DoubleValue STOMACH_DAMAGE;
    private static final ModConfigSpec.BooleanValue THERMOMETER;
    private static final ModConfigSpec.BooleanValue RINSE;
    private static final ModConfigSpec.IntValue RINSE_SECONDS;
    private static final ModConfigSpec.IntValue RINSE_FLOOR;
    private static final ModConfigSpec.BooleanValue REFERRED_PAIN;
    private static final ModConfigSpec.BooleanValue CHILL;
    private static final ModConfigSpec.DoubleValue CHILL_PER_OPERATION;
    private static final ModConfigSpec.DoubleValue CHILL_DEGREES;
    private static final ModConfigSpec.DoubleValue CHILL_HYPOTHERMIA;
    private static final ModConfigSpec.IntValue CHILL_WARM_SECONDS;
    private static final ModConfigSpec.DoubleValue SNEEZE_CHANCE;
    private static final ModConfigSpec.DoubleValue SNEEZE_DAMAGE;
    private static final ModConfigSpec.DoubleValue COLD_CATCHING;
    private static final ModConfigSpec.IntValue PNEUMONIA_AFTER_SECONDS;
    private static final ModConfigSpec.DoubleValue PNEUMONIA_CHANCE;
    private static final ModConfigSpec.DoubleValue PNEUMONIA_DAMAGE;
    private static final ModConfigSpec.IntValue PNEUMONIA_SEPSIS_SECONDS;
    private static final ModConfigSpec.DoubleValue PERITONITIS_DAMAGE;
    private static final ModConfigSpec.IntValue PERITONITIS_SEPSIS_SECONDS;
    private static final ModConfigSpec.BooleanValue ADRENALINE;
    private static final ModConfigSpec.IntValue ADRENALINE_SECONDS;
    private static final ModConfigSpec.DoubleValue ADRENALINE_RELIEF;
    private static final ModConfigSpec.IntValue ADRENALINE_CRASH_SECONDS;
    private static final ModConfigSpec.DoubleValue ADRENALINE_CRASH_BITE;
    private static final ModConfigSpec.DoubleValue ADRENALINE_SHAKE;
    private static final ModConfigSpec.DoubleValue ADRENALINE_PULSE;
    private static final ModConfigSpec.BooleanValue FATIGUE;
    private static final ModConfigSpec.IntValue FATIGUE_SECONDS;
    private static final ModConfigSpec.IntValue FATIGUE_RECOVER_SECONDS;
    private static final ModConfigSpec.DoubleValue FATIGUE_QUALITY;
    private static final ModConfigSpec.DoubleValue FATIGUE_CARRY_SLOW;
    private static final ModConfigSpec.DoubleValue FATIGUE_HOLD;
    private static final ModConfigSpec.BooleanValue ASSIST;
    private static final ModConfigSpec.DoubleValue ASSIST_QUALITY;
    private static final ModConfigSpec.DoubleValue ASSIST_AWAKE_CAP;
    private static final ModConfigSpec.BooleanValue THEATRE;
    private static final ModConfigSpec.DoubleValue GRIME_INFECTION;
    private static final ModConfigSpec.DoubleValue ROOM_INFECTION;
    private static final ModConfigSpec.DoubleValue PLACE_QUALITY;
    private static final ModConfigSpec.BooleanValue PULSE;
    private static final ModConfigSpec.BooleanValue BREATHING;
    private static final ModConfigSpec.BooleanValue TEMPERATURE;
    private static final ModConfigSpec.BooleanValue ASK;
    private static final ModConfigSpec.DoubleValue ORGAN_BROKEN_BONUS;
    private static final ModConfigSpec.IntValue PROSTHETIC_WEAR_SECONDS;
    private static final ModConfigSpec.DoubleValue PROSTHETIC_WORN_RATIO;
    private static final ModConfigSpec.BooleanValue PHANTOM_PAIN;
    private static final ModConfigSpec.IntValue PHANTOM_PAIN_SECONDS;
    private static final ModConfigSpec.IntValue FINDING_FRESH_SECONDS;
    private static final ModConfigSpec.BooleanValue BEDSIDE_OBS;
    private static final ModConfigSpec.IntValue BEDSIDE_SECONDS;
    private static final ModConfigSpec.IntValue BEDSIDE_RANGE;
    private static final ModConfigSpec.BooleanValue PANIC;
    private static final ModConfigSpec.DoubleValue PANIC_CHANCE;
    private static final ModConfigSpec.BooleanValue BREATH_SOUNDS;
    private static final ModConfigSpec.BooleanValue ARROWS;
    private static final ModConfigSpec.BooleanValue WEAPONS_STICK;
    private static final ModConfigSpec.DoubleValue STICK_CHANCE;
    private static final ModConfigSpec.DoubleValue STICK_DAMAGE;
    private static final ModConfigSpec.DoubleValue STICK_JOSTLE;
    private static final ModConfigSpec.DoubleValue STICK_JOSTLE_BLEED;
    private static final ModConfigSpec.DoubleValue STICK_TEAR;
    private static final ModConfigSpec.DoubleValue ARROW_CHANCE;
    private static final ModConfigSpec.DoubleValue ARROW_TEAR;
    private static final ModConfigSpec.BooleanValue BULLETS_STICK;
    private static final ModConfigSpec.DoubleValue BULLET_STICK_CHANCE;
    private static final ModConfigSpec.DoubleValue BULLET_TEAR;

    // ---- blood that stopped moving -----------------------------------------
    private static final ModConfigSpec.BooleanValue CLOTS;
    private static final ModConfigSpec.DoubleValue CLOT_CHANCE;
    private static final ModConfigSpec.IntValue CLOT_STILL_SECONDS;
    private static final ModConfigSpec.IntValue CLOT_CLEAR_SECONDS;
    private static final ModConfigSpec.DoubleValue EMBOLISM_CHANCE;
    private static final ModConfigSpec.DoubleValue EMBOLISM_DAMAGE;

    // ---- not sleeping ------------------------------------------------------
    private static final ModConfigSpec.DoubleValue SLEEPY_FROM;
    private static final ModConfigSpec.DoubleValue MICROSLEEP_FROM;
    private static final ModConfigSpec.DoubleValue MICROSLEEP_CHANCE;
    private static final ModConfigSpec.IntValue MICROSLEEP_SECONDS;

    // ---- bones that moved --------------------------------------------------
    private static final ModConfigSpec.BooleanValue DISPLACEMENT;
    private static final ModConfigSpec.DoubleValue DISPLACED_CHANCE;
    private static final ModConfigSpec.DoubleValue DISPLACED_BLOW;
    private static final ModConfigSpec.BooleanValue OPEN_FRACTURES;
    private static final ModConfigSpec.DoubleValue OPEN_CHANCE;
    private static final ModConfigSpec.DoubleValue OPEN_INFECTION;
    private static final ModConfigSpec.DoubleValue OPEN_BLOOD_LOSS;
    private static final ModConfigSpec.DoubleValue CROOKED_COST;
    private static final ModConfigSpec.DoubleValue CROOKED_PENALTY;
    private static final ModConfigSpec.DoubleValue SET_TEAR;
    private static final ModConfigSpec.DoubleValue SET_ARTERY;
    private static final ModConfigSpec.BooleanValue SCREAM;
    private static final ModConfigSpec.DoubleValue SCREAM_RANGE;

    // ---- the artery -------------------------------------------------------
    private static final ModConfigSpec.BooleanValue ARTERIAL;
    private static final ModConfigSpec.DoubleValue ARTERIAL_CHANCE;
    private static final ModConfigSpec.DoubleValue ARTERIAL_BLOW;
    private static final ModConfigSpec.DoubleValue ARTERIAL_RATE;
    private static final ModConfigSpec.DoubleValue BLOOD_PER_ARTERIAL;
    private static final ModConfigSpec.BooleanValue ARTERIAL_SPURT;

    // ---- going out --------------------------------------------------------
    private static final ModConfigSpec.BooleanValue FAINT;
    private static final ModConfigSpec.DoubleValue FAINT_BLOOD_LOSS;
    private static final ModConfigSpec.DoubleValue FAINT_CHANCE;
    private static final ModConfigSpec.IntValue FAINT_SECONDS;
    private static final ModConfigSpec.IntValue FAINT_PAIN;
    private static final ModConfigSpec.IntValue FAINT_CLEAR_SECONDS;

    // ---- what it leaves ---------------------------------------------------
    private static final ModConfigSpec.BooleanValue SCARS;
    private static final ModConfigSpec.DoubleValue SCAR_STIFFNESS;
    private static final ModConfigSpec.DoubleValue BURN_STIFFNESS;
    private static final ModConfigSpec.DoubleValue GRAFT_STIFFNESS;
    private static final ModConfigSpec.DoubleValue STIFF_QUALITY;
    private static final ModConfigSpec.DoubleValue STIFF_SLOW;
    private static final ModConfigSpec.DoubleValue SCAR_INFECTION;
    private static final ModConfigSpec.DoubleValue KNIT_FRAGILITY;
    private static final ModConfigSpec.BooleanValue ACHE;
    private static final ModConfigSpec.IntValue ACHE_SECONDS;
    private static final ModConfigSpec.BooleanValue STRETCH;
    private static final ModConfigSpec.DoubleValue STRETCH_GAIN;
    private static final ModConfigSpec.DoubleValue STRETCH_SELF_CAP;
    private static final ModConfigSpec.DoubleValue STRETCH_TEAR;

    // ---- the stick on the outside -----------------------------------------
    private static final ModConfigSpec.BooleanValue SPLINT_WORN;
    private static final ModConfigSpec.IntValue SPLINT_SECONDS;
    private static final ModConfigSpec.IntValue SPLINT_RUN_PENALTY;
    private static final ModConfigSpec.IntValue SPLINT_RUN_CEILING;
    private static final ModConfigSpec.DoubleValue SPLINT_SLOW;
    private static final ModConfigSpec.DoubleValue SPLINT_QUALITY;

    // ---- taken off by hand ------------------------------------------------
    private static final ModConfigSpec.BooleanValue SEVER;
    private static final ModConfigSpec.DoubleValue SEVER_BLOW;
    private static final ModConfigSpec.DoubleValue SEVER_CHANCE;
    private static final ModConfigSpec.DoubleValue SEVER_HEALTHY;

    // ---- medicine ---------------------------------------------------------
    private static final ModConfigSpec.DoubleValue TRANSFUSION_RESTORE;
    private static final ModConfigSpec.DoubleValue BLOOD_RESTORE;
    private static final ModConfigSpec.DoubleValue DONATION_COST;
    private static final ModConfigSpec.DoubleValue MAX_DONOR_LOSS;

    private static final ModConfigSpec.ConfigValue<List<? extends String>> BANDAGES;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> TOURNIQUETS;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> SPLINTS;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> MEDKITS;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> SURGERY;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> PAINKILLERS;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> ANTISEPTICS;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> SALVES;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> ANTIBIOTICS;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> SALINES;
    private static final ModConfigSpec.DoubleValue BANDAGE_HEAL;
    private static final ModConfigSpec.DoubleValue MEDKIT_HEAL;
    private static final ModConfigSpec.DoubleValue SALVE_HEAL;
    private static final ModConfigSpec.DoubleValue SURGERY_RESTORE;

    public static final ModConfigSpec SPEC;

    private static Values values = new Values();

    private Config() {
    }

    static {
        BUILDER.comment("Three illnesses that come out of the shape of a wound or out of a",
                        "treatment rather than out of dirt, and the numbers behind them.")
                .translation("triage.configuration.complications").push("complications");
        TETANUS_SECONDS = BUILDER.comment("How long lockjaw stays silent and treatable. When this runs",
                        "out a course stops working, and nothing after that does.")
                .translation("triage.configuration.complications.tetanusSeconds").defineInRange("tetanusSeconds", 420, 30, 100000);
        TETANUS_CHANCE = BUILDER.comment("Chance per second that a dressed, dirty puncture - an arrow or a",
                        "fragment closed over - becomes lockjaw. Small on purpose: this is",
                        "the price of one bad decision, not a tax on being shot.")
                .translation("triage.configuration.complications.tetanusChance").defineInRange("tetanusChance", 0.0016, 0.0, 1.0);
        CRUSH_SECONDS = BUILDER.comment("How long there is, once the strap is off a limb that was under it",
                        "too long, before the heart answers for it.")
                .translation("triage.configuration.complications.crushSeconds").defineInRange("crushSeconds", 75, 10, 100000);
        CRUSH_STRAP_SECONDS = BUILDER.comment("How long a tourniquet has to have been on before releasing it",
                        "is dangerous. Below this, nothing happens at all.")
                .translation("triage.configuration.complications.crushStrapSeconds").defineInRange("crushStrapSeconds", 180, 10, 100000);
        RELAPSE_CHANCE = BUILDER.comment("Chance that a cured illness comes back in waves instead of ending.",
                        "What it is really measuring is a course that was not finished.")
                .translation("triage.configuration.complications.relapseChance").defineInRange("relapseChance", 0.18, 0.0, 1.0);
        BUILDER.pop();

        BUILDER.comment("Which of the two the mod is playing.",
                        "",
                        "Off is arcade, which is what this mod has always been: the body",
                        "chart is a readout. Open it and it tells you the left leg has two",
                        "breaks and an infection, and you go and treat that.",
                        "",
                        "On is realistic, and it changes exactly one thing - where that",
                        "knowledge comes from. The body is identical: same injuries, same",
                        "rates, same treatments, same minigames. What goes is the free",
                        "answer. A part nobody has examined reads as a blank rather than as",
                        "a healthy one, and the difference between 'nothing there' and 'did",
                        "not look' is the whole of the mode.",
                        "",
                        "Everything needed to fill that chart in is already in the mod and",
                        "was built one version at a time: a pulse, asking where it hurts,",
                        "asking for a number out of ten, pressing on a burn, a thermometer,",
                        "an X-ray - and two new ones, feeling along a bone and listening to",
                        "a chest. In arcade those are colour. Here they are the only channel.",
                        "",
                        "The server decides this for everybody in the world. Two people in",
                        "one world cannot be playing two different games.")
                .translation("triage.configuration.difficulty").push("difficulty");
        REALISTIC = BUILDER.translation("triage.configuration.difficulty.realistic").define("realistic", false);
        BUILDER.pop();

        BUILDER.comment("How much health each part of the body has. These are not hearts:",
                        "the seven parts add up to a pool of their own, and the vanilla bar",
                        "only mirrors what is left of it.",
                        "",
                        "Head and thorax are vital - empty either one and the player dies.",
                        "The rest black out instead.")
                .translation("triage.configuration.health").push("health");
        HEAD_MAX = BUILDER.translation("triage.configuration.health.headMax").defineInRange("headMax", 10.0, 1.0, 1000.0);
        THORAX_MAX = BUILDER.translation("triage.configuration.health.thoraxMax").defineInRange("thoraxMax", 22.0, 1.0, 1000.0);
        STOMACH_MAX = BUILDER.translation("triage.configuration.health.stomachMax").defineInRange("stomachMax", 16.0, 1.0, 1000.0);
        ARM_MAX = BUILDER.comment("Each arm.").translation("triage.configuration.health.armMax").defineInRange("armMax", 12.0, 1.0, 1000.0);
        LEG_MAX = BUILDER.comment("Each leg.").translation("triage.configuration.health.legMax").defineInRange("legMax", 14.0, 1.0, 1000.0);
        DAMAGE_SCALE = BUILDER.comment("Incoming damage is multiplied by this before it is taken off a part.",
                        "Damage arrives here after armour and resistance, so armour still works.",
                        "Raise it to make the game more lethal, lower it to make bodies tougher.")
                .translation("triage.configuration.health.damageScale").defineInRange("damageScale", 2.0, 0.1, 100.0);
        HEAL_SCALE = BUILDER.comment("Same, for healing: potions, food regeneration and the like.")
                .translation("triage.configuration.health.healScale").defineInRange("healScale", 2.0, 0.1, 100.0);
        REGENERATION = BUILDER.comment("",
                        "Whether the body mends itself at all.",
                        "",
                        "Off turns off every source of natural healing - food, potions,",
                        "golden apples, beacons and anything else that calls vanilla's",
                        "heal - and leaves only what somebody does with their hands.",
                        "Bandages, medkits, splints and surgery heal the body directly",
                        "and keep working; this is about the passive recovery that",
                        "quietly undoes a wound while the player waits.",
                        "",
                        "The default is on. Turn it off for a world where every injury",
                        "has to be treated, or leave it on and use the healing scale",
                        "above to decide how fast a body comes back.")
                .translation("triage.configuration.health.regeneration").define("regeneration", true);
        OVERFLOW_TO_THORAX = BUILDER.comment("Share of a hit that a blacked-out limb cannot absorb and passes",
                        "on to the thorax. 0 makes dead limbs a free shield.")
                .translation("triage.configuration.health.overflowToThorax").defineInRange("overflowToThorax", 0.5, 0.0, 1.0);
        BLACKED_DRAIN = BUILDER.comment("Damage per second done to the thorax by every blacked-out limb.",
                        "This is what eventually kills a player who never treats anything.")
                .translation("triage.configuration.health.blackedLimbDrain").defineInRange("blackedLimbDrain", 0.15, 0.0, 10.0);
        ONE_SHOT_ABOVE = BUILDER.comment("",
                        "No single blow may empty a head or a chest that was healthier than this",
                        "share of its maximum. A drawn bow to the chest is realistic and it is",
                        "also the end of the game before it started: the arrow you never saw",
                        "leaves no decision to make, and this mod is about decisions.",
                        "",
                        "Below the threshold a hit can still finish you outright - being already",
                        "hurt is supposed to be dangerous. Set to 0 to allow one-shots again.")
                .translation("triage.configuration.health.oneShotProtectionAbove").defineInRange("oneShotProtectionAbove", 0.5, 0.0, 1.0);
        ONE_SHOT_FLOOR = BUILDER.comment("What such a blow leaves behind, in health.")
                .translation("triage.configuration.health.oneShotFloor").defineInRange("oneShotFloor", 1.0, 0.0, 100.0);
        BUILDER.pop();

        BUILDER.comment("Relative chance of an ordinary physical hit landing on each part.",
                        "Only the ratios matter, not the totals. Falls always go to the legs,",
                        "and damage with no point of impact - drowning, poison, fire - is",
                        "spread over the whole body instead.")
                .translation("triage.configuration.routing").push("routing");
        HEAD_WEIGHT = BUILDER.translation("triage.configuration.routing.head").defineInRange("head", 8, 0, 1000);
        THORAX_WEIGHT = BUILDER.translation("triage.configuration.routing.thorax").defineInRange("thorax", 25, 0, 1000);
        STOMACH_WEIGHT = BUILDER.translation("triage.configuration.routing.stomach").defineInRange("stomach", 15, 0, 1000);
        ARM_WEIGHT = BUILDER.comment("Per arm.").translation("triage.configuration.routing.arm").defineInRange("arm", 13, 0, 1000);
        LEG_WEIGHT = BUILDER.comment("Per leg.").translation("triage.configuration.routing.leg").defineInRange("leg", 13, 0, 1000);
        BUILDER.pop();

        BUILDER.comment("Bleeding damages the part it is on, once a second, and does not stop",
                        "on its own unless lightBleedSeconds says so. Bandages stop it.")
                .translation("triage.configuration.bleeding").push("bleeding");
        LIGHT_BLEED_RATE = BUILDER.translation("triage.configuration.bleeding.lightPerSecond").defineInRange("lightPerSecond", 0.25, 0.0, 100.0);
        HEAVY_BLEED_RATE = BUILDER.translation("triage.configuration.bleeding.heavyPerSecond").defineInRange("heavyPerSecond", 0.7, 0.0, 100.0);
        BLEED_CHANCE_PER_DAMAGE = BUILDER.comment("Chance of a cut per point of damage the part took. Capped at 60%.")
                .translation("triage.configuration.bleeding.chancePerDamage").defineInRange("chancePerDamage", 0.05, 0.0, 1.0);
        HEAVY_BLEED_THRESHOLD = BUILDER.comment("Damage to one part in a single hit that makes the bleed a heavy one.")
                .translation("triage.configuration.bleeding.heavyThreshold").defineInRange("heavyThreshold", 10.0, 0.1, 1000.0);
        LIGHT_BLEED_SECONDS = BUILDER.comment("How long a light bleed runs before it clots. 0 means never.")
                .translation("triage.configuration.bleeding.lightBleedSeconds").defineInRange("lightBleedSeconds", 60, 0, 100000);
        BUILDER.comment("",
                        "Blood lost while bleeding, as a share of all of it. This is what a bleed",
                        "leaves behind once it is bandaged: the wound closes, the blood does not",
                        "come back for a long while, and a body short of it is slow and dizzy.",
                        "Rest and food put it back slowly; a drip puts it back at once, but only",
                        "once nothing is still open.")
                .translation("triage.configuration.bleeding.blood").push("blood");
        BLOOD_PER_LIGHT = BUILDER.translation("triage.configuration.bleeding.blood.lightPerSecond").defineInRange("lightPerSecond", 0.004, 0.0, 1.0);
        BLOOD_PER_HEAVY = BUILDER.translation("triage.configuration.bleeding.blood.heavyPerSecond").defineInRange("heavyPerSecond", 0.010, 0.0, 1.0);
        BLOOD_RECOVERY = BUILDER.comment("Put back each second, but only while there is food in the tank.")
                .translation("triage.configuration.bleeding.blood.recoveryPerSecond").defineInRange("recoveryPerSecond", 0.0030, 0.0, 1.0);
        BLOOD_RECOVERY_HUNGER = BUILDER.comment("Hunger needed before blood is made at all, out of 20.")
                .translation("triage.configuration.bleeding.blood.recoveryHunger").defineInRange("recoveryHunger", 7, 0, 20);
        BLOOD_LOSS_KILLS = BUILDER.comment("Whether running out of blood entirely is fatal. With this off, an",
                        "empty player is merely slow and dizzy forever, which rather undercuts",
                        "the word 'bleeding'.")
                .translation("triage.configuration.bleeding.blood.bloodLossKills").define("bloodLossKills", true);
        BLOOD_PARTICLES = BUILDER.comment("Blood running off a bleeding player and staining the floor under them.",
                        "Nothing is placed in the world and nothing is saved: stains are redrawn",
                        "as particles for a while and then dry up.")
                .translation("triage.configuration.bleeding.blood.particles").define("particles", true);
        BLOOD_STAIN_SECONDS = BUILDER.comment("",
                        "How long blood stays on the floor before it has dried, in seconds.",
                        "",
                        "This is how long a trail is worth following. Set short it is not a trail",
                        "at all - by the time somebody has worked out which way the marks lead,",
                        "the far end of them has gone and the direction went with it.")
                .translation("triage.configuration.bleeding.blood.stainSeconds").defineInRange("stainSeconds", 300, 1, 100000);
        TOURNIQUET_DAMAGE = BUILDER.comment("Damage a strapped limb takes each second while the tourniquet is on.",
                        "",
                        "Zero, and that is a correction rather than a nerf. This used to be the",
                        "whole cost of a strap and it was right when it was: something had to",
                        "make keeping one on a decision. Then necrosis arrived and gave the strap",
                        "a three minute clock - and left this running underneath it, at a rate",
                        "that emptied the limb in about thirty seconds. The clock was never",
                        "reached by anybody, because there was nothing left to reach it with.",
                        "",
                        "A real tourniquet is fine for a while and then it is not, which is what",
                        "the clock says and this cannot.",
                        "",
                        "It is back at a rate the clock outlives: about nine over three minutes,",
                        "so a healthy limb is badly used by the time it dies and never quite",
                        "empties on its own. And it goes onto that limb ONLY - what a part cannot",
                        "absorb no longer runs into the chest, because a strap round an arm does",
                        "nothing whatever to a chest and billing one for the other was how three",
                        "minutes under a strap became unsurvivable.")
                .translation("triage.configuration.bleeding.blood.tourniquetDamagePerSecond").defineInRange("tourniquetDamagePerSecond", 0.05, 0.0, 100.0);
        BUILDER.pop();
        BUILDER.pop();

        BUILDER.comment("Broken bones. A fracture does not heal on its own - it needs a splint.")
                .translation("triage.configuration.fractures").push("fractures");
        FRACTURES = BUILDER.translation("triage.configuration.fractures.enabled").define("enabled", true);
        FRACTURE_FALL = BUILDER.comment("Fall damage (before scaling) that can break a leg.")
                .translation("triage.configuration.fractures.fallThreshold").defineInRange("fallThreshold", 4.0, 0.1, 1000.0);
        FRACTURE_BLOW = BUILDER.comment("Damage to one part in a single hit that can break the bone under it.")
                .translation("triage.configuration.fractures.blowThreshold").defineInRange("blowThreshold", 14.0, 0.1, 1000.0);
        FRACTURE_CHANCE = BUILDER.translation("triage.configuration.fractures.chance").defineInRange("chance", 0.5, 0.0, 1.0);
        FRACTURE_PAIN = BUILDER.comment("Damage done to a fractured leg each second the player sprints on it.")
                .translation("triage.configuration.fractures.painPerSecond").defineInRange("painPerSecond", 1.5, 0.0, 100.0);
        CRACK_BLOW = BUILDER.comment("Damage to one part in a single hit that can crack the bone without",
                        "breaking it. A crack is a warning: left alone it knits by itself, but",
                        "another hit or a sprint on it turns it into a real fracture.")
                .translation("triage.configuration.fractures.crackThreshold").defineInRange("crackThreshold", 8.0, 0.1, 1000.0);
        CRACK_CHANCE = BUILDER.translation("triage.configuration.fractures.crackChance").defineInRange("crackChance", 0.45, 0.0, 1.0);
        CRACK_HEAL_SECONDS = BUILDER.comment("Seconds of not being leaned on before a crack knits. Every hit on",
                        "the part, and every second of sprinting on a cracked leg, starts it over.")
                .translation("triage.configuration.fractures.crackHealSeconds").defineInRange("crackHealSeconds", 90, 1, 100000);
        CRACK_BREAK_CHANCE = BUILDER.comment("Chance that leaning on a crack breaks it properly.")
                .translation("triage.configuration.fractures.crackBreakChance").defineInRange("crackBreakChance", 0.5, 0.0, 1.0);
        BUILDER.pop();

        BUILDER.comment("Vanilla armour has already done its work by the time a hit is routed, and",
                        "it protects the whole body evenly. This is the part it cannot express:",
                        "a hit that lands where you are wearing nothing hurts more.",
                        "",
                        "Helmet covers the head, chestplate the thorax and stomach, leggings and",
                        "boots the legs. Arms are never covered - there is no slot for them.",
                        "Set to 1.0 to switch this off.")
                .translation("triage.configuration.armour").push("armour");
        UNCOVERED_MULTIPLIER = BUILDER.translation("triage.configuration.armour.uncoveredMultiplier").defineInRange("uncoveredMultiplier", 1.3, 1.0, 10.0);
        AIMED_HITS = BUILDER.comment("",
                        "Whether a blow lands where it was actually aimed. An arrow is asked",
                        "where it is; a melee swing is traced from the attacker's line of sight",
                        "into the victim. Aim at a head, hit a head.",
                        "",
                        "Turn this off to go back to the weights in [routing] deciding everything,",
                        "which is kinder to anyone being shot at by a skeleton.")
                .translation("triage.configuration.armour.aimedHits").define("aimedHits", true);
        BUILDER.pop();

        BUILDER.comment("A wound that is not cleaned goes bad. Infection eats the part it is on,",
                        "stops that part healing by itself, and at its worst makes the whole body",
                        "ill - see [diseases]. Antiseptic clears it.")
                .translation("triage.configuration.infection").push("infection");
        INFECTION = BUILDER.translation("triage.configuration.infection.enabled").define("enabled", true);
        BLEED_SECONDS_BEFORE_RISK = BUILDER.comment("How long a part bleeds before it can start going bad.")
                .translation("triage.configuration.infection.bleedingSecondsBeforeRisk").defineInRange("bleedingSecondsBeforeRisk", 45, 1, 100000);
        BLACKED_SECONDS_BEFORE_RISK = BUILDER.comment("Same, for a limb that has been blacked out and left.")
                .translation("triage.configuration.infection.blackedSecondsBeforeRisk").defineInRange("blackedSecondsBeforeRisk", 60, 1, 100000);
        INFECTION_RISK = BUILDER.comment("Chance per second, once past those thresholds.")
                .translation("triage.configuration.infection.riskPerSecond").defineInRange("riskPerSecond", 0.012, 0.0, 1.0);
        INFECTION_WORSEN_SECONDS = BUILDER.comment("Seconds a mild infection runs before it turns severe.")
                .translation("triage.configuration.infection.worsenSeconds").defineInRange("worsenSeconds", 90, 1, 100000);
        INFECTION_FEVER_SECONDS = BUILDER.comment("Seconds a severe infection runs before it becomes a fever.")
                .translation("triage.configuration.infection.feverSeconds").defineInRange("feverSeconds", 60, 1, 100000);
        INFECTION_MILD_DAMAGE = BUILDER.translation("triage.configuration.infection.mildDamagePerSecond").defineInRange("mildDamagePerSecond", 0.08, 0.0, 100.0);
        INFECTION_SEVERE_DAMAGE = BUILDER.translation("triage.configuration.infection.severeDamagePerSecond").defineInRange("severeDamagePerSecond", 0.2, 0.0, 100.0);
        BUILDER.pop();

        BUILDER.comment("Fire and lava leave burns, and a burn has a depth.",
                        "",
                        "Red heals itself. Blistered wants a salve. Through the skin will not",
                        "close at all and no salve reaches it - that one wants the knife. Which",
                        "of the three it ends up being is not decided when the fire touches you:",
                        "every burn arrives shallow and then cooks, and what happens during that",
                        "window is the only part of a burn anybody can change.",
                        "",
                        "The worst of the three does not hurt, because the nerve endings that",
                        "would have done the hurting are part of what burnt. So the loudest burn",
                        "is the one that will be fine and the quiet patch beside it is the one",
                        "that needs help, which is why a burn is tested by hand rather than",
                        "looked at.")
                .translation("triage.configuration.burns").push("burns");
        BURNS = BUILDER.translation("triage.configuration.burns.enabled").define("enabled", true);
        BURN_CHANCE = BUILDER.comment("Chance per point of fire damage taken. Capped at 70%.")
                .translation("triage.configuration.burns.chancePerDamage").defineInRange("chancePerDamage", 0.08, 0.0, 1.0);
        BURN_COOK_SECONDS = BUILDER.comment("",
                        "How long a burn goes on deciding how deep it is, after the fire is out.",
                        "",
                        "This is the window, and it is the whole of the first aid: water in it",
                        "stops the burn where it is, and nothing outside it changes anything.")
                .translation("triage.configuration.burns.cookSeconds").defineInRange("cookSeconds", 30, 0, 100000);
        BURN_DEEPEN = BUILDER.comment("Chance per second in that window of going a step deeper.")
                .translation("triage.configuration.burns.deepenPerSecond").defineInRange("deepenPerSecond", 0.05, 0.0, 1.0);
        BURN_FADE_SECONDS = BUILDER.comment("",
                        "How long a superficial burn takes to close on its own.",
                        "",
                        "Red skin heals, and it is the only one of the three that does. A mod",
                        "that made every brush with a campfire into an errand for a jar would be",
                        "wrong about burns in the direction that annoys rather than the one that",
                        "teaches.")
                .translation("triage.configuration.burns.fadeSeconds").defineInRange("fadeSeconds", 90, 1, 100000);
        BURN_COOL_BACK = BUILDER.comment("",
                        "And the chance that a burn held under water for the whole window comes",
                        "back a step. The only thing in this mod that undoes an injury rather",
                        "than treating it, and the one place where that is true in life too.")
                .translation("triage.configuration.burns.coolBackChance").defineInRange("coolBackChance", 0.5, 0.0, 1.0);
        BURN_ICE = BUILDER.comment("",
                        "What packing a burn in something freezing does to it, as a multiplier on",
                        "the deepening chance. Snow and ice are not water: they shut down what",
                        "little blood is still reaching the burn and take it deeper, which is a",
                        "mistake common enough that every first aid course warns about it.")
                .translation("triage.configuration.burns.iceFactor").defineInRange("iceFactor", 2.0, 0.0, 100.0);
        BURN_FLUID_FLOOR = BUILDER.comment("",
                        "How much of somebody has to be burnt before it costs volume.",
                        "",
                        "The rule of nines, added up over the parts burnt past the skin: head",
                        "nine, each arm nine, each leg eighteen, chest and belly eighteen apiece.",
                        "Under this it is an injury; over it, it is a person emptying out through",
                        "their own surface, and the answer is fluid faster than it is going.")
                .translation("triage.configuration.burns.fluidFloor").defineInRange("fluidFloor", 0.15, 0.0, 1.0);
        BURN_FLUID_DRAIN = BUILDER.comment("Volume lost per second, per share of surface over that floor.")
                .translation("triage.configuration.burns.fluidDrainPerSecond").defineInRange("fluidDrainPerSecond", 0.02, 0.0, 1.0);
        BURN_CHILL = BUILDER.comment("",
                        "And how fast a burnt body cools, per share of surface per second. Skin",
                        "is the thing that keeps heat in. This feeds the same figure an operation",
                        "feeds, so the circle the mod already has closes around burns as well:",
                        "cold does not clot, and a big burn is a cold patient.")
                .translation("triage.configuration.burns.chillPerSecond").defineInRange("chillPerSecond", 0.004, 0.0, 1.0);
        SMOKE_CHANCE = BUILDER.comment("",
                        "Breathing it, which is the half of fire that kills quickly.",
                        "",
                        "A burn on the outside is measured in hours. An airway that has breathed",
                        "fire is measured in minutes, it happens to people who walked out looking",
                        "fine, and the tell is the voice rather than the skin.",
                        "",
                        "Chance per second of it while standing in fire or lava with nothing over",
                        "your face. 0 turns it off.")
                .translation("triage.configuration.burns.smokeChancePerSecond").defineInRange("smokeChancePerSecond", 0.08, 0.0, 1.0);
        SMOKE_SECONDS = BUILDER.comment("How long it takes to come right, out of the smoke.")
                .translation("triage.configuration.burns.smokeSeconds").defineInRange("smokeSeconds", 240, 1, 100000);
        SMOKE_BREATH = BUILDER.comment("What it takes off the breathing rate while it lasts.")
                .translation("triage.configuration.burns.smokeBreath").defineInRange("smokeBreath", 0.35, 0.0, 1.0);
        BUILDER.pop();

        BUILDER.comment("Whether people say anything when the worst of it happens to them.",
                        "",
                        "Everything in this mod has been told to the player in the voice of a",
                        "chart: a bone breaks and a line names the bone. That is the right voice",
                        "for a readout and the wrong one for the moment itself, because the moment",
                        "is not information - it is a person making a noise.",
                        "",
                        "Whoever is within about fourteen blocks hears it, which is the only thing",
                        "in the mod a bystander gets without walking over and opening a chart. It",
                        "is also the first warning a medic ever gets.",
                        "",
                        "None of it is a mechanic. No line does anything.")
                .translation("triage.configuration.reactions").push("reactions");
        REACTIONS = BUILDER.translation("triage.configuration.reactions.enabled").define("enabled", true);
        BUILDER.pop();

        BUILDER.comment("The one thing in the bag that will kill this particular person.",
                        "",
                        "Rolled with the blood type, at the same moment and for the same reason:",
                        "it is a fact about somebody rather than an event in their day, and it is",
                        "wanted before a bag is opened over them rather than after.",
                        "",
                        "What happens is anaphylaxis, which this mod already had, and the answer",
                        "is the needle it already keeps for exactly that. The dose is spent either",
                        "way - it is not a refusal, it is a treatment that went in and did the",
                        "wrong thing.",
                        "",
                        "And it can be asked about, which is the whole of what makes it",
                        "interesting: a conscious patient tells you, and an unconscious one does",
                        "not, so the same dose is a treatment on a man who can talk and a killing",
                        "on the same man ten minutes later.")
                .translation("triage.configuration.allergies").push("allergies");
        ALLERGIES = BUILDER.translation("triage.configuration.allergies.enabled").define("enabled", true);
        ALLERGY_CHANCE = BUILDER.comment("Share of bodies that arrive with one. Even odds between the three.")
                .translation("triage.configuration.allergies.chance").defineInRange("chance", 0.18, 0.0, 1.0);
        BUILDER.pop();

        BUILDER.comment("Whether a limb that comes off lands on the floor, and can go back on.",
                        "",
                        "Losing one here was final in the flattest way: the arm stopped existing",
                        "the moment it came off. That is true of an amputation somebody chose and",
                        "not of the other way limbs come off - something takes one off you, and",
                        "the thing it took is lying four feet away.",
                        "",
                        "So it drops, with a clock on it. What decides whether it can go back on",
                        "happened before anybody opened a screen: how long it has been off, and",
                        "whether anyone thought to keep it cold.",
                        "",
                        "It makes nothing survivable that was not. The stump bleeds the same and",
                        "the shock arrives the same - what it adds is a second clock running",
                        "beside the first, and a decision about which to spend the minute on.")
                .translation("triage.configuration.grafts").push("grafts");
        GRAFTS = BUILDER.translation("triage.configuration.grafts.enabled").define("enabled", true);
        GRAFT_SECONDS = BUILDER.comment("How long a limb is worth sewing back on, warm.")
                .translation("triage.configuration.grafts.graftSeconds").defineInRange("graftSeconds", 300, 1, 100000);
        GRAFT_COLD = BUILDER.comment("",
                        "And how much slower that clock runs in water or snow. A third is the",
                        "real advice: a severed hand travels to hospital on ice, and it is the",
                        "one piece of first aid about this that everybody has heard and nobody",
                        "expects a game to know.")
                .translation("triage.configuration.grafts.graftCold").defineInRange("graftCold", 0.33, 0.01, 1.0);
        GRAFT_RESTORE = BUILDER.comment("",
                        "What a limb comes back on, as a share of what it was, before the",
                        "surgery is taken into account. It comes back stiff and has to be rested",
                        "like a set bone, and it comes back dirty - it has been on the floor.")
                .translation("triage.configuration.grafts.graftRestore").defineInRange("graftRestore", 0.45, 0.05, 1.0);
        BUILDER.pop();

        BUILDER.comment("Worms, from what was eaten.",
                        "",
                        "The commonest illness in the world and the one nobody puts in a game,",
                        "because there is no scene in it. It does not burn, it opens nothing and",
                        "it will not kill anybody this afternoon. It takes: it eats before you do",
                        "and drinks off the blood while it is there, and what it leaves is",
                        "somebody hungry and short and tired for weeks with no dramatic moment in",
                        "the whole business.",
                        "",
                        "Which is why it belongs here. Nothing else in this mod is slow.",
                        "",
                        "It does not pass on its own - nothing about a parasite is self-limiting -",
                        "and a bottle of antibiotics does nothing for it. What answers it is the",
                        "other card, which answers nothing else.")
                .translation("triage.configuration.parasites").push("parasites");
        PARASITE_CHANCE = BUILDER.comment("Chance that something eaten badly was carrying them.")
                .translation("triage.configuration.parasites.chance").defineInRange("chance", 0.25, 0.0, 1.0);
        PARASITE_HUNGER = BUILDER.comment("Food burnt per second while they are in there. They eat first.")
                .translation("triage.configuration.parasites.hungerPerSecond").defineInRange("hungerPerSecond", 0.08, 0.0, 10.0);
        PARASITE_DRAIN = BUILDER.comment("And volume taken per second, which is what makes it anaemia.")
                .translation("triage.configuration.parasites.drainPerSecond").defineInRange("drainPerSecond", 0.0006, 0.0, 1.0);
        BUILDER.pop();

        BUILDER.comment("Illness, as opposed to injury: it belongs to the whole body and no",
                        "bandage will touch it. A fever left alone becomes blood poisoning, and",
                        "that one does not pass by itself. Antibiotics cure any of them.")
                .translation("triage.configuration.diseases").push("diseases");
        DISEASES = BUILDER.translation("triage.configuration.diseases.enabled").define("enabled", true);
        COLD_SECONDS = BUILDER.comment("How long a cold lasts before it passes on its own.")
                .translation("triage.configuration.diseases.coldSeconds").defineInRange("coldSeconds", 240, 1, 100000);
        POISONING_SECONDS = BUILDER.translation("triage.configuration.diseases.poisoningSeconds").defineInRange("poisoningSeconds", 180, 1, 100000);
        FEVER_SECONDS = BUILDER.comment("How long a fever runs before it becomes sepsis. This is the deadline",
                        "the whole infection chain exists to impose.")
                .translation("triage.configuration.diseases.feverSeconds").defineInRange("feverSeconds", 300, 1, 100000);
        SEPSIS_DAMAGE = BUILDER.comment("Damage done to the thorax each second by blood poisoning.")
                .translation("triage.configuration.diseases.sepsisDamagePerSecond").defineInRange("sepsisDamagePerSecond", 0.3, 0.0, 100.0);
        POISONING_DAMAGE = BUILDER.comment("Damage done to the stomach each second while poisoned.")
                .translation("triage.configuration.diseases.poisoningDamagePerSecond").defineInRange("poisoningDamagePerSecond", 0.12, 0.0, 100.0);
        COLD_RISK = BUILDER.comment("Chance per second of catching a cold while freezing or soaked in a",
                        "cold place. A cold is mild, but it doubles the chance of a wound going bad.")
                .translation("triage.configuration.diseases.coldRiskPerSecond").defineInRange("coldRiskPerSecond", 0.02, 0.0, 1.0);
        FOOD_POISON_CHANCE = BUILDER.comment("Chance of poisoning from eating something that deserved it -",
                        "rotten flesh, raw chicken, spider eyes, pufferfish.")
                .translation("triage.configuration.diseases.foodPoisonChance").defineInRange("foodPoisonChance", 0.35, 0.0, 1.0);
        BUILDER.comment("",
                        "A hard knock to the head. Nothing about the head itself changes - what it",
                        "costs is knowing your own condition: while it lasts, every number the mod",
                        "would show you is a question mark and the colours are all you have.",
                        "No medicine touches it and none needs to; it wears off.")
                .translation("triage.configuration.diseases.concussion").push("concussion");
        CONCUSSION_SECONDS = BUILDER.translation("triage.configuration.diseases.concussion.concussionSeconds").defineInRange("concussionSeconds", 75, 1, 100000);
        CONCUSSION_BLOW = BUILDER.comment("Damage to the head in one blow that can rattle it. 0 turns this off.")
                .translation("triage.configuration.diseases.concussion.concussionBlow").defineInRange("concussionBlow", 6.0, 0.0, 1000.0);
        CONCUSSION_CHANCE = BUILDER.comment("Chance that a blow that hard actually does it.")
                .translation("triage.configuration.diseases.concussion.concussionChance").defineInRange("concussionChance", 0.4, 0.0, 1.0);
        BUILDER.pop();

        BUILDER.comment("",
                        "Circulation giving out, which is the half of bleeding this mod did not",
                        "have. Losing blood used to be a penalty you could walk off; shock is",
                        "what kills people who have already been bandaged.",
                        "",
                        "It arrives on volume, not on damage, and it does not run down a clock.",
                        "It lifts when the reason for it is gone - nothing still open, volume back",
                        "under the lower mark - and only while the patient stays still. That is",
                        "the argument it exists to have: they will want to get up and keep going,",
                        "and getting up is what stops them recovering.")
                .translation("triage.configuration.diseases.shock").push("shock");
        SHOCK = BUILDER.translation("triage.configuration.diseases.shock.enabled").define("enabled", true);
        SHOCK_BLOOD_LOSS = BUILDER.comment("Share of blood gone that tips a body into shock.",
                        "",
                        "Real shock starts around a fifth to a third of it, and the first number",
                        "tried here was nearly half - which meant a heavy chest bleed emptied the",
                        "chest and killed the player about fifteen seconds before shock could",
                        "arrive. A threshold nothing ever reaches is not a threshold.")
                .translation("triage.configuration.diseases.shock.bloodLoss").defineInRange("bloodLoss", 0.30, 0.05, 1.0);
        SHOCK_CLEAR_BLOOD_LOSS = BUILDER.comment("And the share it has to come back under before it can lift.",
                        "Deliberately lower than the one above: a body that comes out of shock",
                        "the instant it crosses the same line would flicker in and out of it.")
                .translation("triage.configuration.diseases.shock.clearBloodLoss").defineInRange("clearBloodLoss", 0.18, 0.0, 1.0);
        SHOCK_REST_SECONDS = BUILDER.comment("Seconds of lying still, with nothing bleeding, before it lifts.",
                        "Sprinting and jumping start the count over.")
                .translation("triage.configuration.diseases.shock.restSeconds").defineInRange("restSeconds", 30, 1, 100000);
        SHOCK_BLACKED_PARTS = BUILDER.comment("How many parts have to be gone at once for the body to go into shock",
                        "on their own, with no blood loss at all. This is the crush injury: enough",
                        "of somebody destroyed and the circulation gives up whether or not any of",
                        "it is leaking. 0 turns that route off.")
                .translation("triage.configuration.diseases.shock.blackedParts").defineInRange("blackedParts", 2, 0, 7);
        SHOCK_DOWNED_RATE = BUILDER.comment("How much faster the clock runs on a downed player who is in shock.",
                        "This is where it bites hardest, and it should: shock is the reason two",
                        "people with the same wound have different amounts of time.")
                .translation("triage.configuration.diseases.shock.downedRate").defineInRange("downedRate", 1.5, 1.0, 10.0);
        BUILDER.pop();

        BUILDER.comment("",
                        "Cold that has stopped being a nuisance. It does little by itself; what it",
                        "does is stop blood setting, so every light cut that would have clotted on",
                        "its own keeps running instead.",
                        "",
                        "Blood loss makes it far easier to catch, which is not a lever - a body",
                        "short of blood cannot keep itself warm. The two feed each other, and that",
                        "is why the first thing to do with somebody on a cold floor is get them",
                        "off the cold floor.")
                .translation("triage.configuration.diseases.hypothermia").push("hypothermia");
        HYPOTHERMIA = BUILDER.translation("triage.configuration.diseases.hypothermia.enabled").define("enabled", true);
        HYPOTHERMIA_RISK = BUILDER.comment("Chance per second while freezing, or while soaked somewhere cold.",
                        "Doubled again for every quarter of their blood a player has lost.")
                .translation("triage.configuration.diseases.hypothermia.riskPerSecond").defineInRange("riskPerSecond", 0.015, 0.0, 1.0);
        HYPOTHERMIA_WARM_SECONDS = BUILDER.comment("Seconds of being warm and dry before it lifts. Standing in the",
                        "light of a fire halves it; being cold or wet starts it over.")
                .translation("triage.configuration.diseases.hypothermia.warmSeconds").defineInRange("warmSeconds", 45, 1, 100000);
        BUILDER.pop();

        BUILDER.comment("",
                        "What the wrong blood does. Every player is given a blood type, rerolled",
                        "every time they respawn, and blood may only go into somebody who already",
                        "has every antigen it carries - the real ABO and Rh rules, which make O-",
                        "worth keeping and AB+ a lucky thing to be.",
                        "",
                        "Getting it wrong has to be worse than doing nothing at all, or reading",
                        "the label before hanging the bag would be optional.")
                .translation("triage.configuration.diseases.reaction").push("reaction");
        REACTION_SECONDS = BUILDER.translation("triage.configuration.diseases.reaction.reactionSeconds").defineInRange("reactionSeconds", 90, 1, 100000);
        REACTION_DAMAGE = BUILDER.comment("Damage done to the thorax each second while it runs.")
                .translation("triage.configuration.diseases.reaction.reactionDamagePerSecond").defineInRange("reactionDamagePerSecond", 0.45, 0.0, 100.0);
        REACTION_BLOOD_LOSS = BUILDER.comment("Blood lost each second on top of that: the transfusion running",
                        "backwards as the body destroys what was put into it.")
                .translation("triage.configuration.diseases.reaction.reactionBloodLossPerSecond").defineInRange("reactionBloodLossPerSecond", 0.006, 0.0, 1.0);
        BUILDER.pop();
        BUILDER.pop();

        BUILDER.comment("A broken rib pushed the wrong way, and now there is air where the lung",
                        "wants to be. It is the only injury here with nothing to see: nothing",
                        "bleeds and nothing is bent, and the only way to find it is to read a",
                        "chart. It is also the only one that gets worse the longer it is left",
                        "rather than at a steady rate, because the pressure keeps building.",
                        "",
                        "Nothing heals it and nothing waits it out. A needle between the ribs lets",
                        "the air out, and that is the whole treatment.")
                .translation("triage.configuration.chest").push("chest");
        PNEUMOTHORAX = BUILDER.translation("triage.configuration.chest.enabled").define("enabled", true);
        PNEUMO_BLOW = BUILDER.comment("Damage to the chest in one hit that can do it. Only ever after the",
                        "ribs have already gone - this is what a broken one does next.")
                .translation("triage.configuration.chest.blowThreshold").defineInRange("blowThreshold", 9.0, 0.1, 1000.0);
        PNEUMO_CHANCE = BUILDER.translation("triage.configuration.chest.chance").defineInRange("chance", 0.35, 0.0, 1.0);
        PNEUMO_DAMAGE = BUILDER.comment("Damage to the chest each second when it starts.")
                .translation("triage.configuration.chest.damagePerSecond").defineInRange("damagePerSecond", 0.25, 0.0, 100.0);
        PNEUMO_RAMP_SECONDS = BUILDER.comment("Seconds over which that damage climbs to three times itself. This is",
                        "the deadline: left alone it stops being survivable well before the",
                        "infection chain would have got anywhere.")
                .translation("triage.configuration.chest.rampSeconds").defineInRange("rampSeconds", 75, 1, 100000);
        BUILDER.pop();

        BUILDER.comment("What a wrecked body does to the player it belongs to.")
                .translation("triage.configuration.effects").push("effects");
        EFFECTS = BUILDER.comment("Turn off to keep the health system and drop all the penalties.")
                .translation("triage.configuration.effects.enabled").define("enabled", true);
        REGEN_PENALTY = BUILDER.comment("How strongly a damaged thorax slows healing down.",
                        "0 disables it, 1 means a thorax at half health heals at half speed.")
                .translation("triage.configuration.effects.thoraxRegenPenalty").defineInRange("thoraxRegenPenalty", 1.0, 0.0, 1.0);
        PAINKILLER_SECONDS = BUILDER.comment("How long one painkiller hides the penalties. It heals nothing.")
                .translation("triage.configuration.effects.painkillerSeconds").defineInRange("painkillerSeconds", 45, 1, 100000);
        PAINKILLER_FALLOFF = BUILDER.comment("What each further course is worth, as a share of the one before,",
                        "while the last is still wearing off.",
                        "",
                        "Without this the pills are a button with no cost - swallow one whenever",
                        "anything hurts. At the default the fourth in a row is barely worth",
                        "swallowing, which makes them a thing you spend rather than a thing you",
                        "hold down. 1.0 turns the whole idea off.")
                .translation("triage.configuration.effects.painkillerFalloff").defineInRange("painkillerFalloff", 0.6, 0.05, 1.0);
        PAINKILLER_TOLERANCE_SECONDS = BUILDER.comment("How long it takes to shake off one step of that, once the last",
                        "course has run out. Tolerance does not fall while pills are still in.")
                .translation("triage.configuration.effects.painkillerToleranceSeconds").defineInRange("painkillerToleranceSeconds", 240, 1, 100000);
        HEARTBEAT = BUILDER.comment("Whether a body low on blood can be heard as well as read. Only the",
                        "player it belongs to ever hears it, and it quickens as it gets worse.")
                .translation("triage.configuration.effects.heartbeat").define("heartbeat", true);
        ASSESS = BUILDER.comment("Whether right-clicking another player with an empty hand shows their",
                        "condition without treating them. Sorting the wounded is the thing the",
                        "mod is named after, and it needs somewhere to read them from.")
                .translation("triage.configuration.effects.assess").define("assess", true);
        TAGS = BUILDER.comment("",
                        "Whether a player who has been looked over can be marked, and whether",
                        "those marks show on nameplates along with who is on the floor.",
                        "",
                        "A look is worth nothing to anybody but the person doing the looking",
                        "until there is somewhere to put the answer. This is that somewhere: one",
                        "of four colours, written on a tag, read across a field by everybody",
                        "else. The mod never decides which colour is correct and never will -",
                        "that decision is the thing the whole feature exists to make somebody",
                        "make.",
                        "",
                        "Off, tags are inert paper and nobody's nameplate says anything.")
                .translation("triage.configuration.effects.tags").define("tags", true);
        TAG_STALE_SECONDS = BUILDER.comment("How long a mark reads as current. After this it keeps its colour and",
                        "gains a question mark, and the mod stops claiming to know how old it is.",
                        "",
                        "The point of the number is that a mark is a snapshot of a body that has",
                        "carried on being a body. Set it long and the group will trust old notes;",
                        "set it short and somebody has to keep walking back.")
                .translation("triage.configuration.effects.tagStaleSeconds").defineInRange("tagStaleSeconds", 120, 5, 100000);
        MINIGAME = BUILDER.comment("Whether treating a wound opens the screen where you have to do it by hand.",
                        "Turn it off and medicine works the moment it is used, on whichever",
                        "part needs it most. Painkillers never open it either way.")
                .translation("triage.configuration.effects.minigame").define("minigame", true);
        INTERRUPT_DAMAGE = BUILDER.comment("How much damage it takes to knock a player off a treatment, measured",
                        "on the vanilla health bar. A scratch or a tick of fire should not cost",
                        "a bandage; being set upon should.",
                        "",
                        "0 means treatments are never interrupted. Swallowing pills - painkillers",
                        "and antibiotics - is never interrupted either way, because being ill is",
                        "exactly the state you cannot fight your way out of first.")
                .translation("triage.configuration.effects.interruptDamage").defineInRange("interruptDamage", 3.0, 0.0, 100.0);
        DOWNED = BUILDER.comment("",
                        "Whether emptying a vital part puts the player on the floor with a clock",
                        "running instead of killing them outright. Somebody else can open them up",
                        "with a surgical kit and get them back; nobody coming is what turns it",
                        "into a death.",
                        "",
                        "On your own this is a dramatic last few seconds rather than a rescue,",
                        "so a solo player may well want it off.")
                .translation("triage.configuration.effects.downed").define("downed", true);
        DOWNED_SECONDS = BUILDER.comment("How long there is to save them.")
                .translation("triage.configuration.effects.downedSeconds").defineInRange("downedSeconds", 45, 1, 100000);
        DOWNED_IGNORED = BUILDER.comment("Whether mobs lose interest in a player who is already down.",
                        "With this off, whatever put them there simply finishes the job and the",
                        "rescue the whole feature exists for never gets to happen.")
                .translation("triage.configuration.effects.downedIgnoredByMobs").define("downedIgnoredByMobs", true);
        DOWNED_DAMAGE_SECONDS = BUILDER.comment("Seconds taken off the clock per point of damage dealt to a player who",
                        "is already down. Without this, lying there is the safest place in the",
                        "game: nothing can finish you and only the timer can.")
                .translation("triage.configuration.effects.downedDamageSeconds").defineInRange("downedDamageSeconds", 3.0, 0.0, 1000.0);
        STABILISE_SECONDS = BUILDER.comment("",
                        "How long a dressing holds the clock on somebody who is down.",
                        "",
                        "Packing a wound does not stand them up - only surgery does that - but",
                        "it stops the count while it holds. Without it a medic sixty seconds",
                        "away is a medic who arrives to a body, and the whole rescue only ever",
                        "worked at knife range.",
                        "",
                        "A hit undoes the packing. 0 turns the whole thing off.")
                .translation("triage.configuration.effects.stabiliseSeconds").defineInRange("stabiliseSeconds", 30, 0, 100000);
        STABILISE_FALLOFF = BUILDER.comment("What each further hold on the same downing is worth, as a share of",
                        "the one before. This is what stops bandages being immortality: at the",
                        "default the second hold is half as long and the fourth is barely worth",
                        "the bandage.")
                .translation("triage.configuration.effects.stabiliseFalloff").defineInRange("stabiliseFalloff", 0.5, 0.0, 1.0);
        RAGDOLL = BUILDER.comment("",
                        "Whether a downed player goes limp for real, when Sable: Ragdolls is",
                        "installed. Does nothing at all without it, and Triage never asks for it.",
                        "",
                        "Triage's own version of lying down is a forced swimming pose: the same",
                        "shape whether you were blown off a roof or bled out standing still. With",
                        "the physics mod there, the body falls instead - and it cannot be got up",
                        "from, which is the rule this mod has always had about being down, only",
                        "now it is the same rule the player can see.",
                        "",
                        "Off, the pose comes back and the physics mod is left alone.")
                .translation("triage.configuration.effects.ragdoll").define("ragdoll", true);
        CARRY = BUILDER.comment("",
                        "Whether a downed player can be picked up and carried: Shift + right click",
                        "with an empty hand, and the give-up key sets them down again.",
                        "",
                        "Without it the only surgery available is surgery where they fell - in the",
                        "fire, in the open, in front of whatever put them there. It costs the",
                        "carrier their hands and most of their speed, so it is a decision rather",
                        "than a free extra.")
                .translation("triage.configuration.effects.carry").define("carry", true);
        CARRY_SLOW = BUILDER.comment("How much slower the carrier moves. A body is heavy.")
                .translation("triage.configuration.effects.carrySlow").defineInRange("carrySlow", 0.45, 0.0, 0.95);
        DOWNED_CARRIED_RATE = BUILDER.comment("How fast the clock runs while somebody is carrying them, as a share",
                        "of normal. Being carried is not treatment and heals nothing, but somebody",
                        "has hold of them, and at the full rate carrying anybody anywhere would",
                        "cost more time than it could ever buy.",
                        "",
                        "0 stops the clock entirely while they are being carried.")
                .translation("triage.configuration.effects.downedCarriedRate").defineInRange("downedCarriedRate", 0.5, 0.0, 1.0);
        AIRWAY = BUILDER.comment("",
                        "Whether an unconscious body stops breathing for itself.",
                        "",
                        "A player on the floor is not holding their own airway open, and after a",
                        "while air stops getting past. It costs nothing to fix and cannot be",
                        "fixed once: somebody has to kneel down, open it, and come back and do it",
                        "again. It is the only thing in the mod that asks for a person rather",
                        "than for an item, which is why it is here.",
                        "",
                        "You can hear it from a few blocks away. That is deliberate - everything",
                        "else about a body has to be gone and read on purpose, and somebody who",
                        "cannot breathe is a noise rather than a fact.")
                .translation("triage.configuration.effects.airway").define("airway", true);
        AIRWAY_SECONDS = BUILDER.comment("How long air keeps getting in with nobody holding it open. The count",
                        "starts when they go down and goes back to nothing every time somebody",
                        "opens it.")
                .translation("triage.configuration.effects.airwaySeconds").defineInRange("airwaySeconds", 20, 1, 100000);
        AIRWAY_DOWNED_RATE = BUILDER.comment("How much faster the clock runs while they are not breathing. This is",
                        "the largest single thing that can be done about somebody dying on the",
                        "floor, and it is free, which is exactly how it is in life.",
                        "",
                        "It counts against the arrest window at the same rate, so it matters more",
                        "after the heart stops rather than less: compressions on a body no air is",
                        "reaching only move the same blood round.")
                .translation("triage.configuration.effects.airwayDownedRate").defineInRange("airwayDownedRate", 2.0, 1.0, 100.0);
        CPR = BUILDER.comment("",
                        "Whether the clock running out stops the heart instead of ending the",
                        "player.",
                        "",
                        "In arrest there is no clock left of their own - what is left is a window",
                        "somebody else holds open by hand, one round of compressions at a time.",
                        "Compressions never stand anybody up: they buy the minute in which",
                        "somebody with a surgical kit can arrive, and nothing else.",
                        "",
                        "Off, the clock running out kills as it used to.")
                .translation("triage.configuration.effects.cpr").define("cpr", true);
        ARREST_SECONDS = BUILDER.comment("How long one round of compressions buys.")
                .translation("triage.configuration.effects.arrestSeconds").defineInRange("arrestSeconds", 25, 1, 100000);
        CPR_FALLOFF = BUILDER.comment("What each further round is worth, as a share of the one before. This is",
                        "what stops one pair of patient hands being immortality: at the default",
                        "the fourth round is worth about a fifth of the first.")
                .translation("triage.configuration.effects.cprFalloff").defineInRange("cprFalloff", 0.6, 0.0, 1.0);
        CPR_RIB_CHANCE = BUILDER.comment("Chance per round of breaking one of their ribs. A chest pushed hard",
                        "enough to move blood is a chest that cracks, and the patient wakes up",
                        "with the evidence - which is the price of the answer, not a punishment",
                        "for giving it.")
                .translation("triage.configuration.effects.cprRibChance").defineInRange("cprRibChance", 0.3, 0.0, 1.0);
        PRESSURE = BUILDER.comment("",
                        "Whether a bare hand pressed on a wound stops it bleeding while it is",
                        "there.",
                        "",
                        "The first thing anybody is taught about bleeding, and the last thing",
                        "this mod had: before it, a bleeding teammate and no bandage was a",
                        "bleeding teammate and nothing at all. It closes nothing and heals",
                        "nothing - it buys the walk to the dressing, at the price of standing",
                        "there. A light bleed goes on clotting underneath, so one held long",
                        "enough does close on its own.")
                .translation("triage.configuration.effects.pressure").define("pressure", true);
        PRESSURE_SECONDS = BUILDER.comment("How long one hand stays on before it has to be put back.")
                .translation("triage.configuration.effects.pressureSeconds").defineInRange("pressureSeconds", 12, 1, 100000);
        SCHIZOPHRENIA = BUILDER.comment("",
                        "Whether some bodies arrive with it.",
                        "",
                        "The only condition in the mod that nobody catches and nothing",
                        "caused: it is rolled with the blood type, at the same moment, and",
                        "for the same reason - it is a fact about a person rather than an",
                        "event in their day.",
                        "",
                        "What it does is not what films do with it. The voices comment and",
                        "criticise; they never give orders, because the version that does is",
                        "the most repeated untruth about this illness. The heaviest part of",
                        "it here is the part with no drama in it: a body that does not want",
                        "to do anything, and mends more slowly for it.",
                        "",
                        "It is treatable and not curable. A course holds it down while it is",
                        "being taken and costs enough that stopping is a real temptation.",
                        "",
                        "On by default, as it was when it was added. Some players do not",
                        "want a condition that cannot be caught, prevented or examined",
                        "away; turn it off if the group does not want that story in their",
                        "world.")
                .translation("triage.configuration.effects.schizophrenia").define("schizophrenia", true);
        SCHIZOPHRENIA_CHANCE = BUILDER.comment("The share of bodies that have it, when it is enabled.",
                        "",
                        "The real figure is under one in a hundred, which in a game of four",
                        "people means nobody ever meets it. The default here is deliberately",
                        "higher than that: it is a condition some people have, and a rate",
                        "nobody ever meets is not a feature.",
                        "",
                        "1.0 gives it to everybody. 0.0 is the same as turning it off.")
                .translation("triage.configuration.effects.schizophreniaChance").defineInRange("schizophreniaChance", 0.1, 0.0, 1.0);
        ANTIPSYCHOTIC_SECONDS = BUILDER.comment("How long one course holds the symptoms down.")
                .translation("triage.configuration.effects.antipsychoticSeconds").defineInRange("antipsychoticSeconds", 600, 1, 100000);
        REBOUND_SECONDS = BUILDER.comment("How long the symptoms come back worse for after a course runs out.",
                        "",
                        "This is the reason people stop taking it and then wish they had not,",
                        "and it is why the choice in this mod is not 'treat or do not' but",
                        "'which of the two do you put up with'.")
                .translation("triage.configuration.effects.reboundSeconds").defineInRange("reboundSeconds", 60, 0, 100000);
        DELIRIUM = BUILDER.comment("",
                        "Whether a body that has stopped supplying its head starts getting",
                        "things wrong.",
                        "",
                        "Not an illness anybody catches and not one anybody treats. It is as",
                        "true as its causes - blood gone, air not getting in, a temperature",
                        "that has stopped the works, or the body poisoning itself - and it",
                        "goes when they do. The point of it is that it is the only thing in",
                        "the mod that makes your own chart lie to you: concussion makes the",
                        "numbers coarse, this makes them wrong. On somebody else it reads as",
                        "'whatever is wrong with them has reached their head'.",
                        "",
                        "You will also hear things that are not there. Only you.")
                .translation("triage.configuration.effects.delirium").define("delirium", true);
        DELIRIUM_BLOOD_LOSS = BUILDER.comment("How much volume has to be gone before the head starts missing things.")
                .translation("triage.configuration.effects.deliriumBloodLoss").defineInRange("deliriumBloodLoss", 0.5, 0.0, 1.0);
        DELIRIUM_CLEAR_SECONDS = BUILDER.comment("How long after the cause is dealt with before somebody is back in",
                        "their own senses. Short, and not zero: a volume sitting on the",
                        "threshold would otherwise switch them in and out once a second.")
                .translation("triage.configuration.effects.deliriumClearSeconds").defineInRange("deliriumClearSeconds", 20, 0, 100000);
        WHISPER_SECONDS = BUILDER.comment("Roughly how many seconds between things that are not said, while not on",
                        "a course. Twice as often in the stretch after one runs out.",
                        "",
                        "Four kinds, in equal measure: a noise from where nothing is, a line",
                        "of chat with nobody attached to it, a word across the middle of the",
                        "screen, and a message with another player's name on it that they did",
                        "not write. None of them is an instruction - real voices comment and",
                        "criticise, and the ones that give orders are a thing films made up.",
                        "",
                        "All of it happens on the one client. Nobody else hears, reads or",
                        "sees any of it, least of all the player whose name gets borrowed.",
                        "",
                        "0 turns them off and leaves the figures.")
                .translation("triage.configuration.effects.whisperSeconds").defineInRange("whisperSeconds", 25, 0, 100000);
        PHANTOM_SECONDS = BUILDER.comment("Roughly how many seconds between somebody who is not there, while not",
                        "on a course. Twice as often in the stretch after one runs out.",
                        "",
                        "It is a person and not a monster, it looks like somebody really on",
                        "the server, it never comes closer and it is gone the moment you turn",
                        "towards it. Nobody else can see it, because there is nothing to see.",
                        "",
                        "0 turns them off and leaves the voices.")
                .translation("triage.configuration.effects.phantomSeconds").defineInRange("phantomSeconds", 180, 0, 100000);
        CATATONIA_CHANCE = BUILDER.comment("Chance per second of the illness stopping somebody outright, while",
                        "they are not on a course. Three times that in the stretch after one",
                        "has run out.",
                        "",
                        "It takes movement away completely and passes on its own. Nothing in",
                        "the box touches it, which makes it the second thing in this mod",
                        "whose only answer is somebody else being there.")
                .translation("triage.configuration.effects.catatoniaChance").defineInRange("catatoniaChance", 0.0008, 0.0, 1.0);
        CATATONIA_SECONDS = BUILDER.comment("How long it lasts.")
                .translation("triage.configuration.effects.catatoniaSeconds").defineInRange("catatoniaSeconds", 30, 1, 100000);
        NEUROLEPTIC_CHANCE = BUILDER.comment("",
                        "Chance that a course causes the reaction that kills.",
                        "",
                        "Rare, real, and the sharpest form of this mod's oldest argument:",
                        "every treatment is a trade, and this is the one where the trade can",
                        "be the thing that ends you. There is no antidote - stop the course",
                        "and get cold.")
                .translation("triage.configuration.effects.neurolepticChance").defineInRange("neurolepticChance", 0.03, 0.0, 1.0);
        NEUROLEPTIC_DAMAGE = BUILDER.comment("What it costs the whole body per second while it runs.")
                .translation("triage.configuration.effects.neurolepticDamage").defineInRange("neurolepticDamage", 0.5, 0.0, 100.0);
        NEUROLEPTIC_COOL_SECONDS = BUILDER.comment("How long off the course and cold - in water, in the rain, in the",
                        "snow - before it breaks.")
                .translation("triage.configuration.effects.neurolepticCoolSeconds").defineInRange("neurolepticCoolSeconds", 25, 1, 100000);
        ORGANS = BUILDER.comment("",
                        "Whether a hard blow to a torso can tear something inside it.",
                        "",
                        "The injury this mod did not have and trauma medicine is mostly about:",
                        "the one that does not bleed anywhere anybody can see. From the moment",
                        "it happens the patient is losing blood into their own abdomen or chest,",
                        "and nothing outside them shows it. No wound on the figure. No dressing",
                        "that helps. No clot, ever, because there is nothing outside for a clot",
                        "to close.",
                        "",
                        "It is on the chart, because the chart has never lied - it is simply not",
                        "on the FIGURE, the same as a crushed lung. This is the injury found by",
                        "reading rather than by looking, and it is the one that kills somebody",
                        "who was walking and talking a minute ago.",
                        "",
                        "One thing stops it, and it is a surgeon.")
                .translation("triage.configuration.effects.organs").define("organs", true);
        ORGAN_BLOW = BUILDER.comment("How hard a single blow has to be before it can tear something.")
                .translation("triage.configuration.effects.organBlow").defineInRange("organBlow", 11.0, 0.0, 1000.0);
        ORGAN_CHANCE = BUILDER.comment("And the chance that it does.")
                .translation("triage.configuration.effects.organChance").defineInRange("organChance", 0.30, 0.0, 1.0);
        ORGAN_HEART_STOP_CHANCE = BUILDER.comment("",
                        "Chance that tearing the heart stops it, rather than merely",
                        "bleeding into the chest.",
                        "",
                        "The heart is the one organ in the mod that does not give",
                        "anybody three minutes. It tears at the same threshold as the",
                        "rest, and when it does this is the chance the player goes",
                        "down with an arrest instead of simply losing volume. CPR",
                        "and a surgeon are the only answers, so with CPR disabled this",
                        "does nothing and the tear is an ordinary - which is to say",
                        "still very bad - internal bleed.")
                .translation("triage.configuration.effects.heartStopChance").defineInRange("heartStopChance", 0.35, 0.0, 1.0);
        ORGAN_BLEED = BUILDER.comment("Share of volume lost each second while something is torn.",
                        "",
                        "Between a light bleed and a heavy one on purpose: it is slower than an",
                        "artery and it never, ever stops, which over a few minutes is worse.")
                .translation("triage.configuration.effects.organBleedPerSecond").defineInRange("organBleedPerSecond", 0.007, 0.0, 1.0);
        ORGAN_DAMAGE = BUILDER.comment("What a torn organ costs its own part each second, on top of that.")
                .translation("triage.configuration.effects.organDamagePerSecond").defineInRange("organDamagePerSecond", 0.12, 0.0, 100.0);
        ORGAN_SEPSIS = BUILDER.comment("Chance per second that a torn gut turns into blood poisoning.",
                        "",
                        "The one organ whose danger is not the bleeding. What comes out of a gut",
                        "is not blood, and the thing that kills people is three minutes later.")
                .translation("triage.configuration.effects.organSepsisChance").defineInRange("organSepsisChance", 0.012, 0.0, 1.0);
        ORGAN_HEAL_SECONDS = BUILDER.comment("How long a bruised one takes to settle on its own.",
                        "Only the bruised kind. The torn kind waits for a surgeon and nothing else.")
                .translation("triage.configuration.effects.organHealSeconds").defineInRange("organHealSeconds", 150, 1, 100000);
        REPERFUSION = BUILDER.comment("",
                        "Whether letting a long-strapped limb go costs the rest of the body.",
                        "",
                        "The real complication of a tourniquet, and the one nobody expects: what",
                        "has been building up in a limb with no blood moving through it goes",
                        "everywhere the moment the strap comes off. The longer it was on, the",
                        "more there is of it.",
                        "",
                        "Which turns the strap into the argument it should have been all along.",
                        "Taken off early it costs nothing. Left until the clock is nearly out, it",
                        "is dangerous to take off at all - and the limb under it is dying anyway.",
                        "That is the whole road from a strap to a saw, and it is the road the",
                        "real one goes down.",
                        "",
                        "Nothing at all under a third of the clock. A bandage over the wound",
                        "releases the strap too, so it is charged the same: releasing is",
                        "releasing.")
                .translation("triage.configuration.effects.reperfusion").define("reperfusion", true);
        REPERFUSION_DAMAGE = BUILDER.comment("What a strap left the whole clock costs the body when it comes off.",
                        "Spread over everything, and scaled by how much of the clock had run.")
                .translation("triage.configuration.effects.reperfusionDamage").defineInRange("reperfusionDamage", 7.0, 0.0, 1000.0);
        DRESSING_STUCK_SECONDS = BUILDER.comment("How long before a dressing has dried onto what is under it.",
                        "",
                        "Fingers can take a fresh one off cleanly. Past this it is stuck to the",
                        "wound by the thing that was trying to close it, and pulling it away",
                        "takes that with it - so a dressing left on is not only dirty, it is",
                        "harder to get off. Scissors do not care either way, which is the whole",
                        "reason to carry a pair.")
                .translation("triage.configuration.effects.dressingStuckSeconds").defineInRange("dressingStuckSeconds", 120, 1, 100000);
        ANAESTHESIA = BUILDER.comment("",
                        "Whether somebody can be put under, and whether it matters.",
                        "",
                        "Two treatments in this mod are done to a patient who is awake and can",
                        "feel every part of it: a surgical kit opening them up, and a saw taking",
                        "their leg off. Until now the mod did not mention that. It does now, and",
                        "the mention has a price attached: operating on somebody conscious caps",
                        "how well it can possibly go, hurts them on top of the operation, and can",
                        "put them into shock from the pain alone.",
                        "",
                        "Nobody can put themselves under and then operate - being under is being",
                        "unable to do anything - so this is the first item in the mod whose whole",
                        "value is what it lets somebody ELSE do properly.",
                        "",
                        "Off, an operation is what it was: nobody minds, and nobody is asked to.")
                .translation("triage.configuration.effects.anaesthesia").define("anaesthesia", true);
        ANAESTHESIA_SECONDS = BUILDER.comment("How long a good dose keeps somebody under, in seconds.",
                        "A poor one is worth a fraction of it.")
                .translation("triage.configuration.effects.anaesthesiaSeconds").defineInRange("anaesthesiaSeconds", 90, 1, 100000);
        ANAESTHESIA_ARREST = BUILDER.comment("Chance a badly given dose stops their breathing altogether.",
                        "",
                        "Scaled by how badly. This is the only thing in the mod that can kill a",
                        "patient by working too well, and the answer to it is the answer the mod",
                        "already has: hands on the chest.")
                .translation("triage.configuration.effects.anaesthesiaArrestChance").defineInRange("anaesthesiaArrestChance", 0.35, 0.0, 1.0);
        AWAKE_QUALITY_CAP = BUILDER.comment("The best an operation on a conscious patient can go, however well",
                        "the hands did. They are moving, and no amount of skill fixes that.")
                .translation("triage.configuration.effects.awakeQualityCap").defineInRange("awakeQualityCap", 0.5, 0.0, 1.0);
        AWAKE_DAMAGE = BUILDER.comment("What that costs the part, on top of the operation itself.")
                .translation("triage.configuration.effects.awakeDamage").defineInRange("awakeDamage", 3.0, 0.0, 1000.0);
        DRESSINGS = BUILDER.comment("",
                        "Whether a dressing stays on, and goes bad if it is left there.",
                        "",
                        "A bandage used to be an event: it went on, the bleeding stopped,",
                        "and that was the end of it. Now it is a thing on somebody. While it",
                        "is fresh it is the best item in the box - nothing under a clean",
                        "dressing can go bad at all, which is the one preventive thing in the",
                        "whole mod. Left on past its time it becomes what is keeping the dirt",
                        "in, which is the commonest way a treated wound turns into an infected",
                        "one, and the infection risk starts running with no wait at all.",
                        "",
                        "The answer is another bandage: the dirty one comes off with it.",
                        "It also comes off by itself once there is nothing left underneath",
                        "worth dressing, so nobody is changing the bandage on a healed arm.",
                        "",
                        "Off, a bandage goes back to being an event and nothing is tracked.")
                .translation("triage.configuration.effects.dressings").define("dressings", true);
        DRESSING_CLEAN_SECONDS = BUILDER.comment("How long a dressing stays clean, in seconds.")
                .translation("triage.configuration.effects.dressingCleanSeconds").defineInRange("dressingCleanSeconds", 480, 1, 100000);
        NECROSIS = BUILDER.comment("",
                        "Whether a limb that has stopped being fed can die on the body.",
                        "",
                        "Two roads reach it, and a player chooses both of them. A tourniquet",
                        "stops the bleeding and starts a clock; something festering in a limb",
                        "nobody cleaned starts the same one. Neither is a surprise and both are",
                        "reversible right up until they are not - the count runs back down as",
                        "soon as the reason for it is dealt with.",
                        "",
                        "What is at the end of it is the first state in the mod that nothing",
                        "repairs. A dead limb takes no treatment, mends nothing, and poisons",
                        "the body it is attached to until it comes off.",
                        "",
                        "Off, a strap is what it was before: a slow cost with no deadline.")
                .translation("triage.configuration.effects.necrosis").define("necrosis", true);
        STRAP_DEAD_SECONDS = BUILDER.comment("How long a tourniquet can stay on before what is under it dies.",
                        "",
                        "This is the number that gives the strap its teeth. It is generous by the",
                        "standards of the real thing and short by the standards of forgetting.")
                .translation("triage.configuration.effects.tourniquetDeadSeconds").defineInRange("tourniquetDeadSeconds", 180, 1, 100000);
        ROT_DEAD_SECONDS = BUILDER.comment("How long a limb can carry a severe infection before it dies.")
                .translation("triage.configuration.effects.rotDeadSeconds").defineInRange("rotDeadSeconds", 240, 1, 100000);
        DEAD_LIMB_DRAIN = BUILDER.comment("Damage the chest takes each second for every dead limb still attached.",
                        "",
                        "Half again what a merely blacked-out one costs, and the smaller half of",
                        "what a dead limb does: the rest of it arrives as the blood going bad,",
                        "below. Between them they are meant to be an emergency with a way out of",
                        "it - long enough to find a saw, put a strap on and use both.")
                .translation("triage.configuration.effects.deadLimbDrain").defineInRange("deadLimbDrain", 0.20, 0.0, 100.0);
        DEAD_LIMB_SEPSIS = BUILDER.comment("Chance per second that a dead limb starts poisoning the blood.")
                .translation("triage.configuration.effects.deadLimbSepsisChance").defineInRange("deadLimbSepsisChance", 0.010, 0.0, 1.0);
        AMPUTATION = BUILDER.comment("",
                        "Whether a dead limb can be taken off.",
                        "",
                        "The only treatment in the mod that removes something rather than",
                        "repairing it, and the only one whose result is permanent. It is offered",
                        "on nothing but a limb that has already died, so it is never the clever",
                        "shortcut - by the time the saw is an option, everything else has already",
                        "stopped being one.",
                        "",
                        "Off, a dead limb stays on the body and keeps costing, which is a slower",
                        "death and nothing else.")
                .translation("triage.configuration.effects.amputation").define("amputation", true);
        AMPUTATION_BLOOD_LOSS = BUILDER.comment("Share of volume the operation costs, done properly.")
                .translation("triage.configuration.effects.amputationBloodLoss").defineInRange("amputationBloodLoss", 0.12, 0.0, 1.0);
        AMPUTATION_OPEN_BLOOD_LOSS = BUILDER.comment("Extra volume gone when it is done with no strap above the cut.",
                        "",
                        "On top of a heavy bleed on the stump that will not clot. Sawing through",
                        "an unstrapped limb is the fastest way to kill somebody in this mod and",
                        "it is meant to be: the strap first is the whole rule of the operation.")
                .translation("triage.configuration.effects.amputationOpenBloodLoss").defineInRange("amputationOpenBloodLoss", 0.20, 0.0, 1.0);
        PROSTHETICS = BUILDER.comment("",
                        "Whether a stump can have something fitted to it.",
                        "",
                        "A wooden leg and a hook. Neither is the limb and neither pretends to be:",
                        "they hand back part of what was lost and stop there. Without them the",
                        "worst road in the mod ends in a wall, which is honest and is not a game.")
                .translation("triage.configuration.effects.prosthetics").define("prosthetics", true);
        PROSTHETIC_RATIO = BUILDER.comment("How much of a working limb a plain fitted one counts as.")
                .translation("triage.configuration.effects.prostheticRatio").defineInRange("prostheticRatio", 0.45, 0.0, 1.0);
        PROSTHETIC_RATIO_FINE = BUILDER.comment("And how much one with a joint in it counts as.",
                        "",
                        "The difference between a peg and a leg somebody has spent money on is a",
                        "knee, and what a knee gives back is most of the difference between",
                        "walking and getting about. It is not a limb and this number should never",
                        "reach one: the mod has exactly one permanent thing in it and softening",
                        "it to nothing would be taking that back.")
                .translation("triage.configuration.effects.prostheticRatioFine").defineInRange("prostheticRatioFine", 0.75, 0.0, 1.0);
        RABIES = BUILDER.comment("",
                        "Whether a bite from an animal can give somebody rabies.",
                        "",
                        "Every other illness here is a race you can join late. A fever gives you",
                        "minutes; sepsis gives you a fight; both can be answered after they have",
                        "announced themselves.",
                        "",
                        "This one cannot, and that is why it is here. It is the only condition in",
                        "medicine where the treatment works perfectly right up until the first",
                        "symptom and then does not work at all, ever, for anybody.",
                        "",
                        "So it is silent. It sits there showing nothing, and a course of",
                        "antibiotics during that time takes it away completely. Which makes it the",
                        "only reason in the mod to treat somebody who is not ill - and the answer",
                        "to a question every player asks once: what happens if I wait and see.",
                        "",
                        "off - a bite is a bite.")
                .translation("triage.configuration.effects.rabies").define("rabies", true);
        RABIES_CHANCE = BUILDER.comment("Chance a bite from a wolf, fox, bat or bear carries it.")
                .translation("triage.configuration.effects.rabiesChance").defineInRange("rabiesChance", 0.14, 0.0, 1.0);
        RABIES_SECONDS = BUILDER.comment("How long it stays silent. This is all the time anybody gets.")
                .translation("triage.configuration.effects.rabiesSeconds").defineInRange("rabiesSeconds", 300, 1, 1000000);
        FRENZY_SECONDS = BUILDER.comment("And how long the end of it lasts once it shows.")
                .translation("triage.configuration.effects.frenzySeconds").defineInRange("frenzySeconds", 90, 1, 1000000);
        FRENZY_DAMAGE = BUILDER.comment("What it takes off the head each second, growing as it goes.")
                .translation("triage.configuration.effects.frenzyDamage").defineInRange("frenzyDamage", 0.35, 0.0, 100.0);
        DEAFNESS_SECONDS = BUILDER.comment("",
                        "How long a blast leaves somebody deaf, in seconds.",
                        "",
                        "The injury everybody near an explosion has and nobody counts. No",
                        "treatment and none needed - it wears off. What it costs is the one sense",
                        "this mod uses to warn you about yourself: a heartbeat you cannot hear",
                        "stops telling you how much blood is left, and a pulse has to be counted",
                        "by eye instead.",
                        "",
                        "And it costs the medic. A deafened patient cannot hear the question, so",
                        "asking stops working on exactly the casualty who was closest to it.",
                        "",
                        "0 turns it off.")
                .translation("triage.configuration.effects.deafnessSeconds").defineInRange("deafnessSeconds", 90, 0, 100000);
        BLAST = BUILDER.comment("",
                        "Whether an explosion does what an explosion does.",
                        "",
                        "Until this existed the mod treated a blast as a big fire: damage smeared",
                        "over the body, nothing broken, nothing bleeding, nothing to treat. Blast",
                        "injury is the one pattern trauma medicine has a whole vocabulary for, and",
                        "it arrives in four kinds at once - the wave, the fragments, the throw and",
                        "the heat.",
                        "",
                        "off - an explosion goes back to being a big fire.")
                .translation("triage.configuration.effects.blast").define("blast", true);
        TEAR_OFF_BLOW = BUILDER.comment("",
                        "How hard a blow takes a dead limb clean off, in damage.",
                        "",
                        "Dead flesh is not attached to anything. It is held on by what has already",
                        "stopped working, so a hard enough knock takes it away - which turns the",
                        "necrosis clock from a deadline into a warning. Past the end of it you are",
                        "not only carrying something useless, you are carrying something that can",
                        "be knocked off you.")
                .translation("triage.configuration.effects.tearOffBlow").defineInRange("tearOffBlow", 6.0, 0.0, 1000.0);
        TEAR_OFF_CHANCE = BUILDER.comment("And how likely that is, per blow that hard.")
                .translation("triage.configuration.effects.tearOffChance").defineInRange("tearOffChance", 0.5, 0.0, 1.0);
        BLAST_TEAR_OFF = BUILDER.comment("",
                        "Whether a blast can take a limb off outright.",
                        "",
                        "The worst thing in this mod and the only one that happens to somebody",
                        "without anybody deciding it. Everything else that ends in a stump is a",
                        "decision - a strap left on too long, a saw somebody picked up. This is a",
                        "distance.",
                        "",
                        "So it is kept to the middle of a blast and kept unlikely even there. A",
                        "rule that takes an arm off at the edge of a radius makes explosions",
                        "unplayable; one that takes an arm off at the centre makes standing next",
                        "to a charge a thing people do once.",
                        "",
                        "What it leaves is worse than a saw leaves. A cut is straight and somebody",
                        "chose where; this is a tear, there is no strap above it because nobody",
                        "knew it was coming, and it is the only wound here that arrives already",
                        "emptying somebody.",
                        "",
                        "off - a blast can only break what it finds.")
                .translation("triage.configuration.effects.blastTearOff").define("blastTearOff", true);
        BLAST_TEAR_CHANCE = BUILDER.comment("Chance of it, dead in the middle of a blast.")
                .translation("triage.configuration.effects.blastTearChance").defineInRange("blastTearChance", 0.35, 0.0, 1.0);
        BLAST_FRAGMENTS = BUILDER.comment("",
                        "Whether a blast throws fragments that stay in people.",
                        "",
                        "Several small wounds at once, in whatever was facing it. They are the",
                        "same problem an arrow is and come out the same way; the chart simply",
                        "calls them what they are.")
                .translation("triage.configuration.effects.blastFragments").define("blastFragments", true);
        BLAST_WAVE = BUILDER.comment("",
                        "Whether the pressure wave itself injures.",
                        "",
                        "This is the half worth having. Pressure does nothing at all to muscle -",
                        "what it destroys is anything with gas in it, lungs first and bowel after.",
                        "It leaves NO MARK. Somebody walks away from a blast talking and drowns in",
                        "their own chest twenty minutes later, and the only sign in between is a",
                        "breathing rate that will not settle.",
                        "",
                        "And armour does not touch it, which is the rule that makes the whole",
                        "thing worth building: plate is a very good answer to a piece of metal",
                        "travelling at somebody and no answer at all to the air around it. The",
                        "hardest casualty to find is the one in full armour.",
                        "",
                        "off - a blast is fragments and heat and nothing else.")
                .translation("triage.configuration.effects.blastWave").define("blastWave", true);
        BLAST_ARMOUR_STOP = BUILDER.comment("How much of the fragments full armour stops.")
                .translation("triage.configuration.effects.blastArmourStop").defineInRange("blastArmourStop", 0.85, 0.0, 1.0);
        BLAST_LUNG_CHANCE = BUILDER.comment("Chance per lung of the wave tearing it, at the centre.")
                .translation("triage.configuration.effects.blastLungChance").defineInRange("blastLungChance", 0.45, 0.0, 1.0);
        BLAST_GUT_CHANCE = BUILDER.comment("Chance of the wave tearing bowel, at the centre.")
                .translation("triage.configuration.effects.blastGutChance").defineInRange("blastGutChance", 0.25, 0.0, 1.0);
        BLAST_BURN_CHANCE = BUILDER.comment("Chance of a burn, scaled by how close.")
                .translation("triage.configuration.effects.blastBurnChance").defineInRange("blastBurnChance", 0.5, 0.0, 1.0);
        MENDING = BUILDER.comment("",
                        "Whether a part that has been operated on has to be left alone.",
                        "",
                        "Everything in this mod has ended at the moment the treatment did. A",
                        "surgeon closed somebody up and they stood straight back up and went to",
                        "work, which is the one thing about surgery nobody who has had any",
                        "believes.",
                        "",
                        "This is not a debuff and nothing is taken away by it. A mending arm swings",
                        "exactly as well as it did an hour ago. It is a wound that has not finished",
                        "being one: work the part hard - sprint on a leg, swing or mine with an arm",
                        "- and it comes open again, and everything that was true before the",
                        "operation is true afresh.",
                        "",
                        "Which makes it the first rule here whose answer is to do nothing. There is",
                        "no item for it and there is not going to be one.",
                        "",
                        "off - a treatment ends when it ends.")
                .translation("triage.configuration.effects.mending").define("mending", true);
        MENDING_SECONDS = BUILDER.comment("How long a part needs after somebody has been inside it.")
                .translation("triage.configuration.effects.mendingSeconds").defineInRange("mendingSeconds", 90, 1, 100000);
        MENDING_TEAR_CHANCE = BUILDER.comment("Chance per second of working it open, while working it.")
                .translation("triage.configuration.effects.mendingTearChance").defineInRange("mendingTearChance", 0.10, 0.0, 1.0);
        SLEEPLESS = BUILDER.comment("",
                        "Whether being awake for days costs anything.",
                        "",
                        "The game has always counted this and has always done one thing with it,",
                        "which is to send birds. That is a threat from outside. What being awake",
                        "for three days actually does is from the inside, and it is the best",
                        "documented thing on this list: a tired pair of hands is a worse pair of",
                        "hands, and the person they belong to is the last to know.",
                        "",
                        "So it is the fifth thing that decides a treatment, after the room, the",
                        "instrument, the second pair of hands and what this pair has just been",
                        "doing - and the only one of the five with no answer available in the next",
                        "half minute. It also costs two other things that are just as real: a body",
                        "that has not slept catches what is going around, and it mends worse,",
                        "because mending is mostly something a body does while it is asleep.",
                        "",
                        "Nothing is stored for any of it. The count is the game own one, the one",
                        "the phantoms read, so it persists and is reset by the one thing that",
                        "should reset it.",
                        "",
                        "off - a week awake is a week like any other.")
                .translation("triage.configuration.effects.sleepless").define("sleepless", true);
        SLEEPLESS_DAYS = BUILDER.comment("Days awake at which it is as bad as it gets. The first is free.")
                .translation("triage.configuration.effects.sleeplessDays").defineInRange("sleeplessDays", 3, 2, 100);
        SLEEPLESS_HANDS = BUILDER.comment("What a pair of hands that far gone takes off a treatment.")
                .translation("triage.configuration.effects.sleeplessHands").defineInRange("sleeplessHands", 0.2, 0.0, 1.0);
        SLEEPLESS_ILLNESS = BUILDER.comment("And how much likelier they are to catch what is going around.")
                .translation("triage.configuration.effects.sleeplessIllness").defineInRange("sleeplessIllness", 1.0, 0.0, 10.0);
        SLEEPLESS_MENDING = BUILDER.comment("And how much longer their own wounds take to knit.")
                .translation("triage.configuration.effects.sleeplessMending").defineInRange("sleeplessMending", 0.5, 0.0, 10.0);
        EPINEPHRINE_ROUNDS = BUILDER.comment("",
                        "The three needles, and why they are their own idea.",
                        "",
                        "Everything else in the bag is something you do with your hands over a",
                        "minute. These are decisions made in a second, and every one of them can",
                        "be the wrong decision.",
                        "",
                        "Adrenaline has two right uses and both are real. In a stopped heart it",
                        "does not restart anything - nothing in a needle restarts a heart - it makes",
                        "the next round of compressions worth what the first one was, which is why",
                        "it is given between cycles. And it is the only answer to a body destroying",
                        "the blood that was hung on it.",
                        "",
                        "Its wrong use looks exactly like its right one, which is what makes it the",
                        "most dangerous thing here: somebody emptying out looks like somebody",
                        "fading, and the instinct is to push something into them. Raising the",
                        "pressure in a body with an open vessel means the bleed pours.",
                        "",
                        "How many rounds of compressions it hands back.")
                .translation("triage.configuration.effects.epinephrineRounds").defineInRange("epinephrineRounds", 2, 0, 100);
        EPINEPHRINE_BLEED = BUILDER.comment("And what it costs in volume, per part already bleeding.")
                .translation("triage.configuration.effects.epinephrineBleed").defineInRange("epinephrineBleed", 0.06, 0.0, 1.0);
        EPINEPHRINE_ARREST = BUILDER.comment("Chance a second shot on top of a surge stops the heart.")
                .translation("triage.configuration.effects.epinephrineArrestChance").defineInRange("epinephrineArrestChance", 0.35, 0.0, 1.0);
        MORPHINE_SECONDS = BUILDER.comment("",
                        "Morphine. The pills are a course: slow, safe, worth less each time. This",
                        "is the other thing entirely - at once, strong, works on somebody already",
                        "on the floor, and untouched by how many courses they have had, because it",
                        "is not the same drug.",
                        "",
                        "What it costs is breathing. That is not a balance decision, it is what an",
                        "opiate does: the drive to breathe is what it puts to sleep first.",
                        "",
                        "How long one holds.")
                .translation("triage.configuration.effects.morphineSeconds").defineInRange("morphineSeconds", 75, 1, 100000);
        MORPHINE_DEPRESSES = BUILDER.comment("",
                        "Whether it can stop somebody breathing at all.",
                        "",
                        "Only on a chest that is already failing - crushed, broken, full of",
                        "something, or on a body too empty to spare the effort. They go down and",
                        "they stop breathing, which the airway clock and a pair of hands already",
                        "answer, and so does the third needle.",
                        "",
                        "off - it is a stronger painkiller and nothing else.")
                .translation("triage.configuration.effects.morphineDepresses").define("morphineDepresses", true);
        MORPHINE_STOPS = BUILDER.comment("Chance of it, on a chest that cannot afford it.")
                .translation("triage.configuration.effects.morphineStopsBreathing").defineInRange("morphineStopsBreathing", 0.4, 0.0, 1.0);
        NALOXONE_SHOCK_PAIN = BUILDER.comment("",
                        "The undo, and the only item here that is one. It answers morphine and",
                        "nothing else.",
                        "",
                        "Its own wrong use is the reason it is a decision and not a reflex: pushed",
                        "into somebody who is on morphine for a good reason it does what it says on",
                        "the ampoule and takes the morphine away. All of it, at once, out of a",
                        "person who was being held together by it.",
                        "",
                        "How many things have to be wrong with them for that to be dangerous.")
                .translation("triage.configuration.effects.naloxoneShockPain").defineInRange("naloxoneShockPain", 3, 1, 7);
        NALOXONE_SHOCK_CHANCE = BUILDER.comment("And the chance it puts them into shock when it is.")
                .translation("triage.configuration.effects.naloxoneShockChance").defineInRange("naloxoneShockChance", 0.5, 0.0, 1.0);
        FLU_SECONDS = BUILDER.comment("",
                        "Viruses, and the single thing they are here for.",
                        "",
                        "Everything else a bottle answers in this mod is bacterial, and the mod",
                        "never had to say so, because there was nothing the bottle did not answer.",
                        "A virus is the sentence that makes the bottle mean something: it is not a",
                        "stronger illness, it is a different kind of thing, and a course poured into",
                        "one achieves nothing at all except that the course is gone.",
                        "",
                        "Influenza runs hot, spreads off a sneeze the way a cold does, and passes",
                        "on its own given time and somewhere warm - which most of the time is all",
                        "it does. What it can do on the way is leave a chest open to pneumonia -",
                        "which is how influenza has always killed people, and which is the one",
                        "part of it a bottle does answer. Whether it does is about the body: see",
                        "fluPneumoniaPerSecond.",
                        "",
                        "How long it runs before that becomes a possibility.")
                .translation("triage.configuration.effects.fluSeconds").defineInRange("fluSeconds", 300, 1, 100000);
        FLU_RISK = BUILDER.comment("",
                        "Where it comes from, which is the part a virus cannot do without.",
                        "",
                        "A cold is caught from the weather and a fever from your own wound.",
                        "Influenza is caught from people, and it is the one illness in here that",
                        "has no other honest source - so the vector is the only crowd this game",
                        "has, which is a village. Stand around in one long enough and somebody in",
                        "it was ill.",
                        "",
                        "That also makes the sleepless rule reachable by one player on their own:",
                        "a body that has not slept catches what is going around, and now there is",
                        "something going around.",
                        "",
                        "Chance per second while standing in a crowd. 0 stops it arriving on its",
                        "own and leaves it travelling off a sneeze only.")
                .translation("triage.configuration.effects.fluRiskPerSecond").defineInRange("fluRiskPerSecond", 0.0008, 0.0, 1.0);
        FLU_CROWD = BUILDER.comment("How many villagers within eight blocks count as a crowd.")
                .translation("triage.configuration.effects.fluCrowd").defineInRange("fluCrowd", 3, 1, 100);
        FLU_PNEUMONIA = BUILDER.comment("",
                        "And the chance per second that it goes down into the chest, at the end",
                        "of its run. It climbs from nothing to this as the illness goes on.",
                        "",
                        "Only rolled at all on a body that is cold, wet or badly short of sleep.",
                        "Warm, dry and rested, the flu is just the flu - which is the sentence the",
                        "whole feature is for, and the reason this is a roll and not a clock.")
                .translation("triage.configuration.effects.fluPneumoniaPerSecond").defineInRange("fluPneumoniaPerSecond", 0.005, 0.0, 1.0);
        STOMACH_SECONDS = BUILDER.comment("",
                        "And the other one, which empties people rather than burning them.",
                        "",
                        "Caught off what was eaten. Antibiotics do nothing here either, and the",
                        "answer is the one nobody thinks of as medicine: fluid, faster than it is",
                        "going out. Which is a reason to reach for a bag of saline that is not a",
                        "wound, and there was not one before.")
                .translation("triage.configuration.effects.stomachSeconds").defineInRange("stomachSeconds", 240, 1, 100000);
        STOMACH_CHANCE = BUILDER.comment("Chance that something eaten badly was alive rather than poison.")
                .translation("triage.configuration.effects.stomachChance").defineInRange("stomachChance", 0.35, 0.0, 1.0);
        STOMACH_DRAIN = BUILDER.comment("Volume lost per second to it. This is the whole danger.")
                .translation("triage.configuration.effects.stomachDrain").defineInRange("stomachDrain", 0.004, 0.0, 1.0);
        STOMACH_DAMAGE = BUILDER.comment("And what the belly itself takes.")
                .translation("triage.configuration.effects.stomachDamage").defineInRange("stomachDamage", 0.04, 0.0, 100.0);
        THERMOMETER = BUILDER.comment("",
                        "Whether a figure needs an instrument.",
                        "",
                        "Every other reading here is taken with the hands and is there the moment",
                        "you look. A temperature is not. A hot forehead is something anybody can",
                        "feel - so the word stays free, and the chart still says warm - but thirty",
                        "eight point six is not a thing any hand has reported, and the mod said it",
                        "anyway.",
                        "",
                        "With this on, the figure costs a thermometer and the half minute it takes",
                        "the column to settle. Which is the right price, because the figure is what",
                        "separates a fever starting from one that is finished.",
                        "",
                        "off - the number is free again, as it used to be.")
                .translation("triage.configuration.effects.thermometer").define("thermometer", true);
        RINSE = BUILDER.comment("",
                        "Whether leaving a dirty instrument in water rinses it.",
                        "",
                        "Water rinses; it does not sterilise, and it never has. What comes off a",
                        "blade in a stream is the blood - the part you can see and the part that",
                        "matters least - and the stream makes its own contribution.",
                        "",
                        "So this takes an instrument most of the way down and stops short. The",
                        "last of it still wants a medical table or a bottle of antiseptic, which",
                        "is the right shape for a field expedient: worth doing, and no substitute",
                        "for doing the job properly.",
                        "",
                        "off - water is water and a saw is a saw.")
                .translation("triage.configuration.effects.rinse").define("rinse", true);
        RINSE_SECONDS = BUILDER.comment("Seconds in water per patient rinsed off it.")
                .translation("triage.configuration.effects.rinseSeconds").defineInRange("rinseSeconds", 6, 1, 100000);
        RINSE_FLOOR = BUILDER.comment("How clean water can get it. 0 lets a stream do the whole job.")
                .translation("triage.configuration.effects.rinseFloor").defineInRange("rinseFloor", 1, 0, 8);
        REFERRED_PAIN = BUILDER.comment("",
                        "Whether a torn organ is felt somewhere it is not.",
                        "",
                        "A ruptured spleen hurts at the tip of the left shoulder. It is called",
                        "Kehr's sign, it is real, and it is how that injury gets found by",
                        "somebody with no machine to hand. The liver does it on the right.",
                        "",
                        "The answer is not marked as suspect, because the patient is not lying -",
                        "they do not know anything is wrong with their shoulder. What gives it",
                        "away is the chart: a shoulder that hurts more than anything else in a",
                        "body, on a shoulder with nothing whatever wrong with it, is not a",
                        "shoulder problem. Add a pulse that is running and it can only be one",
                        "thing.",
                        "",
                        "It only ever fires when the named place is genuinely unhurt, so the",
                        "contradiction is always real. A coincidence would not be a clue.",
                        "",
                        "off - they name the part the injury is actually in.")
                .translation("triage.configuration.effects.referredPain").define("referredPain", true);
        CHILL = BUILDER.comment("",
                        "Whether an open body loses heat.",
                        "",
                        "This mod already had two thirds of the oldest circle in trauma surgery",
                        "and did not know it. Hypothermia here stops clotting outright - a bleed",
                        "that would have closed by itself never does - and losing blood cools",
                        "somebody, which is in the temperature reading. The missing third is the",
                        "one that closes it: the operation itself is what cools the patient.",
                        "",
                        "Until this existed the only way to get cold was to stand in powder snow",
                        "or in the rain in the wrong biome. A man lying opened up on a table in a",
                        "blizzard caught nothing at all. In life he is the likeliest person in the",
                        "world to, which is why an operating theatre is kept uncomfortably warm",
                        "for everybody standing up in it.",
                        "",
                        "So where you cut decides how much it costs. A lit room out of the weather",
                        "is almost free. The open, in the snow, at night, is not - and enough of",
                        "it is hypothermia, which is the thing that stops the bleeding stopping.",
                        "",
                        "Nothing new punishes anybody for it: every consequence was already here",
                        "and already right. This only supplies the cause.",
                        "",
                        "off - a body opened in a blizzard is a body opened indoors.")
                .translation("triage.configuration.effects.chill").define("chill", true);
        CHILL_PER_OPERATION = BUILDER.comment("What one cut costs, in the worst place there is.")
                .translation("triage.configuration.effects.chillPerOperation").defineInRange("chillPerOperation", 0.3, 0.0, 1.0);
        CHILL_DEGREES = BUILDER.comment("And what a body that cold reads on the forehead.")
                .translation("triage.configuration.effects.chillDegrees").defineInRange("chillDegrees", 2.5, 0.0, 20.0);
        CHILL_HYPOTHERMIA = BUILDER.comment("Past this it stops being a reading and becomes the illness.")
                .translation("triage.configuration.effects.chillHypothermia").defineInRange("chillHypothermia", 1.0, 0.0, 1.0);
        CHILL_WARM_SECONDS = BUILDER.comment("Seconds of being warm to get it back, halved by firelight.")
                .translation("triage.configuration.effects.chillWarmSeconds").defineInRange("chillWarmSeconds", 90, 1, 100000);
        SNEEZE_CHANCE = BUILDER.comment("",
                        "A sneeze used to be a sound and a line of text addressed to the person",
                        "sneezing, which is the one audience a sneeze is not for. It now does the",
                        "three things a sneeze does: it is loud, it is catching, and it hurts if",
                        "your chest is already wrong.",
                        "",
                        "Chance per second, while ill.")
                .translation("triage.configuration.effects.sneezeChance").defineInRange("sneezeChance", 0.04, 0.0, 1.0);
        SNEEZE_DAMAGE = BUILDER.comment("What one costs a chest with a broken rib in it.")
                .translation("triage.configuration.effects.sneezeDamage").defineInRange("sneezeDamage", 0.8, 0.0, 100.0);
        COLD_CATCHING = BUILDER.comment("",
                        "Chance somebody within four blocks catches it off that sneeze.",
                        "",
                        "The first thing here that travels from one player to another, and it is a",
                        "cold rather than anything worse on purpose: an illness that spreads and",
                        "also matters would make standing near a wounded friend the wrong move,",
                        "and this whole mod is built on standing near wounded friends.",
                        "",
                        "0 - nobody catches anything off anybody.")
                .translation("triage.configuration.effects.coldCatching").defineInRange("coldCatching", 0.25, 0.0, 1.0);
        PNEUMONIA_AFTER_SECONDS = BUILDER.comment("",
                        "A cold used to sit at mild-and-passes however long somebody spent wet and",
                        "freezing with it. Past this many seconds it can go down into the chest -",
                        "but only while they are still cold or still wet. Getting dry and warm is",
                        "the whole of the answer, which is what getting dry and warm is.",
                        "",
                        "Pneumonia bruises both lungs rather than tearing anything: there is no",
                        "hole for a surgeon to find, so only a course answers it.")
                .translation("triage.configuration.effects.pneumoniaAfterSeconds").defineInRange("pneumoniaAfterSeconds", 180, 1, 100000);
        PNEUMONIA_CHANCE = BUILDER.comment("Chance per second past that, while still cold.")
                .translation("triage.configuration.effects.pneumoniaChance").defineInRange("pneumoniaChance", 0.01, 0.0, 1.0);
        PNEUMONIA_DAMAGE = BUILDER.comment("What it costs the chest each second.")
                .translation("triage.configuration.effects.pneumoniaDamage").defineInRange("pneumoniaDamage", 0.06, 0.0, 100.0);
        PNEUMONIA_SEPSIS_SECONDS = BUILDER.comment("And how long before it stops being pneumonia.")
                .translation("triage.configuration.effects.pneumoniaSepsisSeconds").defineInRange("pneumoniaSepsisSeconds", 300, 1, 100000);
        PERITONITIS_DAMAGE = BUILDER.comment("",
                        "What a belly full of gut costs each second.",
                        "",
                        "A torn gut used to roll straight for sepsis, which took a body from a",
                        "wound to the worst thing in the mod in one step, with nothing in between",
                        "to notice or to act on. This is that step.",
                        "",
                        "It is also the one illness antibiotics cannot finish: while the gut is",
                        "still open the cause is still arriving, so a course buys time and the",
                        "surgeon ends it.")
                .translation("triage.configuration.effects.peritonitisDamage").defineInRange("peritonitisDamage", 0.12, 0.0, 100.0);
        PERITONITIS_SEPSIS_SECONDS = BUILDER.comment("How long there is to close the hole.")
                .translation("triage.configuration.effects.peritonitisSepsisSeconds").defineInRange("peritonitisSepsisSeconds", 240, 1, 100000);
        ADRENALINE = BUILDER.comment("",
                        "Whether a fresh injury takes a moment to start hurting.",
                        "",
                        "This mod used to charge for a wound from the instant it happened: a leg",
                        "breaks and the limp starts on the same tick, which is the one part of",
                        "breaking a leg nobody who has broken one recognises.",
                        "",
                        "It hides pain. It does not mend bone. People finish races on stress",
                        "fractures and walk away from crashes on broken ankles; nobody runs on a",
                        "femur in two pieces, because what is missing there is not the will to",
                        "ignore it. A part that is gone, dead, or beaten past use at all is",
                        "untouched by this.",
                        "",
                        "It is not a buff. The heart is already going, the hands shake - which",
                        "makes it the worst possible moment to operate on anybody - and when it",
                        "ends the whole of what was held back arrives at once and worse. There is",
                        "no way to ask for it on purpose, and no second one until the crash from",
                        "the last has passed.",
                        "",
                        "off - a wound hurts from the tick it happens.")
                .translation("triage.configuration.effects.adrenaline").define("adrenaline", true);
        ADRENALINE_SECONDS = BUILDER.comment("How long you do not feel it.")
                .translation("triage.configuration.effects.adrenalineSeconds").defineInRange("adrenalineSeconds", 25, 1, 100000);
        ADRENALINE_RELIEF = BUILDER.comment("What share of the pain goes quiet. Structure is never hidden.")
                .translation("triage.configuration.effects.adrenalineRelief").defineInRange("adrenalineRelief", 0.8, 0.0, 1.0);
        ADRENALINE_CRASH_SECONDS = BUILDER.comment("And how long the bill takes to pay afterwards.")
                .translation("triage.configuration.effects.adrenalineCrashSeconds").defineInRange("adrenalineCrashSeconds", 20, 0, 100000);
        ADRENALINE_CRASH_BITE = BUILDER.comment("How much worse than normal it is while that lasts.")
                .translation("triage.configuration.effects.adrenalineCrashBite").defineInRange("adrenalineCrashBite", 0.3, 0.0, 1.0);
        ADRENALINE_SHAKE = BUILDER.comment("What the shaking takes off anything you treat during it.")
                .translation("triage.configuration.effects.adrenalineShake").defineInRange("adrenalineShake", 0.15, 0.0, 1.0);
        ADRENALINE_PULSE = BUILDER.comment("And what it adds to a heart that is already going.")
                .translation("triage.configuration.effects.adrenalinePulse").defineInRange("adrenalinePulse", 30.0, 0.0, 200.0);
        FATIGUE = BUILDER.comment("",
                        "Whether arms get tired.",
                        "",
                        "This is not a stamina bar and nothing you normally do builds it.",
                        "Sprinting does not, mining does not, fighting does not - vanilla already",
                        "charges for those, and charging twice would be a tax on playing rather",
                        "than a rule about medicine.",
                        "",
                        "It comes from the two things in this mod that are physically heavy and",
                        "that this mod invented: carrying somebody who cannot walk, and bracing",
                        "somebody who is being cut into. A body over the shoulder used to be a",
                        "slower way of walking and nothing else, so getting them out first had no",
                        "limit on it - and a move with no limit is not a decision.",
                        "",
                        "It costs two things: how far you can carry before your arms give out and",
                        "put the patient down, and how steady your hands are for a while after.",
                        "Bad arms tire up to three times faster, which makes a wounded medic a",
                        "worse stretcher-bearer without a separate rule saying so.",
                        "",
                        "The answer is to stand still for a moment, which is the same answer",
                        "mending has.",
                        "",
                        "off - a body weighs nothing after the first step.")
                .translation("triage.configuration.effects.fatigue").define("fatigue", true);
        FATIGUE_SECONDS = BUILDER.comment("Seconds of carrying, with sound arms, before they give out.")
                .translation("triage.configuration.effects.fatigueSeconds").defineInRange("fatigueSeconds", 90, 1, 100000);
        FATIGUE_RECOVER_SECONDS = BUILDER.comment("Seconds of not carrying to come back, halved standing still.")
                .translation("triage.configuration.effects.fatigueRecoverSeconds").defineInRange("fatigueRecoverSeconds", 45, 1, 100000);
        FATIGUE_QUALITY = BUILDER.comment("What fully gone arms take off a treatment.")
                .translation("triage.configuration.effects.fatigueQuality").defineInRange("fatigueQuality", 0.25, 0.0, 1.0);
        FATIGUE_CARRY_SLOW = BUILDER.comment("How much heavier a body feels once the arms are gone.")
                .translation("triage.configuration.effects.fatigueCarrySlow").defineInRange("fatigueCarrySlow", 0.5, 0.0, 4.0);
        FATIGUE_HOLD = BUILDER.comment("What bracing a patient through one treatment costs.")
                .translation("triage.configuration.effects.fatigueHold").defineInRange("fatigueHold", 0.15, 0.0, 1.0);
        ASSIST = BUILDER.comment("",
                        "Whether a second person holding the patient still is worth anything.",
                        "",
                        "Everything in this mod is easier with somebody else, and until now",
                        "somebody else could do everything except the one thing an assistant",
                        "actually does, which is hold on.",
                        "",
                        "They have to be crouched next to the patient with an empty hand. Both are",
                        "deliberate - nobody does either by walking past - so the mod never has to",
                        "guess whether they meant it.",
                        "",
                        "It is worth most where there is nothing else: held down, somebody being",
                        "operated on awake moves less, which is what an operation was before",
                        "anaesthetic and why every picture of an old operating table has straps on",
                        "it. It is still worse than the bottle.",
                        "",
                        "off - the second person in the room is scenery again.")
                .translation("triage.configuration.effects.assist").define("assist", true);
        ASSIST_QUALITY = BUILDER.comment("What a pair of hands adds to how well it goes.")
                .translation("triage.configuration.effects.assistQuality").defineInRange("assistQuality", 0.15, 0.0, 1.0);
        ASSIST_AWAKE_CAP = BUILDER.comment("The ceiling on an operation without anaesthetic, while held.")
                .translation("triage.configuration.effects.assistAwakeCap").defineInRange("assistAwakeCap", 0.7, 0.0, 1.0);
        THEATRE = BUILDER.comment("",
                        "Whether where you operate and what you operate with matter.",
                        "",
                        "Until this existed a surgical kit opened in a lit room over a table did",
                        "the same job as one opened face-down in a cave, with the same saw that",
                        "took somebody else's leg off an hour ago and has not been touched since.",
                        "",
                        "The instrument is the famous half. For most of the history of surgery it",
                        "was the thing that killed people - not the cutting, the carrying: a blade",
                        "went from one patient into the next and nobody thought anything of it.",
                        "That gives the two tools that go into a wound and are never spent - the",
                        "saw and the forceps - the one thing they never had, which is a running cost.",
                        "",
                        "Neither half is a tax on playing. A clean instrument in an ordinary room",
                        "is worth exactly what a treatment has always been worth; somewhere good",
                        "is better than that, and a filthy saw in the dark is worse.",
                        "",
                        "off - a treatment is the same treatment anywhere, with anything.")
                .translation("triage.configuration.effects.theatre").define("theatre", true);
        GRIME_INFECTION = BUILDER.comment("Chance a filthy instrument leaves something behind.")
                .translation("triage.configuration.effects.grimeInfection").defineInRange("grimeInfection", 0.55, 0.0, 1.0);
        ROOM_INFECTION = BUILDER.comment("And what the worst possible place adds on its own.")
                .translation("triage.configuration.effects.roomInfection").defineInRange("roomInfection", 0.18, 0.0, 1.0);
        PLACE_QUALITY = BUILDER.comment("How much a proper theatre is worth to a pair of hands.")
                .translation("triage.configuration.effects.placeQuality").defineInRange("placeQuality", 0.25, 0.0, 1.0);
        PULSE = BUILDER.comment("",
                        "Whether a pulse can be felt for.",
                        "",
                        "The first thing anybody does to anybody: free, instant, needs no item,",
                        "and works on somebody still standing up. It is also the only outward",
                        "sign of the injury this mod deliberately made invisible - a torn spleen",
                        "puts nothing on the skin, and a body losing blood answers by driving the",
                        "heart faster. A fast thin pulse on somebody with nothing open on them is",
                        "the tell, and it is the same tell it is in life.",
                        "",
                        "How well it is felt buys precision rather than healing: a good hold",
                        "gives the number, a poor one gives the word.",
                        "",
                        "off - a heart is something you read off a chart again.")
                .translation("triage.configuration.effects.pulse").define("pulse", true);
        BREATHING = BUILDER.comment("",
                        "Whether a chest can be watched.",
                        "",
                        "The one vital you get without touching anybody: you watch, and you have",
                        "the answer from across the room. It is also what the pulse is to internal",
                        "bleeding, for the other injury this mod hides - a crushed lung has never",
                        "had an outward sign, and fast and shallow is what one looks like.",
                        "",
                        "off - a chest stops being something you can read.")
                .translation("triage.configuration.effects.breathing").define("breathing", true);
        TEMPERATURE = BUILDER.comment("",
                        "Whether somebody's temperature can be felt.",
                        "",
                        "The slowest vital and the only one that sees the future: it starts",
                        "climbing while an infection is still a colour on the chart and long",
                        "before it is a fever with a name. Somebody who checks a forehead every",
                        "so often catches the thing that kills quietly.",
                        "",
                        "It runs the other way too, and that half is the one people get wrong.",
                        "Lost blood and cold blood both pull it down, and cold blood does not",
                        "clot - which is the loop that kills a casualty who was going to live.",
                        "",
                        "off - a forehead tells you nothing.")
                .translation("triage.configuration.effects.temperature").define("temperature", true);
        ASK = BUILDER.comment("",
                        "Whether a conscious patient can be asked where it hurts.",
                        "",
                        "The only channel in this mod that comes out of a body rather than off",
                        "one. It is the only thing that says WHERE - a pulse says the blood went",
                        "somewhere, and a conscious person says which side of their belly it went",
                        "into - and it stops working the moment they stop being able to talk.",
                        "",
                        "It can also be wrong, and that is deliberate. A rattled head names",
                        "something that hurts rather than the worst thing. And painkillers work:",
                        "a dosed patient reports less than is true - small complaints go entirely",
                        "and big ones come down a grade - so the medic who dosed them keeps the",
                        "location and loses the urgency. Nothing else in the mod makes a",
                        "treatment cost information.",
                        "",
                        "off - nobody says anything.")
                .translation("triage.configuration.effects.ask").define("ask", true);
        ORGAN_BROKEN_BONUS = BUILDER.comment("",
                        "How much likelier a blow is to tear something inside when the bone",
                        "over it is already broken.",
                        "",
                        "A ribcage is a shield, and a broken one is not. This mod has said from",
                        "the start that a rib going in and a spleen going are the same kick from",
                        "different angles - so a kick that finds a chest already caved in finds",
                        "what is behind it far more easily.",
                        "",
                        "Which turns a broken rib from a thing that hurts into a thing that has",
                        "stopped protecting somebody, and gives the splint a second reason to",
                        "exist that nobody has to be told about: the second blow is the",
                        "dangerous one. A crack counts for half of it.",
                        "",
                        "1.0 turns it off and bone stops mattering to what is under it.")
                .translation("triage.configuration.effects.organBrokenBonus").defineInRange("organBrokenBonus", 2.0, 1.0, 10.0);
        PROSTHETIC_WEAR_SECONDS = BUILDER.comment("",
                        "How long a fitted limb takes to wear out completely, in seconds.",
                        "",
                        "Nothing made of wood and leather and worn all day stays the shape it was",
                        "cut to. A socket loosens, a strap stretches, a joint develops play - and",
                        "a worn prosthesis is not a broken one, it is a worse one. It rubs, and it",
                        "gives back less of what it gave back on the day it was fitted.",
                        "",
                        "Fitting a fresh one puts it right, which is the standing job the one",
                        "permanent thing in this mod ought to come with: losing a leg is not an",
                        "event somebody gets past, it is a thing they then live with, and living",
                        "with one is mostly maintenance.",
                        "",
                        "0 turns it off and a fitted limb stays as good as the day it went on.")
                .translation("triage.configuration.effects.prostheticWearSeconds").defineInRange("prostheticWearSeconds", 2400, 0, 1000000);
        PROSTHETIC_WORN_RATIO = BUILDER.comment("What a completely worn one is worth, as a share of a fresh one.")
                .translation("triage.configuration.effects.prostheticWornRatio").defineInRange("prostheticWornRatio", 0.55, 0.0, 1.0);
        PHANTOM_PAIN = BUILDER.comment("",
                        "Whether a limb that is gone still hurts.",
                        "",
                        "Almost everybody who loses one has this, which is the only reason it is",
                        "here: it costs nothing, it changes nothing, and leaving it out would",
                        "have been the mod quietly saying the body forgets. It does not. Only the",
                        "player it belongs to is ever told, and painkillers do work on it.")
                .translation("triage.configuration.effects.phantomPain").define("phantomPain", true);
        PHANTOM_PAIN_SECONDS = BUILDER.comment("Roughly how many seconds between one and the next.")
                .translation("triage.configuration.effects.phantomPainSeconds").defineInRange("phantomPainSeconds", 90, 1, 100000);
        ARROWS = BUILDER.comment("",
                        "Whether arrows can stay in.",
                        "",
                        "A lodged arrow is not extra damage - the damage happened on the way in.",
                        "It is a wound that cannot be dealt with while it is there: it will not",
                        "clot, it will not mend, and a bandage over the top of it is a bandage",
                        "over an arrow. Getting it out costs nothing but hands, and getting it",
                        "out badly costs the patient more than leaving it would have.",
                        "",
                        "A blade is the other half of this, and it has its own switch below.")
                .translation("triage.configuration.effects.arrows").define("arrows", true);
        ARROW_CHANCE = BUILDER.comment("Chance that an arrow which hits stays in.")
                .translation("triage.configuration.effects.arrowChance").defineInRange("arrowChance", 0.35, 0.0, 1.0);
        ARROW_TEAR = BUILDER.comment("Damage a badly pulled arrow does on the way out, on top of the heavy",
                        "bleed it leaves. This is the only thing in the mod that can make somebody",
                        "worse off by being helped, and that is deliberate: an arrow in a leg is",
                        "the one injury where waiting for steadier hands is a real plan.")
                .translation("triage.configuration.effects.arrowTear").defineInRange("arrowTear", 3.0, 0.0, 1000.0);
        BUILDER.comment("Bullets from a gun mod, and what they leave behind.",
                        "",
                        "A rifle round is not an arrow. It goes in deep, it does not stand",
                        "out of the skin, and it cannot be drawn back the way a shaft can:",
                        "the only way out is to open the wound and pick it out. Until it is",
                        "out the part will not clot, will not mend and cannot be dressed.",
                        "",
                        "This only does anything when a gun mod is installed. TaCZ is the",
                        "one this was written against; no class from it is named, so any",
                        "gun mod that reports its bullets the same way works too.")
                .translation("triage.configuration.effects.guns").push("guns");
        BULLETS_STICK = BUILDER.comment("Whether a bullet can stay in.")
                .translation("triage.configuration.effects.guns.bulletsStick").define("bulletsStick", true);
        BULLET_STICK_CHANCE = BUILDER.comment("Chance that a bullet which hits stays in.",
                        "",
                        "Higher than an arrow's, because a round that has gone through a",
                        "body has already spent most of what it had and stops in the meat",
                        "far more often than a shaft does. Set it to 0 to keep the damage",
                        "and lose the extraction.")
                .translation("triage.configuration.effects.guns.bulletStickChance").defineInRange("bulletStickChance", 0.25, 0.0, 1.0);
        BULLET_TEAR = BUILDER.comment("Damage a badly cut-out bullet does on the way out, on top of the",
                        "heavy bleed it leaves. The same trade as an arrow: a steady hand",
                        "leaves an ordinary cut, and an impatient one makes the patient worse.")
                .translation("triage.configuration.effects.guns.bulletTear").defineInRange("bulletTear", 3.0, 0.0, 1000.0);
        BUILDER.pop();
        WEAPONS_STICK = BUILDER.comment("",
                        "Whether a weapon can stay in the person it was swung at.",
                        "",
                        "Rare, and worth much more than its rate. Three things happen at once that",
                        "nothing else in this mod does: the thing in the wound is holding the wound",
                        "shut, so taking it out is the injury; whoever swung it does not have it any",
                        "more, which is the only disarm here; and it is visible across a field on",
                        "somebody who is still walking around.",
                        "",
                        "Anything with a tier can do it - a sword, an axe, a pick, and modded",
                        "equivalents built the ordinary way - and a trident. Not a fist.")
                .translation("triage.configuration.effects.weaponsStick").define("weaponsStick", true);
        STICK_CHANCE = BUILDER.comment("Chance that a heavy enough blow leaves the weapon in.")
                .translation("triage.configuration.effects.stickChance").defineInRange("stickChance", 0.06, 0.0, 1.0);
        STICK_DAMAGE = BUILDER.comment("How hard the blow has to be first. Below this it is a cut,",
                        "not something that went in deep enough to hold.")
                .translation("triage.configuration.effects.stickDamage").defineInRange("stickDamage", 5.0, 0.0, 1000.0);
        STICK_JOSTLE = BUILDER.comment("Damage per second done to a part while walking about with",
                        "something still in it. Sprinting costs four times this.")
                .translation("triage.configuration.effects.stickJostle").defineInRange("stickJostle", 0.25, 0.0, 1000.0);
        STICK_JOSTLE_BLEED = BUILDER.comment("Chance per second, while moving, that the wound lets go",
                        "around it anyway.")
                .translation("triage.configuration.effects.stickJostleBleed").defineInRange("stickJostleBleed", 0.06, 0.0, 1.0);
        FINDING_FRESH_SECONDS = BUILDER.comment("",
                        "How many seconds a finding counts as current, in realistic difficulty.",
                        "",
                        "Past this the chart prints it faintly. It is not taken away: an old",
                        "finding is still the last thing anybody knows, and throwing it out would",
                        "punish the one person who did the work. What goes is the confidence.")
                .translation("triage.configuration.effects.findingFreshSeconds").defineInRange("findingFreshSeconds", 120, 5, 100000);
        BEDSIDE_OBS = BUILDER.comment("",
                        "Whether a patient beside a medical table is observed automatically.",
                        "",
                        "A ward is not a place with better medicine in it - it is a place where",
                        "somebody writes a number on a page every so often. Pulse and temperature",
                        "go onto the observation sheet on a clock, quietly, with no line in chat.",
                        "",
                        "It is also the first reason in this mod to carry somebody somewhere:",
                        "supplies travel, and a place that measures for you does not.")
                .translation("triage.configuration.effects.bedsideObs").define("bedsideObs", true);
        BEDSIDE_SECONDS = BUILDER.comment("Seconds between one automatic observation and the next.")
                .translation("triage.configuration.effects.bedsideSeconds").defineInRange("bedsideSeconds", 40, 5, 100000);
        BEDSIDE_RANGE = BUILDER.comment("How many blocks from the table still counts as beside it.")
                .translation("triage.configuration.effects.bedsideRange").defineInRange("bedsideRange", 3, 1, 16);
        PANIC = BUILDER.comment("",
                        "Whether an awake patient in pain can pull away from an operation.",
                        "",
                        "The only interruption in this mod that comes from the patient rather than",
                        "from somebody hitting the medic. It never happens to anybody who is under",
                        "ether, dosed, on the floor, or being held by a second pair of hands - so",
                        "it is not a tax on surgery, it is the reason those three things exist.")
                .translation("triage.configuration.effects.panic").define("panic", true);
        PANIC_CHANCE = BUILDER.comment("Chance per second at the worst pain there is. Scaled down by how",
                        "much pain they are actually in, so a scratch never does this.",
                        "",
                        "Per second, which means the number that matters is not this one - it is",
                        "what it comes to over a job. At this setting a fifteen-second operation on",
                        "somebody in serious pain is interrupted about a third of the time, and on",
                        "somebody in the worst pain there is, four times in ten. Nothing is spent",
                        "when it happens: what it costs is the time and the walk back to whatever",
                        "would have stopped it.")
                .translation("triage.configuration.effects.panicChance").defineInRange("panicChance", 0.035, 0.0, 1.0);
        BREATH_SOUNDS = BUILDER.comment("",
                        "Whether a chest in trouble can be heard from a body length away.",
                        "",
                        "The third thing in this mod that costs nothing to find out, after the",
                        "colour of somebody and the way they are standing. What it says is that",
                        "something is wrong with a chest - never the rate and never the cause, so",
                        "the stethoscope keeps its job and gains a better one.",
                        "",
                        "A chest that has stopped makes no sound at all. That is not a gap.")
                .translation("triage.configuration.effects.breathSounds").define("breathSounds", true);
        STICK_TEAR = BUILDER.comment("Damage done by pulling one out with bare hands, on top of the",
                        "bleed. A surgeon with forceps does neither.")
                .translation("triage.configuration.effects.stickTear").defineInRange("stickTear", 4.0, 0.0, 1000.0);
        BUILDER.pop();

        BUILDER.comment("",
                        "Blood that stopped moving.",
                        "",
                        "Everything else in this mod arrives on the end of something. This is what",
                        "a body does to itself when nothing is happening to it: blood that is not",
                        "moving sets, and what stops it moving is a strap, a splint, a broken leg",
                        "and an hour on the floor waiting for somebody to come.",
                        "",
                        "Which makes it the first injury here caused by treatment - the tourniquet",
                        "that saved the limb and the stillness that let it work. Nothing about it",
                        "punishes a mistake; it charges for a correct decision.",
                        "",
                        "The limb is not the danger. It aches, and that is all it ever does. What",
                        "it is waiting for is the patient to stand up and walk.")
                .translation("triage.configuration.clots").push("clots");
        CLOTS = BUILDER.translation("triage.configuration.clots.clots").define("clots", true);
        CLOT_CHANCE = BUILDER.comment("Chance per second of one forming in a still limb, before the",
                        "multipliers for the strap, the splint, the break and the blood.",
                        "",
                        "A clot needs a reason now: a tourniquet, a splint, a fracture or",
                        "crack, real blood loss, or being on the floor. A player who is",
                        "simply standing still with nothing wrong will not develop one.")
                .translation("triage.configuration.clots.clotChance").defineInRange("clotChance", 0.00008, 0.0, 1.0);
        CLOT_STILL_SECONDS = BUILDER.comment("How long a limb has to be still before a clot can start.",
                        "",
                        "This is the clock that stops an ordinary pause from becoming an",
                        "injury. The default is a minute and a half; the risk factors",
                        "above still multiply the chance after that.")
                .translation("triage.configuration.clots.stillSeconds").defineInRange("stillSeconds", 90, 1, 100000);
        CLOT_CLEAR_SECONDS = BUILDER.comment("How long one sits before the body deals with it by itself. Most of",
                        "them do, which is the only reason having one is survivable.")
                .translation("triage.configuration.clots.clotClearSeconds").defineInRange("clotClearSeconds", 240, 1, 100000);
        EMBOLISM_CHANCE = BUILDER.comment("And the chance per second that a clot goes, while the limb it is in",
                        "is moving. Tripled by running.")
                .translation("triage.configuration.clots.embolismChance").defineInRange("embolismChance", 0.002, 0.0, 1.0);
        EMBOLISM_DAMAGE = BUILDER.comment("What one in the lungs costs the chest every second it is there.")
                .translation("triage.configuration.clots.embolismDamage").defineInRange("embolismDamage", 0.28, 0.0, 1000.0);
        BUILDER.pop();

        BUILDER.comment("",
                        "What being awake for days looks like from inside.",
                        "",
                        "Minecraft has always counted this and has always done one thing with",
                        "it, which is to send birds. That is a threat from outside. What three",
                        "days awake actually does is on the inside of somebody\'s eyes, and it",
                        "is the one affliction in this mod that nobody else can see at all.",
                        "",
                        "The lids come down, they come down further the longer it goes on, and",
                        "the blinks get longer until one of them does not end when it should.",
                        "Sleep clears all of it, and nothing else does.")
                .translation("triage.configuration.sleep").push("sleep");
        SLEEPY_FROM = BUILDER.comment("How far into the count before the lids start showing at all.",
                        "One late night is not an affliction.")
                .translation("triage.configuration.sleep.sleepyFrom").defineInRange("sleepyFrom", 0.25, 0.0, 1.0);
        MICROSLEEP_FROM = BUILDER.comment("And how far in before one of the blinks stops being a blink.")
                .translation("triage.configuration.sleep.microsleepFrom").defineInRange("microsleepFrom", 0.7, 0.0, 1.0);
        MICROSLEEP_CHANCE = BUILDER.comment("Chance of that per second at the very end of the count. It is the",
                        "thing that happens to drivers: the world goes away for a second and a",
                        "half and comes back somewhere else.")
                .translation("triage.configuration.sleep.microsleepChance").defineInRange("microsleepChance", 0.010, 0.0, 1.0);
        MICROSLEEP_SECONDS = BUILDER.comment("How long the lights are out for one.")
                .translation("triage.configuration.sleep.microsleepSeconds").defineInRange("microsleepSeconds", 2, 1, 100000);
        BUILDER.pop();

        BUILDER.comment("",
                        "Bones that moved, and bones that healed where they were left.",
                        "",
                        "The mod had one kind of broken and there are two. A bone can break and",
                        "stay where it was - a bad month - or the ends can move, which is the",
                        "thing everybody pictures when they hear the word: the limb is the wrong",
                        "shape, and anybody can see it without reading a chart.",
                        "",
                        "It matters because a splint stops being the whole answer. Strapping a",
                        "stick to a bone that is out of line holds it exactly where it is, and",
                        "where it is is wrong - so it knits like that, and that is permanent.",
                        "Something has to pull it back into line first, and that something is a",
                        "pair of hands, done to somebody who is awake, and a great deal of noise.",
                        "",
                        "And above both of them, the one that is also a wound: the end of the",
                        "bone through the skin. That one is a surgeon's job before it is",
                        "anybody else's.")
                .translation("triage.configuration.displacement").push("displacement");
        DISPLACEMENT = BUILDER.translation("triage.configuration.displacement.displacement").define("displacement", true);
        DISPLACED_CHANCE = BUILDER.comment("Chance that a fresh break is out of line rather than merely broken.")
                .translation("triage.configuration.displacement.displacedChance").defineInRange("displacedChance", 0.5, 0.0, 1.0);
        DISPLACED_BLOW = BUILDER.comment("And how hard a blow has to be before it can move the ends at all.",
                        "Under this a bone breaks and stays where it was.")
                .translation("triage.configuration.displacement.displacedBlow").defineInRange("displacedBlow", 10.0, 0.0, 1000.0);
        OPEN_FRACTURES = BUILDER.comment("Whether the end can come through the skin.")
                .translation("triage.configuration.displacement.openFractures").define("openFractures", true);
        OPEN_CHANCE = BUILDER.comment("Chance of that, on a break that is already out of line.")
                .translation("triage.configuration.displacement.openChance").defineInRange("openChance", 0.2, 0.0, 1.0);
        OPEN_INFECTION = BUILDER.comment("Multiplier on how fast an open fracture goes bad. The inside of a leg",
                        "has been introduced to the floor, and this is the fastest road to",
                        "infection there is in this mod.")
                .translation("triage.configuration.displacement.openInfection").defineInRange("openInfection", 3.0, 0.0, 100.0);
        OPEN_BLOOD_LOSS = BUILDER.comment("Share of volume an open fracture costs when it happens.")
                .translation("triage.configuration.displacement.openBloodLoss").defineInRange("openBloodLoss", 0.08, 0.0, 1.0);
        CROOKED_COST = BUILDER.comment("",
                        "What a bone that healed out of line costs the part, as a share of its",
                        "maximum, for good.",
                        "",
                        "The only permanent loss of health in the mod, and it is here because",
                        "that is what a malunion is: the bone is shorter or bent, and the limb",
                        "does less than it did. It can be undone, and only one way - somebody",
                        "breaks it again and sets it properly this time.")
                .translation("triage.configuration.displacement.crookedCost").defineInRange("crookedCost", 0.12, 0.0, 0.9);
        CROOKED_PENALTY = BUILDER.comment("And what it costs to use, on top of the health: speed in a leg,",
                        "steadiness in an arm.")
                .translation("triage.configuration.displacement.crookedPenalty").defineInRange("crookedPenalty", 0.15, 0.0, 1.0);
        SET_TEAR = BUILDER.comment("Damage a reduction does when the hands go the wrong way.")
                .translation("triage.configuration.displacement.setTear").defineInRange("setTear", 4.0, 0.0, 1000.0);
        SET_ARTERY = BUILDER.comment("And the chance that a badly done one puts a bone end through something",
                        "that was carrying blood. This is why a femur out of line is the injury",
                        "that frightens people who know what they are looking at.")
                .translation("triage.configuration.displacement.setArteryChance").defineInRange("setArteryChance", 0.2, 0.0, 1.0);
        SCREAM = BUILDER.comment("",
                        "Whether setting a bone on somebody who is awake makes a noise the world",
                        "can hear.",
                        "",
                        "The only thing in this mod that a treatment does to the room rather than",
                        "to the patient. Nothing else here has ever cost anybody a decision about",
                        "where to do it - and this is the one job where standing in the open, at",
                        "night, near anything with ears, is a bad plan.",
                        "",
                        "It is also the first reason to carry the mask that is not about quality.")
                .translation("triage.configuration.displacement.scream").define("scream", true);
        SCREAM_RANGE = BUILDER.comment("How far it carries, in blocks.")
                .translation("triage.configuration.displacement.screamRange").defineInRange("screamRange", 32.0, 0.0, 256.0);
        BUILDER.pop();

        BUILDER.comment("",
                        "The one that is not just a faster bleed.",
                        "",
                        "Everything else in here is a body losing blood. This is a body throwing",
                        "it away: it goes with the heart rather than in a trickle, it does not",
                        "clot, ever, and a dressing over the top of one soaks through and keeps",
                        "soaking. What it leaves is a count in tens of seconds.",
                        "",
                        "Which is why it exists. The mod had two grades of bleeding and they were",
                        "the same thing at two speeds, so the answer was always the same answer",
                        "in less of a hurry. There are exactly two answers to this one - a hand",
                        "pushed hard into it, or a strap above it - and both were already here",
                        "waiting for something that needed them.")
                .translation("triage.configuration.artery").push("artery");
        ARTERIAL = BUILDER.translation("triage.configuration.artery.arterial").define("arterial", true);
        ARTERIAL_CHANCE = BUILDER.comment("Chance that a bleed bad enough to qualify opens a vessel instead.",
                        "",
                        "Deliberately low. A wound this bad turning up in every other fight would",
                        "not be frightening, it would be the weather.")
                .translation("triage.configuration.artery.arterialChance").defineInRange("arterialChance", 0.12, 0.0, 1.0);
        ARTERIAL_BLOW = BUILDER.comment("How hard a single blow has to land before it can reach one at all.")
                .translation("triage.configuration.artery.arterialBlow").defineInRange("arterialBlow", 14.0, 0.0, 1000.0);
        ARTERIAL_RATE = BUILDER.comment("Damage to the part per second, against 0.7 for a heavy bleed.")
                .translation("triage.configuration.artery.arterialRate").defineInRange("arterialRate", 2.0, 0.0, 1000.0);
        BLOOD_PER_ARTERIAL = BUILDER.comment("And volume per second, against 0.010 for a heavy bleed. This is the",
                        "number that makes it a different injury rather than a worse one.")
                .translation("triage.configuration.artery.bloodPerArterialSecond").defineInRange("bloodPerArterialSecond", 0.045, 0.0, 1.0);
        ARTERIAL_SPURT = BUILDER.comment("Whether it throws blood in time with the heart rather than dripping.",
                        "Costs nothing but particles, and it is the only way anybody can tell",
                        "the difference across a room.")
                .translation("triage.configuration.artery.spurts").define("spurts", true);
        BUILDER.pop();

        BUILDER.comment("",
                        "Going out.",
                        "",
                        "Not the same thing as going down, and the difference is the whole reason",
                        "it is here. Being downed is the end of your luck: a clock, a rescue, or",
                        "a grave. A faint is a body deciding, without asking, that it cannot keep",
                        "the head supplied while standing up - so it stops standing up. Ten",
                        "seconds later it is fine, and nothing about it is permanent.",
                        "",
                        "What it costs is the ten seconds, and wherever you happened to be.")
                .translation("triage.configuration.faint").push("faint");
        FAINT = BUILDER.translation("triage.configuration.faint.faint").define("faint", true);
        FAINT_BLOOD_LOSS = BUILDER.comment("Share of blood gone before an empty body can drop on its own.")
                .translation("triage.configuration.faint.faintBloodLoss").defineInRange("faintBloodLoss", 0.42, 0.0, 1.0);
        FAINT_CHANCE = BUILDER.comment("Chance per second while at risk. Doubled by sprinting, which is the",
                        "honest version of why people faint when they stand up too fast.")
                .translation("triage.configuration.faint.faintChance").defineInRange("faintChance", 0.020, 0.0, 1.0);
        FAINT_SECONDS = BUILDER.comment("How long the lights are out.")
                .translation("triage.configuration.faint.faintSeconds").defineInRange("faintSeconds", 7, 1, 100000);
        FAINT_PAIN = BUILDER.comment("Pain out of ten at which a body can go over from pain alone. This is",
                        "the common faint - the one at the sight of a broken arm, usually",
                        "somebody else's - and it needs no blood loss at all.")
                .translation("triage.configuration.faint.faintPain").defineInRange("faintPain", 8, 0, 10);
        FAINT_CLEAR_SECONDS = BUILDER.comment("Grace after coming round before it can happen again.")
                .translation("triage.configuration.faint.faintClearSeconds").defineInRange("faintClearSeconds", 25, 0, 100000);
        BUILDER.pop();

        BUILDER.comment("",
                        "What a body keeps.",
                        "",
                        "Until this, every injury in the mod healed to the same body it started",
                        "as. Somebody who had survived four amputations, a septic belly and a",
                        "skull fracture was, once mended, indistinguishable from somebody who had",
                        "never been touched - which is a mod about medicine quietly saying that",
                        "none of it mattered once it was over.",
                        "",
                        "So parts now remember. A bone that has knitted knows about weather and",
                        "is where the next fall lands; skin that has been opened and closed is",
                        "scar tissue, which has less blood in it and is where the next infection",
                        "settles; a deep burn tightens as it heals.",
                        "",
                        "The counts are permanent and only dying clears them. The stiffness is",
                        "not: that is worked back down by hand, over days, which is the whole",
                        "design - the history ratchets one way and the disability does not.")
                .translation("triage.configuration.scars").push("scars");
        SCARS = BUILDER.translation("triage.configuration.scars.scars").define("scars", true);
        SCAR_STIFFNESS = BUILDER.comment("How tight each closed wound leaves the part, 0 to 1.")
                .translation("triage.configuration.scars.scarStiffness").defineInRange("scarStiffness", 0.10, 0.0, 1.0);
        BURN_STIFFNESS = BUILDER.comment("And a burn that healed through the skin, which is the one that really",
                        "contracts: burnt skin closes by pulling the edges together.")
                .translation("triage.configuration.scars.burnStiffness").defineInRange("burnStiffness", 0.35, 0.0, 1.0);
        GRAFT_STIFFNESS = BUILDER.comment("And a limb that was sewn back on.")
                .translation("triage.configuration.scars.graftStiffness").defineInRange("graftStiffness", 0.40, 0.0, 1.0);
        STIFF_QUALITY = BUILDER.comment("What a completely stiff pair of hands costs on the treatment screens,",
                        "as a share. A stiff arm is not a weak arm - it is one that does not go",
                        "where it is put.")
                .translation("triage.configuration.scars.stiffQuality").defineInRange("stiffQuality", 0.35, 0.0, 1.0);
        STIFF_SLOW = BUILDER.comment("And what a completely stiff leg costs off walking speed.")
                .translation("triage.configuration.scars.stiffSlow").defineInRange("stiffSlow", 0.20, 0.0, 1.0);
        SCAR_INFECTION = BUILDER.comment("Multiplier on the chance a wound goes bad where the skin has been",
                        "opened before, or where something has grown once already.")
                .translation("triage.configuration.scars.scarInfection").defineInRange("scarInfection", 1.7, 0.0, 100.0);
        KNIT_FRAGILITY = BUILDER.comment("Multiplier on the blow needed to break a bone that has knitted here",
                        "before. Under one, it goes more easily.",
                        "",
                        "Not because knitted bone is weak - it is not, and the mod will not",
                        "teach that it is. It is because whatever put you on the floor hard",
                        "enough to break that femur is the same thing you keep doing.")
                .translation("triage.configuration.scars.knitFragility").defineInRange("knitFragility", 0.78, 0.1, 10.0);
        ACHE = BUILDER.comment("Whether old breaks ache when the weather turns.",
                        "",
                        "Costs nothing and heals nothing. It is the single most reported thing",
                        "about an old fracture by the people who have one, and it hands a player",
                        "something no readout in this mod does: a reason to notice the sky.")
                .translation("triage.configuration.scars.ache").define("ache", true);
        ACHE_SECONDS = BUILDER.comment("Roughly how many seconds between one twinge and the next.")
                .translation("triage.configuration.scars.acheSeconds").defineInRange("acheSeconds", 75, 1, 100000);
        STRETCH = BUILDER.comment("Whether a stiff part can be worked loose by hand.")
                .translation("triage.configuration.scars.stretch").define("stretch", true);
        STRETCH_GAIN = BUILDER.comment("Stiffness a perfect session takes off. One session per part per day.")
                .translation("triage.configuration.scars.stretchGain").defineInRange("stretchGain", 0.20, 0.0, 1.0);
        STRETCH_SELF_CAP = BUILDER.comment("The most of that anybody gets working on their own limb. You stop at",
                        "the pain, because it is your pain, which is precisely why this is a",
                        "thing done to people rather than by them.")
                .translation("triage.configuration.scars.stretchSelfCap").defineInRange("stretchSelfCap", 0.5, 0.0, 1.0);
        STRETCH_TEAR = BUILDER.comment("Damage done by a session pushed past the joint rather than to it.")
                .translation("triage.configuration.scars.stretchTear").defineInRange("stretchTear", 2.5, 0.0, 1000.0);
        BUILDER.pop();

        BUILDER.comment("",
                        "The stick, after it is tied on.",
                        "",
                        "A splint used to be a full stop: the bone was set, the item was spent,",
                        "and the stick vanished. Now it stays on the limb - drawn on the body,",
                        "on the chart, and holding the limb still for as long as it is there.",
                        "",
                        "The clock on it is not a timer, it is a bargain. Walking runs it down.",
                        "Running on a set bone puts time back on, every second of it, which is",
                        "the honest version of what happens to a fracture nobody will stay off:",
                        "it does not start you over, it simply takes longer - and longer again",
                        "the next time you do it.")
                .translation("triage.configuration.splints").push("splints");
        SPLINT_WORN = BUILDER.translation("triage.configuration.splints.wornSplints").define("wornSplints", true);
        SPLINT_SECONDS = BUILDER.comment("How long one has to stay on before the bone is done with it.")
                .translation("triage.configuration.splints.splintSeconds").defineInRange("splintSeconds", 300, 1, 100000);
        SPLINT_RUN_PENALTY = BUILDER.comment("Seconds put back on per second of running on a splinted leg.")
                .translation("triage.configuration.splints.splintRunPenaltySeconds").defineInRange("splintRunPenaltySeconds", 3, 0, 100000);
        SPLINT_RUN_CEILING = BUILDER.comment("And the most it can ever be pushed out to, so that a bad week still",
                        "ends. Nothing in this mod should be able to become forever by accident.")
                .translation("triage.configuration.splints.splintCeilingSeconds").defineInRange("splintCeilingSeconds", 900, 1, 100000);
        SPLINT_SLOW = BUILDER.comment("What a splinted leg costs off walking speed while it is on.")
                .translation("triage.configuration.splints.splintSlow").defineInRange("splintSlow", 0.18, 0.0, 1.0);
        SPLINT_QUALITY = BUILDER.comment("And what a splinted arm costs the hands on a treatment screen.")
                .translation("triage.configuration.splints.splintQuality").defineInRange("splintQuality", 0.25, 0.0, 1.0);
        BUILDER.pop();

        BUILDER.comment("",
                        "Limbs taken off by whatever was swinging.",
                        "",
                        "The mod could already take an arm off with a blast or with a saw, and",
                        "the one thing in between - somebody hitting you with an axe - did",
                        "nothing but damage. That is the wrong gap to have: the blast is rare",
                        "and the saw is a decision, so in practice nobody ever lost a limb to",
                        "the thing that actually takes limbs off.",
                        "",
                        "It needs a blade. A zombie does not take an arm off, a skeleton does",
                        "not, and a fall certainly does not - what does it is an edge, swung",
                        "hard, into a limb that is already ruined.")
                .translation("triage.configuration.dismemberment").push("dismemberment");
        SEVER = BUILDER.translation("triage.configuration.dismemberment.severing").define("severing", true);
        SEVER_BLOW = BUILDER.comment("How hard the blow has to be, after armour.")
                .translation("triage.configuration.dismemberment.severBlow").defineInRange("severBlow", 7.0, 0.0, 1000.0);
        SEVER_CHANCE = BUILDER.comment("Chance of it, on a limb that has already been beaten to nothing.")
                .translation("triage.configuration.dismemberment.severChance").defineInRange("severChance", 0.35, 0.0, 1.0);
        SEVER_HEALTHY = BUILDER.comment("And the chance on a limb that is still in one piece, which is the",
                        "clean stroke everybody pictures and nearly nobody lands. Set it to zero",
                        "for a game where limbs only come off things that are already wrecked.")
                .translation("triage.configuration.dismemberment.severHealthyChance").defineInRange("severHealthyChance", 0.04, 0.0, 1.0);
        BUILDER.pop();

        BUILDER.comment("Extra items that count as medicine, on top of the mod's own.",
                        "",
                        "The mod's bandage, splint, medkit, surgical kit and painkillers already",
                        "work on their own and are not listed here. These lists are for pointing",
                        "the same treatments at somebody else's items - another medical mod, or",
                        "plain wool if that is the game you want. Items listed here are used with",
                        "Shift + right click, so an ordinary block still places normally.",
                        "",
                        "Entries are item ids ('minecraft:stick') or item tags ('#minecraft:wool').",
                        "",
                        "  bandages    stop bleeding on one part and patch it up a little",
                        "  splints     set a broken bone",
                        "  medkits     heal the part in the worst shape",
                        "  surgery     bring a blacked-out limb back, partly",
                        "  painkillers hide the penalties for a while, healing nothing",
                        "  salines     put lost blood volume back, once nothing is still open")
                .translation("triage.configuration.medicine").push("medicine");
        BANDAGES = BUILDER.translation("triage.configuration.medicine.bandages").defineList("bandages", List.of(), () -> "", Config::isItemEntry);
        TOURNIQUETS = BUILDER.translation("triage.configuration.medicine.tourniquets").defineList("tourniquets", List.of(), () -> "", Config::isItemEntry);
        SPLINTS = BUILDER.translation("triage.configuration.medicine.splints").defineList("splints", List.of(), () -> "", Config::isItemEntry);
        MEDKITS = BUILDER.translation("triage.configuration.medicine.medkits").defineList("medkits", List.of(), () -> "", Config::isItemEntry);
        SURGERY = BUILDER.translation("triage.configuration.medicine.surgery").defineList("surgery", List.of(), () -> "", Config::isItemEntry);
        PAINKILLERS = BUILDER.translation("triage.configuration.medicine.painkillers").defineList("painkillers", List.of(), () -> "", Config::isItemEntry);
        ANTISEPTICS = BUILDER.translation("triage.configuration.medicine.antiseptics").defineList("antiseptics", List.of(), () -> "", Config::isItemEntry);
        SALVES = BUILDER.translation("triage.configuration.medicine.salves").defineList("salves", List.of(), () -> "", Config::isItemEntry);
        ANTIBIOTICS = BUILDER.translation("triage.configuration.medicine.antibiotics").defineList("antibiotics", List.of(), () -> "", Config::isItemEntry);
        SALINES = BUILDER.translation("triage.configuration.medicine.salines").defineList("salines", List.of(), () -> "", Config::isItemEntry);
        BANDAGE_HEAL = BUILDER.translation("triage.configuration.medicine.bandageHeal").defineInRange("bandageHeal", 2.0, 0.0, 1000.0);
        MEDKIT_HEAL = BUILDER.translation("triage.configuration.medicine.medkitHeal").defineInRange("medkitHeal", 12.0, 0.0, 1000.0);
        SALVE_HEAL = BUILDER.comment("What burn salve puts back, on top of clearing the burn.")
                .translation("triage.configuration.medicine.salveHeal").defineInRange("salveHeal", 3.0, 0.0, 1000.0);
        SURGERY_RESTORE = BUILDER.comment("Share of its maximum a blacked-out part comes back at.")
                .translation("triage.configuration.medicine.surgeryRestore").defineInRange("surgeryRestore", 0.3, 0.05, 1.0);
        TRANSFUSION_RESTORE = BUILDER.comment("Share of a full body of blood one drip puts back.",
                        "",
                        "It is refused outright while anything is still bleeding, so this cannot",
                        "be used to outrun a wound - only to undo one that has been closed.")
                .translation("triage.configuration.medicine.transfusionRestore").defineInRange("transfusionRestore", 0.5, 0.01, 1.0);
        BLOOD_RESTORE = BUILDER.comment("Share a bag of matching blood puts back. More than saline, because it",
                        "is the real thing - and because somebody had to bleed for it.")
                .translation("triage.configuration.medicine.bloodRestore").defineInRange("bloodRestore", 0.85, 0.01, 1.0);
        DONATION_COST = BUILDER.comment("Share of their own blood a donor loses filling one bag.")
                .translation("triage.configuration.medicine.donationCost").defineInRange("donationCost", 0.3, 0.01, 1.0);
        MAX_DONOR_LOSS = BUILDER.comment("How empty a donor may be left. A draw that would take them past this",
                        "is refused, so nobody can be bled to death one bag at a time.")
                .translation("triage.configuration.medicine.maxDonorLoss").defineInRange("maxDonorLoss", 0.5, 0.05, 1.0);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private static boolean isItemEntry(Object entry) {
        if (!(entry instanceof String text) || text.isBlank()) {
            return false;
        }
        return ResourceLocation.tryParse(text.startsWith("#") ? text.substring(1) : text) != null;
    }

    /** The current numbers. Never null, and safe to read before the config has loaded. */
    public static Values values() {
        return values;
    }

    /** Called when the config loads or is reloaded; see {@link TriageMod}. */
    public static void bake() {
        Values baked = new Values();
        baked.tetanusSeconds = TETANUS_SECONDS.get();
        baked.tetanusChance = TETANUS_CHANCE.get().floatValue();
        baked.crushSeconds = CRUSH_SECONDS.get();
        baked.crushStrapSeconds = CRUSH_STRAP_SECONDS.get();
        baked.relapseChance = RELAPSE_CHANCE.get().floatValue();
        baked.realistic = REALISTIC.get();
        baked.headMax = HEAD_MAX.get().floatValue();
        baked.thoraxMax = THORAX_MAX.get().floatValue();
        baked.stomachMax = STOMACH_MAX.get().floatValue();
        baked.armMax = ARM_MAX.get().floatValue();
        baked.legMax = LEG_MAX.get().floatValue();
        baked.damageScale = DAMAGE_SCALE.get().floatValue();
        baked.healScale = HEAL_SCALE.get().floatValue();
        baked.regeneration = REGENERATION.get();
        baked.overflowToThorax = OVERFLOW_TO_THORAX.get().floatValue();
        baked.blackedLimbDrain = BLACKED_DRAIN.get().floatValue();
        baked.oneShotProtectionAbove = ONE_SHOT_ABOVE.get().floatValue();
        baked.oneShotFloor = ONE_SHOT_FLOOR.get().floatValue();

        baked.headWeight = HEAD_WEIGHT.get();
        baked.thoraxWeight = THORAX_WEIGHT.get();
        baked.stomachWeight = STOMACH_WEIGHT.get();
        baked.armWeight = ARM_WEIGHT.get();
        baked.legWeight = LEG_WEIGHT.get();

        baked.lightBleedRate = LIGHT_BLEED_RATE.get().floatValue();
        baked.heavyBleedRate = HEAVY_BLEED_RATE.get().floatValue();
        baked.bleedChancePerDamage = BLEED_CHANCE_PER_DAMAGE.get().floatValue();
        baked.heavyBleedThreshold = HEAVY_BLEED_THRESHOLD.get().floatValue();
        baked.lightBleedSeconds = LIGHT_BLEED_SECONDS.get();
        baked.clots = CLOTS.get();
        baked.clotChance = CLOT_CHANCE.get().floatValue();
        baked.clotStillSeconds = CLOT_STILL_SECONDS.get();
        baked.clotClearSeconds = CLOT_CLEAR_SECONDS.get();
        baked.embolismChance = EMBOLISM_CHANCE.get().floatValue();
        baked.embolismDamage = EMBOLISM_DAMAGE.get().floatValue();
        baked.sleepyFrom = SLEEPY_FROM.get().floatValue();
        baked.microsleepFrom = MICROSLEEP_FROM.get().floatValue();
        baked.microsleepChance = MICROSLEEP_CHANCE.get().floatValue();
        baked.microsleepSeconds = MICROSLEEP_SECONDS.get();
        baked.displacement = DISPLACEMENT.get();
        baked.displacedChance = DISPLACED_CHANCE.get().floatValue();
        baked.displacedBlow = DISPLACED_BLOW.get().floatValue();
        baked.openFractures = OPEN_FRACTURES.get();
        baked.openChance = OPEN_CHANCE.get().floatValue();
        baked.openInfection = OPEN_INFECTION.get().floatValue();
        baked.openBloodLoss = OPEN_BLOOD_LOSS.get().floatValue();
        baked.crookedCost = CROOKED_COST.get().floatValue();
        baked.crookedPenalty = CROOKED_PENALTY.get().floatValue();
        baked.setTear = SET_TEAR.get().floatValue();
        baked.setArteryChance = SET_ARTERY.get().floatValue();
        baked.scream = SCREAM.get();
        baked.screamRange = SCREAM_RANGE.get().floatValue();
        baked.arterial = ARTERIAL.get();
        baked.arterialChance = ARTERIAL_CHANCE.get().floatValue();
        baked.arterialBlow = ARTERIAL_BLOW.get().floatValue();
        baked.arterialRate = ARTERIAL_RATE.get().floatValue();
        baked.bloodPerArterial = BLOOD_PER_ARTERIAL.get().floatValue();
        baked.arterialSpurt = ARTERIAL_SPURT.get();
        baked.faint = FAINT.get();
        baked.faintBloodLoss = FAINT_BLOOD_LOSS.get().floatValue();
        baked.faintChance = FAINT_CHANCE.get().floatValue();
        baked.faintSeconds = FAINT_SECONDS.get();
        baked.faintPain = FAINT_PAIN.get();
        baked.faintClearSeconds = FAINT_CLEAR_SECONDS.get();
        baked.scars = SCARS.get();
        baked.scarStiffness = SCAR_STIFFNESS.get().floatValue();
        baked.burnStiffness = BURN_STIFFNESS.get().floatValue();
        baked.graftStiffness = GRAFT_STIFFNESS.get().floatValue();
        baked.stiffQuality = STIFF_QUALITY.get().floatValue();
        baked.stiffSlow = STIFF_SLOW.get().floatValue();
        baked.scarInfection = SCAR_INFECTION.get().floatValue();
        baked.knitFragility = KNIT_FRAGILITY.get().floatValue();
        baked.ache = ACHE.get();
        baked.acheSeconds = ACHE_SECONDS.get();
        baked.stretch = STRETCH.get();
        baked.stretchGain = STRETCH_GAIN.get().floatValue();
        baked.stretchSelfCap = STRETCH_SELF_CAP.get().floatValue();
        baked.stretchTear = STRETCH_TEAR.get().floatValue();
        baked.wornSplints = SPLINT_WORN.get();
        baked.splintSeconds = SPLINT_SECONDS.get();
        baked.splintRunPenalty = SPLINT_RUN_PENALTY.get();
        baked.splintCeilingSeconds = SPLINT_RUN_CEILING.get();
        baked.splintSlow = SPLINT_SLOW.get().floatValue();
        baked.splintQuality = SPLINT_QUALITY.get().floatValue();
        baked.severing = SEVER.get();
        baked.severBlow = SEVER_BLOW.get().floatValue();
        baked.severChance = SEVER_CHANCE.get().floatValue();
        baked.severHealthyChance = SEVER_HEALTHY.get().floatValue();
        baked.bloodPerLight = BLOOD_PER_LIGHT.get().floatValue();
        baked.bloodPerHeavy = BLOOD_PER_HEAVY.get().floatValue();
        baked.bloodRecovery = BLOOD_RECOVERY.get().floatValue();
        baked.bloodRecoveryHunger = BLOOD_RECOVERY_HUNGER.get();
        baked.bloodLossKills = BLOOD_LOSS_KILLS.get();
        baked.bloodParticles = BLOOD_PARTICLES.get();
        baked.bloodStainSeconds = BLOOD_STAIN_SECONDS.get();
        baked.tourniquetDamage = TOURNIQUET_DAMAGE.get().floatValue();
        baked.uncoveredMultiplier = UNCOVERED_MULTIPLIER.get().floatValue();
        baked.aimedHits = AIMED_HITS.get();

        baked.fractures = FRACTURES.get();
        baked.fractureFall = FRACTURE_FALL.get().floatValue();
        baked.fractureBlow = FRACTURE_BLOW.get().floatValue();
        baked.fractureChance = FRACTURE_CHANCE.get().floatValue();
        baked.fracturePain = FRACTURE_PAIN.get().floatValue();
        baked.crackBlow = CRACK_BLOW.get().floatValue();
        baked.crackChance = CRACK_CHANCE.get().floatValue();
        baked.crackHealSeconds = CRACK_HEAL_SECONDS.get();
        baked.crackBreakChance = CRACK_BREAK_CHANCE.get().floatValue();

        baked.infection = INFECTION.get();
        baked.bleedSecondsBeforeRisk = BLEED_SECONDS_BEFORE_RISK.get();
        baked.blackedSecondsBeforeRisk = BLACKED_SECONDS_BEFORE_RISK.get();
        baked.infectionRisk = INFECTION_RISK.get().floatValue();
        baked.infectionWorsenSeconds = INFECTION_WORSEN_SECONDS.get();
        baked.infectionFeverSeconds = INFECTION_FEVER_SECONDS.get();
        baked.infectionMildDamage = INFECTION_MILD_DAMAGE.get().floatValue();
        baked.infectionSevereDamage = INFECTION_SEVERE_DAMAGE.get().floatValue();

        baked.burns = BURNS.get();
        baked.burnChance = BURN_CHANCE.get().floatValue();
        baked.burnCookSeconds = BURN_COOK_SECONDS.get();
        baked.burnDeepenChance = BURN_DEEPEN.get().floatValue();
        baked.burnFadeSeconds = BURN_FADE_SECONDS.get();
        baked.burnCoolBack = BURN_COOL_BACK.get().floatValue();
        baked.burnIceFactor = BURN_ICE.get().floatValue();
        baked.burnFluidFloor = BURN_FLUID_FLOOR.get().floatValue();
        baked.burnFluidDrain = BURN_FLUID_DRAIN.get().floatValue();
        baked.burnChill = BURN_CHILL.get().floatValue();
        baked.smokeChance = SMOKE_CHANCE.get().floatValue();
        baked.smokeSeconds = SMOKE_SECONDS.get();
        baked.smokeBreath = SMOKE_BREATH.get().floatValue();
        baked.reactions = REACTIONS.get();
        baked.allergies = ALLERGIES.get();
        baked.allergyChance = ALLERGY_CHANCE.get().floatValue();
        baked.grafts = GRAFTS.get();
        baked.graftSeconds = GRAFT_SECONDS.get();
        baked.graftColdFactor = GRAFT_COLD.get().floatValue();
        baked.graftRestore = GRAFT_RESTORE.get().floatValue();
        baked.parasiteChance = PARASITE_CHANCE.get().floatValue();
        baked.parasiteHunger = PARASITE_HUNGER.get().floatValue();
        baked.parasiteDrain = PARASITE_DRAIN.get().floatValue();

        baked.diseases = DISEASES.get();
        baked.coldSeconds = COLD_SECONDS.get();
        baked.poisoningSeconds = POISONING_SECONDS.get();
        baked.feverSeconds = FEVER_SECONDS.get();
        baked.sepsisDamage = SEPSIS_DAMAGE.get().floatValue();
        baked.poisoningDamage = POISONING_DAMAGE.get().floatValue();
        baked.coldRisk = COLD_RISK.get().floatValue();
        baked.foodPoisonChance = FOOD_POISON_CHANCE.get().floatValue();
        baked.concussionSeconds = CONCUSSION_SECONDS.get();
        baked.concussionBlow = CONCUSSION_BLOW.get().floatValue();
        baked.concussionChance = CONCUSSION_CHANCE.get().floatValue();
        baked.shock = SHOCK.get();
        baked.shockBloodLoss = SHOCK_BLOOD_LOSS.get().floatValue();
        baked.shockClearBloodLoss = SHOCK_CLEAR_BLOOD_LOSS.get().floatValue();
        baked.shockRestSeconds = SHOCK_REST_SECONDS.get();
        baked.shockBlackedParts = SHOCK_BLACKED_PARTS.get();
        baked.shockDownedRate = SHOCK_DOWNED_RATE.get().floatValue();
        baked.hypothermia = HYPOTHERMIA.get();
        baked.hypothermiaRisk = HYPOTHERMIA_RISK.get().floatValue();
        baked.hypothermiaWarmSeconds = HYPOTHERMIA_WARM_SECONDS.get();
        baked.pneumothorax = PNEUMOTHORAX.get();
        baked.pneumoBlow = PNEUMO_BLOW.get().floatValue();
        baked.pneumoChance = PNEUMO_CHANCE.get().floatValue();
        baked.pneumoDamage = PNEUMO_DAMAGE.get().floatValue();
        baked.pneumoRampSeconds = PNEUMO_RAMP_SECONDS.get();
        baked.reactionSeconds = REACTION_SECONDS.get();
        baked.reactionDamage = REACTION_DAMAGE.get().floatValue();
        baked.reactionBloodLoss = REACTION_BLOOD_LOSS.get().floatValue();

        baked.effects = EFFECTS.get();
        baked.regenPenalty = REGEN_PENALTY.get().floatValue();
        baked.painkillerSeconds = PAINKILLER_SECONDS.get();
        baked.painkillerFalloff = PAINKILLER_FALLOFF.get().floatValue();
        baked.painkillerToleranceSeconds = PAINKILLER_TOLERANCE_SECONDS.get();
        baked.heartbeat = HEARTBEAT.get();
        baked.assess = ASSESS.get();
        baked.tags = TAGS.get();
        baked.tagStaleSeconds = TAG_STALE_SECONDS.get();
        baked.minigame = MINIGAME.get();
        baked.interruptDamage = INTERRUPT_DAMAGE.get().floatValue();
        baked.downed = DOWNED.get();
        baked.downedSeconds = DOWNED_SECONDS.get();
        baked.downedIgnoredByMobs = DOWNED_IGNORED.get();
        baked.downedDamageSeconds = DOWNED_DAMAGE_SECONDS.get().floatValue();
        baked.stabiliseSeconds = STABILISE_SECONDS.get();
        baked.stabiliseFalloff = STABILISE_FALLOFF.get().floatValue();
        baked.ragdoll = RAGDOLL.get();
        baked.carry = CARRY.get();
        baked.carrySlow = CARRY_SLOW.get();
        baked.downedCarriedRate = DOWNED_CARRIED_RATE.get().floatValue();
        baked.airway = AIRWAY.get();
        baked.airwaySeconds = AIRWAY_SECONDS.get();
        baked.airwayDownedRate = AIRWAY_DOWNED_RATE.get().floatValue();
        baked.cpr = CPR.get();
        baked.arrestSeconds = ARREST_SECONDS.get();
        baked.cprFalloff = CPR_FALLOFF.get().floatValue();
        baked.cprRibChance = CPR_RIB_CHANCE.get().floatValue();
        baked.pressure = PRESSURE.get();
        baked.pressureSeconds = PRESSURE_SECONDS.get();
        baked.whisperSeconds = WHISPER_SECONDS.get();
        baked.phantomSeconds = PHANTOM_SECONDS.get();
        baked.catatoniaChance = CATATONIA_CHANCE.get().floatValue();
        baked.catatoniaSeconds = CATATONIA_SECONDS.get();
        baked.neurolepticChance = NEUROLEPTIC_CHANCE.get().floatValue();
        baked.neurolepticDamage = NEUROLEPTIC_DAMAGE.get().floatValue();
        baked.neurolepticCoolSeconds = NEUROLEPTIC_COOL_SECONDS.get();
        baked.delirium = DELIRIUM.get();
        baked.deliriumBloodLoss = DELIRIUM_BLOOD_LOSS.get().floatValue();
        baked.deliriumClearSeconds = DELIRIUM_CLEAR_SECONDS.get();
        baked.schizophrenia = SCHIZOPHRENIA.get();
        baked.schizophreniaChance = SCHIZOPHRENIA_CHANCE.get().floatValue();
        baked.antipsychoticSeconds = ANTIPSYCHOTIC_SECONDS.get();
        baked.reboundSeconds = REBOUND_SECONDS.get();
        baked.organs = ORGANS.get();
        baked.organBlow = ORGAN_BLOW.get().floatValue();
        baked.organChance = ORGAN_CHANCE.get().floatValue();
        baked.organHeartStopChance = ORGAN_HEART_STOP_CHANCE.get().floatValue();
        baked.organBleed = ORGAN_BLEED.get().floatValue();
        baked.organDamage = ORGAN_DAMAGE.get().floatValue();
        baked.organSepsis = ORGAN_SEPSIS.get().floatValue();
        baked.organHealSeconds = ORGAN_HEAL_SECONDS.get();
        baked.reperfusion = REPERFUSION.get();
        baked.reperfusionDamage = REPERFUSION_DAMAGE.get().floatValue();
        baked.dressingStuckSeconds = DRESSING_STUCK_SECONDS.get();
        baked.anaesthesia = ANAESTHESIA.get();
        baked.anaesthesiaSeconds = ANAESTHESIA_SECONDS.get();
        baked.anaesthesiaArrest = ANAESTHESIA_ARREST.get().floatValue();
        baked.awakeQualityCap = AWAKE_QUALITY_CAP.get().floatValue();
        baked.awakeDamage = AWAKE_DAMAGE.get().floatValue();
        baked.dressings = DRESSINGS.get();
        baked.dressingCleanSeconds = DRESSING_CLEAN_SECONDS.get();
        baked.necrosis = NECROSIS.get();
        baked.strapDeadSeconds = STRAP_DEAD_SECONDS.get();
        baked.rotDeadSeconds = ROT_DEAD_SECONDS.get();
        baked.deadLimbDrain = DEAD_LIMB_DRAIN.get().floatValue();
        baked.deadLimbSepsis = DEAD_LIMB_SEPSIS.get().floatValue();
        baked.amputation = AMPUTATION.get();
        baked.amputationBloodLoss = AMPUTATION_BLOOD_LOSS.get().floatValue();
        baked.amputationOpenBloodLoss = AMPUTATION_OPEN_BLOOD_LOSS.get().floatValue();
        baked.prosthetics = PROSTHETICS.get();
        baked.prostheticRatio = PROSTHETIC_RATIO.get().floatValue();
        baked.prostheticRatioFine = PROSTHETIC_RATIO_FINE.get().floatValue();
        baked.rabies = RABIES.get();
        baked.rabiesChance = RABIES_CHANCE.get().floatValue();
        baked.rabiesSeconds = RABIES_SECONDS.get();
        baked.frenzySeconds = FRENZY_SECONDS.get();
        baked.frenzyDamage = FRENZY_DAMAGE.get().floatValue();
        baked.deafnessSeconds = DEAFNESS_SECONDS.get();
        baked.blast = BLAST.get();
        baked.tearOffBlow = TEAR_OFF_BLOW.get().floatValue();
        baked.tearOffChance = TEAR_OFF_CHANCE.get().floatValue();
        baked.blastTearOff = BLAST_TEAR_OFF.get();
        baked.blastTearChance = BLAST_TEAR_CHANCE.get().floatValue();
        baked.blastFragments = BLAST_FRAGMENTS.get();
        baked.blastWave = BLAST_WAVE.get();
        baked.blastArmourStop = BLAST_ARMOUR_STOP.get().floatValue();
        baked.blastLungChance = BLAST_LUNG_CHANCE.get().floatValue();
        baked.blastGutChance = BLAST_GUT_CHANCE.get().floatValue();
        baked.blastBurnChance = BLAST_BURN_CHANCE.get().floatValue();
        baked.mending = MENDING.get();
        baked.mendingSeconds = MENDING_SECONDS.get();
        baked.mendingTearChance = MENDING_TEAR_CHANCE.get().floatValue();
        baked.sleepless = SLEEPLESS.get();
        baked.sleeplessDays = SLEEPLESS_DAYS.get();
        baked.sleeplessHands = SLEEPLESS_HANDS.get().floatValue();
        baked.sleeplessIllness = SLEEPLESS_ILLNESS.get().floatValue();
        baked.sleeplessMending = SLEEPLESS_MENDING.get().floatValue();
        baked.epinephrineRounds = EPINEPHRINE_ROUNDS.get();
        baked.epinephrineBleed = EPINEPHRINE_BLEED.get().floatValue();
        baked.epinephrineArrestChance = EPINEPHRINE_ARREST.get().floatValue();
        baked.morphineSeconds = MORPHINE_SECONDS.get();
        baked.morphineDepresses = MORPHINE_DEPRESSES.get();
        baked.morphineStopsBreathing = MORPHINE_STOPS.get().floatValue();
        baked.naloxoneShockPain = NALOXONE_SHOCK_PAIN.get();
        baked.naloxoneShockChance = NALOXONE_SHOCK_CHANCE.get().floatValue();
        baked.fluSeconds = FLU_SECONDS.get();
        baked.fluRisk = FLU_RISK.get().floatValue();
        baked.fluCrowd = FLU_CROWD.get();
        baked.fluPneumonia = FLU_PNEUMONIA.get().floatValue();
        baked.stomachSeconds = STOMACH_SECONDS.get();
        baked.stomachChance = STOMACH_CHANCE.get().floatValue();
        baked.stomachDrain = STOMACH_DRAIN.get().floatValue();
        baked.stomachDamage = STOMACH_DAMAGE.get().floatValue();
        baked.thermometer = THERMOMETER.get();
        baked.rinse = RINSE.get();
        baked.rinseSeconds = RINSE_SECONDS.get();
        baked.rinseFloor = RINSE_FLOOR.get();
        baked.referredPain = REFERRED_PAIN.get();
        baked.chill = CHILL.get();
        baked.chillPerOperation = CHILL_PER_OPERATION.get().floatValue();
        baked.chillDegrees = CHILL_DEGREES.get().floatValue();
        baked.chillHypothermia = CHILL_HYPOTHERMIA.get().floatValue();
        baked.chillWarmSeconds = CHILL_WARM_SECONDS.get();
        baked.sneezeChance = SNEEZE_CHANCE.get().floatValue();
        baked.sneezeDamage = SNEEZE_DAMAGE.get().floatValue();
        baked.coldCatching = COLD_CATCHING.get().floatValue();
        baked.pneumoniaAfterSeconds = PNEUMONIA_AFTER_SECONDS.get();
        baked.pneumoniaChance = PNEUMONIA_CHANCE.get().floatValue();
        baked.pneumoniaDamage = PNEUMONIA_DAMAGE.get().floatValue();
        baked.pneumoniaSepsisSeconds = PNEUMONIA_SEPSIS_SECONDS.get();
        baked.peritonitisDamage = PERITONITIS_DAMAGE.get().floatValue();
        baked.peritonitisSepsisSeconds = PERITONITIS_SEPSIS_SECONDS.get();
        baked.adrenaline = ADRENALINE.get();
        baked.adrenalineSeconds = ADRENALINE_SECONDS.get();
        baked.adrenalineRelief = ADRENALINE_RELIEF.get().floatValue();
        baked.adrenalineCrashSeconds = ADRENALINE_CRASH_SECONDS.get();
        baked.adrenalineCrashBite = ADRENALINE_CRASH_BITE.get().floatValue();
        baked.adrenalineShake = ADRENALINE_SHAKE.get().floatValue();
        baked.adrenalinePulse = ADRENALINE_PULSE.get().floatValue();
        baked.fatigue = FATIGUE.get();
        baked.fatigueSeconds = FATIGUE_SECONDS.get();
        baked.fatigueRecoverSeconds = FATIGUE_RECOVER_SECONDS.get();
        baked.fatigueQuality = FATIGUE_QUALITY.get().floatValue();
        baked.fatigueCarrySlow = FATIGUE_CARRY_SLOW.get().floatValue();
        baked.fatigueHold = FATIGUE_HOLD.get().floatValue();
        baked.assist = ASSIST.get();
        baked.assistQuality = ASSIST_QUALITY.get().floatValue();
        baked.assistAwakeCap = ASSIST_AWAKE_CAP.get().floatValue();
        baked.theatre = THEATRE.get();
        baked.grimeInfection = GRIME_INFECTION.get().floatValue();
        baked.roomInfection = ROOM_INFECTION.get().floatValue();
        baked.placeQuality = PLACE_QUALITY.get().floatValue();
        baked.pulse = PULSE.get();
        baked.breathing = BREATHING.get();
        baked.temperature = TEMPERATURE.get();
        baked.ask = ASK.get();
        baked.organBrokenBonus = ORGAN_BROKEN_BONUS.get().floatValue();
        baked.prostheticWearSeconds = PROSTHETIC_WEAR_SECONDS.get();
        baked.prostheticWornRatio = PROSTHETIC_WORN_RATIO.get().floatValue();
        baked.phantomPain = PHANTOM_PAIN.get();
        baked.phantomPainSeconds = PHANTOM_PAIN_SECONDS.get();
        baked.arrows = ARROWS.get();
        baked.arrowChance = ARROW_CHANCE.get().floatValue();
        baked.arrowTear = ARROW_TEAR.get().floatValue();
        baked.bulletsStick = BULLETS_STICK.get();
        baked.bulletStickChance = BULLET_STICK_CHANCE.get().floatValue();
        baked.bulletTear = BULLET_TEAR.get().floatValue();
        baked.weaponsStick = WEAPONS_STICK.get();
        baked.stickChance = STICK_CHANCE.get().floatValue();
        baked.stickDamage = STICK_DAMAGE.get().floatValue();
        baked.stickJostle = STICK_JOSTLE.get().floatValue();
        baked.stickJostleBleed = STICK_JOSTLE_BLEED.get().floatValue();
        baked.stickTear = STICK_TEAR.get().floatValue();
        baked.breathSounds = BREATH_SOUNDS.get();
        baked.findingFreshSeconds = FINDING_FRESH_SECONDS.get();
        baked.bedsideObs = BEDSIDE_OBS.get();
        baked.bedsideSeconds = BEDSIDE_SECONDS.get();
        baked.bedsideRange = BEDSIDE_RANGE.get();
        baked.panic = PANIC.get();
        baked.panicChance = PANIC_CHANCE.get().floatValue();

        baked.bandageHeal = BANDAGE_HEAL.get().floatValue();
        baked.medkitHeal = MEDKIT_HEAL.get().floatValue();
        baked.salveHeal = SALVE_HEAL.get().floatValue();
        baked.surgeryRestore = SURGERY_RESTORE.get().floatValue();
        baked.transfusionRestore = TRANSFUSION_RESTORE.get().floatValue();
        baked.bloodRestore = BLOOD_RESTORE.get().floatValue();
        baked.donationCost = DONATION_COST.get().floatValue();
        baked.maxDonorLoss = MAX_DONOR_LOSS.get().floatValue();

        baked.medicine.put(MedicineKind.BANDAGE, parse(BANDAGES.get()));
        baked.medicine.put(MedicineKind.TOURNIQUET, parse(TOURNIQUETS.get()));
        baked.medicine.put(MedicineKind.SPLINT, parse(SPLINTS.get()));
        baked.medicine.put(MedicineKind.MEDKIT, parse(MEDKITS.get()));
        baked.medicine.put(MedicineKind.SURGERY, parse(SURGERY.get()));
        baked.medicine.put(MedicineKind.PAINKILLER, parse(PAINKILLERS.get()));
        baked.medicine.put(MedicineKind.ANTISEPTIC, parse(ANTISEPTICS.get()));
        baked.medicine.put(MedicineKind.SALVE, parse(SALVES.get()));
        baked.medicine.put(MedicineKind.ANTIBIOTICS, parse(ANTIBIOTICS.get()));
        baked.medicine.put(MedicineKind.TRANSFUSION, parse(SALINES.get()));

        values = baked;
    }

    private static List<ItemMatcher> parse(List<? extends String> entries) {
        List<ItemMatcher> matchers = new ArrayList<>();
        for (String entry : entries) {
            boolean isTag = entry.startsWith("#");
            ResourceLocation id = ResourceLocation.tryParse(isTag ? entry.substring(1) : entry);
            if (id == null) {
                TriageMod.LOGGER.warn("Triage: '{}' in the medicine config is not a valid id, skipping it", entry);
                continue;
            }
            matchers.add(isTag ? new ItemMatcher(TagKey.create(Registries.ITEM, id), null) : new ItemMatcher(null, id));
        }
        return List.copyOf(matchers);
    }

    /** One line of the medicine config: either an item tag or a single item. */
    public record ItemMatcher(@Nullable TagKey<Item> tag, @Nullable ResourceLocation item) {
        public boolean matches(ItemStack stack) {
            if (tag != null) {
                return stack.is(tag);
            }
            return item != null && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(item);
        }
    }

    /** A snapshot of the config, as plain fields. Defaults match the spec above. */
    public static final class Values {
        /**
         * Whether the chart is a readout or a form. See the config comment, and chapter 0
         * of DESIGN.md - this is the one flag the rest of the mod asks about before it
         * decides what a player is allowed to know.
         */
        public boolean realistic = false;

        public int tetanusSeconds = 420;
        public float tetanusChance = 0.0016F;
        public int crushSeconds = 75;
        public int crushStrapSeconds = 180;
        public float relapseChance = 0.18F;

        public float headMax = 10.0F;
        public float thoraxMax = 22.0F;
        public float stomachMax = 16.0F;
        public float armMax = 12.0F;
        public float legMax = 14.0F;
        public float damageScale = 2.0F;
        public float healScale = 2.0F;
        public boolean regeneration = true;
        public float overflowToThorax = 0.5F;
        public float blackedLimbDrain = 0.15F;
        public float oneShotProtectionAbove = 0.5F;
        public float oneShotFloor = 1.0F;

        public int headWeight = 8;
        public int thoraxWeight = 25;
        public int stomachWeight = 15;
        public int armWeight = 13;
        public int legWeight = 13;

        public float lightBleedRate = 0.25F;
        public float heavyBleedRate = 0.7F;
        public float bleedChancePerDamage = 0.05F;
        public float heavyBleedThreshold = 10.0F;
        public int lightBleedSeconds = 60;
        public boolean clots = true;
        public float clotChance = 0.00008F;
        public int clotStillSeconds = 90;
        public int clotClearSeconds = 240;
        public float embolismChance = 0.002F;
        public float embolismDamage = 0.28F;
        public float sleepyFrom = 0.25F;
        public float microsleepFrom = 0.7F;
        public float microsleepChance = 0.010F;
        public int microsleepSeconds = 2;
        public boolean displacement = true;
        public float displacedChance = 0.5F;
        public float displacedBlow = 10.0F;
        public boolean openFractures = true;
        public float openChance = 0.2F;
        public float openInfection = 3.0F;
        public float openBloodLoss = 0.08F;
        public float crookedCost = 0.12F;
        public float crookedPenalty = 0.15F;
        public float setTear = 4.0F;
        public float setArteryChance = 0.2F;
        public boolean scream = true;
        public float screamRange = 32.0F;
        public boolean arterial = true;
        public float arterialChance = 0.12F;
        public float arterialBlow = 14.0F;
        public float arterialRate = 2.0F;
        public float bloodPerArterial = 0.045F;
        public boolean arterialSpurt = true;
        public boolean faint = true;
        public float faintBloodLoss = 0.42F;
        public float faintChance = 0.020F;
        public int faintSeconds = 7;
        public int faintPain = 8;
        public int faintClearSeconds = 25;
        public boolean scars = true;
        public float scarStiffness = 0.10F;
        public float burnStiffness = 0.35F;
        public float graftStiffness = 0.40F;
        public float stiffQuality = 0.35F;
        public float stiffSlow = 0.20F;
        public float scarInfection = 1.7F;
        public float knitFragility = 0.78F;
        public boolean ache = true;
        public int acheSeconds = 75;
        public boolean stretch = true;
        public float stretchGain = 0.20F;
        public float stretchSelfCap = 0.5F;
        public float stretchTear = 2.5F;
        public boolean wornSplints = true;
        public int splintSeconds = 300;
        public int splintRunPenalty = 3;
        public int splintCeilingSeconds = 900;
        public float splintSlow = 0.18F;
        public float splintQuality = 0.25F;
        public boolean severing = true;
        public float severBlow = 7.0F;
        public float severChance = 0.35F;
        public float severHealthyChance = 0.04F;
        public float bloodPerLight = 0.004F;
        public float bloodPerHeavy = 0.010F;
        public float bloodRecovery = 0.0030F;
        public int bloodRecoveryHunger = 7;
        public boolean bloodLossKills = true;
        public boolean bloodParticles = true;
        public int bloodStainSeconds = 300;
        public float tourniquetDamage = 0.05F;

        public float uncoveredMultiplier = 1.3F;
        public boolean aimedHits = true;

        public boolean fractures = true;
        public float fractureFall = 4.0F;
        public float fractureBlow = 14.0F;
        public float fractureChance = 0.5F;
        public float fracturePain = 1.5F;
        public float crackBlow = 8.0F;
        public float crackChance = 0.45F;
        public int crackHealSeconds = 90;
        public float crackBreakChance = 0.5F;

        public boolean infection = true;
        public int bleedSecondsBeforeRisk = 45;
        public int blackedSecondsBeforeRisk = 60;
        public float infectionRisk = 0.012F;
        public int infectionWorsenSeconds = 90;
        public int infectionFeverSeconds = 60;
        public float infectionMildDamage = 0.08F;
        public float infectionSevereDamage = 0.2F;

        public boolean burns = true;
        public float burnChance = 0.08F;
        public int burnCookSeconds = 30;
        public float burnDeepenChance = 0.05F;
        public int burnFadeSeconds = 90;
        public float burnCoolBack = 0.5F;
        public float burnIceFactor = 2.0F;
        public float burnFluidFloor = 0.15F;
        public float burnFluidDrain = 0.02F;
        public float burnChill = 0.004F;
        public float smokeChance = 0.08F;
        public int smokeSeconds = 240;
        public float smokeBreath = 0.35F;
        public boolean reactions = true;
        public boolean allergies = true;
        public float allergyChance = 0.18F;
        public boolean grafts = true;
        public int graftSeconds = 300;
        public float graftColdFactor = 0.33F;
        public float graftRestore = 0.45F;
        public float parasiteChance = 0.25F;
        public float parasiteHunger = 0.08F;
        public float parasiteDrain = 0.0006F;

        public boolean diseases = true;
        public int coldSeconds = 240;
        public int poisoningSeconds = 180;
        public int feverSeconds = 300;
        public float sepsisDamage = 0.3F;
        public float poisoningDamage = 0.12F;
        public float coldRisk = 0.02F;
        public float foodPoisonChance = 0.35F;
        public int concussionSeconds = 75;
        public float concussionBlow = 6.0F;
        public float concussionChance = 0.4F;
        public boolean shock = true;
        public float shockBloodLoss = 0.30F;
        public float shockClearBloodLoss = 0.18F;
        public int shockRestSeconds = 30;
        public int shockBlackedParts = 2;
        public float shockDownedRate = 1.5F;
        public boolean hypothermia = true;
        public float hypothermiaRisk = 0.015F;
        public int hypothermiaWarmSeconds = 45;
        public boolean pneumothorax = true;
        public float pneumoBlow = 9.0F;
        public float pneumoChance = 0.35F;
        public float pneumoDamage = 0.25F;
        public int pneumoRampSeconds = 75;
        public int reactionSeconds = 90;
        public float reactionDamage = 0.45F;
        public float reactionBloodLoss = 0.006F;

        public boolean effects = true;
        public float regenPenalty = 1.0F;
        public int painkillerSeconds = 45;
        public float painkillerFalloff = 0.6F;
        public int painkillerToleranceSeconds = 240;
        public boolean heartbeat = true;
        public boolean assess = true;
        public boolean tags = true;
        public int tagStaleSeconds = 120;
        public boolean minigame = true;
        public float interruptDamage = 3.0F;
        public boolean downed = true;
        public int downedSeconds = 45;
        public boolean downedIgnoredByMobs = true;
        public float downedDamageSeconds = 3.0F;
        public int stabiliseSeconds = 30;
        public float stabiliseFalloff = 0.5F;
        public boolean ragdoll = true;
        public boolean carry = true;
        public double carrySlow = 0.45;
        public float downedCarriedRate = 0.5F;
        public boolean airway = true;
        public int airwaySeconds = 20;
        public float airwayDownedRate = 2.0F;
        public boolean cpr = true;
        public int arrestSeconds = 25;
        public float cprFalloff = 0.6F;
        public float cprRibChance = 0.3F;
        public boolean pressure = true;
        public int pressureSeconds = 12;
        public int whisperSeconds = 25;
        public int phantomSeconds = 180;
        public float catatoniaChance = 0.0008F;
        public int catatoniaSeconds = 30;
        public float neurolepticChance = 0.03F;
        public float neurolepticDamage = 0.5F;
        public int neurolepticCoolSeconds = 25;
        public boolean delirium = true;
        public float deliriumBloodLoss = 0.5F;
        public int deliriumClearSeconds = 20;
        public boolean schizophrenia = true;
        public float schizophreniaChance = 0.1F;
        public int antipsychoticSeconds = 600;
        public int reboundSeconds = 60;
        public boolean organs = true;
        public float organBlow = 11.0F;
        public float organChance = 0.30F;
        public float organHeartStopChance = 0.35F;
        public float organBleed = 0.007F;
        public float organDamage = 0.12F;
        public float organSepsis = 0.012F;
        public int organHealSeconds = 150;
        public boolean reperfusion = true;
        public float reperfusionDamage = 7.0F;
        public int dressingStuckSeconds = 120;
        public boolean anaesthesia = true;
        public int anaesthesiaSeconds = 90;
        public float anaesthesiaArrest = 0.35F;
        public float awakeQualityCap = 0.5F;
        public float awakeDamage = 3.0F;
        public boolean dressings = true;
        public int dressingCleanSeconds = 480;
        public boolean necrosis = true;
        public int strapDeadSeconds = 180;
        public int rotDeadSeconds = 240;
        public float deadLimbDrain = 0.20F;
        public float deadLimbSepsis = 0.010F;
        public boolean amputation = true;
        public float amputationBloodLoss = 0.12F;
        public float amputationOpenBloodLoss = 0.20F;
        public boolean prosthetics = true;
        public float prostheticRatio = 0.45F;
        public float prostheticRatioFine = 0.75F;
        public boolean rabies = true;
        public float rabiesChance = 0.14F;
        public int rabiesSeconds = 300;
        public int frenzySeconds = 90;
        public float frenzyDamage = 0.35F;
        public int deafnessSeconds = 90;
        public boolean blast = true;
        public float tearOffBlow = 6.0F;
        public float tearOffChance = 0.5F;
        public boolean blastTearOff = true;
        public float blastTearChance = 0.35F;
        public boolean blastFragments = true;
        public boolean blastWave = true;
        public float blastArmourStop = 0.85F;
        public float blastLungChance = 0.45F;
        public float blastGutChance = 0.25F;
        public float blastBurnChance = 0.5F;
        public boolean mending = true;
        public int mendingSeconds = 90;
        public float mendingTearChance = 0.10F;
        public boolean sleepless = true;
        public int sleeplessDays = 3;
        public float sleeplessHands = 0.2F;
        public float sleeplessIllness = 1.0F;
        public float sleeplessMending = 0.5F;
        public int epinephrineRounds = 2;
        public float epinephrineBleed = 0.06F;
        public float epinephrineArrestChance = 0.35F;
        public int morphineSeconds = 75;
        public boolean morphineDepresses = true;
        public float morphineStopsBreathing = 0.4F;
        public int naloxoneShockPain = 3;
        public float naloxoneShockChance = 0.5F;
        public int fluSeconds = 300;
        public float fluRisk = 0.0008F;
        public int fluCrowd = 3;
        public float fluPneumonia = 0.005F;
        public int stomachSeconds = 240;
        public float stomachChance = 0.35F;
        public float stomachDrain = 0.004F;
        public float stomachDamage = 0.04F;
        public boolean thermometer = true;
        public boolean rinse = true;
        public int rinseSeconds = 6;
        public int rinseFloor = 1;
        public boolean referredPain = true;
        public boolean chill = true;
        public float chillPerOperation = 0.3F;
        public float chillDegrees = 2.5F;
        public float chillHypothermia = 1.0F;
        public int chillWarmSeconds = 90;
        public float sneezeChance = 0.04F;
        public float sneezeDamage = 0.8F;
        public float coldCatching = 0.25F;
        public int pneumoniaAfterSeconds = 180;
        public float pneumoniaChance = 0.01F;
        public float pneumoniaDamage = 0.06F;
        public int pneumoniaSepsisSeconds = 300;
        public float peritonitisDamage = 0.12F;
        public int peritonitisSepsisSeconds = 240;
        public boolean adrenaline = true;
        public int adrenalineSeconds = 25;
        public float adrenalineRelief = 0.8F;
        public int adrenalineCrashSeconds = 20;
        public float adrenalineCrashBite = 0.3F;
        public float adrenalineShake = 0.15F;
        public float adrenalinePulse = 30.0F;
        public boolean fatigue = true;
        public int fatigueSeconds = 90;
        public int fatigueRecoverSeconds = 45;
        public float fatigueQuality = 0.25F;
        public float fatigueCarrySlow = 0.5F;
        public float fatigueHold = 0.15F;
        public boolean assist = true;
        public float assistQuality = 0.15F;
        public float assistAwakeCap = 0.7F;
        public boolean theatre = true;
        public float grimeInfection = 0.55F;
        public float roomInfection = 0.18F;
        public float placeQuality = 0.25F;
        public boolean pulse = true;
        public boolean breathing = true;
        public boolean temperature = true;
        public boolean ask = true;
        public float organBrokenBonus = 2.0F;
        public int prostheticWearSeconds = 2400;
        public float prostheticWornRatio = 0.55F;
        public boolean phantomPain = true;
        public int phantomPainSeconds = 90;
        public boolean arrows = true;
        public float arrowChance = 0.35F;
        public float arrowTear = 3.0F;
        public boolean bulletsStick = true;
        public float bulletStickChance = 0.25F;
        public float bulletTear = 3.0F;
        public boolean weaponsStick = true;
        public float stickChance = 0.06F;
        public float stickDamage = 5.0F;
        public float stickJostle = 0.25F;
        public float stickJostleBleed = 0.06F;
        public float stickTear = 4.0F;
        public boolean breathSounds = true;
        public int findingFreshSeconds = 120;
        public boolean bedsideObs = true;
        public int bedsideSeconds = 40;
        public int bedsideRange = 3;
        public boolean panic = true;
        public float panicChance = 0.035F;

        public float bandageHeal = 2.0F;
        public float medkitHeal = 12.0F;
        public float salveHeal = 3.0F;
        public float surgeryRestore = 0.3F;
        public float transfusionRestore = 0.5F;
        public float bloodRestore = 0.85F;
        public float donationCost = 0.3F;
        public float maxDonorLoss = 0.5F;

        public final Map<MedicineKind, List<ItemMatcher>> medicine = new EnumMap<>(MedicineKind.class);

        public List<ItemMatcher> medicine(MedicineKind kind) {
            return medicine.getOrDefault(kind, List.of());
        }
    }
}
