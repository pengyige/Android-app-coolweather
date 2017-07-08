package com.example.coolweather.gson;

/**
 * Created by 彭旎 on 2017/7/6.
 */

public class AQI {

    public AQICity city;

    public class AQICity
    {
        public String aqi;
        public String pm25;
    }
}
