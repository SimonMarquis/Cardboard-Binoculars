package fr.smarquis.binoculars;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.Matrix;

import com.google.vrtoolkit.cardboard.CardboardView;
import com.google.vrtoolkit.cardboard.Eye;
import com.google.vrtoolkit.cardboard.HeadTransform;
import com.google.vrtoolkit.cardboard.Viewport;

import javax.microedition.khronos.egl.EGLConfig;

public class CustomCardboardView extends CardboardView implements CardboardView.StereoRenderer, SurfaceTexture.OnFrameAvailableListener {

    private final float[] mMVPMatrix = new float[16];

    private final float[] mProjectionMatrix = new float[16];

    private final float[] mViewMatrix = new float[16];

    private CameraPreview cameraPreview;

    public CustomCardboardView(Context context) {
        super(context);
        setEGLContextClientVersion(2);
        setRestoreGLStateEnabled(false);
        setRenderer(this);
        setAlignmentMarkerEnabled(false);
        setSettingsButtonEnabled(false);
        // setDistortionCorrectionEnabled(false);
        // setLowLatencyModeEnabled(true);
    }

    @Override
    public void onSurfaceCreated(EGLConfig eglConfig) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        cameraPreview = new CameraPreview(this);
    }


    @Override
    public void onSurfaceChanged(int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        float ratio = (float) 1080 / 1920;
        Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1, 1, 3, 7);
    }

    @Override
    public void onNewFrame(HeadTransform headTransform) {
        // Draw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Set the camera position (View matrix)
        Matrix.setLookAtM(mViewMatrix, 0, 0, 0, -3, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

        // Calculate the projection and view transformation
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);

        cameraPreview.onNewFrame();
    }

    @Override
    public void onDrawEye(Eye eye) {
        cameraPreview.onDraw(mMVPMatrix);
    }

    @Override
    public void onFinishFrame(Viewport viewport) {

    }

    @Override
    public void onRendererShutdown() {

    }

    @Override
    public void onResume() {
        super.onResume();
        if (cameraPreview != null) {
            cameraPreview.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (cameraPreview != null) {
            cameraPreview.onPause();
        }
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        requestRender();
    }

    public void onCardboardTrigger() {
        if (cameraPreview != null) {
            cameraPreview.onCardboardTrigger();
        }
    }
}
