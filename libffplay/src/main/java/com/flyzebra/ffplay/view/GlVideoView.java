package com.flyzebra.ffplay.view;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.SurfaceHolder;

import com.flyzebra.ffplay.AudioPlayer;
import com.flyzebra.ffplay.FfPlayer;
import com.flyzebra.ffplay.IFfPlayer;
import com.flyzebra.utils.FlyLog;

import java.util.Arrays;


/**
 * Author: FlyZebra
 * Time: 18-5-14 下午9:00.
 * Discription: This is GlVideoView
 */
public class GlVideoView extends GLSurfaceView implements SurfaceHolder.Callback, IFfPlayer {
    private GlRenderI420 glRender = null;
    private FfPlayer ffplayer = null;
    private AudioPlayer audioPlayer = null;
    private String videoUrl = null;
    private boolean loop = true;

    public GlVideoView(Context context) {
        this(context, null);
    }

    public GlVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(width, (int) (width * 9f / 16f));
    }

    private void init(Context context) {
        setEGLContextClientVersion(2);
        glRender = new GlRenderI420(context);
        setRenderer(glRender);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        getHolder().addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        super.surfaceCreated(surfaceHolder);
        play(videoUrl);
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        super.surfaceChanged(surfaceHolder, i, i1, i2);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        super.surfaceDestroyed(surfaceHolder);
        stop();
    }

    @Override
    public void onVideoDecode(byte[] videoBytes, int size, int widht, int height) {
        if (ffplayer != null) glRender.upFrame(videoBytes, size, widht, height);
        requestRender();
    }

    @Override
    public void onAudioDecode(byte[] audioBytes, int size, int sampleRateInHz, int channelConfig, int audioFormat) {
        try {
            if (audioPlayer == null) {
                audioPlayer = new AudioPlayer(sampleRateInHz, channelConfig, audioFormat);
            }
            audioPlayer.write(audioBytes, size);
        } catch (Exception e) {
            FlyLog.e(e.toString());
        }
    }

    @Override
    public void onComplete() {
        stop();
        if (loop && !TextUtils.isEmpty(videoUrl)) {
            play(videoUrl);
        }
    }

    public void play(String url) {
        videoUrl = url;
        if (!TextUtils.isEmpty(videoUrl)) {
            if (ffplayer != null) ffplayer.stop();
            displayBlackGround();
            ffplayer = new FfPlayer();
            ffplayer.open(this, videoUrl);
        }
    }

    public void stop() {
        if (audioPlayer != null) {
            audioPlayer.stop();
            audioPlayer = null;
        }
        if (ffplayer != null) {
            ffplayer.stop();
            ffplayer = null;
        }
        displayBlackGround();
    }

    private void displayBlackGround() {
        if (glRender.width <= 0 || glRender.height <= 0) return;
        int size = glRender.width * glRender.height * 3 / 2;
        byte[] data = new byte[size];
        Arrays.fill(data, glRender.width * glRender.height, size, (byte) 128);
        glRender.upFrame(data, size, glRender.width, glRender.height);
        requestRender();
    }
}
