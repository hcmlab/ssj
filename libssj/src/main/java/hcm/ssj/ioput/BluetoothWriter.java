/*
 * BluetoothWriter.java
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

import java.io.IOException;

import hcm.ssj.core.Consumer;
import hcm.ssj.core.Log;
import hcm.ssj.core.Util;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;

/**
 * Created by Johnny on 05.03.2015.
 */
public class BluetoothWriter extends Consumer {

    public class Options extends OptionList
    {
        public final Option<String> serverName = new Option<>("serverName", "SSJ_BLServer", String.class, "");
        public final Option<String> serverAddr = new Option<>("serverAddr", null, String.class, "if this is a client");
        public final Option<String> connectionName = new Option<>("connectionName", "SSJ", String.class, "must match that of the peer");
        public final Option<BluetoothConnection.Type> connectionType = new Option<>("connectionType", BluetoothConnection.Type.CLIENT, BluetoothConnection.Type.class, "");

        /**
         *
         */
        private Options() {
            addOptions();
        }
    }

    public final Options options = new Options();

    private BluetoothConnection _conn;
    private byte[] _data;

    private boolean _connected = false;

    public BluetoothWriter() {
        _name = "SSJ_consumer_BluetoothWriter";
    }

    @Override
    public void enter(Stream[] stream_in) {
        try {
            switch(options.connectionType.getValue())
            {
                case SERVER:
                    _conn = new BluetoothServer(options.connectionName.getValue(), options.serverName.getValue());
                    _conn.connect();
                    break;
                case CLIENT:
                    _conn = new BluetoothClient(options.connectionName.getValue(), options.serverName.getValue(), options.serverAddr.getValue());
                    _conn.connect();
                    break;
            }
        } catch (Exception e)
        {
            Log.e("error in setting up connection "+ options.connectionName, e);
            return;
        }

        BluetoothDevice dev = _conn.getSocket().getRemoteDevice();

        _data = new byte[stream_in[0].tot];

        Log.i("connected to " + dev.getName() + " @ " + dev.getAddress());
        _connected = true;
    }

    protected void consume(Stream[] stream_in) {
        if (!_connected || !_conn.isConnected())
            return;

        try {
            Util.arraycopy(stream_in[0].ptr(), 0, _data, 0, _data.length);
            _conn.output().write(_data);

        } catch (IOException e) {
            Log.w("failed sending data", e);
        }
    }

    public void flush(Stream[] stream_in) {
        _connected = false;

        try {
            _conn.disconnect();
        } catch (IOException e) {
            Log.e("failed closing connection", e);
        }
    }

    @Override
    public void forcekill() {

        try {
            _conn.disconnect();
        } catch (IOException e) {
            Log.e("failed closing connection", e);
        }

        super.forcekill();
    }
}
