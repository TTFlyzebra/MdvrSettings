package com.flyzebra.mdrvset.fm;

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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.flyzebra.ffplay.view.GlVideoView;
import com.flyzebra.mdrvset.Config;
import com.flyzebra.mdrvset.activity.ArcsoftSetActivity;
import com.flyzebra.mdrvset.http.AdasInfo;
import com.flyzebra.mdrvset.http.RtmpInfo;
import com.flyzebra.mdrvset.view.AdasSetView;
import com.flyzebra.mdvrset.R;
import com.flyzebra.utils.FlyLog;
import com.flyzebra.utils.GsonUtil;
import com.flyzebra.utils.WifiUtil;
import com.flyzebra.utils.http.HttpResult;
import com.flyzebra.utils.http.HttpUtil;

public class AdasSetFragment extends Fragment {
    private AdasInfo adasInfo = new AdasInfo();
    private GlVideoView glVideoView;
    private RelativeLayout start_layout;
    private RelativeLayout line_layout;
    private Spinner channel_spinner;
    private AdasSetView adasSetView;

    private TextView adas_cali_horizon_text;
    private TextView adas_cali_carMiddle_text;
    private TextView adas_cali_cameraHeight_text;
    private TextView adas_cali_cameraToAxle_text;
    private TextView adas_cali_carWidth_text;
    private TextView adas_cali_cameraToBumper_text;
    private TextView adas_cali_cameraToLeftWheel_text;

    private ImageButton adas_cali_horizont_up;
    private ImageButton adas_cali_horizont_down;
    private ImageButton adas_cali_carMiddle_left;
    private ImageButton adas_cali_carMiddle_right;
    private ImageButton adas_cali_cameraHeight_left;
    private ImageButton adas_cali_cameraHeight_right;
    private ImageButton adas_cali_cameraToAxle_left;
    private ImageButton adas_cali_cameraToAxle_right;
    private ImageButton adas_cali_carWidth_left;
    private ImageButton adas_cali_carWidth_right;
    private ImageButton adas_cali_cameraToBumper_left;
    private ImageButton adas_cali_cameraToBumper_right;
    private ImageButton adas_cali_cameraToLeftWheel_left;
    private ImageButton adas_cali_cameraToLeftWheel_right;

    private ImageView adas_save_btn;
    private Button calibration_start_btn;
    private boolean is_connected = false;

    private int mLiveChannel = 0;
    public Runnable playTask = new Runnable() {
        @Override
        public void run() {
            ArcsoftSetActivity activity = (ArcsoftSetActivity) getActivity();
            if (activity == null) return;
            String gateway = WifiUtil.getGateway(activity);
            if (TextUtils.isEmpty(gateway)) {
                mHandler.post(() -> activity.showMessage(R.string.note_wifi_connected));
                return;
            }
            String str = String.format(RtmpInfo.GetRequest, mLiveChannel);
            final HttpResult result = HttpUtil.doPostJson("http://" + gateway + "/bin-cgi/mlg.cgi", str);
            if (result.code == 200) {
                try {
                    RtmpInfo.GetRtmpResult getRtmpResult = GsonUtil.json2Object(result.data, RtmpInfo.GetRtmpResult.class);
                    if (getRtmpResult != null && getRtmpResult.LIVE_PREVIEW_RTMP != null && getRtmpResult.LIVE_PREVIEW_RTMP.size() > 0) {
                        String playUrl = getRtmpResult.LIVE_PREVIEW_RTMP.get(0).RTMP_ADDR;
                        mHandler.post(() -> glVideoView.play(playUrl));
                    } else {
                        tHandler.postDelayed(AdasSetFragment.this.playTask, 2000);
                    }
                } catch (Exception e) {
                    FlyLog.e(e.toString());
                    tHandler.postDelayed(AdasSetFragment.this.playTask, 2000);
                }
            } else {
                tHandler.postDelayed(AdasSetFragment.this.playTask, 2000);
            }
        }
    };

    private static final HandlerThread httpThread = new HandlerThread("http_thread");

    static {
        httpThread.start();
    }

    private static final Handler tHandler = new Handler(httpThread.getLooper());
    private static final Handler mHandler = new Handler(Looper.getMainLooper());

    private ProgressDialog progressDialog;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_adasset, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        progressDialog = new ProgressDialog(getActivity());
        progressDialog.setMessage(getActivity().getString(R.string.save_adas));

        glVideoView = view.findViewById(R.id.gl_ffplay);
        start_layout = view.findViewById(R.id.fm_aiset_start_layout);
        line_layout = view.findViewById(R.id.fm_aiset_line_layout);
        start_layout.setVisibility(View.VISIBLE);
        line_layout.setVisibility(View.INVISIBLE);

        channel_spinner = view.findViewById(R.id.adas_channel_spinner);
        adasSetView = view.findViewById(R.id.adasSetView);

        adas_cali_horizon_text = view.findViewById(R.id.adas_cali_horizon_text);
        adas_cali_carMiddle_text = view.findViewById(R.id.adas_cali_carMiddle_text);
        adas_cali_cameraHeight_text = view.findViewById(R.id.adas_cali_cameraHeight_text);
        adas_cali_cameraToAxle_text = view.findViewById(R.id.adas_cali_cameraToAxle_text);
        adas_cali_carWidth_text = view.findViewById(R.id.adas_cali_carWidth_text);
        adas_cali_cameraToBumper_text = view.findViewById(R.id.adas_cali_cameraToBumper_text);
        adas_cali_cameraToLeftWheel_text = view.findViewById(R.id.adas_cali_cameraToLeftWheel_text);
        adas_cali_horizont_up = view.findViewById(R.id.adas_cali_horizont_up);
        adas_cali_horizont_down = view.findViewById(R.id.adas_cali_horizont_down);
        adas_cali_carMiddle_left = view.findViewById(R.id.adas_cali_carMiddle_left);
        adas_cali_carMiddle_right = view.findViewById(R.id.adas_cali_carMiddle_right);
        adas_cali_cameraHeight_left = view.findViewById(R.id.adas_cali_cameraHeight_left);
        adas_cali_cameraHeight_right = view.findViewById(R.id.adas_cali_cameraHeight_right);
        adas_cali_cameraToAxle_left = view.findViewById(R.id.adas_cali_cameraToAxle_left);
        adas_cali_cameraToAxle_right = view.findViewById(R.id.adas_cali_cameraToAxle_right);
        adas_cali_carWidth_left = view.findViewById(R.id.adas_cali_carWidth_left);
        adas_cali_carWidth_right = view.findViewById(R.id.adas_cali_carWidth_right);
        adas_cali_cameraToBumper_left = view.findViewById(R.id.adas_cali_cameraToBumper_left);
        adas_cali_cameraToBumper_right = view.findViewById(R.id.adas_cali_cameraToBumper_right);
        adas_cali_cameraToLeftWheel_left = view.findViewById(R.id.adas_cali_cameraToLeftWheel_left);
        adas_cali_cameraToLeftWheel_right = view.findViewById(R.id.adas_cali_cameraToLeftWheel_right);

        adas_save_btn = view.findViewById(R.id.adas_save_btn);

        channel_spinner.setSelection(0);
        channel_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                glVideoView.stop();
                mLiveChannel = position + 1;
                tHandler.removeCallbacks(playTask);
                tHandler.post(playTask);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        adasSetView.setMoveLisenter(adasInfo -> {
            this.adasInfo = adasInfo;
            adas_cali_horizon_text.setText(String.valueOf(this.adasInfo.horizon));
            adas_cali_carMiddle_text.setText(String.valueOf(this.adasInfo.carMiddle));
        });

        adas_cali_horizon_text.setOnClickListener(v -> showDialog((TextView) v, R.string.horizontal_line2, R.id.adas_cali_horizon_text));
        adas_cali_carMiddle_text.setOnClickListener(v -> showDialog((TextView) v, R.string.car_central_line2, R.id.adas_cali_carMiddle_text));
        adas_cali_cameraHeight_text.setOnClickListener(v -> showDialog((TextView) v, R.string.camera_height, R.id.adas_cali_cameraHeight_text));
        adas_cali_cameraToAxle_text.setOnClickListener(v -> showDialog((TextView) v, R.string.camera_to_axle, R.id.adas_cali_cameraToAxle_text));
        adas_cali_carWidth_text.setOnClickListener(v -> showDialog((TextView) v, R.string.car_width, R.id.adas_cali_carWidth_text));
        adas_cali_cameraToBumper_text.setOnClickListener(v -> showDialog((TextView) v, R.string.camera_to_bumper, R.id.adas_cali_cameraToBumper_text));
        adas_cali_cameraToLeftWheel_text.setOnClickListener(v -> showDialog((TextView) v, R.string.camera_to_left_wheel, R.id.adas_cali_cameraToLeftWheel_text));

        adas_cali_horizont_up.setOnClickListener(v -> {
            if (adasInfo.horizon > 0) {
                adasInfo.horizon--;
                adas_cali_horizon_text.setText(String.valueOf(adasInfo.horizon));
                adasSetView.updateView();
            }
        });
        adas_cali_horizont_down.setOnClickListener(v -> {
            if (adasInfo.horizon < (Config.CAM_HEIGHT - 1)) {
                adasInfo.horizon++;
                adas_cali_horizon_text.setText(String.valueOf(adasInfo.horizon));
                adasSetView.updateView();
            }
        });

        adas_cali_carMiddle_left.setOnClickListener(v -> {
            if (adasInfo.carMiddle > (-Config.CAM_WIDTH / 2) - 1) {
                adasInfo.carMiddle--;
                adas_cali_carMiddle_text.setText(String.valueOf(adasInfo.carMiddle));
                adasSetView.updateView();
            }
        });
        adas_cali_carMiddle_right.setOnClickListener(v -> {
            if (adasInfo.carMiddle < (Config.CAM_WIDTH) / 2 - 1) {
                adasInfo.carMiddle++;
                adas_cali_carMiddle_text.setText(String.valueOf(adasInfo.carMiddle));
                adasSetView.updateView();
            }
        });

        adas_cali_cameraHeight_left.setOnClickListener(v -> {
            if (adasInfo.cameraHeight > 0) {
                adasInfo.cameraHeight--;
                adas_cali_cameraHeight_text.setText(String.valueOf(adasInfo.cameraHeight));
            }
        });
        adas_cali_cameraHeight_right.setOnClickListener(v -> {
            adasInfo.cameraHeight++;
            adas_cali_cameraHeight_text.setText(String.valueOf(adasInfo.cameraHeight));
        });

        adas_cali_cameraToAxle_left.setOnClickListener(v -> {
            adasInfo.cameraToAxle--;
            adas_cali_cameraToAxle_text.setText(String.valueOf(adasInfo.cameraToAxle));
        });
        adas_cali_cameraToAxle_right.setOnClickListener(v -> {
            adasInfo.cameraToAxle++;
            adas_cali_cameraToAxle_text.setText(String.valueOf(adasInfo.cameraToAxle));
        });

        adas_cali_carWidth_left.setOnClickListener(v -> {
            if (adasInfo.carWidth > 0) {
                adasInfo.carWidth--;
                adas_cali_carWidth_text.setText(String.valueOf(adasInfo.carWidth));
            }
        });
        adas_cali_carWidth_right.setOnClickListener(v -> {
            adasInfo.carWidth++;
            adas_cali_carWidth_text.setText(String.valueOf(adasInfo.carWidth));
        });

        adas_cali_cameraToBumper_left.setOnClickListener(v -> {
            adasInfo.cameraToBumper--;
            adas_cali_cameraToBumper_text.setText(String.valueOf(adasInfo.cameraToBumper));
        });
        adas_cali_cameraToBumper_right.setOnClickListener(v -> {
            adasInfo.cameraToBumper++;
            adas_cali_cameraToBumper_text.setText(String.valueOf(adasInfo.cameraToBumper));
        });

        adas_cali_cameraToLeftWheel_left.setOnClickListener(v -> {
            adasInfo.cameraToLeftWheel--;
            adas_cali_cameraToLeftWheel_text.setText(String.valueOf(adasInfo.cameraToLeftWheel));
        });
        adas_cali_cameraToLeftWheel_right.setOnClickListener(v -> {
            adasInfo.cameraToLeftWheel++;
            adas_cali_cameraToLeftWheel_text.setText(String.valueOf(adasInfo.cameraToLeftWheel));
        });

        adas_save_btn.setOnClickListener(v -> {
            String gateway = WifiUtil.getGateway(getActivity());
            if (TextUtils.isEmpty(gateway)) {
                return;
            }
            AdasInfo.SetRequest setRequest = new AdasInfo.SetRequest();
            setRequest.DATA = adasInfo;
            String setString = GsonUtil.objectToJson(setRequest);
            progressDialog.show();
            tHandler.post(() -> {
                final HttpResult result = HttpUtil.doPostJson("http://" + gateway + "/bin-cgi/mlg.cgi", setString);
                mHandler.post(() -> {
                    progressDialog.dismiss();
                    ArcsoftSetActivity activity = (ArcsoftSetActivity) getActivity();
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

        adas_save_btn.setVisibility(View.INVISIBLE);
        calibration_start_btn = start_layout.findViewById(R.id.calibration_start_btn);
        calibration_start_btn.setOnClickListener(v -> {
            if (is_connected) {
                start_layout.setVisibility(View.INVISIBLE);
                line_layout.setVisibility(View.VISIBLE);
                adas_save_btn.setVisibility(View.VISIBLE);
            } else {
                ArcsoftSetActivity activity = (ArcsoftSetActivity) getActivity();
                if (activity != null) {
                    activity.showMessage(R.string.note_wifi_connected);
                }
                checkConnected();
            }
        });
        updateView();
    }

    private void showDialog(TextView textView, int textId, int resID) {
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_adsa_edit, null);
        TextView title = view.findViewById(R.id.dlg_text);
        EditText edit = view.findViewById(R.id.dlg_edit);
        edit.setText(textView.getText());
        edit.requestFocus();
        Button bt1 = view.findViewById(R.id.lg_dlg_bt1);
        title.setText(textId);
        final AlertDialog dlg = new AlertDialog.Builder(getActivity()).setView(view).show();
        bt1.setOnClickListener(v -> {
            textView.setText(edit.getText());
            if (resID == R.id.adas_cali_horizon_text) {
                adasInfo.horizon = Integer.parseInt(edit.getText().toString());
                adasSetView.updateView();
            } else if (resID == R.id.adas_cali_carMiddle_text) {
                adasInfo.carMiddle = Integer.parseInt(edit.getText().toString());
                adasSetView.updateView();
            } else if (resID == R.id.adas_cali_cameraHeight_text) {
                adasInfo.cameraHeight = Integer.parseInt(edit.getText().toString());
            } else if (resID == R.id.adas_cali_cameraToAxle_text) {
                adasInfo.cameraToAxle = Integer.parseInt(edit.getText().toString());
            } else if (resID == R.id.adas_cali_carWidth_text) {
                adasInfo.carWidth = Integer.parseInt(edit.getText().toString());
            } else if (resID == R.id.adas_cali_cameraToBumper_text) {
                adasInfo.cameraToBumper = Integer.parseInt(edit.getText().toString());
            } else if (resID == R.id.adas_cali_cameraToLeftWheel_text) {
                adasInfo.cameraToLeftWheel = Integer.parseInt(edit.getText().toString());
            }
            dlg.dismiss();
        });
    }

    private void updateView() {
        adas_cali_horizon_text.setText(String.valueOf(adasInfo.horizon));
        adas_cali_carMiddle_text.setText(String.valueOf(adasInfo.carMiddle));
        adas_cali_cameraHeight_text.setText(String.valueOf(adasInfo.cameraHeight));
        adas_cali_cameraToAxle_text.setText(String.valueOf(adasInfo.cameraToAxle));
        adas_cali_carWidth_text.setText(String.valueOf(adasInfo.carWidth));
        adas_cali_cameraToBumper_text.setText(String.valueOf(adasInfo.cameraToBumper));
        adas_cali_cameraToLeftWheel_text.setText(String.valueOf(adasInfo.cameraToLeftWheel));
        adasSetView.upAdasInfo(adasInfo);
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
            AdasInfo.GetRequest getRequest = new AdasInfo.GetRequest();
            String postStr = GsonUtil.objectToJson(getRequest);
            tHandler.removeCallbacksAndMessages(null);
            tHandler.post(() -> {
                final HttpResult result = HttpUtil.doPostJson("http://" + gateway + "/bin-cgi/mlg.cgi", postStr);
                mHandler.removeCallbacksAndMessages(null);
                mHandler.post(() -> {
                    if (result.code == 200) {
                        try {
                            AdasInfo.GetResult data = GsonUtil.json2Object(result.data, AdasInfo.GetResult.class);
                            if (!TextUtils.isEmpty(data.ErrNO) && data.ErrNO.equals("0000")) {
                                adasInfo = data.DATA;
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
