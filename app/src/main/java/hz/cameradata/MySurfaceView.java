package hz.cameradata;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import hz.unitylib.CameraManeger;

/**
 * Created by Administrator on 2017/11/6 0006.
 */

public class MySurfaceView extends GLSurfaceView implements CameraManeger.YUVFrameCallback {

    // 源视频帧宽/高
    private int srcFrameWidth  = 640;
    private int srcFrameHeight = 480;
    private int frameWidth = 640, frameHeight = 480;


    private ByteBuffer yBuf = null, uBuf = null, vBuf = null;
    private  int yFrameSize = 640*480;
    // 纹理id
    private int[] Ytexture = new int[1];
    private int[] Utexture = new int[1];
    private int[] Vtexture = new int[1];
    private int[] Mtexture = new int[1];
    private int aPositionMain = 0, aTexCoordMain = 0,  uYTextureMain = 0, uUTextureMain = 0, uVTextureMain = 0,uMTextureMain = 0;
    private int programHandleMain = 0;
    private static final int FLOAT_SIZE_BYTES = 4;

    private FloatBuffer squareVertices = null;
    private FloatBuffer coordVertices = null;
    private boolean mbpaly = false;

    public MySurfaceView(Context context) {
        this(context, null);
    }

    public MySurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setEGLContextClientVersion(2);
        setRenderer(new MyRenderer());
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        int uvFrameSize = yFrameSize >> 2;
        yBuf = ByteBuffer.allocateDirect(yFrameSize);
        yBuf.order(ByteOrder.nativeOrder()).position(0);

        uBuf = ByteBuffer.allocateDirect(uvFrameSize);
        uBuf.order(ByteOrder.nativeOrder()).position(0);

        vBuf = ByteBuffer.allocateDirect(uvFrameSize);
        vBuf.order(ByteOrder.nativeOrder()).position(0);

        // 顶点坐标
        squareVertices = ByteBuffer.allocateDirect(util.squareVertices.length * FLOAT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        squareVertices.put(util.squareVertices).position(0);
        //纹理坐标
        coordVertices = ByteBuffer.allocateDirect(util.coordVertices.length * FLOAT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        coordVertices.put(util.coordVertices).position(0);

        CameraManeger cameraManeger = new CameraManeger();
        cameraManeger.open(new SurfaceTexture(51));
        cameraManeger.setYUVFrameCallback(this);
    }

    @Override
    public void onYUVFrames(byte[] data, int length) {
        if (  length != 0 && mbpaly )
        {
            yBuf.clear();
            uBuf.clear();
            vBuf.clear();
            rotateYUV(data, srcFrameWidth, srcFrameHeight);
            requestRender();
        }
    }

    private class MyRenderer implements Renderer {

        @Override
        public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
            mbpaly = false;
            //设置背景的颜色
            GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
            //启动纹理
            GLES20.glEnable(GLES20.GL_TEXTURE_2D);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE3);

            initShader();
            //创建yuv纹理
            createTexture(frameWidth, frameHeight, GLES20.GL_LUMINANCE, Ytexture);
            createTexture(frameWidth>>1, frameHeight>>1, GLES20.GL_LUMINANCE, Utexture);
            createTexture(frameWidth >> 1, frameHeight >> 1, GLES20.GL_LUMINANCE, Vtexture);
            createTexture(frameWidth, frameHeight, GLES20.GL_RGBA, Mtexture);


            mbpaly = true;
        }

        @Override
        public void onSurfaceChanged(GL10 gl10, int width, int height) {
            GLES20.glViewport(0, 0, width, height);
        }

        @Override
        public void onDrawFrame(GL10 gl10) {
            // 重绘背景色
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

            if ( yBuf != null )
            {
                //y
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, Ytexture[0]);
                GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D,
                        0,
                        0,
                        0,
                        frameWidth,
                        frameHeight,
                        GLES20.GL_LUMINANCE,
                        GLES20.GL_UNSIGNED_BYTE,
                        yBuf);

                //u
                GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, Utexture[0]);
                GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D,
                        0,
                        0,
                        0,
                        frameWidth >> 1,
                        frameHeight >> 1,
                        GLES20.GL_LUMINANCE,
                        GLES20.GL_UNSIGNED_BYTE,
                        uBuf);

                //v
                GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, Vtexture[0]);
                GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D,
                        0,
                        0,
                        0,
                        frameWidth >> 1,
                        frameHeight >> 1,
                        GLES20.GL_LUMINANCE,
                        GLES20.GL_UNSIGNED_BYTE,
                        vBuf);

                //mark图层
                GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, Mtexture[0]);
                GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, frameWidth, frameHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
            }

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        }

        private void initShader() {
            programHandleMain = util.createShaderProgram();
            if ( programHandleMain != -1 )
            {
                // 获取VertexShader变量
                aPositionMain = getShaderHandle(programHandleMain, "vPosition");
                aTexCoordMain = getShaderHandle(programHandleMain, "a_texCoord");
                // 获取FrameShader变量
                uYTextureMain = getShaderHandle(programHandleMain, "SamplerY");
                uUTextureMain = getShaderHandle(programHandleMain, "SamplerU");
                uVTextureMain = getShaderHandle(programHandleMain, "SamplerV");
                uMTextureMain = getShaderHandle(programHandleMain, "SamplerM");

                // 使用滤镜着色器程序
                GLES20.glUseProgram(programHandleMain);

                //给变量赋值
                GLES20.glUniform1i(uYTextureMain, 0);
                GLES20.glUniform1i(uUTextureMain, 1);
                GLES20.glUniform1i(uVTextureMain, 2);
                GLES20.glUniform1i(uMTextureMain, 3);
                GLES20.glEnableVertexAttribArray(aPositionMain);
                GLES20.glEnableVertexAttribArray(aTexCoordMain);

                // 设置Vertex Shader数据
                squareVertices.position(0);
                GLES20.glVertexAttribPointer(aPositionMain, 2, GLES20.GL_FLOAT, false, 0, squareVertices);
                coordVertices.position(0);
                GLES20.glVertexAttribPointer(aTexCoordMain, 2, GLES20.GL_FLOAT, false, 0, coordVertices);
            }
        }

        // 创建纹理
        private void createTexture(int width, int height, int format, int[] textureId) {
            //创建纹理
            GLES20.glGenTextures(1, textureId, 0);
            //绑定纹理
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[0]);
            //设置纹理属性
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, format, width, height, 0, format, GLES20.GL_UNSIGNED_BYTE, null);
        }

    }

    public  int  getShaderHandle(int programHandle,String name) {
        int handle = GLES20.glGetAttribLocation(programHandle, name);
        if (handle == -1)
        {
            handle = GLES20.glGetUniformLocation(programHandle, name);
        }
        return handle;
    }

    public void rotateYUV(byte[] src,int width,int height) {
        byte [] yArray = new  byte[yBuf.limit()];
        byte [] uArray = new  byte[uBuf.limit()];
        byte [] vArray = new  byte[vBuf.limit()];
        int nFrameSize = width * height;
        int k          = 0;
        int uvCount    = nFrameSize>>1;

        //取分量y值
        for(int i = 0;i < height*width;i++ )
        {
            yArray[ k ] = src[ i ];
            k++;
        }

        k = 0;

        //取分量uv值
        for( int i = 0;i < uvCount ;i+=2 )
        {
            vArray[ k ] = src[ nFrameSize +  i ]; //v
            uArray[ k ] = src[ nFrameSize +  i + 1 ];//u
            k++;
        }

        yBuf.put(yArray).position(0);
        uBuf.put(uArray).position(0);
        vBuf.put(vArray).position(0);
    }

}
