/*
 * BluetoothEventWriter.java
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

import android.bluetooth.BluetoothDevice;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Event;
import hcm.ssj.core.EventHandler;
import hcm.ssj.core.Log;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;

/**
 * Created by Johnny on 05.03.2015.
 */
public class BluetoothEventWriter extends EventHandler
{
    public class Options extends OptionList
    {
        public final Option<String> serverName = new Option<>("serverName", "SSJ_BLServer", Cons.Type.STRING, "");
        public final Option<String> serverAddr = new Option<>("serverAddr", null, Cons.Type.STRING, "we need an address if this is the first time these two devices connect");
        public final Option<String> connectionName = new Option<>("connectionName", "SSJ", Cons.Type.STRING, "must match that of the peer");
        public final Option<BluetoothConnection.Type> connectionType = new Option<>("connectionType", BluetoothConnection.Type.CLIENT, Cons.Type.CUSTOM, "");

        /**
         *
         */
        private Options() {
            addOptions();
        }
    }

    public final Options options = new Options();

    private BluetoothConnection _conn;
    private DataOutputStream _out;

    private boolean _connected = false;
    byte[] _buffer;
    int _evID[];

    public BluetoothEventWriter() {
        _name = "SSJ_consumer_BluetoothEventWriter";
    }

    @Override
    public void enter() {

        if(_evchannel_in == null || _evchannel_in.size() == 0)
            throw new RuntimeException("no incoming event channels defined");

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

            _out = new DataOutputStream(_conn.getSocket().getOutputStream());
        } catch (Exception e)
        {
            Log.e("error in setting up connection", e);
            return;
        }

        BluetoothDevice dev = _conn.getSocket().getRemoteDevice();
        Log.i("connected to " + dev.getName() + " @ " + dev.getAddress());

        _buffer = new byte[Cons.MAX_EVENT_SIZE];
        _evID = new int[_evchannel_in.size()];

        _connected = true;
    }

    @Override
    protected void process()
    {
        if (!_connected)
            return;

        String msg = null;

        for(int i = 0; i < _evchannel_in.size(); ++i)
        {
            Event ev = _evchannel_in.get(i).getEvent(_evID[i], false);
            if (ev == null)
                continue;

            _evID[i] = ev.id + 1;

            //build event
            msg += "<event sender=\"" + ev.sender + "\""
                    + " event=\"" + ev.name + "\""
                    + " from=\"" + ev.time + "\""
                    + " dur=\"" + ev.dur + "\""
                    + " prob=\"1.000000\""
                    + " type=\"STRING\""
                    + " state=\"" + ev.state + "\""
                    + " glue=\"0\">"
                    + ev.msg
                    + "</event>";
        }

        if(msg != null)
        {
            msg = "<events fw=\"ssj\" v=\""+ _frame.getVersion() +"\">" + msg + "</events>";

            ByteBuffer buf = ByteBuffer.wrap(_buffer);
            buf.order(ByteOrder.BIG_ENDIAN);

            //store event
            buf.put(msg.getBytes());

            try
            {
                _out.write(_buffer, 0, buf.position());
                _out.flush();

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
