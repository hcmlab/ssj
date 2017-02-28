/*
 * BluetoothEventWriter.java
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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

import hcm.ssj.core.Cons;
import hcm.ssj.core.EventHandler;
import hcm.ssj.core.Log;
import hcm.ssj.core.Util;
import hcm.ssj.core.event.Event;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.file.LoggingConstants;

/**
 * Created by Johnny on 05.03.2015.
 */
public class BluetoothEventWriter extends EventHandler
{
    public class Options extends OptionList
    {
        public final Option<String> serverName = new Option<>("serverName", "SSJ_BLServer", String.class, "");
        public final Option<String> serverAddr = new Option<>("serverAddr", null, String.class, "we need an address if this is the first time these two devices connect");
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

    private boolean _connected = false;
    byte[] _buffer;
    int _evID[];
    StringBuilder _builder = new StringBuilder();

    public BluetoothEventWriter() {
        _name = "BluetoothEventWriter";
        _doWakeLock = true;
    }

    @Override
    public void enter() {

        if(_evchannel_in == null || _evchannel_in.size() == 0)
            throw new RuntimeException("no incoming event channels defined");

        try {
            switch(options.connectionType.get())
            {
                case SERVER:
                    _conn = new BluetoothServer(UUID.nameUUIDFromBytes(options.connectionName.get().getBytes()), options.serverName.get());
                    _conn.connect(false);
                    break;
                case CLIENT:
                    _conn = new BluetoothClient(UUID.nameUUIDFromBytes(options.connectionName.get().getBytes()), options.serverName.get(), options.serverAddr.get());
                    _conn.connect(false);
                    break;
            }
        } catch (Exception e)
        {
            Log.e("error in setting up connection", e);
            return;
        }

        BluetoothDevice dev = _conn.getRemoteDevice();
        if(dev == null) {
            Log.e("cannot retrieve remote device");
            return;
        }

        Log.i("connected to " + dev.getName() + " @ " + dev.getAddress());

        _buffer = new byte[Cons.MAX_EVENT_SIZE];
        _evID = new int[_evchannel_in.size()];

        _connected = true;
    }

    @Override
    protected void process()
    {
        if (!_connected || !_conn.isConnected())
            return;

        _builder.delete(0, _builder.length());

        _builder.append("<events ssi-v=\"2\" ssj-v=\"");
        _builder.append(_frame.getVersion());
        _builder.append("\">");

        int count = 0;
        for(int i = 0; i < _evchannel_in.size(); ++i)
        {
            Event ev = _evchannel_in.get(i).getEvent(_evID[i], false);
            if (ev == null)
                continue;

            _evID[i] = ev.id + 1;
            count++;

            //build event
            Util.eventToXML(_builder, ev);
            _builder.append(LoggingConstants.DELIMITER_LINE);
        }

        if(count > 0)
        {
            _builder.append( "</events>");

            ByteBuffer buf = ByteBuffer.wrap(_buffer);
            buf.order(ByteOrder.BIG_ENDIAN);

            //store event
            buf.put(_builder.toString().getBytes());

            try
            {
                _conn.output().write(_buffer, 0, buf.position());
                _conn.output().flush();

            }
            catch (IOException e)
            {
                Log.w("failed sending data", e);
            }
        }
    }

    @Override
    public void flush() {
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

        } catch (Exception e) {
            Log.e("error force killing thread", e);
        }

        super.forcekill();
    }
}
