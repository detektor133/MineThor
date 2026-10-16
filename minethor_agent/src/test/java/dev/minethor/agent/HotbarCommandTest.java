package dev.minethor.agent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.Executor;

import org.junit.Test;

public class HotbarCommandTest {
    private static BridgeProtocol.Request select(int requestId, int slot) {
        return BridgeProtocol.parse("{\"type\":\"SELECT_HOTBAR_SLOT\",\"protocolVersion\":4,\"requestId\":" + requestId + ",\"slot\":" + slot + "}");
    }

    private static final ClientThread DIRECT = new ClientThread(() -> (Executor) Runnable::run);

    @Test
    public void appliedCommandIsAcked() {
        int[] selected = {-1};
        String reply = MineThorBridgeServer.selectHotbarSlot(select(5, 3), 0, DIRECT, slot -> {
            selected[0] = slot;
            return true;
        });
        assertEquals(BridgeProtocol.ack(5), reply);
        assertEquals(3, selected[0]);
    }

    @Test
    public void staleOrReplayedRequestIdIsRejectedAndNotApplied() {
        boolean[] touched = {false};
        String reply = MineThorBridgeServer.selectHotbarSlot(select(5, 3), 5, DIRECT, slot -> touched[0] = true);
        assertEquals(BridgeProtocol.rejected(5, BridgeProtocol.REASON_STALE_STATE), reply);
        assertFalse(touched[0]);
        assertEquals(BridgeProtocol.rejected(4, BridgeProtocol.REASON_STALE_STATE),
                MineThorBridgeServer.selectHotbarSlot(select(4, 3), 5, DIRECT, slot -> true));
    }

    @Test
    public void badRequestsAreRejected() {
        assertTrue(MineThorBridgeServer.selectHotbarSlot(select(0, 3), 0, DIRECT, s -> true).contains("BAD_REQUEST"));
        assertTrue(MineThorBridgeServer.selectHotbarSlot(select(1, 9), 0, DIRECT, s -> true).contains("BAD_REQUEST"));
        assertTrue(MineThorBridgeServer.selectHotbarSlot(select(1, -1), 0, DIRECT, s -> true).contains("BAD_REQUEST"));
    }

    @Test
    public void withoutClientThreadCommandsAreUnsupportedAndNeverTouchTheGame() {
        boolean[] touched = {false};
        ClientThread none = new ClientThread(() -> null);
        String reply = MineThorBridgeServer.selectHotbarSlot(select(1, 2), 0, none, slot -> touched[0] = true);
        assertEquals(BridgeProtocol.rejected(1, BridgeProtocol.REASON_UNSUPPORTED), reply);
        assertFalse(touched[0]);
    }

    @Test
    public void gameRefusingTheActionIsUnsupported() {
        assertTrue(MineThorBridgeServer.selectHotbarSlot(select(1, 2), 0, DIRECT, s -> false).contains("UNSUPPORTED"));
    }
}
