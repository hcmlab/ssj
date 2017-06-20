/*
 * BluetoothEventReader.java
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
import android.content.Context;
import android.os.PowerManager;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.UUID;

import hcm.ssj.core.Cons;
import hcm.ssj.core.EventHandler;
import hcm.ssj.core.Log;
import hcm.ssj.core.SSJApplication;
import hcm.ssj.core.Util;
import hcm.ssj.core.event.Event;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;

/**
 * Bluetooth event reader - reads SSJ events from a bluetooth source
 * Created by Johnny on 07.04.2015.
 */
public class BluetoothEventReader extends EventHandler
{
    public class Options extends OptionList
    {
        public final Option<String> serverName = new Option<>("serverName", "SSJ_BLServer", String.class, "");
        public final Option<String> connectionName = new Option<>("connectionName", "SSJ", String.class, "must match that of the peer");
        public final Option<String> serverAddr = new Option<>("serverAddr", null, String.class, "if this is a client");
        public final Option<BluetoothConnection.Type> connectionType = new Option<>("connectionType", BluetoothConnection.Type.SERVER, BluetoothConnection.Type.class, "");
        public final Option<Boolean> parseXmlToEvent = new Option<>("parseXmlToEvent", true, Boolean.class, "attempt to convert the message to an SSJ event format");

        /**
         *
         */
        private Options() {
            addOptions();
        }
    }
    public final Options options = new Options();

    private final int MSG_HEADER_SIZE = 12;

    private BluetoothConnection _conn;
    boolean _connected = false;
    byte[] _buffer;

    XmlPullParser _parser;
    PowerManager _mgr;
    PowerManager.WakeLock _wakeLock;

    public BluetoothEventReader()
    {
        _name = "BluetoothEventReader";

        _doWakeLock = false; //disable SSJ's WL-manager, WL will be handled locally
        _mgr = (PowerManager) SSJApplication.getAppContext().getSystemService(Context.POWER_SERVICE);
        _wakeLock = _mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this._name);
    }

    @Override
    public void enter()
    {
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

        if(!options.parseXmlToEvent.get())
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
        if(!_connected || !_conn.isConnected())
            return;

        try
        {
            if (!options.parseXmlToEvent.get())
            {
                int len = _conn.input().read(_buffer);
                _wakeLock.acquire();

                Event ev = Event.create(Cons.Type.STRING);
                ev.setData(new String(_buffer, 0, len));
                _evchannel_out.pushEvent(ev);
            }
            else
            {
                _parser.setInput(new InputStreamReader(_conn.input()));

                //first element must be <events>
                _parser.next();

                _wakeLock.acquire();

                if (_parser.getEventType() != XmlPullParser.START_TAG || !_parser.getName().equalsIgnoreCase("events"))
                {
                    Log.w("unknown or malformed bluetooth message");
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
            _conn.notifyDataTranferResult(true);
        }
        catch(IOException e)
        {
            Log.w("failed to receive BL data", e);
            _conn.notifyDataTranferResult(false);
        }
        catch(XmlPullParserException e)
        {
            Log.w("failed to parse package", e);
        }
        finally
        {
            if(_wakeLock.isHeld())
                _wakeLock.release();
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
