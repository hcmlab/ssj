/*
 * BandListener.java
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

package hcm.ssj.msband;

import com.microsoft.band.sensors.BandAccelerometerEvent;
import com.microsoft.band.sensors.BandAccelerometerEventListener;
import com.microsoft.band.sensors.BandAltimeterEvent;
import com.microsoft.band.sensors.BandAltimeterEventListener;
import com.microsoft.band.sensors.BandAmbientLightEvent;
import com.microsoft.band.sensors.BandAmbientLightEventListener;
import com.microsoft.band.sensors.BandBarometerEvent;
import com.microsoft.band.sensors.BandBarometerEventListener;
import com.microsoft.band.sensors.BandCaloriesEvent;
import com.microsoft.band.sensors.BandCaloriesEventListener;
import com.microsoft.band.sensors.BandDistanceEvent;
import com.microsoft.band.sensors.BandDistanceEventListener;
import com.microsoft.band.sensors.BandGsrEvent;
import com.microsoft.band.sensors.BandGsrEventListener;
import com.microsoft.band.sensors.BandGyroscopeEvent;
import com.microsoft.band.sensors.BandGyroscopeEventListener;
import com.microsoft.band.sensors.BandHeartRateEvent;
import com.microsoft.band.sensors.BandHeartRateEventListener;
import com.microsoft.band.sensors.BandPedometerEvent;
import com.microsoft.band.sensors.BandPedometerEventListener;
import com.microsoft.band.sensors.BandRRIntervalEvent;
import com.microsoft.band.sensors.BandRRIntervalEventListener;
import com.microsoft.band.sensors.BandSkinTemperatureEvent;
import com.microsoft.band.sensors.BandSkinTemperatureEventListener;

/**
 * Created by Michael Dietz on 06.07.2016.
 */
public class BandListener implements BandGsrEventListener, BandSkinTemperatureEventListener, BandHeartRateEventListener, BandAccelerometerEventListener, BandGyroscopeEventListener, BandAmbientLightEventListener, BandBarometerEventListener, BandCaloriesEventListener, BandDistanceEventListener, BandPedometerEventListener, BandRRIntervalEventListener, BandAltimeterEventListener
{
	private int   gsr; // Resistance in ohm
	private float skinTemperature;
	private int   heartRate;
	private float accelerationX;
	private float accelerationY;
	private float accelerationZ;
	private float angularVelocityX;
	private float angularVelocityY;
	private float angularVelocityZ;
	private int brightness;
	private double airPressure;
	private double temperature;
	private long calories;

	public boolean receivedData;

	public BandListener()
	{
		reset();
	}

	public void reset()
	{
		gsr = 0;
		skinTemperature = 0;
		heartRate = 0;
		accelerationX = 0;
		accelerationY = 0;
		accelerationZ = 0;
		angularVelocityX = 0;
		angularVelocityY = 0;
		angularVelocityZ = 0;
		brightness = 0;
		airPressure = 0;
		temperature = 0;
		calories = 0;

		receivedData = false;
	}

	@Override
	public void onBandGsrChanged(BandGsrEvent bandGsrEvent)
	{
		receivedData = true;
		gsr = bandGsrEvent.getResistance();
	}

	@Override
	public void onBandSkinTemperatureChanged(BandSkinTemperatureEvent bandSkinTemperatureEvent)
	{
		receivedData = true;
		skinTemperature = bandSkinTemperatureEvent.getTemperature();
	}

	@Override
	public void onBandHeartRateChanged(BandHeartRateEvent bandHeartRateEvent)
	{
		receivedData = true;
		heartRate = bandHeartRateEvent.getHeartRate();
	}

	@Override
	public void onBandAccelerometerChanged(BandAccelerometerEvent bandAccelerometerEvent)
	{
		receivedData = true;
		accelerationX = bandAccelerometerEvent.getAccelerationX();
		accelerationY = bandAccelerometerEvent.getAccelerationY();
		accelerationZ = bandAccelerometerEvent.getAccelerationZ();
	}

	@Override
	public void onBandGyroscopeChanged(BandGyroscopeEvent bandGyroscopeEvent)
	{
		receivedData = true;
		angularVelocityX = bandGyroscopeEvent.getAngularVelocityX();
		angularVelocityY = bandGyroscopeEvent.getAngularVelocityY();
		angularVelocityZ = bandGyroscopeEvent.getAngularVelocityZ();

		// accelerationX = bandGyroscopeEvent.getAccelerationX();
		// accelerationY = bandGyroscopeEvent.getAccelerationY();
		// accelerationZ = bandGyroscopeEvent.getAccelerationZ();
	}

	@Override
	public void onBandAmbientLightChanged(BandAmbientLightEvent bandAmbientLightEvent)
	{
		receivedData = true;
		brightness = bandAmbientLightEvent.getBrightness();
	}

	@Override
	public void onBandBarometerChanged(BandBarometerEvent bandBarometerEvent)
	{
		receivedData = true;
		airPressure = bandBarometerEvent.getAirPressure();
		temperature = bandBarometerEvent.getTemperature();
	}

	@Override
	public void onBandCaloriesChanged(BandCaloriesEvent bandCaloriesEvent)
	{
		receivedData = true;
		calories = bandCaloriesEvent.getCalories();
	}

	@Override
	public void onBandDistanceChanged(BandDistanceEvent bandDistanceEvent)
	{
		bandDistanceEvent.getTotalDistance();
		bandDistanceEvent.getSpeed();
		bandDistanceEvent.getPace();
		bandDistanceEvent.getMotionType();
	}

	@Override
	public void onBandPedometerChanged(BandPedometerEvent bandPedometerEvent)
	{
		bandPedometerEvent.getTotalSteps();
	}

	@Override
	public void onBandRRIntervalChanged(BandRRIntervalEvent bandRRIntervalEvent)
	{
		bandRRIntervalEvent.getInterval();
	}

	@Override
	public void onBandAltimeterChanged(BandAltimeterEvent bandAltimeterEvent)
	{

	}

	public int getGsr()
	{
		return gsr;
	}

	public float getSkinTemperature()
	{
		return skinTemperature;
	}

	public int getHeartRate()
	{
		return heartRate;
	}

	public float getAccelerationX()
	{
		return accelerationX;
	}

	public float getAccelerationY()
	{
		return accelerationY;
	}

	public float getAccelerationZ()
	{
		return accelerationZ;
	}

	public float getAngularVelocityX()
	{
		return angularVelocityX;
	}

	public float getAngularVelocityY()
	{
		return angularVelocityY;
	}

	public float getAngularVelocityZ()
	{
		return angularVelocityZ;
	}

	public int getBrightness()
	{
		return brightness;
	}

	public double getAirPressure()
	{
		return airPressure;
	}

	public double getTemperature()
	{
		return temperature;
	}

	public long getCalories()
	{
		return calories;
	}
}
