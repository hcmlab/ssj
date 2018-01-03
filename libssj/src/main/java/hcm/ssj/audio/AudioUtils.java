/*
 * AudioUtils.java
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

package hcm.ssj.audio;

import java.util.Arrays;

/**
 * Collection of helper methods for audio.
 */
public final class AudioUtils
{
	/**
	 * Prevent class from being instantiated.
	 */
	private AudioUtils() {}

	/**
	 * Calculate highest and lowest points in given byte array.
	 * @param data Audio sample.
	 * @param sampleSize Size of the given audio sample.
	 * @return Two dimensional byte array of minimums and maximums.
	 */
	public static short[][] getExtremes(short[] data, int sampleSize)
	{
		short[][] newData = new short[sampleSize][];
		int groupSize = data.length / sampleSize;

		for (int i = 0; i < sampleSize; i++)
		{
			short[] group = Arrays.copyOfRange(data, i * groupSize,
											   Math.min((i + 1) * groupSize, data.length));
			short min = Short.MAX_VALUE;
			short max = Short.MIN_VALUE;

			for (short a : group)
			{
				min = (short) Math.min(min, a);
				max = (short) Math.max(max, a);
			}
			newData[i] = new short[] { max, min };
		}
		return newData;
	}
}
