package org.angelauramc.methodsInjectorAgent.minethor;

import java.util.LinkedHashMap;
import java.util.Map;

final class IconStackRegistry {
    // Long play sessions can produce many distinct item/damage/enchant fingerprints;
    // cap retention so this doesn't grow without bound for the life of the game process.
    private static final int MAX_ENTRIES = 512;
    private static final Map<String, Object> STACKS = new LinkedHashMap<String, Object>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Object> eldest) {
            return size() > MAX_ENTRIES;
        }
    };

    private IconStackRegistry() {
    }

    static synchronized void put(String iconKey, Object itemStack) {
        if (iconKey == null || iconKey.isEmpty() || itemStack == null) return;
        STACKS.put(iconKey, itemStack);
    }

    static synchronized Object get(String iconKey) {
        if (iconKey == null || iconKey.isEmpty()) return null;
        return STACKS.get(iconKey);
    }
}
