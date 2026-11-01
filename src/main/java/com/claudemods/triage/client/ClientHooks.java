package com.claudemods.triage.client;

import com.claudemods.triage.event.Event;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.Input;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.level.Level;

/**
 * The client half of the wrappers; see {@link Event} for what these are and why.
 *
 * <p>Same bargain as on the server and a slightly better one here, because this is where
 * Fabric has the fewest events of its own: the mod bends the camera, eats a sound, hides
 * an arm that is not there and writes on a name tag, and not one of those is a callback on
 * either loader - they are mixins here and patched events there. What the four client
 * classes say about all that is the same on both, so it is written once and copied.
 */
public final class ClientHooks {
    private ClientHooks() {
    }

    /** A player being drawn, before and after - where a missing limb is switched off. */
    public abstract static class RenderPlayerEvent {
        private final AbstractClientPlayer entity;
        private final PlayerRenderer renderer;

        private RenderPlayerEvent(AbstractClientPlayer entity, PlayerRenderer renderer) {
            this.entity = entity;
            this.renderer = renderer;
        }

        public AbstractClientPlayer getEntity() {
            return entity;
        }

        public PlayerRenderer getRenderer() {
            return renderer;
        }

        public static final class Pre extends RenderPlayerEvent {
            public Pre(AbstractClientPlayer entity, PlayerRenderer renderer) {
                super(entity, renderer);
            }
        }

        public static final class Post extends RenderPlayerEvent {
            public Post(AbstractClientPlayer entity, PlayerRenderer renderer) {
                super(entity, renderer);
            }
        }
    }

    /** The bare arm in the first person. */
    public static final class RenderArmEvent extends Event {
        private final AbstractClientPlayer player;
        private final HumanoidArm arm;

        public RenderArmEvent(AbstractClientPlayer player, HumanoidArm arm) {
            this.player = player;
            this.arm = arm;
        }

        public AbstractClientPlayer getPlayer() {
            return player;
        }

        public HumanoidArm getArm() {
            return arm;
        }
    }

    /** The same arm with whatever it is holding. */
    public static final class RenderHandEvent extends Event {
        private final InteractionHand hand;

        public RenderHandEvent(InteractionHand hand) {
            this.hand = hand;
        }

        public InteractionHand getHand() {
            return hand;
        }
    }

    public static final class RenderNameTagEvent {
        private final Entity entity;
        private Component content;

        public RenderNameTagEvent(Entity entity, Component content) {
            this.entity = entity;
            this.content = content;
        }

        public Entity getEntity() {
            return entity;
        }

        public Component getContent() {
            return content;
        }

        public void setContent(Component value) {
            content = value;
        }
    }

    /** A sound about to be played, which deafness may swap for a duller one. */
    public static final class PlaySoundEvent {
        private SoundInstance sound;

        public PlaySoundEvent(SoundInstance sound) {
            this.sound = sound;
        }

        public SoundInstance getSound() {
            return sound;
        }

        public void setSound(SoundInstance value) {
            sound = value;
        }
    }

    /** What the eye is doing: how level it is holding the horizon, and how wide it is. */
    public static final class ViewportEvent {
        private ViewportEvent() {
        }

        public static final class ComputeCameraAngles {
            private final float partialTick;
            private float roll;

            public ComputeCameraAngles(float partialTick, float roll) {
                this.partialTick = partialTick;
                this.roll = roll;
            }

            public double getPartialTick() {
                return partialTick;
            }

            public float getRoll() {
                return roll;
            }

            public void setRoll(float value) {
                roll = value;
            }
        }

        public static final class ComputeFov {
            private double fov;

            public ComputeFov(double fov) {
                this.fov = fov;
            }

            public double getFOV() {
                return fov;
            }

            public void setFOV(double value) {
                fov = value;
            }
        }
    }

    /** Swinging at something. Cancelling it stops the packet, not the arm. */
    public static final class InteractionKeyMappingTriggered extends Event {
        private final boolean attack;
        private boolean swingHand;

        public InteractionKeyMappingTriggered(boolean attack) {
            this.attack = attack;
        }

        public boolean isAttack() {
            return attack;
        }

        public void setSwingHand(boolean value) {
            swingHand = value;
        }

        public boolean shouldSwingHand() {
            return swingHand;
        }
    }

    public static final class MovementInputUpdateEvent {
        private final Input input;

        public MovementInputUpdateEvent(Input input) {
            this.input = input;
        }

        public Input getInput() {
            return input;
        }
    }

    /** A screen that has just been built, and can still be added to. */
    public static final class ScreenInit {
        private final Screen screen;

        public ScreenInit(Screen screen) {
            this.screen = screen;
        }

        public Screen getScreen() {
            return screen;
        }

        public void addListener(AbstractWidget widget) {
            Screens.getButtons(screen).add(widget);
        }
    }

    /** Somebody going out of sight, whether they walked, died or logged off. */
    public static final class EntityLeaveLevelEvent {
        private final Entity entity;

        public EntityLeaveLevelEvent(Entity entity) {
            this.entity = entity;
        }

        public Entity getEntity() {
            return entity;
        }

        public Level getLevel() {
            return entity.level();
        }
    }
}
