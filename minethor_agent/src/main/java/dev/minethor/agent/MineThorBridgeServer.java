package dev.minethor.agent;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntPredicate;

/**
 * Loopback bridge between the game and the Android launcher. Started by {@link MineThorAgent}.
 * The launcher passes a free port and a one-time token as JVM arguments
 * ({@code -Dminethor.port}, {@code -Dminethor.token}); without them the bridge stays off.
 */
public final class MineThorBridgeServer {
    private static final String HOST = "127.0.0.1";
    private static final int ACCEPT_TIMEOUT_MS = 250;
    private static final int HANDSHAKE_TIMEOUT_MS = 3000;
    private static final int CLIENT_READ_TIMEOUT_MS = 50;
    private static final int SNAPSHOT_INTERVAL_MS = 100;
    private static final int HEARTBEAT_INTERVAL_MS = 2000;
    private static final int DEAD_PEER_TIMEOUT_MS = 10000;
    private static final int SNAPSHOT_READ_TIMEOUT_MS = 80;
    private static final int COMMAND_TIMEOUT_MS = 250;
    private static volatile boolean started;

    private MineThorBridgeServer() {
    }

    public static void start() {
        if (started) return;

        int port = Integer.getInteger("minethor.port", 0);
        String token = System.getProperty("minethor.token", "");
        if (port <= 0 || token.isEmpty()) {
            System.out.println("MineThorBridge: disabled, the launcher did not provide a session");
            return;
        }
        started = true;

        Thread worker = new Thread(() -> serverLoop(port, token), "MineThorBridgeServer");
        worker.setDaemon(true);
        worker.start();
    }

    private static void serverLoop(int port, String token) {
        MinecraftStateAccess state = new MinecraftStateAccess();
        ClientThread clientThread = new ClientThread(state::clientExecutor);
        try (ServerSocket serverSocket = new ServerSocket(port, 1, InetAddress.getByName(HOST))) {
            serverSocket.setSoTimeout(ACCEPT_TIMEOUT_MS);
            System.out.println("MineThorBridge: listening on " + HOST + ":" + port);

            while (true) {
                try {
                    serveClient(serverSocket.accept(), token, state, clientThread);
                } catch (SocketTimeoutException ignored) {
                }
            }
        } catch (IOException e) {
            System.out.println("MineThorBridge: server unavailable: " + e);
        }
    }

    private static void serveClient(Socket socket, String token, MinecraftStateAccess state, ClientThread clientThread) {
        try (Socket clientSocket = socket) {
            DataInputStream input = new DataInputStream(clientSocket.getInputStream());
            DataOutputStream output = new DataOutputStream(clientSocket.getOutputStream());

            clientSocket.setSoTimeout(HANDSHAKE_TIMEOUT_MS);
            BridgeProtocol.HelloCheck check = BridgeProtocol.checkHello(BridgeProtocol.parse(BridgeProtocol.readFrame(input)), token);
            if (check == BridgeProtocol.HelloCheck.AUTH_FAILED) return; // silent close, reveals nothing
            if (check == BridgeProtocol.HelloCheck.PROTOCOL_MISMATCH) {
                BridgeProtocol.writeFrame(output, BridgeProtocol.rejected(0, BridgeProtocol.REASON_PROTOCOL));
                return;
            }
            clientSocket.setSoTimeout(CLIENT_READ_TIMEOUT_MS);

            boolean clientThreadAvailable = clientThread.available();
            BridgeProtocol.writeFrame(output, BridgeProtocol.hello(clientThreadAvailable, capabilities(clientThreadAvailable), HEARTBEAT_INTERVAL_MS));
            if (!clientThreadAvailable) {
                System.out.println("MineThorBridge: no client-thread executor, game state is read best-effort and commands are refused");
            }

            MinecraftIconRenderer iconRenderer = new MinecraftIconRenderer();
            int lastAppliedRequestId = 0;
            String lastSnapshot = null;
            boolean forceSnapshot = true;
            long now = System.currentTimeMillis();
            long lastReceivedAt = now;
            long lastSentAt = now;
            long lastPollAt = 0L;

            while (!clientSocket.isClosed()) {
                BridgeProtocol.Request request = readRequest(input);
                now = System.currentTimeMillis();
                if (request != null) {
                    lastReceivedAt = now;
                    if ("REQUEST_ICON".equals(request.type)) {
                        MinecraftIconRenderer.IconRenderResult result = iconRenderer.requestIcon(request.iconKey);
                        if (!result.status.startsWith("pending:")) writeIcon(output, request.iconKey, result);
                    } else if ("PING".equals(request.type)) {
                        BridgeProtocol.writeFrame(output, BridgeProtocol.pong());
                    } else if ("SELECT_HOTBAR_SLOT".equals(request.type)) {
                        String reply = selectHotbarSlot(request, lastAppliedRequestId, clientThread, state::selectHotbarSlot);
                        BridgeProtocol.writeFrame(output, reply);
                        if (reply.startsWith("{\"type\":\"ACK\"")) {
                            lastAppliedRequestId = request.requestId;
                            forceSnapshot = true;
                        }
                        lastSentAt = now;
                    } else if (request.requestId > 0 && !"PONG".equals(request.type)) {
                        BridgeProtocol.writeFrame(output, BridgeProtocol.rejected(request.requestId, BridgeProtocol.REASON_BAD_REQUEST));
                    }
                }

                MineThorRenderQueue.CompletedIcon completedIcon;
                while ((completedIcon = iconRenderer.pollCompletedIcon()) != null) {
                    writeIcon(output, completedIcon.iconKey, completedIcon.result);
                }

                if (now - lastPollAt >= SNAPSHOT_INTERVAL_MS) {
                    lastPollAt = now;
                    MinecraftSnapshot snapshot = readSnapshot(state, clientThread, clientThreadAvailable);
                    if (snapshot != null) {
                        // Only send when something changed; unchanged state costs nothing on the wire.
                        String json = BridgeProtocol.snapshot(snapshot, lastAppliedRequestId);
                        if (forceSnapshot || !json.equals(lastSnapshot)) {
                            BridgeProtocol.writeFrame(output, json);
                            lastSnapshot = json;
                            lastSentAt = now;
                            forceSnapshot = false;
                        }
                    }
                }

                if (now - lastSentAt >= HEARTBEAT_INTERVAL_MS) {
                    BridgeProtocol.writeFrame(output, BridgeProtocol.ping());
                    lastSentAt = now;
                }
                if (now - lastReceivedAt > DEAD_PEER_TIMEOUT_MS) {
                    System.out.println("MineThorBridge: launcher stopped answering heartbeat");
                    return;
                }
            }
        } catch (EOFException ignored) {
        } catch (IOException e) {
            System.out.println("MineThorBridge: client disconnected: " + e);
        }
    }

    /** Returns the wire reply (ACK or REJECTED) for a hotbar selection. */
    static String selectHotbarSlot(BridgeProtocol.Request request, int lastAppliedRequestId, ClientThread clientThread, IntPredicate hotbarSetter) {
        if (request.requestId <= 0 || request.slot < 0 || request.slot > 8) {
            return BridgeProtocol.rejected(request.requestId, BridgeProtocol.REASON_BAD_REQUEST);
        }
        if (request.requestId <= lastAppliedRequestId) {
            return BridgeProtocol.rejected(request.requestId, BridgeProtocol.REASON_STALE_STATE);
        }
        if (!clientThread.available()) {
            return BridgeProtocol.rejected(request.requestId, BridgeProtocol.REASON_UNSUPPORTED);
        }

        int slot = request.slot;
        Boolean applied = clientThread.call(() -> hotbarSetter.test(slot), COMMAND_TIMEOUT_MS, null);
        if (applied == null) return BridgeProtocol.rejected(request.requestId, BridgeProtocol.REASON_TIMEOUT);
        if (!applied) return BridgeProtocol.rejected(request.requestId, BridgeProtocol.REASON_UNSUPPORTED);
        return BridgeProtocol.ack(request.requestId);
    }

    private static MinecraftSnapshot readSnapshot(MinecraftStateAccess state, ClientThread clientThread, boolean clientThreadAvailable) {
        if (clientThreadAvailable) return clientThread.call(state::readSnapshot, SNAPSHOT_READ_TIMEOUT_MS, null);
        return state.readSnapshot(); // best effort, see the HELLO clientThread flag
    }

    private static String[] capabilities(boolean clientThreadAvailable) {
        List<String> list = new ArrayList<>();
        list.add("player.state");
        list.add("inventory.read");
        list.add("item.icon");
        if (clientThreadAvailable) list.add("hotbar.select");
        return list.toArray(new String[0]);
    }

    private static void writeIcon(DataOutputStream output, String iconKey, MinecraftIconRenderer.IconRenderResult result) throws IOException {
        String message = BridgeProtocol.icon(iconKey, result.status, result.byteCount, result.pngBase64);
        if (!BridgeProtocol.fits(message)) {
            // An oversized icon must not kill the connection.
            message = BridgeProtocol.icon(iconKey, "too_large", result.byteCount, "");
        }
        BridgeProtocol.writeFrame(output, message);
    }

    private static BridgeProtocol.Request readRequest(DataInputStream input) throws IOException {
        try {
            return BridgeProtocol.parse(BridgeProtocol.readFrame(input));
        } catch (SocketTimeoutException ignored) {
            return null;
        }
    }
}
