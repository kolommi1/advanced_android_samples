package cz.uhk.advanced_android_samples.cardboard_stereo_view;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.os.Bundle;
import android.util.Log;

import com.google.vr.sdk.base.GvrActivity;
import com.google.vr.sdk.base.GvrView;

public class StereoViewMainActivity extends GvrActivity {
    private static final String TAG = "AAS_stereo_view";

    private StereoViewRenderer renderer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stereo_view_main);

        // Kontrola podpory OpenGL ES 2.0/3.0
        ConfigurationInfo configurationInfo = ((ActivityManager)getSystemService(Context.ACTIVITY_SERVICE)).getDeviceConfigurationInfo();
        final int supportedOpenGLESVersion = configurationInfo.reqGlEsVersion;

        // nastavení OpenGL view
        GvrView gvrOpenGLView = findViewById(R.id.gvr_view);
        gvrOpenGLView.setEGLConfigChooser(8, 8, 8, 8, 16, 8);
        if (supportedOpenGLESVersion >= 0x30000)  {
            Log.i(TAG, "Vytvořen OpenGL ES "+3 + ".0 kontext");
            gvrOpenGLView.setEGLContextClientVersion(3);
            renderer = new StereoViewRenderer(this, supportedOpenGLESVersion);
            gvrOpenGLView.setRenderer(renderer);
        }
        else if (supportedOpenGLESVersion >= 0x20000) {
            Log.i(TAG, "Vytvořen OpenGL ES"+2 + ".0 kontext");
            gvrOpenGLView.setEGLContextClientVersion(2);
            renderer = new StereoViewRenderer(this, supportedOpenGLESVersion);
            gvrOpenGLView.setRenderer(renderer);
        }
        else {
            throw new RuntimeException("Zařízení nepodporuje OpenGL ES 2.0");
        }

        setGvrView(gvrOpenGLView);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onCardboardTrigger() {
        Log.i(TAG, "Google carboard trigger");
    }
}
