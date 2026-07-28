package net.kdt.pojavlaunch.minethor;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class CompanionProtocolCodec {
    public static final int PROTOCOL_VERSION = 1;
    private static final int MAX_MESSAGE_BYTES = 64 * 1024;

    private CompanionProtocolCodec() {
    }

    public static JSONObject readMessage(DataInputStream input) throws IOException, JSONException {
        int length = input.readInt();
        if (length <= 0 || length > MAX_MESSAGE_BYTES) {
            throw new IOException("Invalid MineThor message size: " + length);
        }

        byte[] bytes = new byte[length];
        input.readFully(bytes);
        return new JSONObject(new String(bytes, StandardCharsets.UTF_8));
    }

    public static void writeMessage(DataOutputStream output, JSONObject message) throws IOException {
        byte[] bytes = message.toString().getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_MESSAGE_BYTES) {
            throw new IOException("MineThor message is too large: " + bytes.length);
        }

        output.writeInt(bytes.length);
        output.write(bytes);
        output.flush();
    }

    public static CompanionSnapshot applyMessage(CompanionSnapshot current, JSONObject message) throws JSONException {
        String type = message.optString("type", "");
        if ("HELLO".equals(type)) {
            int protocolVersion = message.optInt("protocolVersion", -1);
            if (protocolVersion != PROTOCOL_VERSION) return current;
            return new CompanionSnapshot(true, current.player, current.inventory);
        }
        if ("PLAYER_STATE".equals(type)) {
            return new CompanionSnapshot(current.connected, playerFromJson(current.player, message), current.inventory);
        }
        if ("INVENTORY_STATE".equals(type)) {
            return new CompanionSnapshot(current.connected, current.player, inventoryFromJson(current.inventory, message));
        }
        if ("SNAPSHOT".equals(type)) {
            PlayerSnapshot player = message.has("player") ? playerFromJson(current.player, message.getJSONObject("player")) : current.player;
            InventorySnapshot inventory = message.has("inventory") ? inventoryFromJson(current.inventory, message.getJSONObject("inventory")) : current.inventory;
            return new CompanionSnapshot(true, player, inventory);
        }
        return current;
    }

    public static JSONObject hotbarCommand(int requestId, int slot) throws JSONException {
        JSONObject message = command("SELECT_HOTBAR_SLOT", requestId);
        message.put("slot", slot);
        return message;
    }

    public static JSONObject inventorySelectCommand(int requestId, int slot) throws JSONException {
        JSONObject message = command("SELECT_INVENTORY_SLOT", requestId);
        message.put("slot", slot);
        return message;
    }

    private static JSONObject command(String type, int requestId) throws JSONException {
        JSONObject message = new JSONObject();
        message.put("type", type);
        message.put("requestId", requestId);
        message.put("protocolVersion", PROTOCOL_VERSION);
        return message;
    }

    private static PlayerSnapshot playerFromJson(PlayerSnapshot current, JSONObject message) {
        return new PlayerSnapshot(
                message.optInt("x", current.x),
                message.optInt("y", current.y),
                message.optInt("z", current.z),
                message.optInt("yaw", current.yaw),
                message.optInt("health", current.health),
                message.optInt("maxHealth", current.maxHealth),
                message.optInt("food", current.food),
                message.optInt("maxFood", current.maxFood),
                message.optInt("armor", current.armor),
                message.optInt("xpLevel", current.xpLevel)
        );
    }

    private static InventorySnapshot inventoryFromJson(InventorySnapshot current, JSONObject message) {
        return new InventorySnapshot(
                message.optInt("selectedHotbarSlot", current.selectedHotbarSlot),
                message.optInt("selectedInventorySlot", current.selectedInventorySlot)
        );
    }
}
