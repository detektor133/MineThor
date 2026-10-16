package net.kdt.pojavlaunch.minethor;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.security.SecureRandom;

/**
 * Where and how the launcher talks to the agent inside the game JVM. A new session (free loopback
 * port + one-time token) is created for every game launch and handed to the agent as JVM
 * arguments, so no other app on the device can use the bridge.
 */
public final class CompanionEndpoint {
    public static final String LOOPBACK_HOST = "127.0.0.1";

    public enum Status {
        /** No game launched yet, there is nothing to connect to. */
        WAITING_FOR_GAME,
        CONNECTING,
        CONNECTED,
        /** The agent closed the connection right after the handshake: the token did not match. */
        AUTH_REJECTED,
        PROTOCOL_MISMATCH
    }

    private static volatile int port;
    private static volatile String token = "";
    private static volatile Status status = Status.WAITING_FOR_GAME;
    private static volatile boolean mappingsAvailable = true;

    private CompanionEndpoint() {
    }

    /** Creates a fresh port and token. Returns false if no free port could be found. */
    public static synchronized boolean newSession() {
        try (ServerSocket probe = new ServerSocket(0, 1, InetAddress.getByName(LOOPBACK_HOST))) {
            port = probe.getLocalPort();
        } catch (IOException e) {
            port = 0;
            token = "";
            return false;
        }
        token = randomToken();
        status = Status.WAITING_FOR_GAME;
        return true;
    }

    public static boolean hasSession() {
        return port > 0 && !token.isEmpty();
    }

    public static int port() {
        return port;
    }

    public static String token() {
        return token;
    }

    public static Status status() {
        return status;
    }

    public static void setStatus(Status newStatus) {
        status = newStatus;
    }

    /** False when no mapping descriptor could be prepared for the launched version (degraded mode). */
    public static boolean mappingsAvailable() {
        return mappingsAvailable;
    }

    public static void setMappingsAvailable(boolean available) {
        mappingsAvailable = available;
    }

    static String randomToken() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) builder.append(String.format("%02x", b));
        return builder.toString();
    }
}
