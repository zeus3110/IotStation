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

import android.util.Log;

import com.zeus3110.android_things_driver.Display.SB1602B;
import com.zeus3110.android_things_driver.IOExpander.MCP23008;
import com.zeus3110.android_things_driver.Sensor.BME280;
import com.zeus3110.android_things_driver.Sensor.MhZ19Pwm;
import com.zeus3110.android_things_driver.Sensor.TSL2561;
import com.zeus3110.android_things_driver.Sensor.Veml6070;
import com.zeus3110.android_things_driver.Sensor.DSM501A;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

public class DeviceServer {
    private static final String TAG = DeviceServer.class.getSimpleName();

    private Veml6070 mUVSensor;
    private MhZ19Pwm mCO2Sensor;
    private TSL2561 mLumiSensor;
    private BME280 mTempSensor;
    private MCP23008 mIOExpander;
    private DSM501A mDustSensor;
    private SB1602B mLCD;

    private ConfigurationBuilder cb;
    TwitterFactory factory;

    ServerSocket serverSocket;
    String message = "";
    static final int socketServerPORT = 12345;


    public DeviceServer() throws IOException {
        Log.i(TAG, "Command Server Thread Start");
        Thread socketServerThread = new Thread(new SocketServerThread());
        socketServerThread.start();

        Log.i(TAG, "Twetter Auth Setting Start");
        TwitterSetting();

        Log.i(TAG, "Registering PWM Input CO2 Sensor driver");
        mCO2Sensor = new MhZ19Pwm(BoardDefaults.getGPIOForPwmIn());

        Log.i(TAG, "Registering PWM Input Dust Sensor driver");
        mDustSensor = new DSM501A("BCM4");

        Thread thread = new Thread(new Runnable(){
            public void run(){
                try {
                    InitializeDevice();
                } catch (IOException e) {
                    Log.i(TAG,"Device Initialize error");
                } catch (InterruptedException e) {
                    Log.e(TAG, "LCD Initialize error", e);
                }
            }
        });
        thread.start();
    }

    public int getPort() {
        return socketServerPORT;
    }

    public void onDestroy() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private class SocketServerThread extends Thread {

        int count = 0;

        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(socketServerPORT);

                while (true) {
                    Socket socket = serverSocket.accept();
                    count++;
                    String msg = "#" + count + " from "
                            + socket.getInetAddress() + ":"
                            + socket.getPort();

                    Log.i(TAG,msg);

                    SocketServerReplyThread socketServerReplyThread = new SocketServerReplyThread(
                            socket, count);
                    socketServerReplyThread.run();

                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }

    private class SocketServerReplyThread extends Thread {

        private Socket hostThreadSocket;
        int cnt;

        SocketServerReplyThread(Socket socket, int c) {
            hostThreadSocket = socket;
            cnt = c;
        }

        @Override
        public void run() {
            OutputStream outputStream;
            InputStream inputStream;

            String msgReply = "Hello from Server, you are #" + cnt;
            byte[] buffer = new byte[8192];

            try {
                outputStream = new BufferedOutputStream(hostThreadSocket.getOutputStream());
                inputStream = new BufferedInputStream( hostThreadSocket.getInputStream() );

                String msg = "replayed: " + msgReply + "\n";
                Log.i(TAG,msg);

                int count;
                while((count = inputStream.read(buffer)) >= 0) {
                    byte command_buf[] =new byte[count];
                    System.arraycopy(buffer,0,command_buf,0,count);
                    String commandStr = new String(command_buf,"UTF-8");
                    if(commandStr != null) {
                        String res_str;
                        byte res[];
                        switch(commandStr) {
                            case "TMP":
                                Log.i(TAG,"Temperature command");
                                float tmp= mTempSensor.readTemperature();
                                res_str = String.format("%.1f", tmp);
                                res = res_str.getBytes("UTF-8");
                                outputStream.write(res, 0, res.length);
                                outputStream.flush();
                                break;
                            case "HMD":
                                Log.i(TAG,"Humidity command");
                                float hmd= mTempSensor.readHumidity();
                                res_str = String.format("%.1f", hmd);
                                res = res_str.getBytes("UTF-8");
                                outputStream.write(res, 0, res.length);
                                outputStream.flush();
                                break;
                            case "PRS":
                                Log.i(TAG,"Pressure command");
                                float prs= mTempSensor.readPressure();
                                res_str = String.format("%.1f", prs);
                                res = res_str.getBytes("UTF-8");
                                outputStream.write(res, 0, res.length);
                                outputStream.flush();
                                break;
                            case "LUM":
                                Log.i(TAG,"Luminance command");
                                float lux= mLumiSensor.GetLuxData();
                                res_str = String.format("%.1f", lux);
                                res = res_str.getBytes("UTF-8");
                                outputStream.write(res, 0, res.length);
                                outputStream.flush();
                                break;
                            case "CO2":
                                Log.i(TAG,"CO2 command");
                                float co2= mCO2Sensor.GetCO2PPM();
                                res_str = String.format("%.1f", co2);
                                res = res_str.getBytes("UTF-8");
                                outputStream.write(res, 0, res.length);
                                outputStream.flush();
                                break;
                            case "UVS":
                                Log.i(TAG,"UV command");
                                float uv= mUVSensor.ReadUVData();
                                res_str = String.format("%.1f", uv);
                                res = res_str.getBytes("UTF-8");
                                outputStream.write(res, 0, res.length);
                                outputStream.flush();
                                break;
                            case "INP1":
                            case "INP2":
                            case "INP3":
                            case "INP4":
                                Log.i(TAG,"Input command: " + commandStr);
                                int portIn=(int)(mIOExpander.ReadInputs());
                                int reqPort = Integer.parseInt(commandStr.substring(3));
                                if( (portIn & (0x08 << reqPort)) != 0x00) {
                                    outputStream.write("OFF".getBytes(), 0, 3);
                                }
                                else {
                                    outputStream.write("ON".getBytes(), 0, 2);
                                }
                                outputStream.flush();
                                break;
                            case "OUT1ON":
                            case "OUT1OFF":
                            case "OUT2ON":
                            case "OUT2OFF":
                            case "OUT3ON":
                            case "OUT3OFF":
                            case "OUT4ON":
                            case "OUT4OFF":
                                Log.i(TAG,"Output command: " + commandStr);
                                int portCurrent=(int)(mIOExpander.ReadOutputs());
                                int reqOutPort = Integer.parseInt(commandStr.substring(3,4));
                                int portOut;
                                if( commandStr.substring(4).equals("ON") ) {
                                    portOut = portCurrent | ( 0x01 << ( reqOutPort -1) );
                                    mIOExpander.WriteOutputs( (byte)portOut );
                                } else {
                                    portOut = portCurrent & ~( 0x01 << ( reqOutPort -1) );
                                    mIOExpander.WriteOutputs( (byte)portOut );
                                }
                                Log.i(TAG,"Port:"+String.valueOf(reqOutPort) + " COM:" + commandStr.substring(4) + " NOW:" + String.valueOf(portCurrent) + " OUT:" + portOut);
                                outputStream.write(command_buf, 0, count);
                                outputStream.flush();
                                break;
                            default:
                                Log.i(TAG,"Unknown command");
                                outputStream.write(command_buf, 0, count);
                                outputStream.flush();
                        }
                    }


                }

                hostThreadSocket.shutdownOutput(); // half close


            } catch(SocketException e) {
                e.printStackTrace();
                String msg = "Socket error! " + e.toString();
                Log.e(TAG,msg);

            } catch(IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                String msg = "Something wrong! " + e.toString();
                Log.e(TAG,msg);
            }
        }

    }

    public void InitializeDevice() throws IOException, InterruptedException {
        Log.i(TAG, "Registering I2C UV Sensor driver");
        mUVSensor=new Veml6070(BoardDefaults.getI2cBus());
        mUVSensor.setMode(Veml6070.IT_4);

        Log.i(TAG, "Registering I2C Luminance Sensor driver");
        mLumiSensor = new TSL2561(BoardDefaults.getI2cBus(),TSL2561.TSL2561_ADDRESS_GND);
        mLumiSensor.SetGainAndIntegtime(TSL2561.TIMING_GAIN_16,TSL2561.TIMING_TIME_402);

        Log.i(TAG, "Registering I2C Temperature Sensor driver");
        mTempSensor = new BME280(BoardDefaults.getI2cBus());
        mTempSensor.setMode(BME280.MODE_NORMAL);
        mTempSensor.setTemperatureOversampling(BME280.OVERSAMPLING_1X);
        mTempSensor.setPressureOversampling(BME280.OVERSAMPLING_1X);
        mTempSensor.setHumidityOversampling(BME280.OVERSAMPLING_1X);

        Log.i(TAG, "Registering I2C GPIO Expander driver");
        mIOExpander = new MCP23008(BoardDefaults.getI2cBus(), MCP23008.MCP23008_ADDRESS0);
        // set Pin0-3 to output pin
        mIOExpander.SetOutputPins(MCP23008.MCP23008_PIN3|MCP23008.MCP23008_PIN2|MCP23008.MCP23008_PIN1|MCP23008.MCP23008_PIN0);

        Log.i(TAG, "Registering I2C LCD driver");
        String ip;
        ip = getIpAddress();
        Log.i(TAG, "IP Address: " + ip);
        mLCD = new SB1602B(BoardDefaults.getI2cBus());
        mLCD.setContrast(0x3E);
        mLCD.putByteArray(0,ip.getBytes("US-ASCII"));
    }

    private void TwitterSetting() {
        cb = new ConfigurationBuilder();
        // change your key and token
        cb.setDebugEnabled(true)
                .setOAuthConsumerKey(TwitterKey.CONSUMER_KEY)
                .setOAuthConsumerSecret(TwitterKey.CONSUMER_SECRET)
                .setOAuthAccessToken(TwitterKey.ACCESS_TOKEN_KEY)
                .setOAuthAccessTokenSecret(TwitterKey.ACCESS_TOKEN_SECRET);
        factory = new TwitterFactory(cb.build());
        Log.i(TAG, "Twetter Auth Setting End");
    }

    public void Tweet(){
        float lux_data;
        float temp_data;
        float press_data;
        float humid_data;
        float uv_data;
        int co2_data;
        float dust_data;
        String tweetStr;
        Log.i(TAG, "Tweet Event");
        try{
            uv_data=mUVSensor.ReadUVData();
            lux_data=mLumiSensor.GetLuxData();
            co2_data = mCO2Sensor.GetCO2PPM();
            temp_data = mTempSensor.readTemperature();
            press_data = mTempSensor.readPressure();
            humid_data = mTempSensor.readHumidity();
            dust_data = mDustSensor.GetDustDensity();

            Log.i(TAG,"Sensor Data: "+String.valueOf(uv_data)+" uW/cm²");
            Log.i(TAG,"Luminance Data: "+String.valueOf(lux_data)+" lux");
            Log.i(TAG,"Sensor Data: "+String.valueOf(co2_data)+" ppm");
            Log.i(TAG,"Temperature Data: "+String.valueOf(temp_data)+" ℃");
            Log.i(TAG,"Humidity Data: "+String.valueOf(humid_data)+" %");
            Log.i(TAG,"Pressure Data: "+String.valueOf(press_data)+" hPa");
            Log.i(TAG,"Dust Data: "+String.valueOf(dust_data)+" mg/㎥");

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
            tweetStr = tweetStr + "Dust: " + String.format("%.2f", press_data)+" mg/㎥\n";
            new TweetAsyncTask(tweetStr, factory).execute();

        } catch (IOException e) {
            Log.e(TAG, "Error Sensor Data Read", e);
        } catch (Exception e){
            Log.e(TAG, "Other Error", e);
        }
    }

    public String getIpAddress() {
        String ip = "";
        try {
            Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface
                    .getNetworkInterfaces();
            while (enumNetworkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = enumNetworkInterfaces
                        .nextElement();
                Enumeration<InetAddress> enumInetAddress = networkInterface
                        .getInetAddresses();
                while (enumInetAddress.hasMoreElements()) {
                    InetAddress inetAddress = enumInetAddress
                            .nextElement();

                    if (inetAddress.isSiteLocalAddress()) {
                        ip += inetAddress.getHostAddress();
                    }
                }
            }

        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return ip;
    }
}
