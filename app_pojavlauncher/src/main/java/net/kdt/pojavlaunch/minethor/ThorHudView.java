package net.kdt.pojavlaunch.minethor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

public class ThorHudView extends View {
    private static final int SLOT_COUNT = 9;
    private static final int INVENTORY_COLUMNS = 9;
    private static final int INVENTORY_ROWS = 3;
    private static final int SCREEN_INVENTORY = 0;
    private static final int SCREEN_MAP = 1;
    private static final String GUI_ICONS = "assets/minecraft/textures/gui/icons.png";

    private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint panelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint slotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint slotLightPaint = new Paint();
    private final Paint slotDarkPaint = new Paint();
    private final Paint slotSelectedPaint = new Paint();
    private final Paint iconPaint = new Paint();
    private final Paint statPaint = new Paint();
    private final Paint xpPaint = new Paint();
    private final Paint smallTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint touchPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ThorCompanionController controller;
    private int activeScreen = SCREEN_INVENTORY;
    private int pointerCount;

    public ThorHudView(Context context, ThorCompanionController controller) {
        super(context);
        this.controller = controller;
        init();
        this.controller.setListener(this::postInvalidate);
    }

    public ThorHudView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.controller = new ThorCompanionController(new SocketCompanionStateProvider(CompanionEndpoint.LOOPBACK_HOST, CompanionEndpoint.PORT));
        init();
        this.controller.setListener(this::postInvalidate);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        pointerCount = event.getPointerCount();
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            if (!updateScreenMode(event.getX(), event.getY())) updateSelection(event.getX(), event.getY());
        }
        invalidate();
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(0f, 0f, getWidth(), getHeight(), backgroundPaint);

        RectF content = contentBounds();
        if (activeScreen == SCREEN_MAP) {
            drawMapScreen(canvas, content);
        } else {
            drawInventoryScreen(canvas, content);
        }
        drawModeButtons(canvas, content);

        if (pointerCount > 1) {
            canvas.drawCircle(getWidth() - 56f, 56f, 22f, touchPaint);
        }
    }

    private RectF contentBounds() {
        float margin = Math.max(24f, getWidth() * 0.025f);
        return new RectF(margin, margin, getWidth() - margin, getHeight() - margin);
    }

    private RectF inventoryPanelBounds(RectF content) {
        float availableTop = content.top + modeButtonHeight() - 2f;
        float availableWidth = content.width();
        float availableHeight = content.bottom - availableTop;
        float slotSize = inventorySlotSize(content);
        float unit = slotSize / 18f;
        float padding = 8f * unit;
        float statusHeight = 28f * unit;
        float panelWidth = SLOT_COUNT * slotSize + padding * 2f;
        float panelHeight = padding * 2f + statusHeight + 12f * unit + INVENTORY_ROWS * slotSize + 24f * unit + slotSize;
        float left = content.left + (availableWidth - panelWidth) / 2f;
        float top = availableTop + (availableHeight - panelHeight) / 2f;
        return new RectF(left, top, left + panelWidth, top + panelHeight);
    }

    private float inventorySlotSize(RectF content) {
        float availableTop = content.top + modeButtonHeight() + 18f;
        float availableHeight = content.bottom - availableTop;
        float byWidth = (content.width() - 32f) / SLOT_COUNT;
        float byHeight = (availableHeight - 64f) / 5.5f;
        return Math.max(42f, Math.min(byWidth, byHeight));
    }

    private void drawModeButtons(Canvas canvas, RectF content) {
        drawMenuTab(canvas, modeButtonBounds(content, SCREEN_INVENTORY), "INV", activeScreen == SCREEN_INVENTORY);
        drawMenuTab(canvas, modeButtonBounds(content, SCREEN_MAP), "MAP", activeScreen == SCREEN_MAP);
    }

    private RectF modeButtonBounds(RectF content, int mode) {
        float width = Math.max(74f, getWidth() * 0.07f);
        float height = modeButtonHeight();
        float gap = 10f;
        float left = content.left + 18f + mode * (width + gap);
        return new RectF(left, content.top, left + width, content.top + height);
    }

    private float modeButtonHeight() {
        return Math.max(50f, getHeight() * 0.07f);
    }

    private void drawMenuTab(Canvas canvas, RectF rect, String label, boolean selected) {
        slotPaint.setColor(selected ? Color.rgb(198, 198, 198) : Color.rgb(72, 72, 72));
        canvas.drawRect(rect, slotPaint);
        drawPanelBorder(canvas, rect);
        if (selected) {
            slotPaint.setColor(Color.rgb(198, 198, 198));
            canvas.drawRect(rect.left + 4f, rect.bottom - 4f, rect.right - 4f, rect.bottom + 4f, slotPaint);
        }
        float scale = Math.max(2f, rect.height() / 38f);
        float textWidth = MineThorBitmapFont.width(label, scale);
        MineThorBitmapFont.draw(canvas, label, rect.centerX() - textWidth / 2f, rect.centerY() - 4f * scale, scale);
    }

    private void drawInventoryScreen(Canvas canvas, RectF content) {
        RectF panel = inventoryPanelBounds(content);
        PlayerSnapshot player = controller.currentSnapshot().player;
        InventorySnapshot inventory = controller.currentSnapshot().inventory;
        float slotSize = inventorySlotSize(content);
        float unit = slotSize / 18f;
        float padding = 8f * unit;

        panelPaint.setColor(Color.rgb(198, 198, 198));
        canvas.drawRect(panel, panelPaint);
        drawPanelBorder(canvas, panel);

        float left = panel.left + padding;
        float top = panel.top + padding;
        drawHudBars(canvas, left, top, player, unit);
        drawInventory(canvas, panel, inventory, unit);
        drawHotbar(canvas, panel, inventory, unit);
    }

    private void drawPanelBorder(Canvas canvas, RectF rect) {
        slotLightPaint.setColor(Color.WHITE);
        slotDarkPaint.setColor(Color.rgb(55, 55, 55));
        canvas.drawRect(rect.left, rect.top, rect.right, rect.top + 4f, slotLightPaint);
        canvas.drawRect(rect.left, rect.top, rect.left + 4f, rect.bottom, slotLightPaint);
        canvas.drawRect(rect.left, rect.bottom - 4f, rect.right, rect.bottom, slotDarkPaint);
        canvas.drawRect(rect.right - 4f, rect.top, rect.right, rect.bottom, slotDarkPaint);
    }

    private void init() {
        setFocusable(false);
        setFocusableInTouchMode(false);

        backgroundPaint.setColor(Color.rgb(8, 8, 8));
        panelPaint.setColor(Color.rgb(198, 198, 198));
        slotPaint.setColor(Color.rgb(139, 139, 139));
        slotLightPaint.setColor(Color.rgb(255, 255, 255));
        slotDarkPaint.setColor(Color.rgb(55, 55, 55));
        slotSelectedPaint.setColor(Color.rgb(112, 255, 112));
        iconPaint.setAntiAlias(false);
        iconPaint.setFilterBitmap(false);
        iconPaint.setDither(false);
        statPaint.setAntiAlias(false);
        xpPaint.setAntiAlias(false);
        touchPaint.setColor(Color.rgb(220, 184, 72));

        textPaint.setColor(Color.rgb(64, 64, 64));
        textPaint.setTextSize(30f);
        smallTextPaint.setColor(Color.WHITE);
        smallTextPaint.setTextSize(24f);
        smallTextPaint.setShadowLayer(2f, 2f, 2f, Color.BLACK);
        statPaint.setStrokeWidth(2f);
        xpPaint.setColor(Color.rgb(126, 203, 55));
        xpPaint.setStrokeWidth(6f);
    }

    private void drawInventory(Canvas canvas, RectF panel, InventorySnapshot inventory, float unit) {
        float slotSize = 18f * unit;
        float left = panel.left + 8f * unit;
        float top = panel.top + 48f * unit;

        for (int row = 0; row < INVENTORY_ROWS; row++) {
            for (int column = 0; column < INVENTORY_COLUMNS; column++) {
                int index = row * INVENTORY_COLUMNS + column;
                int inventoryIndex = index + SLOT_COUNT;
                float slotLeft = left + column * slotSize;
                float slotTop = top + row * slotSize;
                RectF rect = new RectF(slotLeft, slotTop, slotLeft + slotSize, slotTop + slotSize);
                drawSlotFrame(canvas, rect, unit);
                if (inventory.selectedInventorySlot == inventoryIndex) drawSelectedSlot(canvas, rect);
                drawSlotContent(canvas, inventory.mainSlots[inventoryIndex], rect);
            }
        }
    }

    private void drawHotbar(Canvas canvas, RectF panel, InventorySnapshot inventory, float unit) {
        float slotSize = 18f * unit;
        float left = panel.left + 8f * unit;
        float top = panel.bottom - 26f * unit;

        for (int index = 0; index < SLOT_COUNT; index++) {
            float slotLeft = left + index * slotSize;
            RectF rect = new RectF(slotLeft, top, slotLeft + slotSize, top + slotSize);
            drawSlotFrame(canvas, rect, unit);
            if (index == inventory.selectedHotbarSlot) drawSelectedSlot(canvas, rect);
            drawSlotContent(canvas, inventory.mainSlots[index], rect);
        }
    }

    private void drawSlotFrame(Canvas canvas, RectF rect, float unit) {
        slotPaint.setColor(Color.rgb(139, 139, 139));
        canvas.drawRect(rect, slotPaint);
        slotDarkPaint.setColor(Color.rgb(55, 55, 55));
        slotLightPaint.setColor(Color.WHITE);
        canvas.drawRect(rect.left, rect.top, rect.right, rect.top + unit, slotDarkPaint);
        canvas.drawRect(rect.left, rect.top, rect.left + unit, rect.bottom, slotDarkPaint);
        canvas.drawRect(rect.left, rect.bottom - unit, rect.right, rect.bottom, slotLightPaint);
        canvas.drawRect(rect.right - unit, rect.top, rect.right, rect.bottom, slotLightPaint);
    }

    private void drawSelectedSlot(Canvas canvas, RectF rect) {
        Bitmap widgets = MineThorMinecraftAssets.widgetsTexture();
        float unit = rect.width() / 18f;
        RectF dst = new RectF(rect.left - 3f * unit, rect.top - 3f * unit, rect.right + 3f * unit, rect.bottom + 3f * unit);
        if (widgets != null) {
            canvas.drawBitmap(widgets, new Rect(0, 22, 24, 46), dst, iconPaint);
            return;
        }

        slotLightPaint.setColor(Color.WHITE);
        slotDarkPaint.setColor(Color.rgb(55, 55, 55));
        canvas.drawRect(dst.left, dst.top, dst.right, dst.top + 2f * unit, slotLightPaint);
        canvas.drawRect(dst.left, dst.top, dst.left + 2f * unit, dst.bottom, slotLightPaint);
        canvas.drawRect(dst.left, dst.bottom - 2f * unit, dst.right, dst.bottom, slotDarkPaint);
        canvas.drawRect(dst.right - 2f * unit, dst.top, dst.right, dst.bottom, slotDarkPaint);
    }

    private void drawHudBars(Canvas canvas, float left, float top, PlayerSnapshot player, float unit) {
        drawArmorIcons(canvas, left, top, player.armor, unit);
        drawHearts(canvas, left, top + 8f * unit, player.health, player.maxHealth, unit);
        drawFood(canvas, left + 92f * unit, top + 8f * unit, player.food, player.maxFood, unit);
        drawExperience(canvas, left, top + 25f * unit, 150f * unit, player.xpLevel, unit);
    }

    private void drawHearts(Canvas canvas, float left, float top, int health, int maxHealth, float unit) {
        int hearts = Math.max(10, (int) Math.ceil(maxHealth / 2f));
        for (int i = 0; i < Math.min(10, hearts); i++) {
            float x = left + i * 8f * unit;
            int value = health - i * 2;
            drawHudIcon(canvas, hudIconPath("heart", value), legacyHeartSource(value), x, top, unit);
        }
    }

    private void drawArmorIcons(Canvas canvas, float left, float top, int armor, float unit) {
        for (int i = 0; i < 10; i++) {
            float x = left + i * 8f * unit;
            int value = armor - i * 2;
            drawHudIcon(canvas, armorIconPath(value), legacyArmorSource(value), x, top, unit);
        }
    }

    private void drawFood(Canvas canvas, float left, float top, int food, int maxFood, float unit) {
        int icons = Math.max(10, (int) Math.ceil(maxFood / 2f));
        for (int i = 0; i < Math.min(10, icons); i++) {
            float x = left + i * 8f * unit;
            int value = food - i * 2;
            drawHudIcon(canvas, foodIconPath(value), legacyFoodSource(value), x, top, unit);
        }
    }

    private void drawExperience(Canvas canvas, float left, float top, float width, int level, float unit) {
        drawExperienceBar(canvas, left, top, width, unit);
        String text = String.valueOf(level);
        float fontScale = Math.max(1f, unit);
        MineThorBitmapFont.draw(canvas, text, left + width / 2f - MineThorBitmapFont.width(text, fontScale) / 2f, top - 8f * unit, fontScale);
    }

    private void drawHudIcon(Canvas canvas, String modernPath, Rect legacySource, float x, float y, float unit) {
        RectF dst = new RectF(x, y, x + 9f * unit, y + 9f * unit);
        Bitmap modern = MineThorMinecraftAssets.texture(modernPath);
        if (modern != null) {
            canvas.drawBitmap(modern, null, dst, iconPaint);
            return;
        }

        Bitmap legacy = MineThorMinecraftAssets.texture(GUI_ICONS);
        if (legacy != null) {
            canvas.drawBitmap(legacy, legacySource, dst, iconPaint);
        }
    }

    private void drawExperienceBar(Canvas canvas, float left, float top, float width, float unit) {
        Bitmap background = MineThorMinecraftAssets.texture("assets/minecraft/textures/gui/sprites/hud/experience_bar_background.png");
        Bitmap progress = MineThorMinecraftAssets.texture("assets/minecraft/textures/gui/sprites/hud/experience_bar_progress.png");
        RectF dst = new RectF(left, top, left + width, top + 5f * unit);
        if (background != null && progress != null) {
            canvas.drawBitmap(background, null, dst, iconPaint);
            canvas.drawBitmap(progress, null, new RectF(left, top, left + width * 0.45f, top + 5f * unit), iconPaint);
            return;
        }

        Bitmap legacy = MineThorMinecraftAssets.texture(GUI_ICONS);
        if (legacy != null) {
            canvas.drawBitmap(legacy, new Rect(0, 64, 182, 69), dst, iconPaint);
            canvas.drawBitmap(legacy, new Rect(0, 69, 82, 74), new RectF(left, top, left + width * 0.45f, top + 5f * unit), iconPaint);
            return;
        }

        statPaint.setColor(Color.rgb(45, 45, 45));
        canvas.drawRect(dst, statPaint);
        xpPaint.setStrokeWidth(2f * unit);
        canvas.drawLine(left + unit, top + 2.5f * unit, left + width * 0.45f, top + 2.5f * unit, xpPaint);
    }

    private static String hudIconPath(String group, int value) {
        if (value >= 2) return "assets/minecraft/textures/gui/sprites/hud/" + group + "/full.png";
        if (value == 1) return "assets/minecraft/textures/gui/sprites/hud/" + group + "/half.png";
        return "assets/minecraft/textures/gui/sprites/hud/" + group + "/container.png";
    }

    private static String armorIconPath(int value) {
        if (value >= 2) return "assets/minecraft/textures/gui/sprites/hud/armor_full.png";
        if (value == 1) return "assets/minecraft/textures/gui/sprites/hud/armor_half.png";
        return "assets/minecraft/textures/gui/sprites/hud/armor_empty.png";
    }

    private static String foodIconPath(int value) {
        if (value >= 2) return "assets/minecraft/textures/gui/sprites/hud/food_full.png";
        if (value == 1) return "assets/minecraft/textures/gui/sprites/hud/food_half.png";
        return "assets/minecraft/textures/gui/sprites/hud/food_empty.png";
    }

    private static Rect legacyHeartSource(int value) {
        if (value >= 2) return new Rect(52, 0, 61, 9);
        if (value == 1) return new Rect(61, 0, 70, 9);
        return new Rect(16, 0, 25, 9);
    }

    private static Rect legacyArmorSource(int value) {
        if (value >= 2) return new Rect(34, 9, 43, 18);
        if (value == 1) return new Rect(25, 9, 34, 18);
        return new Rect(16, 9, 25, 18);
    }

    private static Rect legacyFoodSource(int value) {
        if (value >= 2) return new Rect(52, 27, 61, 36);
        if (value == 1) return new Rect(61, 27, 70, 36);
        return new Rect(16, 27, 25, 36);
    }

    private void drawSlotContent(Canvas canvas, InventorySlotSnapshot slot, RectF rect) {
        if (slot == null || slot.isEmpty()) return;

        Bitmap icon = MineThorIconCache.icon(slot.iconKey);
        if (icon == null && MineThorIconCache.shouldRequest(slot.iconKey)) {
            controller.requestIcon(slot.iconKey);
        }
        float inset = rect.width() * 0.08f;
        if (icon != null) {
            canvas.drawBitmap(icon, null, new RectF(rect.left + inset, rect.top + inset, rect.right - inset, rect.bottom - inset), iconPaint);
        } else {
            String fallback = fallbackItemText(slot.itemId);
            canvas.drawText(fallback, rect.left + 10f, rect.top + rect.height() * 0.55f, smallTextPaint);
        }

        if (slot.count > 1) {
            String count = String.valueOf(slot.count);
            float scale = rect.width() / 18f;
            float textWidth = MineThorBitmapFont.width(count, scale);
            MineThorBitmapFont.draw(canvas, count, rect.right - textWidth - rect.width() * 0.02f, rect.bottom - 8f * scale, scale);
        }
    }

    private static String fallbackItemText(String itemId) {
        int separator = itemId.indexOf(':');
        String path = separator < 0 ? itemId : itemId.substring(separator + 1);
        if (path.length() <= 3) return path;
        return path.substring(0, 3);
    }

    private void updateSelection(float touchX, float touchY) {
        if (!controller.currentSnapshot().connected || activeScreen != SCREEN_INVENTORY) return;

        int inventorySlot = inventorySlotIndexFromTouch(touchX, touchY);
        if (inventorySlot >= 0) {
            return;
        }

        int hotbarSlot = hotbarSlotIndexFromTouch(touchX, touchY);
        if (hotbarSlot >= 0) controller.selectHotbarSlot(hotbarSlot);
    }

    private int inventorySlotIndexFromTouch(float touchX, float touchY) {
        RectF panel = inventoryPanelBounds(contentBounds());
        float unit = inventorySlotSize(contentBounds()) / 18f;
        float slotSize = 18f * unit;
        float left = panel.left + 8f * unit;
        float top = panel.top + 48f * unit;

        if (touchY < top || touchY > top + INVENTORY_ROWS * slotSize) return -1;

        for (int row = 0; row < INVENTORY_ROWS; row++) {
            for (int column = 0; column < INVENTORY_COLUMNS; column++) {
                float slotLeft = left + column * slotSize;
                float slotTop = top + row * slotSize;
                if (touchX >= slotLeft && touchX <= slotLeft + slotSize && touchY >= slotTop && touchY <= slotTop + slotSize) {
                    return row * INVENTORY_COLUMNS + column + SLOT_COUNT;
                }
            }
        }
        return -1;
    }

    private int hotbarSlotIndexFromTouch(float touchX, float touchY) {
        RectF panel = inventoryPanelBounds(contentBounds());
        float unit = inventorySlotSize(contentBounds()) / 18f;
        float slotSize = 18f * unit;
        float left = panel.left + 8f * unit;
        float top = panel.bottom - 26f * unit;

        if (touchY < top || touchY > top + slotSize) return -1;
        for (int index = 0; index < SLOT_COUNT; index++) {
            float slotLeft = left + index * slotSize;
            if (touchX >= slotLeft && touchX <= slotLeft + slotSize) return index;
        }
        return -1;
    }

    private boolean updateScreenMode(float touchX, float touchY) {
        RectF content = contentBounds();
        if (modeButtonBounds(content, SCREEN_INVENTORY).contains(touchX, touchY)) {
            activeScreen = SCREEN_INVENTORY;
            return true;
        }
        if (modeButtonBounds(content, SCREEN_MAP).contains(touchX, touchY)) {
            activeScreen = SCREEN_MAP;
            return true;
        }
        return false;
    }

    private void drawMapScreen(Canvas canvas, RectF content) {
        PlayerSnapshot player = controller.currentSnapshot().player;
        float top = content.top + modeButtonHeight() - 2f;
        RectF panel = new RectF(content.left, top, content.right, content.bottom);
        panelPaint.setColor(Color.rgb(198, 198, 198));
        canvas.drawRect(panel, panelPaint);
        drawPanelBorder(canvas, panel);

        float unit = Math.min(panel.width(), panel.height()) / 180f;
        float compassSize = Math.min(panel.height() * 0.42f, panel.width() * 0.28f);
        float compassLeft = panel.right - compassSize - 14f * unit;
        RectF compass = new RectF(compassLeft, panel.top + 18f * unit, compassLeft + compassSize, panel.top + 18f * unit + compassSize);
        drawCompass(canvas, compass, player.yaw, unit);

        float mapRight = compass.left - 18f * unit;
        RectF map = new RectF(panel.left + 14f * unit, panel.top + 18f * unit, mapRight, panel.bottom - 18f * unit);
        drawCoordinateMap(canvas, map, player, unit);
        RectF time = new RectF(compass.left, compass.bottom + 18f * unit, compass.right, panel.bottom - 18f * unit);
        drawTimeDial(canvas, time, player.dayTime, unit);
    }

    private void drawCoordinateMap(Canvas canvas, RectF map, PlayerSnapshot player, float unit) {
        slotPaint.setColor(Color.rgb(139, 139, 139));
        canvas.drawRect(map, slotPaint);
        drawPanelBorder(canvas, map);

        statPaint.setColor(Color.rgb(95, 95, 95));
        statPaint.setStrokeWidth(Math.max(1f, unit));
        for (int i = 1; i < 6; i++) {
            float x = map.left + map.width() * i / 6f;
            float y = map.top + map.height() * i / 6f;
            canvas.drawLine(x, map.top, x, map.bottom, statPaint);
            canvas.drawLine(map.left, y, map.right, y, statPaint);
        }

        String coords = "XYZ " + player.x + " / " + player.y + " / " + player.z;
        float scale = Math.max(1.4f, Math.min(unit * 0.82f, (map.width() - 16f * unit) / Math.max(1f, MineThorBitmapFont.width(coords, 1f))));
        MineThorBitmapFont.draw(canvas, coords, map.left + 8f * unit, map.top + 8f * unit, scale);

        float cx = map.centerX();
        float cy = map.centerY();
        touchPaint.setColor(Color.rgb(255, 255, 255));
        canvas.drawCircle(cx, cy, 3f * unit, touchPaint);
        double yawRadians = Math.toRadians(player.yaw);
        canvas.drawLine(cx, cy, cx + (float) Math.sin(yawRadians) * 18f * unit, cy - (float) Math.cos(yawRadians) * 18f * unit, touchPaint);
        touchPaint.setColor(Color.rgb(220, 184, 72));
    }

    private void drawCompass(Canvas canvas, RectF rect, int yaw, float unit) {
        Bitmap compass = MineThorMinecraftAssets.texture("assets/minecraft/textures/item/compass_00.png");
        if (compass != null) canvas.drawBitmap(compass, null, rect, iconPaint);

        statPaint.setColor(Color.rgb(55, 55, 55));
        statPaint.setStyle(Paint.Style.STROKE);
        statPaint.setStrokeWidth(2f * unit);
        canvas.drawOval(rect, statPaint);
        statPaint.setStyle(Paint.Style.FILL);

        double yawRadians = Math.toRadians(yaw);
        float cx = rect.centerX();
        float cy = rect.centerY();
        float radius = rect.width() * 0.38f;
        xpPaint.setColor(Color.rgb(210, 40, 40));
        xpPaint.setStrokeWidth(3f * unit);
        canvas.drawLine(cx, cy, cx + (float) Math.sin(yawRadians) * radius, cy - (float) Math.cos(yawRadians) * radius, xpPaint);

        float scale = Math.max(2f, unit);
        MineThorBitmapFont.draw(canvas, "N", cx - 4f * scale, rect.top - 10f * scale, scale);
        MineThorBitmapFont.draw(canvas, String.valueOf(yaw), cx - MineThorBitmapFont.width(String.valueOf(yaw), scale) / 2f, cy - 4f * scale, scale);
    }

    private void drawTimeDial(Canvas canvas, RectF rect, long dayTime, float unit) {
        slotPaint.setColor(Color.rgb(139, 139, 139));
        canvas.drawRect(rect, slotPaint);
        drawPanelBorder(canvas, rect);

        float phase = (dayTime % 24000L) / 24000f;
        float centerX = rect.centerX();
        float horizonY = rect.centerY() + rect.height() * 0.18f;
        float radius = rect.width() * 0.34f;
        statPaint.setColor(Color.rgb(95, 95, 95));
        statPaint.setStrokeWidth(Math.max(1f, unit));
        canvas.drawLine(rect.left + 10f * unit, horizonY, rect.right - 10f * unit, horizonY, statPaint);

        drawSkyBody(canvas, "assets/minecraft/textures/environment/sun.png", true, phase, centerX, horizonY, radius, unit);
        drawSkyBody(canvas, "assets/minecraft/textures/environment/moon_phases.png", false, (phase + 0.5f) % 1f, centerX, horizonY, radius, unit);

        int clockFrame = Math.round(phase * 63f) % 64;
        Bitmap clock = MineThorMinecraftAssets.texture(String.format(java.util.Locale.US, "assets/minecraft/textures/item/clock_%02d.png", clockFrame));
        if (clock != null) {
            float size = Math.min(rect.width(), rect.height()) * 0.2f;
            RectF clockRect = new RectF(rect.right - size - 8f * unit, rect.bottom - size - 8f * unit, rect.right - 8f * unit, rect.bottom - 8f * unit);
            canvas.drawBitmap(clock, null, clockRect, iconPaint);
        }
    }

    private void drawSkyBody(Canvas canvas, String path, boolean sun, float phase, float centerX, float horizonY, float radius, float unit) {
        double angle = phase * Math.PI * 2.0 - Math.PI;
        float x = centerX + (float) Math.cos(angle) * radius;
        float y = horizonY + (float) Math.sin(angle) * radius * 0.72f;
        float size = 14f * unit;
        RectF dst = new RectF(x - size / 2f, y - size / 2f, x + size / 2f, y + size / 2f);
        Bitmap texture = MineThorMinecraftAssets.texture(path);
        if (texture != null) {
            Rect source = sun ? null : new Rect(0, 0, texture.getWidth() / 4, texture.getHeight() / 2);
            canvas.drawBitmap(texture, source, dst, iconPaint);
            return;
        }

        xpPaint.setColor(sun ? Color.rgb(236, 210, 72) : Color.rgb(210, 210, 230));
        canvas.drawRect(dst, xpPaint);
    }
}
