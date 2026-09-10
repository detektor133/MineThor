package net.kdt.pojavlaunch.minethor;

import org.json.JSONException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class CompanionProtocolCodec {
    public static final int PROTOCOL_VERSION = 3;
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
            return new CompanionSnapshot(true, current.lastAppliedCommandId, current.player, current.inventory);
        }
        if ("PLAYER_STATE".equals(type)) {
            return new CompanionSnapshot(current.connected, commandAckFromJson(current, message), playerFromJson(current.player, message), current.inventory);
        }
        if ("INVENTORY_STATE".equals(type)) {
            return new CompanionSnapshot(current.connected, commandAckFromJson(current, message), current.player, inventoryFromJson(current.inventory, message));
        }
        if ("SNAPSHOT".equals(type)) {
            PlayerSnapshot player = message.has("player") ? playerFromJson(current.player, message.getJSONObject("player")) : current.player;
            InventorySnapshot inventory = message.has("inventory") ? inventoryFromJson(current.inventory, message.getJSONObject("inventory")) : current.inventory;
            return new CompanionSnapshot(message.optBoolean("connected", true), commandAckFromJson(current, message), player, inventory);
        }
        if ("ICON_DATA".equals(type)) {
            MineThorIconCache.logDataStatus(
                    message.optString("iconKey", ""),
                    message.optString("debugStatus", ""),
                    message.optInt("debugBytes", 0)
            );
            MineThorIconCache.put(message.optString("iconKey", ""), message.optString("pngBase64", ""));
            return current;
        }
        return current;
    }

    public static JSONObject hotbarCommand(int commandId, int slot) throws JSONException {
        JSONObject message = command("SELECT_HOTBAR_SLOT", commandId);
        message.put("slot", slot);
        return message;
    }

    public static JSONObject helloMessage() throws JSONException {
        JSONObject message = new JSONObject();
        message.put("type", "HELLO");
        message.put("protocolVersion", PROTOCOL_VERSION);
        return message;
    }

    public static JSONObject iconRequest(String iconKey) throws JSONException {
        JSONObject message = new JSONObject();
        message.put("type", "REQUEST_ICON");
        message.put("protocolVersion", PROTOCOL_VERSION);
        message.put("iconKey", iconKey);
        return message;
    }

    public static JSONObject snapshotMessage(CompanionSnapshot snapshot) throws JSONException {
        JSONObject message = new JSONObject();
        message.put("type", "SNAPSHOT");
        message.put("protocolVersion", PROTOCOL_VERSION);
        message.put("connected", snapshot.connected);
        message.put("lastAppliedCommandId", snapshot.lastAppliedCommandId);
        message.put("player", playerToJson(snapshot.player));
        message.put("inventory", inventoryToJson(snapshot.inventory));
        return message;
    }

    private static JSONObject command(String type, int commandId) throws JSONException {
        JSONObject message = new JSONObject();
        message.put("type", type);
        message.put("commandId", commandId);
        message.put("protocolVersion", PROTOCOL_VERSION);
        return message;
    }

    private static int commandAckFromJson(CompanionSnapshot current, JSONObject message) {
        return Math.max(current.lastAppliedCommandId, message.optInt("lastAppliedCommandId", current.lastAppliedCommandId));
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
                message.optInt("xpLevel", current.xpLevel),
                message.optLong("dayTime", current.dayTime)
        );
    }

    private static InventorySnapshot inventoryFromJson(InventorySnapshot current, JSONObject message) {
        return new InventorySnapshot(
                message.optInt("selectedHotbarSlot", current.selectedHotbarSlot),
                message.optInt("selectedInventorySlot", current.selectedInventorySlot),
                slotsFromJson(message.optJSONArray("mainSlots"), current.mainSlots, InventorySnapshot.MAIN_SLOT_COUNT),
                slotsFromJson(message.optJSONArray("armorSlots"), current.armorSlots, InventorySnapshot.ARMOR_SLOT_COUNT),
                slotsFromJson(message.optJSONArray("offhandSlots"), current.offhandSlots, InventorySnapshot.OFFHAND_SLOT_COUNT)
        );
    }

    private static JSONObject playerToJson(PlayerSnapshot player) throws JSONException {
        JSONObject message = new JSONObject();
        message.put("x", player.x);
        message.put("y", player.y);
        message.put("z", player.z);
        message.put("yaw", player.yaw);
        message.put("health", player.health);
        message.put("maxHealth", player.maxHealth);
        message.put("food", player.food);
        message.put("maxFood", player.maxFood);
        message.put("armor", player.armor);
        message.put("xpLevel", player.xpLevel);
        message.put("dayTime", player.dayTime);
        return message;
    }

    private static JSONObject inventoryToJson(InventorySnapshot inventory) throws JSONException {
        JSONObject message = new JSONObject();
        message.put("selectedHotbarSlot", inventory.selectedHotbarSlot);
        message.put("selectedInventorySlot", inventory.selectedInventorySlot);
        message.put("mainSlots", slotsToJson(inventory.mainSlots));
        message.put("armorSlots", slotsToJson(inventory.armorSlots));
        message.put("offhandSlots", slotsToJson(inventory.offhandSlots));
        return message;
    }

    private static InventorySlotSnapshot[] slotsFromJson(JSONArray array, InventorySlotSnapshot[] current, int size) {
        InventorySlotSnapshot[] slots = new InventorySlotSnapshot[size];
        for (int i = 0; i < size; i++) {
            slots[i] = current != null && i < current.length && current[i] != null
                    ? current[i]
                    : new InventorySlotSnapshot(i, "", "", 0, 0, 0);
        }
        if (array == null) return slots;

        int length = Math.min(array.length(), size);
        for (int i = 0; i < length; i++) {
            JSONObject slot = array.optJSONObject(i);
            if (slot == null) continue;

            slots[i] = new InventorySlotSnapshot(
                    slot.optInt("index", i),
                    slot.optString("itemId", ""),
                    slot.optString("name", ""),
                    slot.optString("iconKey", ""),
                    slot.optInt("count", 0),
                    slot.optInt("damage", 0),
                    slot.optInt("maxDamage", 0)
            );
        }
        return slots;
    }

    private static JSONArray slotsToJson(InventorySlotSnapshot[] slots) throws JSONException {
        JSONArray array = new JSONArray();
        if (slots == null) return array;

        for (InventorySlotSnapshot slot : slots) {
            JSONObject message = new JSONObject();
            if (slot != null) {
                message.put("index", slot.index);
                message.put("itemId", slot.itemId);
                message.put("name", slot.name);
                message.put("iconKey", slot.iconKey);
                message.put("count", slot.count);
                message.put("damage", slot.damage);
                message.put("maxDamage", slot.maxDamage);
            }
            array.put(message);
        }
        return array;
    }
}
