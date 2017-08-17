/*
 * GPSListener.java
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

package hcm.ssj.gps;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

/**
 * Created by Michael Dietz on 05.07.2016.
 */
public class GPSListener implements LocationListener
{
	private double latitude;
	private double longitude;
	private long   time;
	private long   updateTime;

	public boolean receivedData;

	public GPSListener(long updateTime)
	{
		reset();

		this.updateTime = updateTime;
	}

	public void reset()
	{
		latitude = 0;
		longitude = 0;
		time = 0;

		receivedData = false;
	}

	@Override
	public void onLocationChanged(Location location)
	{
		if (location.getProvider().equals(LocationManager.NETWORK_PROVIDER) && location.getTime() - time > updateTime * 2)
		{
			receivedData = true;

			latitude = location.getLatitude();
			longitude = location.getLongitude();
		}

		if (location.getProvider().equals(LocationManager.GPS_PROVIDER))
		{
			receivedData = true;

			latitude = location.getLatitude();
			longitude = location.getLongitude();
			time = location.getTime();
		}
	}

	public double getLatitude()
	{
		return latitude;
	}

	public double getLongitude()
	{
		return longitude;
	}

	public long getTime()
	{
		return time;
	}

	// Unused listener methods

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras)
	{

	}

	@Override
	public void onProviderEnabled(String provider)
	{

	}

	@Override
	public void onProviderDisabled(String provider)
	{
		receivedData = false;
	}
}
