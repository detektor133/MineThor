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
    private static final Map<String, Boolean> MISS_CACHE = new HashMap<>();
    private static String activeVersionId = "";

    private MineThorItemIconResolver() {
    }

    public static synchronized void setActiveVersion(String versionId) {
        if (versionId == null || versionId.equals(activeVersionId)) return;

        activeVersionId = versionId;
        ICON_CACHE.clear();
        MISS_CACHE.clear();
    }

    public static synchronized Bitmap iconFor(String itemId) {
        if (itemId == null || itemId.isEmpty() || activeVersionId.isEmpty()) return null;
        if (ICON_CACHE.containsKey(itemId)) return ICON_CACHE.get(itemId);
        if (MISS_CACHE.containsKey(itemId)) return null;

        Bitmap icon = loadIcon(itemId);
        if (icon == null) {
            MISS_CACHE.put(itemId, true);
        } else {
            ICON_CACHE.put(itemId, icon);
        }
        return icon;
    }

    private static Bitmap loadIcon(String itemId) {
        File clientJar = new File(Tools.DIR_HOME_VERSION, activeVersionId + File.separator + activeVersionId + ".jar");
        if (!clientJar.isFile()) {
            Log.d(TAG, "Client jar is missing for " + itemId + ": " + clientJar.getAbsolutePath());
            return null;
        }

        try (ZipFile jar = new ZipFile(clientJar)) {
            ResourceId modelId = ResourceId.parse(itemId);
            String texture = resolveModelTexture(jar, modelId);
            if (texture.isEmpty()) {
                Log.d(TAG, "Texture is missing for " + itemId + " in " + modelId.modelPath());
                return null;
            }

            Bitmap bitmap = readTexture(jar, ResourceId.parse(texture));
            if (bitmap == null) Log.d(TAG, "Texture png is missing for " + itemId + ": " + ResourceId.parse(texture).texturePath());
            return bitmap;
        } catch (IOException | JSONException | RuntimeException e) {
            Log.d(TAG, "Cannot load icon for " + itemId, e);
            return null;
        }
    }

    private static String resolveModelTexture(ZipFile jar, ResourceId modelId) throws IOException, JSONException {
        ModelData model = readModel(jar, modelId, 0);
        if (model == null) return "";

        String layer = resolveTexture(model.textures, "layer0");
        if (!layer.isEmpty()) return layer;
        String particle = resolveTexture(model.textures, "particle");
        if (!particle.isEmpty()) return particle;

        return firstPresentTexture(model.textures, "all", "side", "top", "front", "end");
    }

    private static ModelData readModel(ZipFile jar, ResourceId modelId, int depth) throws IOException, JSONException {
        if (depth > MAX_MODEL_DEPTH) return null;

        JSONObject model = readJson(jar, modelId.modelPath());
        if (model == null) {
            Log.d(TAG, "Model json is missing: " + modelId.modelPath());
            return null;
        }

        JSONObject mergedTextures = new JSONObject();

        String parent = model.optString("parent", "");
        if (!parent.isEmpty() && !parent.startsWith("builtin/")) {
            ResourceId parentId = ResourceId.parse(parent, modelId.namespace);
            ModelData parentData = readModel(jar, parentId, depth + 1);
            if (parentData != null) copyTextures(parentData.textures, mergedTextures);
        }

        JSONObject textures = model.optJSONObject("textures");
        if (textures != null) copyTextures(textures, mergedTextures);
        return new ModelData(mergedTextures);
    }

    private static String resolveTexture(JSONObject textures, String key) {
        String value = textures.optString(key, "");
        for (int i = 0; i < MAX_MODEL_DEPTH && value.startsWith("#"); i++) {
            value = textures.optString(value.substring(1), "");
        }
        return value.startsWith("#") ? "" : value;
    }

    private static String firstPresentTexture(JSONObject textures, String... keys) {
        for (String key : keys) {
            String value = resolveTexture(textures, key);
            if (!value.isEmpty()) return value;
        }
        return "";
    }

    private static void copyTextures(JSONObject source, JSONObject target) throws JSONException {
        Iterator<String> keys = source.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            target.put(key, source.optString(key, ""));
        }
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
            if (path.startsWith("item/") || path.startsWith("block/")) {
                return "assets/" + namespace + "/models/" + path + ".json";
            }
            return "assets/" + namespace + "/models/item/" + path + ".json";
        }

        String texturePath() {
            return "assets/" + namespace + "/textures/" + path + ".png";
        }
    }

    private static final class ModelData {
        final JSONObject textures;

        ModelData(JSONObject textures) {
            this.textures = textures;
        }
    }
}
