/*
 * BandListener.java
 * Copyright (c) 2017
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

package hcm.ssj.bitalino;

import info.plux.pluxapi.bitalino.BITalinoFrame;
import info.plux.pluxapi.bitalino.bth.OnBITalinoDataAvailable;

/**
 * Created by Michael Dietz on 06.07.2016.
 */
public class BitalinoListener implements OnBITalinoDataAvailable
{
	private final int TIMEOUT = 10000; //in ms

	BITalinoFrame lastDataFrame = null;
	private long lastDataTimestamp = 0;

	public BitalinoListener()
	{
		reset();
	}

	public void reset()
	{
		lastDataFrame = null;
		lastDataTimestamp = 0;
	}

	public int getAnalogData(int pos)
	{
		return lastDataFrame.getAnalog(pos);
	}

	public int getDigitalData(int pos)
	{
		return lastDataFrame.getDigital(pos);
	}

	private synchronized void dataReceived()
	{
		lastDataTimestamp = System.currentTimeMillis();
	}

	public boolean isConnected()
	{
		return System.currentTimeMillis() - lastDataTimestamp < TIMEOUT;
	}

	public boolean hasReceivedData()
	{
		return lastDataTimestamp != 0;
	}

	@Override
	public void onBITalinoDataAvailable(BITalinoFrame biTalinoFrame)
	{
		dataReceived();
		lastDataFrame = biTalinoFrame;
	}
}
