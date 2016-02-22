package hcm.ssj;

import android.app.Application;
import android.content.res.AssetManager;
import android.test.ApplicationTestCase;

import hcm.ssj.androidSensor.AndroidSensor;
import hcm.ssj.androidSensor.AndroidSensorProvider;
import hcm.ssj.androidSensor.SensorType;
import hcm.ssj.core.TheFramework;
import hcm.ssj.mobileSSI.MobileSSIConsumer;
import hcm.ssj.test.Logger;

import static java.lang.System.loadLibrary;

/**
 * Tests all classes in the android sensor package.<br>
 * Created by Frank Gaibler on 13.08.2015.
 */
public class MobileSSITest extends ApplicationTestCase<Application>
{
    //test length in milliseconds
    private final static int TEST_LENGTH = 1000 * 35;//2 * 60 * 1000;

    public native void startSSI(String path, AssetManager am, boolean extractFiles);
    public native void stopSSI();

    /**
     *
     */
    public MobileSSITest()
    {
        super(Application.class);
    }

    /**
     * @throws Exception
     */
    public void testSensors() throws Exception
    {

        loadLibrary("ssiframe");
        loadLibrary("ssievent");
        loadLibrary("ssiioput");
        loadLibrary("ssiandroidsensors");
        loadLibrary("ssimodel");
        loadLibrary("ssisignal");
        loadLibrary("ssissjSensor");
        loadLibrary("android_xmlpipe");


        //test for every sensor type
        SensorType acc = SensorType.GYROSCOPE;
        SensorType mag = SensorType.ACCELEROMETER;

        //setup
        TheFramework frame = TheFramework.getFramework();
        frame.options.bufferSize = 10.0f;
        //sensor
        AndroidSensor sensor = new AndroidSensor(acc);
        AndroidSensor s2 = new AndroidSensor(mag);

        frame.addSensor(sensor);
        frame.addSensor(s2);

        //provider
        AndroidSensorProvider sensorProvider = new AndroidSensorProvider();
        AndroidSensorProvider sensorPmag = new AndroidSensorProvider();

        sensor.addProvider(sensorProvider);
        s2.addProvider(sensorPmag);

        //logger
        Logger log = new Logger();
        frame.addConsumer(log, sensorProvider, 1, 0);
        frame.addConsumer(log, sensorPmag, 1, 0);

        MobileSSIConsumer mobileSSI2 =new MobileSSIConsumer();
        MobileSSIConsumer mobileSSI = new MobileSSIConsumer();

        mobileSSI.setId(1);
        mobileSSI2.setId(2);

        frame.addConsumer(mobileSSI, sensorProvider, 1, 0);
        frame.addConsumer(mobileSSI2, sensorPmag, 1, 0);
        //start framework

        startSSI("/sdcard/android_xmlpipe", null, false);
        Thread.sleep(3100);
        frame.Start();

        mobileSSI.setSensor(sensorProvider, null, mobileSSI.getId());
        mobileSSI2.setSensor(sensorPmag, null, mobileSSI2.getId());
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
        stopSSI();
        frame.Stop();
        frame.clear();


    }


}
