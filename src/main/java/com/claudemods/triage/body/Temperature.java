package com.claudemods.triage.body;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * How warm somebody is, which is the slowest vital and the only one that sees the future.
 *
 * <p>A pulse and a chest tell you what is happening now. A temperature tells you what is
 * going to happen: it starts climbing while an infection is still a colour on the chart
 * and hours before it is a fever with a name, and it is the reason infection has ever
 * been survivable outside a hospital. Somebody who checks a forehead every so often
 * catches the thing that kills quietly.
 *
 * <p>It also runs the other way, and that half is the one people get wrong. Cold blood
 * and lost blood both pull it down, so a casualty who is bleeding out goes cold - and a
 * cold casualty stops clotting, which is the loop that kills them. The mod already has
 * both ends of that. This is the number that shows it closing.
 *
 * <p>Degrees rather than a bar, because a temperature is a number people already know
 * how to read: thirty-seven is fine and thirty-nine is not, and nobody needs that
 * explained.
 */
public record Temperature(float degrees, int band) {
    public static final int LOW = 0;
    public static final int NORMAL = 1;
    /** Warm enough to mean something is starting. This is the one worth catching. */
    public static final int RAISED = 2;
    public static final int HIGH = 3;

    private static final float RESTING = 37.0F;

    public static Temperature of(BodyHealth body) {
        float degrees = RESTING;

        // The one that comes in waves. Everything else on this list is a state: it is there
        // or it is not, and a figure taken now says which. This one is normal at the bottom
        // of its cycle - so a thermometer used at the wrong moment is honest, accurate and
        // wrong, and the only way to catch it is to measure twice ten seconds apart. There
        // is nothing else in this mod that can be right and wrong at the same time.
        if (body.has(Disease.RELAPSE)) {
            float phase = (body.diseaseTicks(Disease.RELAPSE) % 400) / 400.0F;
            float wave = Math.max(0.0F, Mth.sin(phase * Mth.TWO_PI));
            degrees += wave * wave * 2.1F;
        }

        // Infection first, and before any disease has been declared: this is the whole
        // point of taking it. A wound going bad is warm long before it has a name.
        int worst = PartState.INFECTION_NONE;
        int count = 0;
        for (BodyPart part : BodyPart.VALUES) {
            int level = body.part(part).infection;
            if (level > PartState.INFECTION_NONE) {
                count++;
                worst = Math.max(worst, level);
            }
        }
        if (worst == PartState.INFECTION_SEVERE) {
            degrees += 1.4F;
        } else if (worst > PartState.INFECTION_NONE) {
            degrees += 0.7F;
        }
        // More than one going at once is worse than the worst of them.
        if (count > 1) {
            degrees += Math.min(0.6F, (count - 1) * 0.3F);
        }

        // One ladder, worst rung first. Both of the new two run hot, and a forehead is
        // the cheapest way anybody finds out - but neither is sepsis, and the reading has
        // to keep saying which of them it is.
        if (body.has(Disease.SEPSIS)) {
            degrees += 2.0F;
        } else if (body.has(Disease.PERITONITIS)) {
            degrees += 1.6F;
        } else if (body.has(Disease.PNEUMONIA)) {
            degrees += 1.4F;
        } else if (body.has(Disease.FEVER)) {
            degrees += 1.3F;
        } else if (body.has(Disease.FLU)) {
            // Hot, and the figure is the only thing that says how hot - which is the
            // reason the instrument and the virus arrived together.
            degrees += 1.1F;
        }
        // Dead flesh on a living body is the hottest thing in here for a reason.
        for (BodyPart part : BodyPart.VALUES) {
            if (body.part(part).necrotic()) {
                degrees += 0.8F;
                break;
            }
        }

        // An open body loses heat, and this is where it shows before it becomes the
        // illness. The oldest circle in trauma surgery, and the mod had two thirds of it
        // already: cold does not clot, and blood loss cools.
        degrees -= Chill.degrees(body);

        // And down. Losing volume cools somebody, and cold blood does not clot - which
        // is the loop this number exists to make visible before it closes.
        if (body.has(Disease.HYPOTHERMIA)) {
            degrees -= 4.0F;
        } else if (body.has(Disease.COLD)) {
            degrees -= 0.7F;
        }
        // Enough that emptying somebody actually reads as cold rather than as nearly
        // cold. This is the loop the number exists to show closing: a casualty who is
        // bleeding goes cold, and cold blood does not clot.
        degrees -= 2.2F * body.bloodLoss();

        float clamped = Mth.clamp(degrees, 28.0F, 42.5F);
        return new Temperature(clamped, band(clamped));
    }

    private static int band(float degrees) {
        if (degrees < 35.5F) {
            return LOW;
        }
        if (degrees >= 38.8F) {
            return HIGH;
        }
        // The narrow one, and the useful one: this is where a wound going bad shows up
        // while there is still time to do something cheap about it.
        return degrees >= 37.6F ? RAISED : NORMAL;
    }

    /** Whether this is worth saying out loud without being asked. */
    public boolean alarming() {
        return band == LOW || band == HIGH;
    }

    public Component bandTitle() {
        return Component.translatable(switch (band) {
            case LOW -> "triage.temp.low";
            case RAISED -> "triage.temp.raised";
            case HIGH -> "triage.temp.high";
            default -> "triage.temp.normal";
        });
    }

    public ChatFormatting colour() {
        return switch (band) {
            case LOW -> ChatFormatting.AQUA;
            case RAISED -> ChatFormatting.GOLD;
            case HIGH -> ChatFormatting.RED;
            default -> ChatFormatting.GREEN;
        };
    }

    /** One decimal, which is as fine as a hand on a forehead has any business being. */
    public String readout() {
        return String.format("%.1f", degrees);
    }

    /**
     * The same figure as a whole number of tenths, for writing down.
     *
     * <p>Tenths rather than a float because this goes on the notes, and the notes travel
     * and are saved: an integer says exactly what was measured and says the same thing on
     * both sides of a packet.
     */
    public int tenths() {
        return Math.round(degrees * 10.0F);
    }

    /** And back again, for a chart printing what somebody wrote down. */
    public static String readout(int tenths) {
        return String.format("%.1f", tenths / 10.0F);
    }
}
