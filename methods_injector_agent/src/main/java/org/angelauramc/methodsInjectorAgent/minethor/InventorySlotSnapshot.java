package org.angelauramc.methodsInjectorAgent.minethor;

final class InventorySlotSnapshot {
    final int index;
    final String itemId;
    final String name;
    final int count;
    final int damage;
    final int maxDamage;

    InventorySlotSnapshot(int index, String itemId, String name, int count, int damage, int maxDamage) {
        this.index = index;
        this.itemId = itemId;
        this.name = name;
        this.count = count;
        this.damage = damage;
        this.maxDamage = maxDamage;
    }

    static InventorySlotSnapshot empty(int index) {
        return new InventorySlotSnapshot(index, "", "", 0, 0, 0);
    }
}
