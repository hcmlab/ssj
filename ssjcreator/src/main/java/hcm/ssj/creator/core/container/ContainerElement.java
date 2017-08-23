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

import java.util.LinkedHashMap;

import hcm.ssj.core.Component;
import hcm.ssj.core.Provider;

/**
 * Container element for pipeline builder.<br>
 * Created by Frank Gaibler on 10.03.2016.
 */
public class ContainerElement<T>
{
    private T element;
    private Double frameSize = null;
    private double delta = 0;
	private boolean eventTrigger = false;
    private LinkedHashMap<Provider, Boolean> hmStreamProviders = new LinkedHashMap<>();
    private LinkedHashMap<Component, Boolean> hmEventProviders = new LinkedHashMap<>();


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

    public void setEventTrigger(boolean eventTrigger)
	{
		this.eventTrigger = eventTrigger;
	}

	public boolean getEventTrigger()
	{
		return eventTrigger;
	}

    /**
     * @return LinkedHashMap
     */
    public LinkedHashMap<Provider, Boolean> getHmStreamProviders()
    {
        return hmStreamProviders;
    }

    /**
     * @param provider Provider
     * @return boolean
     */
    public boolean setStreamAdded(Provider provider)
    {
        return hmStreamProviders.containsKey(provider) && !hmStreamProviders.put(provider, true);
    }

    /**
     * @return boolean
     */
    public boolean allStreamAdded()
    {
        for (boolean value : hmStreamProviders.values())
        {
            if (!value)
            {
                return false;
            }
        }
        return true;
    }

    /**
     * @param provider Provider
     * @return boolean
     */
    public boolean addStreamProvider(Provider provider)
    {
        return !hmStreamProviders.containsKey(provider) && hmStreamProviders.put(provider, false) == null;
    }

    /**
     * @param provider Provider
     * @return boolean
     */
    public boolean removeStreamProvider(Provider provider)
    {
        return hmStreamProviders.remove(provider) != null;
    }

	/**
	 * @return LinkedHashMap
	 */
	public LinkedHashMap<Component, Boolean> getHmEventProviders()
	{
		return hmEventProviders;
	}

	/**
	 * @param provider Provider
	 * @return boolean
	 */
	public boolean addEventProvider(Component provider)
	{
		return !hmEventProviders.containsKey(provider) && hmEventProviders.put(provider, false) == null;
	}

	/**
	 * @param provider Provider
	 * @return boolean
	 */
	public boolean removeEventProvider(Component provider)
	{
		return hmEventProviders.remove(provider) != null;
	}
}
