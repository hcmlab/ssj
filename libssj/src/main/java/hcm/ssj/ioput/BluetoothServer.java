/*
 * BluetoothServer.java
 * Copyright (c) 2016
 * Authors: Ionut Damian, Michael Dietz, Frank Gaibler, Daniel Langerenken
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
import java.util.UUID;

import hcm.ssj.core.Log;

/**
 * Created by Johnny on 07.04.2015.
 */
public class BluetoothServer implements BluetoothConnection
{
    private String _name = "SSJ_BluetoothServer";

    private BluetoothServerSocket _server = null;
    private BluetoothSocket _socket = null;

    private String _connectionName;
    private String _serverName;

    BluetoothAdapter _adapter = null;

    public BluetoothServer(String connectionName, String serverName)
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

        _connectionName = connectionName;
        _serverName = serverName;
    }

    public void connect() throws IOException
    {
        UUID uuid = UUID.nameUUIDFromBytes(_connectionName.getBytes());

        Log.i("attempting to set up connection " + _connectionName + " on " + _adapter.getName() + " @ " + _adapter.getAddress());
        _server = _adapter.listenUsingInsecureRfcommWithServiceRecord(_serverName, uuid);

        Log.i("connection " + _connectionName + " on " + _adapter.getName() + " @ " + _adapter.getAddress() + " ready");
        Log.i("waiting for clients...");

        _socket = _server.accept();
    }

    public void disconnect() throws IOException
    {
        if(_server != null)
        {
            _server.close();
            _server = null;
        }
        else
        {
            _adapter.cancelDiscovery();
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
