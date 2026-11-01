package com.claudemods.triage.client;

import java.lang.reflect.Method;

/**
 * Whether Iris is currently drawing this player's world through a shader pack.
 *
 * <p>The whole-body colour wash in {@link SkinLayer} is a translucent render layer. Iris
 * treats that layer differently from vanilla: in several packs it is composited after the
 * shader pass, and the result is that the player and every other casualty go see-through.
 * The wash is a nice-to-have; being able to see your own arms is not.
 *
 * <p>The test has to be reflective. Iris is not a dependency, its API package has moved
 * before, and a client without it must never load a class that mentions it. The answer is
 * cached for a couple of seconds because this is asked once per rendered player per frame
 * and a shader pack does not change between frames.
 */
final class IrisCompat {
    private static final String API = "net.irisshaders.iris.api.v0.IrisApi";
    private static final long CHECK_MS = 2000L;

    private static boolean resolved;
    private static boolean available;
    private static Object api;
    private static Method isShaderPackInUse;

    private static boolean shaders;
    private static long checkedAt;

    private IrisCompat() {
    }

    static boolean shadersActive() {
        long now = System.currentTimeMillis();
        if (!resolved) {
            resolve();
        }
        if (!available) {
            return false;
        }
        if (now - checkedAt < CHECK_MS) {
            return shaders;
        }
        checkedAt = now;
        try {
            shaders = (boolean) isShaderPackInUse.invoke(api);
        } catch (Throwable ignored) {
            // An API that changed shape, or a client shutting down. Either way the safe
            // answer is "no shaders": the vanilla look is the one that always works.
            shaders = false;
        }
        return shaders;
    }

    private static void resolve() {
        resolved = true;
        try {
            Class<?> type = Class.forName(API);
            Method getInstance = type.getMethod("getInstance");
            api = getInstance.invoke(null);
            isShaderPackInUse = type.getMethod("isShaderPackInUse");
            available = api != null;
        } catch (Throwable ignored) {
            available = false;
        }
    }
}
