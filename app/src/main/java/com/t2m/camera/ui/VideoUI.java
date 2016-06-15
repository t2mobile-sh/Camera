package com.t2m.camera.ui;

import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.media.Image;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.t2m.camera.CameraActivity;
import com.t2m.camera.R;
import com.t2m.camera.VideoController;
import com.t2m.camera.view.ShutterButton;

/**
 * Created by user on 6/7/16.
 */
public class VideoUI implements
        TextureView.SurfaceTextureListener, SurfaceHolder.Callback,
        View.OnClickListener {
    private static final String TAG = "CAM_VideoUI";
    private CameraActivity mActivity;
    private View mRootView;
    private TextureView mTextureView;
    private SurfaceTexture mSurfaceTexture;


    private SurfaceView mSurfaceView = null;
    private VideoController mController;
    private View mPreviewCover;
    private TextView mRecordTime;
    private ImageView mPreviewThumb;
    private ShutterButton mShutterButton;


    public VideoUI(CameraActivity activity, VideoController controller, View parent) {
        initView(activity, controller, parent);
    }

    private void initView(CameraActivity activity, VideoController controller, View parent) {
        mActivity = activity;
        mController = controller;
        mRootView = parent;
        mActivity.getLayoutInflater().inflate(R.layout.video_module, (ViewGroup) mRootView, true);
        mPreviewCover = mRootView.findViewById(R.id.preview_cover);
        mTextureView = (TextureView) mRootView.findViewById(R.id.preview_content);
        mTextureView.setSurfaceTextureListener(this);
        mRecordTime = (TextView) mRootView.findViewById(R.id.recording_time);
        mPreviewThumb = (ImageView) mRootView.findViewById(R.id.preview_thumb);
        mShutterButton = (ShutterButton) mRootView.findViewById(R.id.shutter_button);
        mShutterButton.setMode(ShutterButton.MODE_RECORD);
        mShutterButton.updateImageResource(false);
        mShutterButton.setOnShutterButtonListener(mController);
        mShutterButton.setVisibility(View.VISIBLE);
        mShutterButton.requestFocus();
        mShutterButton.enableTouch(true);
        mShutterButton.setOnClickListener(this);
        mPreviewThumb.setOnClickListener(this);
    }


    public void showTimeLapseUI(boolean enable) {
        if (mRecordTime != null) {
            mRecordTime.setVisibility(enable ? View.VISIBLE : View.GONE);
        }
    }

    public SurfaceHolder getSurfaceHolder() {
        return mSurfaceView.getHolder();
    }

    public void hideSurfaceView() {
        mSurfaceView.setVisibility(View.GONE);
        mTextureView.setVisibility(View.VISIBLE);
    }

    public void showSurfaceView() {
        mSurfaceView.setVisibility(View.VISIBLE);
        mTextureView.setVisibility(View.GONE);
    }

    public SurfaceTexture getSurfaceTexture() {
        return mSurfaceTexture;
    }


    public void enableShutter(boolean enable) {
        if (mShutterButton != null) {
            mShutterButton.setEnabled(enable);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        //Todo
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        //Todo
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        //Todo
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        mSurfaceTexture = surface;
        mController.onPreviewUIReady();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        //Todo
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.shutter_button:
                mShutterButton.click();
                break;
            case R.id.camera_switcher:

                break;
            case R.id.preview_thumb:
                //Todo enter Gallery to view

                break;
        }
    }

    public void updateShutterButton(boolean isRecording) {
        mShutterButton.updateImageResource(isRecording);
    }
}
