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

package hcm.ssj.creator.core;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import hcm.ssj.core.Consumer;
import hcm.ssj.core.Log;
import hcm.ssj.core.Provider;
import hcm.ssj.core.Sensor;
import hcm.ssj.core.SensorChannel;
import hcm.ssj.core.TheFramework;
import hcm.ssj.core.Transformer;
import hcm.ssj.core.option.Option;
import hcm.ssj.creator.core.container.ContainerElement;

/**
 * Save and load files in a {@link Linker} friendly format.<br>
 * Created by Frank Gaibler on 28.06.2016.
 */
public abstract class SaveLoad
{
    private final static String ROOT = "ssjSaveFile";
    private final static String VERSION = "version";
    private final static String VERSION_NUMBER = "0.2";
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
    private final static String ID = "id";
    private final static String OPTIONS = "options";
    private final static String OPTION = "option";
    private final static String NAME = "name";
    private final static String VALUE = "value";
    private final static String PROVIDER_ID = "providerId";
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
            LinkedHashSet<SensorChannel> hsSensorChannels = Linker.getInstance().hsSensorChannels;
            for (SensorChannel sensorChannel : hsSensorChannels)
            {
                serializer.startTag(null, SENSOR_PROVIDER);
                addStandard(serializer, sensorChannel);
                addOptions(serializer, sensorChannel);
                serializer.endTag(null, SENSOR_PROVIDER);
            }
            serializer.endTag(null, SENSOR_PROVIDER_LIST);
            //sensors
            serializer.startTag(null, SENSOR_LIST);
            for (ContainerElement<Sensor> containerElement : Linker.getInstance().hsSensorElements)
            {
                addContainerElement(serializer, SENSOR, containerElement, false);
            }
            serializer.endTag(null, SENSOR_LIST);
            //transformers
            serializer.startTag(null, TRANSFORMER_LIST);
            for (ContainerElement<Transformer> containerElement : Linker.getInstance().hsTransformerElements)
            {
                addContainerElement(serializer, TRANSFORMER, containerElement, true);
            }
            serializer.endTag(null, TRANSFORMER_LIST);
            //consumers
            serializer.startTag(null, CONSUMER_LIST);
            for (ContainerElement<Consumer> containerElement : Linker.getInstance().hsConsumerElements)
            {
                addContainerElement(serializer, CONSUMER, containerElement, true);
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
        FileInputStream fileInputStream;
        try
        {
            fileInputStream = new FileInputStream(file);

        } catch (FileNotFoundException e)
        {
            Log.e("file not found");
            return false;
        }
        return load(fileInputStream);
    }

    /**
     * @param fileInputStream FileInputStream
     * @return boolean
     */
    public static boolean load(FileInputStream fileInputStream)
    {
        try
        {
            //check file version
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(fileInputStream, null);
            parser.nextTag();
            if (parser.getName().equals(ROOT))
            {
                String value = parser.getAttributeValue(null, VERSION);
                if (Double.valueOf(VERSION_NUMBER) < Double.valueOf(value))
                {
                    return false;
                }
            } else
            {
                return false;
            }
            //load classes
            parser.nextTag();
            String tag;
            Object context = null;
            Option[] options = null;
            HashMap<Object, LinkContainer> map = new HashMap<>();
            while (!(tag = parser.getName()).equals(ROOT))
            {
                if (parser.getEventType() == XmlPullParser.START_TAG)
                {
                    switch (tag)
                    {
                        case FRAMEWORK:
                        {
                            context = TheFramework.getFramework();
                            break;
                        }
                        case OPTIONS:
                        {
                            options = Linker.getOptionList(context);
                            break;
                        }
                        case OPTION:
                        {
                            if (options != null)
                            {
                                String name = parser.getAttributeValue(null, NAME);
                                String value = parser.getAttributeValue(null, VALUE);
                                for (Option option : options)
                                {
                                    if (option.getName().equals(name))
                                    {
                                        option.setValue(value);
                                        break;
                                    }
                                }
                            }
                            break;
                        }
                        case SENSOR_PROVIDER:
                        case SENSOR:
                        {
                            String clazz = parser.getAttributeValue(null, CLASS);
                            context = Class.forName(clazz).newInstance();
                            Linker.getInstance().add(context);
                            String hash = parser.getAttributeValue(null, ID);
                            LinkContainer container = new LinkContainer();
                            container.hash = Integer.parseInt(hash);
                            map.put(context, container);
                            break;
                        }
                        case TRANSFORMER:
                        case CONSUMER:
                        {
                            String clazz = parser.getAttributeValue(null, CLASS);
                            context = Class.forName(clazz).newInstance();
                            Linker.getInstance().add(context);
                            Linker.getInstance().setFrameSize(context, Double.valueOf(parser.getAttributeValue(null, FRAME_SIZE)));
                            Linker.getInstance().setDelta(context, Double.valueOf(parser.getAttributeValue(null, DELTA)));
                            String hash = parser.getAttributeValue(null, ID);
                            LinkContainer container = new LinkContainer();
                            container.hash = Integer.parseInt(hash);
                            map.put(context, container);
                            break;
                        }
                        case PROVIDER_ID:
                        {
                            String hash = parser.getAttributeValue(null, ID);
                            map.get(context).hashes.add(Integer.parseInt(hash));
                            break;
                        }
                    }
                }
                parser.nextTag();
            }
            //set connections
            for (Map.Entry<Object, LinkContainer> entry : map.entrySet())
            {
                Object key = entry.getKey();
                LinkContainer value = entry.getValue();
                for (int provider : value.hashes)
                {
                    for (Map.Entry<Object, LinkContainer> candidate : map.entrySet())
                    {
                        Object candidateKey = candidate.getKey();
                        LinkContainer candidateValue = candidate.getValue();
                        if (candidateValue.hash == provider)
                        {
                            Linker.getInstance().addProvider(key, (Provider) candidateKey);
                        }
                    }
                }
            }
            return true;
        } catch (IOException | XmlPullParserException ex)
        {
            Log.e("could not parse file", ex);
            return false;
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex)
        {
            Log.e("could not create class", ex);
            return false;
        } finally
        {
            try
            {
                fileInputStream.close();
            } catch (IOException ex)
            {
                Log.e("could not close stream", ex);
            }
        }
    }

    /**
     * @param serializer XmlSerializer
     * @param object     Object
     * @throws IOException
     */
    private static void addStandard(XmlSerializer serializer, Object object) throws IOException
    {
        serializer.attribute(null, CLASS, object.getClass().getName());
        serializer.attribute(null, ID, String.valueOf(object.hashCode()));
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
            for (Option option : options)
            {
                if (option.isAssignableByString() && option.get() != null)
                {
                    serializer.startTag(null, OPTION);
                    serializer.attribute(null, NAME, option.getName());
                    if (option.getType().isArray())
                    {
                        Object value = option.get();
                        List ar = new ArrayList();
                        int length = Array.getLength(value);
                        for (int i = 0; i < length; i++)
                        {
                            ar.add(Array.get(value, i));
                        }
                        Object[] objects = ar.toArray();
                        serializer.attribute(null, VALUE, Arrays.toString(objects));
                    } else
                    {
                        serializer.attribute(null, VALUE, String.valueOf(option.get()));
                    }
                    serializer.endTag(null, OPTION);
                }
            }
        }
        serializer.endTag(null, OPTIONS);
    }

    /**
     * @param serializer       XmlSerializer
     * @param tag              String
     * @param containerElement ContainerElement
     * @param withAttributes   boolean
     */
    private static void addContainerElement(XmlSerializer serializer, String tag, ContainerElement<?> containerElement, boolean withAttributes) throws IOException
    {
        serializer.startTag(null, tag);
        addStandard(serializer, containerElement.getElement());
        if (withAttributes)
        {
            serializer.attribute(null, FRAME_SIZE, String.valueOf(containerElement.getFrameSize()));
            serializer.attribute(null, DELTA, String.valueOf(containerElement.getDelta()));
        }
        addOptions(serializer, containerElement.getElement());
        HashMap<Provider, Boolean> hashMap = containerElement.getHmProviders();
        serializer.startTag(null, PROVIDER_LIST);
        for (Map.Entry<Provider, Boolean> element : hashMap.entrySet())
        {
            serializer.startTag(null, PROVIDER_ID);
            serializer.attribute(null, ID, String.valueOf(element.getKey().hashCode()));
            serializer.endTag(null, PROVIDER_ID);
        }
        serializer.endTag(null, PROVIDER_LIST);
        serializer.endTag(null, tag);
    }

    /**
     * Used to add connections.
     */
    private static class LinkContainer
    {
        int hash;
        ArrayList<Integer> hashes = new ArrayList<>();
    }
}
