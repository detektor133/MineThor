package net.kdt.pojavlaunch.minethor;

import android.app.Presentation;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;

public class ThorCompanionPresentation extends Presentation {
    private static final String TAG = "MineThorCompanion";

    public ThorCompanionPresentation(Context outerContext, Display display) {
        super(outerContext, display);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "Presentation created on display " + getDisplay().getDisplayId());
        CompanionStateProvider provider = new MockCompanionStateProvider();
        setContentView(new ThorHudView(getContext(), new ThorCompanionController(provider)));
    }
}
