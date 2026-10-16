package dev.minethor.agent;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.MessageDigest;

/**
 * Wire format of the launcher &lt;-&gt; agent bridge (protocol v4). Pure functions only, no sockets and
 * no game access, so every rule here is unit-tested.
 *
 * Frame: 4-byte big-endian length + UTF-8 JSON. Every request that changes the game carries a
 * positive {@code requestId} and gets exactly one {@code ACK} or {@code REJECTED} reply.
 */
final class BridgeProtocol {
    static final int VERSION = 4;
    static final int MAX_MESSAGE_BYTES = 64 * 1024;

    static final String REASON_STALE_STATE = "STALE_STATE";
    static final String REASON_UNSUPPORTED = "UNSUPPORTED";
    static final String REASON_POLICY = "POLICY";
    static final String REASON_BAD_REQUEST = "BAD_REQUEST";
    static final String REASON_TIMEOUT = "TIMEOUT";
    static final String REASON_PROTOCOL = "PROTOCOL";

    enum HelloCheck { OK, AUTH_FAILED, PROTOCOL_MISMATCH }

    static final class Request {
        final String type;
        final int protocolVersion;
        final int requestId;
        final int slot;
        final String iconKey;
        final String token;

        Request(String type, int protocolVersion, int requestId, int slot, String iconKey, String token) {
            this.type = type;
            this.protocolVersion = protocolVersion;
            this.requestId = requestId;
            this.slot = slot;
            this.iconKey = iconKey;
            this.token = token;
        }
    }

    private BridgeProtocol() {
    }

    static Request parse(String json) {
        return new Request(
                JsonProtocol.stringValue(json, "type", ""),
                JsonProtocol.intValue(json, "protocolVersion", -1),
                JsonProtocol.intValue(json, "requestId", 0),
                JsonProtocol.intValue(json, "slot", -1),
                JsonProtocol.stringValue(json, "iconKey", ""),
                JsonProtocol.stringValue(json, "token", ""));
    }

    /** Token is checked first, so a peer without the token learns nothing about versions. */
    static HelloCheck checkHello(Request hello, String expectedToken) {
        if (!"HELLO".equals(hello.type) || !tokenMatches(expectedToken, hello.token)) return HelloCheck.AUTH_FAILED;
        if (hello.protocolVersion != VERSION) return HelloCheck.PROTOCOL_MISMATCH;
        return HelloCheck.OK;
    }

    static boolean tokenMatches(String expected, String provided) {
        if (expected == null || expected.isEmpty() || provided == null) return false;
        try {
            return MessageDigest.isEqual(expected.getBytes("UTF-8"), provided.getBytes("UTF-8"));
        } catch (java.io.UnsupportedEncodingException e) {
            return false;
        }
    }

    static String hello(boolean clientThread, String[] capabilities, int heartbeatMs) {
        StringBuilder builder = new StringBuilder("{\"type\":\"HELLO\",\"protocolVersion\":" + VERSION
                + ",\"clientThread\":" + clientThread + ",\"heartbeatMs\":" + heartbeatMs + ",\"capabilities\":[");
        for (int i = 0; i < capabilities.length; i++) {
            if (i > 0) builder.append(',');
            builder.append('"').append(escape(capabilities[i])).append('"');
        }
        return builder.append("]}").toString();
    }

    static String ack(int requestId) {
        return "{\"type\":\"ACK\",\"protocolVersion\":" + VERSION + ",\"requestId\":" + requestId + "}";
    }

    static String rejected(int requestId, String reason) {
        return "{\"type\":\"REJECTED\",\"protocolVersion\":" + VERSION + ",\"requestId\":" + requestId
                + ",\"reason\":\"" + reason + "\"}";
    }

    static String ping() {
        return "{\"type\":\"PING\",\"protocolVersion\":" + VERSION + "}";
    }

    static String pong() {
        return "{\"type\":\"PONG\",\"protocolVersion\":" + VERSION + "}";
    }

    static String snapshot(MinecraftSnapshot snapshot, int lastAppliedRequestId) {
        return "{\"type\":\"SNAPSHOT\",\"protocolVersion\":" + VERSION
                + ",\"connected\":" + snapshot.connected
                + ",\"lastAppliedRequestId\":" + lastAppliedRequestId
                + ",\"player\":{"
                + "\"x\":" + snapshot.x
                + ",\"y\":" + snapshot.y
                + ",\"z\":" + snapshot.z
                + ",\"yaw\":" + snapshot.yaw
                + ",\"health\":" + snapshot.health
                + ",\"maxHealth\":" + snapshot.maxHealth
                + ",\"food\":" + snapshot.food
                + ",\"maxFood\":" + snapshot.maxFood
                + ",\"armor\":" + snapshot.armor
                + ",\"xpLevel\":" + snapshot.xpLevel
                + ",\"dayTime\":" + snapshot.dayTime
                + "},\"inventory\":{"
                + "\"selectedHotbarSlot\":" + snapshot.selectedHotbarSlot
                + ",\"selectedInventorySlot\":" + snapshot.selectedInventorySlot
                + ",\"mainSlots\":" + slots(snapshot.mainSlots)
                + ",\"armorSlots\":" + slots(snapshot.armorSlots)
                + ",\"offhandSlots\":" + slots(snapshot.offhandSlots)
                + "}}";
    }

    static String icon(String iconKey, String status, int byteCount, String pngBase64) {
        return "{\"type\":\"ICON_DATA\",\"protocolVersion\":" + VERSION
                + ",\"iconKey\":\"" + escape(iconKey) + "\""
                + ",\"debugStatus\":\"" + escape(status) + "\""
                + ",\"debugBytes\":" + byteCount
                + ",\"pngBase64\":\"" + escape(pngBase64) + "\"}";
    }

    static boolean fits(String message) {
        // Characters are mostly ASCII; the exact check happens on the encoded bytes in writeFrame.
        return message.length() <= MAX_MESSAGE_BYTES / 3;
    }

    private static String slots(InventorySlotSnapshot[] slots) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < slots.length; i++) {
            if (i > 0) builder.append(',');
            builder.append(slot(slots[i]));
        }
        return builder.append(']').toString();
    }

    private static String slot(InventorySlotSnapshot slot) {
        if (slot == null) slot = InventorySlotSnapshot.empty(0);
        return "{\"index\":" + slot.index
                + ",\"itemId\":\"" + escape(slot.itemId) + "\""
                + ",\"name\":\"" + escape(slot.name) + "\""
                + ",\"iconKey\":\"" + escape(slot.iconKey) + "\""
                + ",\"count\":" + slot.count
                + ",\"damage\":" + slot.damage
                + ",\"maxDamage\":" + slot.maxDamage
                + "}";
    }

    static String escape(String value) {
        if (value == null || value.isEmpty()) return "";

        StringBuilder builder = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\': builder.append("\\\\"); break;
                case '"': builder.append("\\\""); break;
                case '\n': builder.append("\\n"); break;
                case '\r': builder.append("\\r"); break;
                case '\t': builder.append("\\t"); break;
                default:
                    if (c < 0x20) builder.append(String.format("\\u%04x", (int) c));
                    else builder.append(c);
            }
        }
        return builder.toString();
    }

    static String readFrame(DataInputStream input) throws IOException {
        int length = input.readInt();
        if (length <= 0 || length > MAX_MESSAGE_BYTES) {
            throw new IOException("Invalid MineThor message size: " + length);
        }
        byte[] bytes = new byte[length];
        input.readFully(bytes);
        return new String(bytes, "UTF-8");
    }

    static void writeFrame(DataOutputStream output, String message) throws IOException {
        byte[] bytes = message.getBytes("UTF-8");
        if (bytes.length > MAX_MESSAGE_BYTES) {
            throw new IOException("MineThor message is too large: " + bytes.length);
        }
        output.writeInt(bytes.length);
        output.write(bytes);
        output.flush();
    }
}
