/*
 * Copyright 2016 zeus3110
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

package com.zeus3110.iotstation;

import android.app.Activity;
import android.os.Bundle;

import com.google.android.things.contrib.driver.button.Button;
import com.google.android.things.contrib.driver.button.ButtonInputDriver;

import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.GregorianCalendar;

import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;


public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private ButtonInputDriver mButtonInputDriver;
    private Veml6070 mUVSensor;
    private MhZ19Pwm mCO2Sensor;
    private TSL2561 mLumiSensor;
    private BME280 mTempSensor;

    private ConfigurationBuilder cb;
    TwitterFactory factory;

    // Timer
    private final int TWEET_PERIOD = 10*60*1000;    // tweet every 10min
    Handler _handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "Starting ButtonActivity");

        try {
            Log.i(TAG, "Registering button driver");
            // Initialize and register the InputDriver that will emit SPACE key events
            // on GPIO state changes.
            mButtonInputDriver = new ButtonInputDriver(
                    BoardDefaults.getGPIOForButton(),
                    Button.LogicState.PRESSED_WHEN_LOW,
                    KeyEvent.KEYCODE_SPACE);
            mButtonInputDriver.register();


            Log.i(TAG, "Registering I2C UV Sensor driver");
            mUVSensor=new Veml6070(BoardDefaults.getI2cBus());
            mUVSensor.setMode(Veml6070.IT_4);

            Log.i(TAG, "Registering PWM Input CO2 Sensor driver");
            mCO2Sensor = new MhZ19Pwm(BoardDefaults.getGPIOForPwmIn());

            Log.i(TAG, "Registering I2C Luminance Sensor driver");
            mLumiSensor = new TSL2561(BoardDefaults.getI2cBus(),TSL2561.TSL2561_ADDRESS_GND);
            mLumiSensor.SetGainAndIntegtime(TSL2561.TIMING_GAIN_16,TSL2561.TIMING_TIME_402);

            Log.i(TAG, "Registering I2C Temperature Sensor driver");
            mTempSensor = new BME280(BoardDefaults.getI2cBus());
            mTempSensor.setMode(BME280.MODE_NORMAL);
            mTempSensor.setTemperatureOversampling(BME280.OVERSAMPLING_1X);
            mTempSensor.setPressureOversampling(BME280.OVERSAMPLING_1X);
            mTempSensor.setHumidityOversampling(BME280.OVERSAMPLING_1X);

            Log.i(TAG, "Registered Drivers");

            Log.i(TAG, "Twetter Auth Setting Start");
            cb = new ConfigurationBuilder();
            // change your key and token
            cb.setDebugEnabled(true)
                    .setOAuthConsumerKey(TwitterKey.CONSUMER_KEY)
                    .setOAuthConsumerSecret(TwitterKey.CONSUMER_SECRET)
                    .setOAuthAccessToken(TwitterKey.ACCESS_TOKEN_KEY)
                    .setOAuthAccessTokenSecret(TwitterKey.ACCESS_TOKEN_SECRET);
            factory = new TwitterFactory(cb.build());
            Log.i(TAG, "Twetter Auth Setting End");

            Log.i(TAG, "Timer Setting Start");
            _handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.i(TAG, "Timer Event Start");
                    Tweet();
                    _handler.postDelayed(this, TWEET_PERIOD);

                }
            }, TWEET_PERIOD);
            Log.i(TAG, "Timer Setting End");

        } catch (IOException e) {
            Log.e(TAG, "Error configuring GPIO pins", e);
        }
    }

    private void Tweet(){
        float lux_data;
        float temp_data;
        float press_data;
        float humid_data;
        float uv_data;
        int co2_data;
        String tweetStr;
        Log.i(TAG, "Tweet Event");
        try{
            uv_data=mUVSensor.ReadUVData();
            lux_data=mLumiSensor.GetLuxData();
            co2_data = mCO2Sensor.GetCO2PPM();
            temp_data = mTempSensor.readTemperature();
            press_data = mTempSensor.readPressure();
            humid_data = mTempSensor.readHumidity();

            Log.i(TAG,"Sensor Data: "+String.valueOf(uv_data)+" uW/cm²");
            Log.i(TAG,"Luminance Data: "+String.valueOf(lux_data)+" lux");
            Log.i(TAG,"Sensor Data: "+String.valueOf(co2_data)+" ppm");
            Log.i(TAG,"Temperature Data: "+String.valueOf(temp_data)+" ℃");
            Log.i(TAG,"Humidity Data: "+String.valueOf(humid_data)+" %");
            Log.i(TAG,"Pressure Data: "+String.valueOf(press_data)+" hPa");

            // obtain time (JST)
            Date date = new GregorianCalendar().getTime();
            DateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

            TimeZone timeZone = TimeZone.getTimeZone("Japan");
            format.setTimeZone(timeZone);
            tweetStr =  format.format(date);
            tweetStr = tweetStr + " (JST)\n";
            tweetStr = tweetStr + "UV: " + String.format("%.1f", uv_data)+" uW/cm²\n";
            tweetStr = tweetStr + "CO2: " + String.valueOf(co2_data)+" ppm\n";
            tweetStr = tweetStr + "Lux: " + String.format("%.1f", lux_data) +" lux\n";
            tweetStr = tweetStr + "Temp: " + String.format("%.1f", temp_data)+" ℃\n";
            tweetStr = tweetStr + "Humid: " + String.format("%.1f", humid_data)+" %\n";
            tweetStr = tweetStr + "Press: " + String.format("%.1f", press_data)+" hPa\n";
            tweetStr = tweetStr + "from #AndroidThings";
            new TweetAsyncTask(tweetStr, factory).execute();

        } catch (IOException e) {
            Log.e(TAG, "Error Sensor Data Read", e);
        } catch (Exception e){
            Log.e(TAG, "Other Error", e);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_SPACE) {
            Log.i(TAG,"Key Down Event");
            Tweet();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_SPACE) {
            return true;
        }

        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();

        if (mButtonInputDriver != null) {
            mButtonInputDriver.unregister();
            try {
                mButtonInputDriver.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing Button driver", e);
            } finally{
                mButtonInputDriver = null;
            }
        }

        _handler.removeCallbacksAndMessages(null);
    }
}
