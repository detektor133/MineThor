package net.kdt.pojavlaunch.minethor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Properties;

/**
 * Pure JVM tests for the identity-mapping path: no network, no Android (this file deliberately
 * stays clear of anything touching Tools.DIR_DATA, which only a real app context sets - the
 * file-caching wrapper around this logic is covered by a smoke test on device, see docs/TESTING.md).
 *
 * The path under test is why MineThor can work on a future unobfuscated Minecraft (26.1+, see
 * docs/COMPATIBILITY.md) with zero dictionary and zero network cost.
 */
public class MineThorMappingResolverTest {
    private static Object identityIndexInstance() throws ReflectiveOperationException {
        Class<?> identityIndexClass = Class.forName("net.kdt.pojavlaunch.minethor.MineThorMappingResolver$IdentityIndex");
        return identityIndexClass.getDeclaredField("INSTANCE").get(null);
    }

    private static Properties buildIdentityDescriptor(String name) throws ReflectiveOperationException {
        Class<?> nameIndexInterface = Class.forName("net.kdt.pojavlaunch.minethor.MineThorMappingResolver$NameIndex");
        Method buildDescriptor = MineThorMappingResolver.class.getDeclaredMethod("buildDescriptor", String.class, nameIndexInterface);
        buildDescriptor.setAccessible(true);
        return (Properties) buildDescriptor.invoke(null, name, identityIndexInstance());
    }

    @Test
    public void identityIndexReturnsEveryQueryUnchanged() throws Exception {
        Object identity = identityIndexInstance();
        Class<?> type = identity.getClass();

        Method className = type.getMethod("className", String.class);
        Method fieldName = type.getMethod("fieldName", String.class, String.class);
        Method methodName2 = type.getMethod("methodName", String.class, String.class);
        Method methodName3 = type.getMethod("methodName", String.class, String.class, int.class);

        assertEquals("net.minecraft.client.Minecraft", className.invoke(identity, "net.minecraft.client.Minecraft"));
        assertEquals("player", fieldName.invoke(identity, "net.minecraft.client.Minecraft", "player"));
        assertEquals("getInstance", methodName2.invoke(identity, "net.minecraft.client.Minecraft", "getInstance"));
        assertEquals("getX", methodName3.invoke(identity, "net.minecraft.world.entity.Entity", "getX", 0));
    }

    @Test
    public void buildDescriptorWithIdentityIndexProducesACompleteUsableDescriptor() throws Exception {
        Properties descriptor = buildIdentityDescriptor("identity-26.1-test");

        assertNotNull("the identity path must not degrade to null just because there is no dictionary", descriptor);
        assertEquals("7", descriptor.getProperty("schema"));
        assertEquals("identity-26.1-test", descriptor.getProperty("name"));
        // Every value is the canonical name itself: no translation happened.
        assertEquals("net.minecraft.client.Minecraft", descriptor.getProperty("client.classes"));
        assertEquals("getInstance", descriptor.getProperty("client.instanceMethods"));
        assertEquals("player", descriptor.getProperty("player.fields"));
        assertEquals("net.minecraft.client.gui.GuiGraphics", descriptor.getProperty("guiGraphics.classes"));
        assertEquals("getHealth", descriptor.getProperty("health.accessors"));
    }

    @Test
    public void mojangNamespacedDescriptorIsTaggedDifferentlyFromIdentity() throws Exception {
        // Regression guard for the rename done alongside the identity path: a real Mojang-mapped
        // descriptor's "name" property must stay distinguishable from an identity one, since
        // docs/TESTING.md's conformance harness and support requests both read this field.
        Class<?> nameIndexInterface = Class.forName("net.kdt.pojavlaunch.minethor.MineThorMappingResolver$NameIndex");
        Method buildDescriptor = MineThorMappingResolver.class.getDeclaredMethod("buildDescriptor", String.class, nameIndexInterface);
        buildDescriptor.setAccessible(true);
        Properties mojang = (Properties) buildDescriptor.invoke(null, "mojang-1.20.1", identityIndexInstance());
        Properties identity = buildIdentityDescriptor("identity-1.20.1");

        assertTrue(mojang.getProperty("name").startsWith("mojang-"));
        assertTrue(identity.getProperty("name").startsWith("identity-"));
    }
}
