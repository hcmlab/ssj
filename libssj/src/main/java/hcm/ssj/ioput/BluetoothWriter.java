/*
 * BluetoothWriter.java
 * Copyright (c) 2017
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

package hcm.ssj.ioput;

import android.bluetooth.BluetoothDevice;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.UUID;

import hcm.ssj.core.Consumer;
import hcm.ssj.core.Log;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.Util;
import hcm.ssj.core.event.Event;
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
        _name = "BluetoothWriter";
    }

    @Override
	public void enter(Stream[] stream_in) throws SSJFatalException
	{
        try {
            switch(options.connectionType.get())
            {
                case SERVER:
                    _conn = new BluetoothServer(UUID.nameUUIDFromBytes(options.connectionName.get().getBytes()), options.serverName.get());
                    _conn.connect(stream_in.length > 1); //use object output streams if we are sending more than one stream
                    break;
                case CLIENT:
                    _conn = new BluetoothClient(UUID.nameUUIDFromBytes(options.connectionName.get().getBytes()), options.serverName.get(), options.serverAddr.get());
                    _conn.connect(stream_in.length > 1); //use object output streams if we are sending more than one stream
                    break;
            }
        } catch (Exception e)
        {
            throw new SSJFatalException("error in setting up connection "+ options.connectionName, e);
        }

        BluetoothDevice dev = _conn.getRemoteDevice();
        if(dev == null) {
            Log.e("cannot retrieve remote device");
            return;
        }

		if (stream_in.length == 1)
		{
			_data = new byte[stream_in[0].tot];
		}

        Log.i("connected to " + dev.getName() + " @ " + dev.getAddress());
        _connected = true;
    }

    protected void consume(Stream[] stream_in, Event trigger) throws SSJFatalException
    {
        if (!_connected || !_conn.isConnected())
        {
            return;
        }

        try {
            if(stream_in.length == 1)
            {
                Util.arraycopy(stream_in[0].ptr(), 0, _data, 0, _data.length);
                _conn.output().write(_data);
            }
            else if(stream_in.length > 1)
            {
                ((ObjectOutputStream)_conn.output()).reset();
                ((ObjectOutputStream)_conn.output()).writeObject(stream_in);
            }
            _conn.notifyDataTranferResult(true);

        } catch (IOException e) {
            Log.w("failed sending data", e);
            _conn.notifyDataTranferResult(false);
        }
    }

    public void flush(Stream[] stream_in) throws SSJFatalException
    {
        _connected = false;

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
        } catch (IOException e) {
            Log.e("failed closing connection", e);
        }

        super.forcekill();
    }

    @Override
    public void clear()
    {
        _conn.clear();
        _conn = null;
        super.clear();
    }

	@Override
	public OptionList getOptions()
	{
		return options;
	}
}
