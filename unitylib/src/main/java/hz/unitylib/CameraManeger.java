package hz.unitylib;

import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;

import java.io.IOException;

/**
 * Created by Administrator on 2017/11/6 0006.
 */

public class CameraManeger implements Camera.PreviewCallback{

    private Camera mCamera;
    private Camera.Parameters mParameters = null;
    private int mBufferSize = -1;
    private byte[] mBuffer;
    public int mWidth = 640;
    public int mHeight = 480;
    private YUVFrameCallback mYUVFrameCallback = null;

    public void open(SurfaceTexture surfaceTexture) {
        try {
            mCamera = android.hardware.Camera.open();

            mParameters = mCamera.getParameters();
            mParameters.setPreviewFormat(ImageFormat.NV21);
            int[] previewFpsRange = mParameters.getSupportedPreviewFpsRange().get(0);
            mParameters.setPreviewFpsRange(previewFpsRange[0], previewFpsRange[1]);
            mParameters.setFocusMode(android.hardware.Camera.Parameters.FOCUS_MODE_AUTO);
            mParameters.setPreviewSize(mWidth, mHeight);
            mCamera.setParameters(mParameters);
            mCamera.setPreviewTexture(surfaceTexture);
            mCamera.setDisplayOrientation(90);

            mBufferSize = mWidth * mHeight;
            mBufferSize *= ImageFormat.getBitsPerPixel(mParameters.getPreviewFormat()) / 8;
            mBuffer = new byte[mBufferSize];

            mCamera.startPreview();
            mCamera.addCallbackBuffer(mBuffer);
            mCamera.setPreviewCallbackWithBuffer(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {

        if ( mYUVFrameCallback != null )
            mYUVFrameCallback.onYUVFrames(bytes,bytes.length);

        camera.addCallbackBuffer(mBuffer);
    }

    // 保存视频帧
    public interface YUVFrameCallback {
        void onYUVFrames(byte[] data, int length);
    }

    public void setYUVFrameCallback(YUVFrameCallback mYUVFrameCallback) {
        this.mYUVFrameCallback = mYUVFrameCallback;
    }
}
