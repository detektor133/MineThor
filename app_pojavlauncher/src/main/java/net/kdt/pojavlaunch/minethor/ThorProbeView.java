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

public class ThorProbeView extends View {
    private static final int SLOT_COUNT = 9;

    private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint slotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint activeSlotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint touchPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Display display;
    private String lastTouchText;
    private int pointerCount;

    public ThorProbeView(Context context, Display display) {
        super(context);
        this.display = display;
        init();
    }

    public ThorProbeView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.display = null;
        init();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        pointerCount = event.getPointerCount();
        lastTouchText = getResources().getString(
                R.string.minethor_probe_touch,
                event.getActionMasked(),
                event.getX(),
                event.getY(),
                pointerCount
        );
        invalidate();
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(0f, 0f, getWidth(), getHeight(), backgroundPaint);

        float padding = getWidth() * 0.06f;
        float gap = getWidth() * 0.012f;
        float slotSize = (getWidth() - padding * 2f - gap * (SLOT_COUNT - 1)) / SLOT_COUNT;
        float top = getHeight() * 0.42f;

        for (int index = 0; index < SLOT_COUNT; index++) {
            float left = padding + index * (slotSize + gap);
            RectF rect = new RectF(left, top, left + slotSize, top + slotSize);
            canvas.drawRoundRect(rect, 8f, 8f, index == 0 ? activeSlotPaint : slotPaint);
            canvas.drawText(String.valueOf(index + 1), left + slotSize * 0.42f, top + slotSize * 0.58f, textPaint);
        }

        int displayId = display == null ? Display.DEFAULT_DISPLAY : display.getDisplayId();
        canvas.drawText(getResources().getString(R.string.minethor_probe_canvas_title), padding, getHeight() * 0.14f, textPaint);
        canvas.drawText(getResources().getString(R.string.minethor_probe_canvas_display, displayId), padding, getHeight() * 0.22f, textPaint);
        canvas.drawText(getResources().getString(R.string.minethor_probe_canvas_size, getWidth(), getHeight()), padding, getHeight() * 0.30f, textPaint);
        canvas.drawText(lastTouchText == null ? getResources().getString(R.string.minethor_probe_no_touch) : lastTouchText, padding, getHeight() * 0.78f, textPaint);

        if (pointerCount > 1) {
            canvas.drawCircle(getWidth() - padding, getHeight() * 0.18f, 28f, touchPaint);
        }
    }

    private void init() {
        setFocusable(true);
        setFocusableInTouchMode(true);

        backgroundPaint.setColor(Color.rgb(18, 22, 26));
        slotPaint.setColor(Color.rgb(56, 64, 72));
        activeSlotPaint.setColor(Color.rgb(76, 132, 92));
        touchPaint.setColor(Color.rgb(220, 184, 72));

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(28f);
    }
}
