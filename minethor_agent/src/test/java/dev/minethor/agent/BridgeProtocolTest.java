package dev.minethor.agent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.junit.Test;

public class BridgeProtocolTest {
    private static final String TOKEN = "0123456789abcdef0123456789abcdef";

    private static BridgeProtocol.Request hello(int version, String token) {
        return BridgeProtocol.parse("{\"type\":\"HELLO\",\"protocolVersion\":" + version + ",\"token\":\"" + token + "\"}");
    }

    @Test
    public void helloWithRightTokenAndVersionIsAccepted() {
        assertEquals(BridgeProtocol.HelloCheck.OK, BridgeProtocol.checkHello(hello(4, TOKEN), TOKEN));
    }

    @Test
    public void wrongOrMissingTokenIsAuthFailureEvenForWrongVersion() {
        assertEquals(BridgeProtocol.HelloCheck.AUTH_FAILED, BridgeProtocol.checkHello(hello(4, "nope"), TOKEN));
        assertEquals(BridgeProtocol.HelloCheck.AUTH_FAILED, BridgeProtocol.checkHello(hello(99, "nope"), TOKEN));
        assertEquals(BridgeProtocol.HelloCheck.AUTH_FAILED, BridgeProtocol.checkHello(BridgeProtocol.parse("{\"type\":\"HELLO\",\"protocolVersion\":4}"), TOKEN));
    }

    @Test
    public void unknownProtocolVersionIsRejectedAfterAuth() {
        assertEquals(BridgeProtocol.HelloCheck.PROTOCOL_MISMATCH, BridgeProtocol.checkHello(hello(3, TOKEN), TOKEN));
        assertEquals(BridgeProtocol.HelloCheck.PROTOCOL_MISMATCH, BridgeProtocol.checkHello(hello(5, TOKEN), TOKEN));
    }

    @Test
    public void firstMessageMustBeHello() {
        BridgeProtocol.Request notHello = BridgeProtocol.parse("{\"type\":\"SELECT_HOTBAR_SLOT\",\"protocolVersion\":4,\"requestId\":1,\"slot\":1,\"token\":\"" + TOKEN + "\"}");
        assertEquals(BridgeProtocol.HelloCheck.AUTH_FAILED, BridgeProtocol.checkHello(notHello, TOKEN));
    }

    @Test
    public void emptyExpectedTokenNeverMatches() {
        assertFalse(BridgeProtocol.tokenMatches("", ""));
        assertFalse(BridgeProtocol.tokenMatches(null, "x"));
    }

    @Test
    public void ackAndRejectedCarryTheRequestId() {
        assertTrue(BridgeProtocol.ack(7).contains("\"type\":\"ACK\"") && BridgeProtocol.ack(7).contains("\"requestId\":7"));
        String rejected = BridgeProtocol.rejected(9, BridgeProtocol.REASON_STALE_STATE);
        assertTrue(rejected.contains("\"type\":\"REJECTED\"") && rejected.contains("\"requestId\":9") && rejected.contains("STALE_STATE"));
    }

    @Test
    public void helloListsCapabilitiesAndClientThreadFlag() {
        String json = BridgeProtocol.hello(true, new String[]{"player.state", "hotbar.select"}, 2000);
        assertTrue(json.contains("\"clientThread\":true"));
        assertTrue(json.contains("\"capabilities\":[\"player.state\",\"hotbar.select\"]"));
        assertEquals(4, BridgeProtocol.parse(json).protocolVersion);
    }

    @Test
    public void snapshotReportsLastAppliedRequestId() {
        String json = BridgeProtocol.snapshot(MinecraftSnapshot.disconnected(), 12);
        assertEquals(12, JsonProtocol.intValue(json, "lastAppliedRequestId", -1));
        assertTrue(json.contains("\"connected\":false"));
    }

    @Test
    public void escapeHandlesQuotesBackslashesAndControlCharacters() {
        assertEquals("a\\\"b\\\\c\\u0001", BridgeProtocol.escape("a\"b\\c\u0001"));
    }

    @Test
    public void framesRoundTrip() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        BridgeProtocol.writeFrame(new DataOutputStream(bytes), "{\"type\":\"PING\"}");
        String back = BridgeProtocol.readFrame(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
        assertEquals("{\"type\":\"PING\"}", back);
    }

    @Test
    public void oversizedFramesAreRefusedBothWays() throws IOException {
        StringBuilder big = new StringBuilder();
        for (int i = 0; i <= BridgeProtocol.MAX_MESSAGE_BYTES; i++) big.append('x');
        try {
            BridgeProtocol.writeFrame(new DataOutputStream(new ByteArrayOutputStream()), big.toString());
            fail("write must refuse");
        } catch (IOException expected) {
        }

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        new DataOutputStream(bytes).writeInt(BridgeProtocol.MAX_MESSAGE_BYTES + 1);
        try {
            BridgeProtocol.readFrame(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
            fail("read must refuse");
        } catch (IOException expected) {
        }
    }
}
