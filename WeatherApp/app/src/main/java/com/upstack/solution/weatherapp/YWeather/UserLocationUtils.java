package com.upstack.solution.weatherapp.YWeather;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;

import java.util.Timer;
import java.util.TimerTask;

/*
 * need permission
 * <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
 */

class UserLocationUtils {

    private Timer mTimer;
    private LocationManager mLocationManager;
    private LocationResult mLocationResult;
    private boolean mGpsEnabled = false;
    private boolean mNetworkEnabled = false;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private GetLastLocation mGetLastLocationTask;
    private Context context;

    public boolean findUserLocation(Context context, LocationResult result) {
        mLocationResult = result;
        if (mLocationManager == null)
            mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        //exceptions will be thrown if provider is not permitted.
        try {
            mGpsEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
        }
        try {
            mNetworkEnabled = mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) {
        }

        //don't start listeners if no provider is enabled
        if (!mGpsEnabled && !mNetworkEnabled)
            return false;

        if (mGpsEnabled)
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return true;
            }
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListenerGps);
        if (mNetworkEnabled)
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListenerNetwork);
        mGetLastLocationTask = new GetLastLocation();
        mHandler.postDelayed(mGetLastLocationTask, 20000);
        return true;
    }

    LocationListener locationListenerGps = new LocationListener() {
        public void onLocationChanged(Location location) {
            if (mTimer != null) mTimer.cancel();
            mLocationResult.gotLocation(location);
            mLocationManager.removeUpdates(this);
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            mLocationManager.removeUpdates(locationListenerNetwork);
            if (mGetLastLocationTask != null) {
                mHandler.removeCallbacks(mGetLastLocationTask);
                mGetLastLocationTask = null;
            }
        }

        public void onProviderDisabled(String provider) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    };

    LocationListener locationListenerNetwork = new LocationListener() {
        public void onLocationChanged(Location location) {
            if (mTimer != null) mTimer.cancel();
            mLocationResult.gotLocation(location);
            mLocationManager.removeUpdates(this);
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            mLocationManager.removeUpdates(locationListenerGps);
            if (mGetLastLocationTask != null) {
                mHandler.removeCallbacks(mGetLastLocationTask);
                mGetLastLocationTask = null;
            }
        }

        public void onProviderDisabled(String provider) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    };

    class GetLastLocation extends TimerTask {
        @Override
        public void run() {
            YahooWeatherLog.d("20 secs timeout for GPS. GetLastLocation is executed.");
            mLocationManager.removeUpdates(locationListenerGps);
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            mLocationManager.removeUpdates(locationListenerNetwork);

            Location net_loc=null, gps_loc=null;
            if(mGpsEnabled)
                gps_loc=mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if(mNetworkEnabled)
                net_loc=mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            //if there are both values use the latest one
            if(gps_loc!=null && net_loc!=null){
                if(gps_loc.getTime()>net_loc.getTime())
                    mLocationResult.gotLocation(gps_loc);
                else
                    mLocationResult.gotLocation(net_loc);
                return;
            }

            if(gps_loc!=null){
                mLocationResult.gotLocation(gps_loc);
                return;
            }
            if(net_loc!=null){
                mLocationResult.gotLocation(net_loc);
                return;
            }
            mLocationResult.gotLocation(null);
        }
    }

    public static interface LocationResult{
        public abstract void gotLocation(Location location);
    }
}