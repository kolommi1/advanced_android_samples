package cz.uhk.advanced_android_samples.object_visualization;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import cz.uhk.advanced_android_samples.object_visualization.oglutils.OGLBuffers;
import cz.uhk.advanced_android_samples.object_visualization.oglutils.OGLUtils;
import cz.uhk.advanced_android_samples.object_visualization.oglutils.ShaderUtils;
import cz.uhk.advanced_android_samples.object_visualization.oglutils.ToFloatArray;
import cz.uhk.advanced_android_samples.object_visualization.transforms.Mat4;
import cz.uhk.advanced_android_samples.object_visualization.transforms.Mat4PerspRH;
import cz.uhk.advanced_android_samples.object_visualization.transforms.Vec3D;

public class Renderer implements GLSurfaceView.Renderer {

    private MainActivity mainActivity;
    private int width, height;
    private int supportedOpenGLESVersion;
    private OGLBuffers buffers;
    private int shaderProgram, locationVPMat, locationTranslation;
    private Mat4 proj;
    private Vec3D translation;
    private double distance;

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

        //zapnutí depth testu
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        // posun krychle kousek nad Aruco značku
        translation = new Vec3D(0,0,2);
    }

    private void createBuffers(){
        // data vrcholů pro každou stranu krychle
        // 6 stran, každá strana 4 vrcholy
        // každý vrchol 6 hodnot - pozice xyz, barva RGB
        float[] vertexData = {
                // z-
                 1, -1, -1,	0, 0, 0,
                -1, -1, -1,	0, 0, 0,
                 1,  1, -1,	0, 0, 0,
                -1,  1, -1,	0, 0, 0,
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
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        GLES20.glUseProgram(shaderProgram);
        // předání view matice z MainActivity -> OpenGL ES -> shader
        GLES20.glUniformMatrix4fv(locationVPMat, 1, false,
                ToFloatArray.convert(mainActivity.getViewMatrix().mul(proj)), 0);

        // vzdálenost bodu od počátku [0,0,0]
        distance = Math.sqrt(Math.pow(mainActivity.getViewMatrix().get(3,0), 2)
                + Math.pow(mainActivity.getViewMatrix().get(3,1), 2)
                + Math.pow(mainActivity.getViewMatrix().get(3,2), 2))*10;
        // předání pozice objektu shaderu
        GLES20.glUniform3f(locationTranslation, (float) translation.getX(),(float) translation.getY(),(float) translation.getZ());

        // vykreslení
        buffers.draw(GLES20.GL_TRIANGLES, shaderProgram);
    }

    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height) {
        this.width = width;
        this.height = height;
        // nastavení viewportu
        GLES20.glViewport(0, 0, width, height);
        proj = new Mat4PerspRH(Math.PI / 4, height / (double) width, 0.01, 1000.0);
    }

    public double getDistance() {
        return distance;
    }
}
