package com.flyzebra.mdrvset.fragment;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.flyzebra.ffplay.view.GlVideoView;
import com.flyzebra.mdrvset.activity.MD600Activity;
import com.flyzebra.mdrvset.adapder.SpinnerAdapater;
import com.flyzebra.mdrvset.http.AdasInfo;
import com.flyzebra.mdrvset.http.BsdInfo;
import com.flyzebra.mdrvset.http.RtmpInfo;
import com.flyzebra.mdrvset.view.BsdSetView;
import com.flyzebra.mdvrset.R;
import com.flyzebra.utils.FlyLog;
import com.flyzebra.utils.GsonUtil;
import com.flyzebra.utils.WifiUtil;
import com.flyzebra.utils.http.HttpResult;
import com.flyzebra.utils.http.HttpUtil;

public class BsdSetFragment extends Fragment {
    private BsdInfo bsdInfo = new BsdInfo();
    private GlVideoView glVideoView;
    private RelativeLayout start_layout;
    private RelativeLayout line_layout;
    private Spinner channel_spinner;
    private BsdSetView bsdSetView;
    private Spinner bsd_camera_spinner1;
    private Spinner bsd_camera_spinner2;
    private ImageView bsd_save_btn;
    private Button calibration_start_btn;
    private TextView bsd_set_textinfo;

    private boolean is_connected = false;

    private int mLiveChannel = 0;

    private static final HandlerThread httpThread = new HandlerThread("http_thread");

    static {
        httpThread.start();
    }

    private static final Handler tHandler = new Handler(httpThread.getLooper());
    private static final Handler mHandler = new Handler(Looper.getMainLooper());

    private ProgressDialog progressDialog;

    public Runnable playTask = new Runnable() {
        @Override
        public void run() {
            MD600Activity activity = (MD600Activity) getActivity();
            if (activity == null) return;
            String gateway = WifiUtil.getGateway(activity);
            if (TextUtils.isEmpty(gateway)) {
                mHandler.post(() -> activity.showMessage(R.string.note_wifi_connected));
                return;
            }
            String str = String.format(RtmpInfo.GetRequest, mLiveChannel + 1);
            final HttpResult result = HttpUtil.doPostJson("http://" + gateway + "/bin-cgi/mlg.cgi", str);
            if (result.code == 200) {
                try {
                    RtmpInfo.GetRtmpResult getRtmpResult = GsonUtil.json2Object(result.data, RtmpInfo.GetRtmpResult.class);
                    if (getRtmpResult != null && getRtmpResult.LIVE_PREVIEW_RTMP != null && getRtmpResult.LIVE_PREVIEW_RTMP.size() > 0) {
                        String playUrl = getRtmpResult.LIVE_PREVIEW_RTMP.get(0).RTMP_ADDR;
                        mHandler.post(() -> glVideoView.play(playUrl));
                    } else {
                        tHandler.postDelayed(BsdSetFragment.this.playTask, 3000);
                    }
                } catch (Exception e) {
                    FlyLog.e(e.toString());
                    tHandler.postDelayed(BsdSetFragment.this.playTask, 3000);
                }
            } else {
                tHandler.postDelayed(BsdSetFragment.this.playTask, 3000);
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        progressDialog = new ProgressDialog(getActivity());
        progressDialog.setMessage(getActivity().getString(R.string.save_adas));
        return inflater.inflate(R.layout.fragment_bdsset, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        glVideoView = view.findViewById(R.id.gl_ffplay);
        start_layout = view.findViewById(R.id.fm_aiset_start_layout);
        line_layout = view.findViewById(R.id.fm_aiset_line_layout);
        channel_spinner = view.findViewById(R.id.bsd_channel_spinner);
        bsdSetView = view.findViewById(R.id.bsdSetView);
        bsd_camera_spinner1 = view.findViewById(R.id.bsd_camera_spinner1);
        bsd_camera_spinner2 = view.findViewById(R.id.bsd_camera_spinner2);
        bsd_save_btn = view.findViewById(R.id.bsd_save_btn);

        start_layout.setVisibility(View.VISIBLE);
        line_layout.setVisibility(View.INVISIBLE);

        channel_spinner.setAdapter(new SpinnerAdapater(getContext(), getResources().getStringArray(R.array.spinnerchannelnum)));
        channel_spinner.setSelection(0);
        channel_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mLiveChannel = position;
                tHandler.removeCallbacks(playTask);
                tHandler.post(playTask);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        bsdSetView.setMoveLisenter(bsdInfo -> {
            this.bsdInfo = bsdInfo;
            bsd_set_textinfo.setText(this.bsdInfo.toText());
        });

        bsd_camera_spinner1.setAdapter(new SpinnerAdapater(getContext(), getResources().getStringArray(R.array.bsdcamerachanenl)));
        bsd_camera_spinner1.setSelection(bsdInfo.BSD_CHN_INDEX);
        bsd_camera_spinner1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                bsdInfo.BSD_CHN_INDEX = position;
                updateView();
                checkConnected();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        bsd_camera_spinner2.setAdapter(new SpinnerAdapater(getContext(), getResources().getStringArray(R.array.bsdcameradirection)));
        bsd_camera_spinner2.setSelection(bsdInfo.reversed);
        bsd_camera_spinner2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                bsdInfo.reversed = position;
                updateView();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        bsd_save_btn.setOnClickListener(v -> {
            String gateway = WifiUtil.getGateway(getActivity());
            if (TextUtils.isEmpty(gateway)) {
                return;
            }
            BsdInfo.SetRequest setRequest = new BsdInfo.SetRequest();
            setRequest.DATA = bsdInfo;
            String setString = GsonUtil.objectToJson(setRequest);
            progressDialog.show();
            tHandler.post(() -> {
                final HttpResult result = HttpUtil.doPostJson("http://" + gateway + "/bin-cgi/mlg.cgi", setString);
                mHandler.post(() -> {
                    progressDialog.dismiss();
                    MD600Activity activity = (MD600Activity) getActivity();
                    if (activity == null) return;
                    if (result.code == 200) {
                        try {
                            AdasInfo.SetResult data = GsonUtil.json2Object(result.data, AdasInfo.SetResult.class);
                            if (!TextUtils.isEmpty(data.ErrNO) && data.ErrNO.equals("0000")) {
                                activity.showMessage(R.string.set_ok);
                            } else {
                                activity.showMessage(R.string.set_json_error);
                            }
                        } catch (Exception e) {
                            FlyLog.e(e.toString());
                            activity.showMessage(R.string.set_json_error);
                        }
                    } else {
                        activity.showMessage(R.string.set_error_network);
                    }
                });
            });
        });

        bsd_save_btn.setVisibility(View.INVISIBLE);
        calibration_start_btn = start_layout.findViewById(R.id.calibration_start_btn);
        calibration_start_btn.setOnClickListener(v -> {
            if (is_connected) {
                start_layout.setVisibility(View.INVISIBLE);
                line_layout.setVisibility(View.VISIBLE);
                bsd_save_btn.setVisibility(View.VISIBLE);
            } else {
                MD600Activity activity = (MD600Activity) getActivity();
                if (activity != null) {
                    activity.showMessage(R.string.note_wifi_connected);
                }
                checkConnected();
            }
        });

        bsd_set_textinfo = view.findViewById(R.id.bsd_set_textinfo);
        updateView();
    }

    private void updateView() {
        bsd_set_textinfo.setText(bsdInfo.toText());
        bsd_camera_spinner2.setSelection(bsdInfo.reversed);
        bsdSetView.upBsdInfo(bsdInfo);
    }

    @Override
    public void onResume() {
        super.onResume();
        checkConnected();
    }

    private void checkConnected() {
        is_connected = false;
        String gateway = WifiUtil.getGateway(getActivity());
        if (!TextUtils.isEmpty(gateway)) {
            String getString = String.format(BsdInfo.GetRequest, bsdInfo.BSD_CHN_INDEX);
            tHandler.removeCallbacksAndMessages(null);
            tHandler.post(() -> {
                final HttpResult result = HttpUtil.doPostJson("http://" + gateway + "/bin-cgi/mlg.cgi", getString);
                mHandler.removeCallbacksAndMessages(null);
                mHandler.post(() -> {
                    if (result.code == 200) {
                        try {
                            BsdInfo.GetResult data = GsonUtil.json2Object(result.data, BsdInfo.GetResult.class);
                            if (!TextUtils.isEmpty(data.ErrNO) && data.ErrNO.equals("0000")) {
                                bsdInfo = data.DATA;
                                updateView();
                                is_connected = true;
                            }
                        } catch (Exception e) {
                            FlyLog.e(e.toString());
                        }
                    }
                });
            });
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        tHandler.removeCallbacksAndMessages(null);
        mHandler.removeCallbacksAndMessages(null);
    }
}
