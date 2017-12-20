/*
 * CPULoadChannel.java
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

package hcm.ssj.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.SSJApplication;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.SensorChannel;
import hcm.ssj.core.Util;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;

/**
 * Audio Sensor - get data from audio interface and forwards it
 * Created by Johnny on 05.03.2015.
 */
public class CPULoadChannel extends SensorChannel
{
	@Override
	public OptionList getOptions()
	{
		return options;
	}

	public class Options extends OptionList
    {
        public final Option<Integer> sampleRate = new Option<>("sampleRate", 1, Integer.class, "");
        public final Option<String> packagename = new Option<>("packagename", SSJApplication.getAppContext().getPackageName(), String.class, "name of the package to monitor");

        /**
         *
         */
        private Options()
        {
            addOptions();
        }
    }
    public final Options options = new Options();

    private String cmd;

    public CPULoadChannel()
    {
        _name = "Profiler_CPU";
    }


    @Override
	public void enter(Stream stream_out) throws SSJFatalException
    {
        //set delay to be < frame window since top is blocking
        cmd = "top -n 1 -m 10 -d " + (1.0 / options.sampleRate.get()) * 0.9;
    }

    @Override
    protected boolean process(Stream stream_out) throws SSJFatalException
    {
        stream_out.ptrF()[0] = getCPULoad(cmd, options.packagename.get());
        return true;
    }

    private float getCPULoad(String cmd, String packagename)
    {
        try {
            Process proc = Runtime.getRuntime().exec(cmd);

            BufferedReader read = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            int cpuid = 0;
            String line;
            while ((line = read.readLine()) != null) {
                if (line.length() == 0 || line.startsWith("User"))
                    continue;

                line = line.trim();
                if(line.startsWith("PID"))
                {
                    //compute index of CPU load
                    String elems[] = line.split("\\s+");
                    for(int i = 0; i< elems.length; i++)
                    {
                        if(elems[i].contains("CPU")) {
                            cpuid = i;
                            break;
                        }
                    }
                }
                else if (line.contains(packagename)) {
                    String elems[] = line.split("\\s+");
                    return Float.parseFloat(elems[cpuid].replace("%", ""));
                }
            }
        }
        catch(IOException e)
        {
            Log.w("error executing top cmd", e);
        }
        return 0;
    }

    @Override
    public void flush(Stream stream_out) throws SSJFatalException
    {
    }

    @Override
    public int getSampleDimension()
    {
        return 1;
    }

    @Override
    public double getSampleRate()
    {
        return options.sampleRate.get();
    }

    @Override
    public int getSampleBytes()
    {
        return Util.sizeOf(Cons.Type.FLOAT);
    }

    @Override
    public Cons.Type getSampleType()
    {
        return Cons.Type.FLOAT;
    }

    @Override
    public void describeOutput(Stream stream_out)
    {
        stream_out.desc = new String[1];
        stream_out.desc[0] = "CPU";
    }
}
