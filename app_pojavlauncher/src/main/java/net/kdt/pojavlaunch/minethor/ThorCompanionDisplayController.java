package net.kdt.pojavlaunch.minethor;

import android.app.Activity;
import android.app.Presentation;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

public class ThorCompanionDisplayController implements DisplayManager.DisplayListener {
    private static final String TAG = "MineThorCompanion";

    public interface Listener {
        void onCompanionStateChanged();

        void onCompanionUnavailable();
    }

    private final Activity activity;
    private final DisplayManager displayManager;
    private final Listener listener;
    private ThorCompanionPresentation presentation;
    private boolean started;

    public ThorCompanionDisplayController(Activity activity, Listener listener) {
        this.activity = activity;
        this.displayManager = (DisplayManager) activity.getSystemService(Context.DISPLAY_SERVICE);
        this.listener = listener;
    }

    public void start() {
        if (started) return;

        started = true;
        displayManager.registerDisplayListener(this, null);
        showCompanion();
    }

    public void stop() {
        if (!started) return;

        started = false;
        displayManager.unregisterDisplayListener(this);
        dismissCompanion();
    }

    public void showCompanion() {
        if (presentation != null) return;

        Display display = findSecondaryDisplay();
        if (display == null) {
            notifyUnavailable();
            return;
        }

        try {
            presentation = new ThorCompanionPresentation(activity, display);
            presentation.setOnDismissListener(dialog -> {
                Log.i(TAG, "Presentation dismissed");
                presentation = null;
                notifyStateChanged();
            });
            presentation.show();
            Log.i(TAG, "Presentation shown on display " + display.getDisplayId());
            notifyStateChanged();
        } catch (WindowManager.InvalidDisplayException e) {
            Log.e(TAG, "Cannot show presentation", e);
            presentation = null;
            notifyUnavailable();
        }
    }

    public void dismissCompanion() {
        Presentation currentPresentation = presentation;
        if (currentPresentation == null) return;

        presentation = null;
        currentPresentation.dismiss();
        notifyStateChanged();
    }

    public boolean isShowing() {
        return presentation != null;
    }

    public Display[] getDisplays() {
        return displayManager.getDisplays();
    }

    @Override
    public void onDisplayAdded(int displayId) {
        Log.i(TAG, "Display added: " + displayId);
        activity.runOnUiThread(this::showCompanion);
    }

    @Override
    public void onDisplayRemoved(int displayId) {
        Log.i(TAG, "Display removed: " + displayId);
        activity.runOnUiThread(() -> {
            if (presentation != null && presentation.getDisplay().getDisplayId() == displayId) {
                dismissCompanion();
            }
            notifyStateChanged();
        });
    }

    @Override
    public void onDisplayChanged(int displayId) {
        Log.i(TAG, "Display changed: " + displayId);
        activity.runOnUiThread(this::notifyStateChanged);
    }

    private Display findSecondaryDisplay() {
        Display[] presentationDisplays = displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION);
        if (presentationDisplays.length > 0) return presentationDisplays[0];

        for (Display display : displayManager.getDisplays()) {
            if (display.getDisplayId() != Display.DEFAULT_DISPLAY) return display;
        }
        return null;
    }

    private void notifyStateChanged() {
        if (listener != null) listener.onCompanionStateChanged();
    }

    private void notifyUnavailable() {
        if (listener != null) listener.onCompanionUnavailable();
    }
}
