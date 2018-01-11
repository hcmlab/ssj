/*
 * SSJDescriptor.java
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

package hcm.ssj.creator.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;

import dalvik.system.DexFile;
import dalvik.system.PathClassLoader;
import hcm.ssj.core.SSJApplication;

/**
 * Builds pipelines.<br>
 * Created by Frank Gaibler on 09.03.2016.
 */
public class SSJDescriptor
{
    private static SSJDescriptor instance = null;
    //
    public ArrayList<Class> sensors = new ArrayList<>();
    public ArrayList<Class> sensorChannels = new ArrayList<>();
    public ArrayList<Class> transformers = new ArrayList<>();
    public ArrayList<Class> consumers = new ArrayList<>();
    public ArrayList<Class> eventHandlers = new ArrayList<>();
    public ArrayList<Class> models = new ArrayList<>();
    private HashSet<String> hsClassNames = new HashSet<>();

    /**
     *
     */
    private SSJDescriptor()
    {
        scan();
    }

    /**
     * @return Builder
     */
    public static synchronized SSJDescriptor getInstance()
    {
        if (instance == null)
        {
            instance = new SSJDescriptor();
        }
        return instance;
    }

    private static SharedPreferences getMultiDexPreferences(Context context) {
        return context.getSharedPreferences("multidex.version", Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB ?
                Context.MODE_PRIVATE :
                Context.MODE_PRIVATE | Context.MODE_MULTI_PROCESS);
    }


    /**
     * Parse classes.dex to find all implemented SSJ components.<br>
     * Based on code from stackoverflow (<a href="http://stackoverflow.com/a/31087947">one</a>
     * and <a href="http://stackoverflow.com/a/36491692">two</a>).
     */
    private void scan()
    {
        try
        {
            ApplicationInfo applicationInfo = SSJApplication.getAppContext().getPackageManager().getApplicationInfo(SSJApplication.getAppContext().getPackageName(), 0);

            if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP)
            {
                scanDex(new DexFile(applicationInfo.sourceDir));
            }
            else
            {
                //get source directory
                String dir = applicationInfo.sourceDir.substring(0, applicationInfo.sourceDir.lastIndexOf(File.separator));
                //iterate through all .apk and .dex in the source directory
                File[] files = new File(dir).listFiles();
                for (File slice : files)
                {
                    if (!slice.isFile())
                        continue;

                    String extension = slice.getName().substring(slice.getName().lastIndexOf("."));
                    if (extension.equalsIgnoreCase(".apk") || extension.equalsIgnoreCase(".dex"))
                        scanDex(new DexFile(slice));
                }
            }

        } catch (IOException | PackageManager.NameNotFoundException ex)
        {
            ex.printStackTrace();
        }

        //add classes
        PathClassLoader classLoader = (PathClassLoader) Thread.currentThread().getContextClassLoader();
        for (String className : hsClassNames)
        {
            try
            {
                Class<?> aClass = classLoader.loadClass(className);
                //only add valid classes
                if (!Modifier.isAbstract(aClass.getModifiers()) && !Modifier.isInterface(aClass.getModifiers()) && !Modifier.isPrivate(aClass.getModifiers()))
                {
                    Class<?> parent = aClass.getSuperclass();
                    while (parent != null)
                    {
                        if (parent.getSimpleName().compareToIgnoreCase("Sensor") == 0)
                        {
                            sensors.add(aClass);
                        } else if (parent.getSimpleName().compareToIgnoreCase("SensorChannel") == 0)
                        {
                            sensorChannels.add(aClass);
                        } else if (parent.getSimpleName().compareToIgnoreCase("Transformer") == 0)
                        {
                            transformers.add(aClass);
                        } else if (parent.getSimpleName().compareToIgnoreCase("Consumer") == 0)
                        {
                            consumers.add(aClass);
                        } else if (parent.getSimpleName().compareToIgnoreCase("EventHandler") == 0)
                        {
                            eventHandlers.add(aClass);
                        } else if (parent.getSimpleName().compareToIgnoreCase("Model") == 0)
                        {
                            models.add(aClass);
                        }
                        parent = parent.getSuperclass();
                    }
                }
            } catch (ClassNotFoundException cnfe)
            {
                cnfe.printStackTrace();
            }
        }
    }

    /**
     * @param dexFile DexFile
     */
    private void scanDex(DexFile dexFile)
    {
        Enumeration<String> classNames = dexFile.entries();
        while (classNames.hasMoreElements())
        {
            String className = classNames.nextElement();
            if (className.startsWith("hcm.ssj.") && !className.contains("$"))
            {
                hsClassNames.add(className);
            }
        }
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
