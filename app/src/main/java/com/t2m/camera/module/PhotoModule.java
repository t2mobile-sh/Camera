package com.t2m.camera.module;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.t2m.camera.CameraActivity;
import com.t2m.camera.PhotoController;
import com.t2m.camera.R;
import com.t2m.camera.ui.PhotoUI;
import com.t2m.camera.util.CameraUtil;
import com.t2m.camera.view.ShutterButton;

import java.io.IOException;

/**
 * Created by user on 6/7/16.
 */
public class PhotoModule
        implements CameraModule,
        PhotoController,
        Camera.AutoFocusCallback,
        ShutterButton.OnShutterButtonListener {
    public static final String TAG = "PhotoModule";

    private static final int SCREEN_DELAY = 2 * 60 * 1000;
    private static final int START_PREVIEW = 1;
    private static final int CLEAR_SCREEN_DELAY = 3;
    private static final int SWITCH_CAMERA = 6;
    private static final int SWITCH_CAMERA_START_ANIMATION = 7;
    private static final int CAMERA_OPEN_DONE = 8;
    private static final int OPEN_CAMERA_FAIL = 9;
    private static final int CAMERA_DISABLED = 10;
    private static final int ON_PREVIEW_STARTED = 15;

    private CameraActivity mActivity;
    private Camera mCamera;
    private boolean mPaused;
    private int mCameraId;
    private View mRootView;

    private PhotoUI mUI;
    private OpenCameraThread mOpenCameraThread = null;
    private Handler mHandler = new MainHandler();

    public String mFilePath;

    private class OpenCameraThread extends Thread {
        @Override
        public void run() {
            openCamera();
            startPreview();
        }
    }

    Camera.PictureCallback pictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            if (camera == null) {
                return;
            }
            try {
                mFilePath = CameraUtil.generatePath(CameraUtil.PATH_TYPE_PHOTO);
                ImageSaveTask imageSaveTask =
                        new ImageSaveTask(data, mFilePath, mOnMediaSavedListener);
                imageSaveTask.execute();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    private CameraUtil.OnMediaSavedListener mOnMediaSavedListener =
            new CameraUtil.OnMediaSavedListener() {
                @Override
                public void onMediaSaved(Uri uri) {
                    //notify the media is saved .
                    if (mCamera != null) {
                        mCamera.startPreview();
                    }
                    Toast.makeText(mActivity, mActivity.getText(R.string.save_pic) + mFilePath,
                            Toast.LENGTH_SHORT).show();
                }
            };

    private class MainHandler extends Handler {
        public MainHandler() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case START_PREVIEW: {
                    startPreview();
                    break;
                }

                case CLEAR_SCREEN_DELAY: {
                    mActivity.getWindow().clearFlags(
                            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    break;
                }

                case SWITCH_CAMERA: {
                    switchCamera();
                    break;
                }

                case SWITCH_CAMERA_START_ANIMATION: {
                    // TODO: Need to revisit
                    break;
                }

                case CAMERA_OPEN_DONE: {
                    onCameraOpened();
                    break;
                }

                case OPEN_CAMERA_FAIL: {
                    //Todo fail situation
                    break;
                }

                case CAMERA_DISABLED: {
                    //Todo
                    break;
                }

                case ON_PREVIEW_STARTED: {
                    onPreviewStarted();
                    break;
                }
            }
        }
    }

    private void startPreview() {
        if (mPaused || mCamera == null) {
            return;
        }

        Log.d(TAG, "startPreview");

        SurfaceTexture st = null;
        if (mUI != null) {
            st = mUI.getSurfaceTexture();
        }
        try {
            mCamera.setPreviewTexture(st);
        } catch (IOException e) {
            e.printStackTrace();
        }

        mCamera.startPreview();
    }

    private void switchCamera() {
        //Todo siwtch to forground camera
    }


    private void openCamera() {
        // We need to check whether the activity is paused before long
        // operations to ensure that onPause() can be done ASAP.
        if (mPaused) {
            return;
        }
        Log.i(TAG, "Open camera device.");
        mCamera = CameraUtil.openCamera(
                mActivity, mCameraId, mHandler,
                mActivity.getCameraOpenErrorCallback());
        if (mCamera == null) {
            Log.e(TAG, "Failed to open camera:" + mCameraId);
            mHandler.sendEmptyMessage(OPEN_CAMERA_FAIL);
            return;
        }
        mHandler.sendEmptyMessageDelayed(CAMERA_OPEN_DONE, 100);
        return;
    }

    private void onCameraOpened() {
        int rotation = CameraUtil.getDisplayOrientation(mActivity, mCameraId);
        mCamera.setDisplayOrientation(rotation);
    }

    private void onPreviewStarted() {
        //Todo
    }

    @Override
    public void init(CameraActivity activity, View parent) {
        mActivity = activity;
        mRootView = parent;
        mCameraId = CameraUtil.getCameraId();
//        mCameraId = CameraUtil.getCIDFromManager(mActivity);
        mUI = new PhotoUI(mActivity, this, parent);
        if (mOpenCameraThread == null && !mActivity.mIsModuleSwitchInProgress) {
            mOpenCameraThread = new OpenCameraThread();
            mOpenCameraThread.start();
        }
        keepScreenOnAwhile();
    }

    @Override
    public void onPauseAfterSuper() {
        try {
            if (mOpenCameraThread != null) {
                mOpenCameraThread.join();
            }
        } catch (InterruptedException ex) {
            // ignore
        }
        mOpenCameraThread = null;
        // Remove the messages and runnables in the queue.
        mHandler.removeCallbacksAndMessages(null);
        closeCamera();
    }

    private void closeCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
        resetScreenOn();
    }

    //need set mPause flag to consider the situation of return to launcher.
    @Override
    public void onPauseBeforeSuper() {
        mPaused = true;
    }

    @Override
    public void onResumeBeforeSuper() {
        mPaused = false;
    }

    @Override
    public void onResumeAfterSuper() {
        if (mOpenCameraThread == null) {
            mOpenCameraThread = new OpenCameraThread();
            mOpenCameraThread.start();
        }
    }

    @Override
    public void onPreviewUIReady() {
        //Todo
    }

    @Override
    public void onPreviewUIDestroyed() {
        //Todo
    }


    @Override
    public void onAutoFocus(boolean success, Camera camera) {
        if (success) {
            Camera.Parameters params = mCamera.getParameters();
            params.setPictureFormat(PixelFormat.JPEG);
            mCamera.setParameters(params);
        }
    }

    private void resetScreenOn() {
        mHandler.removeMessages(CLEAR_SCREEN_DELAY);
        mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void keepScreenOnAwhile() {
        mHandler.removeMessages(CLEAR_SCREEN_DELAY);
        mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mHandler.sendEmptyMessageDelayed(CLEAR_SCREEN_DELAY, SCREEN_DELAY);
    }

    @Override
    public void onShutterButtonFocus(boolean pressed) {
        //Todo
    }

    @Override
    public void onShutterButtonClick() {
        if (mPaused) {
            return;
        }
        //play sound and storage
        mCamera.autoFocus(this);
        takePhoto();
    }

    @Override
    public void onShutterButtonLongClick() {
        //Todo focus
    }

    public void takePhoto() {
        if (mCamera == null) {
            return;
        }
        Log.i(TAG, "take photo started");
        mCamera.takePicture(null, null, pictureCallback);
    }

    private class ImageSaveTask extends AsyncTask<Void, Void, Uri> {
        private byte[] data;
        private String path;
        private CameraUtil.OnMediaSavedListener listener;

        public ImageSaveTask(byte[] data, String path,
                             CameraUtil.OnMediaSavedListener listener) {
            this.data = data;
            this.path = path;
            this.listener = listener;
        }

        @Override
        protected void onPreExecute() {
            // do nothing.
        }

        @Override
        protected Uri doInBackground(Void... v) {
                // Decode bounds
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            Bitmap bm = BitmapFactory.decodeByteArray(data, 0, data.length);
            CameraUtil.writeFile(path, data, bm);
            return null;
        }

        @Override
        protected void onPostExecute(Uri uri) {
            if (listener != null) listener.onMediaSaved(uri);
        }
    }
}
