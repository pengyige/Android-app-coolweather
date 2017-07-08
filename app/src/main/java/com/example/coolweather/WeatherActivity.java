package com.example.coolweather;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.coolweather.gson.Forecast;
import com.example.coolweather.gson.Weather;
import com.example.coolweather.util.HttpUtil;
import com.example.coolweather.util.Utility;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Random;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by 彭旎 on 2017/7/7.
 */

public class WeatherActivity extends AppCompatActivity {
    private ScrollView weatherLayout;
    private TextView titleCity;
    private TextView titleUpdateTime;
    private TextView degreeText;
    private TextView weatherInfoText;
    private LinearLayout forecastLayout;
    private TextView aqiText;
    private TextView pm25Text;
    private TextView comfortText;
    private TextView carWashText;
    private TextView sportText;

    private ImageView iv_pic;


    //下拉刷新
    public SwipeRefreshLayout swipeRefresh;
    private String mWeatherId;

    //菜单按钮
    private Button btn_Menu;

    //滑动布局
    public DrawerLayout drawerLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*设置布局与状态栏重合*/
      /*  if(Build.VERSION.SDK_INT >= 21)
        {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            *//*状态栏设置为透明*//*
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }*/

        setContentView(R.layout.activity_weather);
        weatherLayout = (ScrollView) findViewById(R.id.weather_layout);
        titleCity = (TextView) findViewById(R.id.title_city);
        titleUpdateTime = (TextView) findViewById(R.id.title_update_time);
        degreeText = (TextView) findViewById(R.id.degree_text);
        weatherInfoText = (TextView) findViewById(R.id.weather_info_text);
        forecastLayout = (LinearLayout) findViewById(R.id.forecast_layout);
        aqiText = (TextView) findViewById(R.id.api_text);
        pm25Text = (TextView) findViewById(R.id.pm25_text);
        comfortText = (TextView) findViewById(R.id.comfort_text);
        carWashText = (TextView) findViewById(R.id.car_wash_text);
        sportText = (TextView) findViewById(R.id.sport_text);
        swipeRefresh = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);
        iv_pic = (ImageView) findViewById(R.id.iv_pic);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        btn_Menu = (Button) findViewById(R.id.btn_Menu);
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary);

        //菜单监听
        btn_Menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPopupMenu(btn_Menu);
            }
        });


        /*下拉监听*/
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestWeather(mWeatherId);
            }
        });

        /*获取天气缓冲*/
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = prefs.getString("weather",null);
        if(weatherString != null)
        {
            //有缓存时直接解析天气数据
            Weather weather = Utility.handleWeatherResponse(weatherString);
            mWeatherId = weather.basic.weatherId;
            showWeatherInfo(weather);
        } else
        {
            /*无缓存时去服务器解析天气数据*/
            String weatherId = getIntent().getStringExtra("weather_id");
            mWeatherId = weatherId;
            weatherLayout.setVisibility(View.INVISIBLE);
            requestWeather(weatherId);
        }

        /*先判断是否有本地图片*/
        String imagePath  = prefs.getString("imagePath",null);
        if(imagePath != null)
        {
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            iv_pic.setImageBitmap(bitmap);
            return ;
        }

        /*获取背景缓冲*/
        String bingPic = prefs.getString("bing_pic",null);
        if(bingPic != null)
        {
            Glide.with(this).load(bingPic).into(iv_pic);
        }
        else
        {
            /*向图片服务器请求*/
            loadBingPic();
        }


    }

    /**
     * 显示弹出菜单
     * */
    public void showPopupMenu(View view)
    {
        /*相对于当前view*/
        PopupMenu popupMenu = new PopupMenu(this,view);
        /*加载子项布局*/
        popupMenu.getMenuInflater().inflate(R.menu.menu,popupMenu.getMenu());
        /*添加监听*/
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.action_ModifyImage)
                {

                    String path = getApplication().getFilesDir().getAbsolutePath();
                    String package_path = getApplicationContext().getPackageResourcePath();
                    Log.d("Path","当前路径"+path);
                    Log.d("Path",package_path);
                      /*检查权限*/
                    if(ContextCompat.checkSelfPermission(WeatherActivity.this
                            , Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                    {
                    /*请求权限*/
                        ActivityCompat.requestPermissions(WeatherActivity.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
                    }
                    else
                    {
                        openAlbum();
                    }
                }else if(item.getItemId() == R.id.action_ModifyCity)
                {
                  drawerLayout.openDrawer(GravityCompat.START);
                }
                else if(item.getItemId() == R.id.action_EveryPic)
                {
                    loadBingPic();
                }
                return false;
            }
        });
        popupMenu.show();


    }


    /**
     *向服务请求图片并载入
     * */
    public void loadBingPic()
    {
        String requestBingPic = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Toast.makeText(WeatherActivity.this,"获取背景图片失败",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bingPic = response.body().string();
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this
                ).edit();
                editor.putString("bing_pic",bingPic);
                editor.apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(bingPic).into(iv_pic);
                    }
                });
            }
        });
    }

   /**
   * 加载背景
   * */
    public void loadImage()
    {
   /*     Random random = new Random();
        int s = random.nextInt(3)+1;
        switch (s)
        {
            case 1:iv_pic.setImageDrawable(getResources().getDrawable(R.drawable.ic_1));break;
            case 2:iv_pic.setImageDrawable(getResources().getDrawable(R.drawable.ic_2));break;
            case 3:iv_pic.setImageDrawable(getResources().getDrawable(R.drawable.ic_2));break;
            case 4:iv_pic.setImageDrawable(getResources().getDrawable(R.drawable.ic_3));break;
            default:break;
        }*/
    /*    iv_pic.setImageResource(R.drawable.ic_1);
        iv_pic.setVisibility(View.VISIBLE);*/
        Toast.makeText(this,"加载完成",Toast.LENGTH_SHORT).show();

    }


    /**
    * 显示Weather实体类中的数据
    * */
    private void showWeatherInfo(Weather weather)
    {
        String cityName = weather.basic.cityName;
        String updateTime = weather.basic.update.updateTime.split(" ")[1];
        String degree = weather.now.temperature + "℃";
        String weatherInfo = weather.now.more.info;
        /*设置第一部分信息*/
        titleCity.setText(cityName);
        titleUpdateTime.setText(updateTime);
        /*设置第二部分信息*/
        degreeText.setText(degree);
        weatherInfoText.setText(weatherInfo);
        forecastLayout.removeAllViews();
        for(Forecast forecast : weather.forecastList)
        {
            /*加载预报天气的子布局*/
            View view = LayoutInflater.from(this).inflate(R.layout.forecast_item,forecastLayout,false);
            TextView dateText = (TextView) view.findViewById(R.id.date_text);
            TextView infoText = (TextView) view.findViewById(R.id.info_text);
            TextView maxText = (TextView) view.findViewById(R.id.max_text);
            TextView minText = (TextView) view.findViewById(R.id.min_text);
            /*设置第三部分信息*/
            dateText.setText(forecast.date);
            infoText.setText(forecast.more.info);
            maxText.setText(forecast.temperature.max);
            minText.setText(forecast.temperature.min);
            /*将View也就是子项添加到布局中*/
            forecastLayout.addView(view);

        }
        if(weather.aqi != null)
        {
            /*设置第四部分信息*/
            aqiText.setText(weather.aqi.city.aqi);
            pm25Text.setText(weather.aqi.city.pm25);
        }
       String comfort = "舒适度" + weather.suggestion.comfort.info;
        String carWash = "洗车指数" + weather.suggestion.carWash.info;
        String sport = "运动建议" + weather.suggestion.sport.info;
        /*设置第五部分信息*/
        comfortText.setText(comfort);
        carWashText.setText(carWash);
        sportText.setText(sport);
        weatherLayout.setVisibility(View.VISIBLE);
    }


    /**
    * 从服务器获取天气id对应的天气信息
    * */
    public void requestWeather(final String weatherId)
    {
        String weatherUrl = "http://guolin.tech/api/weather?cityid="+weatherId+"&key=" +
                "af006af112ce43379cd7981ee413ed8d";
        Log.d("weatherUrl",weatherUrl);
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Toast.makeText(WeatherActivity.this,"Fail,获取天气信息失败",Toast.LENGTH_SHORT).show();
                swipeRefresh.setRefreshing(false);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText = response.body().string();
                final Weather weather = Utility.handleWeatherResponse(responseText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(weather != null && "ok".equals(weather.status))
                        {
                            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this)
                                    .edit();
                            editor.putString("weather",responseText);
                            editor.apply();
                            showWeatherInfo(weather);
                        }
                        else
                        {
                            Toast.makeText(WeatherActivity.this,"获取天气信息失败",Toast.LENGTH_SHORT).show();
                        }
                        /*关闭下拉刷新*/
                        mWeatherId = weather.basic.weatherId;//刷新还是上一次请求的城市
                        swipeRefresh.setRefreshing(false);
                    }
                });
            }
        });
    }

     /*请求回调*/

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    openAlbum();
                }
                else
                {
                    Toast.makeText(this,"你不同这个权限",Toast.LENGTH_SHORT).show();
                }
                break;


            default:break;
        }
    }

    /*打开相册*/
    public void openAlbum()
    {
        Intent intent = new Intent("android.intent.action.GET_CONTENT");
        intent.setType("image/*");
        startActivityForResult(intent,2);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode)
        {
            case 2:
                if(resultCode == RESULT_OK)
                {
                    /*判断手机系统版本号
                     */
                    if(Build.VERSION.SDK_INT >= 19)
                    {
                        /*4.4及以上系统使用这个方法*/
                        Toast.makeText(this,"高版本",Toast.LENGTH_SHORT).show();
                        String imagePath = null;
                        Uri uri = data.getData();
                        if("file".equalsIgnoreCase(uri.getScheme()))
                        {
                            imagePath = uri.getPath();
                        }
                        displayImage(imagePath);
                    }

                }
                break;
            default:break;
        }
    }

    /*显示图片*/
    private void displayImage(String imagePath)
    {
        if(imagePath != null)
        {
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            iv_pic.setImageBitmap(bitmap);
            /*保存图片路径*/
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
            editor.putString("imagePath",imagePath);
            editor.apply();
        }
        else
        {
            Toast.makeText(this,"载入图片失败",Toast.LENGTH_SHORT).show();
        }
    }




}
