/*
 * MyoListener.java
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

package hcm.ssj.myo;

import com.thalmic.myo.Arm;
import com.thalmic.myo.DeviceListener;
import com.thalmic.myo.Myo;
import com.thalmic.myo.Pose;
import com.thalmic.myo.Quaternion;
import com.thalmic.myo.Vector3;
import com.thalmic.myo.XDirection;

import java.util.Arrays;
import java.util.UUID;

import hcm.ssj.core.Log;

/**
 * Created by Michael Dietz on 01.04.2015.
 */
public class MyoListener implements DeviceListener
{
	boolean onArm;
	boolean isUnlocked;

	float accelerationX;
	float accelerationY;
	float accelerationZ;

    double orientationX;
    double orientationY;
    double orientationZ;
    double orientationW;

    int rollW;
    int yawW;
    int pitchW;

	Pose currentPose;
	Arm  whichArm;

	int emg[] = new int[16];

	private static final String TAG = "SSJ_sensor_MyoListener";

	public MyoListener()
	{
		reset();
	}

	public void reset()
	{
		onArm = false;
		whichArm = Arm.UNKNOWN;
		currentPose = Pose.UNKNOWN;

		accelerationX = 0;
		accelerationY = 0;
		accelerationZ = 0;

        orientationX = 0;
        orientationY = 0;
        orientationZ = 0;
        orientationW = 0;

        rollW = 0;
        yawW = 0;
        pitchW = 0;

		Arrays.fill(emg, 0);
	}

	@Override
	public void onAttach(Myo myo, long l)
	{
	}

	@Override
	public void onDetach(Myo myo, long l)
	{
		reset();
	}

	@Override
	public void onConnect(Myo myo, long l)
	{
		Log.i("Successfully connected to Myo with MAC: " + myo.getMacAddress());
	}

	@Override
	public void onDisconnect(Myo myo, long l)
	{
		Log.i("Disconnected from Myo");

		reset();
	}

	@Override
	public void onArmSync(Myo myo, long l, Arm arm, XDirection xDirection)
	{
		onArm = true;
		whichArm = arm;
	}

	@Override
	public void onArmUnsync(Myo myo, long l)
	{
		onArm = false;
	}

	@Override
	public void onUnlock(Myo myo, long l)
	{
		isUnlocked = true;
	}

	@Override
	public void onLock(Myo myo, long l)
	{
		isUnlocked = false;
	}

	@Override
	public void onPose(Myo myo, long l, Pose pose)
	{
		currentPose = pose;
	}

	@Override
	public void onOrientationData(Myo myo, long l, Quaternion quaternion)
	{
        orientationW = quaternion.w();
        orientationX = quaternion.x();
        orientationY = quaternion.y();
        orientationZ = quaternion.z();

		float roll = (float) Math.atan2(2.0 * (quaternion.w() * quaternion.x() + quaternion.y() * quaternion.z()), 1.0 - 2.0 * (quaternion.x() * quaternion.x() + quaternion.y() * quaternion.y()));
		float pitch = (float) Math.asin(Math.max(-1.0, Math.min(1.0, 2.0 * (quaternion.w() * quaternion.y() - quaternion.z() * quaternion.x()))));
		float yaw = (float) Math.atan2(2.0 * (quaternion.w() * quaternion.z() + quaternion.x() * quaternion.y()), 1.0 - 2.0 * (quaternion.y() * quaternion.y() + quaternion.z() * quaternion.z()));

		// Convert the floating point angles in radians to a scale from 0 to 18.
		rollW = (int) ((roll + (float) Math.PI) / (Math.PI * 2.0) * 18);
		pitchW = (int) ((pitch + (float) Math.PI / 2.0) / Math.PI * 18);
		yawW = (int) ((yaw + (float) Math.PI) / (Math.PI * 2.0) * 18);
	}

	@Override
	public void onAccelerometerData(Myo myo, long l, Vector3 vector3)
	{
		accelerationX = (float) vector3.x();
		accelerationY = (float) vector3.y();
		accelerationZ = (float) vector3.z();
	}

	@Override
	public void onGyroscopeData(Myo myo, long l, Vector3 vector3)
	{

	}

	@Override
	public void onRssi(Myo myo, long l, int i)
	{

	}

	public void onEMGData(Myo myo, UUID uuid, byte[] data)
	{
        for (int i = 0; i < 16; i++) {
            emg[i] = data[i];
        }
	}
}
