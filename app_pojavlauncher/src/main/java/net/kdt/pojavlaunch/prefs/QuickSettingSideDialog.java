package net.kdt.pojavlaunch.prefs;

import static net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_DISABLE_GESTURES;
import static net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_ENABLE_GYRO;
import static net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_GYRO_INVERT_X;
import static net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_GYRO_INVERT_Y;
import static net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_GYRO_SENSITIVITY;
import static net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_LONGPRESS_TRIGGER;
import static net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_MOUSESPEED;
import static net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_SCALE_FACTOR;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.kdt.CustomSeekbar;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.utils.SimpleSeekBarListener;

/**
 * Side dialog for quick settings that you can change in game
 * The implementation has to take action on some preference changes
 */
public abstract class QuickSettingSideDialog extends com.kdt.SideDialogView<ConstraintLayout> {

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private Switch mGyroSwitch, mGyroXSwitch, mGyroYSwitch, mGestureSwitch;
    private CustomSeekbar mGyroSensitivityBar, mMouseSpeedBar, mGestureDelayBar, mResolutionBar;
    private TextView mGyroSensitivityText, mGyroSensitivityDisplayText, mMouseSpeedText, mGestureDelayText, mGestureDelayDisplayText, mResolutionText;

    private boolean mOriginalGyroEnabled, mOriginalGyroXEnabled, mOriginalGyroYEnabled, mOriginalGestureDisabled;
    private float mOriginalGyroSensitivity, mOriginalMouseSpeed, mOriginalResolution;
    private int mOriginalGestureDelay;

    public QuickSettingSideDialog(Context context, ViewGroup parent) {
        super(context, parent, R.layout.dialog_quick_setting);
        setTitle(R.string.quick_setting_title);
        setupCancelButton();
    }

    @Override
    protected void onAppear(boolean hasBuilt) {
        if (hasBuilt) {
            bindLayout();
            setupListeners();
        }
    }

    @Override
    protected void onDisappear(boolean willDestroy) {
        if (willDestroy) removeListeners();
    }

    private void bindLayout() {
        // Bind layout elements
        mGyroSwitch = mDialogContent.findViewById(R.id.checkboxGyro);
        mGyroXSwitch = mDialogContent.findViewById(R.id.checkboxGyroX);
        mGyroYSwitch = mDialogContent.findViewById(R.id.checkboxGyroY);
        mGestureSwitch = mDialogContent.findViewById(R.id.checkboxGesture);

        mGyroSensitivityBar = mDialogContent.findViewById(R.id.editGyro_seekbar);
        mMouseSpeedBar = mDialogContent.findViewById(R.id.editMouseSpeed_seekbar);
        mGestureDelayBar = mDialogContent.findViewById(R.id.editGestureDelay_seekbar);
        mResolutionBar = mDialogContent.findViewById(R.id.editResolution_seekbar);

        mGyroSensitivityText = mDialogContent.findViewById(R.id.editGyro_textView_percent);
        mGyroSensitivityDisplayText = mDialogContent.findViewById(R.id.editGyro_textView);
        mMouseSpeedText = mDialogContent.findViewById(R.id.editMouseSpeed_textView_percent);
        mGestureDelayText = mDialogContent.findViewById(R.id.editGestureDelay_textView_percent);
        mGestureDelayDisplayText = mDialogContent.findViewById(R.id.editGestureDelay_textView);
        mResolutionText = mDialogContent.findViewById(R.id.editResolution_textView_percent);
    }

    private void setupListeners() {
        mOriginalGyroEnabled = PREF_ENABLE_GYRO;
        mOriginalGyroXEnabled = PREF_GYRO_INVERT_X;
        mOriginalGyroYEnabled = PREF_GYRO_INVERT_Y;
        mOriginalGestureDisabled = PREF_DISABLE_GESTURES;

        mOriginalGyroSensitivity = PREF_GYRO_SENSITIVITY;
        mOriginalMouseSpeed = PREF_MOUSESPEED;
        mOriginalGestureDelay = PREF_LONGPRESS_TRIGGER;
        mOriginalResolution = PREF_SCALE_FACTOR;

        mGyroSwitch.setChecked(mOriginalGyroEnabled);
        mGyroXSwitch.setChecked(mOriginalGyroXEnabled);
        mGyroYSwitch.setChecked(mOriginalGyroYEnabled);
        mGestureSwitch.setChecked(mOriginalGestureDisabled);

        mGyroSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            PREF_ENABLE_GYRO = isChecked;
            onGyroStateChanged();
            updateGyroVisibility(isChecked);
            LauncherPreferences.DEFAULT_PREF.edit().putBoolean("enableGyro", isChecked).apply();
        });

        mGyroXSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            PREF_GYRO_INVERT_X = isChecked;
            onGyroStateChanged();
            LauncherPreferences.DEFAULT_PREF.edit().putBoolean("gyroInvertX", isChecked).apply();
        });

        mGyroYSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            PREF_GYRO_INVERT_Y = isChecked;
            onGyroStateChanged();
            LauncherPreferences.DEFAULT_PREF.edit().putBoolean("gyroInvertY", isChecked).apply();
        });

        mGestureSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            PREF_DISABLE_GESTURES = isChecked;
            updateGestureVisibility(isChecked);
            LauncherPreferences.DEFAULT_PREF.edit().putBoolean("disableGestures", isChecked).apply();
        });

        mGyroSensitivityBar.setRange(25, 300);
        mGyroSensitivityBar.setIncrement(5);
        mGyroSensitivityBar.setOnSeekBarChangeListener((SimpleSeekBarListener) (seekBar, progress, fromUser) -> {
            PREF_GYRO_SENSITIVITY = progress / 100f;
            LauncherPreferences.DEFAULT_PREF.edit().putInt("gyroSensitivity", progress).apply();
            mGyroSensitivityText.setText(progress + "%");
        });
        mGyroSensitivityBar.setProgress((int) (mOriginalGyroSensitivity * 100f));

        mMouseSpeedBar.setRange(25, 300);
        mMouseSpeedBar.setIncrement(5);
        mMouseSpeedBar.setOnSeekBarChangeListener((SimpleSeekBarListener) (seekBar, progress, fromUser) -> {
            PREF_MOUSESPEED = progress / 100f;
            LauncherPreferences.DEFAULT_PREF.edit().putInt("mousespeed", progress).apply();
            mMouseSpeedText.setText(progress + "%");
        });
        mMouseSpeedBar.setProgress((int) (mOriginalMouseSpeed * 100f));

        mGestureDelayBar.setRange(100, 1000);
        mGestureDelayBar.setIncrement(10);
        mGestureDelayBar.setOnSeekBarChangeListener((SimpleSeekBarListener) (seekBar, progress, fromUser) -> {
            PREF_LONGPRESS_TRIGGER = progress;
            LauncherPreferences.DEFAULT_PREF.edit().putInt("timeLongPressTrigger", progress).apply();
            mGestureDelayText.setText(progress + "ms");
        });
        mGestureDelayBar.setProgress(mOriginalGestureDelay);

        mResolutionBar.setRange(25, 100);
        mResolutionBar.setIncrement(5);
        mResolutionBar.setOnSeekBarChangeListener((SimpleSeekBarListener) (seekBar, progress, fromUser) -> {
            PREF_SCALE_FACTOR = progress/100f;
            LauncherPreferences.DEFAULT_PREF.edit().putInt("resolutionRatio", progress).apply();
            mResolutionText.setText(progress + "%");
            onResolutionChanged();
        });
        mResolutionBar.setProgress((int) (mOriginalResolution * 100));


        updateGyroVisibility(mOriginalGyroEnabled);
        updateGestureVisibility(mOriginalGestureDisabled);
    }

    private void updateGyroVisibility(boolean isEnabled) {
        int visibility = isEnabled ? View.VISIBLE : View.GONE;
        mGyroXSwitch.setVisibility(visibility);
        mGyroYSwitch.setVisibility(visibility);

        mGyroSensitivityBar.setVisibility(visibility);
        mGyroSensitivityText.setVisibility(visibility);
        mGyroSensitivityDisplayText.setVisibility(visibility);
    }

    private void updateGestureVisibility(boolean isDisabled) {
        int visibility = isDisabled ? View.GONE : View.VISIBLE;
        mGestureDelayBar.setVisibility(visibility);
        mGestureDelayText.setVisibility(visibility);
        mGestureDelayDisplayText.setVisibility(visibility);
    }

    private void removeListeners() {
        mGyroSwitch.setOnCheckedChangeListener(null);
        mGyroXSwitch.setOnCheckedChangeListener(null);
        mGyroYSwitch.setOnCheckedChangeListener(null);
        mGestureSwitch.setOnCheckedChangeListener(null);

        mGyroSensitivityBar.setOnSeekBarChangeListener(null);
        mMouseSpeedBar.setOnSeekBarChangeListener(null);
        mGestureDelayBar.setOnSeekBarChangeListener(null);
        mResolutionBar.setOnSeekBarChangeListener(null);
    }

    private void setupCancelButton() {
        setStartButtonListener(android.R.string.cancel, v -> cancel());
        setEndButtonListener(android.R.string.ok, v -> disappear(true));
    }

    /** Resets all settings to their original values */
    public void cancel() {
        // Reset all settings if we were editing
        if(isDisplaying()) {
            PREF_ENABLE_GYRO = mOriginalGyroEnabled;
            PREF_GYRO_INVERT_X = mOriginalGyroXEnabled;
            PREF_GYRO_INVERT_Y = mOriginalGyroYEnabled;
            PREF_DISABLE_GESTURES = mOriginalGestureDisabled;

            PREF_GYRO_SENSITIVITY = mOriginalGyroSensitivity;
            PREF_MOUSESPEED = mOriginalMouseSpeed;
            PREF_LONGPRESS_TRIGGER = mOriginalGestureDelay;
            PREF_SCALE_FACTOR = mOriginalResolution;

            onGyroStateChanged();
            onResolutionChanged();
        }

        disappear(true);
    }

    /** Called when the resolution is changed. Use {@link LauncherPreferences#PREF_SCALE_FACTOR} */
    public abstract void onResolutionChanged();

    /** Called when the gyro state is changed.
     * Use {@link LauncherPreferences#PREF_ENABLE_GYRO}
     * Use {@link LauncherPreferences#PREF_GYRO_INVERT_X}
     * Use {@link LauncherPreferences#PREF_GYRO_INVERT_Y}
     */
    public abstract void onGyroStateChanged();

}
