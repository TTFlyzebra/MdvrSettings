package com.flyzebra.mdrvset.view.remote;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import com.flyzebra.core.notify.INotify;
import com.flyzebra.core.notify.Notify;
import com.flyzebra.core.notify.NotifyType;
import com.flyzebra.mdrvset.Config;
import com.flyzebra.mdrvset.bean.WifiP2PBean;
import com.flyzebra.utils.ByteUtil;

/**
 * Author: FlyZebra
 * Time: 18-5-14 下午9:00.
 * Discription: This is GlVideoView
 */
public class RcBaseView extends GLSurfaceView implements INotify {
    protected GLRender glRender;
    protected WifiP2PBean mdvrBean;
    protected int mWidth;
    protected int mHeight;
    protected int sWidth = Config.CAM_WIDTH;
    protected int sHeight = Config.CAM_HEIGHT;

    public RcBaseView(Context context) {
        this(context, null);
    }

    public RcBaseView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    protected void init(Context context) {
        setEGLContextClientVersion(3);
        glRender = new GLRender(context);
        setRenderer(glRender);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    public void setMdvrBean(WifiP2PBean mdvrBean) {
        this.mdvrBean = mdvrBean;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Notify.get().registerListener(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Notify.get().unregisterListener(this);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (width == 0 || height == 0) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        } else {
            if ((float) width / (float) sWidth > (float) height / (float) sHeight) {
                mHeight = height;
                mWidth = (int) (mHeight * sWidth / (float) sHeight);
            } else {
                mWidth = width;
                mHeight = (int) (mWidth * sHeight / (float) sWidth);
            }
            setMeasuredDimension(mWidth, mHeight);
        }
    }

    public void showDisconnect() {
        glRender.showDisconnect();
        requestRender();
    }

    @Override
    public void notify(byte[] data, int size) {

    }

    @Override
    public void handle(int type, byte[] data, int size, byte[] params) {
        long tid = ByteUtil.bytes2Long(params, 0, true);
        if (tid != mdvrBean.getTid()) return;
        if (type == NotifyType.NOTI_SCREEN_YUV) {
            int width = ByteUtil.bytes2Int(params, 8, true);
            int height = ByteUtil.bytes2Int(params, 12, true);
            glRender.upYuvData(data, 0, width, height, size);
            requestRender();
        }
    }
}