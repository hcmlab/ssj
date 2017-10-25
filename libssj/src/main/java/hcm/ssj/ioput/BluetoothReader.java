/*
 * BluetoothReader.java
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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.UUID;

import hcm.ssj.core.Log;
import hcm.ssj.core.SSJException;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.Sensor;
import hcm.ssj.core.Util;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;

/**
 * Created by Johnny on 05.03.2015.
 */
public class BluetoothReader extends Sensor {

    public class Options extends OptionList
    {
        public final Option<String> connectionName = new Option<>("connectionName", "SSJ", String.class, "must match that of the peer");
        public final Option<String> serverName = new Option<>("serverName", "SSJ_BLServer", String.class, "");
        public final Option<String> serverAddr = new Option<>("serverAddr", null, String.class, "if this is a client");
        public final Option<Integer> numStreams = new Option<>("numStreams", null, Integer.class, "number of streams to be received (null = use number of defined SensorChannels)");
        public final Option<BluetoothConnection.Type> connectionType = new Option<>("connectionType", BluetoothConnection.Type.SERVER, BluetoothConnection.Type.class, "");

        /**
         *
         */
        private Options() {
            addOptions();
        }
    }

    public final Options options = new Options();

    protected BluetoothConnection _conn;
    protected byte[][] _recvData;
    protected int numStreams;

    public BluetoothReader() {
        _name = "BluetoothReader";
    }

    @Override
    public void init() throws SSJException
    {
        try {
            switch(options.connectionType.get())
            {
                case SERVER:
                    _conn = new BluetoothServer(UUID.nameUUIDFromBytes(options.connectionName.get().getBytes()), options.serverName.get());
                    break;
                case CLIENT:
                    _conn = new BluetoothClient(UUID.nameUUIDFromBytes(options.connectionName.get().getBytes()), options.serverName.get(), options.serverAddr.get());
                    break;
            }

        } catch (IOException e) {
           throw new SSJException("error in setting up connection "+ options.connectionName, e);
        }
    }

    @Override
	public boolean connect() throws SSJFatalException
    {
		if (options.numStreams.get() == null || options.numStreams.get() == 0)
		{
			numStreams = _provider.size();
		}
		else
		{
			numStreams = options.numStreams.get();
		}

		if (numStreams < _provider.size())
		{
			throw new SSJFatalException("Invalid configuration. Expected incoming number of streams (" + numStreams + ") is smaller than number of defined channels (" + _provider.size() + ")");
		}
		else if (numStreams > _provider.size())
		{
			Log.w("Unusual configuration. Expected incoming number of streams (" + numStreams + ") is greater than number of defined channels (" + _provider.size() + ")");
		}

        Log.i("setting up sensor to receive " + numStreams + " streams");
        _recvData = new byte[numStreams][];
        for(int i=0; i< _provider.size(); ++i)
        {
            BluetoothChannel ch = (BluetoothChannel)_provider.get(i);
            _recvData[ch.options.channel_id.get()] = new byte[ch.getOutputStream().tot];
        }

        //use object input streams if we expect more than one input
        _conn.connect(numStreams > 1);

        BluetoothDevice dev = _conn.getConnectedDevice();
        Log.i("connected to " + dev.getName() + " @ " + dev.getAddress());
        return true;
    }

    public void update() throws SSJFatalException
    {
        if (!_conn.isConnected())
        {
            return;
        }

        try
        {
            if(numStreams == 1)
            {
                ((DataInputStream)_conn.input()).readFully(_recvData[0]);
            }
            else if(numStreams > 1)
            {
                Stream recvStreams[] = (Stream[]) ((ObjectInputStream)_conn.input()).readObject();

                if (recvStreams.length != _recvData.length)
                {
                    throw new IOException("unexpected amount of incoming streams");
                }

                for (int i = 0; i < recvStreams.length && i < _recvData.length; ++i)
                {
                    Util.arraycopy(recvStreams[i].ptr(), 0, _recvData[i], 0, _recvData[i].length);
                }
            }
            _conn.notifyDataTranferResult(true);
        }
        catch (IOException | ClassNotFoundException e)
        {
            Log.w("unable to read from data stream", e);
            _conn.notifyDataTranferResult(false);
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

    public BluetoothConnection getConnection()
    {
        return _conn;
    }

    public byte[] getData(int channel_id)
    {
        if(channel_id >= _recvData.length)
            return null;

        return _recvData[channel_id];
    }

    @Override
    public void clear()
    {
        _conn.clear();
        _conn = null;
        super.clear();
    }
}
