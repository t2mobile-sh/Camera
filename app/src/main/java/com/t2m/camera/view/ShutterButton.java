package com.t2m.camera.view;/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import com.t2m.camera.R;

/**
 * A button designed to be used for the on-screen shutter button.
 * It's currently an {@code ImageView} that can call a delegate when the
 * pressed state changes.
 */
public class ShutterButton extends ImageView {
    public static final String TAG = "ShutterButton";

    public static final int MODE_TAKE_PHOTO = 0;
    public static final int MODE_RECORD = 1;
    private int mode = MODE_TAKE_PHOTO;

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    private class LongClickListener implements View.OnLongClickListener {
        public boolean onLongClick(View v) {
            if ( null != mListener ) {
                mListener.onShutterButtonLongClick();
                return true;
            }
            return false;
        }
    }

    private boolean mTouchEnabled = true;
    private LongClickListener mLongClick = new LongClickListener();

    /**
     * A callback to be invoked when a ShutterButton's pressed state changes.
     */
    public interface OnShutterButtonListener {
        /**
         * Called when a ShutterButton has been pressed.
         *
         * @param pressed The ShutterButton that was pressed.
         */
        void onShutterButtonFocus(boolean pressed);
        void onShutterButtonClick();
        void onShutterButtonLongClick();
    }

    private OnShutterButtonListener mListener;
    private boolean mOldPressed;

    public ShutterButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setOnShutterButtonListener(OnShutterButtonListener listener) {
        mListener = listener;
        setOnLongClickListener(mLongClick);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent m) {
        if (mTouchEnabled) {
            return super.dispatchTouchEvent(m);
        } else {
            return false;
        }
    }

    public void enableTouch(boolean enable) {
        mTouchEnabled = enable;
        setLongClickable(enable);
    }

    /**
     * Hook into the drawable state changing to get changes to isPressed -- the
     * onPressed listener doesn't always get called when the pressed state
     * changes.
     */
    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        final boolean pressed = isPressed();
        if (pressed != mOldPressed) {
            if (!pressed) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        callShutterButtonFocus(pressed);
                    }
                });
            } else {
                callShutterButtonFocus(pressed);
            }
            mOldPressed = pressed;
        }
    }

    private void callShutterButtonFocus(boolean pressed) {
        if (mListener != null) {
            mListener.onShutterButtonFocus(pressed);
        }
    }

    public void click() {
        if (mListener != null && getVisibility() == View.VISIBLE) {
            mListener.onShutterButtonClick();
        }
    }

    public void updateImageResource(boolean recording) {
        if (mode == MODE_RECORD) {
            if (recording) {
                this.setImageResource(R.drawable.btn_shutter_video_recording);
            } else {
                this.setImageResource(R.drawable.btn_new_shutter_video);
            }
        } else if (mode == MODE_TAKE_PHOTO) {
            this.setImageResource(R.drawable.btn_new_shutter);
        }
    }
}
