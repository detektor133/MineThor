package net.kdt.pojavlaunch.minethor;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import net.kdt.pojavlaunch.Tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class MineThorMinecraftAssets {
    private static final String INVENTORY_TEXTURE = "assets/minecraft/textures/gui/container/inventory.png";
    private static final String WIDGETS_TEXTURE = "assets/minecraft/textures/gui/widgets.png";
    private static final String ASCII_FONT_TEXTURE = "assets/minecraft/textures/font/ascii.png";
    private static Bitmap inventoryTexture;
    private static Bitmap widgetsTexture;
    private static Bitmap asciiFontTexture;
    private static final Map<String, Bitmap> TEXTURES = new HashMap<>();
    private static String activeVersionId;

    private MineThorMinecraftAssets() {
    }

    /**
     * Binds asset lookups to the version being launched, invalidating any textures
     * cached for a previously launched version. Must be called before/at launch,
     * since {@link #loadTexture} otherwise has no way to know which of the
     * potentially many installed versions is actually running.
     */
    public static synchronized void setActiveVersion(String versionId) {
        if (versionId == null || versionId.equals(activeVersionId)) return;
        activeVersionId = versionId;
        inventoryTexture = null;
        widgetsTexture = null;
        asciiFontTexture = null;
        TEXTURES.clear();
    }

    public static synchronized Bitmap inventoryTexture() {
        if (inventoryTexture == null) inventoryTexture = loadTexture(INVENTORY_TEXTURE);
        return inventoryTexture;
    }

    public static synchronized Bitmap widgetsTexture() {
        if (widgetsTexture == null) widgetsTexture = loadTexture(WIDGETS_TEXTURE);
        return widgetsTexture;
    }

    public static synchronized Bitmap asciiFontTexture() {
        if (asciiFontTexture == null) asciiFontTexture = loadTexture(ASCII_FONT_TEXTURE);
        return asciiFontTexture;
    }

    public static synchronized Bitmap texture(String path) {
        if (!TEXTURES.containsKey(path)) TEXTURES.put(path, loadTexture(path));
        return TEXTURES.get(path);
    }

    private static Bitmap loadTexture(String path) {
        Bitmap resourcePackBitmap = loadFromResourcePacks(path);
        if (resourcePackBitmap != null) return resourcePackBitmap;

        if (activeVersionId != null) {
            File versionDir = new File(Tools.DIR_HOME_VERSION, activeVersionId);
            File jar = new File(versionDir, activeVersionId + ".jar");
            Bitmap bitmap = loadFromJar(jar, path);
            if (bitmap != null) return bitmap;
        }

        // Fall back to scanning installed versions only if we don't know (or
        // couldn't resolve from) the version actually being launched.
        File versionsDir = new File(Tools.DIR_HOME_VERSION);
        File[] versionDirs = versionsDir.listFiles(File::isDirectory);
        if (versionDirs == null) return null;

        java.util.Arrays.sort(versionDirs, Comparator.comparingLong(File::lastModified).reversed());
        for (File versionDir : versionDirs) {
            File jar = new File(versionDir, versionDir.getName() + ".jar");
            Bitmap bitmap = loadFromJar(jar, path);
            if (bitmap != null) return bitmap;
        }
        return null;
    }

    private static Bitmap loadFromResourcePacks(String path) {
        List<File> packs = activeResourcePacks();
        for (File pack : packs) {
            Bitmap bitmap = pack.isDirectory() ? loadFromDirectory(pack, path) : loadFromJar(pack, path);
            if (bitmap != null) return bitmap;
        }
        return null;
    }

    private static List<File> activeResourcePacks() {
        List<File> packs = new ArrayList<>();
        File options = new File(Tools.DIR_GAME_NEW, "options.txt");
        File packsDir = new File(Tools.DIR_GAME_NEW, "resourcepacks");
        if (!options.isFile() || !packsDir.isDirectory()) return packs;

        List<String> names = readResourcePackNames(options);
        for (int i = names.size() - 1; i >= 0; i--) {
            String name = normalizedPackName(names.get(i));
            if (name == null) continue;

            File pack = new File(packsDir, name);
            if (pack.exists()) packs.add(pack);
        }
        return packs;
    }

    private static List<String> readResourcePackNames(File options) {
        List<String> names = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(options))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("resourcePacks:")) continue;
                collectQuotedValues(line.substring("resourcePacks:".length()), names);
                break;
            }
        } catch (IOException ignored) {
            return names;
        }
        return names;
    }

    private static void collectQuotedValues(String value, List<String> target) {
        StringBuilder current = null;
        boolean escaping = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (current == null) {
                if (c == '"') current = new StringBuilder();
                continue;
            }
            if (escaping) {
                current.append(c);
                escaping = false;
            } else if (c == '\\') {
                escaping = true;
            } else if (c == '"') {
                target.add(current.toString());
                current = null;
            } else {
                current.append(c);
            }
        }
    }

    private static String normalizedPackName(String value) {
        if (value == null || value.equals("vanilla") || value.equals("programmer_art")) return null;
        return value.startsWith("file/") ? value.substring("file/".length()) : value;
    }

    private static Bitmap loadFromDirectory(File directory, String path) {
        File file = new File(directory, path);
        if (!file.isFile()) return null;

        try (InputStream input = new FileInputStream(file)) {
            return BitmapFactory.decodeStream(input);
        } catch (IOException | RuntimeException ignored) {
            return null;
        }
    }

    private static Bitmap loadFromJar(File jar, String path) {
        if (!jar.isFile()) return null;

        try (ZipFile zip = new ZipFile(jar)) {
            ZipEntry entry = zip.getEntry(path);
            if (entry == null) return null;

            try (InputStream input = zip.getInputStream(entry)) {
                return BitmapFactory.decodeStream(input);
            }
        } catch (IOException | RuntimeException ignored) {
            return null;
        }
    }
}
