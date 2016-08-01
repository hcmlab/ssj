/*
 * BluetoothConnection.java
 * Copyright (c) 2016
 * Authors: Ionut Damian, Michael Dietz, Frank Gaibler, Daniel Langerenken, Simon Flutura
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

package hcm.ssj.ioput;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import hcm.ssj.core.Log;
import hcm.ssj.core.SSJApplication;

/**
 * Created by Johnny on 07.04.2015.
 */
public abstract class BluetoothConnection extends BroadcastReceiver
{
    public enum Type
    {
        CLIENT,
        SERVER
    }

    protected BluetoothDevice _connectedDevice = null;

    Thread _thread;
    protected boolean _terminate = false;
    protected boolean _isConnected = false;

    protected final Object _newConnection = new Object();
    protected final Object _newDisconnection = new Object();

    protected ObjectInputStream _in;
    protected ObjectOutputStream _out;

    public ObjectInputStream input() {return _in;}
    public ObjectOutputStream output() {return _out;}

    public BluetoothConnection()
    {
        //register listener for BL status changes
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        SSJApplication.getAppContext().registerReceiver(this, filter);
    }

    abstract void connect() throws IOException;
    abstract void disconnect() throws IOException;
    abstract BluetoothSocket getSocket();

    public BluetoothDevice getConnectedDevice()
    {
        return _connectedDevice;
    }

    public void onReceive(Context ctx, Intent intent) {

        final BluetoothDevice device = intent.getParcelableExtra( BluetoothDevice.EXTRA_DEVICE );
        String action = intent.getAction();

        if (BluetoothDevice.ACTION_ACL_CONNECTED.equalsIgnoreCase( action ) )   {
            if (!device.equals(_connectedDevice))
            {
                Log.v("received ACTION_ACL_CONNECTED with " + device.getName() );
                _connectedDevice = device;
            }
        }

        if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equalsIgnoreCase(action ) )    {
            if (device.equals(_connectedDevice))
            {
                Log.w("disconnected from " + device.getName() );
                _connectedDevice = null;
                setConnectionStatus(false);
            }
        }
    }

    public void waitForConnection()
    {
        while(!isConnected())
        {
            try
            {
                synchronized (_newConnection)
                {
                    _newConnection.wait();
                }
            }
            catch (InterruptedException e) {}
        }
    }

    public void waitForDisconnection()
    {
        while(isConnected())
        {
            try
            {
                synchronized (_newDisconnection)
                {
                    _newDisconnection.wait();
                }
            }
            catch (InterruptedException e) {}
        }
    }

    public boolean isConnected()
    {
        return _connectedDevice != null && _isConnected;
    }

    protected void setConnectionStatus(boolean connected)
    {
        if(connected)
        {
            _isConnected = true;
            synchronized (_newConnection)
            {
                _newConnection.notifyAll();
            }
        }
        else
        {
            _isConnected = false;
            synchronized (_newDisconnection)
            {
                _newDisconnection.notifyAll();
            }
        }
    }
}
