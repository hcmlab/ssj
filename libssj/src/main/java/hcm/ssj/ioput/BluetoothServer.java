/*
 * BluetoothServer.java
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
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.UUID;

import hcm.ssj.core.Log;

/**
 * Created by Johnny on 07.04.2015.
 */
public class BluetoothServer extends BluetoothConnection implements Runnable
{
    private String _name = "SSJ_BluetoothServer";

    private BluetoothServerSocket _server = null;
    private BluetoothSocket _socket = null;

    private UUID _uuid;
    private String _serverName;

    BluetoothAdapter _adapter = null;

    public BluetoothServer(String connectionName, String serverName) throws IOException
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

        _serverName = serverName;
        _uuid = UUID.nameUUIDFromBytes(connectionName.getBytes());
        Log.i("server connection " + connectionName + " on " + _adapter.getName() + " @ " + _adapter.getAddress() + " initialized");
    }

    public void connect()
    {
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
                Log.i("setting up server on " + _adapter.getName() + " @ " + _adapter.getAddress());
                _server = _adapter.listenUsingInsecureRfcommWithServiceRecord(_serverName, _uuid);

                Log.i("waiting for clients...");
                _socket = _server.accept();

                _out = new ObjectOutputStream(_socket.getOutputStream());
                _in = new ObjectInputStream(_socket.getInputStream());
            }
            catch (IOException e)
            {
                Log.w("failed to connect to client", e);
            }

            if(_socket.isConnected())
            {
                Log.i("connected to client " + _socket.getRemoteDevice().getName());
                setConnectionStatus(true);

                //wait as long as there is an active connection
                waitForDisconnection();
            }

            try
            {
                _server.close();
                _socket.close();
            }
            catch (IOException e)
            {
                Log.e("failed to close sockets", e);
            }
        }
    }

    public void disconnect() throws IOException
    {
        _terminate = true;

        _newConnection.notifyAll();
        _newDisconnection.notifyAll();

        if(_server != null)
        {
            _server.close();
            _server = null;
        }

        if(_socket != null)
        {
            _socket.close();
            _socket = null;
        }
    }

    public BluetoothSocket getSocket()
    {
        return _socket;
    }
}
