package com.t2m.camera.ui;

import android.graphics.SurfaceTexture;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.t2m.camera.CameraActivity;
import com.t2m.camera.PhotoController;
import com.t2m.camera.R;
import com.t2m.camera.view.ShutterButton;

/**
 * Created by user on 6/7/16.
 */
public class PhotoUI
        implements TextureView.SurfaceTextureListener,
        View.OnClickListener {

    private static final String TAG = "CAM_UI";
    private CameraActivity mActivity;
    private View mRootView;
    private SurfaceTexture mSurfaceTexture;
    private ShutterButton mShutterButton;
    private FrameLayout mCameraControls;

    private TextureView mTextureView;

    private View mPreviewCover;
    private final Object mSurfaceTextureLock = new Object();
    private PhotoController mController;
    //for thumb show
    private ImageView mPreviewThumb;


    public PhotoUI(CameraActivity activity, PhotoController controller, View parent) {
        Log.i(TAG, "PhotoUI create");
        mActivity = activity;
        mController = controller;
        mRootView = parent;

        mActivity.getLayoutInflater().inflate(R.layout.photo_module,
                (ViewGroup) mRootView, true);
        mPreviewCover = mRootView.findViewById(R.id.preview_cover);
        // display the view
        mTextureView = (TextureView) mRootView.findViewById(R.id.preview_content);
        mTextureView.setSurfaceTextureListener(this);

        mShutterButton = (ShutterButton) mRootView.findViewById(R.id.shutter_button);
        mShutterButton.setMode(ShutterButton.MODE_TAKE_PHOTO);
        mShutterButton.updateImageResource(false);
        mShutterButton.setOnClickListener(this);
        mShutterButton.setOnShutterButtonListener(mController);
        mCameraControls = (FrameLayout) mRootView.findViewById(R.id.camera_controls);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.shutter_button:
                Log.i(TAG, "shutter_button click");
                mShutterButton.click();
            break;
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        synchronized (mSurfaceTextureLock) {
            Log.i(TAG, "SurfaceTexture ready.");
            mSurfaceTexture = surface;
            mController.onPreviewUIReady();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        // Ignored, Camera does all the work for us
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        synchronized (mSurfaceTextureLock) {
            mSurfaceTexture = null;
            mController.onPreviewUIDestroyed();
            Log.i(TAG, "SurfaceTexture destroyed");
            return true;
        }
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // Make sure preview cover is hidden if preview data is available.
        if (mPreviewCover.getVisibility() != View.GONE) {
            mPreviewCover.setVisibility(View.GONE);
        }
    }

    public SurfaceTexture getSurfaceTexture() {
        return mSurfaceTexture;
    }

}
