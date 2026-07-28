package net.kdt.pojavlaunch.minethor;

public class MockCompanionStateProvider implements CompanionStateProvider {
    private CompanionSnapshot snapshot = MockCompanionState.create();
    private int commandIds;
    private Listener listener;

    @Override
    public CompanionSnapshot currentSnapshot() {
        return snapshot;
    }

    @Override
    public CompanionSnapshot selectHotbarSlot(int slot) {
        int commandId = ++commandIds;
        snapshot = new CompanionSnapshot(
                snapshot.connected,
                commandId,
                snapshot.player,
                snapshot.inventory.withSelectedHotbarSlot(slot)
        );
        notifyChanged();
        return snapshot;
    }

    @Override
    public CompanionSnapshot selectInventorySlot(int slot) {
        int commandId = ++commandIds;
        snapshot = new CompanionSnapshot(
                snapshot.connected,
                commandId,
                snapshot.player,
                snapshot.inventory.withSelectedInventorySlot(slot)
        );
        notifyChanged();
        return snapshot;
    }

    @Override
    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public void close() {
    }

    private void notifyChanged() {
        if (listener != null) listener.onSnapshotChanged();
    }
}
