/*
 * MathTools.java
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

package hcm.ssj.signal;

/**
 * Created by Michael Dietz on 19.10.2016.
 */

public class MathTools
{
	private static MathTools _instance = null;

	/**
	 * Private constructor for singleton pattern.
	 */
	private MathTools()
	{

	}

	/**
	 * Method for accessing the singleton instance.
	 *
	 * @return singleton instance
	 */
	public static MathTools getInstance()
	{
		if (_instance == null)
		{
			_instance = new MathTools();
		}

		return _instance;
	}

	/**
	 * Calculates the sum of all values
	 */
	public float getSum(float[] values)
	{
		float sum = 0;

		for (int i = 0; i < values.length; i++)
		{
			sum += values[i];
		}

		return sum;
	}

	/**
	 * Calculates the mean of all values
	 */
	public float getMean(float[] values)
	{
		float mean = 0;

		if (values.length > 0)
		{
			mean = getSum(values) / (float) values.length;
		}

		return mean;
	}

	/**
	 * Calculates the variance of all values
	 */
	public float getVariance(float[] values)
	{
		float variance = 0;

		if (values.length > 0)
		{
			float mean = getMean(values);

			for (int i = 0; i < values.length; i++)
			{
				variance += Math.pow(values[i] - mean, 2);
			}

			variance = variance / (float) values.length;
		}

		return variance;
	}

	/**
	* Calculates the standard deviation of all values
	*/
	public float getStdDeviation(float[] values)
	{
		float stdDeviation = 0;

		if (values.length > 0)
		{
			stdDeviation = (float) Math.sqrt(getVariance(values));
		}

		return stdDeviation;
	}
}
