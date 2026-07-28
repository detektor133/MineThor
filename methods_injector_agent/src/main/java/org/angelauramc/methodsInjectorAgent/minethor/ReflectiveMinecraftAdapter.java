package org.angelauramc.methodsInjectorAgent.minethor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

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
                    inventory == null ? -1 : intField(inventory, -1, selectedSlotFields()),
                    -1
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

    protected abstract String[] inventoryMethods();

    protected abstract String[] inventoryFields();

    protected abstract String[] inventoryClassNames();

    protected abstract String[] selectedSlotFields();

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
