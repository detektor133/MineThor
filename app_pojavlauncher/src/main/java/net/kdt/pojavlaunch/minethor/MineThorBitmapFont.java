package net.kdt.pojavlaunch.minethor;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

public final class MineThorBitmapFont {
    private static final int GLYPH_COLUMNS = 16;
    private static final int GLYPH_SIZE = 8;
    private static final Paint FONT_PAINT = new Paint();

    static {
        FONT_PAINT.setAntiAlias(false);
        FONT_PAINT.setFilterBitmap(false);
        FONT_PAINT.setDither(false);
    }

    private MineThorBitmapFont() {
    }

    public static float width(String text, float scale) {
        if (text == null || text.isEmpty()) return 0f;
        return text.length() * GLYPH_SIZE * scale;
    }

    public static void draw(Canvas canvas, String text, float x, float y, float scale) {
        if (text == null || text.isEmpty()) return;

        Bitmap font = MineThorMinecraftAssets.asciiFontTexture();
        if (font == null) return;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            int index = c >= 32 && c < 128 ? c : '?';
            int column = index % GLYPH_COLUMNS;
            int row = index / GLYPH_COLUMNS;
            Rect src = new Rect(column * GLYPH_SIZE, row * GLYPH_SIZE, (column + 1) * GLYPH_SIZE, (row + 1) * GLYPH_SIZE);
            float left = x + i * GLYPH_SIZE * scale;
            RectF dst = new RectF(left, y, left + GLYPH_SIZE * scale, y + GLYPH_SIZE * scale);
            canvas.drawBitmap(font, src, dst, FONT_PAINT);
        }
    }
}
