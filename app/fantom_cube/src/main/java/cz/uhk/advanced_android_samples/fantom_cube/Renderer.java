package cz.uhk.advanced_android_samples.fantom_cube;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import cz.uhk.advanced_android_samples.utils_library.oglutils.OGLBuffers;
import cz.uhk.advanced_android_samples.utils_library.oglutils.OGLUtils;
import cz.uhk.advanced_android_samples.utils_library.oglutils.ShaderUtils;
import cz.uhk.advanced_android_samples.utils_library.oglutils.ToFloatArray;
import cz.uhk.advanced_android_samples.utils_library.transforms.Mat4;
import cz.uhk.advanced_android_samples.utils_library.transforms.Mat4PerspRH;

public class Renderer implements GLSurfaceView.Renderer {

    private static final String TAG = "AAS_ov_OpenGL";
    private MainActivity mainActivity;
    private int supportedOpenGLESVersion;
    private OGLBuffers buffers;
    private int shaderProgram, locationVPMat, locationTranslation, locationRotation;
    private Mat4 proj;
    private int framesPerSecond = 0;
    private long prevTime = 0;
    private long currentTime = 1000;

    Renderer(MainActivity mainActivity, int supportedOpenGLESVersion){
        this.supportedOpenGLESVersion = supportedOpenGLESVersion;
        this.mainActivity = mainActivity;
    }

    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        // kontrola dostupnosti shaderů
        OGLUtils.shaderCheck(supportedOpenGLESVersion);

        // vytvoření programu shaderů
        // soubory shaderů jsou ve složce projektu /assets/
        shaderProgram = ShaderUtils.loadProgram(mainActivity, supportedOpenGLESVersion,  "shaders/simple_shader.vert",
                "shaders/simple_shader.frag", null);
        createBuffers();

        locationVPMat = GLES20.glGetUniformLocation(shaderProgram, "vpMatrix");
        locationTranslation = GLES20.glGetUniformLocation(shaderProgram, "translation");
        locationRotation = GLES20.glGetUniformLocation(shaderProgram, "rotation");

        //zapnutí depth testu
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }

    private void createBuffers(){
        // data vrcholů pro každou stranu krychle
        // 6 stran, každá strana 4 vrcholy
        // každý vrchol 6 hodnot - pozice xyz, barva RGB
        float[] vertexData = {
                // z-
                 1, -1, -1,	1, 0, 1,
                -1, -1, -1, 1, 0, 1,
                 1,  1, -1, 1, 0, 1,
                -1,  1, -1,	1, 0, 1,
                // z+
                 1, -1, 1,	0, 0, 1,
                -1, -1, 1,	0, 0, 1,
                 1,  1, 1,	0, 0, 1,
                -1,  1, 1,	0, 0, 1,
                // x+
                1,  1, -1,	1, 0, 0,
                1, -1, -1,	1, 0, 0,
                1,  1,  1,	1, 0, 0,
                1, -1,  1,	1, 0, 0,
                // x-
                -1,  1, -1,	1, 1, 0,
                -1, -1, -1,	1, 1, 0,
                -1,  1,  1,	1, 1, 0,
                -1, -1,  1,	1, 1, 0,
                // y+
                 1, 1, -1,	0, 1, 0,
                -1, 1, -1,	0, 1, 0,
                 1, 1,  1,	0, 1, 0,
                -1, 1,  1,	0, 1, 0,
                // y-
                 1, -1, -1,	0, 1, 1,
                -1, -1, -1,	0, 1, 1,
                 1, -1,  1,	0, 1, 1,
                -1, -1,  1,	0, 1, 1
        };

        // každá strana vykreslena pomocí 2 trojúhelníků
        // každý trojúhelník - 3 vrcholy
        short[] indexData = new short[36];
        for (int i = 0; i<6; i++){
            indexData[i*6] = (short)(i*4);
            indexData[i*6 + 1] = (short)(i*4 + 1);
            indexData[i*6 + 2] = (short)(i*4 + 2);
            indexData[i*6 + 3] = (short)(i*4 + 1);
            indexData[i*6 + 4] = (short)(i*4 + 2);
            indexData[i*6 + 5] = (short)(i*4 + 3);
        }

        OGLBuffers.Attrib[] attributes = {
                new OGLBuffers.Attrib("inPosition", 3),
                new OGLBuffers.Attrib("inColor", 3)
        };
        buffers = new OGLBuffers(vertexData, attributes, indexData);
    }

    @Override
    public void onDrawFrame(GL10 glUnused) {
        // reset FPS po každé vteřině
        if (currentTime - prevTime >= 1000) {
           // Log.i(TAG, framesPerSecond + "");
            framesPerSecond = 0;
            prevTime = System.currentTimeMillis();
        }
        currentTime = System.currentTimeMillis();
        framesPerSecond += 1;

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        GLES20.glUseProgram(shaderProgram);

        if(mainActivity.getHighestIndex()>=0){
            // předání view matice z MainActivity -> OpenGL ES -> shader
            GLES20.glUniformMatrix4fv(locationVPMat, 1, false,
                    ToFloatArray.convert(mainActivity.getViewMatrix().mul(proj)), 0);

            GLES20.glUniform3f(locationTranslation, 1f,0.9f,-1f);
            switch (mainActivity.getHighestIndex()){
                // FRONT
                case 0:
                    // předání rotace objektu do shaderu
                    GLES20.glUniform3f(locationRotation, 0,  0, 0);
                    // vykreslení
                    buffers.draw(GLES20.GL_TRIANGLES, shaderProgram);
                    break;
                // LEFT
                case 1:
                    GLES20.glUniform3f(locationRotation, 0,  (float)(90*Math.PI/180), 0);
                    buffers.draw(GLES20.GL_TRIANGLES, shaderProgram);
                    break;
                // BACK
                case 2:
                    GLES20.glUniform3f(locationRotation, 0,  (float)(180*Math.PI/180), 0);
                    buffers.draw(GLES20.GL_TRIANGLES, shaderProgram);
                    break;
                // RIGHT
                case 3:
                    GLES20.glUniform3f(locationRotation, 0,  (float)(270*Math.PI/180), 0);
                    buffers.draw(GLES20.GL_TRIANGLES, shaderProgram);
                    break;
                // TOP
                case 4:
                    GLES20.glUniform3f(locationRotation, (float)(+90*Math.PI/180),  0, 0);
                    buffers.draw(GLES20.GL_TRIANGLES, shaderProgram);
                    break;
                // BOTTOM
                case 5:
                    GLES20.glUniform3f(locationRotation, (float)(-90*Math.PI/180),  0, 0);
                    buffers.draw(GLES20.GL_TRIANGLES, shaderProgram);
                    break;
            }
        }
    }

    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height) {
        // nastavení viewportu
        GLES20.glViewport(0, 0, width, height);
        proj = new Mat4PerspRH(Math.PI / 4, height / (double) width, 0.01, 1000.0);
    }

}
