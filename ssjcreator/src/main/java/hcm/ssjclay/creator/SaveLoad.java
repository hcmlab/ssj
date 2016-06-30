/*
 * SaveLoad.java
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

package hcm.ssjclay.creator;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import hcm.ssj.core.Consumer;
import hcm.ssj.core.Log;
import hcm.ssj.core.Provider;
import hcm.ssj.core.Sensor;
import hcm.ssj.core.SensorProvider;
import hcm.ssj.core.TheFramework;
import hcm.ssj.core.Transformer;
import hcm.ssj.core.option.Option;
import hcm.ssj.file.SimpleXmlParser;
import hcm.ssjclay.creator.container.ContainerElement;

/**
 * Save and load files in a {@link Linker} friendly format.<br>
 * Created by Frank Gaibler on 28.06.2016.
 */
public abstract class SaveLoad
{
    private final static String ROOT = "ssjSaveFile";
    private final static String VERSION = "version";
    private final static String VERSION_NUMBER = "1.0";
    private final static String FRAMEWORK = "framework";
    private final static String SENSOR_PROVIDER_LIST = "sensorProviderList";
    private final static String SENSOR_LIST = "sensorList";
    private final static String TRANSFORMER_LIST = "transformerList";
    private final static String CONSUMER_LIST = "consumerList";
    private final static String SENSOR_PROVIDER = "sensorProvider";
    private final static String SENSOR = "sensor";
    private final static String TRANSFORMER = "transformer";
    private final static String CONSUMER = "consumer";
    private final static String CLASS = "class";
    private final static String HASH = "hash";
    private final static String OPTIONS = "options";
    private final static String OPTION = "option";
    private final static String NAME = "name";
    private final static String VALUE = "value";
    private final static String PROVIDER_HASH = "providerHash";
    private final static String PROVIDER_LIST = "providerList";
    private final static String FRAME_SIZE = "frameSize";
    private final static String DELTA = "delta";

    /**
     * Saves the values in {@link Linker}
     *
     * @param file File
     * @return boolean
     */
    public static boolean save(File file)
    {
        //open stream
        FileOutputStream fileOutputStream;
        try
        {
            fileOutputStream = new FileOutputStream(file);

        } catch (FileNotFoundException e)
        {
            Log.e("file not found");
            return false;
        }
        XmlSerializer serializer = Xml.newSerializer();
        try
        {
            //start document
            serializer.setOutput(fileOutputStream, "UTF-8");
            serializer.startDocument(null, true);
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            serializer.startTag(null, ROOT);
            serializer.attribute(null, VERSION, VERSION_NUMBER);
            //framework
            serializer.startTag(null, FRAMEWORK);
            addOptions(serializer, TheFramework.getFramework());
            serializer.endTag(null, FRAMEWORK);
            //sensorProviders
            serializer.startTag(null, SENSOR_PROVIDER_LIST);
            LinkedHashSet<SensorProvider> hsSensorProviders = Linker.getInstance().hsSensorProviders;
            for (SensorProvider sensorProvider : hsSensorProviders)
            {
                serializer.startTag(null, SENSOR_PROVIDER);
                addStandard(serializer, sensorProvider);
                addOptions(serializer, sensorProvider);
                serializer.endTag(null, SENSOR_PROVIDER);
            }
            serializer.endTag(null, SENSOR_PROVIDER_LIST);
            //sensors
            serializer.startTag(null, SENSOR_LIST);
            LinkedHashMap<Sensor, SensorProvider> hmSensors = Linker.getInstance().hmSensors;
            for (Map.Entry<Sensor, SensorProvider> element : hmSensors.entrySet())
            {
                Sensor sensor = element.getKey();
                serializer.startTag(null, SENSOR);
                addStandard(serializer, sensor);
                addOptions(serializer, sensor);
                SensorProvider sensorProvider = element.getValue();
                if (sensorProvider != null)
                {
                    serializer.startTag(null, PROVIDER_HASH);
                    serializer.attribute(null, HASH, String.valueOf(sensorProvider.hashCode()));
                    serializer.endTag(null, PROVIDER_HASH);
                }
                serializer.endTag(null, SENSOR);
            }
            serializer.endTag(null, SENSOR_LIST);
            //transformers
            serializer.startTag(null, TRANSFORMER_LIST);
            for (ContainerElement<Transformer> containerElement : Linker.getInstance().hsTransformerElements)
            {
                addContainerElement(serializer, TRANSFORMER, containerElement);
            }
            serializer.endTag(null, TRANSFORMER_LIST);
            //consumers
            serializer.startTag(null, CONSUMER_LIST);
            for (ContainerElement<Consumer> containerElement : Linker.getInstance().hsConsumerElements)
            {
                addContainerElement(serializer, CONSUMER, containerElement);
            }
            serializer.endTag(null, CONSUMER_LIST);
            //finish document
            serializer.endTag(null, ROOT);
            serializer.endDocument();
            serializer.flush();
        } catch (IOException ex)
        {
            Log.e("could not save file");
            return false;
        } finally
        {
            try
            {
                fileOutputStream.close();
            } catch (IOException ex)
            {
                Log.e("could not close stream");
            }
        }
        return true;
    }

    /**
     * @param file File
     * @return boolean
     */
    public static boolean load(File file)
    {
        try
        {
            //check document version
            SimpleXmlParser xmlParser = new SimpleXmlParser();
            SimpleXmlParser.XmlValues xmlValues = xmlParser.parse(new FileInputStream(file),
                    new String[]{ROOT},
                    new String[]{VERSION}
            );
            ArrayList<String[]> foundAttributes = xmlValues.foundAttributes;
            if (foundAttributes.isEmpty() || Double.valueOf(foundAttributes.get(0)[0]) > Double.valueOf(VERSION_NUMBER))
            {
                Log.e("wrong file version");
                return false;
            }
            //framework
            xmlValues = xmlParser.parse(new FileInputStream(file),
                    new String[]{ROOT, FRAMEWORK, OPTIONS, OPTION},
                    new String[]{NAME, VALUE, CLASS}
            );
            foundAttributes = xmlValues.foundAttributes;
            Option[] options = Linker.getOptionList(TheFramework.getFramework());
            for (String[] strings : foundAttributes)
            {
                String name = strings[0], value = strings[1];
                for (Option option : options)
                {
                    if (option.getName().equals(name))
                    {
                        setValue(option, value);
                        break;
                    }
                }
            }
            //@todo implement rest
            //sensorProviders
            //sensors
            //transformers
            //consumers
            //close document
        } catch (IOException ex)
        {
            Log.e("could not load file");
            return false;
        } catch (XmlPullParserException ex)
        {
            Log.e("could not parse file");
            return false;
        }
        return true;
    }

    /**
     * @param serializer XmlSerializer
     * @param object     Object
     * @throws IOException
     */
    private static void addStandard(XmlSerializer serializer, Object object) throws IOException
    {
        serializer.attribute(null, CLASS, object.getClass().getName());
        serializer.attribute(null, HASH, String.valueOf(object.hashCode()));
    }

    /**
     * @param serializer XmlSerializer
     * @param object     Object
     * @throws IOException
     */
    private static void addOptions(XmlSerializer serializer, Object object) throws IOException
    {
        serializer.startTag(null, OPTIONS);
        Option[] options = Linker.getOptionList(object);
        if (options != null)
        {
            //@todo only handle primitives, arrays and strings
            //@todo handle arrays and remove objects
            for (Option option : options)
            {
                serializer.startTag(null, OPTION);
                serializer.attribute(null, NAME, option.getName());
                serializer.attribute(null, VALUE, String.valueOf(option.get()));
                serializer.attribute(null, CLASS, option.getType().getName());
                serializer.endTag(null, OPTION);
            }
        }
        serializer.endTag(null, OPTIONS);
    }

    /**
     * @param serializer       XmlSerializer
     * @param tag              String
     * @param containerElement ContainerElement
     */
    private static void addContainerElement(XmlSerializer serializer, String tag, ContainerElement<?> containerElement) throws IOException
    {
        serializer.startTag(null, tag);
        addStandard(serializer, containerElement.getElement());
        serializer.attribute(null, FRAME_SIZE, String.valueOf(containerElement.getFrameSize()));
        serializer.attribute(null, DELTA, String.valueOf(containerElement.getDelta()));
        addOptions(serializer, containerElement.getElement());
        HashMap<Provider, Boolean> hashMap = containerElement.getHmProviders();
        serializer.startTag(null, PROVIDER_LIST);
        for (Map.Entry<Provider, Boolean> element : hashMap.entrySet())
        {
            serializer.startTag(null, PROVIDER_HASH);
            serializer.attribute(null, HASH, String.valueOf(element.getKey().hashCode()));
            serializer.endTag(null, PROVIDER_HASH);
        }
        serializer.endTag(null, PROVIDER_LIST);
        serializer.endTag(null, tag);
    }

    /**
     * @param option Option
     * @param value  String
     */
    private static void setValue(Option option, String value)
    {
        //@todo handle arrays
        Class<?> type = option.getType();
        if (type == Byte.class)
        {
            option.set(Byte.valueOf(value));
        } else if (type == Short.class)
        {
            option.set(Short.valueOf(value));
        } else if (type == Integer.class)
        {
            option.set(Integer.valueOf(value));
        } else if (type == Long.class)
        {
            option.set(Long.valueOf(value));
        } else if (type == Float.class)
        {
            option.set(Float.valueOf(value));
        } else if (type == Double.class)
        {
            option.set(Double.valueOf(value));
        } else if (type == Character.class)
        {
            option.set(value.charAt(0));
        } else if (type == String.class)
        {
            option.set(value);
        } else if (type == Boolean.class)
        {
            option.set(Boolean.valueOf(value));
        }
    }
}
