package net.kdt.pojavlaunch.minethor;

public class InventorySlotSnapshot {
    public final int index;
    public final String itemId;
    public final String name;
    public final String iconKey;
    public final int count;
    public final int damage;
    public final int maxDamage;

    public InventorySlotSnapshot(int index, String itemId, String name, int count, int damage, int maxDamage) {
        this(index, itemId, name, "", count, damage, maxDamage);
    }

    public InventorySlotSnapshot(int index, String itemId, String name, String iconKey, int count, int damage, int maxDamage) {
        this.index = index;
        this.itemId = itemId;
        this.name = name;
        this.iconKey = iconKey;
        this.count = count;
        this.damage = damage;
        this.maxDamage = maxDamage;
    }

    public boolean isEmpty() {
        return itemId == null || itemId.isEmpty() || count <= 0;
    }
}
