package net.kdt.pojavlaunch.minethor;

public class MockCompanionStateProvider implements CompanionStateProvider {
    private CompanionSnapshot snapshot = MockCompanionState.create();

    @Override
    public CompanionSnapshot currentSnapshot() {
        return snapshot;
    }

    @Override
    public CompanionSnapshot selectHotbarSlot(int slot) {
        snapshot = new CompanionSnapshot(
                snapshot.connected,
                snapshot.player,
                snapshot.inventory.withSelectedHotbarSlot(slot)
        );
        return snapshot;
    }

    @Override
    public CompanionSnapshot selectInventorySlot(int slot) {
        snapshot = new CompanionSnapshot(
                snapshot.connected,
                snapshot.player,
                snapshot.inventory.withSelectedInventorySlot(slot)
        );
        return snapshot;
    }

    @Override
    public void close() {
    }
}
