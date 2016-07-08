/*
 * Linker.java
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

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import hcm.ssj.core.Consumer;
import hcm.ssj.core.Provider;
import hcm.ssj.core.Sensor;
import hcm.ssj.core.SensorProvider;
import hcm.ssj.core.TheFramework;
import hcm.ssj.core.Transformer;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssjclay.creator.container.ContainerElement;

/**
 * Linker for a pipeline.<br>
 * Created by Frank Gaibler on 09.03.2016.
 */
public class Linker
{
    private static Linker instance = null;
    protected LinkedHashSet<SensorProvider> hsSensorProviders = new LinkedHashSet<>();
    protected LinkedHashSet<ContainerElement<Sensor>> hsSensorElements = new LinkedHashSet<>();
    protected LinkedHashSet<ContainerElement<Transformer>> hsTransformerElements = new LinkedHashSet<>();
    protected LinkedHashSet<ContainerElement<Consumer>> hsConsumerElements = new LinkedHashSet<>();
//    private LinkedHashSet<Container<EventConsumer>> hsEventConsumers = new LinkedHashSet<>(); //doesn't work yet, because of EventChannel and optional frame size

    public enum Type
    {
        Sensor, SensorProvider, Transformer, Consumer
    }

    /**
     *
     */
    private Linker()
    {
    }

    /**
     * @return Linker
     */
    public static synchronized Linker getInstance()
    {
        if (instance == null)
        {
            instance = new Linker();
        }
        return instance;
    }

    /**
     *
     */
    public void clear()
    {
        hsSensorProviders.clear();
        hsSensorElements.clear();
        hsTransformerElements.clear();
        hsConsumerElements.clear();
    }

    /**
     * @param o Object
     * @return Object[]
     */
    public Object[] getProviders(Object o)
    {
        if (o instanceof Sensor)
        {
            for (ContainerElement element : hsSensorElements)
            {
                if (element.getElement().equals(o))
                {
                    return element.getHmProviders().keySet().toArray();
                }
            }
        } else if (o instanceof Transformer)
        {
            for (ContainerElement element : hsTransformerElements)
            {
                if (element.getElement().equals(o))
                {
                    return element.getHmProviders().keySet().toArray();
                }
            }
        } else if (o instanceof Consumer)
        {
            for (ContainerElement element : hsConsumerElements)
            {
                if (element.getElement().equals(o))
                {
                    return element.getHmProviders().keySet().toArray();
                }
            }
        }
        return null;
    }

    /**
     * @param type Type
     */
    public Object[] getAll(Type type)
    {
        switch (type)
        {
            case Sensor:
            {
                Object[] objects = new Object[hsSensorElements.size()];
                int i = 0;
                for (ContainerElement element : hsSensorElements)
                {
                    objects[i] = element.getElement();
                    i++;
                }
                return objects;
            }
            case SensorProvider:
                return hsSensorProviders.toArray();
            case Transformer:
            {
                Object[] objects = new Object[hsTransformerElements.size()];
                int i = 0;
                for (ContainerElement element : hsTransformerElements)
                {
                    objects[i] = element.getElement();
                    i++;
                }
                return objects;
            }
            case Consumer:
            {
                Object[] objects = new Object[hsConsumerElements.size()];
                int i = 0;
                for (ContainerElement element : hsConsumerElements)
                {
                    objects[i] = element.getElement();
                    i++;
                }
                return objects;
            }
            default:
                throw new RuntimeException();
        }
    }

    /**
     *
     */
    public void buildPipe()
    {
        TheFramework framework = TheFramework.getFramework();
        //add to framework
        //sensors and sensorProviders
        for (ContainerElement<Sensor> element : hsSensorElements)
        {
            Sensor sensor = element.getElement();
            HashMap<Provider, Boolean> hmProviders = element.getHmProviders();
            if (hmProviders.size() > 0)
            {
                framework.addSensor(sensor);
                for (Map.Entry<Provider, Boolean> entry : hmProviders.entrySet())
                {
                    SensorProvider sensorProvider = (SensorProvider) entry.getKey();
                    sensor.addProvider(sensorProvider);
                    //activate in transformer
                    for (ContainerElement<Transformer> element2 : hsTransformerElements)
                    {
                        element2.setAdded(sensorProvider);
                    }
                    //activate in consumer
                    for (ContainerElement<Consumer> element2 : hsConsumerElements)
                    {
                        element2.setAdded(sensorProvider);
                    }
                }
            }
        }
        //transformers
        int count = 0;
        int lastCount = -1;
        while (count < hsTransformerElements.size() && count != lastCount)
        {
            lastCount = count;
            for (ContainerElement<Transformer> element : hsTransformerElements)
            {
                if (element.getHmProviders().size() > 0)
                {
                    if (element.allAdded())
                    {
                        framework.addTransformer(element.getElement(),
                                element.getHmProviders().keySet().toArray(new Provider[element.getHmProviders().size()]),
                                element.getFrameSize(), element.getDelta());
                        count++;
                        //activate in transformer
                        for (ContainerElement<Transformer> element2 : hsTransformerElements)
                        {
                            element2.setAdded(element.getElement());
                        }
                        //activate in consumer
                        for (ContainerElement<Consumer> element2 : hsConsumerElements)
                        {
                            element2.setAdded(element.getElement());
                        }
                    }
                } else
                {
                    count++;
                }
            }
        }
        //consumers
        for (ContainerElement<Consumer> element : hsConsumerElements)
        {
            if (element.getHmProviders().size() > 0 && element.allAdded())
            {
                framework.addConsumer(element.getElement(),
                        element.getHmProviders().keySet().toArray(new Provider[element.getHmProviders().size()]),
                        element.getFrameSize(), element.getDelta());
            }
        }
    }

    /**
     * @param o Object
     * @return boolean
     */
    public boolean add(Object o)
    {
        //sensorProvider
        if (o instanceof SensorProvider)
        {
            return hsSensorProviders.add((SensorProvider) o);
        }
        //sensor
        else if (o instanceof Sensor)
        {
            for (ContainerElement<Sensor> element : hsSensorElements)
            {
                if (element.getElement().equals(o))
                {
                    return false;
                }
            }
            return hsSensorElements.add(new ContainerElement<>((Sensor) o));
        }
        //transformer
        else if (o instanceof Transformer)
        {
            for (ContainerElement<Transformer> element : hsTransformerElements)
            {
                if (element.getElement().equals(o))
                {
                    return false;
                }
            }
            return hsTransformerElements.add(new ContainerElement<>((Transformer) o));
        }
        //consumer
        else if (o instanceof Consumer)
        {
            for (ContainerElement<Consumer> element : hsConsumerElements)
            {
                if (element.getElement().equals(o))
                {
                    return false;
                }
            }
            return hsConsumerElements.add(new ContainerElement<>((Consumer) o));
        }
        return false;
    }

    /**
     * @param o Object
     * @return boolean
     */
    public boolean remove(Object o)
    {
        //sensor
        if (o instanceof Sensor)
        {
            for (ContainerElement<Sensor> element : hsSensorElements)
            {
                if (element.getElement().equals(o))
                {
                    return hsSensorElements.remove(element);
                }
            }
        }
        //sensorProvider
        else if (o instanceof SensorProvider)
        {
            SensorProvider sensorProvider = (SensorProvider) o;
            //also remove in sensors
            for (ContainerElement<Sensor> element : hsSensorElements)
            {
                element.removeProvider(sensorProvider);
            }
            //also remove in transformers
            for (ContainerElement<Transformer> element : hsTransformerElements)
            {
                element.removeProvider(sensorProvider);
            }
            //also remove in consumers
            for (ContainerElement<Consumer> element : hsConsumerElements)
            {
                element.removeProvider(sensorProvider);
            }
            return hsSensorProviders.remove(sensorProvider);
        }
        //transformer
        else if (o instanceof Transformer)
        {
            for (ContainerElement<Transformer> element : hsTransformerElements)
            {
                if (element.getElement().equals(o))
                {
                    //also remove in all other transformers
                    for (ContainerElement<Transformer> element2 : hsTransformerElements)
                    {
                        element2.removeProvider((Provider) o);
                    }
                    //also remove in consumers
                    for (ContainerElement<Consumer> element3 : hsConsumerElements)
                    {
                        element3.removeProvider((Provider) o);
                    }
                    return hsTransformerElements.remove(element);
                }
            }
        }
        //consumer
        else if (o instanceof Consumer)
        {
            for (ContainerElement<Consumer> element : hsConsumerElements)
            {
                if (element.getElement().equals(o))
                {
                    return hsConsumerElements.remove(element);
                }
            }
        }
        return false;
    }

    /**
     * @param o        Object
     * @param provider Provider
     * @return boolean
     */
    public boolean addProvider(Object o, Provider provider)
    {
        //check for existence in
        //sensorProviders
        if (provider instanceof SensorProvider)
        {
            if (!hsSensorProviders.contains(provider))
            {
                return false;
            }
        }
        //transformers
        else if (provider instanceof Transformer)
        {
            boolean found = false;
            for (ContainerElement<Transformer> element : hsTransformerElements)
            {
                if (element.getElement().equals(provider))
                {
                    found = true;
                }
            }
            if (!found)
            {
                return false;
            }
        }
        //add to sensor
        if (o instanceof Sensor && provider instanceof SensorProvider)
        {
            for (ContainerElement<Sensor> element : hsSensorElements)
            {
                if (element.getElement().equals(o))
                {
                    return element.addProvider(provider);
                }
            }
        }
        //add to transformer
        if (o instanceof Transformer)
        {
            for (ContainerElement<Transformer> element : hsTransformerElements)
            {
                //prevent adding it to itself
                if (element.getElement().equals(o) && !element.getElement().equals(provider))
                {
                    return element.addProvider(provider);
                }
            }
        }
        //add to consumer
        else if (o instanceof Consumer)
        {
            for (ContainerElement<Consumer> element : hsConsumerElements)
            {
                if (element.getElement().equals(o))
                {
                    return element.addProvider(provider);
                }
            }
        }
        return false;
    }

    /**
     * @param o        Object
     * @param provider Provider
     * @return boolean
     */
    public boolean removeProvider(Object o, Provider provider)
    {
        //check for existence in
        //sensorProviders
        if (provider instanceof SensorProvider)
        {
            if (!hsSensorProviders.contains(provider))
            {
                return false;
            }
        }
        //transformers
        else if (provider instanceof Transformer)
        {
            boolean found = false;
            for (ContainerElement<Transformer> element : hsTransformerElements)
            {
                if (element.getElement().equals(provider))
                {
                    found = true;
                }
            }
            if (!found)
            {
                return false;
            }
        }
        //remove from sensor
        if (o instanceof Sensor && provider instanceof SensorProvider)
        {
            for (ContainerElement<Sensor> element : hsSensorElements)
            {
                if (element.getElement().equals(o))
                {
                    return element.removeProvider(provider);
                }
            }
        }
        //remove from transformer
        if (o instanceof Transformer)
        {
            for (ContainerElement<Transformer> element : hsTransformerElements)
            {
                if (element.getElement().equals(o))
                {
                    return element.removeProvider(provider);
                }
            }
        }
        //remove from consumer
        else if (o instanceof Consumer)
        {
            for (ContainerElement<Consumer> element : hsConsumerElements)
            {
                if (element.getElement().equals(o))
                {
                    return element.removeProvider(provider);
                }
            }
        }
        return false;
    }

    /**
     * @param o Object
     * @return double
     */
    public double getFrameSize(Object o)
    {
        if (o instanceof Transformer)
        {
            for (ContainerElement<Transformer> element : hsTransformerElements)
            {
                if (element.getElement().equals(o))
                {
                    return element.getFrameSize();
                }
            }
        } else if (o instanceof Consumer)
        {
            for (ContainerElement<Consumer> element : hsConsumerElements)
            {
                if (element.getElement().equals(o))
                {
                    return element.getFrameSize();
                }
            }
        }
        return 0;
    }

    /**
     * @param o         Object
     * @param frameSize double
     * @return boolean
     */
    public boolean setFrameSize(Object o, double frameSize)
    {
        if (o instanceof Transformer)
        {
            for (ContainerElement<Transformer> element : hsTransformerElements)
            {
                if (element.getElement().equals(o))
                {
                    element.setFrameSize(frameSize);
                    return true;
                }
            }
        } else if (o instanceof Consumer)
        {
            for (ContainerElement<Consumer> element : hsConsumerElements)
            {
                if (element.getElement().equals(o))
                {
                    element.setFrameSize(frameSize);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @param o Object
     * @return double
     */
    public double getDelta(Object o)
    {
        if (o instanceof Transformer)
        {
            for (ContainerElement<Transformer> element : hsTransformerElements)
            {
                if (element.getElement().equals(o))
                {
                    return element.getDelta();
                }
            }
        } else if (o instanceof Consumer)
        {
            for (ContainerElement<Consumer> element : hsConsumerElements)
            {
                if (element.getElement().equals(o))
                {
                    return element.getDelta();
                }
            }
        }
        return 0;
    }

    /**
     * @param o     Object
     * @param delta double
     * @return boolean
     */
    public boolean setDelta(Object o, double delta)
    {
        if (o instanceof Transformer)
        {
            for (ContainerElement<Transformer> element : hsTransformerElements)
            {
                if (element.getElement().equals(o))
                {
                    element.setDelta(delta);
                    return true;
                }
            }
        } else if (o instanceof Consumer)
        {
            for (ContainerElement<Consumer> element : hsConsumerElements)
            {
                if (element.getElement().equals(o))
                {
                    element.setDelta(delta);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @return int
     */
    public int getNumberOfConnections()
    {
        int number = 0;
        for (ContainerElement<Sensor> element : hsSensorElements)
        {
            number += element.getHmProviders().size();
        }
        for (ContainerElement<Transformer> element : hsTransformerElements)
        {
            number += element.getHmProviders().size();
        }
        for (ContainerElement<Consumer> element : hsConsumerElements)
        {
            number += element.getHmProviders().size();
        }
        return number;
    }

    /**
     * @param o Object
     * @return int[]
     */
    public int[] getConnectionHashes(Object o)
    {
        //sensor
        if (o instanceof Sensor)
        {
            for (ContainerElement<Sensor> element : hsSensorElements)
            {
                if (element.getElement().equals(o))
                {
                    Object[] objects = element.getHmProviders().keySet().toArray();
                    if (objects.length > 0)
                    {
                        int[] hashes = new int[objects.length];
                        for (int i = 0; i < objects.length; i++)
                        {
                            hashes[i] = objects[i].hashCode();
                        }
                        return hashes;
                    }
                    return null;
                }
            }
        }
        //transformer
        else if (o instanceof Transformer)
        {
            for (ContainerElement<Transformer> element : hsTransformerElements)
            {
                if (element.getElement().equals(o))
                {
                    Object[] objects = element.getHmProviders().keySet().toArray();
                    if (objects.length > 0)
                    {
                        int[] hashes = new int[objects.length];
                        for (int i = 0; i < objects.length; i++)
                        {
                            hashes[i] = objects[i].hashCode();
                        }
                        return hashes;
                    }
                    return null;
                }
            }
        }
        //consumer
        else if (o instanceof Consumer)
        {
            for (ContainerElement<Consumer> element : hsConsumerElements)
            {
                if (element.getElement().equals(o))
                {
                    Object[] objects = element.getHmProviders().keySet().toArray();
                    if (objects.length > 0)
                    {
                        int[] hashes = new int[objects.length];
                        for (int i = 0; i < objects.length; i++)
                        {
                            hashes[i] = objects[i].hashCode();
                        }
                        return hashes;
                    }
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * @param o Object
     * @return Option[]
     */
    public static Option[] getOptionList(Object o)
    {
        try
        {
            Field[] fields = o.getClass().getFields();
            for (Field field : fields)
            {
                Object obj = field.get(o);
                if (obj != null)
                {
                    if (obj instanceof OptionList)
                    {
                        return ((OptionList) obj).getOptions();
                    }
                }
            }
        } catch (SecurityException | IllegalArgumentException | IllegalAccessException ex)
        {
            throw new RuntimeException(ex);
        }
        return null;
    }
}
