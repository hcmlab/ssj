/*
 * BluetoothReader.java
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

import android.bluetooth.BluetoothDevice;
import android.util.Log;

import java.io.IOException;

import hcm.ssj.core.Sensor;

/**
 * Created by Johnny on 05.03.2015.
 */
public class BluetoothReader extends Sensor {

    public class Options {
        public String connectionName = "SSJ"; //must match that of the peer
        public String serverName = "SSJ_BLServer";
        public String serverAddr; //if this is a client
        public BluetoothConnection.Type connectionType = BluetoothConnection.Type.SERVER;
    }

    public Options options = new Options();

    protected BluetoothConnection conn;

    public BluetoothReader() {
        _name = "SSJ_consumer_BluetoothReader";
    }

    @Override
    public void connect() {
        try {
            switch(options.connectionType)
            {
                case SERVER:
                    conn = new BluetoothServer(options.connectionName, options.serverName);
                    conn.connect();
                    break;
                case CLIENT:
                    conn = new BluetoothClient(options.connectionName, options.serverName, options.serverAddr);
                    conn.connect();
                    break;
            }

        } catch (IOException e) {
            Log.e(super._name, "error in setting up connection "+ options.connectionName, e);
            throw new RuntimeException(e);
        }

        BluetoothDevice dev = conn.getSocket().getRemoteDevice();
        Log.i(_name, "connected to " + dev.getName() + " @ " + dev.getAddress());
    }

    @Override
    public void disconnect()
    {
        try {
            conn.disconnect();
        } catch (IOException e) {
            Log.e(_name, "failed closing connection", e);
        }
    }

    @Override
    public void forcekill()
    {
        try {
            conn.disconnect();

        } catch (Exception e) {
            Log.e(_name, "error force killing thread", e);
        }

        super.forcekill();
    }
}
