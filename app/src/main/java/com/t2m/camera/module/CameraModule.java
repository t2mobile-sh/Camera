/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.t2m.camera.module;

import android.content.Intent;
import android.content.res.Configuration;
import android.view.KeyEvent;
import android.view.View;

import com.t2m.camera.CameraActivity;

public interface CameraModule {

    public void init(CameraActivity activity, View frame);

    public void onPauseBeforeSuper();

    public void onPauseAfterSuper();

    public void onResumeBeforeSuper();

    public void onResumeAfterSuper();

}
