package net.kdt.pojavlaunch.minethor;

public final class InitialCompanionState {
    private InitialCompanionState() {
    }

    public static CompanionSnapshot create() {
        PlayerSnapshot player = new PlayerSnapshot(
                0,
                0,
                0,
                0,
                0,
                20,
                0,
                20,
                0,
                0
        );
        InventorySnapshot inventory = new InventorySnapshot(-1, -1);
        return new CompanionSnapshot(false, 0, player, inventory);
    }
}
