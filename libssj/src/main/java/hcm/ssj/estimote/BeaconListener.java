/*
 * BeaconListener.java
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

package hcm.ssj.estimote;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.estimote.sdk.Utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Michael Dietz on 08.03.2017.
 */

public class BeaconListener implements BeaconManager.RangingListener
{
	private static final int TIMEOUT = 60 * 1000; //in ms

	private long                              lastDataTimestamp;
	private Map<String, Double>               beaconDistances;
	private EstimoteBeacon.IdentificationMode idMode;

	public BeaconListener()
	{
		reset();
	}

	public void reset()
	{
		lastDataTimestamp = 0;

		beaconDistances = new HashMap<>();
	}

	@Override
	public void onBeaconsDiscovered(Region region, List<Beacon> list)
	{
		dataReceived();

		for (Beacon beacon : list)
		{
			beaconDistances.put(getIdentifier(beacon), calculateDistance(beacon));
		}
	}

	private String getIdentifier(Beacon beacon)
	{
		String key = beacon.getMacAddress().toStandardString();

		if (idMode == EstimoteBeacon.IdentificationMode.UUID_MAJOR_MINOR)
		{
			key = beacon.getProximityUUID() + ":" + beacon.getMajor() + ":" + beacon.getMinor();
		}

		return key;
	}

	public double getDistance(String beaconIdentifier)
	{
		double distance = -1;

		if (beaconDistances.containsKey(beaconIdentifier.toLowerCase()))
		{
			distance = beaconDistances.get(beaconIdentifier.toLowerCase());
		}

		return distance;
	}

	private double calculateDistance(Beacon beacon)
	{
		double calculatedDistance = Math.pow(10d, ((double) beacon.getMeasuredPower() - beacon.getRssi()) / (10 * 4));
		double estimoteDistance = Utils.computeAccuracy(beacon);

		return (calculatedDistance + estimoteDistance) / 2.0;
	}

	private void dataReceived()
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

	public void setIdMode(EstimoteBeacon.IdentificationMode idMode)
	{
		this.idMode = idMode;
	}
}
