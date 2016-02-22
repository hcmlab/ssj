package hcm.ssj;

import android.app.Application;
import android.hardware.Camera;
import android.test.ApplicationTestCase;
import android.view.SurfaceView;

import java.io.File;

import hcm.ssj.camera.CameraPainter;
import hcm.ssj.camera.CameraProvider;
import hcm.ssj.camera.CameraSensor;
import hcm.ssj.camera.CameraWriter;
import hcm.ssj.core.TheFramework;

/**
 * Tests all camera sensor, provider and consumer.<br>
 * Created by Frank Gaibler on 28.01.2016.
 */
public class CameraSensorTest extends ApplicationTestCase<Application>
{
    //test length in milliseconds
    private final static int TEST_LENGTH = 2 * 60 * 1000;

    /**
     * Test types
     */
    private enum Type
    {
        WRITER, PAINTER
    }

    /**
     *
     */
    public CameraSensorTest()
    {
        super(Application.class);
    }

    /**
     * @throws Throwable
     */
    public void testCameraWriter() throws Throwable
    {
        buildPipe(Type.WRITER);
    }

    /**
     * @throws Throwable
     */
    public void testCameraPainter() throws Throwable
    {
        buildPipe(Type.PAINTER);
    }

    /**
     *
     */
    private void buildPipe(Type type)
    {
        //small values because of memory usage
        int frameRate = 10;
        int width = 176;
        int height = 144;
        //resources
        File file = null;
        //setup
        TheFramework frame = TheFramework.getFramework();
        frame.options.bufferSize = 10.0f;
        //sensor
        CameraSensor cameraSensor = new CameraSensor();
        cameraSensor.options.cameraInfo = Camera.CameraInfo.CAMERA_FACING_BACK;
        cameraSensor.options.width = width;
        cameraSensor.options.height = height;
        cameraSensor.options.previewFpsRangeMin = 4 * 1000;
        cameraSensor.options.previewFpsRangeMax = 16 * 1000;
        frame.addSensor(cameraSensor);
        //provider
        CameraProvider cameraProvider = new CameraProvider();
        cameraProvider.options.sampleRate = frameRate;
        cameraSensor.addProvider(cameraProvider);
        //consumer
        switch (type)
        {
            case WRITER:
            {
                //file
                File dir = getContext().getFilesDir();
                String fileName = getClass().getSimpleName() + "." + getClass().getSimpleName();
                file = new File(dir, fileName);
                //
                CameraWriter cameraWriter = new CameraWriter();
                cameraWriter.options.file = file;
                cameraWriter.options.width = width;
                cameraWriter.options.height = height;
                frame.addConsumer(cameraWriter, cameraProvider, 1.0 / frameRate, 0);
                break;
            }
            case PAINTER:
            {
                CameraPainter cameraPainter = new CameraPainter();
                cameraPainter.options.width = width;
                cameraPainter.options.height = height;
                cameraPainter.options.colorFormat = CameraPainter.ColorFormat.NV21_UV_SWAPPED.value;
                cameraPainter.options.surfaceView = new SurfaceView(this.getContext());
                frame.addConsumer(cameraPainter, cameraProvider, 1 / frameRate, 0);
                break;
            }
        }
        //start framework
        frame.Start();
        //run test
        long end = System.currentTimeMillis() + TEST_LENGTH;
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
        frame.Stop();
        frame.clear();
        //cleanup
        switch (type)
        {
            case WRITER:
            {
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
