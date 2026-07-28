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
import java.util.concurrent.atomic.AtomicInteger;

public class SocketCompanionStateProvider implements CompanionStateProvider {
    private static final String TAG = "MineThorSocket";
    private static final int CONNECT_TIMEOUT_MS = 500;
    private static final int RECONNECT_DELAY_MS = 1000;

    private final String host;
    private final int port;
    private final AtomicInteger requestIds = new AtomicInteger(1);
    private volatile boolean closed;
    private volatile CompanionSnapshot snapshot = MockCompanionState.create();
    private volatile Listener listener;
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
        sendCommand(slot, true);
        snapshot = new CompanionSnapshot(
                snapshot.connected,
                snapshot.player,
                snapshot.inventory.withSelectedHotbarSlot(slot)
        );
        notifyChanged();
        return snapshot;
    }

    @Override
    public CompanionSnapshot selectInventorySlot(int slot) {
        sendCommand(slot, false);
        snapshot = new CompanionSnapshot(
                snapshot.connected,
                snapshot.player,
                snapshot.inventory.withSelectedInventorySlot(slot)
        );
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
                DataOutputStream socketOutput = new DataOutputStream(socket.getOutputStream());
                output = socketOutput;
                snapshot = new CompanionSnapshot(true, snapshot.player, snapshot.inventory);
                notifyChanged();

                DataInputStream input = new DataInputStream(socket.getInputStream());
                while (!closed && !socket.isClosed()) {
                    JSONObject message = CompanionProtocolCodec.readMessage(input);
                    snapshot = CompanionProtocolCodec.applyMessage(snapshot, message);
                    notifyChanged();
                }
            } catch (IOException | JSONException e) {
                if (!closed) Log.d(TAG, "Companion socket unavailable", e);
            } finally {
                socket = null;
                closeOutput();
                if (!closed && snapshot.connected) {
                    snapshot = new CompanionSnapshot(false, snapshot.player, snapshot.inventory);
                    notifyChanged();
                }
            }
            sleepBeforeReconnect();
        }
    }

    private void sendCommand(int slot, boolean hotbar) {
        DataOutputStream currentOutput = output;
        if (currentOutput == null) return;

        try {
            int requestId = requestIds.getAndIncrement();
            JSONObject message = hotbar
                    ? CompanionProtocolCodec.hotbarCommand(requestId, slot)
                    : CompanionProtocolCodec.inventorySelectCommand(requestId, slot);
            synchronized (currentOutput) {
                CompanionProtocolCodec.writeMessage(currentOutput, message);
            }
        } catch (IOException | JSONException e) {
            Log.d(TAG, "Cannot send companion command", e);
        }
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
