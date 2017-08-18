/*
 * PipelineRunner.java
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

package hcm.demo;

import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceView;

import hcm.ssj.camera.CameraChannel;
import hcm.ssj.camera.CameraPainter;
import hcm.ssj.camera.CameraSensor;
import hcm.ssj.camera.ImageNormalizer;
import hcm.ssj.camera.ImageResizer;
import hcm.ssj.camera.NV21ToRGBDecoder;
import hcm.ssj.core.ExceptionHandler;
import hcm.ssj.core.Pipeline;
import hcm.ssj.ml.Classifier;

public class PipelineRunner extends Thread {

    private boolean _terminate = false;
    private Pipeline _ssj;

    private MainActivity _act = null;


    public PipelineRunner(MainActivity a)
    {
        _act = a;

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

            String trainerName = "inception.trainer";
            String trainerURL = "https://raw.githubusercontent.com/hcmlab/ssj/master/models";

            final int PREVIEW_WIDTH = 640;
            final int PREVIEW_HEIGHT = 480;
            final int IMAGE_MEAN = 117;
            final int CROP_SIZE = 224;
            final int MIN_FPS = 15;
			final int MAX_FPS = 15;

            final float IMAGE_STD = 1;
            final float DELTA = 0;

            final boolean MAINTAIN_ASPECT = true;
            final boolean SCALE_IMAGE = true;
            final boolean SHOW_BEST_MATCH = true;

            // Instantiate camera sensor and set options.
            CameraSensor cameraSensor = new CameraSensor();
            cameraSensor.options.cameraInfo.set(Camera.CameraInfo.CAMERA_FACING_BACK);
            cameraSensor.options.width.set(PREVIEW_WIDTH);
            cameraSensor.options.height.set(PREVIEW_HEIGHT);
            cameraSensor.options.previewFpsRangeMin.set(MIN_FPS);
            cameraSensor.options.previewFpsRangeMax.set(MAX_FPS);

            CameraChannel channelForPainter = new CameraChannel();
            channelForPainter.options.sampleRate.set(15.0);
            _ssj.addSensor(cameraSensor, channelForPainter);

            CameraPainter cameraPainter = new CameraPainter();
			cameraPainter.options.colorFormat.set(CameraPainter.ColorFormat.NV21_UV_SWAPPED);
            cameraPainter.options.scale.set(SCALE_IMAGE);
            cameraPainter.options.showBestMatch.set(SHOW_BEST_MATCH);
            cameraPainter.options.surfaceView.set((SurfaceView) _act.findViewById(R.id.video));
            _ssj.addConsumer(cameraPainter, channelForPainter, 0.1, DELTA);

            // Add sensor to the pipeline.
            CameraChannel channelForClassifier = new CameraChannel();
            channelForClassifier.options.sampleRate.set(1.0);
            _ssj.addSensor(cameraSensor, channelForClassifier);

            // Set up a NV21 decoder.
            NV21ToRGBDecoder decoder = new NV21ToRGBDecoder();
            _ssj.addTransformer(decoder, channelForClassifier, 1, DELTA);

            // Add image resizer to the pipeline.
            ImageResizer resizer = new ImageResizer();
            resizer.options.maintainAspect.set(MAINTAIN_ASPECT);
            resizer.options.size.set(CROP_SIZE);
            _ssj.addTransformer(resizer, decoder, 1, DELTA);

            // Add image pixel value normalizer to the pipeline.
            ImageNormalizer imageNormalizer = new ImageNormalizer();
            imageNormalizer.options.imageMean.set(IMAGE_MEAN);
            imageNormalizer.options.imageStd.set(IMAGE_STD);
            _ssj.addTransformer(imageNormalizer, resizer, 1, DELTA);

            // Add classifier transformer to the pipeline.
            Classifier classifier = new Classifier();
            classifier.options.trainerPath.set(trainerURL);
            classifier.options.trainerFile.set(trainerName);
            classifier.options.merge.set(false);
            _ssj.addConsumer(classifier, imageNormalizer, 1, DELTA);

            // Send label of the best match to the camera painter.
            _ssj.registerEventListener(cameraPainter, classifier);
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
}
