/*
 * PipelineBuilder.java
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import hcm.ssj.core.Annotation;
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
import hcm.ssj.creator.core.container.FeedbackCollectionContainerElement;
import hcm.ssj.feedback.Feedback;
import hcm.ssj.feedback.FeedbackCollection;
import hcm.ssj.ml.IModelHandler;
import hcm.ssj.ml.Model;
import hcm.ssj.ml.Trainer;

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
	protected LinkedHashSet<ContainerElement<EventHandler>> hsEventHandlerElements = new LinkedHashSet<>();
	protected LinkedHashSet<ContainerElement<Model>> hsModelElements = new LinkedHashSet<>();

	protected Annotation anno;

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
	 * @param o Object
	 * @return Option[]
	 */
	public static Option[] getOptionList(Object o)
	{
		try
		{
			Method[] methods = o.getClass().getMethods();
			for (Method method : methods)
			{
				if (method.getName().equals("getOptions"))
				{
					OptionList list = (OptionList) method.invoke(o);
					if(list != null)
						return list.getOptions();
					else
						return null;
				}
			}
		}
		catch (SecurityException | IllegalArgumentException | IllegalAccessException | InvocationTargetException ex)
		{
			throw new RuntimeException(ex);
		}
		return null;
	}

	public FeedbackCollectionContainerElement getFeedbackCollectionContainerElement(FeedbackCollection feedbackCollection)
	{
		for(ContainerElement<EventHandler> containerElement : hsEventHandlerElements)
		{
			if(containerElement.getElement().equals(feedbackCollection))
			{
				return (FeedbackCollectionContainerElement) containerElement;
			}
		}
		return null;
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
		hsModelElements.clear();

		if(anno != null)
			anno.clear();
	}

	public Component getComponentForHash(int hash)
	{
		for (ContainerElement containerElement : hsSensorElements)
		{
			Sensor element = (Sensor) containerElement.getElement();
			if (element.hashCode() == hash)
			{
				return element;
			}
		}
		for (ContainerElement containerElement : hsSensorChannelElements)
		{
			SensorChannel element = (SensorChannel) containerElement.getElement();
			if (element.hashCode() == hash)
			{
				return element;
			}
		}
		for (ContainerElement containerElement : hsTransformerElements)
		{
			Transformer element = (Transformer) containerElement.getElement();
			if (element.hashCode() == hash)
			{
				return element;
			}
		}
		for (ContainerElement containerElement : hsConsumerElements)
		{
			Consumer element = (Consumer) containerElement.getElement();
			if (element.hashCode() == hash)
			{
				return element;
			}
		}
		for (ContainerElement containerElement : hsEventHandlerElements)
		{
			EventHandler element = (EventHandler) containerElement.getElement();
			if (element.hashCode() == hash)
			{
				return element;
			}
		}
		for (ContainerElement containerElement : hsModelElements)
		{
			Model element = (Model) containerElement.getElement();
			if (element.hashCode() == hash)
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
		}
		else if (o instanceof Transformer)
		{
			for (ContainerElement element : hsTransformerElements)
			{
				if (element.getElement().equals(o))
				{
					return element.getHmStreamProviders().keySet().toArray();
				}
			}
		}
		else if (o instanceof Consumer)
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

	public List<Component> getComponentsOfClass(Type type, Class componentClass)
	{
		List<Component> componentList = new ArrayList<>();
		Object[] componentsOfType = getAll(type);
		for (Object component : componentsOfType)
		{
			if (componentClass.isInstance(component))
			{
				componentList.add((Component) component);
			}
		}
		return componentList;
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
			case Model:
			{
				Object[] objects = new Object[hsModelElements.size()];
				int i = 0;
				for (ContainerElement element : hsModelElements)
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
		//models
		for (ContainerElement<Model> element : hsModelElements)
		{
			Model model = element.getElement();
			framework.addModel(model);

			HashMap<IModelHandler, Boolean> hmModelHandlers = element.getHmModelHandlers();
			if (hmModelHandlers.size() > 0)
			{
				for (Map.Entry<IModelHandler, Boolean> entry : hmModelHandlers.entrySet())
				{
					IModelHandler modelHandler = entry.getKey();
					modelHandler.setModel(model);
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
						Provider[] sources = new Provider[element.getHmStreamProviders().size()];
						element.getHmStreamProviders().keySet().toArray(sources);
						double frame = (element.getFrameSize() != null) ? element.getFrameSize() : sources[0].getOutputStream().num / sources[0].getOutputStream().sr;

						framework.addTransformer(element.getElement(), sources, frame, element.getDelta());
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
				}
				else
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
			EventChannel trigger = null;
			Object triggerSource = element.getEventTrigger();
			if(triggerSource != null && triggerSource instanceof Component)
				trigger = ((Component)triggerSource).getEventChannelOut();
			else if(triggerSource != null && triggerSource instanceof Annotation)
				trigger = ((Annotation)triggerSource).getChannel();

			if (element.getHmStreamProviders().size() > 0 && element.allStreamAdded())
			{
				Provider[] sources = new Provider[element.getHmStreamProviders().size()];
				element.getHmStreamProviders().keySet().toArray(sources);
				double frame = (element.getFrameSize() != null) ? element.getFrameSize() : sources[0].getOutputStream().num / sources[0].getOutputStream().sr;

				if (trigger != null)
				{
					framework.addConsumer(element.getElement(), sources, trigger);
				}
				else
				{
					framework.addConsumer(element.getElement(), sources, frame, element.getDelta());
					hsConsumerElementsNotTriggeredByEvent.add(element);
				}
			}

			//special case: Trainer
			if(element.getElement() instanceof Trainer)
			{
				Trainer trainer = (Trainer)element.getElement();
				getAnnotation().setClasses(trainer.getModel().getClassNames());
			}
		}

		buildEventPipeline(hsSensorElements);
		buildEventPipeline(hsSensorChannelElements);
		buildEventPipeline(hsTransformerElements);
		buildEventPipeline(hsConsumerElementsNotTriggeredByEvent);
		buildEventPipeline(hsEventHandlerElements);
	}

	private <T extends Component> void buildEventPipeline(LinkedHashSet<ContainerElement<T>> hsElements)
	{
		for (ContainerElement<T> element : hsElements)
		{
			if (!element.getHmEventProviders().isEmpty())
			{
				for (Component provider : element.getHmEventProviders().keySet())
				{
					Pipeline.getInstance().registerEventListener(element.getElement(), provider.getEventChannelOut());
				}
			}
		}

		// Register elements in feedback container AFTER all elements are registered!
		for (ContainerElement<T> element : hsElements)
		{
			if (element instanceof FeedbackCollectionContainerElement)
			{
				FeedbackCollection feedbackCollection = (FeedbackCollection) element.getElement();
				List<Map<Feedback, FeedbackCollection.LevelBehaviour>> feedbackList = ((FeedbackCollectionContainerElement) element).getFeedbackList();
				Pipeline.getInstance().registerInFeedbackCollection(feedbackCollection, feedbackList);
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
			if(o instanceof FeedbackCollection)
				return hsEventHandlerElements.add(new FeedbackCollectionContainerElement((FeedbackCollection) o));
			else
				return hsEventHandlerElements.add(new ContainerElement<>((EventHandler) o));
		}
		//Models
		else if (o instanceof Model)
		{
			for (ContainerElement<Model> element : hsModelElements)
			{
				if (element.getElement().equals(o))
				{
					return false;
				}
			}
			return hsModelElements.add(new ContainerElement<>((Model) o));
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
		//Models
		else if (o instanceof Model)
		{
			for (ContainerElement<Model> element : hsModelElements)
			{
				if (element.getElement().equals(o))
				{
					return hsModelElements.remove(element);
				}
			}
		}
		return false;
	}

	private void removeAllReferences(Component component)
	{
		for (ContainerElement<Sensor> sensor : hsSensorElements)
		{
			if (component instanceof Provider)
			{
				sensor.removeStreamProvider((Provider) component);
			}
			sensor.removeEventProvider(component);
		}
		for (ContainerElement<SensorChannel> sensorChannel : hsSensorChannelElements)
		{
			if (component instanceof Provider)
			{
				sensorChannel.removeStreamProvider((Provider) component);
			}
			sensorChannel.removeEventProvider(component);
		}
		for (ContainerElement<Transformer> transformer : hsTransformerElements)
		{
			if (component instanceof Provider)
			{
				transformer.removeStreamProvider((Provider) component);
			}
			transformer.removeEventProvider(component);
		}
		for (ContainerElement<Consumer> consumer : hsConsumerElements)
		{
			if (component instanceof Provider)
			{
				consumer.removeStreamProvider((Provider) component);
			}
			consumer.removeEventProvider(component);
		}
		for (ContainerElement<EventHandler> eventHandler : hsEventHandlerElements)
		{
			if (component instanceof Provider)
			{
				eventHandler.removeStreamProvider((Provider) component);
			}
			eventHandler.removeEventProvider(component);
		}
		for (ContainerElement<Model> model : hsModelElements)
		{
			if (component instanceof IModelHandler)
			{
				model.removeConnection((IModelHandler) component);
			}
		}
	}

	public void addFeedbackToCollectionContainer(FeedbackCollection feedbackCollection, Feedback feedback, int level, FeedbackCollection.LevelBehaviour levelBehaviour)
	{
		add(feedback);

		// Add to container
		for (ContainerElement<EventHandler> element : hsEventHandlerElements)
		{
			if (element.getElement().equals(feedbackCollection))
			{
				((FeedbackCollectionContainerElement) element).addFeedback(feedback, level, levelBehaviour);
			}
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
	 * @return Double
	 */
	public Double getFrameSize(Object o)
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
		}
		else if (o instanceof Consumer)
		{
			for (ContainerElement<Consumer> element : hsConsumerElements)
			{
				if (element.getElement().equals(o))
				{
					return element.getFrameSize();
				}
			}
		}
		return null;
	}

	/**
	 * @param o         Object
	 * @param frameSize Double
	 * @return boolean
	 */
	public boolean setFrameSize(Object o, Double frameSize)
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
		}
		else if (o instanceof Consumer)
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
		}
		else if (o instanceof Consumer)
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
		}
		else if (o instanceof Consumer)
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


	public boolean setEventTrigger(Object o, Object trigger)
	{
		if (o instanceof Consumer)
		{
			for (ContainerElement<Consumer> element : hsConsumerElements)
			{
				if (element.getElement().equals(o))
				{
					element.setEventTrigger(trigger);
					return true;
				}
			}
		}
		return false;
	}

	public Object getEventTrigger(Object o)
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
		return null;
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
		else if (o instanceof Transformer)
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
		}
		else if (o instanceof SensorChannel)
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
		}
		else if (o instanceof Consumer)
		{
			for (ContainerElement element : hsConsumerElements)
			{
				if (element.getElement().equals(o))
				{
					return element.getHmEventProviders().keySet().toArray();
				}
			}
		}
		else if (o instanceof EventHandler)
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

	public boolean isManagedFeedback(Object element)
	{
		if (!(element instanceof Feedback))
		{
			return false;
		}

		for (ContainerElement<EventHandler> hsEventHandlerElement : hsEventHandlerElements)
		{
			if(hsEventHandlerElement instanceof FeedbackCollectionContainerElement)
			{
				for (Map<Feedback, FeedbackCollection.LevelBehaviour> feedbackLevelBehaviourMap : ((FeedbackCollectionContainerElement) hsEventHandlerElement).getFeedbackList())
				{
					if (feedbackLevelBehaviourMap.containsKey(element))
					{
						return true;
					}
				}
			}
		}

		return false;
	}

	/**
	 * @param start Component
	 * @param end Component
	 * @return boolean
	 */
	public boolean addModelConnection(Component start, Component end)
	{
		if (start instanceof IModelHandler && end instanceof Model)
		{
			for (ContainerElement<Model> element : hsModelElements)
			{
				if (element.getElement().equals(end))
				{
					return element.addConnection((IModelHandler) start);
				}
			}
		}
		//add to sensor
		if (start instanceof Model && end instanceof IModelHandler)
		{
			for (ContainerElement<Model> element : hsModelElements)
			{
				if (element.getElement().equals(start))
				{
					return element.addConnection((IModelHandler) end);
				}
			}
		}
		return false;
	}

	/**
	 * @param start Component
	 * @param end Component
	 * @return boolean
	 */
	public boolean removeModelConnection(Component start, Component end)
	{
		if (start instanceof IModelHandler && end instanceof Model)
		{
			for (ContainerElement<Model> element : hsModelElements)
			{
				if (element.getElement().equals(end))
				{
					return element.removeConnection((IModelHandler) start);
				}
			}
		}
		//add to sensor
		if (start instanceof Model && end instanceof IModelHandler)
		{
			for (ContainerElement<Model> element : hsModelElements)
			{
				if (element.getElement().equals(start))
				{
					return element.removeConnection((IModelHandler) end);
				}
			}
		}
		return false;
	}

	/**
	 * @param o Object
	 * @return Object[]
	 */
	public Object[] getModelConnections(Object o)
	{
		if (o instanceof Model)
		{
			for (ContainerElement<Model> element : hsModelElements)
			{
				if (element.getElement().equals(o))
				{
					return element.getHmModelHandlers().keySet().toArray();
				}
			}
		}
		if (o instanceof IModelHandler)
		{
			ArrayList<Model> connections = new ArrayList<>();

			for (ContainerElement<Model> element : hsModelElements)
			{
				for(IModelHandler mh : element.getHmModelHandlers().keySet())
				{
					if (mh.equals(o))
					{
						connections.add(element.getElement());
					}
				}
			}
			return connections.toArray();
		}
		return null;
	}

	/**
	 * @return int
	 */
	public int getNumberOfModelConnections()
	{
		int number = 0;
		for (ContainerElement<Model> element : hsModelElements)
		{
			number += element.getHmModelHandlers().size();
		}
		return number;
	}

	public int[] getModelConnectionHashes(Object o)
	{
		//model
		if (o instanceof Model)
		{
			for (ContainerElement<Model> element : hsModelElements)
			{
				if (element.getElement().equals(o))
				{
					Object[] objects = element.getHmModelHandlers().keySet().toArray();
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

	public enum Type
	{
		Sensor, SensorChannel, Transformer, Consumer, EventHandler, Model, ModelHandler
	}

	/**
	 * @return Object[]
	 */
	public Object[] getPossibleEventInputs(Object object)
	{
		//add possible providers
		ArrayList<Object> alCandidates = new ArrayList<>();
		alCandidates.addAll(Arrays.asList(getAll(PipelineBuilder.Type.Sensor)));
		alCandidates.addAll(Arrays.asList(getAll(PipelineBuilder.Type.SensorChannel)));
		alCandidates.addAll(Arrays.asList(getAll(PipelineBuilder.Type.Transformer)));
		alCandidates.addAll(Arrays.asList(getAll(PipelineBuilder.Type.Consumer)));
		alCandidates.addAll(Arrays.asList(getAll(PipelineBuilder.Type.EventHandler)));

		//remove oneself
		alCandidates.remove(object);

		return alCandidates.toArray();
	}

	/**
	 * @return Object[]
	 */
	public Object[] getModelHandlers()
	{
		//add possible providers
		ArrayList<Object> alCandidates = new ArrayList<>();
		alCandidates.addAll(PipelineBuilder.getInstance().getComponentsOfClass(PipelineBuilder.Type.Consumer, IModelHandler.class));
		alCandidates.addAll(PipelineBuilder.getInstance().getComponentsOfClass(PipelineBuilder.Type.Transformer, IModelHandler.class));

		return alCandidates.toArray();
	}

	/**
	 * @return Object[]
	 */
	public Object[] getPossibleStreamInputs(Object object)
	{
		//add possible providers
		Object[] sensProvCandidates = getAll(PipelineBuilder.Type.SensorChannel);
		ArrayList<Object> alCandidates = new ArrayList<>();
		//only add sensorChannels for sensors
		if (!(object instanceof Sensor))
		{
			alCandidates.addAll(Arrays.asList(getAll(PipelineBuilder.Type.Transformer)));
			//remove oneself
			for (int i = 0; i < alCandidates.size(); i++)
			{
				if (object.equals(alCandidates.get(i)))
				{
					alCandidates.remove(i);
				}
			}
		}
		alCandidates.addAll(0, Arrays.asList(sensProvCandidates));
		return alCandidates.toArray();
	}

	public synchronized boolean annotationExists()
	{
		return anno != null;
	}

	public synchronized Annotation getAnnotation()
	{
		if(anno == null)
			anno = new Annotation();

		return anno;
	}
}
