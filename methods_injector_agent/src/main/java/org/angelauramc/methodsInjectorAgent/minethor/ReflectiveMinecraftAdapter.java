package org.angelauramc.methodsInjectorAgent.minethor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

abstract class ReflectiveMinecraftAdapter implements MinecraftAdapter {
    @Override
    public boolean isAvailable() {
        try {
            return minecraftClient() != null;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    @Override
    public MinecraftSnapshot readSnapshot() {
        try {
            Object client = minecraftClient();
            Object player = fieldValue(client, playerFields());
            if (player == null) return MinecraftSnapshot.disconnected();

            Object inventory = inventory(player);
            Object foodData = invokeFirst(player, foodDataAccessors());
            Object level = fieldValue(client, levelFields());
            return new MinecraftSnapshot(
                    true,
                    roundedDouble(player, 0, xAccessors()),
                    roundedDouble(player, 0, yAccessors()),
                    roundedDouble(player, 0, zAccessors()),
                    normalizedYaw(floatValue(player, 0f, yawAccessors())),
                    roundedFloat(player, 0, healthAccessors()),
                    roundedFloat(player, 20, maxHealthAccessors()),
                    intFrom(foodData, 0, foodAccessors()),
                    20,
                    intFrom(player, 0, armorAccessors()),
                    intField(player, 0, xpLevelFields()),
                    longFrom(level, 0L, dayTimeAccessors()),
                    inventory == null ? -1 : intField(inventory, -1, selectedSlotFields()),
                    -1,
                    inventory == null ? MinecraftSnapshot.emptySlots(36) : inventorySlots(inventory, mainInventoryFields(), 36),
                    inventory == null ? MinecraftSnapshot.emptySlots(4) : inventorySlots(inventory, armorInventoryFields(), 4),
                    inventory == null ? MinecraftSnapshot.emptySlots(1) : inventorySlots(inventory, offhandInventoryFields(), 1)
            );
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return MinecraftSnapshot.disconnected();
        }
    }

    @Override
    public boolean selectHotbarSlot(int slot) {
        if (slot < 0 || slot > 8) return false;

        try {
            Object client = minecraftClient();
            Object player = fieldValue(client, playerFields());
            if (player == null) return false;

            Object inventory = inventory(player);
            if (inventory == null) return false;

            Field field = field(inventory.getClass(), selectedSlotFields());
            if (field == null) return false;

            field.setAccessible(true);
            field.setInt(inventory, slot);
            return true;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    protected abstract String[] clientClasses();

    protected abstract String[] clientInstanceMethods();

    protected abstract String[] playerFields();

    protected abstract String[] levelFields();

    protected abstract String[] inventoryMethods();

    protected abstract String[] inventoryFields();

    protected abstract String[] inventoryClassNames();

    protected abstract String[] selectedSlotFields();

    protected abstract String[] mainInventoryFields();

    protected abstract String[] armorInventoryFields();

    protected abstract String[] offhandInventoryFields();

    protected abstract String[] itemStackIsEmptyMethods();

    protected abstract String[] itemStackCountMethods();

    protected abstract String[] itemStackDescriptionIdMethods();

    protected abstract String[] itemStackHoverNameMethods();

    protected abstract String[] itemStackDamageMethods();

    protected abstract String[] itemStackMaxDamageMethods();

    protected abstract String[] itemStackCopyMethods();

    protected abstract String[] itemStackTagMethods();

    protected abstract String[] itemStackFoilMethods();

    protected abstract String[] componentStringMethods();

    protected abstract String[] xAccessors();

    protected abstract String[] yAccessors();

    protected abstract String[] zAccessors();

    protected abstract String[] yawAccessors();

    protected abstract String[] healthAccessors();

    protected abstract String[] maxHealthAccessors();

    protected abstract String[] foodAccessors();

    protected abstract String[] armorAccessors();

    protected abstract String[] foodDataAccessors();

    protected abstract String[] xpLevelFields();

    protected abstract String[] dayTimeAccessors();

    private Object minecraftClient() throws ReflectiveOperationException {
        for (String className : clientClasses()) {
            Class<?> clientClass = classOrNull(className);
            if (clientClass == null) continue;

            for (String methodName : clientInstanceMethods()) {
                Method method = methodOrNull(clientClass, methodName);
                if (method == null) continue;

                method.setAccessible(true);
                Object client = method.invoke(null);
                if (client != null) return client;
            }
        }
        throw new ClassNotFoundException("Minecraft client class is not loaded");
    }

    private Object inventory(Object player) throws ReflectiveOperationException {
        Object inventory = invokeFirst(player, inventoryMethods());
        if (inventory != null) return inventory;
        inventory = fieldValue(player, inventoryFields());
        if (inventory != null) return inventory;
        return fieldValueByClassName(player, inventoryClassNames());
    }

    private static Object invokeFirst(Object target, String... names) throws ReflectiveOperationException {
        if (target == null) return null;

        for (String name : names) {
            Method method = methodOrNull(target.getClass(), name);
            if (method == null) continue;

            method.setAccessible(true);
            return method.invoke(target);
        }
        return null;
    }

    private static int intFrom(Object target, int fallback, String... names) throws ReflectiveOperationException {
        Object value = invokeFirst(target, names);
        if (value instanceof Number) return ((Number) value).intValue();
        return fallback;
    }

    private static long longFrom(Object target, long fallback, String... names) throws ReflectiveOperationException {
        Object value = invokeFirst(target, names);
        if (value instanceof Number) return ((Number) value).longValue();
        return fallback;
    }

    private InventorySlotSnapshot[] inventorySlots(Object inventory, String[] fields, int count) throws ReflectiveOperationException {
        InventorySlotSnapshot[] slots = MinecraftSnapshot.emptySlots(count);
        Object list = fieldValue(inventory, fields);
        if (!(list instanceof List)) return slots;

        List<?> stacks = (List<?>) list;
        int size = Math.min(stacks.size(), count);
        for (int i = 0; i < size; i++) {
            slots[i] = itemStackSlot(i, stacks.get(i));
        }
        return slots;
    }

    private InventorySlotSnapshot itemStackSlot(int index, Object itemStack) throws ReflectiveOperationException {
        if (itemStack == null || booleanFrom(itemStack, true, itemStackIsEmptyMethods())) {
            return InventorySlotSnapshot.empty(index);
        }

        int count = intFrom(itemStack, 0, itemStackCountMethods());
        String descriptionId = stringFrom(itemStack, "", itemStackDescriptionIdMethods());
        String itemId = itemIdFromDescriptionId(descriptionId);
        String name = componentString(invokeFirst(itemStack, itemStackHoverNameMethods()));
        int damage = intFrom(itemStack, 0, itemStackDamageMethods());
        int maxDamage = intFrom(itemStack, 0, itemStackMaxDamageMethods());
        boolean foil = booleanFrom(itemStack, false, itemStackFoilMethods());
        String tag = stringValue(invokeFirst(itemStack, itemStackTagMethods()));
        String fingerprint = itemId + "|d=" + damage + "|m=" + maxDamage + "|f=" + foil + "|t=" + tag;
        String iconKey = sha1(fingerprint);
        Object copy = invokeFirst(itemStack, itemStackCopyMethods());
        IconStackRegistry.put(iconKey, copy == null ? itemStack : copy);
        return new InventorySlotSnapshot(index, itemId, name, iconKey, count, damage, maxDamage);
    }

    private static boolean booleanFrom(Object target, boolean fallback, String... names) throws ReflectiveOperationException {
        Object value = invokeFirst(target, names);
        if (value instanceof Boolean) return (Boolean) value;
        return fallback;
    }

    private static String stringFrom(Object target, String fallback, String... names) throws ReflectiveOperationException {
        Object value = invokeFirst(target, names);
        if (value instanceof String) return (String) value;
        return fallback;
    }

    private String componentString(Object component) throws ReflectiveOperationException {
        String value = stringFrom(component, "", componentStringMethods());
        if (!value.isEmpty()) return value;
        return component == null ? "" : component.toString();
    }

    private static String stringValue(Object value) {
        return value == null ? "" : value.toString();
    }

    private static String sha1(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] bytes = digest.digest(value.getBytes("UTF-8"));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                String hex = Integer.toHexString(b & 0xff);
                if (hex.length() == 1) builder.append('0');
                builder.append(hex);
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException | java.io.UnsupportedEncodingException e) {
            return Integer.toHexString(value.hashCode());
        }
    }

    private static String itemIdFromDescriptionId(String descriptionId) {
        String itemPrefix = "item.";
        String blockPrefix = "block.";
        String value = descriptionId;
        if (value.startsWith(itemPrefix)) {
            value = value.substring(itemPrefix.length());
        } else if (value.startsWith(blockPrefix)) {
            value = value.substring(blockPrefix.length());
        }

        int namespaceEnd = value.indexOf('.');
        if (namespaceEnd < 0) return value;
        return value.substring(0, namespaceEnd) + ":" + value.substring(namespaceEnd + 1).replace('.', '_');
    }

    private static int roundedDouble(Object target, int fallback, String... names) throws ReflectiveOperationException {
        Object value = invokeFirst(target, names);
        if (value instanceof Number) return (int) Math.round(((Number) value).doubleValue());
        value = fieldValue(target, names);
        if (value instanceof Number) return (int) Math.round(((Number) value).doubleValue());
        return fallback;
    }

    private static int roundedFloat(Object target, int fallback, String... names) throws ReflectiveOperationException {
        Object value = invokeFirst(target, names);
        if (value instanceof Number) return Math.round(((Number) value).floatValue());
        value = fieldValue(target, names);
        if (value instanceof Number) return Math.round(((Number) value).floatValue());
        return fallback;
    }

    private static float floatValue(Object target, float fallback, String... names) throws ReflectiveOperationException {
        Object value = invokeFirst(target, names);
        if (value instanceof Number) return ((Number) value).floatValue();
        value = fieldValue(target, names);
        if (value instanceof Number) return ((Number) value).floatValue();
        return fallback;
    }

    private static int normalizedYaw(float yaw) {
        int roundedYaw = Math.round(yaw) % 360;
        return roundedYaw < 0 ? roundedYaw + 360 : roundedYaw;
    }

    private static int intField(Object target, int fallback, String... names) throws ReflectiveOperationException {
        if (target == null) return fallback;

        Field field = field(target.getClass(), names);
        if (field == null) return fallback;

        field.setAccessible(true);
        return field.getInt(target);
    }

    private static Object fieldValue(Object target, String... names) throws ReflectiveOperationException {
        if (target == null) return null;

        Field field = field(target.getClass(), names);
        if (field == null) return null;

        field.setAccessible(true);
        return field.get(target);
    }

    private static Object fieldValueByClassName(Object target, String... classNames) throws ReflectiveOperationException {
        if (target == null) return null;

        Class<?> currentClass = target.getClass();
        while (currentClass != null) {
            for (Field field : currentClass.getDeclaredFields()) {
                for (String className : classNames) {
                    if (!field.getType().getName().equals(className)) continue;

                    field.setAccessible(true);
                    return field.get(target);
                }
            }
            currentClass = currentClass.getSuperclass();
        }
        return null;
    }

    private static Field field(Class<?> sourceClass, String... names) {
        Class<?> currentClass = sourceClass;
        while (currentClass != null) {
            for (String name : names) {
                try {
                    return currentClass.getDeclaredField(name);
                } catch (NoSuchFieldException ignored) {
                }
            }
            currentClass = currentClass.getSuperclass();
        }
        return null;
    }

    private static Method methodOrNull(Class<?> sourceClass, String name) {
        Class<?> currentClass = sourceClass;
        while (currentClass != null) {
            try {
                return currentClass.getDeclaredMethod(name);
            } catch (NoSuchMethodException ignored) {
            }
            currentClass = currentClass.getSuperclass();
        }
        return null;
    }

    private static Class<?> classOrNull(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }
}
