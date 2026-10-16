package dev.minethor.agent;

import java.lang.instrument.Instrumentation;

/**
 * Entry point of the MineThor companion agent (-javaagent). Independent from upstream's
 * MioLibPatcher agent so upstream changes to its agent never touch this code.
 */
public final class MineThorAgent {
    private MineThorAgent() {
    }

    public static void premain(String args, Instrumentation inst) {
        MineThorBridgeServer.start();
    }
}
