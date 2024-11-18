package com.kdt;

import static net.kdt.pojavlaunch.Tools.currentDisplayMetrics;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.CallSuper;
import androidx.annotation.IntegerRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.res.ResourcesCompat;

import net.kdt.pojavlaunch.R;

public class SideDialogView<T extends View> {

    private final ConstraintLayout mDialogLayout;
    private final DefocusableScrollView mScrollView;
    protected final T mDialogContent;
    private final int mMargin;
    private final ObjectAnimator mSideDialogAnimator;
    private boolean mDisplaying = false;

    private final Button mStartButton, mEndButton;
    private final TextView mTitleTextview;
    private final View mTitleDivider;

    public SideDialogView(Context context, ViewGroup parent, @LayoutRes int layoutId) {
        // Inflate layouts
        mDialogLayout = (ConstraintLayout) LayoutInflater.from(context).inflate(R.layout.dialog_side_dialog, parent, false);
        mScrollView = mDialogLayout.findViewById(R.id.side_dialog_scrollview);
        mStartButton = mDialogLayout.findViewById(R.id.side_dialog_start_button);
        mEndButton = mDialogLayout.findViewById(R.id.side_dialog_end_button);
        mTitleTextview = mDialogLayout.findViewById(R.id.side_dialog_title_textview);
        mTitleDivider = mDialogLayout.findViewById(R.id.side_dialog_title_divider);

        LayoutInflater.from(context).inflate(layoutId, mScrollView, true);
        mDialogContent = (T) mScrollView.getChildAt(0);



        // Attach layouts
        //mScrollView.addView(mDialogContent);
        parent.addView(mDialogLayout);

        mMargin = context.getResources().getDimensionPixelOffset(R.dimen._20sdp);
        mSideDialogAnimator = ObjectAnimator.ofFloat(mDialogLayout, "x", 0).setDuration(600);
        Interpolator decelerate = new AccelerateDecelerateInterpolator();
        mSideDialogAnimator.setInterpolator(decelerate);

        mDialogLayout.setElevation(10);
        mDialogLayout.setTranslationZ(10);

        mDialogLayout.setVisibility(View.VISIBLE);
        mDialogLayout.setBackground(ResourcesCompat.getDrawable(mDialogLayout.getResources(), R.drawable.background_control_editor, null));

        //TODO offset better according to view width
        mDialogLayout.setX(-mDialogLayout.getResources().getDimensionPixelOffset(R.dimen._280sdp));
    }

    public void setTitle(@StringRes int textId) {
        mTitleTextview.setText(textId);
        mTitleTextview.setVisibility(View.VISIBLE);
        mTitleDivider.setVisibility(View.VISIBLE);
    }

    public final void setStartButtonListener(@StringRes int textId, @Nullable View.OnClickListener listener) {
        setButton(mStartButton, textId, listener);
    }

    public final void setEndButtonListener(@StringRes int textId, @Nullable View.OnClickListener listener) {
        setButton(mEndButton, textId, listener);
    }

    private void setButton(@NonNull Button button, @StringRes int textId, @Nullable View.OnClickListener listener) {
        button.setText(textId);
        button.setOnClickListener(listener);
        button.setVisibility(View.VISIBLE);
    }



    /**
     * Slide the layout into the visible screen area
     * @return Whether the layout position has changed
     */
    @CallSuper
    public boolean appear(boolean fromRight) {
        boolean hasChanged = false;
        if (fromRight) {
            if (!mDisplaying || !isAtRight()) {
                mSideDialogAnimator.setFloatValues(currentDisplayMetrics.widthPixels, currentDisplayMetrics.widthPixels - mScrollView.getWidth() - mMargin);
                mSideDialogAnimator.start();
                hasChanged = true;
            }
        } else {
            if (!mDisplaying || isAtRight()) {
                mSideDialogAnimator.setFloatValues(-mDialogLayout.getWidth(), mMargin);
                mSideDialogAnimator.start();
                hasChanged = true;
            }
        }

        mDisplaying = true;
        return hasChanged;
    }

    private boolean isAtRight() {
        return mDialogLayout.getX() > currentDisplayMetrics.widthPixels / 2f;
    }

    /**
     * Slide out the layout
     */
    @CallSuper
    public void disappear() {
        if (!mDisplaying) return;

        mDisplaying = false;
        if (isAtRight())
            mSideDialogAnimator.setFloatValues(currentDisplayMetrics.widthPixels - mDialogLayout.getWidth() - mMargin, currentDisplayMetrics.widthPixels);
        else
            mSideDialogAnimator.setFloatValues(mMargin, -mDialogLayout.getWidth());

        mSideDialogAnimator.start();
    }
}
