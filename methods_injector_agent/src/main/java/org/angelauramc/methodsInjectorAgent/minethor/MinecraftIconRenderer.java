package org.angelauramc.methodsInjectorAgent.minethor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Base64;
import java.util.concurrent.Executor;

final class MinecraftIconRenderer {
    private static final int ICON_SIZE = 64;
    private static final float ICON_SCALE = 4f;
    private static final int GL_COLOR_BUFFER_BIT = 16384;

    IconRenderResult requestIcon(String iconKey) {
        return MineThorRenderQueue.requestIcon(iconKey, this);
    }

    MineThorRenderQueue.CompletedIcon pollCompletedIcon() {
        return MineThorRenderQueue.pollCompleted();
    }

    void scheduleIconRender(String iconKey, Object itemStack) {
        try {
            Object client = minecraftClient();
            if (!(client instanceof Executor)) {
                MineThorRenderQueue.complete(iconKey, IconRenderResult.error("client-not-executor:" + client.getClass().getName()));
                return;
            }

            ((Executor) client).execute(() -> {
                MineThorRenderQueue.complete(iconKey, renderIconOnClientThread(iconKey, itemStack));
            });
        } catch (ReflectiveOperationException | RuntimeException e) {
            MineThorRenderQueue.complete(iconKey, IconRenderResult.error("schedule-failed:" + e.getClass().getSimpleName() + ":" + e.getMessage()));
        }
    }

    private IconRenderResult renderIconOnClientThread(String iconKey, Object itemStack) {
        try {
            Object client = minecraftClient();
            Object result = renderIconToPngBytes(client, itemStack);
            if (!(result instanceof byte[])) {
                String resultType = result == null ? "null" : result.getClass().getName();
                return IconRenderResult.error("unexpected-result:" + resultType);
            }
            byte[] pngBytes = (byte[]) result;
            if (pngBytes.length == 0) return IconRenderResult.error("empty-png");
            return IconRenderResult.success(Base64.getEncoder().encodeToString(pngBytes), pngBytes.length);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return IconRenderResult.error(e.getClass().getSimpleName() + ":" + e.getMessage());
        }
    }

    private Object renderIconToPngBytes(Object client, Object itemStack) {
        Object target = null;
        Object image = null;
        RenderState renderState = null;
        try {
            target = textureTarget();
            renderState = captureRenderState(client);
            invoke(target, "renderTarget.bindWriteMethods", true);
            clearTarget();
            setupIconProjection();

            Object guiGraphics = guiGraphics(client);
            invoke(guiGraphics, "guiGraphics.renderItemMethods", itemStack, 0, 0);
            invoke(guiGraphics, "guiGraphics.flushMethods");

            image = invokeStatic(classFor("screenshot.classes"), "screenshot.takeMethods", target);
            return invoke(image, "nativeImage.byteArrayMethods");
        } catch (ReflectiveOperationException | RuntimeException e) {
            throw new IllegalStateException("render-step:" + e.getClass().getSimpleName() + ":" + e.getMessage(), e);
        } finally {
            restoreRenderState(renderState);
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

    private void setupIconProjection() throws ReflectiveOperationException {
        Class<?> renderSystem = Class.forName("com.mojang.blaze3d.systems.RenderSystem");
        method(renderSystem, new String[]{"backupProjectionMatrix"}).invoke(null);
        method(renderSystem, new String[]{"viewport"}, int.class, int.class, int.class, int.class).invoke(null, 0, 0, ICON_SIZE, ICON_SIZE);

        Class<?> matrixClass = Class.forName("org.joml.Matrix4f");
        Object projection = matrixClass.getDeclaredConstructor().newInstance();
        Method setOrtho = methodOrNull(matrixClass, "setOrtho", float.class, float.class, float.class, float.class, float.class, float.class);
        if (setOrtho == null) setOrtho = methodOrNull(matrixClass, "ortho", float.class, float.class, float.class, float.class, float.class, float.class);
        if (setOrtho == null) throw new NoSuchMethodException("Matrix4f.setOrtho");
        setOrtho.invoke(projection, 0f, (float) ICON_SIZE, (float) ICON_SIZE, 0f, 1000f, 3000f);

        Method setProjection = methodOrNullByName(renderSystem, "setProjectionMatrix", 2);
        if (setProjection == null) throw new NoSuchMethodException("RenderSystem.setProjectionMatrix");
        Object vertexSorting = vertexSorting(setProjection.getParameterTypes()[1]);
        if (vertexSorting == null) throw new NoSuchFieldException(setProjection.getParameterTypes()[1].getName());
        setProjection.invoke(null, projection, vertexSorting);

        Object modelViewStack = method(renderSystem, new String[]{"getModelViewStack"}).invoke(null);
        invoke(modelViewStack, "poseStack.setIdentityMethods");
        invoke(modelViewStack, "poseStack.translateMethods", 0f, 0f, -2000f);
        invoke(modelViewStack, "poseStack.scaleMethods", ICON_SCALE, ICON_SCALE, 1f);
        method(renderSystem, new String[]{"applyModelViewMatrix"}).invoke(null);
    }

    private RenderState captureRenderState(Object client) throws ReflectiveOperationException {
        return new RenderState(mainRenderTarget(client), windowWidth(client), windowHeight(client));
    }

    private void restoreRenderState(RenderState renderState) {
        if (renderState == null) return;

        try {
            Class<?> renderSystem = Class.forName("com.mojang.blaze3d.systems.RenderSystem");
            method(renderSystem, new String[]{"restoreProjectionMatrix"}).invoke(null);
            Object modelViewStack = method(renderSystem, new String[]{"getModelViewStack"}).invoke(null);
            invoke(modelViewStack, "poseStack.setIdentityMethods");
            method(renderSystem, new String[]{"applyModelViewMatrix"}).invoke(null);
            invoke(renderState.renderTarget, "renderTarget.bindWriteMethods", true);
            method(renderSystem, new String[]{"viewport"}, int.class, int.class, int.class, int.class)
                    .invoke(null, 0, 0, renderState.width, renderState.height);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private Object mainRenderTarget(Object client) throws ReflectiveOperationException {
        return fieldValue(client, "mainRenderTarget.fields");
    }

    private int windowWidth(Object client) throws ReflectiveOperationException {
        Object window = window(client);
        Object value = invoke(window, "window.widthMethods");
        if (value instanceof Number) return ((Number) value).intValue();
        throw new IllegalStateException("Window width is not numeric");
    }

    private int windowHeight(Object client) throws ReflectiveOperationException {
        Object window = window(client);
        Object value = invoke(window, "window.heightMethods");
        if (value instanceof Number) return ((Number) value).intValue();
        throw new IllegalStateException("Window height is not numeric");
    }

    private Object window(Object client) throws ReflectiveOperationException {
        return fieldValue(client, "window.fields");
    }

    private Object fieldValue(Object target, String descriptorKey) throws ReflectiveOperationException {
        for (String fieldName : DescriptorProperties.values(descriptorKey)) {
            Field field = fieldOrNull(target.getClass(), fieldName);
            if (field == null) continue;

            field.setAccessible(true);
            Object value = field.get(target);
            if (value != null) return value;
        }
        throw new NoSuchFieldException(target.getClass().getName() + "." + descriptorKey);
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
        for (String methodName : DescriptorProperties.values(descriptorKey)) {
            Method method = methodOrNull(target.getClass(), methodName, args);
            if (method == null) continue;

            method.setAccessible(true);
            return method.invoke(target, args);
        }
        throw new NoSuchMethodException(target.getClass().getName());
    }

    private Object invokeStatic(Class<?> sourceClass, String descriptorKey, Object... args) throws ReflectiveOperationException {
        for (String methodName : DescriptorProperties.values(descriptorKey)) {
            Method method = methodOrNull(sourceClass, methodName, args);
            if (method == null) continue;

            method.setAccessible(true);
            return method.invoke(null, args);
        }
        throw new NoSuchMethodException(sourceClass.getName());
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

    private static Method methodOrNullByName(Class<?> sourceClass, String name, int arity) {
        Class<?> currentClass = sourceClass;
        while (currentClass != null) {
            for (Method method : currentClass.getDeclaredMethods()) {
                if (method.getName().equals(name) && method.getParameterTypes().length == arity) return method;
            }
            currentClass = currentClass.getSuperclass();
        }
        return null;
    }

    private static Object firstStaticValue(Class<?> sourceClass) throws ReflectiveOperationException {
        for (Field field : sourceClass.getDeclaredFields()) {
            int modifiers = field.getModifiers();
            if (!Modifier.isStatic(modifiers) || !sourceClass.isAssignableFrom(field.getType())) continue;

            field.setAccessible(true);
            return field.get(null);
        }
        return null;
    }

    private static Object vertexSorting(Class<?> expectedClass) throws ReflectiveOperationException {
        Class<?> sourceClass = classFor("vertexSorting.classes");
        for (String fieldName : DescriptorProperties.values("vertexSorting.orthographicFields")) {
            Field field = fieldOrNull(sourceClass, fieldName);
            if (field == null || !expectedClass.isAssignableFrom(field.getType())) continue;

            field.setAccessible(true);
            return field.get(null);
        }
        return firstStaticValue(expectedClass);
    }

    private static Field fieldOrNull(Class<?> sourceClass, String name) {
        Class<?> currentClass = sourceClass;
        while (currentClass != null) {
            try {
                return currentClass.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
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

    static final class IconRenderResult {
        final String pngBase64;
        final String status;
        final int byteCount;

        private IconRenderResult(String pngBase64, String status, int byteCount) {
            this.pngBase64 = pngBase64;
            this.status = status;
            this.byteCount = byteCount;
        }

        static IconRenderResult success(String pngBase64, int byteCount) {
            return new IconRenderResult(pngBase64, "ok", byteCount);
        }

        static IconRenderResult error(String status) {
            return new IconRenderResult("", status, 0);
        }

        static IconRenderResult pending(String status) {
            return new IconRenderResult("", "pending:" + status, 0);
        }
    }

    private static final class RenderState {
        final Object renderTarget;
        final int width;
        final int height;

        RenderState(Object renderTarget, int width, int height) {
            this.renderTarget = renderTarget;
            this.width = width;
            this.height = height;
        }
    }
}
