/*
 * SocketEventReader.java
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

import hcm.ssj.core.Cons;
import hcm.ssj.core.EventHandler;
import hcm.ssj.core.Log;
import hcm.ssj.core.Util;
import hcm.ssj.core.event.Event;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;

/**
 * Audio Sensor - get data from audio interface and forwards it
 * Created by Johnny on 05.03.2015.
 */
public class SocketEventReader extends EventHandler
{
    final int MAX_MSG_SIZE = 4096;

    public class Options extends OptionList
    {
        public Option<String> ip = new Option<>("ip", null, String.class, "");
        public Option<Integer> port = new Option<>("port", 0, Integer.class, "");
        public Option<Boolean> parseXmlToEvent = new Option<>("parseXmlToEvent", true, Boolean.class, "attempt to convert the message to an SSJ event format");

        /**
         *
         */
        private Options()
        {
            addOptions();
        }
    }
    public final Options options = new Options();

    DatagramSocket _socket;
    boolean _connected = false;
    byte[] _buffer;
    XmlPullParser _parser;

    public SocketEventReader()
    {
        _name = "SocketEventReader";
    }

    @Override
    public void enter()
    {
        if (options.ip.get() == null)
        {
            try
            {
                options.ip.set(Util.getIPAddress(true));
            }
            catch (SocketException e)
            {
                Log.e("unable to determine local IP address", e);
            }
        }

        try
        {
            _socket = new DatagramSocket(null);
            _socket.setReuseAddress(true);

            InetAddress addr = InetAddress.getByName(options.ip.get());
            InetSocketAddress saddr = new InetSocketAddress(addr, options.port.get());
            _socket.bind(saddr);
        }
        catch (IOException e)
        {
            Log.e("ERROR: cannot bind socket", e);
            return;
        }

        _buffer = new byte[MAX_MSG_SIZE];

        if(options.parseXmlToEvent.get())
        {
            try
            {
                _parser = Xml.newPullParser();
                _parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            }
            catch (XmlPullParserException e)
            {
                Log.e("unable to initialize parser", e);
                return;
            }
        }

        Log.i("socket ready ("+options.ip.get() + "@" + options.port.get() +")");
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
            Log.w("failed to receive packet", e);
            return;
        }

        if(!options.parseXmlToEvent.get())
        {
            Event ev = Event.create(Cons.Type.STRING);
            ev.setData(new String(_buffer, 0, packet.getLength()));
            _evchannel_out.pushEvent(ev);
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
                    Log.w("unknown or malformed socket message");
                    return;
                }

                while (_parser.next() != XmlPullParser.END_DOCUMENT)
                {
                    if (_parser.getEventType() == XmlPullParser.START_TAG && _parser.getName().equalsIgnoreCase("event"))
                    {
                        Event ev = Event.create(Cons.Type.STRING);

                        ev.name = _parser.getAttributeValue(null, "event");
                        ev.sender = _parser.getAttributeValue(null, "sender");
                        ev.time = Integer.valueOf(_parser.getAttributeValue(null, "from"));
                        ev.dur = Integer.valueOf(_parser.getAttributeValue(null, "dur"));
                        ev.state = Event.State.valueOf(_parser.getAttributeValue(null, "state"));

                        _parser.next();
                        ev.setData(Util.xmlToString(_parser));

                        _evchannel_out.pushEvent(ev);
                    }
                    if (_parser.getEventType() == XmlPullParser.END_TAG && _parser.getName().equalsIgnoreCase("events"))
                        break;
                }
            }
            catch (IOException | XmlPullParserException e)
            {
                Log.w("failed to receive packet", e);
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
