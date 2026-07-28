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

    private final String host;
    private final int port;
    private final LinkedBlockingQueue<JSONObject> outgoingMessages = new LinkedBlockingQueue<>();
    private final AtomicInteger commandIds = new AtomicInteger(1);
    private final Object snapshotLock = new Object();
    private volatile boolean closed;
    private volatile CompanionSnapshot snapshot = MockCompanionState.create();
    private volatile Listener listener;
    private int pendingHotbarCommandId;
    private int pendingHotbarSlot;
    private int pendingInventoryCommandId;
    private int pendingInventorySlot;
    private Socket socket;
    private DataOutputStream output;
    private Thread worker;

    public SocketCompanionStateProvider(String host, int port) {
        this.host = host;
        this.port = port;
        start();
    }

    @Override
    public CompanionSnapshot currentSnapshot() {
        return snapshot;
    }

    @Override
    public CompanionSnapshot selectHotbarSlot(int slot) {
        int commandId = sendCommand(slot, true);
        synchronized (snapshotLock) {
            pendingHotbarCommandId = commandId;
            pendingHotbarSlot = slot;
            snapshot = snapshotWithPendingSelections(snapshot);
        }
        notifyChanged();
        return snapshot;
    }

    @Override
    public CompanionSnapshot selectInventorySlot(int slot) {
        int commandId = sendCommand(slot, false);
        synchronized (snapshotLock) {
            pendingInventoryCommandId = commandId;
            pendingInventorySlot = slot;
            snapshot = snapshotWithPendingSelections(snapshot);
        }
        notifyChanged();
        return snapshot;
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
                synchronized (snapshotLock) {
                    snapshot = new CompanionSnapshot(true, snapshot.lastAppliedCommandId, snapshot.player, snapshot.inventory);
                }
                notifyChanged();

                DataInputStream input = new DataInputStream(socket.getInputStream());
                while (!closed && !socket.isClosed()) {
                    writePendingMessages(socketOutput);
                    readSnapshotIfAvailable(input);
                }
            } catch (IOException | JSONException e) {
                if (!closed) Log.d(TAG, "Companion socket unavailable", e);
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

    private int sendCommand(int slot, boolean hotbar) {
        int commandId = commandIds.getAndIncrement();
        try {
            JSONObject message = hotbar
                    ? CompanionProtocolCodec.hotbarCommand(commandId, slot)
                    : CompanionProtocolCodec.inventorySelectCommand(commandId, slot);
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
            synchronized (snapshotLock) {
                CompanionSnapshot authoritativeSnapshot = CompanionProtocolCodec.applyMessage(snapshot, message);
                clearAcknowledgedPendingSelections(authoritativeSnapshot.lastAppliedCommandId);
                snapshot = snapshotWithPendingSelections(authoritativeSnapshot);
            }
            notifyChanged();
        } catch (SocketTimeoutException ignored) {
        }
    }

    private void clearAcknowledgedPendingSelections(int lastAppliedCommandId) {
        if (pendingHotbarCommandId <= lastAppliedCommandId) {
            pendingHotbarCommandId = 0;
        }
        if (pendingInventoryCommandId <= lastAppliedCommandId) {
            pendingInventoryCommandId = 0;
        }
    }

    private CompanionSnapshot snapshotWithPendingSelections(CompanionSnapshot source) {
        InventorySnapshot inventory = source.inventory;
        if (pendingHotbarCommandId > source.lastAppliedCommandId) {
            inventory = inventory.withSelectedHotbarSlot(pendingHotbarSlot);
        }
        if (pendingInventoryCommandId > source.lastAppliedCommandId) {
            inventory = inventory.withSelectedInventorySlot(pendingInventorySlot);
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
