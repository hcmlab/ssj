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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.Pipeline;
import hcm.ssj.core.SSJApplication;

/**
 * Created by Johnny on 07.04.2015.
 */
public abstract class BluetoothConnection extends BroadcastReceiver implements Runnable
{
    public enum Type
    {
        CLIENT,
        SERVER
    }

    protected String _name = "BluetoothConnection";
    Pipeline pipe;

    private long _firstError = 0;

    protected BluetoothDevice _connectedDevice = null;

    protected boolean _terminate = false;
    protected boolean _isConnected = false;

    protected final Object _newConnection = new Object();
    protected final Object _newDisconnection = new Object();

    protected InputStream _in;
    protected OutputStream _out;

    protected boolean _useObjectStreams = false;

    public InputStream input() {return _in;}
    public OutputStream output() {return _out;}

    public BluetoothConnection()
    {
        pipe = Pipeline.getInstance();

        //register listener for BL status changes
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        SSJApplication.getAppContext().registerReceiver(this, filter);
    }

    public void clear()
    {
        SSJApplication.getAppContext().unregisterReceiver(this);
    }

    public void connect(boolean useObjectStreams)
    {
        Log.i(_name + " connecting");
        _useObjectStreams = useObjectStreams;

        _isConnected = false;
        _terminate = false;

        pipe.executeRunnable(this);

        waitForConnection();
        Log.i(_name + " connected");
    }

    public void disconnect() throws IOException
    {
        Log.i(_name + " disconnecting");
        _terminate = true;
        _isConnected = false;

        synchronized (_newConnection) {
            _newConnection.notifyAll();
        }
        synchronized (_newDisconnection) {
            _newDisconnection.notifyAll();
        }

        close();
        Log.i(_name + " disconnected");
    }

    abstract BluetoothDevice getRemoteDevice();

    public void onReceive(Context ctx, Intent intent) {

        String action = intent.getAction();
        Log.v("new bluetooth state: " + action);

        if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
            BluetoothDevice device = intent.getParcelableExtra( BluetoothDevice.EXTRA_DEVICE );
            if (!device.equals(_connectedDevice))
            {
                Log.v("connected with " + device.getName() );
                setConnectionStatus(true);
            }
        }

        else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action))    {
            BluetoothDevice device = intent.getParcelableExtra( BluetoothDevice.EXTRA_DEVICE );
            if (device.equals(_connectedDevice))
            {
                Log.w("disconnected from " + device.getName() );
                setConnectionStatus(false);
            }
        }

        else if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                                                 BluetoothAdapter.ERROR);
            switch (state) {
                case BluetoothAdapter.STATE_OFF:
                case BluetoothAdapter.STATE_TURNING_OFF:
                    setConnectionStatus(false);
                    break;
            }
        }

        else if (action.equals(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)) {
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE,
                                                 BluetoothAdapter.ERROR);
            BluetoothDevice device = intent.getParcelableExtra( BluetoothDevice.EXTRA_DEVICE );
            if (device.equals(_connectedDevice)) {
                switch (state) {
                    case BluetoothAdapter.STATE_DISCONNECTED:
                    case BluetoothAdapter.STATE_DISCONNECTING:
                        setConnectionStatus(false);
                        break;
                    case BluetoothAdapter.STATE_CONNECTED:
                        setConnectionStatus(true);
                        break;
                }
            }
        }
    }

    public void waitForConnection()
    {
        while(!isConnected() && !_terminate)
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
        while(isConnected() && !_terminate)
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
        boolean value;
        synchronized (this)
        {
            value = _connectedDevice != null && _isConnected;
        }
        return value;
    }

    public BluetoothDevice getConnectedDevice()
    {
        return _connectedDevice;
    }

    protected void setConnectedDevice(BluetoothDevice device)
    {
        synchronized (this) {
            _connectedDevice = device;
        }
    }

    protected void setConnectionStatus(boolean connected)
    {
        if(connected)
        {
            synchronized (this) {
                _isConnected = true;
            }

            synchronized (_newConnection) {
                _newConnection.notifyAll();
            }
        }
        else
        {
            synchronized (this) {
                _isConnected = false;
            }

            synchronized (_newDisconnection){
                _newDisconnection.notifyAll();
            }
        }
    }

    protected void notifyDataTranferResult(boolean value)
    {
        if(!value)
        {
            if(_firstError == 0)
                _firstError = System.currentTimeMillis();
            else if (System.currentTimeMillis() - _firstError > Cons.WAIT_BL_DISCONNECT)
            {
                Log.w("connection interrupted");
                setConnectionStatus(false);
            }
        }
        else
            _firstError = 0;
    }

    abstract protected void close();
}
