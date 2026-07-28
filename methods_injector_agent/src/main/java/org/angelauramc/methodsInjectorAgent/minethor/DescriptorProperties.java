package org.angelauramc.methodsInjectorAgent.minethor;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

final class DescriptorProperties {
    private static final String DESCRIPTOR_PROPERTY = "minethor.mappingDescriptor";
    private static final Properties DESCRIPTOR = new Properties();
    private static boolean loaded;

    private DescriptorProperties() {
    }

    static boolean load() {
        if (loaded) return !DESCRIPTOR.isEmpty();
        loaded = true;

        String descriptorPath = System.getProperty(DESCRIPTOR_PROPERTY, "");
        if (descriptorPath.isEmpty()) return false;

        try (FileInputStream input = new FileInputStream(descriptorPath)) {
            DESCRIPTOR.load(input);
            return !DESCRIPTOR.isEmpty();
        } catch (IOException e) {
            System.out.println("MineThorBridge: cannot read mapping descriptor " + descriptorPath + ": " + e);
            return false;
        }
    }

    static String value(String key) {
        load();
        return DESCRIPTOR.getProperty(key, "");
    }

    static String[] values(String key) {
        String value = value(key);
        if (value.isEmpty()) return new String[0];

        String[] parts = value.split(",");
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim();
        }
        return parts;
    }
}
