/*
 * SocketWriter.java
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

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

import hcm.ssj.core.Cons;
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
public class SocketWriter extends Consumer {


	@Override
	public OptionList getOptions()
	{
		return options;
	}

	public class Options extends OptionList
    {
        public final Option<Integer> port = new Option<>("port", 34300, Integer.class, "");
        public final Option<String> ip = new Option<>("ip", "127.0.0.1", String.class, "");
        public final Option<Cons.SocketType> type = new Option<>("type", Cons.SocketType.UDP, Cons.SocketType.class, "");

        /**
         *
         */
        private Options() {
            addOptions();
        }
    }
    public final Options options = new Options();

    private DatagramSocket _socket_udp;
    private Socket _socket_tcp;
    private InetAddress _addr;
    private DataOutputStream _out;
    private byte[] _data;

    private boolean _connected = false;

    public SocketWriter()
    {
        _name = "SocketWriter";
    }

    @Override
	public void enter(Stream[] stream_in) throws SSJFatalException
    {
        //start client
        try {
            _addr = InetAddress.getByName(options.ip.get());
            switch(options.type.get()) {
                case UDP:
                    _socket_udp = new DatagramSocket();
                    break;
                case TCP:
                    _socket_tcp = new Socket(_addr, options.port.get());
                    _out = new DataOutputStream(_socket_tcp.getOutputStream());
                    break;
            }
        }
        catch (IOException e)
        {
            throw new SSJFatalException("error in setting up connection", e);
        }

        _data = new byte[stream_in[0].tot];

        Log.i("Streaming data to " + _addr.getHostName() +"@"+ options.port +"("+ options.type.get().toString() +")");
        _connected = true;
    }

    protected void consume(Stream[] stream_in, Event trigger) throws SSJFatalException
    {
        if (!_connected)
        {
            return;
        }

        try {
            switch(options.type.get()) {
                case UDP:
                    Util.arraycopy(stream_in[0].ptr(), 0, _data, 0, _data.length);
                    DatagramPacket pack = new DatagramPacket(_data, _data.length, _addr, options.port.get());
                    _socket_udp.send(pack);
                    break;
                case TCP:
                    Util.arraycopy(stream_in[0].ptr(), 0, _data, 0, _data.length);
                    _out.write(_data);
                    _out.flush();
                    break;
            }

        } catch (IOException e) {
            Log.w("failed sending data", e);
        }
    }

    public void flush(Stream[] stream_in) throws SSJFatalException
    {
        _connected = false;

        try {
            switch(options.type.get()) {
                case UDP:
                    _socket_udp.close();
                    _socket_udp = null;
                    break;
                case TCP:
                    _socket_tcp.close();
                    _socket_tcp = null;
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
