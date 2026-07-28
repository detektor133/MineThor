package net.kdt.pojavlaunch.minethor;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class DevLoopbackCompanionServer {
    private static final String TAG = "MineThorDevServer";
    private static final int ACCEPT_TIMEOUT_MS = 250;
    private static final int CLIENT_READ_TIMEOUT_MS = 50;
    private static final int SNAPSHOT_INTERVAL_MS = 250;

    private volatile boolean closed;
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private Thread worker;
    private CompanionSnapshot snapshot = MockCompanionState.create();

    public void start() {
        if (worker != null) return;

        worker = new Thread(this::serverLoop, "MineThorDevLoopbackServer");
        worker.setDaemon(true);
        worker.start();
    }

    public void close() {
        closed = true;
        closeClient();
        closeServer();
        Thread currentWorker = worker;
        if (currentWorker != null) currentWorker.interrupt();
    }

    private void serverLoop() {
        try (ServerSocket server = new ServerSocket(
                CompanionEndpoint.PORT,
                1,
                InetAddress.getByName(CompanionEndpoint.LOOPBACK_HOST)
        )) {
            serverSocket = server;
            server.setSoTimeout(ACCEPT_TIMEOUT_MS);
            Log.i(TAG, "Dev companion server listening on " + CompanionEndpoint.LOOPBACK_HOST + ":" + CompanionEndpoint.PORT);

            while (!closed) {
                try {
                    serveClient(server.accept());
                } catch (SocketTimeoutException ignored) {
                }
            }
        } catch (IOException e) {
            if (!closed) Log.d(TAG, "Dev companion server unavailable", e);
        } finally {
            closeClient();
            serverSocket = null;
        }
    }

    private void serveClient(Socket socket) {
        try (Socket acceptedSocket = socket) {
            clientSocket = acceptedSocket;
            acceptedSocket.setSoTimeout(CLIENT_READ_TIMEOUT_MS);
            DataInputStream input = new DataInputStream(acceptedSocket.getInputStream());
            DataOutputStream output = new DataOutputStream(acceptedSocket.getOutputStream());
            CompanionProtocolCodec.writeMessage(output, CompanionProtocolCodec.helloMessage());

            long lastSnapshotAt = 0L;
            while (!closed && !acceptedSocket.isClosed()) {
                long now = System.currentTimeMillis();
                boolean commandApplied = readCommandIfAvailable(input);
                if (commandApplied) {
                    CompanionProtocolCodec.writeMessage(output, CompanionProtocolCodec.snapshotMessage(snapshot));
                    lastSnapshotAt = now;
                }
                if (now - lastSnapshotAt >= SNAPSHOT_INTERVAL_MS) {
                    snapshot = advanceSnapshot(snapshot);
                    CompanionProtocolCodec.writeMessage(output, CompanionProtocolCodec.snapshotMessage(snapshot));
                    lastSnapshotAt = now;
                }
            }
        } catch (IOException | JSONException e) {
            if (!closed) Log.d(TAG, "Dev companion client disconnected", e);
        } finally {
            clientSocket = null;
        }
    }

    private boolean readCommandIfAvailable(DataInputStream input) throws IOException, JSONException {
        try {
            JSONObject command = CompanionProtocolCodec.readMessage(input);
            if (command.optInt("protocolVersion", -1) != CompanionProtocolCodec.PROTOCOL_VERSION) return false;
            int commandId = command.optInt("commandId", 0);
            if (commandId <= snapshot.lastAppliedCommandId) return false;

            String type = command.optString("type", "");
            if ("SELECT_HOTBAR_SLOT".equals(type)) {
                snapshot = new CompanionSnapshot(
                        snapshot.connected,
                        commandId,
                        snapshot.player,
                        snapshot.inventory.withSelectedHotbarSlot(command.optInt("slot", snapshot.inventory.selectedHotbarSlot))
                );
                return true;
            } else if ("SELECT_INVENTORY_SLOT".equals(type)) {
                snapshot = new CompanionSnapshot(
                        snapshot.connected,
                        commandId,
                        snapshot.player,
                        snapshot.inventory.withSelectedInventorySlot(command.optInt("slot", snapshot.inventory.selectedInventorySlot))
                );
                return true;
            }
        } catch (SocketTimeoutException ignored) {
        }
        return false;
    }

    private CompanionSnapshot advanceSnapshot(CompanionSnapshot current) {
        PlayerSnapshot player = current.player;
        PlayerSnapshot nextPlayer = new PlayerSnapshot(
                player.x + 1,
                player.y,
                player.z - 1,
                (player.yaw + 3) % 360,
                player.health,
                player.maxHealth,
                player.food,
                player.maxFood,
                player.armor,
                player.xpLevel
        );
        return new CompanionSnapshot(true, current.lastAppliedCommandId, nextPlayer, current.inventory);
    }

    private void closeClient() {
        Socket socket = clientSocket;
        if (socket == null) return;

        try {
            socket.close();
        } catch (IOException e) {
            Log.d(TAG, "Cannot close dev companion client", e);
        }
    }

    private void closeServer() {
        ServerSocket server = serverSocket;
        if (server == null) return;

        try {
            server.close();
        } catch (IOException e) {
            Log.d(TAG, "Cannot close dev companion server", e);
        }
    }
}
