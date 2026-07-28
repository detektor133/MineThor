package org.angelauramc.methodsInjectorAgent.minethor;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

final class DescriptorMinecraftAdapter extends ReflectiveMinecraftAdapter {
    private static final String DESCRIPTOR_PROPERTY = "minethor.mappingDescriptor";
    private final Properties descriptor = new Properties();
    private boolean loaded;

    @Override
    public String name() {
        return value("name");
    }

    @Override
    public boolean isAvailable() {
        return loadDescriptor() && super.isAvailable();
    }

    @Override
    protected String[] clientClasses() {
        return values("client.classes");
    }

    @Override
    protected String[] clientInstanceMethods() {
        return values("client.instanceMethods");
    }

    @Override
    protected String[] playerFields() {
        return values("player.fields");
    }

    @Override
    protected String[] inventoryMethods() {
        return values("inventory.methods");
    }

    @Override
    protected String[] inventoryFields() {
        return values("inventory.fields");
    }

    @Override
    protected String[] inventoryClassNames() {
        return values("inventory.classNames");
    }

    @Override
    protected String[] selectedSlotFields() {
        return values("selectedSlot.fields");
    }

    @Override
    protected String[] xAccessors() {
        return values("x.accessors");
    }

    @Override
    protected String[] yAccessors() {
        return values("y.accessors");
    }

    @Override
    protected String[] zAccessors() {
        return values("z.accessors");
    }

    @Override
    protected String[] yawAccessors() {
        return values("yaw.accessors");
    }

    @Override
    protected String[] healthAccessors() {
        return values("health.accessors");
    }

    @Override
    protected String[] maxHealthAccessors() {
        return values("maxHealth.accessors");
    }

    @Override
    protected String[] foodAccessors() {
        return values("food.accessors");
    }

    @Override
    protected String[] armorAccessors() {
        return values("armor.accessors");
    }

    @Override
    protected String[] foodDataAccessors() {
        return values("foodData.accessors");
    }

    @Override
    protected String[] xpLevelFields() {
        return values("xpLevel.fields");
    }

    private boolean loadDescriptor() {
        if (loaded) return !descriptor.isEmpty();
        loaded = true;

        String descriptorPath = System.getProperty(DESCRIPTOR_PROPERTY, "");
        if (descriptorPath.isEmpty()) return false;

        try (FileInputStream input = new FileInputStream(descriptorPath)) {
            descriptor.load(input);
            return !descriptor.isEmpty();
        } catch (IOException e) {
            System.out.println("MineThorBridge: cannot read mapping descriptor " + descriptorPath + ": " + e);
            return false;
        }
    }

    private String value(String key) {
        return descriptor.getProperty(key, "");
    }

    private String[] values(String key) {
        String value = value(key);
        if (value.isEmpty()) return new String[0];
        String[] parts = value.split(",");
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim();
        }
        return parts;
    }
}
