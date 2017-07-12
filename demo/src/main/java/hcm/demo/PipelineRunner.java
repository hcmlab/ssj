/*
 * Pipeline.java
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

package hcm.demo;

import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceView;

import com.jjoe64.graphview.GraphView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import hcm.ssj.audio.AudioChannel;
import hcm.ssj.audio.Microphone;
import hcm.ssj.audio.Pitch;
import hcm.ssj.camera.CameraChannel;
import hcm.ssj.camera.CameraPainter;
import hcm.ssj.camera.CameraSensor;
import hcm.ssj.camera.ImageNormalizer;
import hcm.ssj.camera.ImageResizer;
import hcm.ssj.camera.NV21ToRGBDecoder;
import hcm.ssj.core.ExceptionHandler;
import hcm.ssj.core.Pipeline;
import hcm.ssj.core.Provider;
import hcm.ssj.graphic.SignalPainter;
import hcm.ssj.ml.Classifier;

public class PipelineRunner extends Thread {

    private boolean _terminate = false;
    private Pipeline _ssj;

    private MainActivity _act = null;
    private GraphView _graphs[] = null;

    public PipelineRunner(MainActivity a, GraphView[] graphs)
    {
        _act = a;
        _graphs = graphs;

        if(Pipeline.isInstanced())
            Pipeline.getInstance().clear();
        _ssj = Pipeline.getInstance();
    }

    public void setExceptionHandler(ExceptionHandler h)
    {
        if(_ssj == null)
            return;

        _ssj.setExceptionHandler(h);
    }

    public void run()
    {
        try {
            _ssj.options.bufferSize.set(10.0f);
            _ssj.options.countdown.set(1);
            _ssj.options.log.set(true);

			//File dir = getContext().getFilesDir();
			File dir = _act.getFilesDir();

			// Neural network trainer file for classifying images
			String modelName = "inception_model.trainer";

			// Option parameters for camera sensor
			double sampleRate = 1;
			int width = 320 * 2;
			int height = 240 * 2;

			final int IMAGE_MEAN = 117;
			final float IMAGE_STD = 1;
			final int CROP_SIZE = 224;
			final boolean MAINTAIN_ASPECT = true;

			// Load inception model and trainer file
			copyAssetToFile(modelName, new File(dir, modelName));
			copyAssetToFile(modelName + ".model", new File(dir, modelName + ".model"));

			// Get pipeline instance
			Pipeline frame = Pipeline.getInstance();
			frame.options.bufferSize.set(10.0f);

			// Instantiate camera sensor and set options
			CameraSensor cameraSensor = new CameraSensor();
			cameraSensor.options.cameraInfo.set(Camera.CameraInfo.CAMERA_FACING_BACK);
			cameraSensor.options.width.set(width);
			cameraSensor.options.height.set(height);
			cameraSensor.options.previewFpsRangeMin.set(15);
			cameraSensor.options.previewFpsRangeMax.set(15);

			// Add sensor to the pipeline
			CameraChannel classifierChannel = new CameraChannel();
			classifierChannel.options.sampleRate.set(1.0);
			frame.addSensor(cameraSensor, classifierChannel);

			CameraChannel cameraChannel = new CameraChannel();
			cameraChannel.options.sampleRate.set(15.0);
			frame.addSensor(cameraSensor, cameraChannel);

			// Set up a NV21 decoder
			NV21ToRGBDecoder decoder = new NV21ToRGBDecoder();
			frame.addTransformer(decoder, classifierChannel, 1, 0);

			// Add image resizer to the pipeline
			ImageResizer resizer = new ImageResizer();
			resizer.options.maintainAspect.set(MAINTAIN_ASPECT);
			resizer.options.cropSize.set(CROP_SIZE);
			frame.addTransformer(resizer, decoder, 1, 0);

			// Add image pixel value normalizer to the pipeline
			ImageNormalizer imageNormalizer = new ImageNormalizer();
			imageNormalizer.options.imageMean.set(IMAGE_MEAN);
			imageNormalizer.options.imageStd.set(IMAGE_STD);
			frame.addTransformer(imageNormalizer, resizer, 1, 0);

			// Add classifier transformer to the pipeline
			Classifier classifier = new Classifier();
			classifier.options.trainerPath.set(dir.getAbsolutePath());
			classifier.options.trainerFile.set(modelName);
			classifier.options.merge.set(false);
			classifier.options.showLabel.set(true);
			frame.addConsumer(classifier, imageNormalizer, 1.0 / sampleRate, 0);

			CameraPainter painter = new CameraPainter();
			painter.options.surfaceView.set((SurfaceView) _act.findViewById(R.id.video));
			painter.options.colorFormat.set(CameraPainter.ColorFormat.NV21_UV_SWAPPED);
			painter.options.scale.set(true);
			painter.options.width.set(width);
			painter.options.height.set(height);
			_ssj.addConsumer(painter, cameraChannel, 0.1, 0);

			_ssj.registerEventListener(painter, classifier);

        }
        catch(Exception e)
        {
            Log.e("SSJ_Demo", "error building pipe", e);
            return;
        }

        Log.i("SSJ_Demo", "starting pipeline");
        _ssj.start();
        _act.notifyPipeState(true);

        while(!_terminate)
        {
            try
            {
                synchronized(this)
                {
                    this.wait();
                }
            }
            catch (InterruptedException e)
            {
                Log.e("pipeline", "Error", e);
            }
        }

        Log.i("SSJ_Demo", "stopping pipeline");
        _ssj.stop();
        _ssj.clear();
        _act.notifyPipeState(false);
    }

    public void terminate()
    {
        _terminate = true;

        synchronized(this)
        {
            this.notify();
        }
    }

    public boolean isRunning()
    {
        return _ssj.isRunning();
    }

	public void copyAssetToFile(String assetName, File dst) throws IOException
	{
		InputStream in = _act.getAssets().open(assetName);
		OutputStream out = new FileOutputStream(dst);

		byte[] buffer = new byte[1024];
		int read;
		while ((read = in.read(buffer)) != -1)
		{
			out.write(buffer, 0, read);
		}

		in.close();
		out.flush();
		out.close();
	}

}
