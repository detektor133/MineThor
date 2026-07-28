package net.kdt.pojavlaunch.minethor;

import android.util.Log;

import net.kdt.pojavlaunch.JMinecraftVersionList;
import net.kdt.pojavlaunch.PojavApplication;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.mirrors.DownloadMirror;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.utils.DownloadUtils;
import net.kdt.pojavlaunch.utils.FileUtils;
import net.kdt.pojavlaunch.value.MinecraftClientInfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public final class MineThorMappingResolver {
    private static final String TAG = "MineThorMapping";
    private static final String DOWNLOAD_CLIENT_MAPPINGS = "client_mappings";
    private static final String DESCRIPTOR_NAME = "descriptor.properties";
    private static final String DESCRIPTOR_SCHEMA_VERSION = "2";

    private MineThorMappingResolver() {
    }

    public static String resolveDescriptorForLaunch(String versionId, JMinecraftVersionList.Version versionInfo) {
        try {
            File descriptor = resolveDescriptor(versionId, versionInfo);
            return descriptor == null ? null : descriptor.getAbsolutePath();
        } catch (IOException e) {
            Log.i(TAG, "Cannot resolve mapping descriptor for " + versionId, e);
            return null;
        }
    }

    public static void backfillInstalledVersions() {
        PojavApplication.sExecutorService.execute(() -> {
            File versionsDir = new File(Tools.DIR_HOME_VERSION);
            File[] versionDirs = versionsDir.listFiles(File::isDirectory);
            if (versionDirs == null) return;

            for (File versionDir : versionDirs) {
                String versionId = versionDir.getName();
                File versionJson = new File(versionDir, versionId + ".json");
                if (!versionJson.isFile()) continue;

                try {
                    JMinecraftVersionList.Version versionInfo = Tools.GLOBAL_GSON.fromJson(Tools.read(versionJson), JMinecraftVersionList.Version.class);
                    resolveDescriptor(versionId, versionInfo);
                } catch (IOException | RuntimeException e) {
                    Log.i(TAG, "Cannot backfill mapping descriptor for " + versionId, e);
                }
            }
        });
    }

    private static File resolveDescriptor(String versionId, JMinecraftVersionList.Version versionInfo) throws IOException {
        JMinecraftVersionList.Version mappingVersion = mappingVersion(versionId, versionInfo);
        if (mappingVersion == null || mappingVersion.downloads == null) return null;

        MinecraftClientInfo mappingsInfo = mappingVersion.downloads.get(DOWNLOAD_CLIENT_MAPPINGS);
        if (mappingsInfo == null || !Tools.isValidString(mappingsInfo.url)) return null;

        File cacheDir = mappingCacheDir(mappingVersion.id == null ? versionId : mappingVersion.id);
        File descriptorFile = new File(cacheDir, DESCRIPTOR_NAME);
        if (isCachedDescriptorCurrent(descriptorFile)) return descriptorFile;

        File mappingsFile = new File(cacheDir, "client.txt");
        FileUtils.ensureParentDirectory(mappingsFile);
        DownloadUtils.ensureSha1(mappingsFile, LauncherPreferences.PREF_VERIFY_MANIFEST ? mappingsInfo.sha1 : null, () -> {
            DownloadMirror.downloadFileMirrored(DownloadMirror.DOWNLOAD_CLASS_METADATA, mappingsInfo.url, mappingsFile);
            return null;
        });

        MappingIndex index = MappingIndex.read(mappingsFile);
        Properties descriptor = buildDescriptor(mappingVersion.id == null ? versionId : mappingVersion.id, index);
        if (descriptor == null) return null;

        FileUtils.ensureParentDirectory(descriptorFile);
        try (FileWriter writer = new FileWriter(descriptorFile)) {
            descriptor.store(writer, "MineThor generated mapping descriptor");
        }
        Log.i(TAG, "Generated mapping descriptor for " + versionId + ": " + descriptorFile.getAbsolutePath());
        return descriptorFile;
    }

    private static JMinecraftVersionList.Version mappingVersion(String versionId, JMinecraftVersionList.Version versionInfo) throws IOException {
        if (hasClientMappings(versionInfo)) return versionInfo;
        if (versionInfo != null && Tools.isValidString(versionInfo.inheritsFrom)) {
            return mappingVersion(versionInfo.inheritsFrom, readInstalledVersion(versionInfo.inheritsFrom));
        }
        return readInstalledVersion(versionId);
    }

    private static boolean hasClientMappings(JMinecraftVersionList.Version versionInfo) {
        return versionInfo != null && versionInfo.downloads != null && versionInfo.downloads.get(DOWNLOAD_CLIENT_MAPPINGS) != null;
    }

    private static JMinecraftVersionList.Version readInstalledVersion(String versionId) throws IOException {
        File versionJson = new File(Tools.DIR_HOME_VERSION, versionId + File.separator + versionId + ".json");
        if (!versionJson.isFile()) return null;
        return Tools.GLOBAL_GSON.fromJson(Tools.read(versionJson), JMinecraftVersionList.Version.class);
    }

    private static File mappingCacheDir(String versionId) {
        return new File(Tools.DIR_DATA, "minethor" + File.separator + "mappings" + File.separator + versionId);
    }

    private static boolean isCachedDescriptorCurrent(File descriptorFile) {
        if (!descriptorFile.isFile() || !descriptorFile.canRead()) return false;

        Properties descriptor = new Properties();
        try (FileReader reader = new FileReader(descriptorFile)) {
            descriptor.load(reader);
            return DESCRIPTOR_SCHEMA_VERSION.equals(descriptor.getProperty("schema"))
                    && requiredDescriptorValuesPresent(descriptor);
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }

    private static Properties buildDescriptor(String versionId, MappingIndex index) {
        String clientClass = index.className("net.minecraft.client.Minecraft");
        String playerClass = index.className("net.minecraft.world.entity.player.Player");
        String entityClass = index.className("net.minecraft.world.entity.Entity");
        String livingEntityClass = index.className("net.minecraft.world.entity.LivingEntity");
        String inventoryClass = index.className("net.minecraft.world.entity.player.Inventory");

        if (!allPresent(clientClass, playerClass, entityClass, livingEntityClass, inventoryClass)) {
            Log.i(TAG, "Mappings are missing required classes for " + versionId);
            return null;
        }

        Properties descriptor = new Properties();
        descriptor.setProperty("schema", DESCRIPTOR_SCHEMA_VERSION);
        descriptor.setProperty("name", "mojang-" + versionId);
        descriptor.setProperty("client.classes", clientClass);
        descriptor.setProperty("client.instanceMethods", index.methodName("net.minecraft.client.Minecraft", "getInstance"));
        descriptor.setProperty("player.fields", index.fieldName("net.minecraft.client.Minecraft", "player"));
        descriptor.setProperty("inventory.methods", index.methodName("net.minecraft.world.entity.player.Player", "getInventory"));
        descriptor.setProperty("inventory.fields", index.fieldName("net.minecraft.world.entity.player.Player", "inventory"));
        descriptor.setProperty("inventory.classNames", inventoryClass);
        descriptor.setProperty("selectedSlot.fields", index.fieldName("net.minecraft.world.entity.player.Inventory", "selected"));
        descriptor.setProperty("x.accessors", index.methodName("net.minecraft.world.entity.Entity", "getX"));
        descriptor.setProperty("y.accessors", index.methodName("net.minecraft.world.entity.Entity", "getY"));
        descriptor.setProperty("z.accessors", index.methodName("net.minecraft.world.entity.Entity", "getZ"));
        descriptor.setProperty("yaw.accessors", index.methodName("net.minecraft.world.entity.Entity", "getYRot"));
        descriptor.setProperty("health.accessors", index.methodName("net.minecraft.world.entity.LivingEntity", "getHealth"));
        descriptor.setProperty("maxHealth.accessors", index.methodName("net.minecraft.world.entity.LivingEntity", "getMaxHealth"));
        descriptor.setProperty("armor.accessors", index.methodName("net.minecraft.world.entity.LivingEntity", "getArmorValue"));
        descriptor.setProperty("foodData.accessors", index.methodName("net.minecraft.world.entity.player.Player", "getFoodData"));
        descriptor.setProperty("food.accessors", index.methodName("net.minecraft.world.food.FoodData", "getFoodLevel"));
        descriptor.setProperty("xpLevel.fields", index.fieldName("net.minecraft.world.entity.player.Player", "experienceLevel"));

        if (!requiredDescriptorValuesPresent(descriptor)) {
            Log.i(TAG, "Generated descriptor is incomplete for " + versionId);
            return null;
        }
        return descriptor;
    }

    private static boolean requiredDescriptorValuesPresent(Properties descriptor) {
        return allPresent(
                descriptor.getProperty("client.classes"),
                descriptor.getProperty("client.instanceMethods"),
                descriptor.getProperty("player.fields"),
                descriptor.getProperty("inventory.classNames"),
                descriptor.getProperty("selectedSlot.fields"),
                descriptor.getProperty("x.accessors"),
                descriptor.getProperty("y.accessors"),
                descriptor.getProperty("z.accessors"),
                descriptor.getProperty("yaw.accessors"),
                descriptor.getProperty("health.accessors"),
                descriptor.getProperty("maxHealth.accessors"),
                descriptor.getProperty("armor.accessors")
        );
    }

    private static boolean allPresent(String... values) {
        for (String value : values) {
            if (!Tools.isValidString(value)) return false;
        }
        return true;
    }

    private static final class MappingIndex {
        private final Map<String, String> classes = new HashMap<>();
        private final Map<String, Map<String, String>> fields = new HashMap<>();
        private final Map<String, Map<String, String>> methods = new HashMap<>();

        static MappingIndex read(File mappingsFile) throws IOException {
            MappingIndex index = new MappingIndex();
            String currentClass = null;

            try (BufferedReader reader = new BufferedReader(new FileReader(mappingsFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.contains(" -> ")) continue;

                    if (!Character.isWhitespace(line.charAt(0))) {
                        currentClass = index.readClass(line);
                        continue;
                    }
                    if (currentClass == null) continue;

                    index.readMember(currentClass, line.trim());
                }
            }
            return index;
        }

        String className(String officialName) {
            return classes.get(officialName);
        }

        String fieldName(String officialClass, String officialName) {
            Map<String, String> classFields = fields.get(officialClass);
            String name = classFields == null ? null : classFields.get(officialName);
            return name == null ? "" : name;
        }

        String methodName(String officialClass, String officialName) {
            Map<String, String> classMethods = methods.get(officialClass);
            String name = classMethods == null ? null : classMethods.get(methodKey(officialName, 0));
            return name == null ? "" : name;
        }

        private String readClass(String line) {
            int arrow = line.indexOf(" -> ");
            String officialName = line.substring(0, arrow).trim();
            String obfuscatedName = line.substring(arrow + 4).replace(":", "").trim();
            classes.put(officialName, obfuscatedName);
            return officialName;
        }

        private void readMember(String currentClass, String line) {
            int arrow = line.indexOf(" -> ");
            if (arrow < 0) return;

            String left = stripLineNumbers(line.substring(0, arrow).trim());
            String obfuscatedName = line.substring(arrow + 4).trim();
            if (left.contains("(")) {
                String officialName = methodNameFromLeftSide(left);
                int arity = methodArityFromLeftSide(left);
                if (Tools.isValidString(officialName)) {
                    methods.computeIfAbsent(currentClass, ignored -> new HashMap<>()).put(methodKey(officialName, arity), obfuscatedName);
                }
                return;
            }

            String officialName = fieldNameFromLeftSide(left);
            if (Tools.isValidString(officialName)) {
                fields.computeIfAbsent(currentClass, ignored -> new HashMap<>()).put(officialName, obfuscatedName);
            }
        }

        private static String stripLineNumbers(String value) {
            int firstColon = value.indexOf(':');
            if (firstColon < 0) return value;

            int secondColon = value.indexOf(':', firstColon + 1);
            if (secondColon < 0) return value;
            return value.substring(secondColon + 1).trim();
        }

        private static String methodNameFromLeftSide(String value) {
            int argsStart = value.indexOf('(');
            if (argsStart <= 0) return "";

            int nameStart = value.lastIndexOf(' ', argsStart);
            if (nameStart < 0 || nameStart + 1 >= argsStart) return "";
            return value.substring(nameStart + 1, argsStart);
        }

        private static int methodArityFromLeftSide(String value) {
            int argsStart = value.indexOf('(');
            int argsEnd = value.indexOf(')', argsStart + 1);
            if (argsStart < 0 || argsEnd < 0 || argsEnd <= argsStart + 1) return 0;

            int arity = 1;
            for (int i = argsStart + 1; i < argsEnd; i++) {
                if (value.charAt(i) == ',') arity++;
            }
            return arity;
        }

        private static String methodKey(String officialName, int arity) {
            return officialName + "#" + arity;
        }

        private static String fieldNameFromLeftSide(String value) {
            int nameStart = value.lastIndexOf(' ');
            if (nameStart < 0 || nameStart + 1 >= value.length()) return "";
            return value.substring(nameStart + 1);
        }
    }
}
