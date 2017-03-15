/*
 * AccelerationFeaturesTest.java
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

package hcm.ssj;

import android.app.Application;
import android.test.ApplicationTestCase;
import android.test.suitebuilder.annotation.Suppress;

import hcm.ssj.androidSensor.AndroidSensor;
import hcm.ssj.androidSensor.AndroidSensorChannel;
import hcm.ssj.androidSensor.SensorType;
import hcm.ssj.body.AccelerationFeatures;
import hcm.ssj.core.Pipeline;
import hcm.ssj.core.Provider;
import hcm.ssj.file.FileReader;
import hcm.ssj.file.FileReaderChannel;
import hcm.ssj.file.FileWriter;
import hcm.ssj.ml.Classifier;
import hcm.ssj.test.Logger;

/**
 * Created by Michael Dietz on 19.10.2016.
 */

public class AccelerationFeaturesTest extends ApplicationTestCase<Application>
{
	// Test length in milliseconds
	private final static int TEST_LENGTH = 1 * 60 * 1000;

	public AccelerationFeaturesTest()
	{
		super(Application.class);
	}

	@Suppress
	public void testWriting() throws Exception
	{
		// Setup
		Pipeline frame = Pipeline.getInstance();
		frame.options.bufferSize.set(10.0f);

		// Sensor
		AndroidSensor sensor = new AndroidSensor();
		sensor.options.sensorType.set(SensorType.ACCELEROMETER);

		// Channel
		AndroidSensorChannel channel = new AndroidSensorChannel();
		channel.options.sampleRate.set(40);
		frame.addSensor(sensor, channel);

		// Transformer
		AccelerationFeatures features = new AccelerationFeatures();
		frame.addTransformer(features, channel, 2, 2);

		// Consumer
		Logger log = new Logger();
		frame.addConsumer(log, features, 2, 0);

		FileWriter rawWriter = new FileWriter();
		frame.addConsumer(rawWriter, channel, 1, 0);

		FileWriter featureWriter = new FileWriter();
		featureWriter.options.fileName.set("features");
		frame.addConsumer(featureWriter, features, 2, 0);

		// Start framework
		frame.start();

		// Run test
		long end = System.currentTimeMillis() + TEST_LENGTH;
		try
		{
			while (System.currentTimeMillis() < end)
			{
				Thread.sleep(1);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		// Stop framework
		frame.stop();
		frame.clear();
	}

	@Suppress
	public void testReading() throws Exception
	{
		// Setup
		Pipeline frame = Pipeline.getInstance();
		frame.options.countdown.set(0);
		frame.options.bufferSize.set(10.0f);

		// Sensor
		FileReader sensor = new FileReader();
		sensor.options.filePath.set("/sdcard/SSJ/");
		sensor.options.fileName.set("AccX_AccY_AccZ.stream");
		sensor.options.loop.set(false);

		// Channel
		FileReaderChannel sensorChannel = new FileReaderChannel();
		sensorChannel.setWatchInterval(0);
		frame.addSensor(sensor,sensorChannel);

		// Transformer
		AccelerationFeatures features = new AccelerationFeatures();
		frame.addTransformer(features, sensorChannel, 2, 2);

		// SVM
		//Classifier classifier = new Classifier();
		//classifier.options.trainerPath.set("/sdcard/SSJ/Model/");
		//classifier.options.trainerFile.set("my_model.trainer");
		//frame.addTransformer(classifier, features, 1, 0);

		// Logger
		Logger log = new Logger();
		frame.addConsumer(log, features, 1, 0);

		//FileWriter consumer = new FileWriter();
		//consumer.options.fileName.set("features");
		//frame.addConsumer(consumer, features, 1, 0);

		// Start framework
		frame.start();

		// Run test
		long end = System.currentTimeMillis() + TEST_LENGTH;
		try
		{
			while (System.currentTimeMillis() < end)
			{
				Thread.sleep(1);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		// Stop framework
		frame.stop();
		frame.clear();
	}

	//@Suppress
	public void testSVM() throws Exception
	{
		// Setup
		Pipeline frame = Pipeline.getInstance();
		frame.options.bufferSize.set(10.0f);

		// Sensor
		AndroidSensor accSensor = new AndroidSensor();
		accSensor.options.sensorType.set(SensorType.LINEAR_ACCELERATION);

		AndroidSensor gyrSensor = new AndroidSensor();
		gyrSensor.options.sensorType.set(SensorType.GYROSCOPE);

		// Channel
		AndroidSensorChannel accChannel = new AndroidSensorChannel();
		accChannel.options.sampleRate.set(40);
		frame.addSensor(accSensor, accChannel);

		AndroidSensorChannel gyrChannel = new AndroidSensorChannel();
		gyrChannel.options.sampleRate.set(40);
		frame.addSensor(gyrSensor, gyrChannel);

		// Transformer
		AccelerationFeatures accFeatures = new AccelerationFeatures();
		frame.addTransformer(accFeatures, accChannel, 2, 2);

		AccelerationFeatures gyrFeatures = new AccelerationFeatures();
		frame.addTransformer(gyrFeatures, gyrChannel, 2, 2);

		// SVM
		Classifier classifier = new Classifier();
		classifier.options.trainerPath.set("/sdcard/SSJ/Model/");
		classifier.options.trainerFile.set("search_model_feature_fusion.trainer");
		frame.addTransformer(classifier, new Provider[] {accFeatures, gyrFeatures}, 2, 0);

		// Consumer
		Logger log = new Logger();
		frame.addConsumer(log, classifier, 2, 0);

		FileWriter rawWriter = new FileWriter();
		frame.addConsumer(rawWriter, accChannel, 1, 0);

		FileWriter featureWriter = new FileWriter();
		featureWriter.options.fileName.set("features");
		frame.addConsumer(featureWriter, accFeatures, 2, 0);

		FileWriter svmWriter = new FileWriter();
		svmWriter.options.fileName.set("svm_results");
		frame.addConsumer(svmWriter, classifier, 2, 0);

		// Start framework
		frame.start();

		// Run test
		long end = System.currentTimeMillis() + TEST_LENGTH;
		try
		{
			while (System.currentTimeMillis() < end)
			{
				Thread.sleep(1);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		// Stop framework
		frame.stop();
		frame.clear();
	}
}
