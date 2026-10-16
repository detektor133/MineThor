package net.kdt.pojavlaunch.minethor;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class SocketCompanionStateProvider implements CompanionStateProvider {
    private static final String TAG = "MineThorSocket";
    private static final int CONNECT_TIMEOUT_MS = 500;
    private static final int RECONNECT_DELAY_MS = 1000;
    /** The agent pings every 2 s; silence for longer than this means the game is gone or frozen. */
    private static final int HEARTBEAT_TIMEOUT_MS = 6000;

    private final String host;
    private final LinkedBlockingQueue<JSONObject> outgoingMessages = new LinkedBlockingQueue<>();
    private final AtomicInteger commandIds = new AtomicInteger(1);
    private final Object snapshotLock = new Object();
    private volatile boolean closed;
    private volatile CompanionSnapshot snapshot = InitialCompanionState.create();
    private volatile Listener listener;
    private int pendingHotbarCommandId;
    private int pendingHotbarSlot;
    private Socket socket;
    private DataOutputStream output;
    private Thread worker;
    private volatile long lastReceivedAt;

    public SocketCompanionStateProvider() {
        this.host = CompanionEndpoint.LOOPBACK_HOST;
        start();
    }

    @Override
    public CompanionSnapshot currentSnapshot() {
        return snapshot;
    }

    @Override
    public CompanionSnapshot selectHotbarSlot(int slot) {
        int commandId = sendCommand(slot);
        synchronized (snapshotLock) {
            pendingHotbarCommandId = commandId;
            pendingHotbarSlot = slot;
            snapshot = snapshotWithPendingSelections(snapshot);
        }
        notifyChanged();
        return snapshot;
    }

    @Override
    public void requestIcon(String iconKey) {
        try {
            outgoingMessages.offer(CompanionProtocolCodec.iconRequest(iconKey));
        } catch (JSONException e) {
            Log.d(TAG, "Cannot build icon request", e);
        }
    }

    @Override
    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public void close() {
        closed = true;
        closeSocket();
        closeOutput();
        Thread currentWorker = worker;
        if (currentWorker != null) currentWorker.interrupt();
    }

    private void start() {
        worker = new Thread(this::connectionLoop, "MineThorSocketProvider");
        worker.setDaemon(true);
        worker.start();
    }

    private void connectionLoop() {
        while (!closed) {
            if (!CompanionEndpoint.hasSession()) {
                CompanionEndpoint.setStatus(CompanionEndpoint.Status.WAITING_FOR_GAME);
                sleepBeforeReconnect();
                continue;
            }
            int port = CompanionEndpoint.port();
            try (Socket socket = new Socket()) {
                this.socket = socket;
                InetAddress address = InetAddress.getByName(host);
                if (!address.isLoopbackAddress()) {
                    throw new IOException("MineThor companion must use loopback only");
                }

                socket.connect(new InetSocketAddress(address, port), CONNECT_TIMEOUT_MS);
                socket.setSoTimeout(CONNECT_TIMEOUT_MS);
                DataOutputStream socketOutput = new DataOutputStream(socket.getOutputStream());
                output = socketOutput;
                outgoingMessages.clear();
                lastReceivedAt = System.currentTimeMillis();
                CompanionEndpoint.setStatus(CompanionEndpoint.Status.CONNECTING);
                // The handshake carries the one-time token; the agent answers HELLO or closes.
                CompanionProtocolCodec.writeMessage(socketOutput, CompanionProtocolCodec.helloMessage(CompanionEndpoint.token()));

                DataInputStream input = new DataInputStream(socket.getInputStream());
                while (!closed && !socket.isClosed()) {
                    writePendingMessages(socketOutput);
                    readSnapshotIfAvailable(input);
                    if (System.currentTimeMillis() - lastReceivedAt > HEARTBEAT_TIMEOUT_MS) {
                        throw new IOException("Companion heartbeat timeout");
                    }
                }
            } catch (IOException | JSONException e) {
                if (!closed) Log.d(TAG, "Companion socket unavailable", e);
                // Closed during the handshake (before any HELLO): wrong token for this session.
                if (CompanionEndpoint.status() == CompanionEndpoint.Status.CONNECTING) {
                    CompanionEndpoint.setStatus(CompanionEndpoint.Status.AUTH_REJECTED);
                } else if (CompanionEndpoint.status() == CompanionEndpoint.Status.CONNECTED) {
                    CompanionEndpoint.setStatus(CompanionEndpoint.Status.CONNECTING);
                }
            } finally {
                socket = null;
                closeOutput();
                if (!closed && snapshot.connected) {
                    synchronized (snapshotLock) {
                        snapshot = new CompanionSnapshot(false, snapshot.lastAppliedCommandId, snapshot.player, snapshot.inventory);
                    }
                    notifyChanged();
                }
            }
            sleepBeforeReconnect();
        }
    }

    private int sendCommand(int slot) {
        int commandId = commandIds.getAndIncrement();
        try {
            JSONObject message = CompanionProtocolCodec.hotbarCommand(commandId, slot);
            outgoingMessages.offer(message);
        } catch (JSONException e) {
            Log.d(TAG, "Cannot build companion command", e);
        }
        return commandId;
    }

    private void writePendingMessages(DataOutputStream socketOutput) throws IOException {
        JSONObject message;
        while ((message = outgoingMessages.poll()) != null) {
            CompanionProtocolCodec.writeMessage(socketOutput, message);
        }
    }

    private void readSnapshotIfAvailable(DataInputStream input) throws IOException, JSONException {
        try {
            JSONObject message = CompanionProtocolCodec.readMessage(input);
            lastReceivedAt = System.currentTimeMillis();
            if (handleControlMessage(message)) return;
            synchronized (snapshotLock) {
                CompanionSnapshot authoritativeSnapshot = CompanionProtocolCodec.applyMessage(snapshot, message);
                clearAcknowledgedPendingSelections(authoritativeSnapshot.lastAppliedCommandId);
                snapshot = snapshotWithPendingSelections(authoritativeSnapshot);
            }
            notifyChanged();
        } catch (SocketTimeoutException ignored) {
        }
    }

    /** Handles heartbeat, handshake and rejection messages. Returns true if the message is fully consumed. */
    private boolean handleControlMessage(JSONObject message) throws JSONException {
        String type = message.optString("type", "");
        if ("PING".equals(type)) {
            outgoingMessages.offer(CompanionProtocolCodec.pongMessage());
            return true;
        }
        if ("HELLO".equals(type)) {
            boolean compatible = message.optInt("protocolVersion", -1) == CompanionProtocolCodec.PROTOCOL_VERSION;
            CompanionEndpoint.setStatus(compatible ? CompanionEndpoint.Status.CONNECTED : CompanionEndpoint.Status.PROTOCOL_MISMATCH);
            if (!compatible) return true;
            synchronized (snapshotLock) {
                snapshot = new CompanionSnapshot(true, snapshot.lastAppliedCommandId, snapshot.player, snapshot.inventory);
            }
            notifyChanged();
            return true;
        }
        if ("REJECTED".equals(type)) {
            if ("PROTOCOL".equals(message.optString("reason", ""))) {
                CompanionEndpoint.setStatus(CompanionEndpoint.Status.PROTOCOL_MISMATCH);
            }
            boolean rolledBack = false;
            synchronized (snapshotLock) {
                if (CompanionProtocolCodec.isRejectionOf(message, pendingHotbarCommandId)) {
                    // Roll back the optimistic selection: the game state stays authoritative.
                    pendingHotbarCommandId = 0;
                    snapshot = snapshotWithPendingSelections(snapshot);
                    rolledBack = true;
                }
            }
            if (rolledBack) notifyChanged();
            return true;
        }
        return false;
    }

    private void clearAcknowledgedPendingSelections(int lastAppliedCommandId) {
        if (pendingHotbarCommandId <= lastAppliedCommandId) {
            pendingHotbarCommandId = 0;
        }
    }

    private CompanionSnapshot snapshotWithPendingSelections(CompanionSnapshot source) {
        InventorySnapshot inventory = source.inventory;
        if (pendingHotbarCommandId > source.lastAppliedCommandId) {
            inventory = inventory.withSelectedHotbarSlot(pendingHotbarSlot);
        }
        if (inventory == source.inventory) return source;

        return new CompanionSnapshot(
                source.connected,
                source.lastAppliedCommandId,
                source.player,
                inventory
        );
    }

    private void closeOutput() {
        output = null;
    }

    private void closeSocket() {
        Socket currentSocket = socket;
        if (currentSocket == null) return;

        try {
            currentSocket.close();
        } catch (IOException e) {
            Log.d(TAG, "Cannot close companion socket", e);
        }
    }

    private void notifyChanged() {
        Listener currentListener = listener;
        if (currentListener != null) currentListener.onSnapshotChanged();
    }

    private void sleepBeforeReconnect() {
        try {
            Thread.sleep(RECONNECT_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
