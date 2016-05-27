package com.t2m.camera;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.ZoomControls;

import com.t2m.learn.camera.R;

public class ViewPhotoActivity extends Activity {

    ImageView mImageView;
    ZoomControls mZoom;
    private float scaleWidth = 1;
    private float scaleHeight = 1;
    int mBmpWidth;
    int mBmpHeight;
    Bitmap mBitmapOrg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_photo_layout);

        mZoom = (ZoomControls) findViewById(R.id.zoomControls1);
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        mZoom.setIsZoomInEnabled(true);
        mZoom.setIsZoomOutEnabled(true);
        mImageView = (ImageView) findViewById(R.id.img);
        Intent it = getIntent();
        String picPath = (String) it.getCharSequenceExtra("path");
        mBitmapOrg = BitmapFactory.decodeFile(picPath, null);
        mImageView.setImageBitmap(mBitmapOrg);
        mBmpWidth = mBitmapOrg.getWidth();
        mBmpHeight = mBitmapOrg.getHeight();

        mZoom.setOnZoomInClickListener(new OnClickListener() {
            public void onClick(View v) {
                double scale = 1.25;
                scaleWidth = (float) (scaleWidth * scale);
                scaleHeight = (float) (scaleHeight * scale);
                if (scaleWidth > 1.25) {
                    scaleWidth = 1;
                    scaleHeight = 1;
                }
                Matrix matrix = new Matrix();
                matrix.postScale(scaleWidth, scaleHeight);
                Bitmap resizeBmp = Bitmap.createBitmap(mBitmapOrg, 0, 0, mBmpWidth, mBmpHeight, matrix, true);
                mImageView.setImageBitmap(resizeBmp);
            }
        });

        mZoom.setOnZoomOutClickListener(new OnClickListener() {
            public void onClick(View v) {
                double scale = 0.8;
                scaleWidth = (float) (scaleWidth * scale);
                scaleHeight = (float) (scaleHeight * scale);
                Matrix matrix = new Matrix();
                matrix.postScale(scaleWidth, scaleHeight);
                Bitmap resizeBmp = Bitmap.createBitmap(mBitmapOrg, 0, 0, mBmpWidth, mBmpHeight, matrix, true);
                mImageView.setImageBitmap(resizeBmp);
            }
        });
    }
}

