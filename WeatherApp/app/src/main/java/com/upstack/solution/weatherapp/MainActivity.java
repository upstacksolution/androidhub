package com.upstack.solution.weatherapp;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.upstack.solution.weatherapp.Adapter.WeatherListAdapter;
import com.upstack.solution.weatherapp.Utils.AppMethods;
import com.upstack.solution.weatherapp.Utils.Constants;
import com.upstack.solution.weatherapp.YWeather.WeatherInfo;
import com.upstack.solution.weatherapp.YWeather.YahooWeather;
import com.upstack.solution.weatherapp.YWeather.YahooWeatherExceptionListener;
import com.upstack.solution.weatherapp.YWeather.YahooWeatherInfoListener;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;


public class MainActivity extends AppCompatActivity implements YahooWeatherExceptionListener, YahooWeatherInfoListener, WeatherListAdapter.OnItemRecycleViewClickListener {

    TextView todayTemperature;
    TextView todayDescription;
    TextView todayWind;
    TextView todayPressure;
    TextView todayHumidity;
    TextView todaySunrise;
    TextView todaySunset;
    TextView lastUpdate;
    ImageView todayIcon;
    Toolbar toolbar;
    WeatherInfo DefaultWeatherInfo;
    RecyclerView WeatherRecyclerView;
    YahooWeather mYahooWeather = YahooWeather.getInstance(5000, 5000, true);
    WeatherListAdapter mWeatherListAdapter;
    private List<WeatherInfo.ForecastInfo> mForecastInfos;
    ProgressDialog progressDialog;
    String recentCity = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mYahooWeather.setExceptionListener(MainActivity.this);

        initHeader();
        initView();
    }

    private void initHeader() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    private void initView() {
        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setMessage("Loading Data...");
        todayTemperature = (TextView) findViewById(R.id.todayTemperature);
        todayDescription = (TextView) findViewById(R.id.todayDescription);
        todayWind = (TextView) findViewById(R.id.todayWind);
        todayPressure = (TextView) findViewById(R.id.todayPressure);
        todayHumidity = (TextView) findViewById(R.id.todayHumidity);
        todaySunrise = (TextView) findViewById(R.id.todaySunrise);
        todaySunset = (TextView) findViewById(R.id.todaySunset);
        lastUpdate = (TextView) findViewById(R.id.lastUpdate);
        todayIcon = (ImageView) findViewById(R.id.todayIcon);


        LinearLayoutManager layoutManager
                = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);

        WeatherRecyclerView = (RecyclerView) findViewById(R.id.WeatherRecyclerView);
        WeatherRecyclerView.setLayoutManager(layoutManager);

        mWeatherListAdapter = new WeatherListAdapter(MainActivity.this, mForecastInfos, MainActivity.this);
        WeatherRecyclerView.setAdapter(mWeatherListAdapter);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        recentCity = preferences.getString("city", Constants.DEFAULT_CITY);
        String recentData = preferences.getString(Constants.DEFAULT_WEATHER, "");
        Log.d("mytag", "recentData::>>" + recentData);
/*        Gson gson = new Gson();
        DefaultWeatherInfo = gson.fromJson(recentData, WeatherInfo.class);*/
        setDefaultData(DefaultWeatherInfo);
        searchByPlaceName(recentCity);

    }

    private void setDefaultData(WeatherInfo weatherInfo) {
        try {
            if (weatherInfo != null) {
              /*  mForecastInfos = new ArrayList<>();
                for (int i = 0; i < YahooWeather.FORECAST_INFO_MAX_SIZE; i++) {
                    final WeatherInfo.ForecastInfo forecastInfo = weatherInfo.getForecastInfoList().get(i);
                    mForecastInfos.add(forecastInfo);
                }
*/
                String city = weatherInfo.getLocationCity();
                String country = weatherInfo.getLocationCountry();
                getSupportActionBar().setTitle(city + (country.isEmpty() ? "" : ", " + country));
                float temperature = weatherInfo.getCurrentTemp();

                todayTemperature.setText(temperature + " °" + "C");
                todayDescription.setText(weatherInfo.getCurrentText());
                todayWind.setText("Wind: " + weatherInfo.getWindSpeed() + " km/h");
                todayPressure.setText("Pressure: " + weatherInfo.getAtmospherePressure() + " in");
                todayHumidity.setText("Humidity: " + weatherInfo.getAtmosphereHumidity() + " %");
                todaySunrise.setText("Sunrise: " + weatherInfo.getAstronomySunrise());
                todaySunset.setText("Sunset: " + weatherInfo.getAstronomySunset());
                //todayIcon.setImageBitmap(weatherInfo.getCurrentConditionIcon());

                lastUpdate.setText("" + formatTimeWithDayIfNotToday(MainActivity.this, System.currentTimeMillis()));
                mWeatherListAdapter.SetWeatherData(mForecastInfos);
            } else {

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_scrolling, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_refresh) {
            if (AppMethods.isNetworkAvailable(MainActivity.this)) {
                searchByPlaceName(recentCity);
            } else {

            }
            return true;
        }
        if (id == R.id.action_search) {
            searchCities();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void searchCities() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(this.getString(R.string.search_title));
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setMaxLines(1);
        input.setSingleLine(true);
        alert.setView(input, 32, 0, 32, 0);
        alert.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String result = input.getText().toString();
                if (!result.isEmpty()) {
                    saveLocation(result);
                }
            }
        });
        alert.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Cancelled
            }
        });
        alert.show();
    }

    private void saveLocation(String result) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        recentCity = preferences.getString("city", Constants.DEFAULT_CITY);

        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("city", result);
        editor.commit();

        if (!recentCity.equals(result)) {
            recentCity = result;
            searchByPlaceName(result);
        }
    }

    private void ShowProgress(boolean b) {
        if (b) {
            progressDialog.show();
        } else {
            progressDialog.dismiss();
        }
    }

    private void searchByPlaceName(String location) {
        ShowProgress(true);
        mYahooWeather.setNeedDownloadIcons(true);
        mYahooWeather.setUnit(YahooWeather.UNIT.CELSIUS);
        mYahooWeather.setSearchMode(YahooWeather.SEARCH_MODE.PLACE_NAME);
        mYahooWeather.queryYahooWeatherByPlaceName(getApplicationContext(), location, MainActivity.this);
    }

    @Override
    public void onFailConnection(Exception e) {

    }

    @Override
    public void onFailParsing(Exception e) {

    }

    @Override
    public void onFailFindLocation(Exception e) {

    }

    @Override
    public void gotWeatherInfo(WeatherInfo weatherInfo) {
        if (weatherInfo != null) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
            Gson gson = new Gson();
            String JsonData = gson.toJson(weatherInfo, WeatherInfo.class);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(Constants.DEFAULT_WEATHER, JsonData);
            editor.commit();

            ShowProgress(false);
            mForecastInfos = new ArrayList<>();
            for (int i = 0; i < YahooWeather.FORECAST_INFO_MAX_SIZE; i++) {
                final WeatherInfo.ForecastInfo forecastInfo = weatherInfo.getForecastInfoList().get(i);
                mForecastInfos.add(forecastInfo);
            }

            String city = weatherInfo.getLocationCity();
            String country = weatherInfo.getLocationCountry();
            getSupportActionBar().setTitle(city + (country.isEmpty() ? "" : ", " + country));
            float temperature = weatherInfo.getCurrentTemp();

            todayTemperature.setText(temperature + " °" + "C");
            todayDescription.setText(weatherInfo.getCurrentText());
            todayWind.setText("Wind: " + weatherInfo.getWindSpeed() + " km/h");
            todayPressure.setText("Pressure: " + weatherInfo.getAtmospherePressure() + " in");
            todayHumidity.setText("Humidity: " + weatherInfo.getAtmosphereHumidity() + " %");
            todaySunrise.setText("Sunrise: " + weatherInfo.getAstronomySunrise());
            todaySunset.setText("Sunset: " + weatherInfo.getAstronomySunset());
             todayIcon.setImageBitmap(weatherInfo.getCurrentConditionIcon());

            lastUpdate.setText("" + formatTimeWithDayIfNotToday(MainActivity.this, System.currentTimeMillis()));
            mWeatherListAdapter.SetWeatherData(mForecastInfos);
        } else {

        }
    }

    public static String formatTimeWithDayIfNotToday(Context context, long timeInMillis) {
        Calendar now = Calendar.getInstance();
        Calendar lastCheckedCal = new GregorianCalendar();
        lastCheckedCal.setTimeInMillis(timeInMillis);
        Date lastCheckedDate = new Date(timeInMillis);
        String timeFormat = android.text.format.DateFormat.getTimeFormat(context).format(lastCheckedDate);
        if (now.get(Calendar.YEAR) == lastCheckedCal.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == lastCheckedCal.get(Calendar.DAY_OF_YEAR)) {
            // Same day, only show time
            return timeFormat;
        } else {
            return android.text.format.DateFormat.getDateFormat(context).format(lastCheckedDate) + " " + timeFormat;
        }
    }

    @Override
    public void onItemClicked(int position, WeatherListAdapter mAdapter) {

    }
}
