package org.angelauramc.methodsInjectorAgent.minethor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class MineThorRenderQueue {
    private static final Map<String, MinecraftIconRenderer.IconRenderResult> COMPLETED = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> IN_FLIGHT = new ConcurrentHashMap<>();
    private static final ConcurrentLinkedQueue<CompletedIcon> READY = new ConcurrentLinkedQueue<>();
    private static volatile int scheduledTasks;
    private static volatile int completedTasks;

    private MineThorRenderQueue() {
    }

    public static MinecraftIconRenderer.IconRenderResult requestIcon(String iconKey, MinecraftIconRenderer renderer) {
        MinecraftIconRenderer.IconRenderResult completed = COMPLETED.get(iconKey);
        if (completed != null) return completed;

        Object itemStack = IconStackRegistry.get(iconKey);
        if (itemStack == null) return MinecraftIconRenderer.IconRenderResult.error("stack-missing");

        Boolean existing = IN_FLIGHT.putIfAbsent(iconKey, Boolean.TRUE);
        if (existing == null) {
            scheduledTasks++;
            renderer.scheduleIconRender(iconKey, itemStack);
        }
        return MinecraftIconRenderer.IconRenderResult.pending(diagnostics());
    }

    public static CompletedIcon pollCompleted() {
        return READY.poll();
    }

    public static void complete(String iconKey, MinecraftIconRenderer.IconRenderResult result) {
        IN_FLIGHT.remove(iconKey);
        completedTasks++;
        if ("ok".equals(result.status)) COMPLETED.put(iconKey, result);
        READY.offer(new CompletedIcon(iconKey, result));
    }

    private static String diagnostics() {
        return "scheduledTasks=" + scheduledTasks
                + ",completedTasks=" + completedTasks
                + ",inFlight=" + IN_FLIGHT.size();
    }

    public static final class CompletedIcon {
        public final String iconKey;
        public final MinecraftIconRenderer.IconRenderResult result;

        CompletedIcon(String iconKey, MinecraftIconRenderer.IconRenderResult result) {
            this.iconKey = iconKey;
            this.result = result;
        }
    }
}
