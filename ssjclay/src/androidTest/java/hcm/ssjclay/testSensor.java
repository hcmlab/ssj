package hcm.ssjclay;

import android.app.Application;
import android.test.ApplicationTestCase;
import android.util.Log;

import hcm.ssj.androidSensor.AndroidSensor;
import hcm.ssj.androidSensor.AndroidSensorProvider;
import hcm.ssj.androidSensor.SensorType;
import hcm.ssj.androidSensor.transformer.AvgVar;
import hcm.ssj.androidSensor.transformer.Count;
import hcm.ssj.androidSensor.transformer.Distance;
import hcm.ssj.androidSensor.transformer.Median;
import hcm.ssj.androidSensor.transformer.MinMax;
import hcm.ssj.androidSensor.transformer.Progress;
import hcm.ssj.core.TheFramework;
import hcm.ssj.test.Logger;

/**
 * Tests all classes in the android sensor package.<br>
 * Created by Frank Gaibler on 29.03.2016.
 */
public class testSensor extends ApplicationTestCase<Application>
{
    //test length in milliseconds
    private final static int TEST_LENGTH = 1000 * 10;//2 * 60 * 1000;

    /**
     *
     */
    public testSensor()
    {
        super(Application.class);
    }

    /**
     * @throws Exception
     */
    public void testSensors() throws Exception
    {
        Runtime rt = Runtime.getRuntime();
        long maxMemory = rt.maxMemory();
        Log.i("SSJ", "maxMemory: " + Long.toString(maxMemory));


        //test for every sensor type
        for (SensorType type : SensorType.values())
        {
            //setup
            TheFramework frame = TheFramework.getFramework();
            frame.options.bufferSize.setValue(10.0f);
            //sensor
            AndroidSensor sensor = new AndroidSensor();
            sensor.options.sensorType.setValue(type);
            frame.addSensor(sensor);
            //provider
            AndroidSensorProvider sensorProvider = new AndroidSensorProvider();
            sensor.addProvider(sensorProvider);
            //logger
            Logger log = new Logger();
            frame.addConsumer(log, sensorProvider, 1, 0);
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
            frame.invalidateFramework();
        }
    }

    /**
     * @throws Exception
     */
    public void testMinMax() throws Exception
    {
        //test for a few setups
        boolean[][] options = {
                {true, true},
                {true, false},
                {false, true}
        };
        for (boolean[] option : options)
        {
            //setup
            TheFramework frame = TheFramework.getFramework();
            frame.options.bufferSize.setValue(10.0f);
            //create providers
            SensorType[] sensorTypes = {SensorType.ACCELEROMETER, SensorType.AMBIENT_TEMPERATURE, SensorType.GYROSCOPE_UNCALIBRATED};
            AndroidSensorProvider[] sensorProviders = new AndroidSensorProvider[sensorTypes.length];
            for (int i = 0; i < sensorTypes.length; i++)
            {
                //sensor
                AndroidSensor sensor = new AndroidSensor();
                sensor.options.sensorType.setValue(sensorTypes[i]);
                frame.addSensor(sensor);
                //provider
                AndroidSensorProvider sensorProvider = new AndroidSensorProvider();
                sensor.addProvider(sensorProvider);
                sensorProviders[i] = sensorProvider;

            }
            //transformer
            MinMax transformer = new MinMax();
            transformer.options.min.setValue(option[0]);
            transformer.options.max.setValue(option[1]);
            frame.addTransformer(transformer, sensorProviders, 1, 0);
            //logger
            Logger log = new Logger();
            frame.addConsumer(log, transformer, 1, 0);
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
            frame.invalidateFramework();
        }
    }

    /**
     * @throws Exception
     */
    public void testAvgVar() throws Exception
    {
        //test for a few setups
        boolean[][] options = {
                {true, true},
                {true, false},
                {false, true}
        };
        for (boolean[] option : options)
        {
            //setup
            TheFramework frame = TheFramework.getFramework();
            frame.options.bufferSize.setValue(10.0f);
            //create providers
            SensorType[] sensorTypes = {SensorType.ACCELEROMETER, SensorType.AMBIENT_TEMPERATURE, SensorType.GYROSCOPE_UNCALIBRATED};
            AndroidSensorProvider[] sensorProviders = new AndroidSensorProvider[sensorTypes.length];
            for (int i = 0; i < sensorTypes.length; i++)
            {
                //sensor
                AndroidSensor sensor = new AndroidSensor();
                sensor.options.sensorType.setValue(sensorTypes[i]);
                frame.addSensor(sensor);
                //provider
                AndroidSensorProvider sensorProvider = new AndroidSensorProvider();
                sensor.addProvider(sensorProvider);
                sensorProviders[i] = sensorProvider;

            }
            //transformer
            AvgVar transformer = new AvgVar();
            transformer.options.avg.setValue(option[0]);
            transformer.options.var.setValue(option[1]);
            frame.addTransformer(transformer, sensorProviders, 1, 0);
            //logger
            Logger log = new Logger();
            frame.addConsumer(log, transformer, 1, 0);
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
            frame.invalidateFramework();
        }
    }

    /**
     * @throws Exception
     */
    public void testProgressDistanceCount() throws Exception
    {
        //setup
        TheFramework frame = TheFramework.getFramework();
        frame.options.bufferSize.setValue(10.0f);
        //create providers
        SensorType[] sensorTypes = {SensorType.ACCELEROMETER, SensorType.AMBIENT_TEMPERATURE, SensorType.GYROSCOPE_UNCALIBRATED};
        AndroidSensorProvider[] sensorProviders = new AndroidSensorProvider[sensorTypes.length];
        for (int i = 0; i < sensorTypes.length; i++)
        {
            //sensor
            AndroidSensor sensor = new AndroidSensor();
            sensor.options.sensorType.setValue(sensorTypes[i]);
            frame.addSensor(sensor);
            //provider
            AndroidSensorProvider sensorProvider = new AndroidSensorProvider();
            sensor.addProvider(sensorProvider);
            sensorProviders[i] = sensorProvider;

        }
        //transformer
        Progress transformer1 = new Progress();
        frame.addTransformer(transformer1, sensorProviders, 1, 0);
        Distance transformer2 = new Distance();
        frame.addTransformer(transformer2, transformer1, 1, 0);
        Count transformer3 = new Count();
        frame.addTransformer(transformer3, transformer2, 1, 0);
        //logger
        Logger log = new Logger();
        frame.addConsumer(log, transformer3, 1, 0);
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
        frame.invalidateFramework();
    }

    /**
     * @throws Exception
     */
    public void testMedian() throws Exception
    {
        //setup
        TheFramework frame = TheFramework.getFramework();
        frame.options.bufferSize.setValue(10.0f);
        //create providers
        SensorType[] sensorTypes = {SensorType.ACCELEROMETER, SensorType.AMBIENT_TEMPERATURE, SensorType.GYROSCOPE_UNCALIBRATED};
        AndroidSensorProvider[] sensorProviders = new AndroidSensorProvider[sensorTypes.length];
        for (int i = 0; i < sensorTypes.length; i++)
        {
            //sensor
            AndroidSensor sensor = new AndroidSensor();
            sensor.options.sensorType.setValue(sensorTypes[i]);
            frame.addSensor(sensor);
            //provider
            AndroidSensorProvider sensorProvider = new AndroidSensorProvider();
            sensor.addProvider(sensorProvider);
            sensorProviders[i] = sensorProvider;

        }
        //transformer
        Median transformer = new Median();
        frame.addTransformer(transformer, sensorProviders, 1, 0);
        //logger
        Logger log = new Logger();
        frame.addConsumer(log, transformer, 1, 0);
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
        frame.invalidateFramework();
    }
}
