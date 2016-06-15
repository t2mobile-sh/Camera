package com.t2m.camera;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PictureCallback;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;


public class CapturePhotoActivity extends Activity implements
        Callback, AutoFocusCallback {

    SurfaceView mySurfaceView;
    Camera mCamera;
    Button mCapture;
    ImageView mEditPic;
    Context mContext;
    String mFilePath = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.capture_layout);
        mContext = this;
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        mySurfaceView = (SurfaceView) findViewById(R.id.surface_view);
        SurfaceHolder holder = mySurfaceView.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mCapture = (Button) findViewById(R.id.capture);
        mEditPic = (ImageView) findViewById(R.id.edit_pic);
        mCapture.setOnClickListener(takePicture);
        mEditPic.setOnClickListener(editOnClickListener);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        Camera.Parameters params = mCamera.getParameters();
        params.setPictureFormat(PixelFormat.JPEG);
        mCamera.setParameters(params);
        mCamera.setDisplayOrientation(90);
        mCamera.startPreview();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            int numberOfCameras = Camera.getNumberOfCameras();
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            for (int i = 0; i < numberOfCameras; i++) {
                Camera.getCameraInfo(i, cameraInfo);
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    if (mCamera != null) {
                        return;
                    }
                    try {
                        mCamera = Camera.open(i);
                        mCamera.setPreviewDisplay(holder);
                    } catch (Exception e) {
                        e.printStackTrace();
                        relaseCamera();
                    }
                }
            }
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        relaseCamera();
    }

    public void relaseCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    OnClickListener editOnClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            String picPath = (String) v.getTag();
            Intent intent = new Intent();
            intent.putExtra("path", picPath);
            intent.setClass(mContext, ViewPhotoActivity.class);
            startActivity(intent);
            CapturePhotoActivity.this.finish();
        }
    };

    OnClickListener takePicture = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mCamera != null) {
                mCamera.autoFocus(CapturePhotoActivity.this);
            }
        }
    };

    @Override
    public void onAutoFocus(boolean success, Camera camera) {
        if (success) {
            Camera.Parameters params = mCamera.getParameters();
            params.setPictureFormat(PixelFormat.JPEG);
            mCamera.setParameters(params);
            mCamera.takePicture(null, null, pictureCallback);
        }
    }

    PictureCallback pictureCallback = new PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            try {
                Bitmap bm = BitmapFactory.decodeByteArray(data, 0, data.length);
                SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyyMMddhhmmss");
                String date = sDateFormat.format(new java.util.Date());
                mFilePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString();
                mFilePath = mFilePath + date + ".jpg";
                File file = new File(mFilePath);
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
                Matrix matrix = new Matrix();
                matrix.postRotate(90);
                Bitmap rotateBitmap = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);
                rotateBitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
                bos.flush();
                bos.close();
                mEditPic.setBackgroundDrawable(changeBitmapToDrawable(rotateBitmap));
                mEditPic.setTag(mFilePath);
                if (mCamera != null) {
                    mCamera.startPreview();
                }
//                Toast.makeText(mContext, mContext.getText(R.string.save_pic) + mFilePath,
//                        Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    public BitmapDrawable changeBitmapToDrawable(Bitmap bitmapOrg) {
        int width = bitmapOrg.getWidth();
        int height = bitmapOrg.getHeight();
        int newWidth = 100;
        float scaleWidth = (float) newWidth / width;
        float scaleHeight = scaleWidth;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap resizedBitmap = Bitmap.createBitmap(bitmapOrg, 0, 0, width, height, matrix, true);
        BitmapDrawable bitmapDrawable = new BitmapDrawable(resizedBitmap);
        return bitmapDrawable;
    }

}
