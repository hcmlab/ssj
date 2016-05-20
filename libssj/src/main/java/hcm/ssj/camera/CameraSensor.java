/*
 * CameraSensor.java
 * Copyright (c) 2016
 * Authors: Ionut Damian, Michael Dietz, Frank Gaibler, Daniel Langerenken
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

package hcm.ssj.camera;

import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;

import java.io.IOException;
import java.util.List;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;

/**
 * Camera sensor.<br>
 * <b>Hint:</b> Heap size problems can be addressed by setting <code>android:largeHeap="true"</code> in the manifest.<br>
 * Created by Frank Gaibler on 21.12.2015.
 */
@SuppressWarnings("deprecation")
public class CameraSensor extends hcm.ssj.core.Sensor implements Camera.PreviewCallback
{
    /**
     * All options for the camera
     */
    public class Options extends OptionList
    {
        public final Option<Integer> cameraInfo = new Option<>("cameraInfo", Camera.CameraInfo.CAMERA_FACING_FRONT, Cons.Type.INT, "chosen camera device");
        //arbitrary but popular values
        public final Option<Integer> width = new Option<>("width", 640, Cons.Type.INT, "width in pixel");
        public final Option<Integer> height = new Option<>("height", 480, Cons.Type.INT, "height in pixel");
        public final Option<Integer> previewFpsRangeMin = new Option<>("previewFpsRangeMin", 30 * 1000, Cons.Type.INT, "min preview rate for camera");
        public final Option<Integer> previewFpsRangeMax = new Option<>("previewFpsRangeMax", 30 * 1000, Cons.Type.INT, "max preview rate for camera");
        public final Option<Integer> imageFormat = new Option<>("imageFormat", ImageFormat.NV21, Cons.Type.INT, "image format for camera");
        public final Option<Boolean> showSupportedValues = new Option<>("showSupportedValues", false, Cons.Type.BOOL, "show supported values in log");

        /**
         *
         */
        private Options()
        {
            add(cameraInfo);
            add(width);
            add(height);
            add(previewFpsRangeMin);
            add(previewFpsRangeMax);
            add(imageFormat);
            add(showSupportedValues);
        }
    }

    //options
    public final Options options = new Options();
    //camera
    private Camera camera;
    //surface
    private SurfaceTexture surfaceTexture = null;
    //camera supported values
    private int iRealWidth = 0;
    private int iRealHeight = 0;
    //buffer to exchange data
    private byte[] byaSwapBuffer = null;

    /**
     *
     */
    public CameraSensor()
    {
        _name = "SSJ_sensor_" + this.getClass().getSimpleName();
    }

    /**
     * @return int
     */
    protected final int getBufferSize()
    {
        int reqBuffSize = iRealWidth * iRealHeight;
        reqBuffSize += reqBuffSize >> 1;
        return reqBuffSize;
    }

    /**
     * Exchanges data between byte arrays
     *
     * @param bytes byte[]
     * @param write boolean
     */
    public final synchronized void swapBuffer(byte[] bytes, boolean write)
    {
        if (write)
        {
            //write into buffer
            if (byaSwapBuffer.length < bytes.length)
            {
                Log.e("Buffer write changed from " + byaSwapBuffer.length + " to " + bytes.length);
                byaSwapBuffer = new byte[bytes.length];
            }
            System.arraycopy(bytes, 0, byaSwapBuffer, 0, bytes.length);
        } else
        {
            //get data from buffer
            if (bytes.length < byaSwapBuffer.length)
            {
                Log.e("Buffer read changed from " + bytes.length + " to " + byaSwapBuffer.length);
                bytes = new byte[byaSwapBuffer.length];
            }
            System.arraycopy(byaSwapBuffer, 0, bytes, 0, byaSwapBuffer.length);
        }
    }

    /**
     * Configures camera for video capture. <br>
     * Opens a camera and sets parameters. Does not start preview.
     */
    private void prepareCamera()
    {
        //set camera and frame size
        Camera.Parameters parameters = prePrepare();
        if (options.showSupportedValues.getValue())
        {
            //list sizes
            List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
            Log.i("Preview size: (n=" + sizes.size() + ")");
            for (Camera.Size size : sizes)
            {
                Log.i("Preview size: " + size.width + "x" + size.height);
            }
            //list preview formats
            List<Integer> formats = parameters.getSupportedPreviewFormats();
            Log.i("Preview format (n=" + formats.size() + ")");
            for (Integer i : formats)
            {
                Log.i("Preview format: " + i);
            }
        }
        //set preview format
        parameters.setPreviewFormat(options.imageFormat.getValue());
        //set preview fps range
        choosePreviewFpsRange(parameters, options.previewFpsRangeMin.getValue(), options.previewFpsRangeMax.getValue());
        //optimizations for more fps
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
        {
            parameters.setRecordingHint(true);
        }
        List<String> FocusModes = parameters.getSupportedFocusModes();
        if (FocusModes != null && FocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
        {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        }
        //set camera parameters
        camera.setParameters(parameters);
        //display used parameters
        Camera.Size size = parameters.getPreviewSize();
        Log.d("Camera preview size is " + size.width + "x" + size.height);
        int[] range = new int[2];
        parameters.getPreviewFpsRange(range);
        Log.d("Preview fps range is " + (range[0] / 1000) + " to " + (range[1] / 1000));
    }

    /**
     * Sets camera height and width.<br>
     * Will select different parameters, if the ones in options aren't supported.
     *
     * @return Camera.Parameters
     */
    protected final Camera.Parameters prePrepare()
    {
        if (camera != null)
        {
            Log.e("Camera already initialized");
            throw new RuntimeException("Camera already initialized");
        }
        //set camera
        chooseCamera();
        //
        Camera.Parameters parameters = camera.getParameters();
        //set preview size
        choosePreviewSize(parameters, options.width.getValue(), options.height.getValue());
        return parameters;
    }

    /**
     * Tries to access the requested camera.<br>
     * Will select a different one if the requested one is not supported.
     */
    private void chooseCamera()
    {
        Camera.CameraInfo info = new Camera.CameraInfo();
        //search for specified camera
        int numCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numCameras; i++)
        {
            Camera.getCameraInfo(i, info);
            if (info.facing == options.cameraInfo.getValue())
            {
                camera = Camera.open(i);
                break;
            }
        }
        if (camera == null)
        {
            Log.d("No front-facing camera found; opening default");
            camera = Camera.open(); //opens first back-facing camera
        }
        if (camera == null)
        {
            Log.e("Unable to open camera");
            throw new RuntimeException("Unable to open camera");
        }
    }

    /**
     * Attempts to find a preview size that matches the provided width and height (which
     * specify the dimensions of the encoded video). If it fails to find a match it just
     * uses the default preview size.
     */
    private void choosePreviewSize(Camera.Parameters parameters, int width, int height)
    {
        //search for requested size
        for (Camera.Size size : parameters.getSupportedPreviewSizes())
        {
            if (size.width == width && size.height == height)
            {
                parameters.setPreviewSize(width, height);
                iRealWidth = width;
                iRealHeight = height;
                return;
            }
        }
        Log.w("Unable to set preview size to " + width + "x" + height);
        //set to preferred size
        Camera.Size ppsfv = parameters.getPreferredPreviewSizeForVideo();
        if (ppsfv != null)
        {
            parameters.setPreviewSize(ppsfv.width, ppsfv.height);
            iRealWidth = ppsfv.width;
            iRealHeight = ppsfv.height;
        }
    }

    /**
     * Attempts to find a preview range that matches the provided min and max.
     * If it fails to find a match it uses the closest match or the default preview range.
     */
    private void choosePreviewFpsRange(Camera.Parameters parameters, int min, int max)
    {
        //adjust wrong preview range
        if (min > max)
        {
            Log.w("Preview range max is too small");
            max = min;
        }
        //preview ranges have to be a multiple of 1000
        if (min / 1000 <= 0)
        {
            min *= 1000;
        }
        if (max / 1000 <= 0)
        {
            max *= 1000;
        }
        //search for requested size
        List<int[]> ranges = parameters.getSupportedPreviewFpsRange();
        if (options.showSupportedValues.getValue())
        {
            Log.i("Preview fps range: (n=" + ranges.size() + ")");
            for (int[] range : ranges)
            {
                Log.i("Preview fps range: " + range[0] + "-" + range[1]);
            }
        }
        for (int[] range : ranges)
        {
            if (range[0] == min && range[1] == max)
            {
                parameters.setPreviewFpsRange(range[0], range[1]);
                return;
            }
        }
        Log.w("Unable to set preview fps range from " + (min / 1000) + " to " + (max / 1000));
        //try to set to minimum
        for (int[] range : ranges)
        {
            if (range[0] == min)
            {
                parameters.setPreviewFpsRange(range[0], range[1]);
                return;
            }
        }
        //leave preview range at default
    }

    /**
     * Stops camera preview, and releases the camera to the system.
     */
    protected final void releaseCamera()
    {
        if (camera != null)
        {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    /**
     * Configures surface texture for camera preview
     */
    private void prepareSurfaceTexture()
    {
        surfaceTexture = new SurfaceTexture(10);
        try
        {
            camera.setPreviewTexture(surfaceTexture);
        } catch (IOException ex)
        {
            Log.e("Couldn't prepare surface texture: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Set buffer size according to real width and height
     */
    private void initBuffer()
    {
        try
        {
            int reqBuffSize = getBufferSize();
            camera.addCallbackBuffer(new byte[reqBuffSize]);
            camera.setPreviewCallbackWithBuffer(this);
            byaSwapBuffer = new byte[reqBuffSize];
        } catch (Exception ex)
        {
            Log.e("Couldn't init buffer: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Write data into buffer and return used resources to camera
     *
     * @param data byte[]
     * @param cam  Camera
     */
    @Override
    public void onPreviewFrame(byte[] data, Camera cam)
    {
        swapBuffer(data, true);
        camera.addCallbackBuffer(data);
    }

    /**
     * Release the surface texture
     */
    private void releaseSurfaceTexture()
    {
        if (surfaceTexture != null)
        {
            surfaceTexture.release();
            surfaceTexture = null;
        }
    }

    /**
     *
     */
    @Override
    protected boolean connect()
    {
        prepareCamera();
        prepareSurfaceTexture();
        initBuffer();
        camera.startPreview();
        return true;
    }

    /**
     *
     */
    @Override
    protected void disconnect()
    {
        releaseCamera();
        releaseSurfaceTexture();
    }
}

