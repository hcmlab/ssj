/*
 * SocketReader.java
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

import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.Sensor;
import hcm.ssj.core.Util;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;

/**
 * Audio Sensor - get data from audio interface and forwards it
 * Created by Johnny on 05.03.2015.
 */
public class SocketReader extends Sensor
{
    public class Options extends OptionList
    {
        public Option<String> ip = new Option<>("ip", null, String.class, "");
        public Option<Integer> port = new Option<>("port", 0, Integer.class, "");
        public final Option<Cons.SocketType> type = new Option<>("type", Cons.SocketType.UDP, Cons.SocketType.class, "");

        /**
         *
         */
        private Options()
        {
            addOptions();
        }
    }
    public final Options options = new Options();

    private DatagramSocket _socket_udp = null;
    private ServerSocket _server_tcp = null;
    private Socket _client_tcp = null;
    private DataInputStream _in;

    boolean _connected = false;
    byte[] _buffer;

    public SocketReader()
    {
        _name = "SocketReader";
    }

    @Override
    public boolean connect()
    {
        _connected = false;
        _socket_udp = null;
        _server_tcp = null;
        _client_tcp = null;

        Log.i("setting up socket ("+options.ip.get() + "@" + options.port.get() +" / "+ options.type.get().toString() +")");

        if (options.ip.get() == null)
        {
            try
            {
                options.ip.set(Util.getIPAddress(true));
            }
            catch (SocketException e)
            {
                _frame.error(_name, "unable to determine local IP address", e);
            }
        }

        try
        {
            InetAddress addr = InetAddress.getByName(options.ip.get());
            InetSocketAddress saddr = new InetSocketAddress(addr, options.port.get());

            switch(options.type.get()) {
                case UDP:
                    _socket_udp = new DatagramSocket(null);
                    _socket_udp.setReuseAddress(true);
                    _socket_udp.bind(saddr);
                    break;
                case TCP:
                    _server_tcp = new ServerSocket(options.port.get());
                    Log.i("waiting for client ... ");
                    _client_tcp = _server_tcp.accept();
                    _in = new DataInputStream(_client_tcp.getInputStream());
                    break;
            }
        }
        catch (IOException e)
        {
            _frame.error(_name, "ERROR: cannot bind/connect socket", e);
            return false;
        }

        _buffer = new byte[_provider.get(0).getOutputStream().tot];
        _connected = true;

        Log.i("socket connected");

        return true;
    }

    @Override
    protected void update()
    {
        if(!_connected)
            return;

        try {
            switch(options.type.get()) {
                case UDP:
                    DatagramPacket packet = new DatagramPacket(_buffer, _buffer.length);
                    _socket_udp.receive(packet);
                    break;
                case TCP:
                    _in.readFully(_buffer);
                    break;
            }

        } catch (IOException e) {
            Log.w("failed receiving data", e);
        }
    }


    @Override
    public void disconnect()
    {
        _connected = false;

        try {
            switch(options.type.get()) {
                case UDP:
                    if(_socket_udp != null) {
                        _socket_udp.close();
                        _socket_udp = null;
                    }
                    break;
                case TCP:
                    if(_client_tcp != null) {
                        _client_tcp.close();
                        _client_tcp = null;
                    }
                    if(_server_tcp != null) {
                        _server_tcp.close();
                        _server_tcp = null;
                    }
                    break;
            }
        } catch (Exception e) {
            Log.w("failed closing socket", e);
        }
    }

    public byte[] getData()
    {
        return _buffer;
    }
}
