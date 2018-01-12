/*
 * InfraredSensor.java
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import hcm.ssj.core.Log;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.option.OptionList;

/**
 * Infrared sensor for google glass.<br>
 * A feature like wink or head detection needs to be enabled for the sensor to work.
 * To get IR-Sensor permission on glass, go to console and type in the following commands:
 * $ adb root                    (start adb anew with root permissions)
 * $ adb shell                   (open shell on glass)
 * cd sys/bus/i2c/devices/4-0035 (go to infrared sensor)
 * chmod 664 proxraw             (change file permissions)
 * ls -l proxraw                 (inspect file permissions (-rw-rw-r--))
 * exit                          (exit shell)
 * Created by Frank Gaibler on 13.08.2015.
 */
public class InfraredSensor extends hcm.ssj.core.Sensor
{
    private static final float ERROR = -1;
    private boolean report = true; //prevent message flooding

    /**
     *
     */
    public InfraredSensor()
    {
        _name = this.getClass().getSimpleName();
    }

    /**
	 *
     */
    @Override
    protected boolean connect() throws SSJFatalException
    {
        report = true;
        return true;
    }

    /**
	 *
     */
    @Override
    protected void disconnect() throws SSJFatalException
    {
    }

    /**
     * @return float
     */
    protected float getData()
    {
        try
        {
            Process process = Runtime.getRuntime().exec("cat /sys/bus/i2c/devices/4-0035/proxraw");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            int read;
            char[] buffer = new char[8];
            StringBuilder output = new StringBuilder();
            while ((read = reader.read(buffer)) > 0)
            {
                output.append(buffer, 0, read);
            }
            reader.close();
            String result = output.toString();
            if (result.length() > 0)
            {
                return Float.valueOf(result);
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        if (report)
        {
            report = false;
            Log.e("Check permissions on glass");
        }
        return ERROR;
    }

	@Override
	public OptionList getOptions()
	{
		return null;
	}
}

