package net.kdt.pojavlaunch.minethor;

public class InventorySnapshot {
    public final int selectedHotbarSlot;
    public final int selectedInventorySlot;

    public InventorySnapshot(int selectedHotbarSlot, int selectedInventorySlot) {
        this.selectedHotbarSlot = selectedHotbarSlot;
        this.selectedInventorySlot = selectedInventorySlot;
    }

    public InventorySnapshot withSelectedHotbarSlot(int slot) {
        return new InventorySnapshot(slot, selectedInventorySlot);
    }

    public InventorySnapshot withSelectedInventorySlot(int slot) {
        return new InventorySnapshot(selectedHotbarSlot, slot);
    }
}
