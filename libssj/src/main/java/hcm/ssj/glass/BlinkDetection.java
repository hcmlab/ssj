/*
 * BlinkDetection.java
 * Copyright (c) 2018
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

package hcm.ssj.glass;

import java.util.LinkedList;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.Transformer;
import hcm.ssj.core.Util;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;

/**
 * Blink detection for the infrared sensor of google glass.<br>
 * Returns a high signal every time a blink is detected.<br>
 * <b>Warning!</b> The nature of the algorithm delays the output by 3 input values.<br>
 * Created by Frank Gaibler on 24.08.2015.
 *
 * @see <a href="https://www.d2.mpi-inf.mpg.de/sites/default/files/ishimaru14_ah.pdf">In the Blink of an Eye</a>
 */
public class BlinkDetection extends Transformer
{
	@Override
	public OptionList getOptions()
	{
		return options;
	}

	/**
     * All options for the transformer
     */
    public class Options extends OptionList
    {
        /**
         * Peak threshold for blink detection.<br>
         * A lower value will detect more alleged blinks.
         */
        public final Option<Float> blinkThreshold = new Option<>("blinkThreshold", 3.5f, Float.class, "Peak threshold for blink detection");
        /**
         * Variance between left and right value of the peak value.<br>
         * If the threshold is reached, the blink will not be counted,
         * because it is assumed to be a false blink due to high movement.
         */
        public final Option<Float> varianceThreshold = new Option<>("varianceThreshold", 25.f, Float.class, "Variance between left and right value of the peak value");
        /**
         * Count the blinks and return the additive result instead of giving a high signal when a blink occurs.
         */
        public final Option<Boolean> countBlink = new Option<>("countBlink", false, Boolean.class, "Count the blinks and return the additive result instead of giving a high signal when a blink occurs");

        /**
         *
         */
        private Options()
        {
            addOptions();
        }
    }

    public final Options options = new Options();
    //constants
    private static final int DIMENSION = 1;
    //helper variables
    private final float timeThreshold = 500;
    private float timeSave = timeThreshold + 1;
    private final int maxSize = 7;
    private int count = 0;
    private final LimitedQueue<Float> values = new LimitedQueue<>(maxSize);

    /**
     *
     */
    public BlinkDetection()
    {
        _name = this.getClass().getSimpleName();
    }

    /**
	 * The input stream should consist of 1-dimensional float values
     *  @param stream_in  Stream[]
     * @param stream_out Stream
	 */
    @Override
    public void enter(Stream[] stream_in, Stream stream_out) throws SSJFatalException
    {
        //no check for a specific type to allow for different providers
        if (stream_in.length < 1 || stream_in[0].dim < DIMENSION)
        {
            Log.e("invalid input stream");
        }
        //init values
        count = 0;
        timeSave = timeThreshold + 1;
        values.clear();
    }

    /**
     * @param stream_in  Stream[]
     * @param stream_out Stream
     */
    @Override
    public void transform(Stream[] stream_in, Stream stream_out) throws SSJFatalException
    {
        float[] dataAcc = stream_in[0].ptrF();
        float[] out = stream_out.ptrF();
        for (int i = 0; i < stream_in[0].num; i++)
        {
            //add to fifo
            values.add(dataAcc[i]);
            //write to output
            out[i] = detectBlink(stream_in[0].sr);
        }
    }

    /**
     * Detect blinks by comparing the last 7 values.<br>
     * The nature of the algorithm delays the output by 3 input values.<br>
     * The code is slightly modified to improve computation and to fix obvious mistakes.
     *
     * @param sampleRate double
     * @return float
     * @see <a href="https://github.com/shoya140/GlassLogger/blob/master/src/com/mrk1869/glasslogger/MonitoringActivity.java">blink detection github link</a>
     */
    private float detectBlink(double sampleRate)
    {
        if (values.size() == maxSize && timeSave > timeThreshold)
        {
            float left = (values.get(0) + values.get(1) + values.get(2)) / 3.0f; //average range of values before the probable peak
            float right = (values.get(4) + values.get(5) + values.get(6)) / 3.0f; //average range of values after the probable peak
            float peak = values.get(3); //the probable peak
            if ((left >= peak || peak >= right) && (left <= peak || peak <= right))
            {
                float left_to_right = Math.abs(right - left);
                //check for high variance which indicates unfeasible sensor data
                if (left_to_right < options.varianceThreshold.get())
                {
                    float peak_to_left = Math.abs(peak - left);
                    float peak_to_right = Math.abs(peak - right);
                    if (peak_to_left >= left_to_right && peak_to_right >= left_to_right)
                    {
                        float diff = Math.abs(peak - ((left + right) / 2.0f));
                        if (diff > options.blinkThreshold.get())
                        {
                            timeSave = 0;
                            return options.countBlink.get() ? ++count : 1;
                        }
                    }
                }
            }
        }
        timeSave += 1000 / sampleRate;
        return options.countBlink.get() ? count : 0;
    }

    /**
     * @param stream_in Stream[]
     * @return int
     */
    @Override
    public final int getSampleDimension(Stream[] stream_in)
    {
        return DIMENSION;
    }

    /**
     * @param stream_in Stream[]
     * @return int
     */
    @Override
    public int getSampleBytes(Stream[] stream_in)
    {
        return Util.sizeOf(Cons.Type.FLOAT);
    }

    /**
     * @param stream_in Stream[]
     * @return Cons.Type
     */
    @Override
    public Cons.Type getSampleType(Stream[] stream_in)
    {
        return Cons.Type.FLOAT;
    }

    /**
     * @param sampleNumber_in int
     * @return int
     */
    @Override
    public int getSampleNumber(int sampleNumber_in)
    {
        return sampleNumber_in;
    }

    /**
     * @param stream_in  Stream[]
     * @param stream_out Stream
     */
    @Override
    protected void describeOutput(Stream[] stream_in, Stream stream_out)
    {
        stream_out.desc = new String[DIMENSION];
        stream_out.desc[0] = options.countBlink.get() ? "blnkCnt" : "blnk";
    }

    /**
     * Simple fifo implementation.
     *
     * @param <E> queue type
     */
    private class LimitedQueue<E> extends LinkedList<E>
    {
        private int limit;

        /**
         * @param limit int
         */
        public LimitedQueue(int limit)
        {
            this.limit = limit;
        }

        /**
         * @param o E
         * @return boolean
         */
        @Override
        public boolean add(E o)
        {
            super.add(o);
            while (size() > limit)
            {
                super.remove();
            }
            return true;
        }
    }
}
