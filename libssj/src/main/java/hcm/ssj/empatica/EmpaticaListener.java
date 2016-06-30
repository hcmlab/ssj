/*
 * EmpaticaListener.java
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

package hcm.ssj.empatica;

import com.empatica.empalink.delegate.EmpaDataDelegate;

/**
 * Created by Michael Dietz on 15.04.2015.
 */
public class EmpaticaListener implements EmpaDataDelegate
{
	private float gsr;
	private float bvp;
	private float ibi;
	private float temperature;
	private float battery;
	private int   x;
	private int   y;
	private int   z;

	public boolean receivedData;

	public EmpaticaListener()
	{
		reset();
	}

	public void reset()
	{
		gsr = 0;
		bvp = 0;
		ibi = 0;
		temperature = 0;
		battery = 0;
		x = 0;
		y = 0;
		z = 0;
		receivedData = false;
	}

	@Override
	public void didReceiveGSR(float gsr, double timestamp)
	{
		receivedData = true;
		this.gsr = gsr;
	}

	@Override
	public void didReceiveBVP(float bvp, double timestamp)
	{
		receivedData = true;
		this.bvp = bvp;
	}

	@Override
	public void didReceiveIBI(float ibi, double timestamp)
	{
		receivedData = true;
		this.ibi = ibi;
	}

	@Override
	public void didReceiveTemperature(float temperature, double timestamp)
	{
		receivedData = true;
		this.temperature = temperature;
	}

	@Override
	public void didReceiveBatteryLevel(float battery, double timestamp)
	{
		receivedData = true;
		this.battery = battery;
	}

	@Override
	public void didReceiveAcceleration(int x, int y, int z, double timestamp)
	{
		receivedData = true;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public float getGsr()
	{
		return gsr;
	}

	public float getBvp()
	{
		return bvp;
	}

	public float getIbi()
	{
		return ibi;
	}

	public float getTemperature()
	{
		return temperature;
	}

	public float getBattery()
	{
		return battery;
	}

	public int getX()
	{
		return x;
	}

	public int getY()
	{
		return y;
	}

	public int getZ()
	{
		return z;
	}
}
