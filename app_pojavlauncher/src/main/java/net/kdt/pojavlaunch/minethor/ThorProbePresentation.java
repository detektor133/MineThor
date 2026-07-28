package net.kdt.pojavlaunch.minethor;

import android.app.Presentation;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;

public class ThorProbePresentation extends Presentation {
    private static final String TAG = "MineThorProbe";

    public ThorProbePresentation(Context outerContext, Display display) {
        super(outerContext, display);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "Presentation created on display " + getDisplay().getDisplayId());
        setContentView(new ThorProbeView(getContext(), getDisplay()));
    }
}
