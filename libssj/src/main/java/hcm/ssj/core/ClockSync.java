/*
 * ClockSync.java
 * Copyright (c) 2018
 * Authors: Ionut Damian, Michael Dietz, Frank Gaibler, Daniel Langerenken, Simon Flutura,
 * Vitalijs Krumins, Antonio Grieco
 * *****************************************************
 * This file is part of the Social Signal Interpretation for Java (SSJ) framework
 * developed at the Lab for Human Centered Multimedia of the University of Augsburg.
 *
 * SSJ has been inspired by the SSI (http://openssi.net) framework. SSJ is not a
 * one-to-one port of SSI to Java, it is an approximation. Nor does SSJ pretend
 * to offer SSI's comprehensive functionality and performance (this is java after all).
 * Nevertheless, SSJ borrows a lot of programming patterns from SSI.
 *
 * This library is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this library; if not, see <http://www.gnu.org/licenses/>.
 */

package hcm.ssj.core;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

/**
 * Created by Johnny on 25.01.2017.
 */

class ClockSync implements Runnable {

    private final int NUM_REPETITIONS = 10;

    private class SyncListener implements Runnable
    {
        boolean terminate = false;
        int port = 0;

        long delta, rtt, time_send;
        byte[] data = new byte[64];

        SyncListener(int port)
        {
            this.port = port;
            data = new byte[64];
        }

        public long getDelta()
        {
            return delta;
        }

        public void reset(long time_send)
        {
            this.time_send = time_send;
            delta = 0;
            rtt = Long.MAX_VALUE;
        }

        public void terminate()
        {
            terminate = true;
        }

        @Override
        public void run()
        {
            long time_master[] = new long[1];
            byte time_master_byte[] = new byte[8];

            while(!terminate)
            {
                try
                {
                    DatagramPacket packet = new DatagramPacket(data, data.length);
                    recvSocket.receive(packet);
                    Log.d("received packet from " + packet.getAddress().toString());

                    //check data
                    byte[] msg = packet.getData();
                    String str = new String(msg, "ASCII");
                    if (str.startsWith("SSI:SYNC:TIME")) //SSI format for compatibility
                    {
                        Log.d("packet identified as timestamp from master");

                        Util.arraycopy(msg, 12, time_master, 0, 8);
                        long time_recv = frame.getTimeMs();

                        if(time_recv - time_send < rtt)
                        {
                            rtt = time_recv - time_send;
                            delta = time_master[0] - time_recv + rtt/2;
                        }
                    }
                    else if(isMaster && str.startsWith("SSI:SYNC:RQST"))
                    {
                        Log.d("packet identified as time request from slave");

                        time_master[0] = frame.getTimeMs();
                        Util.arraycopy(time_master, 12, time_master_byte, 0, 8);

                        //append time as byte array
                        send("SSI:SYNC:TIME" + new String(time_master_byte));
                    }
                }
                catch (IOException e) {
                    Log.e("error setting up network sync", e);
                }
            }
        }
    }

    private Pipeline frame;
    private DatagramSocket sendSocket;
    private DatagramSocket recvSocket;
    private boolean isMaster = true;
    private InetAddress masterAddr;
    private int port;
    private Timer timer;
    private SyncListener listener;

    public ClockSync(boolean isMaster, InetAddress masterAddr, int port, int interval)
    {
        frame = Pipeline.getInstance();

        this.isMaster = isMaster;
        this.masterAddr = masterAddr;
        this.port = port;

        try {
            sendSocket = new DatagramSocket();
            recvSocket = new DatagramSocket(port);
            sendSocket.setReuseAddress(true);
            recvSocket.setReuseAddress(true);
        } catch (SocketException e) {
            Log.e("error setting up network sync", e);
        }

        timer = new Timer(interval);

        listener = new SyncListener(port);
        new Thread(listener).start();
    }

    @Override
    public void run() {

        while(frame.isRunning())
        {
            if(!isMaster) {
                frame.adjustTime(listener.getDelta());
                listener.reset(frame.getTimeMs());

                //request new time
                send("SSI:SYNC:RQST");
            }

            timer.sync();
        }

        listener.terminate();
    }

    private void send(String msg)
    {
        for(int i = 0; i< NUM_REPETITIONS; i++)
        {
            try
            {
                byte[] data = msg.getBytes("ASCII");
                DatagramPacket packet = new DatagramPacket(data, data.length, masterAddr, port);
                sendSocket.send(packet);
            }
            catch (IOException e) {
                Log.e("error setting up network sync", e);
            }
        }
    }

    /*
     * static helper
     */
    static void listenForStartSignal(int port)
    {
        try
        {
            DatagramSocket syncSocket = new DatagramSocket(port);
            syncSocket.setReuseAddress(true);

            Log.i("waiting for master pipeline (port = " + port + ")");
            while (true) {
                byte[] data = new byte[32];
                DatagramPacket packet = new DatagramPacket(data, 32);
                syncSocket.receive(packet);
                Log.d("received packet from " + packet.getAddress().toString());

                //check data
                String str = new String(packet.getData(), "ASCII");
                if (str.startsWith("SSI:STRT:RUN")) //SSI format for compatibility
                {
                    Log.d("packet identified as start ping");
                    break;
                }
                Log.d("packet not recognized");
            }
        }
        catch(IOException e)
        {
            Log.e("network sync failed", e);
        }
    }

    static void sendStartSignal(int port)
    {
        try
        {
            DatagramSocket syncSocket = new DatagramSocket(null);
            syncSocket.setReuseAddress(true);
            syncSocket.setBroadcast(true);

            String msg = "SSI:STRT:RUN1"; //send in SSI format for compatibility
            byte[] data = msg.getBytes("ASCII");
            DatagramPacket packet = new DatagramPacket(data, data.length, Util.getBroadcastAddress(), port);
            syncSocket.send(packet);

            Log.i("sync ping sent on port " + port);
        }
        catch(IOException e)
        {
            Log.e("network sync failed", e);
        }
    }
}
