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

import com.google.android.things.pio.PeripheralManagerService;
import com.google.android.things.pio.SpiDevice;


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
import java.util.Enumeration;

public class IRCommandServer {
    private static final String TAG = IRCommandServer.class.getSimpleName();

    ServerSocket serverSocket;
    String message = "";
    static final int socketServerPORT = 12346;

    private SpiDevice mDevice;

    public IRCommandServer() throws IOException {
        Log.i(TAG, "IR Command Server Thread Start");
        Thread socketServerThread = new Thread(new SocketServerThread());
        socketServerThread.start();

        Thread thread = new Thread(new Runnable(){
            public void run(){
                try {
                    InitializeDevice();
                } catch (IOException e) {
                    Log.i(TAG,"Device Initialize error");
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

        if (mDevice != null) {
            try {
                mDevice.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing SPI driver", e);
            } finally{
                mDevice = null;
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
                    (new Thread(new txTestThread(command_buf))).start();

                    /*
                    if(commandStr != null) {
                        String res_str;
                        byte res[];
                        switch(commandStr) {
                            case "IR":
                                Log.i(TAG,"Temperature command");
                                SendSignal();
                                break;
                            default:
                                Log.i(TAG,"Unknown command");
                                outputStream.write(command_buf, 0, count);
                                outputStream.flush();
                        }
                    } */
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

    private void InitializeDevice() throws IOException {
        Log.i(TAG, "Registering SPI device driver");
        PeripheralManagerService manager = new PeripheralManagerService();
        mDevice = manager.openSpiDevice(BoardDefaults.getSPIPort());

        mDevice.setMode(SpiDevice.MODE0);
        mDevice.setFrequency(10000000);      // 10MHz
        mDevice.setBitJustification(false); // MSB first
        mDevice.setBitsPerWord(8);          // 8 BPW
        mDevice.setCsChange(false);          // Enable CS
    }

    public void SendSignal() {
        byte[] txData = new byte[]{0x01,0x07,0x00,(byte)0xBA,0x00,0x5B,0x00,0x1D,0x00,0x1D,0x00,0x1E,0x00,0x3E,0x00,0x1D,0x00,0x3E,0x00,0x1F,0x00,0x1C,0x00,0x1D,0x00,0x3F,0x00,0x1E,0x00,0x3E,0x00,0x1D,0x00,0x1D,0x00,0x1E,0x00,0x1B,0x00,0x1E,0x00,0x1C,0x00,0x1E,0x00,0x3E,0x00,0x1E,0x00,0x3E,0x00,0x1E,0x00,0x1C,0x00,0x1E,0x00,0x3D,0x00,0x1F,0x00,0x3E,0x00,0x1E,0x00,0x3D,0x00,0x1F,0x00,0x1C,0x00,0x1E,0x00,0x1B,0x00,0x1E,0x00,0x3E,0x00,0x1E,0x00,0x1C,0x00,0x1E,0x00,0x1C,0x00,0x1E,0x00,0x1C,0x00,0x1E,0x00,0x1C,0x00,0x1E,0x00,0x3E,0x00,0x1E,0x00,0x1C,0x00,0x1E,0x00,0x1C,0x00,0x1E,0x00,0x1B,0x00,0x1F,0x00,0x1C,0x00,0x1E,0x00,0x1B,0x00,0x1E,0x00,0x1C,0x00,0x1E,0x00,0x1C,0x00,0x1E,0x00,0x1C,0x00,0x1E,0x00,0x1C,0x00,0x1E};
        (new Thread(new txTestThread(txData))).start();
    }

    private class txTestThread implements Runnable {
        byte[] txData;

        public txTestThread (byte txDataIn[]) {
            txData=txDataIn.clone();
        }

        @Override
        public void run() {
            try {
                mDevice.write(txData,txData.length);
            } catch (IOException e) {
                Log.e(TAG, "Error on PeripheralIO API", e);
            }
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
