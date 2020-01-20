/*
 * LimitedSizeQueue.java
 * Copyright (c) 2020
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

package hcm.ssj.core;

import java.util.ArrayList;

/**
 * Custom implementation of a LIFO queue with fixed size.
 *
 * @see <a href="https://stackoverflow.com/questions/1963806/is-there-a-fixed-sized-queue-which-removes-excessive-elements">source</a>.
 *
 * Created by Michael Dietz on 09.01.2020.
 */
public class LimitedSizeQueue<K> extends ArrayList<K>
{
	private int maxSize;

	public LimitedSizeQueue(int size)
	{
		this.maxSize = size;
	}

	public boolean add(K k)
	{
		boolean r = super.add(k);
		if (size() > maxSize)
		{
			removeRange(0, size() - maxSize);
		}
		return r;
	}

	public K getYoungest()
	{
		return get(size() - 1);
	}

	public K getOldest()
	{
		return get(0);
	}
}