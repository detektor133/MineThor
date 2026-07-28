package net.kdt.pojavlaunch.minethor;

public interface CompanionStateProvider {
    CompanionSnapshot currentSnapshot();

    CompanionSnapshot selectHotbarSlot(int slot);

    CompanionSnapshot selectInventorySlot(int slot);

    void close();
}
