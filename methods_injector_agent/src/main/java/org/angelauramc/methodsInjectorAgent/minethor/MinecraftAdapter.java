package org.angelauramc.methodsInjectorAgent.minethor;

interface MinecraftAdapter {
    String name();

    boolean isAvailable();

    MinecraftSnapshot readSnapshot();

    boolean selectHotbarSlot(int slot);
}
