/*
 * BluetoothEventReader.java
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
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Event;
import hcm.ssj.core.EventHandler;
import hcm.ssj.core.Log;
import hcm.ssj.core.Util;

/**
 * Bluetooth event reader - reads SSJ events from a bluetooth source
 * Created by Johnny on 07.04.2015.
 */
public class BluetoothEventReader extends EventHandler
{
    public class Options
    {
        public String serverName = "SSJ_BLServer";
        public String connectionName = "SSJ"; //must match that of the peer
        public String serverAddr; //if this is a client
        public BluetoothConnection.Type connectionType = BluetoothConnection.Type.SERVER;
        public boolean parseXmlToEvent = true; //attempt to convert the message to an SSJ event format
    }
    public Options options = new Options();

    private final int MSG_HEADER_SIZE = 12;

    private BluetoothConnection _conn;
    private DataInputStream _in;
    boolean _connected = false;
    byte[] _buffer;

    XmlPullParser _parser;

    public BluetoothEventReader()
    {
        _name = "SSJ_ehandler_BluetoothEventReader";
    }

    @Override
    public void enter()
    {
        try {
            switch(options.connectionType)
            {
                case SERVER:
                    _conn = new BluetoothServer(options.connectionName, options.serverName);
                    _conn.connect();
                    break;
                case CLIENT:
                    _conn = new BluetoothClient(options.connectionName, options.serverName, options.serverAddr);
                    _conn.connect();
                    break;
            }

            _in = new DataInputStream(_conn.getSocket().getInputStream());
        } catch (Exception e)
        {
            Log.e("error in setting up connection", e);
            return;
        }

        BluetoothDevice dev = _conn.getSocket().getRemoteDevice();
        Log.i("connected to " + dev.getName() + " @ " + dev.getAddress());

        if(!options.parseXmlToEvent)
        {
            _buffer = new byte[Cons.MAX_EVENT_SIZE];
        }
        else
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

        _connected = true;
    }

    @Override
    protected void process()
    {
        if(!_connected)
            return;

        try
        {
            //we check whether there is any data as reads are blocking for bluetooth
            if (_in.available() == 0)
                return;

            if (!options.parseXmlToEvent)
            {
                int len = _in.read(_buffer);
                _evchannel_out.pushEvent(_buffer, 0, len);
            }
            else
            {
                _parser.setInput(new InputStreamReader(_in));

                //first element must be <events>
                _parser.next();
                if (_parser.getEventType() != XmlPullParser.START_TAG || !_parser.getName().equalsIgnoreCase("events"))
                {
                    Log.w("unknown or malformed bluetooth message");
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
        }
        catch(IOException | XmlPullParserException e)
        {
            Log.w("failed to receive or parse package", e);
            return;
        }
    }

    @Override
    public void flush()
    {
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
