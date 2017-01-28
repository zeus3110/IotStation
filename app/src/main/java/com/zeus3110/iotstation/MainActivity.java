/*
 * Copyright 2017 zeus3110
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


public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private ButtonInputDriver mButtonInputDriver;

    public DeviceServer deviceServer;

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

            Log.i(TAG, "Registered Drivers");
            deviceServer = new DeviceServer();


            Log.i(TAG, "Timer Setting Start");
            _handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.i(TAG, "Timer Event Start");
                    deviceServer.Tweet();
                    _handler.postDelayed(this, TWEET_PERIOD);
                }
            }, TWEET_PERIOD);
            Log.i(TAG, "Timer Setting End");

        } catch (IOException e) {
            Log.e(TAG, "Error configuring devices.", e);
        }
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_SPACE) {
            Log.i(TAG,"Key Down Event");
            deviceServer.Tweet();
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
