/*
 * Feedback.java
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

package hcm.ssj.feedback;

import hcm.ssj.core.EventHandler;
import hcm.ssj.core.Log;
import hcm.ssj.core.event.Event;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;


/**
 * Created by Antonio Grieco on 06.09.2017.
 */

public abstract class Feedback extends EventHandler
{

	private long lastExecutionTime = 0;
	private boolean active = true;

	private boolean checkLock()
	{
		if (System.currentTimeMillis() - lastExecutionTime < getOptions().lock.get())
		{
			Log.i("ignoring event, lock active for another " +
						  (getOptions().lock.get() - (System.currentTimeMillis() - lastExecutionTime)) +
						  "ms");
			return false;
		}
		else
		{
			lastExecutionTime = System.currentTimeMillis();
			return true;
		}
	}

	@Override
	protected final void enter()
	{
		lastExecutionTime = 0;
		enterFeedback();
	}

	protected abstract void enterFeedback();

	public boolean isActive()
	{
		return active;
	}

	protected void setActive(boolean active)
	{
		this.active = active;
	}

	long getLastExecutionTime()
	{
		return lastExecutionTime;
	}

	private boolean activatedByEventName(String eventName)
	{
		// Allways activate if no eventnames are specified
		if (getOptions().eventNames.get() == null || getOptions().eventNames.get().length == 0)
		{
			return true;
		}
		for (String eventNameToActivate : getOptions().eventNames.get())
		{
			if (eventNameToActivate.equals(eventName))
			{
				return true;
			}
		}
		return false;
	}

	@Override
	public void notify(Event event)
	{
		if (active && activatedByEventName(event.name) && checkLock())
		{
			notifyFeedback(event);
		}
	}

	public abstract Options getOptions();

	public abstract void notifyFeedback(Event event);

	public class Options extends OptionList
	{

		public final Option<Integer> lock = new Option<>("lock", 0, Integer.class, "lock time in ms");
		public final Option<String[]> eventNames = new Option<>("eventNames", null, String[].class, "event names to listen on");

		protected Options()
		{
			addOptions();
		}
	}
}
