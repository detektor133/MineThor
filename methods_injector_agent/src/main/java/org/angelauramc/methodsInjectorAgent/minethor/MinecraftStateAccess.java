package org.angelauramc.methodsInjectorAgent.minethor;

final class MinecraftStateAccess {
    private final MinecraftAdapter[] adapters = {
            new MappedMinecraftAdapter(),
            new Obfuscated1201MinecraftAdapter()
    };
    private MinecraftAdapter activeAdapter;

    MinecraftSnapshot readSnapshot() {
        MinecraftAdapter adapter = adapter();
        if (adapter == null) return MinecraftSnapshot.disconnected();
        return adapter.readSnapshot();
    }

    boolean selectHotbarSlot(int slot) {
        MinecraftAdapter adapter = adapter();
        return adapter != null && adapter.selectHotbarSlot(slot);
    }

    private MinecraftAdapter adapter() {
        if (activeAdapter != null && activeAdapter.isAvailable()) {
            return activeAdapter;
        }

        for (MinecraftAdapter adapter : adapters) {
            if (!adapter.isAvailable()) continue;

            activeAdapter = adapter;
            System.out.println("MineThorBridge: using Minecraft adapter " + adapter.name());
            return adapter;
        }
        activeAdapter = null;
        return null;
    }
}
