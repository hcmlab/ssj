/*
 * BluetoothClient.java
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
import android.bluetooth.BluetoothSocket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Set;
import java.util.UUID;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;

/**
 * Created by Johnny on 07.04.2015.
 */
public class BluetoothClient extends BluetoothConnection implements Runnable
{
    private String _name = "SSJ_BluetoothClient";

    private BluetoothSocket _socket = null;
    private UUID _uuid;
    boolean _useObjectStreams = false;

    BluetoothAdapter _adapter = null;
    BluetoothDevice _server = null;

    public BluetoothClient(UUID connID, String serverName, String serverAddr) throws IOException
    {
        _adapter = BluetoothAdapter.getDefaultAdapter();
        if (_adapter == null)
        {
            Log.e("Device does not support Bluetooth");
            return;
        }

        if (!_adapter.isEnabled())
        {
            Log.e("Bluetooth not enabled");
            return;
        }

        Log.i("searching for server " + serverName);

        Set<BluetoothDevice> pairedDevices = _adapter.getBondedDevices();
        // If there are paired devices
        if (pairedDevices.size() > 0)
        {
            // Loop through paired devices
            for (BluetoothDevice d : pairedDevices)
            {
                if (d.getName().equalsIgnoreCase(serverName))
                {
                    serverAddr = d.getAddress();
                    _server = d;
                }
            }
        }
        //if we haven't found it
        if(_server == null)
        {
            Log.i("not found, searching for server " + serverAddr);

            if (!BluetoothAdapter.checkBluetoothAddress(serverAddr)) {
                Log.e("invalid MAC address: " + serverAddr);
                return;
            }

            _server = _adapter.getRemoteDevice(serverAddr);
        }

        _uuid = connID;
        Log.i("client connection to " + _server.getName() + " initialized");
    }

    public void connect(boolean useObjectStreams)
    {
        _useObjectStreams = useObjectStreams;

        _isConnected = false;
        _terminate = false;
        _thread = new Thread(this);
        _thread.start();

        waitForConnection();
    }

    public void run()
    {
        _adapter.cancelDiscovery();

        while(!_terminate)
        {
            try
            {
                Log.i("setting up connection to " + _server.getName() + ", conn = " + _uuid.toString());
                _socket = _server.createRfcommSocketToServiceRecord(_uuid);

                Log.i("waiting for server ...");
                _socket.connect();

                if(_useObjectStreams)
                {
                    _out = new ObjectOutputStream(_socket.getOutputStream());
                    _in = new ObjectInputStream(_socket.getInputStream());
                }
                else
                {
                    _out = new DataOutputStream(_socket.getOutputStream());
                    _in = new DataInputStream(_socket.getInputStream());
                }
            }
            catch (IOException e)
            {
                Log.w("failed to connect to server", e);
            }

            try {
                Thread.sleep(Cons.WAIT_BL_CONNECT); //give BL adapter some time to establish connection ...
            } catch (InterruptedException e) {}

            if(_socket != null && _socket.isConnected())
            {
                Log.i("connected to server " + _server.getName() + ", conn = " + _uuid.toString());
                setConnectedDevice(_socket.getRemoteDevice());
                setConnectionStatus(true);

                //wait as long as there is an active connection
                waitForDisconnection();
            }

            close();
        }
    }

    private void close()
    {
        try
        {
            if(_socket != null)
            {
                _socket.close();
                _socket = null;
            }
        }
        catch (IOException e)
        {
            Log.e("failed to close socket", e);
        }

    }

    public void disconnect() throws IOException
    {
        _terminate = true;
        _isConnected = false;

        synchronized (_newConnection) {
            _newConnection.notifyAll();
        }
        synchronized (_newDisconnection) {
            _newDisconnection.notifyAll();
        }

        close();
    }

    public BluetoothDevice getRemoteDevice()
    {
        if(_socket != null)
            return _socket.getRemoteDevice();

        return null;
    }
}
