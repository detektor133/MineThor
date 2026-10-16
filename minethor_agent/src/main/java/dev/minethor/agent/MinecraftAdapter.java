package dev.minethor.agent;

interface MinecraftAdapter {
    String name();

    boolean isAvailable();

    MinecraftSnapshot readSnapshot();

    boolean selectHotbarSlot(int slot);
}
