package com.flyzebra.ffplay.view;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import com.flyzebra.ffplay.R;
import com.flyzebra.utils.GlShaderUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Author: FlyZebra
 * Time: 18-5-14 下午9:00.
 * Discription: This is GlRender
 */
public class GlRenderI420 implements GLSurfaceView.Renderer {
    private final Context context;
    private final FloatBuffer vertexBuffer;
    private final FloatBuffer textureBuffer;
    //顶点坐标
    static float[] vertexData = {   // in counterclockwise order:
            -1f, -1f, // bottom left
            +1f, -1f, // bottom right
            -1f, +1f, // top left
            +1f, +1f,  // top right
    };
    //纹理坐标
    static float[] textureData = {   // in counterclockwise order:
            0.0f, 1.0f, // bottom left
            1.0f, 1.0f, // bottom right
            0.0f, 0.0f, // top left
            1.0f, 0.0f,  // top right
    };

    protected float[] vMatrixData = {
            1, 0, 0, 0,
            0, 1, 0, 0,
            0, 0, 1, 0,
            0, 0, 0, 1
    };

    private int glprogram;
    protected int vPosition;
    protected int fPosition;
    protected int vMatrix;
    private int sampler_y;
    private int sampler_u;
    private int sampler_v;
    private final int[] textureIds = new int[3];
    public int width = 0;
    public int height = 0;
    private ByteBuffer y;
    private ByteBuffer u;
    private ByteBuffer v;

    private final Object objectLock = new Object();

    public GlRenderI420(Context context) {
        this.context = context;

        vertexBuffer = ByteBuffer.allocateDirect(vertexData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertexData);
        vertexBuffer.position(0);

        textureBuffer = ByteBuffer.allocateDirect(textureData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(textureData);
        textureBuffer.position(0);
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
        y = ByteBuffer.wrap(new byte[this.width * this.height]);
        u = ByteBuffer.wrap(new byte[this.width * this.height / 4]);
        v = ByteBuffer.wrap(new byte[this.width * this.height / 4]);
    }

    public void upFrame(byte[] data, int size, int width, int height) {
        if (this.width != width || this.height != height) {
            setSize(width, height);
        }
        synchronized (objectLock) {
            y.put(data, 0, width * height);
            u.put(data, width * height, width * height / 4);
            v.put(data, width * height + width * height / 4, width * height / 4);
            y.flip();
            u.flip();
            v.flip();
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        String vertexShader = GlShaderUtil.readRawTextFile(context, R.raw.glsl_i420_vertex);
        String fragmentShader = GlShaderUtil.readRawTextFile(context, R.raw.glsl_i420_fragment);
        glprogram = GlShaderUtil.createProgram(vertexShader, fragmentShader);
        vPosition = GLES20.glGetAttribLocation(glprogram, "vPosition");
        fPosition = GLES20.glGetAttribLocation(glprogram, "fPosition");
        vMatrix = GLES20.glGetUniformLocation(glprogram, "vMatrix");
        sampler_y = GLES20.glGetUniformLocation(glprogram, "sampler_y");
        sampler_u = GLES20.glGetUniformLocation(glprogram, "sampler_u");
        sampler_v = GLES20.glGetUniformLocation(glprogram, "sampler_v");
        GLES20.glGenTextures(textureIds.length, textureIds, 0);
        for (int textureId : textureIds) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        }
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (width <= 0 || height <= 0) return;
        GLES20.glClearColor(0.0f, 0.70f, 0.0f, 1.0f);
        synchronized (objectLock) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[0]);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, width, height, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, y);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[1]);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, width / 2, height / 2, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, u);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[2]);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, width / 2, height / 2, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, v);
        }

        GLES20.glUseProgram(glprogram);
        GLES20.glUniformMatrix4fv(vMatrix, 1, false, vMatrixData, 0);
        GLES20.glUniform1i(sampler_y, 0);
        GLES20.glUniform1i(sampler_u, 1);
        GLES20.glUniform1i(sampler_v, 2);

        GLES20.glEnableVertexAttribArray(vPosition);
        GLES20.glEnableVertexAttribArray(fPosition);
        GLES20.glVertexAttribPointer(vPosition, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glVertexAttribPointer(fPosition, 2, GLES20.GL_FLOAT, false, 0, textureBuffer);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(vPosition);
        GLES20.glDisableVertexAttribArray(fPosition);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }
}
