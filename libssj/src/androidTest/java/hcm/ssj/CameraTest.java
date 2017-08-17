/*
 * CameraTest.java
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

package hcm.ssj;

import android.hardware.Camera;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.view.SurfaceView;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import hcm.ssj.camera.CameraChannel;
import hcm.ssj.camera.CameraPainter;
import hcm.ssj.camera.CameraSensor;
import hcm.ssj.camera.CameraWriter;
import hcm.ssj.core.Pipeline;

import static android.support.test.InstrumentationRegistry.getInstrumentation;

/**
 * Tests all camera sensor, channel and consumer.<br>
 * Created by Frank Gaibler on 28.01.2016.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class CameraTest
{
    /**
     * Test types
     */
    private enum Type
    {
        WRITER, PAINTER
    }

    @Test
    public void testCameraWriter() throws Throwable
    {
        buildPipe(Type.WRITER);
    }

    @Test
    public void testCameraPainter() throws Throwable
    {
        buildPipe(Type.PAINTER);
    }

    private void buildPipe(Type type) throws Exception
    {
        //small values because of memory usage
        int frameRate = 10;
        int width = 176;
        int height = 144;
        //resources
        File file = null;
        //setup
        Pipeline frame = Pipeline.getInstance();
        frame.options.bufferSize.set(10.0f);
        //sensor
        CameraSensor cameraSensor = new CameraSensor();
        cameraSensor.options.cameraInfo.set(Camera.CameraInfo.CAMERA_FACING_BACK);
        cameraSensor.options.width.set(width);
        cameraSensor.options.height.set(height);
        cameraSensor.options.previewFpsRangeMin.set(4 * 1000);
        cameraSensor.options.previewFpsRangeMax.set(16 * 1000);

        //channel
        CameraChannel cameraChannel = new CameraChannel();
        cameraChannel.options.sampleRate.set((double) frameRate);
        frame.addSensor(cameraSensor,cameraChannel);

        //consumer
        switch (type)
        {
            case WRITER:
            {
                //file
                File dir = getInstrumentation().getContext().getFilesDir();
                String fileName = getClass().getSimpleName() + "." + getClass().getSimpleName();
                //
                CameraWriter cameraWriter = new CameraWriter();
                cameraWriter.options.filePath.set(dir.getPath());
                cameraWriter.options.fileName.set(fileName);
                cameraWriter.options.width.set(width);
                cameraWriter.options.height.set(height);
                frame.addConsumer(cameraWriter, cameraChannel, 1.0 / frameRate, 0);
                break;
            }
            case PAINTER:
            {
                CameraPainter cameraPainter = new CameraPainter();
                cameraPainter.options.width.set(width);
                cameraPainter.options.height.set(height);
                cameraPainter.options.colorFormat.set(CameraPainter.ColorFormat.NV21_UV_SWAPPED);
                cameraPainter.options.surfaceView.set(new SurfaceView(getInstrumentation().getContext()));
                frame.addConsumer(cameraPainter, cameraChannel, 1 / frameRate, 0);
                break;
            }
        }
        //start framework
        frame.start();
        //run test
        long end = System.currentTimeMillis() + TestHelper.DUR_TEST_NORMAL;
        try
        {
            while (System.currentTimeMillis() < end)
            {
                Thread.sleep(1);
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        frame.stop();
        frame.release();

        //cleanup
        switch (type)
        {
            case WRITER:
            {
                Assert.assertTrue(file.length() > 1000);

                if (file.exists())
                {
                    if (!file.delete())
                    {
                        throw new RuntimeException("File could not be deleted");
                    }
                }
                break;
            }
        }
    }
}
