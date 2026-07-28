package net.kdt.pojavlaunch.minethor;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import net.kdt.pojavlaunch.Tools;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class MineThorItemIconResolver {
    private static final String TAG = "MineThorIcons";
    private static final int MAX_MODEL_DEPTH = 8;
    private static final Map<String, Bitmap> ICON_CACHE = new HashMap<>();
    private static String activeVersionId = "";

    private MineThorItemIconResolver() {
    }

    public static synchronized void setActiveVersion(String versionId) {
        if (versionId == null || versionId.equals(activeVersionId)) return;

        activeVersionId = versionId;
        ICON_CACHE.clear();
    }

    public static synchronized Bitmap iconFor(String itemId) {
        if (itemId == null || itemId.isEmpty() || activeVersionId.isEmpty()) return null;
        if (ICON_CACHE.containsKey(itemId)) return ICON_CACHE.get(itemId);

        Bitmap icon = loadIcon(itemId);
        ICON_CACHE.put(itemId, icon);
        return icon;
    }

    private static Bitmap loadIcon(String itemId) {
        File clientJar = new File(Tools.DIR_HOME_VERSION, activeVersionId + File.separator + activeVersionId + ".jar");
        if (!clientJar.isFile()) return null;

        try (ZipFile jar = new ZipFile(clientJar)) {
            ResourceId modelId = ResourceId.parse(itemId);
            JSONObject textures = new JSONObject();
            JSONObject model = readModel(jar, modelId, textures, 0);
            if (model == null) return null;

            String texture = resolveTexture(textures, "layer0");
            if (texture.isEmpty()) return null;

            return readTexture(jar, ResourceId.parse(texture));
        } catch (IOException | JSONException | RuntimeException e) {
            Log.d(TAG, "Cannot load icon for " + itemId, e);
            return null;
        }
    }

    private static JSONObject readModel(ZipFile jar, ResourceId modelId, JSONObject mergedTextures, int depth) throws IOException, JSONException {
        if (depth > MAX_MODEL_DEPTH) return null;

        JSONObject model = readJson(jar, modelId.modelPath());
        if (model == null) return null;

        String parent = model.optString("parent", "");
        if (!parent.isEmpty() && !parent.startsWith("builtin/")) {
            ResourceId parentId = ResourceId.parse(parent, modelId.namespace);
            readModel(jar, parentId, mergedTextures, depth + 1);
        }

        JSONObject textures = model.optJSONObject("textures");
        if (textures != null) {
            Iterator<String> keys = textures.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                mergedTextures.put(key, textures.optString(key, ""));
            }
        }
        return model;
    }

    private static String resolveTexture(JSONObject textures, String key) {
        String value = textures.optString(key, "");
        for (int i = 0; i < MAX_MODEL_DEPTH && value.startsWith("#"); i++) {
            value = textures.optString(value.substring(1), "");
        }
        return value.startsWith("#") ? "" : value;
    }

    private static JSONObject readJson(ZipFile jar, String path) throws IOException, JSONException {
        ZipEntry entry = jar.getEntry(path);
        if (entry == null) return null;

        try (InputStream input = jar.getInputStream(entry)) {
            return new JSONObject(new String(readAllBytes(input), "UTF-8"));
        }
    }

    private static Bitmap readTexture(ZipFile jar, ResourceId textureId) throws IOException {
        ZipEntry entry = jar.getEntry(textureId.texturePath());
        if (entry == null) return null;

        try (InputStream input = jar.getInputStream(entry)) {
            return BitmapFactory.decodeStream(input);
        }
    }

    private static byte[] readAllBytes(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) >= 0) {
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private static final class ResourceId {
        final String namespace;
        final String path;

        ResourceId(String namespace, String path) {
            this.namespace = namespace;
            this.path = path;
        }

        static ResourceId parse(String value) {
            return parse(value, "minecraft");
        }

        static ResourceId parse(String value, String defaultNamespace) {
            int separator = value.indexOf(':');
            if (separator < 0) return new ResourceId(defaultNamespace, value);

            return new ResourceId(value.substring(0, separator), value.substring(separator + 1));
        }

        String modelPath() {
            return "assets/" + namespace + "/models/item/" + path + ".json";
        }

        String texturePath() {
            return "assets/" + namespace + "/textures/" + path + ".png";
        }
    }
}
