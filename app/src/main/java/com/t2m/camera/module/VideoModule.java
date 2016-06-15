package com.t2m.camera.module;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.t2m.camera.CameraActivity;
import com.t2m.camera.VideoController;
import com.t2m.camera.ui.VideoUI;
import com.t2m.camera.util.CameraUtil;
import com.t2m.camera.view.ShutterButton;

/**
 * Created by user on 6/7/16.
 */
public class VideoModule implements
        CameraModule, VideoController,
        ShutterButton.OnShutterButtonListener {
    private static final String TAG = "CAM_VideoModule";

    private static final int CHECK_DISPLAY_ROTATION = 3;
    private static final int CLEAR_SCREEN_DELAY = 4;
    private static final int UPDATE_RECORD_TIME = 5;
    private static final int ENABLE_SHUTTER_BUTTON = 6;
    private static final int SHOW_TAP_TO_SNAPSHOT_TOAST = 7;
    private static final int SWITCH_CAMERA = 8;
    private static final int SWITCH_CAMERA_START_ANIMATION = 9;

    private CameraActivity mActivity;
    private boolean mPaused;
    private int mCameraId;
    private Camera.Parameters mParameters;

    private MediaRecorder mMediaRecorder;

    //switch camera
    private boolean mSwitchingCamera;
    private boolean mMediaRecorderRecording = false;
    private boolean mMediaRecorderPausing = false;
    //function for recording timer
    private long mRecordingStartTime;
    private long mRecordingTotalTime;
    private boolean mRecordingTimeCountsDown = false;
    private long mOnResumeTime;

    private String mVideoFilename;
    // The video file that has already been recorded, and that is being
    // examined by the user.
    private Uri mCurrentVideoUri;
    boolean mPreviewing = false;

    private boolean mCaptureTimeLapse = false;
    private boolean mStartRecPending = false;
    private boolean mStopRecPending = false;
    private boolean mStartPrevPending = false;
    private boolean mStopPrevPending = false;
    private final Handler mHandler = new MainHandler();
    private Camera mCamera;
    private VideoUI mUI;
    private CamcorderProfile mProfile;
    private String mVideoPath;


    private final CameraUtil.OnMediaSavedListener mOnVideoSavedListener =
            new CameraUtil.OnMediaSavedListener() {
                @Override
                public void onMediaSaved(Uri uri) {
                    //Todo when video saved
                }
            };

    private class MainHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {

                case ENABLE_SHUTTER_BUTTON:
                    mUI.enableShutter(true);
                    break;

                case CLEAR_SCREEN_DELAY: {
                    mActivity.getWindow().clearFlags(
                            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    break;
                }

                case UPDATE_RECORD_TIME: {
                    //Todo
//                    updateRecordingTime();
                    break;
                }

                case CHECK_DISPLAY_ROTATION: {
                    // Restart the preview if display rotation has changed.
                    // Sometimes this happens when the device is held upside
                    // down and camera app is opened. Rotation animation will
                    // take some time and the rotation value we have got may be
                    // wrong. Framework does not have a callback for this now.
//                        startPreview();
                    //Todo
                    break;
                }

                case SHOW_TAP_TO_SNAPSHOT_TOAST: {
                    //Todo
//                    showTapToSnapshotToast();
                    break;
                }

                case SWITCH_CAMERA: {
                    //Todo
//                    switchCamera();
                    break;
                }

                case SWITCH_CAMERA_START_ANIMATION: {
                    //((CameraScreenNail) mActivity.mCameraScreenNail).animateSwitchCamera();

                    // Enable all camera controls.
                    mSwitchingCamera = false;
                    break;
                }

                default:
                    Log.v(TAG, "Unhandled message: " + msg.what);
                    break;
            }
        }
    }

    protected class CameraOpenThread extends Thread {
        @Override
        public void run() {
            openCamera();
        }
    }

    private void openCamera() {
        if (mCamera == null) {
            mCamera = CameraUtil.openCamera(
                    mActivity, mCameraId, mHandler,
                    mActivity.getCameraOpenErrorCallback());
        }
        if (mCamera == null) {
            // Error.
            return;
        }
        mParameters = mCamera.getParameters();
    }


    @Override
    public void init(CameraActivity activity, View root) {
        mActivity = activity;
        mUI = new VideoUI(activity, this, root);
        mCameraId = CameraUtil.getCameraId();
        /*
         * To reduce startup time, we start the preview in another thread.
         * We make sure the preview is started at the end of onCreate.
         */
        CameraOpenThread cameraOpenThread = new CameraOpenThread();
        cameraOpenThread.start();

        // Make sure camera device is opened.
        try {
            cameraOpenThread.join();
            if (mCamera == null) {
                return;
            }
        } catch (InterruptedException ex) {
            // ignore
        }
        setDisplayOrientation();
        mUI.showTimeLapseUI(mCaptureTimeLapse);
    }

    private void setDisplayOrientation() {
        int rotation = CameraUtil.getDisplayOrientation(mActivity, mCameraId);
        mCamera.setDisplayOrientation(rotation);
    }

    private void startPreview() {
        Log.v(TAG, "startPreview");
        mStartPrevPending = true;

        SurfaceTexture surfaceTexture = mUI.getSurfaceTexture();
        if (surfaceTexture == null || mCamera == null || mPaused == true) {
            mStartPrevPending = false;
            return;
        }

        if (mPreviewing == true) {
            stopPreview();
        }

        setDisplayOrientation();

        try {
            mCamera.setPreviewTexture(surfaceTexture);
            mCamera.startPreview();
            mPreviewing = true;
            onPreviewStarted();
        } catch (Throwable ex) {
            closeCamera();
            throw new RuntimeException("startPreview failed", ex);
        }
        mStartPrevPending = false;
    }

    private void closeCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    private void releasePreviewResources() {
        if (!(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)) {
            mUI.hideSurfaceView();
        }
    }

    private void onPreviewStarted() {
        mUI.enableShutter(true);
    }


    @Override
    public void onPauseBeforeSuper() {
        mPaused = true;
        if (mMediaRecorderRecording) {
            // Camera will be released in onStopVideoRecording.
            stopVideoRecording();
        } else {
            closeCamera();
            releaseMediaRecorder();
        }
        releasePreviewResources();
    }

    @Override
    public void onPauseAfterSuper() {
        //Todo
    }

    @Override
    public void onResumeBeforeSuper() {
        mPaused = false;
    }

    @Override
    public void onResumeAfterSuper() {
        //Todo
    }

    @Override
    public void stopPreview() {
        //Todo
    }

    @Override
    public void onPreviewUIReady() {
        startPreview();
    }

    @Override
    public void onPreviewUIDestroyed() {
        //Todo
    }

    public boolean isPreviewReady() {
        if ((mStartPrevPending == true || mStopPrevPending == true)) {
            return false;
        }
        else {
            return true;
        }
    }

    public boolean isRecorderReady() {
        if ((mStartRecPending == true || mStopRecPending == true)) {
            return false;
        }
        else {
            return true;
        }
    }

    private boolean stopVideoRecording() {
        Log.v(TAG, "stopVideoRecording");
        mUI.updateShutterButton(false);
        mStopRecPending = true;
        mActivity.showSwitcher();

        boolean fail = false;
        if (mMediaRecorderRecording) {
            try {
                mMediaRecorder.setOnErrorListener(null);
                mMediaRecorder.setOnInfoListener(null);
                mMediaRecorder.stop();
            } catch (RuntimeException e) {
                Log.e(TAG, "stop fail",  e);
                fail = true;
            }
            mMediaRecorderRecording = false;
            if (mPaused) {
                closeCamera();
            }
        }
        // release media recorder
        releaseMediaRecorder();
        Toast.makeText(mActivity, "the video save to :"+mVideoPath, Toast.LENGTH_SHORT).show();
        mCamera.lock();
        if (!(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)) {
            stopPreview();
            mUI.hideSurfaceView();
            // Switch back to use SurfaceTexture for preview.
            startPreview();
        }
        mStopRecPending = false;
        return fail;
    }

    private void setupMediaRecorderPreviewDisplay() {
        // Nothing to do here if using SurfaceTexture.
        if (!(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)) {
            // We stop the preview here before unlocking the device because we
            // need to change the SurfaceTexture to SurfaceView for preview.
            stopPreview();
            try {
                mCamera.setPreviewDisplay(mUI.getSurfaceHolder());
            } catch (Exception e) {
                e.printStackTrace();
            }
            // The orientation for SurfaceTexture is different from that for
            // SurfaceView. For SurfaceTexture we don't need to consider the
            // display rotation. Just consider the sensor's orientation and we
            // will set the orientation correctly when showing the texture.
            // Gallery will handle the orientation for the preview. For
            // SurfaceView we will have to take everything into account so the
            // display rotation is considered.
            int rotation = CameraUtil.getDisplayOrientation(mActivity, mCameraId);
            mCamera.setDisplayOrientation(rotation);
            mCamera.startPreview();
            mPreviewing = true;
            mMediaRecorder.setPreviewDisplay(mUI.getSurfaceHolder().getSurface());
        }
    }


    private void initializeRecorder() {
        Log.i(TAG, "initializeRecorder");
        // If the mCameraDevice is null, then this activity is going to finish
        if (mCamera == null) return;

        if (!(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)) {
            // Set the SurfaceView to visible so the surface gets created.
            // surfaceCreated() is called immediately when the visibility is
            // changed to visible. Thus, mSurfaceViewReady should become true
            // right after calling setVisibility().
            mUI.showSurfaceView();
        }
        mVideoPath = CameraUtil.generatePath(CameraUtil.PATH_TYPE_VIDEO);
        mMediaRecorder = new MediaRecorder();

        // Unlock the camera object before passing it to media recorder.
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);
        mMediaRecorder.reset();
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        // back-facing camera
        int rotation = CameraUtil.getDisplayOrientation(mActivity, mCameraId);
        mMediaRecorder.setOrientationHint(rotation);
        mProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
        mMediaRecorder.setProfile(mProfile);
        mMediaRecorder.setVideoFrameRate(30);
        mMediaRecorder.setOutputFile(mVideoPath);
        setupMediaRecorderPreviewDisplay();

        try {
            mMediaRecorder.prepare();
        } catch (Exception e) {
            Log.i(TAG, "prepare failed for " + mVideoFilename, e);
            releaseMediaRecorder();
            throw new RuntimeException(e);
        }
    }

    private void startVideoRecording() {
        Log.v(TAG, "startVideoRecording");
        mStartRecPending = true;
        mUI.updateShutterButton(true);
        mCurrentVideoUri = null;

        initializeRecorder();

        if (mMediaRecorder == null) {
            Log.e(TAG, "Fail to initialize media recorder");
            mStartRecPending = false;
            return;
        }

        try {
            mMediaRecorder.start(); // Recording is now started
        } catch (RuntimeException e) {
            Log.e(TAG, "Could not start media recorder. ", e);
            releaseMediaRecorder();
            // If start fails, frameworks will not lock the camera for us.
            mCamera.lock();
            mStartRecPending = false;
            return;
        }

        mMediaRecorderRecording = true;
        mMediaRecorderPausing = false;
        mRecordingTotalTime = 0L;
        mRecordingStartTime = SystemClock.uptimeMillis();
        mActivity.hideSwitcher();
        mStartRecPending = false;
    }

    private void releaseMediaRecorder() {
        Log.v(TAG, "Releasing media recorder.");
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
        mVideoFilename = null;
    }

    @Override
    public void onShutterButtonClick() {
        boolean stop = mMediaRecorderRecording;

        if (isPreviewReady() == false) {
            return;
        }

        if (isRecorderReady() == false) {
            return;
        }

        if (stop) {
            stopVideoRecording();
        } else {
            startVideoRecording();
        }
        //Todo disable shutterbt for duplicate click
        //mUI.enableShutter(false);
    }

    @Override
    public void onShutterButtonFocus(boolean pressed) {
        //Todo
    }

    @Override
    public void onShutterButtonLongClick() {
        //Todo
    }
}
