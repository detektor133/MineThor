package net.kdt.pojavlaunch.minethor;

import android.app.Presentation;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;

public class ThorCompanionPresentation extends Presentation {
    private static final String TAG = "MineThorCompanion";
    private ThorCompanionController controller;

    public ThorCompanionPresentation(Context outerContext, Display display) {
        super(outerContext, display);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "Presentation created on display " + getDisplay().getDisplayId());
        CompanionStateProvider provider = new SocketCompanionStateProvider("127.0.0.1", 25566);
        controller = new ThorCompanionController(provider);
        setContentView(new ThorHudView(getContext(), controller));
    }

    @Override
    protected void onStop() {
        if (controller != null) controller.close();
        super.onStop();
    }
}
