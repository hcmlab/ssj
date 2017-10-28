/*
 * CameraChannel.java
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

package hcm.ssj.camera;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.SSJException;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.SensorChannel;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.ImageStream;
import hcm.ssj.core.stream.Stream;

/**
 * Camera provider.<br>
 * Created by Frank Gaibler on 21.12.2015.
 */
public class CameraChannel extends SensorChannel
{
    /**
     * All options for the camera provider
     */
    public class Options extends OptionList
    {
        /**
         * The rate in which the provider samples data from the camera.<br>
         * <b>Attention!</b> The camera sensor will provide new data according to its frame rate and min max preview.
         */
        public final Option<Double> sampleRate = new Option<>("sampleRate", 20., Double.class, "sample rate for camera pictures");

        /**
         *
         */
        private Options()
        {
            addOptions();
        }
    }

    public final Options options = new Options();

    private int sampleDimension = 0;
    private CameraSensor cameraSensor = null;

    /**
     *
     */
    public CameraChannel()
    {
        super();
        _name = this.getClass().getSimpleName();
    }

    /**
     *
     */
    @Override
    protected void init() throws SSJException
    {
        cameraSensor = (CameraSensor) _sensor;
        //get sample dimension from camera
        cameraSensor.prePrepare();
        sampleDimension = cameraSensor.getBufferSize();
        //return camera resources immediately
        cameraSensor.releaseCamera();
    }

    /**
	 * @param stream_out Stream
	 */
    @Override
    public void enter(Stream stream_out) throws SSJFatalException
    {
        if (stream_out.num != 1)
        {
            Log.w("unsupported stream format. sample number = " + stream_out.num);
        }
    }

    /**
     * @param stream_out Stream
     */
    @Override
    protected boolean process(Stream stream_out) throws SSJFatalException
    {
        byte[] out = stream_out.ptrB();
        cameraSensor.swapBuffer(out, false);

        return true;
    }

    /**
     * @return double
     */
    @Override
    public double getSampleRate()
    {
        return options.sampleRate.get();
    }

    /**
     * @return int
     */
    @Override
    final public int getSampleDimension()
    {
        return sampleDimension;
    }

    /**
     * @return int
     */
    @Override
    public int getSampleBytes()
    {
        return 1;
    }

    /*
     * @return Cons.Type
     */
    @Override
    public Cons.Type getSampleType()
    {
        return Cons.Type.IMAGE;
    }

    /**
     * @param stream_out Stream
     */
    @Override
    protected void describeOutput(Stream stream_out)
    {
        stream_out.desc = new String[]{"video"};

        ((ImageStream)_stream_out).width = cameraSensor.getImageWidth();
        ((ImageStream)_stream_out).height = cameraSensor.getImageHeight();
        //((ImageStream)_stream_out).format = cameraSensor.getImageFormat();
    }
}
