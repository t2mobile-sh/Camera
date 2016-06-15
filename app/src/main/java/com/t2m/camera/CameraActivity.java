package com.t2m.camera;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.t2m.camera.module.CameraModule;
import com.t2m.camera.module.PhotoModule;
import com.t2m.camera.module.VideoModule;
import com.t2m.camera.util.CameraUtil;

public class CameraActivity extends Activity implements
            View.OnClickListener{

    public static final String TAG = "CAM_Activity";
    public static final int PHOTO_MODULE_INDEX = 0;
    public static final int VIDEO_MODULE_INDEX = 1;

    public Context mContext;
    private int mCurrentModuleIndex;
    private View mCameraModuleRootView;
    private CameraModule mCurrentModule;
    public boolean mIsModuleSwitchInProgress = false;
    private ImageView mSwitcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.startup);
        init();
        mCurrentModule.init(this, mCameraModuleRootView);
        initSwitcher();
    }

    @Override
    protected void onResume() {
        mCurrentModule.onResumeBeforeSuper();
        super.onResume();
        mCurrentModule.onResumeAfterSuper();
    }

    @Override
    protected void onPause() {
        mCurrentModule.onPauseBeforeSuper();
        super.onPause();
        mCurrentModule.onPauseAfterSuper();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private void init() {
        mCameraModuleRootView = findViewById(R.id.camera_app_root);
        mContext = CameraActivity.this;
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        mCurrentModuleIndex = PHOTO_MODULE_INDEX;
        setModuleFromIndex(mCurrentModuleIndex);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.camera_switcher:
                if (mIsModuleSwitchInProgress == true) {
                    return;
                }
                Log.i(TAG, "camera_switcher click");
                updateModule();
                onModuleSwitch(mCurrentModuleIndex);
                break;
        }
    }

    private void updateModule() {
        if (mCurrentModuleIndex == PHOTO_MODULE_INDEX) {
            mCurrentModuleIndex = VIDEO_MODULE_INDEX;
        } else {
            mCurrentModuleIndex = PHOTO_MODULE_INDEX;
        }
    }


    public void hideSwitcher() {
        mSwitcher.setVisibility(View.INVISIBLE);
    }

    public void showSwitcher() {
        mSwitcher.setVisibility(View.VISIBLE);
    }


    private void setModuleFromIndex(int moduleIndex) {
        mCurrentModuleIndex = moduleIndex;
        switch (moduleIndex) {
            case PHOTO_MODULE_INDEX:
                mCurrentModule = new PhotoModule();
                mCurrentModuleIndex = PHOTO_MODULE_INDEX;
                break;
            case VIDEO_MODULE_INDEX:
                mCurrentModule = new VideoModule();
                mCurrentModuleIndex = VIDEO_MODULE_INDEX;
                break;

            default:
                // Fall back to photo mode.
                mCurrentModule = new PhotoModule();
                mCurrentModuleIndex = PHOTO_MODULE_INDEX;
                //mCurrentModuleIndex = ModuleSwitcher.PHOTO_MODULE_INDEX;
                break;
        }
    }

    public void onModuleSwitch(int moduleIndex) {
        Log.i(TAG, "onModuleSwitch the module index is "+moduleIndex);
        mIsModuleSwitchInProgress = true;
        closeModule(mCurrentModule);
        setModuleFromIndex(moduleIndex);
        openModule(mCurrentModule);
        initSwitcher();
        int imageR = mCurrentModuleIndex == PHOTO_MODULE_INDEX ?
                R.drawable.ic_switch_camera : R.drawable.ic_switch_video;
        mSwitcher.setImageResource(imageR);
        mIsModuleSwitchInProgress = false;
    }

    private void initSwitcher() {
        mSwitcher = (ImageView) findViewById(R.id.camera_switcher);
        mSwitcher.setOnClickListener(this);
    }

    private void openModule(CameraModule module) {
        module.init(this, mCameraModuleRootView);
        module.onResumeBeforeSuper();
        module.onResumeAfterSuper();
    }

    private void closeModule(CameraModule module) {
        module.onPauseBeforeSuper();
        module.onPauseAfterSuper();
        ((ViewGroup) mCameraModuleRootView).removeAllViews();
        ((ViewGroup) mCameraModuleRootView).clearDisappearingChildren();
    }

    public CameraUtil.CameraOpenErrorCallback getCameraOpenErrorCallback() {
        return mCameraOpenErrorCallback;
    }

    private CameraUtil.CameraOpenErrorCallback mCameraOpenErrorCallback =
            new CameraUtil.CameraOpenErrorCallback() {
                @Override
                public void onCameraDisabled(int cameraId) {
                    Toast.makeText(mContext, "can't open camera id: "+cameraId, Toast.LENGTH_SHORT).show();
                }};
}
