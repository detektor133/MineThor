package net.kdt.pojavlaunch.minethor;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
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
        boolean shouldRequest = iconKey != null && !iconKey.isEmpty() && !ICONS.containsKey(iconKey) && REQUESTED_KEYS.add(iconKey);
        if (shouldRequest) Log.d(TAG, "Request icon " + iconKey);
        return shouldRequest;
    }

    public static void logDataStatus(String iconKey, String status, int byteCount) {
        Log.d(TAG, "Icon data " + iconKey + " status=" + status + " bytes=" + byteCount);
    }

    public static synchronized void put(String iconKey, String pngBase64) {
        if (iconKey == null || iconKey.isEmpty() || pngBase64 == null || pngBase64.isEmpty()) {
            Log.d(TAG, "Empty icon data for " + iconKey);
            return;
        }

        try {
            byte[] bytes = Base64.decode(pngBase64, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            if (bitmap != null) {
                ICONS.put(iconKey, trimTransparentBounds(removeEdgeBlackBackground(bitmap)));
                Log.d(TAG, "Stored icon " + iconKey + " " + bitmap.getWidth() + "x" + bitmap.getHeight());
            } else {
                Log.d(TAG, "Decoded icon is null " + iconKey + " bytes=" + bytes.length);
            }
        } catch (RuntimeException e) {
            Log.d(TAG, "Cannot decode icon " + iconKey, e);
        }
    }

    private static Bitmap removeEdgeBlackBackground(Bitmap source) {
        Bitmap bitmap = source.copy(Bitmap.Config.ARGB_8888, true);
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        boolean[] visited = new boolean[width * height];
        int[] queue = new int[width * height];
        int head = 0;
        int tail = 0;

        for (int x = 0; x < width; x++) {
            tail = enqueueIfBlack(bitmap, visited, queue, tail, x, 0);
            tail = enqueueIfBlack(bitmap, visited, queue, tail, x, height - 1);
        }
        for (int y = 1; y < height - 1; y++) {
            tail = enqueueIfBlack(bitmap, visited, queue, tail, 0, y);
            tail = enqueueIfBlack(bitmap, visited, queue, tail, width - 1, y);
        }

        while (head < tail) {
            int packed = queue[head++];
            int x = packed % width;
            int y = packed / width;
            bitmap.setPixel(x, y, Color.TRANSPARENT);

            if (x > 0) tail = enqueueIfBlack(bitmap, visited, queue, tail, x - 1, y);
            if (x + 1 < width) tail = enqueueIfBlack(bitmap, visited, queue, tail, x + 1, y);
            if (y > 0) tail = enqueueIfBlack(bitmap, visited, queue, tail, x, y - 1);
            if (y + 1 < height) tail = enqueueIfBlack(bitmap, visited, queue, tail, x, y + 1);
        }

        return bitmap;
    }

    private static Bitmap trimTransparentBounds(Bitmap source) {
        int left = source.getWidth();
        int top = source.getHeight();
        int right = -1;
        int bottom = -1;

        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                if (Color.alpha(source.getPixel(x, y)) == 0) continue;
                if (x < left) left = x;
                if (y < top) top = y;
                if (x > right) right = x;
                if (y > bottom) bottom = y;
            }
        }

        if (right < left || bottom < top) return source;
        return Bitmap.createBitmap(source, left, top, right - left + 1, bottom - top + 1);
    }

    private static int enqueueIfBlack(Bitmap bitmap, boolean[] visited, int[] queue, int tail, int x, int y) {
        int width = bitmap.getWidth();
        int index = y * width + x;
        if (visited[index]) return tail;

        int color = bitmap.getPixel(x, y);
        if (Color.alpha(color) == 0 || Color.red(color) > 32 || Color.green(color) > 32 || Color.blue(color) > 32) {
            return tail;
        }

        visited[index] = true;
        queue[tail] = index;
        return tail + 1;
    }
}
