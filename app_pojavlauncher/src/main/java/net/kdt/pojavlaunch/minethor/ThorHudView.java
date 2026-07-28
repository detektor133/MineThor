package net.kdt.pojavlaunch.minethor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import net.kdt.pojavlaunch.R;

public class ThorHudView extends View {
    private static final int SLOT_COUNT = 9;
    private static final int INVENTORY_COLUMNS = 9;
    private static final int INVENTORY_ROWS = 4;

    private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint panelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint slotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint activeSlotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint armorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint compassPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint compassNeedlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint smallTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint touchPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ThorCompanionController controller;
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
        updateSelection(event.getX(), event.getY());
        invalidate();
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(0f, 0f, getWidth(), getHeight(), backgroundPaint);

        float padding = getWidth() * 0.06f;
        drawHeader(canvas, padding);
        drawCompass(canvas, padding);
        drawStats(canvas, padding);
        drawInventory(canvas, padding);
        drawHotbar(canvas, padding);

        if (pointerCount > 1) {
            canvas.drawCircle(getWidth() - padding * 0.8f, padding * 0.8f, 22f, touchPaint);
        }
    }

    private void init() {
        setFocusable(true);
        setFocusableInTouchMode(true);

        backgroundPaint.setColor(Color.rgb(18, 22, 26));
        panelPaint.setColor(Color.rgb(28, 34, 40));
        slotPaint.setColor(Color.rgb(56, 64, 72));
        activeSlotPaint.setColor(Color.rgb(76, 132, 92));
        armorPaint.setColor(Color.rgb(74, 92, 112));
        compassPaint.setColor(Color.rgb(44, 52, 60));
        compassPaint.setStyle(Paint.Style.STROKE);
        compassPaint.setStrokeWidth(6f);
        compassNeedlePaint.setColor(Color.rgb(224, 74, 74));
        compassNeedlePaint.setStrokeWidth(8f);
        touchPaint.setColor(Color.rgb(220, 184, 72));

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(30f);
        smallTextPaint.setColor(Color.rgb(210, 218, 226));
        smallTextPaint.setTextSize(24f);
    }

    private void drawHeader(Canvas canvas, float padding) {
        CompanionSnapshot snapshot = controller.currentSnapshot();
        canvas.drawText(getResources().getString(R.string.minethor_hud_title), padding, padding, textPaint);
        String status = snapshot.connected
                ? getResources().getString(R.string.minethor_hud_connected)
                : getResources().getString(R.string.minethor_hud_disconnected);
        canvas.drawText(status, padding, padding + 34f, smallTextPaint);
    }

    private void drawCompass(Canvas canvas, float padding) {
        CompanionSnapshot snapshot = controller.currentSnapshot();
        float centerX = getWidth() - padding - 118f;
        float centerY = 100f;
        float radius = 58f;
        float needleLength = 48f;
        double yawRadians = Math.toRadians(snapshot.player.yaw);
        float needleEndX = centerX + (float) Math.sin(yawRadians) * needleLength;
        float needleEndY = centerY - (float) Math.cos(yawRadians) * needleLength;
        canvas.drawCircle(centerX, centerY, radius, compassPaint);
        canvas.drawLine(centerX, centerY, needleEndX, needleEndY, compassNeedlePaint);
        canvas.drawText("N", centerX - 10f, centerY - radius - 16f, textPaint);
        canvas.drawText("E", centerX + radius + 16f, centerY + 10f, smallTextPaint);
        canvas.drawText("S", centerX - 8f, centerY + radius + 34f, smallTextPaint);
        canvas.drawText("W", centerX - radius - 38f, centerY + 10f, smallTextPaint);
        canvas.drawText(getResources().getString(R.string.minethor_hud_yaw, snapshot.player.yaw), centerX - 52f, centerY + 12f, smallTextPaint);
    }

    private void drawStats(Canvas canvas, float padding) {
        PlayerSnapshot player = controller.currentSnapshot().player;
        float top = 220f;
        RectF panel = new RectF(padding, top, getWidth() - padding, top + 112f);
        canvas.drawRoundRect(panel, 8f, 8f, panelPaint);

        canvas.drawText(getResources().getString(R.string.minethor_hud_coords, player.x, player.y, player.z), padding + 24f, top + 42f, textPaint);
        canvas.drawText(getResources().getString(R.string.minethor_hud_health, player.health, player.maxHealth), padding + 24f, top + 84f, smallTextPaint);
        canvas.drawText(getResources().getString(R.string.minethor_hud_food, player.food, player.maxFood), padding + 280f, top + 84f, smallTextPaint);
        canvas.drawText(getResources().getString(R.string.minethor_hud_armor, player.armor), padding + 510f, top + 84f, smallTextPaint);
        canvas.drawText(getResources().getString(R.string.minethor_hud_xp, player.xpLevel), padding + 710f, top + 84f, smallTextPaint);
    }

    private void drawInventory(Canvas canvas, float padding) {
        InventorySnapshot inventory = controller.currentSnapshot().inventory;
        float top = 386f;
        float gap = 8f;
        float slotSize = inventorySlotSize(padding, gap);
        canvas.drawText(getResources().getString(R.string.minethor_hud_inventory), padding, top - 22f, textPaint);

        for (int row = 0; row < INVENTORY_ROWS; row++) {
            for (int column = 0; column < INVENTORY_COLUMNS; column++) {
                int index = row * INVENTORY_COLUMNS + column;
                float left = padding + column * (slotSize + gap);
                float slotTop = top + row * (slotSize + gap);
                Paint paint = inventory.selectedInventorySlot == index ? activeSlotPaint : slotPaint;
                RectF rect = new RectF(left, slotTop, left + slotSize, slotTop + slotSize);
                canvas.drawRoundRect(rect, 8f, 8f, paint);
            }
        }
    }

    private void drawHotbar(Canvas canvas, float padding) {
        InventorySnapshot inventory = controller.currentSnapshot().inventory;
        float gap = getWidth() * 0.012f;
        float slotSize = (getWidth() - padding * 2f - gap * (SLOT_COUNT - 1)) / SLOT_COUNT;
        float top = getHeight() - padding - slotSize - 46f;

        canvas.drawText(getResources().getString(R.string.minethor_hud_hotbar), padding, top - 18f, textPaint);
        for (int index = 0; index < SLOT_COUNT; index++) {
            float left = padding + index * (slotSize + gap);
            RectF rect = new RectF(left, top, left + slotSize, top + slotSize);
            canvas.drawRoundRect(rect, 8f, 8f, index == inventory.selectedHotbarSlot ? activeSlotPaint : slotPaint);
            canvas.drawText(String.valueOf(index + 1), left + slotSize * 0.42f, top + slotSize * 0.58f, textPaint);
        }
    }

    private void updateSelection(float touchX, float touchY) {
        if (!controller.currentSnapshot().connected) return;

        int inventorySlot = inventorySlotIndexFromTouch(touchX, touchY);
        if (inventorySlot >= 0) {
            return;
        }

        int hotbarSlot = hotbarSlotIndexFromTouch(touchX, touchY);
        if (hotbarSlot >= 0) controller.selectHotbarSlot(hotbarSlot);
    }

    private int inventorySlotIndexFromTouch(float touchX, float touchY) {
        float padding = getWidth() * 0.06f;
        float gap = 8f;
        float slotSize = inventorySlotSize(padding, gap);
        float top = 386f;

        if (touchY < top || touchY > top + INVENTORY_ROWS * slotSize + (INVENTORY_ROWS - 1) * gap) return -1;

        for (int row = 0; row < INVENTORY_ROWS; row++) {
            for (int column = 0; column < INVENTORY_COLUMNS; column++) {
                float left = padding + column * (slotSize + gap);
                float slotTop = top + row * (slotSize + gap);
                if (touchX >= left && touchX <= left + slotSize && touchY >= slotTop && touchY <= slotTop + slotSize) {
                    return row * INVENTORY_COLUMNS + column;
                }
            }
        }
        return -1;
    }

    private int hotbarSlotIndexFromTouch(float touchX, float touchY) {
        float padding = getWidth() * 0.06f;
        float gap = getWidth() * 0.012f;
        float slotSize = (getWidth() - padding * 2f - gap * (SLOT_COUNT - 1)) / SLOT_COUNT;
        float top = getHeight() - padding - slotSize - 46f;

        if (touchY < top || touchY > top + slotSize) return -1;
        for (int index = 0; index < SLOT_COUNT; index++) {
            float left = padding + index * (slotSize + gap);
            if (touchX >= left && touchX <= left + slotSize) return index;
        }
        return -1;
    }

    private float inventorySlotSize(float padding, float gap) {
        return Math.min(94f, (getWidth() - padding * 2f - gap * (INVENTORY_COLUMNS - 1)) / INVENTORY_COLUMNS);
    }
}
