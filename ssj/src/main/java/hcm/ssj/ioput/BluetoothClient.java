/*
 * BluetoothClient.java
 * Copyright (c) 2015
 * Authors: Ionut Damian, Michael Dietz, Frank Gaibler
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
 * with this library; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package hcm.ssj.ioput;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

/**
 * Created by Johnny on 07.04.2015.
 */
public class BluetoothClient implements BluetoothConnection
{
    private String _name = "SSJ_BluetoothClient";

    private BluetoothSocket _socket = null;

    private String _connectionName;
    private String _serverName;
    private String _serverAddr;

    BluetoothAdapter _adapter = null;

    public BluetoothClient(String connectionName, String serverName, String serverAddr) throws IOException
    {
        _adapter = BluetoothAdapter.getDefaultAdapter();
        if (_adapter == null)
        {
            Log.e(_name, "Device does not support Bluetooth");
            return;
        }

        if (!_adapter.isEnabled())
        {
            Log.e(_name, "Bluetooth not enabled");
            return;
        }

        _connectionName = connectionName;
        _serverAddr = serverAddr;
        _serverName = serverName;
    }

    public void connect() throws IOException
    {
        Log.i(_name, "searching for server " + _serverAddr);

        BluetoothDevice device = null;
        Set<BluetoothDevice> pairedDevices = _adapter.getBondedDevices();
        // If there are paired devices
        if (pairedDevices.size() > 0)
        {
            // Loop through paired devices
            for (BluetoothDevice d : pairedDevices)
            {
                if (d.getName().equalsIgnoreCase(_serverName))
                {
                    _serverAddr = d.getAddress();
                    device = d;
                }
            }
        }
        //if we haven't found it
        if(device == null)
        {
            if (!BluetoothAdapter.checkBluetoothAddress(_serverAddr)) {
                Log.e(_name, "invalid MAC address: " + _serverAddr);
                return;
            }

            device = _adapter.getRemoteDevice(_serverAddr);
        }

        UUID uuid = UUID.nameUUIDFromBytes(_connectionName.getBytes());

        Log.i(_name, "connecting to " + _serverAddr + " using the connection " + _connectionName);

        _socket = device.createRfcommSocketToServiceRecord(uuid);
        _socket.connect();
    }

    public void disconnect() throws IOException
    {
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
