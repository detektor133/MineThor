package net.kdt.pojavlaunch.minethor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.json.JSONObject;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class CompanionProtocolCodecTest {
    private static CompanionSnapshot initial() {
        return InitialCompanionState.create();
    }

    @Test
    public void protocolVersionIsFour() {
        assertEquals(4, CompanionProtocolCodec.PROTOCOL_VERSION);
    }

    @Test
    public void helloCarriesVersionAndToken() throws Exception {
        JSONObject hello = CompanionProtocolCodec.helloMessage("abc123");
        assertEquals("HELLO", hello.getString("type"));
        assertEquals(4, hello.getInt("protocolVersion"));
        assertEquals("abc123", hello.getString("token"));
    }

    @Test
    public void commandsCarryRequestIdNotCommandId() throws Exception {
        JSONObject command = CompanionProtocolCodec.hotbarCommand(11, 4);
        assertEquals(11, command.getInt("requestId"));
        assertFalse(command.has("commandId"));
        assertEquals(4, command.getInt("slot"));
    }

    @Test
    public void helloWithUnknownProtocolVersionIsIgnored() throws Exception {
        CompanionSnapshot before = initial();
        JSONObject oldHello = new JSONObject().put("type", "HELLO").put("protocolVersion", 3);
        JSONObject futureHello = new JSONObject().put("type", "HELLO").put("protocolVersion", 5);
        assertSame(before, CompanionProtocolCodec.applyMessage(before, oldHello));
        assertSame(before, CompanionProtocolCodec.applyMessage(before, futureHello));
    }

    @Test
    public void helloWithCurrentVersionConnects() throws Exception {
        JSONObject hello = new JSONObject().put("type", "HELLO").put("protocolVersion", 4);
        assertTrue(CompanionProtocolCodec.applyMessage(initial(), hello).connected);
    }

    @Test
    public void snapshotAckIsReadFromLastAppliedRequestIdAndNeverGoesBackwards() throws Exception {
        CompanionSnapshot first = CompanionProtocolCodec.applyMessage(initial(),
                new JSONObject().put("type", "SNAPSHOT").put("lastAppliedRequestId", 7));
        assertEquals(7, first.lastAppliedCommandId);
        CompanionSnapshot second = CompanionProtocolCodec.applyMessage(first,
                new JSONObject().put("type", "SNAPSHOT").put("lastAppliedRequestId", 3));
        assertEquals("a stale snapshot must not lower the acknowledged id", 7, second.lastAppliedCommandId);
    }

    @Test
    public void rejectionMatchesOnlyItsOwnRequest() throws Exception {
        JSONObject rejected = new JSONObject().put("type", "REJECTED").put("requestId", 9).put("reason", "STALE_STATE");
        assertTrue(CompanionProtocolCodec.isRejectionOf(rejected, 9));
        assertFalse(CompanionProtocolCodec.isRejectionOf(rejected, 8));
        assertFalse(CompanionProtocolCodec.isRejectionOf(rejected, 0));
        assertFalse(CompanionProtocolCodec.isRejectionOf(new JSONObject().put("type", "ACK").put("requestId", 9), 9));
    }

    @Test
    public void framesRoundTripAndOversizedFramesAreRefused() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        CompanionProtocolCodec.writeMessage(new DataOutputStream(bytes), CompanionProtocolCodec.pongMessage());
        JSONObject back = CompanionProtocolCodec.readMessage(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
        assertEquals("PONG", back.getString("type"));

        ByteArrayOutputStream huge = new ByteArrayOutputStream();
        new DataOutputStream(huge).writeInt(64 * 1024 + 1);
        try {
            CompanionProtocolCodec.readMessage(new DataInputStream(new ByteArrayInputStream(huge.toByteArray())));
            fail("must refuse");
        } catch (IOException expected) {
        }
    }

    @Test
    public void sessionTokensAreRandomHexAndSessionsHavePorts() {
        String a = CompanionEndpoint.randomToken();
        String b = CompanionEndpoint.randomToken();
        assertEquals(32, a.length());
        assertTrue(a.matches("[0-9a-f]+"));
        assertNotEquals(a, b);

        assertTrue(CompanionEndpoint.newSession());
        assertTrue(CompanionEndpoint.hasSession());
        assertTrue(CompanionEndpoint.port() > 0);
        assertEquals(32, CompanionEndpoint.token().length());
        assertEquals(CompanionEndpoint.Status.WAITING_FOR_GAME, CompanionEndpoint.status());
    }
}
