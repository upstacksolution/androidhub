package com.upstack.materialcamera;

import android.app.Fragment;
import android.support.annotation.NonNull;

import com.upstack.materialcamera.internal.BaseCaptureActivity;
import com.upstack.materialcamera.internal.Camera2Fragment;

public class CaptureActivity2 extends BaseCaptureActivity {

    @Override
    @NonNull
    public Fragment getFragment() {
        return Camera2Fragment.newInstance();
    }
}