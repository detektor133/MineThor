package net.kdt.pojavlaunch.minethor;

import android.app.Presentation;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import net.kdt.pojavlaunch.R;

public class ThorProbeActivity extends AppCompatActivity implements DisplayManager.DisplayListener {
    private static final String TAG = "MineThorProbe";

    private DisplayManager displayManager;
    private TextView statusText;
    private ThorProbePresentation presentation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        displayManager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
        setContentView(createContentView());
        displayManager.registerDisplayListener(this, null);
        showProbe();
        updateStatus();
    }

    @Override
    protected void onDestroy() {
        dismissProbe();
        displayManager.unregisterDisplayListener(this);
        super.onDestroy();
    }

    @Override
    public void onDisplayAdded(int displayId) {
        Log.i(TAG, "Display added: " + displayId);
        runOnUiThread(() -> {
            showProbe();
            updateStatus();
        });
    }

    @Override
    public void onDisplayRemoved(int displayId) {
        Log.i(TAG, "Display removed: " + displayId);
        runOnUiThread(() -> {
            if (presentation != null && presentation.getDisplay().getDisplayId() == displayId) {
                dismissProbe();
            }
            updateStatus();
        });
    }

    @Override
    public void onDisplayChanged(int displayId) {
        Log.i(TAG, "Display changed: " + displayId);
        runOnUiThread(this::updateStatus);
    }

    private LinearLayout createContentView() {
        LinearLayout layout = new LinearLayout(this);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = getResources().getDimensionPixelSize(R.dimen._17sdp);
        layout.setPadding(padding, padding, padding, padding);

        statusText = new TextView(this);
        statusText.setTextSize(16f);
        statusText.setGravity(Gravity.START);
        layout.addView(statusText, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));

        Button showButton = new Button(this);
        showButton.setText(R.string.minethor_probe_show);
        showButton.setOnClickListener(v -> {
            showProbe();
            updateStatus();
        });
        layout.addView(showButton, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        Button closeButton = new Button(this);
        closeButton.setText(R.string.minethor_probe_close);
        closeButton.setOnClickListener(v -> {
            dismissProbe();
            updateStatus();
        });
        layout.addView(closeButton, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        return layout;
    }

    private void updateStatus() {
        Display[] displays = displayManager.getDisplays();
        StringBuilder builder = new StringBuilder();
        builder.append(getString(R.string.minethor_probe_activity_title)).append('\n');
        builder.append(getString(R.string.minethor_probe_display_count, displays.length)).append('\n');
        for (Display display : displays) {
            builder.append(getString(
                    R.string.minethor_probe_display_row,
                    display.getDisplayId(),
                    display.getName()
            )).append('\n');
        }
        builder.append(getString(
                R.string.minethor_probe_state,
                presentation == null ? getString(R.string.minethor_probe_state_closed) : getString(R.string.minethor_probe_state_open)
        ));
        statusText.setText(builder.toString());
    }

    private void showProbe() {
        if (presentation != null) return;

        Display display = findSecondaryDisplay();
        if (display == null) {
            Toast.makeText(this, R.string.minethor_probe_no_secondary_display, Toast.LENGTH_LONG).show();
            return;
        }

        try {
            presentation = new ThorProbePresentation(this, display);
            presentation.setOnDismissListener(dialog -> {
                Log.i(TAG, "Presentation dismissed");
                presentation = null;
                updateStatus();
            });
            presentation.show();
            Log.i(TAG, "Presentation shown on display " + display.getDisplayId());
        } catch (WindowManager.InvalidDisplayException e) {
            Log.e(TAG, "Cannot show presentation", e);
            presentation = null;
            Toast.makeText(this, R.string.minethor_probe_invalid_display, Toast.LENGTH_LONG).show();
        }
    }

    private void dismissProbe() {
        Presentation currentPresentation = presentation;
        if (currentPresentation == null) return;
        presentation = null;
        currentPresentation.dismiss();
    }

    private Display findSecondaryDisplay() {
        Display[] presentationDisplays = displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION);
        if (presentationDisplays.length > 0) return presentationDisplays[0];

        for (Display display : displayManager.getDisplays()) {
            if (display.getDisplayId() != Display.DEFAULT_DISPLAY) return display;
        }
        return null;
    }
}
