package dev.minethor.agent;

import java.util.concurrent.Executor;

final class MinecraftStateAccess {
    private final MinecraftAdapter[] adapters = {
            new DescriptorMinecraftAdapter(),
            new MappedMinecraftAdapter()
    };
    private MinecraftAdapter activeAdapter;

    MinecraftSnapshot readSnapshot() {
        MinecraftAdapter adapter = adapter();
        if (adapter == null) return MinecraftSnapshot.disconnected();
        return adapter.readSnapshot();
    }

    Executor clientExecutor() {
        MinecraftAdapter adapter = adapter();
        return adapter == null ? null : adapter.clientExecutor();
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
