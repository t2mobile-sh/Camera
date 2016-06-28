package com.t2m.camera.util;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Surface;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;

/**
 * Created by user on 6/8/16.
 */
public class CameraUtil {
    public static final String TAG = "CameraUtil";
    public static final String DCIM =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString();

    public static final String SDCARD =
            android.os.Environment.getExternalStorageDirectory().toString();
    public static final String DIRECTORY = DCIM + "/Camera";
    public static final String RAW_DIRECTORY = DCIM + "/Camera/raw";
    public static final int PATH_TYPE_PHOTO = 0;
    public static final int PATH_TYPE_VIDEO = 1;
    public static final String PHOTO_POSTFIX = ".jpg";
    public static final String VIDEO_POSTFIX = ".mp4";
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    public interface CameraOpenErrorCallback {
        public void onCameraDisabled(int cameraId);
    }

    public interface OnMediaSavedListener {
        public void onMediaSaved(Uri uri);
    }

    private static void throwIfCameraDisabled(Activity activity) throws Exception {
        // Check if device policy has disabled the camera.
        DevicePolicyManager dpm = (DevicePolicyManager) activity.getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        if (dpm.getCameraDisabled(null)) {
            throw new Exception();
        }
    }

    /**
     * method for 6.0 request permissin for camera
     * and don't forget media recorder permission
     * @param activity
     * @param cameraId
     * @param handler
     * @param cb
     * @return camera
     */
    @TargetApi(Build.VERSION_CODES.M)
    public static Camera openCameraForM(
            Activity activity, final int cameraId,
            Handler handler, final CameraOpenErrorCallback cb) {
        try {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                activity.requestPermissions(new String[]{Manifest.permission.CAMERA},
                        REQUEST_CAMERA_PERMISSION);
            }
            throwIfCameraDisabled(activity);
            return android.hardware.Camera.open(cameraId);
        } catch (Exception ex) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    cb.onCameraDisabled(cameraId);
                }
            });
        }
        return null;
    }

    public static Camera openCamera(
            Activity activity, final int cameraId,
            Handler handler, final CameraOpenErrorCallback cb) {
        try {
            throwIfCameraDisabled(activity);
            return android.hardware.Camera.open(cameraId);
        } catch (Exception ex) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    cb.onCameraDisabled(cameraId);
                }
            });
        }
        return null;
    }


    public static int getCameraId() {
        int cameraId = -1;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            int numberOfCameras = Camera.getNumberOfCameras();
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            for (int i = 0; i < numberOfCameras; i++) {
                Camera.getCameraInfo(i, cameraInfo);
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    cameraId = i;
                }
            }
        }
        return cameraId;
    }

    public static int getCIDFromManager(Activity activity) {
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                String cameraId = manager.getCameraIdList()[0];
                return Integer.parseInt(cameraId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static String generatePath(int type) {
        String path = "";
        SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyyMMddhhmmss");
        String date = sDateFormat.format(new java.util.Date());
        path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString();
        if (type == PATH_TYPE_PHOTO) {
            path = path + date + PHOTO_POSTFIX;
        } else if (type == PATH_TYPE_VIDEO) {
            path = path + date + VIDEO_POSTFIX;
        }
        return path;
    }


    public static void writeFile(String path, byte[] data, Bitmap bm) {
        FileOutputStream out = null;
        BufferedOutputStream bos = null;
        try {
            out = new FileOutputStream(path);
            bos = new BufferedOutputStream(out);
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            Bitmap rotateBitmap = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);
            rotateBitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            bos.flush();
        } catch (Exception e) {
            Log.e(TAG, "Failed to write data", e);
        } finally {
            try {
                out.close();
                bos.close();
            } catch (Exception e) {
                Log.e(TAG, "Failed to close file after write", e);
            }
        }
    }

    public static int getDisplayRotation(Activity activity) {
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        switch (rotation) {
            case Surface.ROTATION_0: return 0;
            case Surface.ROTATION_90: return 90;
            case Surface.ROTATION_180: return 180;
            case Surface.ROTATION_270: return 270;
        }
        return 0;
    }

    public static int getDisplayOrientation(int degrees, int cameraId) {
        // See android.hardware.Camera.setDisplayOrientation for
        // documentation.
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }

    public static int getDisplayOrientation(Activity activity, int cameraId) {
        int degree = getDisplayRotation(activity);
        int rotation = getDisplayOrientation(degree, cameraId);
        return rotation;
    }
}
