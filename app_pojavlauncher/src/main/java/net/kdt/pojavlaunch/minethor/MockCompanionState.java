package net.kdt.pojavlaunch.minethor;

public final class MockCompanionState {
    private MockCompanionState() {
    }

    public static CompanionSnapshot create() {
        PlayerSnapshot player = new PlayerSnapshot(
                128,
                72,
                -456,
                42,
                20,
                20,
                18,
                20,
                12,
                17
        );
        InventorySnapshot inventory = new InventorySnapshot(0, -1);
        return new CompanionSnapshot(false, player, inventory);
    }
}
