package fr.smarquis.binoculars;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

public class CameraPreview {
    private static final int GL_TEXTURE_EXTERNAL_OES = 0x8D65;

    private final String vertexShaderCode =
            // This matrix member variable provides a hook to manipulate
            // the coordinates of the objects that use this vertex shader
            "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 vPosition;" +
                    "attribute vec2 inputTextureCoordinate;" +
                    "varying vec2 textureCoordinate;" +
                    "void main() {" +
                    // The matrix must be included as a modifier of gl_Position.
                    // Note that the uMVPMatrix factor *must be first* in order
                    // for the matrix multiplication product to be correct.
                    "  gl_Position = uMVPMatrix * vPosition;" +
                    "  textureCoordinate = inputTextureCoordinate;" +
                    "}";

    private final String fragmentShaderCode =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;" +
                    "varying vec2 textureCoordinate;" +
                    "uniform samplerExternalOES s_texture;" +
                    "void main() {" +
                    "  gl_FragColor = texture2D( s_texture, textureCoordinate );" +
                    "}";
    private final FloatBuffer vertexBuffer, textureVerticesBuffer;

    private final ShortBuffer drawListBuffer;

    private final int mProgram;

    private int mPositionHandle;

    private int mTextureHandle;

    private int mInputTextureCoordinatesHandle;

    private int mMVPMatrixHandle;

    static final int COORDS_PER_VERTEX = 3;

    static final int VIDEO_COORDS_PER_VERTEX = 2;

    static float squareCoords[] = { // in counterclockwise order:
            -1.0f, -1.0f, 0.0f,   // 0.left - mid
            1.0f, -1.0f, 0.0f,  // 1. right - mid
            -1.0f, 1.0f, 0.0f,  // 2. left - top
            1.0f, 1.0f, 0.0f // 3. right - top
    };

    static float textureVertices[] = {
            1.0f, 1.0f,
            0.0f, 1.0f,
            1.0f, 0.0f,
            0.0f, 0.0f
    };

    private final short drawOrder[] = {0, 2, 1, 1, 2, 3}; // order to draw vertices

    private final int vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex
    private final int videoVertexStride = VIDEO_COORDS_PER_VERTEX * 4; // 4 bytes per vertex

    private Camera camera;
    private SurfaceTexture surfaceTexture;
    private int texture;

    public CameraPreview(SurfaceTexture.OnFrameAvailableListener listener) {
        // initialize vertex byte buffer for shape coordinates
        ByteBuffer bb = ByteBuffer.allocateDirect(
                // (# of coordinate values * 4 bytes per float)
                squareCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(squareCoords);
        vertexBuffer.position(0);

        // initialize byte buffer for the draw list
        ByteBuffer dlb = ByteBuffer.allocateDirect(
                // (# of coordinate values * 2 bytes per short)
                drawOrder.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(drawOrder);
        drawListBuffer.position(0);

        ByteBuffer bb2 = ByteBuffer.allocateDirect(textureVertices.length * 4);
        bb2.order(ByteOrder.nativeOrder());
        textureVerticesBuffer = bb2.asFloatBuffer();
        textureVerticesBuffer.put(textureVertices);
        textureVerticesBuffer.position(0);

        // prepare shaders and OpenGL program
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        mProgram = GLES20.glCreateProgram();             // create empty OpenGL Program
        GLES20.glAttachShader(mProgram, vertexShader);   // add the vertex shader to program
        GLES20.glAttachShader(mProgram, fragmentShader); // add the fragment shader to program
        GLES20.glLinkProgram(mProgram);                  // create OpenGL program executables

        texture = createGLTexture();
        surfaceTexture = new SurfaceTexture(texture);
        surfaceTexture.setOnFrameAvailableListener(listener);

        openCamera();
    }

    private static int createGLTexture() {
        int[] texture = new int[1];
        GLES20.glGenTextures(1, texture, 0);
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, texture[0]);
        GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
        return texture[0];
    }

    public void onNewFrame() {
        surfaceTexture.updateTexImage();
    }

    private void openCamera() {
        camera = Camera.open();
        Camera.Parameters parameters = camera.getParameters();
        Log.d("Camera Parameters", parameters.flatten());
        parameters.setPreviewSize(1920, 1080);
        List<int[]> ranges = parameters.getSupportedPreviewFpsRange();
        int[] range = ranges.get(ranges.size() - 1);
        parameters.setPreviewFpsRange(range[0], range[1]);
        if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        }
        if (parameters.isVideoStabilizationSupported()) {
            parameters.setVideoStabilization(false);
        }
        camera.setParameters(parameters);
        try {
            camera.setPreviewTexture(surfaceTexture);
            camera.startPreview();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private void releaseCamera() {
        if (camera != null) {
            try {
                camera.setPreviewTexture(null);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    public void onResume() {
        openCamera();
    }

    public void onPause() {
        releaseCamera();
    }


    /**
     * Utility method for compiling a OpenGL shader.
     * <p/>
     * <p><strong>Note:</strong> When developing shaders, use the checkGlError()
     * method to debug shader coding errors.</p>
     *
     * @param type       - Vertex or fragment shader type.
     * @param shaderCode - String containing the shader code.
     * @return - Returns an id for the shader.
     */
    public static int loadShader(int type, String shaderCode) {
        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        int shader = GLES20.glCreateShader(type);
        // add the source code to the shader and compile it
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }

    /**
     * Utility method for debugging OpenGL calls. Provide the name of the call
     * just after making it:
     * <p/>
     * <pre>
     * mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
     * MyGLRenderer.checkGlError("glGetUniformLocation");</pre>
     *
     * If the operation is not successful, the check throws an error.
     *
     * @param glOperation - Name of the OpenGL call to check.
     */
    public static void checkGlError(String glOperation) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e("", glOperation + ": glError " + error);
            throw new RuntimeException(glOperation + ": glError " + error);
        }
    }

    public void onDraw(float[] matrix) {
        // Add program to OpenGL environment
        GLES20.glUseProgram(mProgram);

        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, texture);

        // get handle to vertex shader's vPosition member
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);


        mInputTextureCoordinatesHandle = GLES20.glGetAttribLocation(mProgram, "inputTextureCoordinate");
        GLES20.glEnableVertexAttribArray(mInputTextureCoordinatesHandle);
        GLES20.glVertexAttribPointer(mInputTextureCoordinatesHandle, VIDEO_COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, videoVertexStride, textureVerticesBuffer);

        // get handle to fragment shader's vColor member
        mTextureHandle = GLES20.glGetAttribLocation(mProgram, "s_texture");

        // get handle to shape's transformation matrix
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        checkGlError("glGetUniformLocation");

        // Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, matrix, 0);
        checkGlError("glUniformMatrix4fv");

        // Draw the square
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.length, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mInputTextureCoordinatesHandle);
    }

    public void onCardboardTrigger() {
        if (camera == null || !camera.getParameters().isZoomSupported()) {
            return;
        }
        Camera.Parameters parameters = camera.getParameters();
        int maxZoom = parameters.getMaxZoom();
        int value = parameters.getZoom() == maxZoom ? 0 : maxZoom;
        if (parameters.isSmoothZoomSupported()) {
            camera.startSmoothZoom(value);
        } else {
            parameters.setZoom(value);
            camera.setParameters(parameters);
        }
    }
}
