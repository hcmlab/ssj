/*
 * BVPAndisChannel.java
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

package hcm.ssj.bleSensor;

import hcm.ssj.core.Cons;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.SensorChannel;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;

/**
 * Created by Michael Dietz on 15.04.2015.
 */
public class BVPAndisChannel extends SensorChannel {

    public class Options extends OptionList
    {
        public final Option<Float> sampleRate = new Option<>("sampleRate", 100f, Float.class, "sensor sample rate in Hz");
        public final Option<Integer> dimensions = new Option<>("dimensions", 5, Integer.class, "number of dimensions");

        private Options()
        {
            addOptions();
        }
    }
    public Options options = new Options();

    protected BLESensorListener _listener;

    public BVPAndisChannel() {
        _name = "BLE_BVP";
    }

    @Override
	public void enter(Stream stream_out) throws SSJFatalException
	{

        _listener = ((BLESensor) _sensor).listener;

    }


    @Override
    protected boolean process(Stream stream_out) throws SSJFatalException
    {
        int dimension = getSampleDimension();
        int[] out = stream_out.ptrI();

        out[0] = _listener.getAcc();
        out[1] = _listener.getTemperature();
        out[2] = _listener.getBpm();
        out[3] = _listener.getRMSSD();
        out[4] = _listener.getGsr();


        return true;
    }

    @Override
    public double getSampleRate() {
        return options.sampleRate.get();
    }

    @Override
    public int getSampleDimension() {
        return options.dimensions.get();
    }

    @Override
    public int getSampleBytes() {
        return 4;
    }

    @Override
    public Cons.Type getSampleType() {
        return Cons.Type.INT;
    }

    @Override
    protected void describeOutput(Stream stream_out) {
        stream_out.desc = new String[stream_out.dim];
        stream_out.desc[0] = "Acc";
        stream_out.desc[1] = "Tmp";
        stream_out.desc[2] = "Bvp";
        stream_out.desc[3] = "Rmssd"; //bvp raw values
        stream_out.desc[4] = "gsr";
    }

    @Override
    public OptionList getOptions()
    {
        return options;
    }
}
