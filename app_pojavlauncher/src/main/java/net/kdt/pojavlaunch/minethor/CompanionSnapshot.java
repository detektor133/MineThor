package net.kdt.pojavlaunch.minethor;

public class CompanionSnapshot {
    public final boolean connected;
    public final PlayerSnapshot player;
    public final InventorySnapshot inventory;

    public CompanionSnapshot(boolean connected, PlayerSnapshot player, InventorySnapshot inventory) {
        this.connected = connected;
        this.player = player;
        this.inventory = inventory;
    }
}
