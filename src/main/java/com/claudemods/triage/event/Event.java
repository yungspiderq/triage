package com.claudemods.triage.event;

/**
 * The seven small classes beside this one wear NeoForge's names on purpose.
 *
 * <p>Eight classes in this package are the mod deciding what happens when somebody is hit,
 * eats, dies, logs in or reaches out to another player, and they run to about eleven
 * hundred lines. None of that is about a mod loader; all of it is written against an event
 * object with getters on it. Fabric hands the same moments over as callback arguments
 * instead, and rewriting eleven hundred lines to take arguments would have forked every
 * one of those decisions into a second copy to keep in step by hand, forever.
 *
 * <p>So the argument is wrapped in the shape the handlers already read, {@link Events}
 * does the wrapping, and the handlers themselves are copied across untouched by
 * tools/carry.py.
 *
 * <p>What is here is only what those handlers actually ask for. This is not NeoForge's
 * event system and does not pretend to be one: there is no bus, no priority, no
 * subscribing, and cancelling something means whatever the one place that unwraps it says
 * it means.
 */
public abstract class Event {
    private boolean canceled;

    public void setCanceled(boolean value) {
        canceled = value;
    }

    public boolean isCanceled() {
        return canceled;
    }
}
