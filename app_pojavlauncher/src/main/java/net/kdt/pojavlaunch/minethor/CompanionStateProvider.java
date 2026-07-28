package net.kdt.pojavlaunch.minethor;

public interface CompanionStateProvider {
    interface Listener {
        void onSnapshotChanged();
    }

    CompanionSnapshot currentSnapshot();

    CompanionSnapshot selectHotbarSlot(int slot);

    CompanionSnapshot selectInventorySlot(int slot);

    void setListener(Listener listener);

    void close();
}
