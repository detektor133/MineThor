package net.kdt.pojavlaunch.minethor;

import android.app.Presentation;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Window;
import android.view.WindowManager;

public class ThorCompanionPresentation extends Presentation {
    private static final String TAG = "MineThorCompanion";
    private ThorCompanionController controller;

    public ThorCompanionPresentation(Context outerContext, Display display) {
        super(outerContext, display);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        if (window != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        }
        Log.i(TAG, "Presentation created on display " + getDisplay().getDisplayId());
        CompanionStateProvider provider = new SocketCompanionStateProvider(CompanionEndpoint.LOOPBACK_HOST, CompanionEndpoint.PORT);
        controller = new ThorCompanionController(provider);
        setContentView(new ThorHudView(getContext(), controller));
    }

    @Override
    protected void onStop() {
        if (controller != null) controller.close();
        super.onStop();
    }
}
