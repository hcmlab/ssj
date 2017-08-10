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

package hcm.ssj.creator.core;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import hcm.ssj.core.Component;
import hcm.ssj.core.Consumer;
import hcm.ssj.core.EventChannel;
import hcm.ssj.core.EventHandler;
import hcm.ssj.core.Pipeline;
import hcm.ssj.core.Provider;
import hcm.ssj.core.SSJException;
import hcm.ssj.core.Sensor;
import hcm.ssj.core.SensorChannel;
import hcm.ssj.core.Transformer;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.creator.core.container.ContainerElement;

/**
 * Linker for a pipeline.<br>
 * Created by Frank Gaibler on 09.03.2016.
 */
public class PipelineBuilder
{
    private static PipelineBuilder instance = null;
    protected LinkedHashSet<ContainerElement<SensorChannel>> hsSensorChannelElements = new LinkedHashSet<>();
    protected LinkedHashSet<ContainerElement<Sensor>> hsSensorElements = new LinkedHashSet<>();
    protected LinkedHashSet<ContainerElement<Transformer>> hsTransformerElements = new LinkedHashSet<>();
    protected LinkedHashSet<ContainerElement<Consumer>> hsConsumerElements = new LinkedHashSet<>();
    protected LinkedHashSet<ContainerElement<EventHandler>> hsEventHandlerElements = new LinkedHashSet<>(); //@todo doesn't work yet, because of EventChannel and optional frame size

    public enum Type
    {
        Sensor, SensorChannel, Transformer, Consumer, EventHandler
    }

    /**
     *
     */
    private PipelineBuilder()
    {
    }

    /**
     * @return Linker
     */
    public static synchronized PipelineBuilder getInstance()
    {
        if (instance == null)
        {
            instance = new PipelineBuilder();
        }
        return instance;
    }

    /**
     *
     */
    public void clear()
    {
        hsSensorChannelElements.clear();
        hsSensorElements.clear();
        hsTransformerElements.clear();
        hsConsumerElements.clear();
        hsEventHandlerElements.clear();
    }

    public Component getComponentForHash(int hash)
    {
        for(ContainerElement containerElement : hsSensorElements)
        {
            Sensor element = (Sensor)containerElement.getElement();
            if(element.hashCode() == hash)
            {
                return element;
            }
        }
        for(ContainerElement containerElement : hsSensorChannelElements)
        {
            SensorChannel element = (SensorChannel)containerElement.getElement();
            if(element.hashCode() == hash)
            {
                return element;
            }
        }
        for(ContainerElement containerElement : hsTransformerElements)
        {
            Transformer element = (Transformer)containerElement.getElement();
            if(element.hashCode() == hash)
            {
                return element;
            }
        }
        for(ContainerElement containerElement : hsConsumerElements)
        {
            Consumer element = (Consumer)containerElement.getElement();
            if(element.hashCode() == hash)
            {
                return element;
            }
        }
        for(ContainerElement containerElement : hsEventHandlerElements)
        {
            EventHandler element = (EventHandler)containerElement.getElement();
            if(element.hashCode() == hash)
            {
                return element;
            }
        }
        return null;
    }

    /**
     * @param o Object
     * @return Object[]
     */
    public Object[] getStreamProviders(Object o)
    {
        if (o instanceof Sensor)
        {
            for (ContainerElement element : hsSensorElements)
            {
                if (element.getElement().equals(o))
                {
                    return element.getHmStreamProviders().keySet().toArray();
                }
            }
        } else if (o instanceof Transformer)
        {
            for (ContainerElement element : hsTransformerElements)
            {
                if (element.getElement().equals(o))
                {
                    return element.getHmStreamProviders().keySet().toArray();
                }
            }
        } else if (o instanceof Consumer)
        {
            for (ContainerElement element : hsConsumerElements)
            {
                if (element.getElement().equals(o))
                {
                    return element.getHmStreamProviders().keySet().toArray();
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
            case SensorChannel:
            {
                Object[] objects = new Object[hsSensorChannelElements.size()];
                int i = 0;
                for (ContainerElement element : hsSensorChannelElements)
                {
                    objects[i] = element.getElement();
                    i++;
                }
                return objects;
            }
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
            case EventHandler:
            {
                Object[] objects = new Object[hsEventHandlerElements.size()];
                int i = 0;
                for (ContainerElement element : hsEventHandlerElements)
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
    public void buildPipe() throws SSJException
    {
        Pipeline framework = Pipeline.getInstance();
        //add to framework
        //sensors and sensorChannels
        for (ContainerElement<Sensor> element : hsSensorElements)
        {
            Sensor sensor = element.getElement();
            HashMap<Provider, Boolean> hmStreamProviders = element.getHmStreamProviders();
            if (hmStreamProviders.size() > 0)
            {
                for (Map.Entry<Provider, Boolean> entry : hmStreamProviders.entrySet())
                {
                    SensorChannel sensorChannel = (SensorChannel) entry.getKey();
                    framework.addSensor(sensor, sensorChannel);
                    //activate in transformer
                    for (ContainerElement<Transformer> element2 : hsTransformerElements)
                    {
                        element2.setStreamAdded(sensorChannel);
                    }
                    //activate in consumer
                    for (ContainerElement<Consumer> element2 : hsConsumerElements)
                    {
                        element2.setStreamAdded(sensorChannel);
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
                if (element.getHmStreamProviders().size() > 0)
                {
                    if (element.allStreamAdded())
                    {
                        framework.addTransformer(element.getElement(),
                                element.getHmStreamProviders().keySet().toArray(new Provider[element.getHmStreamProviders().size()]),
                                element.getFrameSize(), element.getDelta());
                        count++;
                        //activate in transformer
                        for (ContainerElement<Transformer> element2 : hsTransformerElements)
                        {
                            element2.setStreamAdded(element.getElement());
                        }
                        //activate in consumer
                        for (ContainerElement<Consumer> element2 : hsConsumerElements)
                        {
                            element2.setStreamAdded(element.getElement());
                        }
                    }
                } else
                {
                    count++;
                }
            }
        }
        //consumers
        //Avoid reregistering consumers as eventlisteners if they are triggered by event
        LinkedHashSet<ContainerElement<Consumer>> hsConsumerElementsNotTriggeredByEvent = new LinkedHashSet<>();
        for (ContainerElement<Consumer> element : hsConsumerElements)
        {
            element.getElement().setTriggeredByEvent(element.getEventTrigger());

            if (element.getHmStreamProviders().size() > 0 && element.allStreamAdded())
            {
                if(element.getEventTrigger()){
                    List<EventChannel> eventChannels = new ArrayList<>();
                    for(Component c : element.getHmEventProviders().keySet())
                    {
                        eventChannels.add(c.getEventChannelOut());
                    }
                    framework.addConsumer(element.getElement(),
                                          element.getHmStreamProviders().keySet().toArray(new Provider[element.getHmStreamProviders().size()]),
                                          eventChannels.toArray(new EventChannel[eventChannels.size()]));
                }
                else{
                    framework.addConsumer(element.getElement(),
                                          element.getHmStreamProviders().keySet().toArray(new Provider[element.getHmStreamProviders().size()]),
                                          element.getFrameSize(), element.getDelta());
                    hsConsumerElementsNotTriggeredByEvent.add(element);
                }
            }
        }

        buildEventPipeline(framework, hsSensorElements);
        buildEventPipeline(framework, hsSensorChannelElements);
        buildEventPipeline(framework, hsTransformerElements);
        buildEventPipeline(framework, hsConsumerElementsNotTriggeredByEvent);
        buildEventPipeline(framework, hsEventHandlerElements);
    }

    private<T extends Component> void buildEventPipeline(Pipeline framework, LinkedHashSet<ContainerElement<T>> hsElements)
    {
        for(ContainerElement<T> element : hsElements)
        {
            if (!element.getHmEventProviders().isEmpty())
            {
                for(Component provider : element.getHmEventProviders().keySet())
                {
                    framework.registerEventListener(element.getElement(), provider.getEventChannelOut());
                }
            }
        }
    }

    /**
     * @param o Object
     * @return boolean
     */
    public boolean add(Object o)
    {
        //sensorChannel
        if (o instanceof SensorChannel)
        {
            for (ContainerElement<SensorChannel> element : hsSensorChannelElements)
            {
                if (element.getElement().equals(o))
                {
                    return false;
                }
            }
            return hsSensorChannelElements.add(new ContainerElement<>((SensorChannel) o));
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
        //eventhandler
        else if (o instanceof EventHandler)
        {
            for (ContainerElement<EventHandler> element : hsEventHandlerElements)
            {
                if (element.getElement().equals(o))
                {
                    return false;
                }
            }
            return hsEventHandlerElements.add(new ContainerElement<>((EventHandler) o));
        }
        return false;
    }

    /**
     * @param o Object
     * @return boolean
     */
    public boolean remove(Object o)
    {

        removeAllReferences((Component) o);

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
        //sensorChannel
        else if (o instanceof SensorChannel)
        {
            for (ContainerElement<SensorChannel> element : hsSensorChannelElements)
            {
                if (element.getElement().equals(o))
                {
                    return hsSensorChannelElements.remove(element);
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
        //EventHandler
        else if (o instanceof EventHandler)
        {
            for (ContainerElement<EventHandler> element : hsEventHandlerElements)
            {
                if (element.getElement().equals(o))
                {
                    return hsEventHandlerElements.remove(element);
                }
            }
        }
        return false;
    }

    private void removeAllReferences(Component component)
    {
        for (ContainerElement<Sensor> sensor : hsSensorElements)
        {
            if(component instanceof Provider)
            {
                sensor.removeStreamProvider((Provider) component);
            }
            sensor.removeEventProvider(component);
        }
        for (ContainerElement<SensorChannel> sensorChannel : hsSensorChannelElements)
        {
            if(component instanceof Provider)
            {
                sensorChannel.removeStreamProvider((Provider) component);
            }
            sensorChannel.removeEventProvider(component);
        }
        for (ContainerElement<Transformer> transformer : hsTransformerElements)
        {
            if(component instanceof Provider)
            {
                transformer.removeStreamProvider((Provider) component);
            }
            transformer.removeEventProvider(component);
        }
        for (ContainerElement<Consumer> consumer : hsConsumerElements)
        {
            if(component instanceof Provider)
            {
                consumer.removeStreamProvider((Provider) component);
            }
            consumer.removeEventProvider(component);
        }
        for (ContainerElement<EventHandler> eventHandler : hsEventHandlerElements)
        {
            if(component instanceof Provider)
            {
                eventHandler.removeStreamProvider((Provider) component);
            }
            eventHandler.removeEventProvider(component);
        }
    }

    /**
     * @param o        Object
     * @param provider Provider
     * @return boolean
     */
    public boolean addStreamProvider(Object o, Provider provider)
    {
        //check for existence in
        //sensorChannels
        if (provider instanceof SensorChannel)
        {
            boolean found = false;
            for (ContainerElement<SensorChannel> element : hsSensorChannelElements)
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
        if (o instanceof Sensor && provider instanceof SensorChannel)
        {
            for (ContainerElement<Sensor> element : hsSensorElements)
            {
                if (element.getElement().equals(o))
                {
                    return element.addStreamProvider(provider);
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
                    return element.addStreamProvider(provider);
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
                    return element.addStreamProvider(provider);
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
    public boolean removeStreamProvider(Object o, Provider provider)
    {
        //check for existence in
        //sensorChannels
        if (provider instanceof SensorChannel)
        {
            boolean found = false;
            for (ContainerElement<SensorChannel> element : hsSensorChannelElements)
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
        if (o instanceof Sensor && provider instanceof SensorChannel)
        {
            for (ContainerElement<Sensor> element : hsSensorElements)
            {
                if (element.getElement().equals(o))
                {
                    return element.removeStreamProvider(provider);
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
                    return element.removeStreamProvider(provider);
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
                    return element.removeStreamProvider(provider);
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


    public boolean setEventTrigger(Object o, boolean eventTrigger)
    {
        if(o instanceof Consumer)
        {
            for (ContainerElement<Consumer> element : hsConsumerElements)
            {
                if (element.getElement().equals(o))
                {
                    element.setEventTrigger(eventTrigger);
                    return true;
                }
            }
        }
        return false;
    }

    public boolean getEventTrigger(Object o)
    {
        if (o instanceof Consumer)
        {
            for (ContainerElement<Consumer> element : hsConsumerElements)
            {
                if (element.getElement().equals(o))
                {
                    return element.getEventTrigger();
                }
            }
        }
        return false;
    }

    /**
     * @return int
     */
    public int getNumberOfStreamConnections()
    {
        int number = 0;
        for (ContainerElement<Sensor> element : hsSensorElements)
        {
            number += element.getHmStreamProviders().size();
        }
        for (ContainerElement<Transformer> element : hsTransformerElements)
        {
            number += element.getHmStreamProviders().size();
        }
        for (ContainerElement<Consumer> element : hsConsumerElements)
        {
            number += element.getHmStreamProviders().size();
        }
        return number;
    }

    /**
     * @param o Object
     * @return int[]
     */
    public int[] getStreamConnectionHashes(Object o)
    {
        //sensor
        if (o instanceof Sensor)
        {
            for (ContainerElement<Sensor> element : hsSensorElements)
            {
                if (element.getElement().equals(o))
                {
                    Object[] objects = element.getHmStreamProviders().keySet().toArray();
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
                    Object[] objects = element.getHmStreamProviders().keySet().toArray();
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
                    Object[] objects = element.getHmStreamProviders().keySet().toArray();
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

    /**
     * @param o        Object
     * @param provider Provider
     * @return boolean
     */
    public boolean addEventProvider(Object o, Component provider)
    {
        //check for existence in
        //sensorChannels
        if (o instanceof SensorChannel)
        {
            for (ContainerElement<SensorChannel> element : hsSensorChannelElements)
            {
                if (element.getElement().equals(o))
                {
                    return element.addEventProvider(provider);
                }
            }
        }
        //add to sensor
        if (o instanceof Sensor /*&& provider instanceof SensorChannel*/)
        {
            for (ContainerElement<Sensor> element : hsSensorElements)
            {
                if (element.getElement().equals(o))
                {
                    return element.addEventProvider(provider);
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
                    return element.addEventProvider(provider);
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
                    return element.addEventProvider(provider);
                }
            }
        }
        //add to eventhandler
        else if (o instanceof EventHandler)
        {
            for (ContainerElement<EventHandler> element : hsEventHandlerElements)
            {
                if (element.getElement().equals(o))
                {
                    return element.addEventProvider(provider);
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
    public boolean removeEventProvider(Object o, Component provider)
    {
        //check for existence in
        //sensorChannels
        if (o instanceof SensorChannel)
        {
            for (ContainerElement<SensorChannel> element : hsSensorChannelElements)
            {
                if (element.getElement().equals(o))
                {
                    return element.removeEventProvider(provider);
                }
            }
        }
        //transformers
        else  if (o instanceof Transformer)
        {
            for (ContainerElement<Transformer> element : hsTransformerElements)
            {
                if (element.getElement().equals(o))
                {
                    return element.removeEventProvider(provider);
                }
            }
        }
        //remove from sensor
        if (o instanceof Sensor /*&& provider instanceof SensorChannel*/)
        {
            for (ContainerElement<Sensor> element : hsSensorElements)
            {
                if (element.getElement().equals(o))
                {
                    return element.removeEventProvider(provider);
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
                    return element.removeEventProvider(provider);
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
                    return element.removeEventProvider(provider);
                }
            }
        }
        //remove from eventhandler
        else if (o instanceof EventHandler)
        {
            for (ContainerElement<EventHandler> element : hsEventHandlerElements)
            {
                if (element.getElement().equals(o))
                {
                    return element.removeEventProvider(provider);
                }
            }
        }
        return false;
    }

    /**
     * @return int
     */
    public int getNumberOfEventConnections()
    {
        int number = 0;
        for (ContainerElement<Sensor> element : hsSensorElements)
        {
            number += element.getHmEventProviders().size();
        }
        for (ContainerElement<SensorChannel> element : hsSensorChannelElements)
        {
            number += element.getHmEventProviders().size();
        }
        for (ContainerElement<Transformer> element : hsTransformerElements)
        {
            number += element.getHmEventProviders().size();
        }
        for (ContainerElement<Consumer> element : hsConsumerElements)
        {
            number += element.getHmEventProviders().size();
        }
        for (ContainerElement<EventHandler> element : hsEventHandlerElements)
        {
            number += element.getHmEventProviders().size();
        }
        return number;
    }

	/**
	 * @param o Object
	 * @return Object[]
	 */
	public Object[] getEventProviders(Object o)
	{
		if (o instanceof Sensor)
		{
			for (ContainerElement element : hsSensorElements)
			{
				if (element.getElement().equals(o))
				{
					return element.getHmEventProviders().keySet().toArray();
				}
			}
		} else if (o instanceof SensorChannel)
        {
            for (ContainerElement element : hsSensorChannelElements)
            {
                if (element.getElement().equals(o))
                {
                    return element.getHmEventProviders().keySet().toArray();
                }
            }
        }
		else if (o instanceof Transformer)
		{
			for (ContainerElement element : hsTransformerElements)
			{
				if (element.getElement().equals(o))
				{
					return element.getHmEventProviders().keySet().toArray();
				}
			}
		} else if (o instanceof Consumer)
		{
			for (ContainerElement element : hsConsumerElements)
			{
				if (element.getElement().equals(o))
				{
					return element.getHmEventProviders().keySet().toArray();
				}
			}
		} else if (o instanceof EventHandler)
        {
            for (ContainerElement element : hsEventHandlerElements)
            {
                if (element.getElement().equals(o))
                {
                    return element.getHmEventProviders().keySet().toArray();
                }
            }
        }
		return null;
	}
	/**
	 * @param o Object
	 * @return int[]
	 */
	public int[] getEventConnectionHashes(Object o)
	{
		//sensor
		if (o instanceof Sensor)
		{
			for (ContainerElement<Sensor> element : hsSensorElements)
			{
				if (element.getElement().equals(o))
				{
					Object[] objects = element.getHmEventProviders().keySet().toArray();
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
		//sensor channel
        else if (o instanceof SensorChannel)
        {
            for (ContainerElement<SensorChannel> element : hsSensorChannelElements)
            {
                if (element.getElement().equals(o))
                {
                    Object[] objects = element.getHmEventProviders().keySet().toArray();
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
					Object[] objects = element.getHmEventProviders().keySet().toArray();
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
					Object[] objects = element.getHmEventProviders().keySet().toArray();
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
        //eventhandler
        else if (o instanceof EventHandler)
        {
            for (ContainerElement<EventHandler> element : hsEventHandlerElements)
            {
                if (element.getElement().equals(o))
                {
                    Object[] objects = element.getHmEventProviders().keySet().toArray();
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
}
