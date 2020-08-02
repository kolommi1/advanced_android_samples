package cz.uhk.advanced_android_samples.cardboard_stereo_view;

import android.opengl.GLES20;
import android.util.Log;

import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.GvrView;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;

import javax.microedition.khronos.egl.EGLConfig;

import cz.uhk.advanced_android_samples.utils_library.oglutils.OGLBuffers;
import cz.uhk.advanced_android_samples.utils_library.oglutils.OGLModelOBJ;
import cz.uhk.advanced_android_samples.utils_library.oglutils.OGLTexture2D;
import cz.uhk.advanced_android_samples.utils_library.oglutils.OGLUtils;
import cz.uhk.advanced_android_samples.utils_library.oglutils.ShaderUtils;
import cz.uhk.advanced_android_samples.utils_library.transforms.Camera;
import cz.uhk.advanced_android_samples.utils_library.transforms.Mat4;
import cz.uhk.advanced_android_samples.utils_library.transforms.Mat4Identity;
import cz.uhk.advanced_android_samples.utils_library.transforms.Mat4Transl;
import cz.uhk.advanced_android_samples.utils_library.transforms.Vec3D;

public class StereoViewRenderer implements GvrView.StereoRenderer  {
    private static final String TAG = "AAS_sv_OpenGL";

    private StereoViewMainActivity mainActivity;
    private int supportedOpenGLESVersion;

    private int shaderProgram;
    private int locMVP_Mat;
    private int framesPerSecond = 0;
    private long prevTime = 0;
    private long currentTime = 1000;
    
    private OGLModelOBJ roomModel;
    private OGLBuffers roomBuffers;
    private OGLTexture2D roomTexture;
    private Camera camera;

    private Mat4 matView;
    private Mat4 matMV;
    private Mat4 matMVP;
    private Mat4 modelRoom;

    StereoViewRenderer(StereoViewMainActivity mainActivity, int supportedOpenGLESVersion){
        this.supportedOpenGLESVersion = supportedOpenGLESVersion;
        this.mainActivity = mainActivity;
    }

    @Override
    public void onSurfaceCreated(EGLConfig eglConfig) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        // kontrola dostupnosti shaderů
        OGLUtils.shaderCheck(supportedOpenGLESVersion);

        // vytvoření programu shaderů
        shaderProgram = ShaderUtils.loadProgram(mainActivity, supportedOpenGLESVersion,  "shaders/simple_shader.vert",
                "shaders/simple_shader.frag", null);

        locMVP_Mat = GLES20.glGetUniformLocation(shaderProgram, "matMVP");

        //zapnutí depth testu
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        camera = new Camera();
        camera = camera.withPosition(new Vec3D(0, 0, 0))
                .withAzimuth(Math.PI/ 2 )
                .withZenith(Math.PI *3/2);
        matView = new Mat4();
        matMV = new Mat4();
        matMVP = new Mat4();
        modelRoom = new Mat4Identity();
        // 1.8m - vzdálenost zařízení od země
        modelRoom = modelRoom.mul(new Mat4Transl(0, -1.8f, 0));

        roomModel = new OGLModelOBJ(mainActivity, "objects/CubeRoom.obj");
        roomTexture = new OGLTexture2D(mainActivity, "objects/CubeRoom_BakedDiffuse.png");
        roomBuffers = roomModel.getBuffers();
    }

    @Override
    public void onSurfaceChanged(int i, int i1) {

    }

    @Override
    public void onNewFrame(HeadTransform headTransform) {

    }

    @Override
    public void onDrawEye(Eye eye) {

        // reset FPS po každé vteřině
        if (currentTime - prevTime >= 1000) {
            //Log.i(TAG, framesPerSecond + "");
            framesPerSecond = 0;
            prevTime = System.currentTimeMillis();
        }
        currentTime = System.currentTimeMillis();
        framesPerSecond += 1;

        //zapnutí depth testu - google carboard sdk za běhu mění hodnoty některých openGL parametrů
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // cameraView * eyeView
        matView = camera.getViewMatrix().mul(new Mat4(eye.getEyeView()));
        Mat4 perspective = new Mat4(eye.getPerspective(0.01f, 10.0f));// zNear, zFar

        // model * view * perspective
        matMV = modelRoom.mul(matView);
        matMVP = matMV.mul(perspective);
        // vykreslení
        GLES20.glUseProgram(shaderProgram);
        GLES20.glUniformMatrix4fv(locMVP_Mat, 1, false, matMVP.floatArray(), 0);
        roomTexture.bind(shaderProgram, "textureID", 0);

        roomBuffers.draw(roomModel.getTopology(), shaderProgram);
        OGLUtils.checkGLError("onDrawEye");
    }

    @Override
    public void onFinishFrame(Viewport viewport) {

    }

    @Override
    public void onRendererShutdown() {

    }

}
