/*
 * PolarListener.java
 * Copyright (c) 2021
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

package hcm.ssj.polar;

import org.reactivestreams.Publisher;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import androidx.annotation.NonNull;
import hcm.ssj.core.Log;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Function;
import polar.com.sdk.api.PolarBleApi;
import polar.com.sdk.api.PolarBleApiCallback;
import polar.com.sdk.api.model.PolarAccelerometerData;
import polar.com.sdk.api.model.PolarDeviceInfo;
import polar.com.sdk.api.model.PolarEcgData;
import polar.com.sdk.api.model.PolarGyroData;
import polar.com.sdk.api.model.PolarHrData;
import polar.com.sdk.api.model.PolarMagnetometerData;
import polar.com.sdk.api.model.PolarOhrData;
import polar.com.sdk.api.model.PolarSensorSetting;

import static polar.com.sdk.api.model.PolarOhrData.OHR_DATA_TYPE.PPG3_AMBIENT1;

/**
 * Created by Michael Dietz on 08.04.2021.
 */
public class PolarListener extends PolarBleApiCallback
{
	public static final int ACC_SR_OFFSET = 1;

	final String DEVICE_ID;
	final PolarBleApi api;

	boolean connected;

	Set<PolarBleApi.DeviceStreamingFeature> streamingFeatures;
	List<Disposable> disposables;

	int sampleRatePPG;
	int sampleRateACC;
	int sampleRateECG;
	int sampleRateGYR;
	int sampleRateMAG;

	ArrayDeque<PolarOhrData.PolarOhrSample> ppgQueue;
	ArrayDeque<PolarAccelerometerData.PolarAccelerometerDataSample> accQueue;
	ArrayDeque<Integer> ecgQueue;
	ArrayDeque<PolarGyroData.PolarGyroDataSample> gyrQueue;
	ArrayDeque<PolarMagnetometerData.PolarMagnetometerDataSample> magQueue;

	PolarHrData hrData;
	boolean hrReady;

	public PolarListener(PolarBleApi polarApi, String deviceIdentifier)
	{
		DEVICE_ID = deviceIdentifier;
		api = polarApi;

		streamingFeatures = new LinkedHashSet<>();
		disposables = new ArrayList<>();

		ppgQueue = new ArrayDeque<>();
		accQueue = new ArrayDeque<>();
		ecgQueue = new ArrayDeque<>();
		gyrQueue = new ArrayDeque<>();
		magQueue = new ArrayDeque<>();

		reset();
	}

	private void reset()
	{
		connected = false;
		streamingFeatures.clear();
		disposables.clear();

		ppgQueue.clear();
		accQueue.clear();
		ecgQueue.clear();
		gyrQueue.clear();
		magQueue.clear();

		sampleRatePPG = -1;
		sampleRateACC = -1;
		sampleRateECG = -1;
		sampleRateGYR = -1;
		sampleRateMAG = -1;

		hrData = null;
		hrReady = false;
	}

	public void deviceConnected(@NonNull PolarDeviceInfo polarDeviceInfo)
	{
		Log.d("Connected to device: " + polarDeviceInfo.deviceId + " (" + DEVICE_ID + ")");

		connected = true;
	}

	public void deviceConnecting(@NonNull PolarDeviceInfo polarDeviceInfo)
	{
		Log.d("Connecting to device: " + polarDeviceInfo.deviceId + " (" + DEVICE_ID + ")");
	}

	public void deviceDisconnected(@NonNull PolarDeviceInfo polarDeviceInfo)
	{
		Log.d("Disconnected from device: " + polarDeviceInfo.deviceId + " (" + DEVICE_ID + ")");

		for (Disposable disposable : disposables)
		{
			disposable.dispose();
		}

		connected = false;
	}

	public void streamingFeaturesReady(@NonNull String identifier, @NonNull Set<PolarBleApi.DeviceStreamingFeature> features)
	{
		for (PolarBleApi.DeviceStreamingFeature feature : features)
		{
			Log.d("Streaming feature " + feature.toString() + " is ready (" + DEVICE_ID + ")");

			if (streamingFeatures.contains(feature))
			{
				switch (feature)
				{
					case ACC:
						disposables.add(streamACC());
						break;
					case PPG:
						disposables.add(streamPPG());
						break;
					case ECG:
						disposables.add(streamECG());
						break;
					case GYRO:
						disposables.add(streamGYR());
						break;
					case MAGNETOMETER:
						disposables.add(streamMAG());
						break;
					case PPI:
						break;
				}
			}
		}
	}

	public void hrFeatureReady(@NonNull String identifier)
	{
		hrReady = true;
	}

	public void disInformationReceived(@NonNull String identifier, @NonNull UUID uuid, @NonNull String value)
	{
		// Log.d("UUID: " + uuid + " Value: " + value);
		Log.d("Device: " + DEVICE_ID + " Value: " + value);
	}

	public void batteryLevelReceived(@NonNull String identifier, int level)
	{
		Log.d("Device: " + DEVICE_ID + " Battery level: " + level);
	}

	public void hrNotificationReceived(@NonNull String identifier, @NonNull PolarHrData data)
	{
		hrData = data;
	}

	private Disposable streamPPG()
	{
		return api.requestStreamSettings(DEVICE_ID, PolarBleApi.DeviceStreamingFeature.PPG)
				.toFlowable()
				.flatMap((Function<PolarSensorSetting, Publisher<PolarOhrData>>) polarPPGSettings -> {
					PolarSensorSetting sensorSetting = polarPPGSettings.maxSettings();

					Set<Integer> values = sensorSetting.settings.get(PolarSensorSetting.SettingType.SAMPLE_RATE);
					if (values != null && !values.isEmpty())
					{
						sampleRatePPG = values.iterator().next();
					}

					return api.startOhrStreaming(DEVICE_ID, sensorSetting);
				})
				.subscribe(
						polarOhrPPGData -> {
							if (polarOhrPPGData.type == PPG3_AMBIENT1)
							{
								ppgQueue.addAll(polarOhrPPGData.samples);
							}
						},
						throwable -> Log.e("Error with PPG stream: " + throwable),
						() -> Log.d("complete")
				);
	}

	private Disposable streamECG()
	{
		return api.requestStreamSettings(DEVICE_ID, PolarBleApi.DeviceStreamingFeature.ECG)
				.toFlowable()
				.flatMap((Function<PolarSensorSetting, Publisher<PolarEcgData>>) polarEcgSettings -> {
					PolarSensorSetting sensorSetting = polarEcgSettings.maxSettings();

					Set<Integer> values = sensorSetting.settings.get(PolarSensorSetting.SettingType.SAMPLE_RATE);
					if (values != null && !values.isEmpty())
					{
						sampleRateECG = values.iterator().next();
					}

					return api.startEcgStreaming(DEVICE_ID, sensorSetting);
				}).subscribe(
						polarEcgData -> {
							ecgQueue.addAll(polarEcgData.samples);
						},
						throwable -> Log.e("Error with ECG stream: " + throwable),
						() -> Log.d("complete")
				);
	}

	private Disposable streamACC()
	{
		return api.requestStreamSettings(DEVICE_ID, PolarBleApi.DeviceStreamingFeature.ACC)
				.toFlowable()
				.flatMap((Function<PolarSensorSetting, Publisher<PolarAccelerometerData>>) settings -> {
					PolarSensorSetting sensorSetting = settings.maxSettings();

					Set<Integer> values = sensorSetting.settings.get(PolarSensorSetting.SettingType.SAMPLE_RATE);
					if (values != null && !values.isEmpty())
					{
						sampleRateACC = values.iterator().next() + ACC_SR_OFFSET;
					}

					return api.startAccStreaming(DEVICE_ID, sensorSetting);
				})
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(
						polarAccelerometerData -> {
							accQueue.addAll(polarAccelerometerData.samples);
						},
						throwable -> Log.e("Error with ACC stream: " + throwable),
						() -> Log.d("complete")
				);
	}

	private Disposable streamGYR()
	{
		return api.requestStreamSettings(DEVICE_ID, PolarBleApi.DeviceStreamingFeature.GYRO)
				.toFlowable()
				.flatMap((Function<PolarSensorSetting, Publisher<PolarGyroData>>) settings -> {
					PolarSensorSetting sensorSetting = settings.maxSettings();

					Set<Integer> values = sensorSetting.settings.get(PolarSensorSetting.SettingType.SAMPLE_RATE);
					if (values != null && !values.isEmpty())
					{
						sampleRateGYR = values.iterator().next();
					}

					return api.startGyroStreaming(DEVICE_ID, sensorSetting);
				})
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(
						polarGyroData -> {
							gyrQueue.addAll(polarGyroData.samples);
						},
						throwable -> Log.e("Error with GYR stream: " + throwable),
						() -> Log.d("complete")
				);
	}

	private Disposable streamMAG()
	{
		return api.requestStreamSettings(DEVICE_ID, PolarBleApi.DeviceStreamingFeature.MAGNETOMETER)
				.toFlowable()
				.flatMap((Function<PolarSensorSetting, Publisher<PolarMagnetometerData>>) settings -> {
					PolarSensorSetting sensorSetting = settings.maxSettings();

					Set<Integer> values = sensorSetting.settings.get(PolarSensorSetting.SettingType.SAMPLE_RATE);
					if (values != null && !values.isEmpty())
					{
						sampleRateMAG = values.iterator().next();
					}

					return api.startMagnetometerStreaming(DEVICE_ID, sensorSetting);
				})
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(
						polarMagData -> {
							magQueue.addAll(polarMagData.samples);
						},
						throwable -> Log.e("Error with MAG stream: " + throwable),
						() -> Log.d("complete")
				);
	}
}
