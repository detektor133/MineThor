package net.kdt.pojavlaunch.minethor;

import android.os.Bundle;
import android.view.Display;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import net.kdt.pojavlaunch.R;

public class ThorCompanionActivity extends AppCompatActivity {
    private TextView statusText;
    private ThorCompanionDisplayController companionDisplayController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        companionDisplayController = new ThorCompanionDisplayController(this, new ThorCompanionDisplayController.Listener() {
            @Override
            public void onCompanionStateChanged() {
                updateStatus();
            }

            @Override
            public void onCompanionUnavailable() {
                Toast.makeText(ThorCompanionActivity.this, R.string.minethor_probe_no_secondary_display, Toast.LENGTH_LONG).show();
                updateStatus();
            }
        });
        setContentView(createContentView());
        companionDisplayController.start();
        updateStatus();
    }

    @Override
    protected void onDestroy() {
        if (companionDisplayController != null) companionDisplayController.stop();
        super.onDestroy();
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
        showButton.setText(R.string.minethor_companion_show);
        showButton.setOnClickListener(v -> {
            companionDisplayController.showCompanion();
            updateStatus();
        });
        layout.addView(showButton, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        Button closeButton = new Button(this);
        closeButton.setText(R.string.minethor_companion_close);
        closeButton.setOnClickListener(v -> {
            companionDisplayController.dismissCompanion();
            updateStatus();
        });
        layout.addView(closeButton, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        return layout;
    }

    private void updateStatus() {
        Display[] displays = companionDisplayController == null ? new Display[0] : companionDisplayController.getDisplays();
        StringBuilder builder = new StringBuilder();
        builder.append(getString(R.string.minethor_companion_activity_title)).append('\n');
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
                companionDisplayController == null || !companionDisplayController.isShowing()
                        ? getString(R.string.minethor_probe_state_closed)
                        : getString(R.string.minethor_probe_state_open)
        ));
        statusText.setText(builder.toString());
    }
}
