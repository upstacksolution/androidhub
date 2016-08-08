package com.upstack.materialcamerasample;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.upstack.materialcamera.MaterialCamera;
import com.afollestad.materialcamerasample.R;
import com.netcompss.ffmpeg4android.GeneralUtils;
import com.netcompss.ffmpeg4android.Prefs;
import com.netcompss.loader.LoadJNI;

import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private final static int CAMERA_RQ = 6969;
    private final static int PERMISSION_RQ = 84;
    String workFolder = null;
    String demoVideoFolder = null;
    String vkLogPath = null;
    private boolean commandValidationFailedFlag = false;
    Handler mHandler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mHandler = new Handler(Looper.getMainLooper());

        findViewById(R.id.launchCamera).setOnClickListener(this);

        demoVideoFolder = Environment.getExternalStorageDirectory().getAbsolutePath() + "/SimpleCamera/";

        Log.i(Prefs.TAG, getString(R.string.app_name) + " version: " + GeneralUtils.getVersionName(getApplicationContext()));
        workFolder = getApplicationContext().getFilesDir().getAbsolutePath() + "/";
        vkLogPath = workFolder + "vk.log";

        GeneralUtils.copyLicenseFromAssetsToSDIfNeeded(this, workFolder);
        GeneralUtils.copyDemoVideoFromAssetsToSDIfNeeded(this, demoVideoFolder);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_RQ);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void onClick(View view) {
        File saveDir = null;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            saveDir = new File(Environment.getExternalStorageDirectory(), "SimpleCamera");
            saveDir.mkdirs();
        }

        new MaterialCamera(this)
                .saveDir(saveDir)
                .allowRetry(true)
                .autoSubmit(false)
                .showPortraitWarning(false)
                .defaultToFrontFacing(false)
                .start(CAMERA_RQ);
    }

    private String readableFileSize(long size) {
        if (size <= 0) return size + " B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.##").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    private String fileSize(File file) {
        return readableFileSize(file.length());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Received recording or error from SampleCamera
        if (requestCode == CAMERA_RQ) {
            if (resultCode == RESULT_OK) {
                final File file = new File(data.getData().getPath());
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        showDialog(MainActivity.this, "Select Resolution", new String[]{"Cancel", "Select"}, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                if (which == 0) {
                                    dialog.dismiss();
                                    if (GeneralUtils.checkIfFileExistAndNotEmpty(file.getAbsolutePath())) {
                                        new TranscdingBackground(MainActivity.this, file.getAbsolutePath(), file.getName(), "160x120", file).execute();
                                    } else {
                                        Toast.makeText(getApplicationContext(), file.getAbsolutePath() + " not found", Toast.LENGTH_LONG).show();
                                    }
                                } else if (which == 1) {
                                    dialog.dismiss();
                                    if (GeneralUtils.checkIfFileExistAndNotEmpty(file.getAbsolutePath())) {
                                        new TranscdingBackground(MainActivity.this, file.getAbsolutePath(), file.getName(), "320x240", file).execute();
                                    } else {
                                        Toast.makeText(getApplicationContext(), file.getAbsolutePath() + " not found", Toast.LENGTH_LONG).show();
                                    }
                                }
                            }
                        });
                    }
                });

            } else if (data != null) {
                Exception e = (Exception) data.getSerializableExtra(MaterialCamera.ERROR_EXTRA);
                if (e != null) {
                    e.printStackTrace();
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    public void showDialog(Context context, String title, String[] btnText,
                           DialogInterface.OnClickListener listener) {

        final CharSequence[] items = {"160x120", "320x240"};

        if (listener == null) {
            listener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface paramDialogInterface,
                                    int paramInt) {
                    paramDialogInterface.dismiss();
                }
            };
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);

        final DialogInterface.OnClickListener finalListener = listener;
        builder.setSingleChoiceItems(items, -1,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        finalListener.onClick(dialog, item);
                    }
                });
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                finalListener.onClick(dialog, -1);
            }
        });
        builder.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {

            Toast.makeText(this, "Videos will be saved in a cache directory instead of an external storage directory since permission was denied.", Toast.LENGTH_LONG).show();
        }
    }

    public class TranscdingBackground extends AsyncTask<String, Integer, Integer> {

        ProgressDialog progressDialog;
        Activity _act;
        String commandStr;
        String _path;
        String _FileName;
        String _reso;
        File _File;

        public TranscdingBackground(Activity act, String path, String FileName, String reso, File file) {
            _act = act;
            _path = path;
            _FileName = FileName;
            _reso = reso;
            _File = file;
        }


        @Override
        protected void onPreExecute() {

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HH_mm_ss");
            String currentTimeStamp = dateFormat.format(new Date());

            commandStr = "ffmpeg -y -i /sdcard/SimpleCamera/" + _FileName + " -strict experimental -s " + _reso + " -r 30 -aspect 3:4 -ab 48000 -ac 2 -ar 22050 -vcodec mpeg4 -b 2097152 /sdcard/SimpleCamera/VID_" + currentTimeStamp + ".mp4";

            progressDialog = new ProgressDialog(_act);
            progressDialog.setMessage("FFmpeg4Android Transcoding in progress...");
            progressDialog.show();

        }

        protected Integer doInBackground(String... paths) {
            Log.i(Prefs.TAG, "doInBackground started...");

            PowerManager powerManager = (PowerManager) _act.getSystemService(Activity.POWER_SERVICE);
            PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "VK_LOCK");
            Log.d(Prefs.TAG, "Acquire wake lock");
            wakeLock.acquire();

            LoadJNI vk = new LoadJNI();
            try {

                vk.run(GeneralUtils.utilConvertToComplex(commandStr), workFolder, getApplicationContext());

                GeneralUtils.copyFileToFolder(vkLogPath, demoVideoFolder);

            } catch (Throwable e) {
                Log.e(Prefs.TAG, e.getMessage());
            } finally {
                if (wakeLock.isHeld())
                    wakeLock.release();
                else {
                    Log.i(Prefs.TAG, "Wake lock is already released, doing nothing");
                }
            }
            return Integer.valueOf(0);
        }

        protected void onProgressUpdate(Integer... progress) {
        }

        @Override
        protected void onCancelled() {
            Log.i(Prefs.TAG, "onCancelled");
            super.onCancelled();
        }


        @Override
        protected void onPostExecute(Integer result) {
            Log.i(Prefs.TAG, "onPostExecute");
            progressDialog.dismiss();
            super.onPostExecute(result);

            String rc = null;
            if (commandValidationFailedFlag) {
                rc = "Command Vaidation Failed";
            } else {
                rc = GeneralUtils.getReturnCodeFromLog(vkLogPath);
            }
            final String status = rc;
            MainActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    _File.delete();
                    Toast.makeText(MainActivity.this, status, Toast.LENGTH_LONG).show();
                    if (status.equals("Transcoding Status: Failed")) {
                        Toast.makeText(MainActivity.this, "Check: " + vkLogPath + " for more information.", Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }
}
