package net.kdt.pojavlaunch.minethor;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class MineThorIconCache {
    private static final String TAG = "MineThorIcons";
    private static final Map<String, Bitmap> ICONS = new HashMap<>();
    private static final Set<String> REQUESTED_KEYS = new HashSet<>();

    private MineThorIconCache() {
    }

    public static synchronized Bitmap icon(String iconKey) {
        return ICONS.get(iconKey);
    }

    public static synchronized boolean shouldRequest(String iconKey) {
        return iconKey != null && !iconKey.isEmpty() && !ICONS.containsKey(iconKey) && REQUESTED_KEYS.add(iconKey);
    }

    public static synchronized void put(String iconKey, String pngBase64) {
        if (iconKey == null || iconKey.isEmpty() || pngBase64 == null || pngBase64.isEmpty()) return;

        try {
            byte[] bytes = Base64.decode(pngBase64, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            if (bitmap != null) ICONS.put(iconKey, bitmap);
        } catch (RuntimeException e) {
            Log.d(TAG, "Cannot decode icon " + iconKey, e);
        }
    }
}
