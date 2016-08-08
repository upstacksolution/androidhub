package com.upstack.materialcamera;

import android.app.Fragment;
import android.support.annotation.NonNull;

import com.upstack.materialcamera.internal.BaseCaptureActivity;
import com.upstack.materialcamera.internal.CameraFragment;

public class CaptureActivity extends BaseCaptureActivity {

    @Override
    @NonNull
    public Fragment getFragment() {
        return CameraFragment.newInstance();
    }
}