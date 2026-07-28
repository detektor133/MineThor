package org.angelauramc.methodsInjectorAgent.minethor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

final class MinecraftStateAccess {
    private static final String[] CLIENT_CLASSES = {
            "net.minecraft.client.Minecraft",
            "net.minecraft.client.MinecraftClient",
            "enn"
    };
    private static final String[] CLIENT_INSTANCE_METHODS = {"getInstance", "getMinecraft", "N"};
    private static final String[] PLAYER_FIELDS = {"player", "thePlayer", "field_1724", "f_91074_", "t"};
    private static final String[] INVENTORY_FIELDS = {"inventory", "field_71071_by", "f_36095_"};
    private static final String[] INVENTORY_METHODS = {"getInventory", "method_31548", "m_150109_"};
    private static final String[] INVENTORY_CLASS_NAMES = {"byn"};
    private static final String[] SELECTED_SLOT_FIELDS = {"selected", "selectedSlot", "currentItem", "field_7545", "f_35977_", "l"};

    MinecraftSnapshot readSnapshot() {
        try {
            Object client = minecraftClient();
            Object player = fieldValue(client, PLAYER_FIELDS);
            if (player == null) return MinecraftSnapshot.disconnected();

            Object inventory = inventory(player);
            Object foodData = invokeFirst(player, "getFoodData", "getHungerManager", "method_7344", "m_36324_");
            return new MinecraftSnapshot(
                    true,
                    roundedDouble(player, 0, "getX", "method_23317", "m_20185_", "J"),
                    roundedDouble(player, 0, "getY", "method_23318", "m_20186_", "K"),
                    roundedDouble(player, 0, "getZ", "method_23321", "m_20189_", "L"),
                    normalizedYaw(floatValue(player, 0f, "getYRot", "getYaw", "method_36454", "m_146908_", "Y")),
                    roundedFloat(player, 0, "getHealth", "method_6032", "m_21223_"),
                    roundedFloat(player, 20, "getMaxHealth", "method_6063", "m_21233_"),
                    intFrom(foodData, 0, "getFoodLevel", "getFoodLevel", "method_7586", "m_38702_"),
                    20,
                    intFrom(player, 0, "getArmorValue", "getArmor", "method_6096", "m_21230_"),
                    intField(player, 0, "experienceLevel", "experienceLevel", "field_7520", "f_108650_"),
                    inventory == null ? -1 : intField(inventory, -1, SELECTED_SLOT_FIELDS),
                    -1
            );
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return MinecraftSnapshot.disconnected();
        }
    }

    boolean selectHotbarSlot(int slot) {
        if (slot < 0 || slot > 8) return false;

        try {
            Object client = minecraftClient();
            Object player = fieldValue(client, PLAYER_FIELDS);
            if (player == null) return false;

            Object inventory = inventory(player);
            if (inventory == null) return false;

            Field field = field(inventory.getClass(), SELECTED_SLOT_FIELDS);
            if (field == null) return false;

            field.setAccessible(true);
            field.setInt(inventory, slot);
            return true;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    private Object minecraftClient() throws ReflectiveOperationException {
        for (String className : CLIENT_CLASSES) {
            Class<?> clientClass = classOrNull(className);
            if (clientClass == null) continue;

            for (String methodName : CLIENT_INSTANCE_METHODS) {
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
        Object inventory = invokeFirst(player, INVENTORY_METHODS);
        if (inventory != null) return inventory;
        inventory = fieldValue(player, INVENTORY_FIELDS);
        if (inventory != null) return inventory;
        return fieldValueByClassName(player, INVENTORY_CLASS_NAMES);
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
