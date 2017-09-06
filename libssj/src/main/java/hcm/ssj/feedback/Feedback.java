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
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;

/**
 * Created by Antonio Grieco on 06.09.2017.
 */

public abstract class Feedback extends EventHandler
{
	protected long lastExecutionTime = 0;

	public class Options extends OptionList
	{
		public final Option<Integer> lock = new Option<>("lock", 0, Integer.class, "lock time in ms");
		protected Options()
		{
			addOptions();
		}
	}

	protected boolean checkLock(Integer lock)
	{
		if (System.currentTimeMillis() - lastExecutionTime < lock)
		{
			Log.i("ignoring event, lock active for another " + (lock - (System.currentTimeMillis() - lastExecutionTime)) + "ms");
			return false;
		}
		else{
			lastExecutionTime = System.currentTimeMillis();
			return true;
		}
	}
}
