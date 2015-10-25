package fr.smarquis.binoculars;

import android.content.Context;

import com.google.vrtoolkit.cardboard.CardboardView;
import com.google.vrtoolkit.cardboard.Eye;
import com.google.vrtoolkit.cardboard.HeadTransform;
import com.google.vrtoolkit.cardboard.Viewport;

import javax.microedition.khronos.egl.EGLConfig;

public class CustomCardboardView extends CardboardView implements CardboardView.StereoRenderer {

    public CustomCardboardView(Context context) {
        super(context);
        setEGLContextClientVersion(2);
        setRestoreGLStateEnabled(false);
        setRenderer((StereoRenderer) this);
    }

    @Override
    public void onSurfaceCreated(EGLConfig eglConfig) {

    }

    @Override
    public void onSurfaceChanged(int width, int height) {

    }

    @Override
    public void onNewFrame(HeadTransform headTransform) {

    }

    @Override
    public void onDrawEye(Eye eye) {

    }

    @Override
    public void onFinishFrame(Viewport viewport) {

    }

    @Override
    public void onRendererShutdown() {

    }
}
