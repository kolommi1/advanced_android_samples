package cz.uhk.advanced_android_samples.room_visualization.room_visualization;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import cz.uhk.advanced_android_samples.utils_library.oglutils.OGLBuffers;
import cz.uhk.advanced_android_samples.utils_library.oglutils.OGLModelOBJ;
import cz.uhk.advanced_android_samples.utils_library.oglutils.OGLTexture2D;
import cz.uhk.advanced_android_samples.utils_library.oglutils.OGLUtils;
import cz.uhk.advanced_android_samples.utils_library.oglutils.ShaderUtils;
import cz.uhk.advanced_android_samples.utils_library.oglutils.ToFloatArray;
import cz.uhk.advanced_android_samples.utils_library.transforms.Mat4;
import cz.uhk.advanced_android_samples.utils_library.transforms.Mat4PerspRH;
import cz.uhk.advanced_android_samples.utils_library.transforms.Vec3D;

public class Renderer implements GLSurfaceView.Renderer {

    private RoomVisualizationMainActivity  mainActivity;
    private int supportedOpenGLESVersion;
    private OGLBuffers buffers;
    private int shaderProgram, locationVPMat, locationTranslation, locationRotation;
    private Mat4 proj;
    private Vec3D translation;
    private double distance;
    private OGLModelOBJ roomModel;
    private OGLTexture2D roomTexture;

    Renderer(RoomVisualizationMainActivity mainActivity, int supportedOpenGLESVersion){
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

        locationVPMat = GLES20.glGetUniformLocation(shaderProgram, "vpMatrix");
        locationTranslation = GLES20.glGetUniformLocation(shaderProgram, "translation");
        locationRotation = GLES20.glGetUniformLocation(shaderProgram, "rotation");

        roomModel = new OGLModelOBJ(mainActivity, "objects/CubeRoom.obj");
        roomTexture = new OGLTexture2D(mainActivity, "objects/CubeRoom_BakedDiffuse.png");
        buffers = roomModel.getBuffers();

        //zapnutí depth testu
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }

    @Override
    public void onDrawFrame(GL10 glUnused) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        GLES20.glUseProgram(shaderProgram);
        // předání view matice z MainActivity -> OpenGL ES -> shader
        GLES20.glUniformMatrix4fv(locationVPMat, 1, false,
                ToFloatArray.convert(mainActivity.getViewMatrix().mul(proj)), 0);


        // předání pozice objektu shaderu
        if(mainActivity.getMarkerId() == 0){
            // posun místnosti (-1.8 vertikální střed místnosti)
            GLES20.glUniform3f(locationTranslation,0,-2.5f,4);
            // rotace místnosti
            GLES20.glUniform3f(locationRotation, 0,0,0);
        }
        else if(mainActivity.getMarkerId() == 1){
            GLES20.glUniform3f(locationTranslation, -4,-2.5f, 0);
            GLES20.glUniform3f(locationRotation, 0, (float)(90*Math.PI/180), 0);
        }
        else if(mainActivity.getMarkerId() == 2){
            GLES20.glUniform3f(locationTranslation, 0,-2.5f,-4);
            GLES20.glUniform3f(locationRotation, 0,  (float)(180*Math.PI/180), 0);
        }
        else {
            GLES20.glUniform3f(locationTranslation, 4,-2.5f,0);
            GLES20.glUniform3f(locationRotation, 0, (float)(270*Math.PI/180), 0);
        }

        roomTexture.bind(shaderProgram, "textureID", 0);

        // buffers.draw(GLES20.GL_LINES, shaderProgram);
        buffers.draw(roomModel.getTopology(), shaderProgram);
    }

    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height) {
        // nastavení viewportu
        GLES20.glViewport(0, 0, width, height);
        proj = new Mat4PerspRH(Math.PI / 4, height / (double) width, 0.01, 1000.0);
    }

}
