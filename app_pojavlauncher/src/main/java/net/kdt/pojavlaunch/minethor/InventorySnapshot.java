package net.kdt.pojavlaunch.minethor;

public class InventorySnapshot {
    public static final int MAIN_SLOT_COUNT = 36;
    public static final int ARMOR_SLOT_COUNT = 4;
    public static final int OFFHAND_SLOT_COUNT = 1;

    public final int selectedHotbarSlot;
    public final int selectedInventorySlot;
    public final InventorySlotSnapshot[] mainSlots;
    public final InventorySlotSnapshot[] armorSlots;
    public final InventorySlotSnapshot[] offhandSlots;

    public InventorySnapshot(int selectedHotbarSlot, int selectedInventorySlot) {
        this(selectedHotbarSlot, selectedInventorySlot, emptySlots(MAIN_SLOT_COUNT), emptySlots(ARMOR_SLOT_COUNT), emptySlots(OFFHAND_SLOT_COUNT));
    }

    public InventorySnapshot(
            int selectedHotbarSlot,
            int selectedInventorySlot,
            InventorySlotSnapshot[] mainSlots,
            InventorySlotSnapshot[] armorSlots,
            InventorySlotSnapshot[] offhandSlots
    ) {
        this.selectedHotbarSlot = selectedHotbarSlot;
        this.selectedInventorySlot = selectedInventorySlot;
        this.mainSlots = normalizedSlots(mainSlots, MAIN_SLOT_COUNT);
        this.armorSlots = normalizedSlots(armorSlots, ARMOR_SLOT_COUNT);
        this.offhandSlots = normalizedSlots(offhandSlots, OFFHAND_SLOT_COUNT);
    }

    public InventorySnapshot withSelectedHotbarSlot(int slot) {
        return new InventorySnapshot(slot, selectedInventorySlot, mainSlots, armorSlots, offhandSlots);
    }

    public InventorySnapshot withSelectedInventorySlot(int slot) {
        return new InventorySnapshot(selectedHotbarSlot, slot, mainSlots, armorSlots, offhandSlots);
    }

    private static InventorySlotSnapshot[] normalizedSlots(InventorySlotSnapshot[] source, int size) {
        InventorySlotSnapshot[] slots = emptySlots(size);
        if (source == null) return slots;

        int length = Math.min(source.length, size);
        for (int i = 0; i < length; i++) {
            if (source[i] != null) slots[i] = source[i];
        }
        return slots;
    }

    private static InventorySlotSnapshot[] emptySlots(int size) {
        InventorySlotSnapshot[] slots = new InventorySlotSnapshot[size];
        for (int i = 0; i < size; i++) {
            slots[i] = new InventorySlotSnapshot(i, "", "", 0, 0, 0);
        }
        return slots;
    }
}
