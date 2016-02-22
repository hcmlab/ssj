/*
 * SocketEventReader.java
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

import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;

import hcm.ssj.core.Event;
import hcm.ssj.core.EventHandler;
import hcm.ssj.core.Util;

/**
 * Audio Sensor - get data from audio interface and forwards it
 * Created by Johnny on 05.03.2015.
 */
public class SocketEventReader extends EventHandler
{
    final int MAX_MSG_SIZE = 4096;

    public class Options
    {
        public String ip = null;
        public int port;
        public boolean parseXmlToEvent = true; //attempt to convert the message to an SSJ event format
    }
    public Options options = new Options();

    DatagramSocket _socket;
    boolean _connected = false;
    byte[] _buffer;
    XmlPullParser _parser;

    public SocketEventReader()
    {
        options = new Options();
        _name = "SSJ_ehandler_SocketEventReader";
    }

    @Override
    public void enter()
    {
        if (options.ip == null)
        {
            try
            {
                options.ip = Util.getIPAddress(true);
            }
            catch (SocketException e)
            {
                Log.e(_name, "unable to determine local IP address", e);
            }
        }

        try
        {
            _socket = new DatagramSocket(null);
            _socket.setReuseAddress(true);

            InetAddress addr = InetAddress.getByName(options.ip);
            InetSocketAddress saddr = new InetSocketAddress(addr, options.port);
            _socket.bind(saddr);
        }
        catch (IOException e)
        {
            Log.e(_name, "ERROR: cannot bind socket", e);
            return;
        }

        _buffer = new byte[MAX_MSG_SIZE];

        if(options.parseXmlToEvent)
        {
            try
            {
                _parser = Xml.newPullParser();
                _parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            }
            catch (XmlPullParserException e)
            {
                Log.e(_name, "unable to initialize parser", e);
                return;
            }
        }

        Log.i(_name, "socket ready ("+options.ip + "@" + options.port+")");
        _connected = true;
    }

    @Override
    protected void process()
    {
        if(!_connected)
            return;

        DatagramPacket packet = new DatagramPacket(_buffer, _buffer.length);

        try
        {
            _socket.receive(packet);
        }
        catch (IOException e)
        {
            Log.w(_name, "failed to receive packet", e);
            return;
        }

        if(!options.parseXmlToEvent)
        {
            _evchannel_out.pushEvent(_buffer, 0, packet.getLength());
        }
        else
        {
            try
            {
                _parser.setInput(new ByteArrayInputStream(_buffer, 0, packet.getLength()), null);

                //first element must be <events>
                _parser.next();
                if (_parser.getEventType() != XmlPullParser.START_TAG || !_parser.getName().equalsIgnoreCase("events"))
                {
                    Log.w(_name, "unknown or malformed socket message");
                    return;
                }

                while (_parser.next() != XmlPullParser.END_DOCUMENT)
                {
                    if (_parser.getEventType() == XmlPullParser.START_TAG && _parser.getName().equalsIgnoreCase("event"))
                    {
                        Event ev = new Event();

                        ev.name = _parser.getAttributeValue(null, "event");
                        ev.sender = _parser.getAttributeValue(null, "sender");
                        ev.time = Integer.valueOf(_parser.getAttributeValue(null, "from"));
                        ev.dur = Integer.valueOf(_parser.getAttributeValue(null, "dur"));
                        ev.state = Event.State.valueOf(_parser.getAttributeValue(null, "state"));

                        _parser.next();
                        ev.msg = Util.xmlToString(_parser);

                        _evchannel_out.pushEvent(ev);
                    }
                    if (_parser.getEventType() == XmlPullParser.END_TAG && _parser.getName().equalsIgnoreCase("events"))
                        break;
                }
            }
            catch (IOException | XmlPullParserException e)
            {
                Log.w(_name, "failed to receive packet", e);
                return;
            }
        }

    }

    @Override
    public void flush()
    {
        _socket.close();
    }
}
