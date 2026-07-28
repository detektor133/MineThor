package org.angelauramc.methodsInjectorAgent.minethor;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

public final class MineThorBridgeServer {
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 25566;
    private static final int PROTOCOL_VERSION = 3;
    private static final int ACCEPT_TIMEOUT_MS = 250;
    private static final int CLIENT_READ_TIMEOUT_MS = 50;
    private static final int SNAPSHOT_INTERVAL_MS = 100;
    private static volatile boolean started;

    private MineThorBridgeServer() {
    }

    public static void start() {
        if (started) return;
        started = true;

        Thread worker = new Thread(MineThorBridgeServer::serverLoop, "MineThorBridgeServer");
        worker.setDaemon(true);
        worker.start();
    }

    private static void serverLoop() {
        MinecraftStateAccess minecraftStateAccess = new MinecraftStateAccess();
        try (ServerSocket serverSocket = new ServerSocket(PORT, 1, InetAddress.getByName(HOST))) {
            serverSocket.setSoTimeout(ACCEPT_TIMEOUT_MS);
            System.out.println("MineThorBridge: listening on " + HOST + ":" + PORT);

            while (true) {
                try {
                    serveClient(serverSocket.accept(), minecraftStateAccess);
                } catch (SocketTimeoutException ignored) {
                }
            }
        } catch (IOException e) {
            System.out.println("MineThorBridge: server unavailable: " + e);
        }
    }

    private static void serveClient(Socket socket, MinecraftStateAccess minecraftStateAccess) {
        try (Socket clientSocket = socket) {
            clientSocket.setSoTimeout(CLIENT_READ_TIMEOUT_MS);
            DataInputStream input = new DataInputStream(clientSocket.getInputStream());
            DataOutputStream output = new DataOutputStream(clientSocket.getOutputStream());
            int lastAppliedCommandId = 0;
            writeMessage(output, helloMessage());

            long lastSnapshotAt = 0L;
            while (!clientSocket.isClosed()) {
                Command command = readCommandIfAvailable(input);
                if (command != null && command.commandId > lastAppliedCommandId) {
                    boolean applied = applyCommand(minecraftStateAccess, command);
                    if (applied) {
                        lastAppliedCommandId = command.commandId;
                        writeMessage(output, snapshotMessage(minecraftStateAccess.readSnapshot(), lastAppliedCommandId));
                        lastSnapshotAt = System.currentTimeMillis();
                    }
                }

                long now = System.currentTimeMillis();
                if (now - lastSnapshotAt >= SNAPSHOT_INTERVAL_MS) {
                    writeMessage(output, snapshotMessage(minecraftStateAccess.readSnapshot(), lastAppliedCommandId));
                    lastSnapshotAt = now;
                }
            }
        } catch (EOFException ignored) {
        } catch (IOException e) {
            System.out.println("MineThorBridge: client disconnected: " + e);
        }
    }

    private static boolean applyCommand(MinecraftStateAccess minecraftStateAccess, Command command) {
        if ("SELECT_HOTBAR_SLOT".equals(command.type)) {
            return minecraftStateAccess.selectHotbarSlot(command.slot);
        }
        return false;
    }

    private static Command readCommandIfAvailable(DataInputStream input) throws IOException {
        try {
            String json = readMessage(input);
            if (JsonProtocol.intValue(json, "protocolVersion", -1) != PROTOCOL_VERSION) return null;
            int commandId = JsonProtocol.intValue(json, "commandId", 0);
            if (commandId <= 0) return null;

            return new Command(
                    JsonProtocol.stringValue(json, "type", ""),
                    commandId,
                    JsonProtocol.intValue(json, "slot", -1)
            );
        } catch (SocketTimeoutException ignored) {
            return null;
        }
    }

    private static String helloMessage() {
        return "{\"type\":\"HELLO\",\"protocolVersion\":" + PROTOCOL_VERSION + "}";
    }

    private static String snapshotMessage(MinecraftSnapshot snapshot, int lastAppliedCommandId) {
        return "{\"type\":\"SNAPSHOT\",\"protocolVersion\":" + PROTOCOL_VERSION
                + ",\"connected\":" + snapshot.connected
                + ",\"lastAppliedCommandId\":" + lastAppliedCommandId
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
                + "},\"inventory\":{"
                + "\"selectedHotbarSlot\":" + snapshot.selectedHotbarSlot
                + ",\"selectedInventorySlot\":" + snapshot.selectedInventorySlot
                + ",\"mainSlots\":" + slotsMessage(snapshot.mainSlots)
                + ",\"armorSlots\":" + slotsMessage(snapshot.armorSlots)
                + ",\"offhandSlots\":" + slotsMessage(snapshot.offhandSlots)
                + "}}";
    }

    private static String slotsMessage(InventorySlotSnapshot[] slots) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < slots.length; i++) {
            if (i > 0) builder.append(',');
            builder.append(slotMessage(slots[i]));
        }
        return builder.append(']').toString();
    }

    private static String slotMessage(InventorySlotSnapshot slot) {
        if (slot == null) slot = InventorySlotSnapshot.empty(0);
        return "{\"index\":" + slot.index
                + ",\"itemId\":\"" + escapeJson(slot.itemId) + "\""
                + ",\"name\":\"" + escapeJson(slot.name) + "\""
                + ",\"count\":" + slot.count
                + ",\"damage\":" + slot.damage
                + ",\"maxDamage\":" + slot.maxDamage
                + "}";
    }

    private static String escapeJson(String value) {
        if (value == null || value.isEmpty()) return "";

        StringBuilder builder = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\':
                    builder.append("\\\\");
                    break;
                case '"':
                    builder.append("\\\"");
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                default:
                    builder.append(c);
                    break;
            }
        }
        return builder.toString();
    }

    private static String readMessage(DataInputStream input) throws IOException {
        int length = input.readInt();
        if (length <= 0 || length > 64 * 1024) {
            throw new IOException("Invalid MineThor message size: " + length);
        }

        byte[] bytes = new byte[length];
        input.readFully(bytes);
        return new String(bytes, "UTF-8");
    }

    private static void writeMessage(DataOutputStream output, String message) throws IOException {
        byte[] bytes = message.getBytes("UTF-8");
        output.writeInt(bytes.length);
        output.write(bytes);
        output.flush();
    }

    private static final class Command {
        final String type;
        final int commandId;
        final int slot;

        Command(String type, int commandId, int slot) {
            this.type = type;
            this.commandId = commandId;
            this.slot = slot;
        }
    }
}
