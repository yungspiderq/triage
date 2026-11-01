package com.claudemods.triage.client;

import com.claudemods.triage.TriageMod;
import com.claudemods.triage.body.Carry;
import com.claudemods.triage.client.screen.BodyScreen;
import com.claudemods.triage.net.PutDownPayload;
import net.minecraft.client.Minecraft;
import com.claudemods.triage.net.PacketDistributor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.Pose;
import com.claudemods.triage.client.ClientHooks.ViewportEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.player.Input;
import com.claudemods.triage.client.ClientHooks.InteractionKeyMappingTriggered;
import com.claudemods.triage.client.ClientHooks.MovementInputUpdateEvent;
import com.claudemods.triage.client.ClientHooks.ScreenInit;
import com.claudemods.triage.client.ClientHooks.EntityLeaveLevelEvent;

public final class ClientEvents {
    /** Whether the pose currently forced on the local player is ours to clear. */
    private static boolean posed;

    private ClientEvents() {
    }

    public static void onLoggingOut() {
        ClientBody.reset();
        ClientTags.reset();
    }

    /**
     * Somebody walked out of range, died or logged off: their mark goes with them.
     *
     * <p>The side check is not decoration. In single player the integrated server lives
     * in this process and fires the same event, and this class is loaded there too.
     */
    public static void onEntityLeft(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide() && event.getEntity() instanceof Player player) {
            ClientTags.forget(player.getId());
        }
    }

    /**
     * A swing at somebody who is not there.
     *
     * <p>Caught before the packet goes out, because the server has never heard of that
     * entity and a client telling it otherwise is a client sending nonsense. The arm
     * still moves - the player really did swing - and the figure is simply gone, which
     * is the whole of what happens when you take a swing at a hallucination.
     */
    public static void onAttack(InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (!Phantoms.aimedAt(minecraft.hitResult)) {
            return;
        }
        Phantoms.struck();
        event.setSwingHand(true);
        event.setCanceled(true);
    }

    /**
     * The horizon, which a body in a bad way does not hold level.
     *
     * <p>Roll only. Pitch and yaw are where the player is looking and taking those away
     * is taking the game away; roll is the head itself, and a head that has been hit does
     * not keep its head still.
     */
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.isSpectator()) {
            return;
        }
        float roll = Symptoms.roll(minecraft.player.tickCount + (float) event.getPartialTick());
        if (roll != 0.0F) {
            event.setRoll(event.getRoll() + roll);
        }
    }

    /** And the world closing in, which is a narrower view rather than a darker one. */
    public static void onFov(ViewportEvent.ComputeFov event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.isSpectator()) {
            return;
        }
        double factor = Symptoms.fov();
        if (factor < 1.0) {
            event.setFOV(event.getFOV() * factor);
        }
    }

    public static void onClientTick() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientBody.tickGiveUp(ModKeys.GIVE_UP.isDown());
        Phantoms.tick(minecraft);
        Whispers.tick(minecraft);
        poseSelf(minecraft);
        putDown(minecraft);
        while (ModKeys.OPEN_BODY.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) {
                minecraft.setScreen(new BodyScreen());
            }
        }
    }

    /**
     * Tapping the give-up key while carrying somebody sets them down instead.
     *
     * <p>The two never overlap - a player carrying somebody is on their feet, and a
     * player on the floor is carrying nobody - so one key covers both without ever
     * having to guess which was meant. Giving up needs the key held for two seconds,
     * so a tap cannot do it by accident either.
     */
    private static void putDown(Minecraft minecraft) {
        while (ModKeys.GIVE_UP.consumeClick()) {
            if (minecraft.player != null && Carry.carrying(minecraft.player) != null) {
                PacketDistributor.sendToServer(new PutDownPayload());
            }
        }
    }

    /**
     * A downed player stops steering.
     *
     * <p>The movement-speed attribute holds them still on land and does nothing at
     * all in water: swimming is driven by the input, not by walking speed, so they
     * could paddle away from a rescue perfectly happily. Zeroing the input is the
     * only thing that covers walking, swimming, jumping and climbing at once.
     *
     * <p>Buoyancy still floats them up, which is fine - that is what happens to
     * somebody unconscious in water, and it is not steering.
     */
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (!ClientBody.get().downed()) {
            return;
        }
        Input input = event.getInput();
        input.forwardImpulse = 0.0F;
        input.leftImpulse = 0.0F;
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        input.jumping = false;
        input.shiftKeyDown = false;
    }

    /**
     * Lies the local player down on their own screen.
     *
     * <p>The server forces the pose too, which is what everybody else sees - but a
     * player works out their own pose every tick from their own state, and knows
     * nothing about being downed unless told. Without this the animation was prone
     * and the camera was standing.
     *
     * <p>Not while somebody is carrying them, though, and that is not cosmetic. The
     * server sends no position at all for a passenger - every client works it out from
     * the vehicle, and the sum subtracts the passenger's own vehicle attachment point,
     * which comes from their dimensions, which come from their pose. Forcing a pose
     * here and nowhere else meant this client and the carrier's client were measuring
     * the same offset from two different bodies, and the two views drifted apart: the
     * carried player rode the shoulder while everyone else watched them lie where they
     * fell. Being a passenger already looks like being carried.
     */
    private static void poseSelf(Minecraft minecraft) {
        if (minecraft.player == null) {
            return;
        }
        boolean downed = ClientBody.get().downed() && !minecraft.player.isPassenger();
        if (downed) {
            com.claudemods.triage.compat.Poses.force(minecraft.player, Pose.SWIMMING);
            posed = true;
        } else if (posed) {
            // Only clear a pose we set ourselves; something else may own it.
            com.claudemods.triage.compat.Poses.force(minecraft.player, null);
            posed = false;
        }
    }

    /**
     * A button on the inventory, because that is where a player goes to look at
     * themselves, and expecting them to remember a keybind for it is optimistic.
     */
    public static void onInventoryOpened(ScreenInit event) {
        if (event.getScreen() instanceof InventoryScreen inventory) {
            event.addListener(new BodyButton(inventory.leftPos - 22, inventory.topPos + 8));
        }
    }
}
