/*
 * BluetoothPressureSensor.java
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

package hcm.ssj.pressureMat;

import java.io.IOException;
import java.util.UUID;

import hcm.ssj.core.Log;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.ioput.BluetoothClient;
import hcm.ssj.ioput.BluetoothReader;

import static hcm.ssj.core.Cons.DEFAULT_BL_SERIAL_UUID;

/**
 * Created by Johnny on 05.03.2015.
 */
public class BluetoothPressureSensor extends BluetoothReader {


    protected short[][] _irecvData;

    public class Options extends OptionList {
        public final Option<String> connectionName = new Option<>("connectionName", "SSJ", String.class, "must match that of the peer");
        public final Option<String> serverName = new Option<>("serverName", "SSJ_BLServer", String.class, "");
        public final Option<String> serverAddr = new Option<>("serverAddr", null, String.class, "if this is a client");

        /**
         *
         */
        private Options() {
            addOptions();
        }
    }

    public final Options options = new Options();
    protected boolean _isreallyConnected = false;

    public BluetoothPressureSensor() {
        _name = "BLPressure";
    }

    @Override
    public void init() {
        try {
            _conn = new BluetoothClient(UUID.fromString(DEFAULT_BL_SERIAL_UUID), options.serverName.get(), options.serverAddr.get());

        } catch (IOException e) {
            Log.e("error in setting up connection " + options.connectionName, e);
        }
    }

    @Override
    public boolean connect() {
        _irecvData = new short[_provider.size()][];
        for (int i = 0; i < _provider.size(); ++i) {
            _irecvData[i] = new short[_provider.get(i).getOutputStream().tot];
        }
        if (!super.connect()) {
            return false;
        }

        try {
            byte[] data = {10, 0};
            _conn.output().write(data);
        } catch (IOException e) {
            Log.w("unable to write init sequence to bt socket", e);
        }

        _isreallyConnected = true;
        return true;
    }

    public void update() {
        if (!_conn.isConnected() && _isreallyConnected) {
            return;
        }

        try {

            byte[] header = new byte[4];
            byte[] data = new byte[1];
            short[] pair = new short[2];
            int stateVariable = -1;


            for (; _conn.input().read(data) != -1; ) {

                //shift header further
                header[3] = header[2];
                header[2] = header[1];
                header[1] = header[0];
                header[0] = data[0];

                // new header found
                if (header[0] == -128 && header[1] == 0 && header[3] == 0 && header[3] == 0) {

                    stateVariable = 0;


                } else // process data
                    if (stateVariable >= 0) {
                        if (stateVariable % 3 == 0) {
                            //first 8 bit of 12 bit int
                            pair[1] = (short) ((short) (((short) data[0]) + 128) << 4);
                        } else if (stateVariable % 3 == 1) {
                            // second 4 bit of 12 bit int
                            pair[1] = (short) (pair[1] | (((((short) data[0]) + 128) & 0xf0) >> 4));

                            //first 4 bit of 12 bit int
                            pair[0] = (short) (((short) (((short) data[0]) + 128) & 0x0f) << 8);
                        } else if (stateVariable % 3 == 2) {
                            //second 8 bit of 12 bit int
                            pair[0] = (short) ((((short) data[0]) + 128) | pair[0]);

                            //copy package of two pixels tp output
                            _irecvData[0][(stateVariable / 3) * 2] = pair[0];
                            _irecvData[0][((stateVariable / 3) * 2) + 1] = pair[1];

                        }


                        if (stateVariable == 1535) { //search new header

                            stateVariable = -1;
                        }

                        stateVariable++;
                    }

            }


        } catch (IOException e) {
            Log.w("unable to read from data stream", e);
        }
    }

    public short[] getDataInt(int channel_id) {
        return _irecvData[channel_id];
    }

}
