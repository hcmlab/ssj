/*
 * SocketEventWriter.java
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
import java.util.Arrays;

import hcm.ssj.core.Cons;
import hcm.ssj.core.EventHandler;
import hcm.ssj.core.Log;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.Util;
import hcm.ssj.core.event.Event;
import hcm.ssj.file.FileCons;

/**
 * Created by Johnny on 05.03.2015.
 */
public class SocketEventWriter extends EventHandler
{
    public final static int SOCKET_TYPE_UDP = 0;
    public final static int SOCKET_TYPE_TCP = 1;

    public class Options
    {
        public int port = 34300;
        public String ip = "127.0.0.1";
        public int type = SOCKET_TYPE_UDP;
    }
    public Options options = new Options();

    private DatagramSocket _socket_udp;
    private Socket _socket_tcp;
    private InetAddress _addr;
    private DataOutputStream _out;

    StringBuilder _builder = new StringBuilder();
    byte[] _buffer;
    int _evID[];

    private boolean _connected = false;

    public SocketEventWriter()
    {
        _name = "SocketEventWriter";
        _doWakeLock = true;
    }

    @Override
	public void enter() throws SSJFatalException
    {
		if (_evchannel_in == null || _evchannel_in.size() == 0)
		{
            throw new SSJFatalException("no incoming event channels defined");
		}

        //start client
        String protocol = "";
        try {
            _addr = InetAddress.getByName(options.ip);
            switch(options.type) {
                case SOCKET_TYPE_UDP:
                    _socket_udp = new DatagramSocket();
                    protocol = "UDP";
                    break;
                case SOCKET_TYPE_TCP:
                    _socket_tcp = new Socket(_addr, options.port);
                    _out = new DataOutputStream(_socket_tcp.getOutputStream());
                    protocol = "TCP";
                    break;
            }
        }
        catch (IOException e)
        {
            throw new SSJFatalException("error in setting up connection", e);
        }

        _buffer = new byte[Cons.MAX_EVENT_SIZE];
        _evID = new int[_evchannel_in.size()];
        Arrays.fill(_evID, 0);

        Log.i("Streaming data to " + _addr.getHostName() +"@"+ options.port +"("+ protocol +")");
        _connected = true;
    }

    @Override
    protected void process() throws SSJFatalException
    {
        if (!_connected)
        {
            return;
        }

        _builder.delete(0, _builder.length());

        _builder.append("<events ssi-v=\"2\" ssj-v=\"");
        _builder.append(_frame.getVersion());
        _builder.append("\">");

        int count = 0;
        for(int i = 0; i < _evchannel_in.size(); ++i)
        {
            Event ev = _evchannel_in.get(i).getEvent(_evID[i], false);
            if (ev == null)
            {
                continue;
            }

            count++;
            _evID[i] = ev.id + 1;

            //build event
            Util.eventToXML(_builder, ev);
            _builder.append(FileCons.DELIMITER_LINE);
        }

        if(count > 0)
        {
            _builder.append( "</events>");
            byte[] data;
            try
            {
                switch(options.type) {
                    case SOCKET_TYPE_UDP:
                        data = _builder.toString().getBytes();
                        DatagramPacket pack = new DatagramPacket(data, data.length, _addr, options.port);
                        _socket_udp.send(pack);
                        break;
                    case SOCKET_TYPE_TCP:
                        data = _builder.toString().getBytes();
                        _out.write(data);
                        _out.flush();
                        break;
                }

            }
            catch (IOException e)
            {
                Log.w("failed sending data", e);
            }
        }
    }

    public void flush() throws SSJFatalException
    {
        _connected = false;

        try {
            switch(options.type) {
                case SOCKET_TYPE_UDP:
                    _socket_udp.close();
                    _socket_udp = null;
                    break;
                case SOCKET_TYPE_TCP:
                    _socket_tcp.close();
                    _socket_tcp = null;
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
