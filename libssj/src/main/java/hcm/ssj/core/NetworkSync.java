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

class NetworkSync {

    private final int NUM_REPETITIONS = 10;

    private class SyncListener implements Runnable
    {
        boolean terminate = false;
        int port = 0;

        long delta, rtt;
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

        public void reset()
        {
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
            long[] tmp = new long[2];
            byte[] data = new byte[29];

            while(!terminate)
            {
                try
                {
                    DatagramPacket packet = new DatagramPacket(data, data.length);
                    recvSocket.receive(packet);

                    //check data
                    byte[] msg = packet.getData();
                    String str = new String(msg, "ASCII");

                    Log.d("received packet from " + packet.getAddress().toString() + ": " + str);

                    if (type == Pipeline.SyncType.CONTINUOUS && str.startsWith("SSI:SYNC:TIME")) //SSI format for compatibility
                    {
                        Log.d("packet identified as timestamp from master");

                        Util.arraycopy(msg, 13, tmp, 0, 8);
                        Util.arraycopy(msg, 21, tmp, 8, 8);

                        long time_send = tmp[0];
                        long time_master = tmp[1];
                        long time_recv = frame.getTimeMs();

                        Log.d("t_master: " + time_master + "\nt_send: " + time_send + "\nt_recv: " + time_recv);

                        if(time_recv - time_send < rtt)
                        {
                            rtt = time_recv - time_send;
                            delta = time_master - time_recv + rtt/2;
                            Log.d("delta: " + delta);
                        }
                    }
                    else if(type == Pipeline.SyncType.CONTINUOUS && isMaster && str.startsWith("SSI:SYNC:RQST"))
                    {
                        Log.d("packet identified as time request from slave");

                        tmp[0] = frame.getTimeMs();

                        //append local (master) time to message, final form: TAG(13) + slave_time(8) + master_time(8)
                        System.arraycopy("SSI:SYNC:TIME".getBytes("ASCII"), 0, data, 0, 13);
                        Util.arraycopy(tmp, 0, data, 21, 8);

                        Log.d("sending time to slave ("+packet.getAddress().toString()+"): " + tmp[0]);
                        send(data, packet.getAddress());
                    }
                    else if (!isMaster && str.startsWith("SSI:STRT"))
                    {
                        if(!str.startsWith("SSI:STRT:RUN1"))
                            Log.w("Only RUN & QUIT is currently supported.");

                        Log.d("packet identified as start ping");

                        synchronized (waitForStartMonitor)
                        {
                            waitForStart = false;
                            waitForStartMonitor.notifyAll();
                        }
                    }
                    else if (!isMaster && str.startsWith("SSI:STOP"))
                    {
                        if(str.startsWith("SSI:STOP:RUNN"))
                            Log.w("Restart is not currently supported, stopping pipeline.");

                        Log.d("packet identified as stop ping");

                        synchronized (waitForStopMonitor)
                        {
                            waitForStop = false;
                            waitForStopMonitor.notifyAll();
                        }
                    }
                }
                catch (IOException e) {
                    Log.e("error in network sync", e);
                }
            }
        }
    }

    private class SyncSender implements Runnable
    {
        boolean terminate = false;
        private Timer timer;

        SyncSender(int interval)
        {
            timer = new Timer(interval);
        }

        public void terminate()
        {
            terminate = true;
        }

        @Override
        public void run() {

            byte[] data = new byte[21];
            long[] tmp = new long[1];

            while(!terminate)
            {
                if(frame.isRunning())
                {
                    frame.adjustTime(listener.getDelta());
                    listener.reset();

                    //request new time
                    for(int i = 0; i< NUM_REPETITIONS; i++)
                    {
                        try
                        {
                            //send current time with message
                            tmp[0] = frame.getTimeMs();
                            System.arraycopy("SSI:SYNC:RQST".getBytes("ASCII"), 0, data, 0, 13);
                            Util.arraycopy(tmp, 0, data, 13, 8);

                            send(data, hostAddr);
                        }
                        catch (IOException e)
                        {
                            Log.e("error sending sync message", e);
                        }
                    }
                }

                timer.sync();
            }
        }
    }

    private Pipeline frame;
    private DatagramSocket sendSocket;
    private DatagramSocket recvSocket;
    private boolean isMaster = true;
    private InetAddress hostAddr;
    private int port;

    private SyncListener listener;
    private SyncSender sender;

    private Pipeline.SyncType type;

    private boolean waitForStart = false;
    private boolean waitForStop = false;
    final private Object waitForStartMonitor = new Object();
    final private Object waitForStopMonitor = new Object();

    public NetworkSync(Pipeline.SyncType type, boolean isMaster, InetAddress masterAddr, int port, int interval)
    {
        frame = Pipeline.getInstance();

        this.isMaster = isMaster;
        this.hostAddr = masterAddr;
        this.port = port;
        this.type = type;

        try {
            sendSocket = new DatagramSocket();
            recvSocket = new DatagramSocket(port);
            sendSocket.setReuseAddress(true);
            recvSocket.setReuseAddress(true);
        } catch (SocketException e) {
            Log.e("error setting up network sync", e);
        }

        listener = new SyncListener(port);
        new Thread(listener).start();

        if(type == Pipeline.SyncType.CONTINUOUS && interval > 0 && !isMaster)
        {
            sender = new SyncSender(interval);
            new Thread(sender).start();
        }
    }

    private void send(byte[] data, InetAddress addr) throws IOException
    {
        DatagramPacket packet = new DatagramPacket(data, data.length, addr, port);
        sendSocket.send(packet);
    }

    public void waitForStartSignal()
    {
        synchronized (waitForStartMonitor)
        {
            waitForStart = true;
            while (waitForStart && frame.getState() != Pipeline.State.STOPPING && frame.getState() != Pipeline.State.INACTIVE)
            {
                try
                {
                    waitForStartMonitor.wait();
                }
                catch (InterruptedException e)
                {
                }
            }
        }
    }

    public void waitForStopSignal()
    {
        synchronized (waitForStopMonitor)
        {
            waitForStop = true;
            while (waitForStop && frame.getState() != Pipeline.State.STOPPING && frame.getState() != Pipeline.State.INACTIVE)
            {
                try
                {
                    waitForStopMonitor.wait();
                }
                catch (InterruptedException e)
                {}
            }
        }
    }

    public void release()
    {
        if(sender != null)
            sender.terminate();

        listener.terminate();

        synchronized (waitForStartMonitor)
        {
            waitForStart = false;
            waitForStartMonitor.notifyAll();
        }

        synchronized (waitForStopMonitor)
        {
            waitForStop = false;
            waitForStopMonitor.notifyAll();
        }
    }

    static void sendStartSignal(int port)
    {
        try
        {
            DatagramSocket syncSocket = new DatagramSocket(null);
            syncSocket.setReuseAddress(true);
            syncSocket.setBroadcast(true);

            String msg = "SSI:STRT:RUN1\0"; //send in SSI format for compatibility
            byte[] data = msg.getBytes("ASCII");
            DatagramPacket packet = new DatagramPacket(data, data.length, Util.getBroadcastAddress(), port);
            syncSocket.send(packet);

            Log.i("start signal sent on port " + port);
        }
        catch(IOException e)
        {
            Log.e("network sync failed", e);
        }
    }

    static void sendStopSignal(int port)
    {
        try
        {
            DatagramSocket syncSocket = new DatagramSocket(null);
            syncSocket.setReuseAddress(true);
            syncSocket.setBroadcast(true);

            String msg = "SSI:STOP:QUIT\0"; //send in SSI format for compatibility
            byte[] data = msg.getBytes("ASCII");
            DatagramPacket packet = new DatagramPacket(data, data.length, Util.getBroadcastAddress(), port);
            syncSocket.send(packet);

            Log.i("stop signal sent on port " + port);
        }
        catch(IOException e)
        {
            Log.e("network sync failed", e);
        }
    }
}
