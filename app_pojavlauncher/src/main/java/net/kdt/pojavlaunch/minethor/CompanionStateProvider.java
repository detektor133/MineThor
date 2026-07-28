package net.kdt.pojavlaunch.minethor;

public interface CompanionStateProvider {
    interface Listener {
        void onSnapshotChanged();
    }

    CompanionSnapshot currentSnapshot();

    CompanionSnapshot selectHotbarSlot(int slot);

    void requestIcon(String iconKey);

    void setListener(Listener listener);

    void close();
}
