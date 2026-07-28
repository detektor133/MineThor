package org.angelauramc.methodsInjectorAgent.minethor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

final class MinecraftIconRenderer {
    private static final int ICON_SIZE = 32;
    private static final int GL_COLOR_BUFFER_BIT = 16384;
    private static final int ICON_TIMEOUT_SECONDS = 2;

    String renderIconBase64(String iconKey) {
        Object itemStack = IconStackRegistry.get(iconKey);
        if (itemStack == null) return "";

        try {
            Object client = minecraftClient();
            Object result = submitToClient(client, () -> renderIconOnClientThread(client, itemStack));
            if (!(result instanceof byte[])) return "";
            return Base64.getEncoder().encodeToString((byte[]) result);
        } catch (ReflectiveOperationException | RuntimeException e) {
            System.out.println("MineThorBridge: cannot render icon " + iconKey + ": " + e);
            return "";
        }
    }

    private Object renderIconOnClientThread(Object client, Object itemStack) {
        Object target = null;
        Object image = null;
        try {
            target = textureTarget();
            invoke(target, "renderTarget.bindWriteMethods", true);
            clearTarget();

            Object guiGraphics = guiGraphics(client);
            invoke(guiGraphics, "guiGraphics.renderItemMethods", itemStack, 8, 8);
            invoke(guiGraphics, "guiGraphics.flushMethods");

            image = invokeStatic(classFor("screenshot.classes"), "screenshot.takeMethods", target);
            return invoke(image, "nativeImage.byteArrayMethods");
        } catch (ReflectiveOperationException | RuntimeException e) {
            System.out.println("MineThorBridge: icon render step failed: " + e);
            return new byte[0];
        } finally {
            closeNativeImage(image);
            destroyTarget(target);
        }
    }

    private Object minecraftClient() throws ReflectiveOperationException {
        for (String className : DescriptorProperties.values("client.classes")) {
            Class<?> clientClass = classOrNull(className);
            if (clientClass == null) continue;

            for (String methodName : DescriptorProperties.values("client.instanceMethods")) {
                Method method = methodOrNull(clientClass, methodName);
                if (method == null) continue;

                method.setAccessible(true);
                Object client = method.invoke(null);
                if (client != null) return client;
            }
        }
        throw new ClassNotFoundException("Minecraft client class is not loaded");
    }

    private Object submitToClient(Object client, Supplier<Object> supplier) throws ReflectiveOperationException {
        Method submit = method(client.getClass(), DescriptorProperties.values("client.submitSupplierMethods"), Supplier.class);
        submit.setAccessible(true);
        Object future = submit.invoke(client, supplier);
        if (!(future instanceof CompletableFuture)) return "";

        try {
            return ((CompletableFuture<?>) future).get(ICON_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException("Icon render timeout", e);
        }
    }

    private Object textureTarget() throws ReflectiveOperationException {
        Class<?> targetClass = classFor("textureTarget.classes");
        Constructor<?> constructor = targetClass.getDeclaredConstructor(int.class, int.class, boolean.class, boolean.class);
        constructor.setAccessible(true);
        return constructor.newInstance(ICON_SIZE, ICON_SIZE, true, true);
    }

    private Object guiGraphics(Object client) throws ReflectiveOperationException {
        Class<?> guiClass = classFor("guiGraphics.classes");
        for (Constructor<?> constructor : guiClass.getDeclaredConstructors()) {
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            if (parameterTypes.length != 2 || !parameterTypes[0].isAssignableFrom(client.getClass())) continue;

            Object bufferSource = bufferSource(parameterTypes[1]);
            constructor.setAccessible(true);
            return constructor.newInstance(client, bufferSource);
        }
        throw new NoSuchMethodException("GuiGraphics constructor");
    }

    private Object bufferSource(Class<?> expectedType) throws ReflectiveOperationException {
        Class<?> tesselatorClass = classFor("tesselator.classes");
        Object tesselator = method(tesselatorClass, DescriptorProperties.values("tesselator.instanceMethods")).invoke(null);
        Object builder = method(tesselator.getClass(), DescriptorProperties.values("tesselator.builderMethods")).invoke(tesselator);

        Class<?> multiBufferSourceClass = classFor("multiBufferSource.classes");
        for (String methodName : DescriptorProperties.values("multiBufferSource.immediateMethods")) {
            Method method = methodOrNull(multiBufferSourceClass, methodName, builder);
            if (method == null) continue;
            if (!expectedType.isAssignableFrom(method.getReturnType())) continue;

            method.setAccessible(true);
            return method.invoke(null, builder);
        }
        throw new NoSuchMethodException("MultiBufferSource.immediate");
    }

    private void clearTarget() throws ReflectiveOperationException {
        Class<?> renderSystem = Class.forName("com.mojang.blaze3d.systems.RenderSystem");
        method(renderSystem, new String[]{"clearColor"}, float.class, float.class, float.class, float.class).invoke(null, 0f, 0f, 0f, 0f);
        method(renderSystem, new String[]{"clear"}, int.class, boolean.class).invoke(null, GL_COLOR_BUFFER_BIT, false);
    }

    private void closeNativeImage(Object image) {
        if (image == null) return;

        try {
            invoke(image, "nativeImage.closeMethods");
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private void destroyTarget(Object target) {
        if (target == null) return;

        try {
            invoke(target, "renderTarget.unbindWriteMethods");
            invoke(target, "renderTarget.destroyBuffersMethods");
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private Object invoke(Object target, String descriptorKey, Object... args) throws ReflectiveOperationException {
        Method method = method(target.getClass(), DescriptorProperties.values(descriptorKey), argTypes(args));
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    private Object invokeStatic(Class<?> sourceClass, String descriptorKey, Object... args) throws ReflectiveOperationException {
        Method method = method(sourceClass, DescriptorProperties.values(descriptorKey), argTypes(args));
        method.setAccessible(true);
        return method.invoke(null, args);
    }

    private static Class<?> classFor(String descriptorKey) throws ReflectiveOperationException {
        for (String className : DescriptorProperties.values(descriptorKey)) {
            Class<?> sourceClass = classOrNull(className);
            if (sourceClass != null) return sourceClass;
        }
        throw new ClassNotFoundException(descriptorKey);
    }

    private static Class<?>[] argTypes(Object[] args) {
        Class<?>[] types = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            types[i] = primitiveCompatibleType(args[i]);
        }
        return types;
    }

    private static Class<?> primitiveCompatibleType(Object arg) {
        if (arg instanceof Integer) return int.class;
        if (arg instanceof Boolean) return boolean.class;
        if (arg instanceof Float) return float.class;
        return arg.getClass();
    }

    private static Method method(Class<?> sourceClass, String... names) throws ReflectiveOperationException {
        return method(sourceClass, names, new Class<?>[0]);
    }

    private static Method method(Class<?> sourceClass, String[] names, Class<?>... parameterTypes) throws ReflectiveOperationException {
        Class<?> currentClass = sourceClass;
        while (currentClass != null) {
            for (String name : names) {
                try {
                    return currentClass.getDeclaredMethod(name, parameterTypes);
                } catch (NoSuchMethodException ignored) {
                }
            }
            currentClass = currentClass.getSuperclass();
        }
        throw new NoSuchMethodException(sourceClass.getName());
    }

    private static Method methodOrNull(Class<?> sourceClass, String name) {
        return methodOrNull(sourceClass, name, new Class<?>[0]);
    }

    private static Method methodOrNull(Class<?> sourceClass, String name, Class<?>... parameterTypes) {
        Class<?> currentClass = sourceClass;
        while (currentClass != null) {
            try {
                return currentClass.getDeclaredMethod(name, parameterTypes);
            } catch (NoSuchMethodException ignored) {
            }
            currentClass = currentClass.getSuperclass();
        }
        return null;
    }

    private static Method methodOrNull(Class<?> sourceClass, String name, Object... args) {
        Class<?> currentClass = sourceClass;
        while (currentClass != null) {
            for (Method method : currentClass.getDeclaredMethods()) {
                if (!method.getName().equals(name) || !parametersMatch(method.getParameterTypes(), args)) continue;
                return method;
            }
            currentClass = currentClass.getSuperclass();
        }
        return null;
    }

    private static boolean parametersMatch(Class<?>[] parameterTypes, Object[] args) {
        if (parameterTypes.length != args.length) return false;

        for (int i = 0; i < parameterTypes.length; i++) {
            if (args[i] == null) continue;
            if (!boxed(parameterTypes[i]).isAssignableFrom(args[i].getClass())) return false;
        }
        return true;
    }

    private static Class<?> boxed(Class<?> sourceClass) {
        if (!sourceClass.isPrimitive()) return sourceClass;
        if (sourceClass == int.class) return Integer.class;
        if (sourceClass == boolean.class) return Boolean.class;
        if (sourceClass == float.class) return Float.class;
        return sourceClass;
    }

    private static Class<?> classOrNull(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }
}
