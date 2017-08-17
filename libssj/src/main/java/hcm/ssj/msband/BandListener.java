/*
 * BandListener.java
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

package hcm.ssj.msband;

import com.microsoft.band.BandConnectionCallback;
import com.microsoft.band.ConnectionState;
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
import com.microsoft.band.sensors.MotionType;

import hcm.ssj.core.Log;

/**
 * Created by Michael Dietz on 06.07.2016.
 */
public class BandListener implements BandGsrEventListener, BandSkinTemperatureEventListener, BandHeartRateEventListener, BandAccelerometerEventListener, BandGyroscopeEventListener, BandAmbientLightEventListener, BandBarometerEventListener, BandCaloriesEventListener, BandDistanceEventListener, BandPedometerEventListener, BandRRIntervalEventListener, BandAltimeterEventListener, BandConnectionCallback
{
	private final int TIMEOUT = 10000; //in ms

	private int   gsr; // Resistance in ohm
	private float skinTemperature;
	private int   heartRate;
	private float accelerationX;
	private float accelerationY;
	private float accelerationZ;
	private float gyrVelocityX;
	private float gyrVelocityY;
	private float gyrVelocityZ;
	private float gyrAccelerationX;
	private float gyrAccelerationY;
	private float gyrAccelerationZ;
	private int brightness;
	private double airPressure;
	private double temperature;
	private long calories;
	private long distance;
	private float speed;
	private float pace;
	private long steps;
	private int motionType;
	private double interBeatInterval; // time between two heart beats
	private long flightsAscended;
	private long flightsDescended;
	private long steppingGain;
	private long steppingLoss;
	private long stepsAscended;
	private long stepsDescended;
	private long altimeterGain;
	private long altimeterLoss;

	private long lastDataTimestamp = 0;
	private boolean connected;

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
		gyrVelocityX = 0;
		gyrVelocityY = 0;
		gyrVelocityZ = 0;
		gyrAccelerationX = 0;
		gyrAccelerationY = 0;
		gyrAccelerationZ = 0;
		brightness = 0;
		airPressure = 0;
		temperature = 0;
		calories = 0;
		distance = 0;
		speed = 0;
		pace = 0;
		steps = 0;
		interBeatInterval = 0;
		flightsAscended = 0;
		flightsDescended = 0;
		motionType = MotionType.UNKNOWN.ordinal();

		lastDataTimestamp = 0;
		connected = false;
	}

	@Override
	public void onStateChanged(ConnectionState connectionState)
	{
		connected = (connectionState == ConnectionState.CONNECTED);
		Log.i("MSBand connection status: " + connectionState.toString());
	}

	@Override
	public void onBandGsrChanged(BandGsrEvent bandGsrEvent)
	{
		dataReceived();
		gsr = bandGsrEvent.getResistance();
	}

	@Override
	public void onBandSkinTemperatureChanged(BandSkinTemperatureEvent bandSkinTemperatureEvent)
	{
		dataReceived();
		skinTemperature = bandSkinTemperatureEvent.getTemperature();
	}

	@Override
	public void onBandHeartRateChanged(BandHeartRateEvent bandHeartRateEvent)
	{
		dataReceived();
		heartRate = bandHeartRateEvent.getHeartRate();
	}

	@Override
	public void onBandAccelerometerChanged(BandAccelerometerEvent bandAccelerometerEvent)
	{
		dataReceived();
		accelerationX = bandAccelerometerEvent.getAccelerationX();
		accelerationY = bandAccelerometerEvent.getAccelerationY();
		accelerationZ = bandAccelerometerEvent.getAccelerationZ();
	}

	@Override
	public void onBandGyroscopeChanged(BandGyroscopeEvent bandGyroscopeEvent)
	{
		dataReceived();
		gyrVelocityX = bandGyroscopeEvent.getAngularVelocityX();
		gyrVelocityY = bandGyroscopeEvent.getAngularVelocityY();
		gyrVelocityZ = bandGyroscopeEvent.getAngularVelocityZ();
		gyrAccelerationX = bandGyroscopeEvent.getAccelerationX();
		gyrAccelerationY = bandGyroscopeEvent.getAccelerationY();
		gyrAccelerationZ = bandGyroscopeEvent.getAccelerationZ();
	}

	@Override
	public void onBandAmbientLightChanged(BandAmbientLightEvent bandAmbientLightEvent)
	{
		dataReceived();
		brightness = bandAmbientLightEvent.getBrightness();
	}

	@Override
	public void onBandBarometerChanged(BandBarometerEvent bandBarometerEvent)
	{
		dataReceived();
		airPressure = bandBarometerEvent.getAirPressure();
		temperature = bandBarometerEvent.getTemperature();
	}

	@Override
	public void onBandCaloriesChanged(BandCaloriesEvent bandCaloriesEvent)
	{
		dataReceived();
		calories = bandCaloriesEvent.getCalories();
	}

	@Override
	public void onBandDistanceChanged(BandDistanceEvent bandDistanceEvent)
	{
		dataReceived();
		distance = bandDistanceEvent.getTotalDistance();
		speed = bandDistanceEvent.getSpeed();
		pace = bandDistanceEvent.getPace();
		motionType = bandDistanceEvent.getMotionType().ordinal();
	}

	@Override
	public void onBandPedometerChanged(BandPedometerEvent bandPedometerEvent)
	{
		dataReceived();
		steps = bandPedometerEvent.getTotalSteps();
	}

	@Override
	public void onBandRRIntervalChanged(BandRRIntervalEvent bandRRIntervalEvent)
	{
		dataReceived();
		interBeatInterval = bandRRIntervalEvent.getInterval();
	}

	@Override
	public void onBandAltimeterChanged(BandAltimeterEvent bandAltimeterEvent)
	{
		dataReceived();
		flightsAscended = bandAltimeterEvent.getFlightsAscended();
		flightsDescended = bandAltimeterEvent.getFlightsDescended();
		steppingGain = bandAltimeterEvent.getSteppingGain();
		steppingLoss = bandAltimeterEvent.getSteppingLoss();
		stepsAscended = bandAltimeterEvent.getStepsAscended();
		stepsDescended = bandAltimeterEvent.getStepsDescended();
		altimeterGain = bandAltimeterEvent.getTotalGain();
		altimeterLoss = bandAltimeterEvent.getTotalLoss();
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
		return gyrVelocityX;
	}

	public float getAngularVelocityY()
	{
		return gyrVelocityY;
	}

	public float getAngularVelocityZ()
	{
		return gyrVelocityZ;
	}

	public float getAngularAccelerationX()
	{
		return gyrAccelerationX;
	}

	public float getAngularAccelerationY()
	{
		return gyrAccelerationY;
	}

	public float getAngularAccelerationZ()
	{
		return gyrAccelerationZ;
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

	public long getDistance()
	{
		return distance;
	}

	public float getSpeed()
	{
		return speed;
	}

	public float getPace()
	{
		return pace;
	}

	public int getMotionType()
	{
		return motionType;
	}

	public long getSteps()
	{
		return steps;
	}

	public double getInterBeatInterval()
	{
		return interBeatInterval;
	}

	public long getFlightsAscended()
	{
		return flightsAscended;
	}

	public long getFlightsDescended()
	{
		return flightsDescended;
	}

	public long getSteppingGain()
	{
		return steppingGain;
	}

	public long getSteppingLoss()
	{
		return steppingLoss;
	}

	public long getStepsAscended()
	{
		return stepsAscended;
	}

	public long getStepsDescended()
	{
		return stepsDescended;
	}

	public long getAltimeterGain()
	{
		return altimeterGain;
	}

	public long getAltimeterLoss()
	{
		return altimeterLoss;
	}

	private synchronized void dataReceived()
	{
		lastDataTimestamp = System.currentTimeMillis();
	}

	public boolean isConnected()
	{
		return (connected && System.currentTimeMillis() - lastDataTimestamp < TIMEOUT);
	}

	public boolean hasReceivedData()
	{
		return lastDataTimestamp != 0;
	}
}
