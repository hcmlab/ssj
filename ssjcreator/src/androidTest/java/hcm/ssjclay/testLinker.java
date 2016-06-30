package hcm.ssjclay;

import android.app.Application;
import android.test.ApplicationTestCase;

import hcm.ssj.androidSensor.AndroidSensor;
import hcm.ssj.androidSensor.AndroidSensorProvider;
import hcm.ssj.core.Consumer;
import hcm.ssj.core.Sensor;
import hcm.ssj.core.SensorProvider;
import hcm.ssj.core.TheFramework;
import hcm.ssj.test.Logger;
import hcm.ssjclay.creator.Builder;
import hcm.ssjclay.creator.Linker;

/**
 * Created by Frank Gaibler on 10.03.2016.
 */
public class testLinker extends ApplicationTestCase<Application>
{
    //test length in milliseconds
    private final static int TEST_LENGTH = 2 * 5 * 1000;

    /**
     *
     */
    public testLinker()
    {
        super(Application.class);
    }

    /**
     * @throws Exception
     */
    public void testBuildAndLink() throws Exception
    {
        //scan content
        Builder builder = Builder.getInstance();
//        Builder.getInstance().scan(this.getContext());
        System.out.println(builder.sensors.get(0));
        System.out.println(builder.sensorProviders.get(0));
        System.out.println(builder.consumers.get(0));
        //
        TheFramework frame = TheFramework.getFramework();
        frame.options.bufferSize.set(2.0f);
        Linker linker = Linker.getInstance();
        //select classes
        Sensor sensor = null;
        for (Class clazz : builder.sensors)
        {
            if (clazz.equals(AndroidSensor.class))
            {
                sensor = (Sensor) Builder.instantiate(clazz);
                break;
            }
        }
        SensorProvider sensorProvider = null;
        if (sensor != null)
        {
            for (Class clazz : builder.sensorProviders)
            {
                if (clazz.equals(AndroidSensorProvider.class))
                {
                    sensorProvider = (SensorProvider) Builder.instantiate(clazz);
                    break;
                }
            }
        }
        Consumer consumer = null;
        if (sensorProvider != null)
        {
            linker.add(sensor);
            linker.add(sensorProvider);
            linker.addProvider(sensor, sensorProvider);
            for (Class clazz : builder.consumers)
            {
                if (clazz.equals(Logger.class))
                {
                    consumer = (Consumer) Builder.instantiate(clazz);
                    break;
                }
            }
            if (consumer != null)
            {
                linker.add(consumer);
                linker.addProvider(consumer, sensorProvider);
                linker.setFrameSize(consumer, 1);
                linker.setDelta(consumer, 0);
            }
        }
        linker.buildPipe();
        //start framework
        frame.Start();
        //run for two minutes
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