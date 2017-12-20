/*
 * ContainerElement.java
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

package hcm.ssj.creator.core.container;

import java.util.LinkedHashSet;

import hcm.ssj.core.Component;
import hcm.ssj.core.Provider;
import hcm.ssj.ml.IModelHandler;

/**
 * Container element for pipeline builder.<br>
 * Created by Frank Gaibler on 10.03.2016.
 */
public class ContainerElement<T>
{
    private T element;
    private Double frameSize = null;
    private double delta = 0;
	private Object eventTrigger = null;

	private boolean added;

	private LinkedHashSet<ContainerElement<Provider>> hsStreamConnections = new LinkedHashSet<>();
    private LinkedHashSet<ContainerElement<Component>> hsEventConnections = new LinkedHashSet<>();
	private LinkedHashSet<ContainerElement<IModelHandler>> hsModelConnections = new LinkedHashSet<>();

    /**
     * @param element T
     */
    public ContainerElement(T element)
    {
        this.element = element;
    }

    /**
     * @return T
     */
    public T getElement()
    {
        return element;
    }

    /**
     * @return Double
     */
    public Double getFrameSize()
    {
        return frameSize;
    }

    /**
     * @param frameSize double
     */
    public void setFrameSize(Double frameSize)
    {
        this.frameSize = frameSize;
    }

    /**
     * @return double
     */
    public double getDelta()
    {
        return delta;
    }

    /**
     * @param delta double
     */
    public void setDelta(double delta)
    {
        this.delta = delta;
    }

    public void setEventTrigger(Object eventTrigger)
	{
		this.eventTrigger = eventTrigger;
	}

	public Object getEventTrigger()
	{
		return eventTrigger;
	}

	public void setAdded(boolean value)
	{
		added = value;
	}

	public boolean hasBeenAdded()
	{
		return added;
	}

	public void reset()
	{
		added = false;
	}

	/**
     * @return LinkedHashMap
     */
    public LinkedHashSet<ContainerElement<Provider>> getStreamConnectionContainers()
    {
        return hsStreamConnections;
    }

	public Provider[] getStreamConnections()
	{
		Provider[] providers = new Provider[hsStreamConnections.size()];

		int i = 0;
		for (ContainerElement<Provider> element : hsStreamConnections)
		{
			providers[i++] = element.getElement();
		}

		return providers;
	}

    /**
     * @return boolean
     */
    public boolean allStreamConnectionsAdded()
    {
    	boolean allAdded = true;
		for (ContainerElement<?> element : hsStreamConnections)
		{
			allAdded &= element.hasBeenAdded();
		}
		return allAdded;
    }

    /**
     * @param element ContainerElement
     * @return boolean
     */
    public boolean addStreamConnection(ContainerElement<Provider> element)
    {
        return !hsStreamConnections.contains(element) && hsStreamConnections.add(element);
    }

    /**
     * @param element ContainerElement
     * @return boolean
     */
    public boolean removeStreamConnection(ContainerElement<Provider> element)
    {
        return hsStreamConnections.remove(element);
    }

	/**
	 * @return LinkedHashMap
	 */
	public LinkedHashSet<ContainerElement<Component>> getEventConnectionContainers()
	{
		return hsEventConnections;
	}

	public Component[] getEventConnections()
	{
		Component[] components = new Component[hsEventConnections.size()];

		int i = 0;
		for (ContainerElement<Component> element : hsEventConnections)
		{
			components[i++] = element.getElement();
		}

		return components;
	}

	/**
	 * @param provider Provider
	 * @return boolean
	 */
	public boolean addEventConnection(ContainerElement<Component> provider)
	{
		return !hsEventConnections.contains(provider) && hsEventConnections.add(provider);
	}

	/**
	 * @param provider Provider
	 * @return boolean
	 */
	public boolean removeEventConnection(ContainerElement<Component> provider)
	{
		return hsEventConnections.remove(provider);
	}

	/**
	 * @return LinkedHashMap
	 */
	public LinkedHashSet<ContainerElement<IModelHandler>> getModelConnectionContainers()
	{
		return hsModelConnections;
	}

	public IModelHandler[] getModelConnections()
	{
		IModelHandler[] handlers = new IModelHandler[hsModelConnections.size()];

		int i = 0;
		for (ContainerElement<IModelHandler> element : hsModelConnections)
		{
			handlers[i++] = element.getElement();
		}

		return handlers;
	}

	/**
	 * @param handler IModelHandler
	 * @return boolean
	 */
	public boolean addModelConnection(ContainerElement<IModelHandler> handler)
	{
		return !hsModelConnections.contains(handler) && hsModelConnections.add(handler);
	}

	/**
	 * @param handler IModelHandler
	 * @return boolean
	 */
	public boolean removeModelConnection(ContainerElement<IModelHandler> handler)
	{
		return hsModelConnections.remove(handler);
	}
}
