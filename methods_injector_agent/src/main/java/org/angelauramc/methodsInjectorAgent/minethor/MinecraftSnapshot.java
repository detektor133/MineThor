package org.angelauramc.methodsInjectorAgent.minethor;

final class MinecraftSnapshot {
    final boolean connected;
    final int x;
    final int y;
    final int z;
    final int yaw;
    final int health;
    final int maxHealth;
    final int food;
    final int maxFood;
    final int armor;
    final int xpLevel;
    final int selectedHotbarSlot;
    final int selectedInventorySlot;

    MinecraftSnapshot(
            boolean connected,
            int x,
            int y,
            int z,
            int yaw,
            int health,
            int maxHealth,
            int food,
            int maxFood,
            int armor,
            int xpLevel,
            int selectedHotbarSlot,
            int selectedInventorySlot
    ) {
        this.connected = connected;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.health = health;
        this.maxHealth = maxHealth;
        this.food = food;
        this.maxFood = maxFood;
        this.armor = armor;
        this.xpLevel = xpLevel;
        this.selectedHotbarSlot = selectedHotbarSlot;
        this.selectedInventorySlot = selectedInventorySlot;
    }

    static MinecraftSnapshot disconnected() {
        return new MinecraftSnapshot(false, 0, 0, 0, 0, 0, 20, 0, 20, 0, 0, -1, -1);
    }
}
