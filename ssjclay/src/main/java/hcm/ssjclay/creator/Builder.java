package hcm.ssjclay.creator;

import android.content.Context;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;

import dalvik.system.DexFile;
import dalvik.system.PathClassLoader;
import hcm.ssj.core.Log;

/**
 * Builds pipelines.<br>
 * Created by Frank Gaibler on 09.03.2016.
 */
public class Builder
{
    public static Builder getInstance()
    {
        if(instance == null)
            instance = new Builder();

        return instance;
    }

    private Builder()
    {}

    private static Builder instance = null;


    public ArrayList<Class> sensors = new ArrayList<>();
    public ArrayList<Class> sensorProviders = new ArrayList<>();
    public ArrayList<Class> transformers = new ArrayList<>();
    public ArrayList<Class> consumers = new ArrayList<>();
    public ArrayList<Class> eventConsumers = new ArrayList<>();

    /*
     * parse classes.dex to find all implemented SSJ components
     * based on code from: http://stackoverflow.com/a/31087947
     */
    public void scan(Context context) throws IOException, ClassNotFoundException, NoSuchMethodException {
        long timeBegin = System.currentTimeMillis();

        PathClassLoader classLoader = (PathClassLoader) context.getClassLoader();
        //PathClassLoader classLoader = (PathClassLoader) Thread.currentThread().getContextClassLoader();//This also works good
        DexFile dexFile = new DexFile(context.getPackageCodePath());
        Enumeration<String> classNames = dexFile.entries();
        while (classNames.hasMoreElements()) {
            String className = classNames.nextElement();
            if(className.startsWith("hcm.ssj.") && !className.contains("$1")) {

                Class<?> aClass = classLoader.loadClass(className);
                Class<?> parent = aClass.getSuperclass();

                while(parent != null) {

                    if(parent.getSimpleName().compareToIgnoreCase("Sensor") == 0)
                        sensors.add(aClass);
                    else if(parent.getSimpleName().compareToIgnoreCase("SensorProvider") == 0)
                        sensorProviders.add(aClass);
                    else if(parent.getSimpleName().compareToIgnoreCase("Transformer") == 0)
                        transformers.add(aClass);
                    else if(parent.getSimpleName().compareToIgnoreCase("Consumer") == 0)
                        consumers.add(aClass);
                    else if(parent.getSimpleName().compareToIgnoreCase("EventConsumer") == 0)
                        eventConsumers.add(aClass);

                    parent = parent.getSuperclass();
                }
            }
        }

        long timeEnd = System.currentTimeMillis();
        long timeElapsed = timeEnd - timeBegin;
        Log.d("scan() cost " + timeElapsed + "ms");
    }

    /**
     * @param clazz Class
     * @return Object
     */
    public static Object instantiate(Class clazz)
    {
        try
        {
            return clazz.newInstance();
        } catch (IllegalAccessException | InstantiationException ex)
        {
            throw new RuntimeException(ex);
        }
    }
}
