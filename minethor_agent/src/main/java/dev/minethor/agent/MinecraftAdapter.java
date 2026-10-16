package dev.minethor.agent;

import java.util.concurrent.Executor;

interface MinecraftAdapter {
    String name();

    boolean isAvailable();

    MinecraftSnapshot readSnapshot();

    boolean selectHotbarSlot(int slot);

    /** Schedules work on the game's main thread, or null if this game version offers no way to do so. */
    default Executor clientExecutor() {
        return null;
    }
}
