package net.kdt.pojavlaunch.minethor;

public class ThorCompanionController {
    private final CompanionStateProvider provider;

    public ThorCompanionController(CompanionStateProvider provider) {
        this.provider = provider;
    }

    public CompanionSnapshot currentSnapshot() {
        return provider.currentSnapshot();
    }

    public void selectHotbarSlot(int slot) {
        provider.selectHotbarSlot(slot);
    }

    public void requestIcon(String iconKey) {
        provider.requestIcon(iconKey);
    }

    public void setListener(CompanionStateProvider.Listener listener) {
        provider.setListener(listener);
    }

    public void close() {
        provider.close();
    }
}
