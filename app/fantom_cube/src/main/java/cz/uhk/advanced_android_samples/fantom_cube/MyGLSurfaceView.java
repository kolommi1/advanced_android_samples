package cz.uhk.advanced_android_samples.fantom_cube;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

public class MyGLSurfaceView extends GLSurfaceView {

    private cz.uhk.advanced_android_samples.fantom_cube.Renderer renderer;

    public MyGLSurfaceView(Context context){
        super(context);
    }

    public MyGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    // vlastní třída rendereru
    public void setRenderer(cz.uhk.advanced_android_samples.fantom_cube.Renderer renderer) {
        this.renderer = renderer;
        super.setRenderer(renderer);
    }
}
