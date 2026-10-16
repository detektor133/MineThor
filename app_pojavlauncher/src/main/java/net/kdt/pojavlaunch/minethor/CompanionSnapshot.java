package net.kdt.pojavlaunch.minethor;

public class CompanionSnapshot {
    public final boolean connected;
    public final int lastAppliedCommandId;
    public final PlayerSnapshot player;
    public final InventorySnapshot inventory;

    public CompanionSnapshot(boolean connected, int lastAppliedCommandId, PlayerSnapshot player, InventorySnapshot inventory) {
        this.connected = connected;
        this.lastAppliedCommandId = lastAppliedCommandId;
        this.player = player;
        this.inventory = inventory;
    }
}
