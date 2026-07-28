package org.angelauramc.methodsInjectorAgent.minethor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

final class IconStackRegistry {
    private static final ConcurrentMap<String, Object> STACKS = new ConcurrentHashMap<>();

    private IconStackRegistry() {
    }

    static void put(String iconKey, Object itemStack) {
        if (iconKey == null || iconKey.isEmpty() || itemStack == null) return;
        STACKS.put(iconKey, itemStack);
    }

    static Object get(String iconKey) {
        if (iconKey == null || iconKey.isEmpty()) return null;
        return STACKS.get(iconKey);
    }
}
