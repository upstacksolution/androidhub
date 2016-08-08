package com.upstack.materialcamera.internal;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.hardware.Camera;
import android.util.FloatMath;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("deprecation")
@SuppressLint("ViewConstructor")
class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

    private final static String TAG = "SF-CameraPreview";
    private static final int INVALID_POINTER_ID = -1;
    protected final SurfaceHolder mHolder;
    private final Camera mCamera;
    private int mRatioWidth = 0;
    private int mRatioHeight = 0;

    private float mLastTouchX;
    private float mLastTouchY;

    // For scaling
    private int mMaxZoom;
    private boolean mIsZoomSupported;
    private int mActivePointerId = INVALID_POINTER_ID;
    private int mScaleFactor = 1;
    private ScaleGestureDetector mScaleDetector;

    // For focus
    private boolean mIsFocus;
    private boolean mIsFocusReady;

    private static final int ZOOM_OUT = 0;
    private static final int ZOOM_IN = 1;
    private static final int ZOOM_DELTA = 1;

    private static final int FOCUS_SQR_SIZE = 100;
    private static final int FOCUS_MAX_BOUND = 1000;
    private static final int FOCUS_MIN_BOUND = -FOCUS_MAX_BOUND;

    private Camera.Area mFocusArea;
    private ArrayList<Camera.Area> mFocusAreas;

    public CameraPreview(Context context, Camera camera) {
        super(context);
        mCamera = camera;
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        mFocusArea = new Camera.Area(new Rect(), 1000);
        mFocusAreas = new ArrayList<>();
        mFocusAreas.add(mFocusArea);

        if (camera != null) {
            Camera.Parameters params = camera.getParameters();
            mIsZoomSupported = params.isZoomSupported();
            if (mIsZoomSupported) {
                mMaxZoom = params.getMaxZoom();
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (Throwable e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mHolder.removeCallback(this);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        if (mHolder.getSurface() == null)
            return;
        try {
            mCamera.stopPreview();
        } catch (Exception ignored) {
        }
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
        } catch (Exception e) {
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    /**
     * Sets the aspect ratio for this view. The size of the view will be measured based on the ratio
     * calculated from the parameters. Note that the actual sizes of parameters don't matter, that
     * is, calling setAspectRatio(2, 3) and setAspectRatio(4, 6) make the same result.
     *
     * @param width  Relative horizontal size
     * @param height Relative vertical size
     */
    public void setAspectRatio(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Size cannot be negative.");
        }
        mRatioWidth = width;
        mRatioHeight = height;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (0 == mRatioWidth || 0 == mRatioHeight) {
            setMeasuredDimension(width, height);
        } else {
            if (width < height * mRatioWidth / mRatioHeight) {
                setMeasuredDimension(width, width * mRatioHeight / mRatioWidth);
            } else {
                setMeasuredDimension(height * mRatioWidth / mRatioHeight, height);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mScaleDetector.onTouchEvent(event);

        final int action = event.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: {
                mIsFocus = true;

                mLastTouchX = event.getX();
                mLastTouchY = event.getY();

                mActivePointerId = event.getPointerId(0);
                break;
            }
            case MotionEvent.ACTION_UP: {
                if (mIsFocus && mIsFocusReady) {
                    handleFocus(mCamera.getParameters());
                }
                mActivePointerId = INVALID_POINTER_ID;
                break;
            }
            case MotionEvent.ACTION_POINTER_DOWN: {
                mCamera.cancelAutoFocus();
                mIsFocus = false;
                break;
            }
            case MotionEvent.ACTION_CANCEL: {
                mActivePointerId = INVALID_POINTER_ID;
                break;
            }
        }

        return true;
    }

    private void handleFocus(Camera.Parameters params) {
        float x = mLastTouchX;
        float y = mLastTouchY;

        if (!setFocusBound(x, y)) return;

        List<String> supportedFocusModes = params.getSupportedFocusModes();
        if (supportedFocusModes != null
                && supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            Log.d(TAG, mFocusAreas.size() + "");
            params.setFocusAreas(mFocusAreas);
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            mCamera.setParameters(params);
            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    // Callback when the auto focus completes
                }
            });
        }
    }

    public void setIsFocusReady(final boolean isFocusReady) {
        mIsFocusReady = isFocusReady;
    }

    private boolean setFocusBound(float x, float y) {
        int left = (int) (x - FOCUS_SQR_SIZE / 2);
        int right = (int) (x + FOCUS_SQR_SIZE / 2);
        int top = (int) (y - FOCUS_SQR_SIZE / 2);
        int bottom = (int) (y + FOCUS_SQR_SIZE / 2);

        if (FOCUS_MIN_BOUND > left || left > FOCUS_MAX_BOUND) return false;
        if (FOCUS_MIN_BOUND > right || right > FOCUS_MAX_BOUND) return false;
        if (FOCUS_MIN_BOUND > top || top > FOCUS_MAX_BOUND) return false;
        if (FOCUS_MIN_BOUND > bottom || bottom > FOCUS_MAX_BOUND) return false;

        mFocusArea.rect.set(left, top, right, bottom);

        return true;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            mScaleFactor = (int) detector.getScaleFactor();
            handleZoom(mCamera.getParameters());
            return true;
        }
    }

    private void handleZoom(Camera.Parameters params) {
        int zoom = params.getZoom();
        if (mScaleFactor == ZOOM_IN) {
            if (zoom < mMaxZoom) zoom += ZOOM_DELTA;
        } else if (mScaleFactor == ZOOM_OUT) {
            if (zoom > 0) zoom -= ZOOM_DELTA;
        }
        params.setZoom(zoom);
        mCamera.setParameters(params);
    }

}