/*
 * BluetoothReader.java
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

import java.io.DataInputStream;
import java.io.IOException;
import java.util.UUID;

import hcm.ssj.core.Log;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;

import static hcm.ssj.core.Cons.DEFAULT_BL_SERIAL_UUID;

/**
 * Created by Johnny on 05.03.2015.
 */
public class BluetoothPressureSensor extends BluetoothReader {

    public class Options extends OptionList
    {
        public final Option<String> connectionName = new Option<>("connectionName", "SSJ", String.class, "must match that of the peer");
        public final Option<String> serverName = new Option<>("serverName", "SSJ_BLServer", String.class, "");
        public final Option<String> serverAddr = new Option<>("serverAddr", null, String.class, "if this is a client");

        /**
         *
         */
        private Options() {
            addOptions();
        }
    }

    public final Options options = new Options();
    protected boolean _isreallyConnected = false;

    public BluetoothPressureSensor() {
        _name = "BLPressure";
    }

    @Override
    public void init()
    {
        try {
            _conn = new BluetoothClient(UUID.fromString(DEFAULT_BL_SERIAL_UUID), options.serverName.get(), options.serverAddr.get());

        } catch (IOException e) {
            Log.e("error in setting up connection "+ options.connectionName, e);
        }
    }

    @Override
    public boolean connect()
    {
        if(!super.connect())
            return false;

        try
        {
            byte[] data={10,0};
            _conn.output().write(data);
        }
        catch (IOException  e)
        {
            Log.w("unable to rwrite init sequence to bt socket", e);
        }

        _isreallyConnected = true;
        return true;
    }

    public void update()
    {
        if(!_conn.isConnected() && _isreallyConnected)
            return;

        try
        {
            ((DataInputStream)_conn.input()).readFully(_recvData[0]);
        }
        catch (IOException  e)
        {
            Log.w("unable to read from data stream", e);
        }
    }

    @Override
    public void disconnect()
    {
        try {
            _conn.disconnect();
        } catch (IOException e) {
            Log.e("failed closing connection", e);
        }
    }

    @Override
    public void forcekill()
    {
        try {
            _conn.disconnect();

        } catch (Exception e) {
            Log.e("error force killing thread", e);
        }

        super.forcekill();
    }
}
