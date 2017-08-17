/*
 * MathTools.java
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

package hcm.ssj.signal;

import java.util.Arrays;

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

	/*
	* Calculates the minimum of all values
	*/
	public float getMin(float[] values)
	{
		float min = Float.MAX_VALUE;

		for (int i = 0; i < values.length; i++)
		{
			if (values[i] < min)
			{
				min = values[i];
			}
		}

		if (min == Float.MAX_VALUE)
		{
			min = 0;
		}

		return min;
	}

	/*
	* Calculates the maximum of all values
	*/
	public float getMax(float[] values)
	{
		float max = -Float.MAX_VALUE;

		for (int i = 0; i < values.length; i++)
		{
			if (values[i] > max)
			{
				max = values[i];
			}
		}

		if (max == -Float.MAX_VALUE)
		{
			max = 0;
		}

		return max;
	}

	/*
	* Calculates the median of all values
	*/
	public float getMedian(float[] values)
	{
		float median = 0;
		int n = values.length;

		if (n > 0)
		{
			// Copy values for sorting
			float[] valueCopies = new float[values.length];
			System.arraycopy(values, 0, valueCopies, 0, values.length);

			// Sort values ascending
			Arrays.sort(valueCopies);

			if (n % 2 == 0)
			{
				// Even
				median =  (valueCopies[n / 2 - 1] + valueCopies[n / 2]) / 2.0f;
			}
			else
			{
				// Odd
				median = valueCopies[(n - 1) / 2];
			}
		}

		return median;
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

	/*
	* Calculates the skew of all values
	*/
	public float getSkew(float[] values)
	{
		float skew = 0;
		float mean = getMean(values);
		float stdDeviation = getStdDeviation(values);

		if (values.length > 0 && stdDeviation > 0)
		{
			for (int i = 0; i < values.length; i++)
			{
				skew += Math.pow((values[i] - mean) / stdDeviation, 3);
			}

			skew = skew / values.length;
		}

		return skew;
	}

	/*
	* Calculates the kurtosis of all values
	*/
	public float getKurtosis(float[] values)
	{
		float kurtosis = 0;
		float mean = getMean(values);
		float stdDeviation = getStdDeviation(values);

		if (values.length > 0 && stdDeviation > 0)
		{
			for (int i = 0; i < values.length; i++)
			{
				kurtosis += Math.pow((values[i] - mean) / stdDeviation, 4);
			}

			kurtosis = kurtosis / values.length;
		}

		return kurtosis;
	}

	/*
	* Calculates the range of all values
	*/
	public float getRange(float[] values)
	{
		return getMax(values) - getMin(values);
	}

	/*
	* Calculates the root mean square
	*/
	public float getRMS(float[] values)
	{
		float rms = 0;

		if (values.length > 0)
		{
			for (int i = 0; i < values.length; i++)
			{
				rms += Math.pow(values[i], 2);
			}

			rms = (float) Math.sqrt(rms / (float) values.length);
		}

		return rms;
	}

	/*
	* Calculates the mean absolute deviation of all values
	*/
	public float getMAD(float[] values)
	{
		float mad = 0;
		float mean = getMean(values);

		if (values.length > 0)
		{
			for (int i = 0; i < values.length; i++)
			{
				mad += Math.abs(values[i] - mean);
			}

			mad = (float) Math.sqrt(mad / (float) values.length);
		}

		return mad;
	}

	/*
	* Calculates the interquartile range
	*/
	public float getIQR(float[] values)
	{
		float iqr = 0;
		int n = values.length;

		if (n > 0)
		{
			// Copy values for sorting
			float[] valueCopies = new float[values.length];
			System.arraycopy(values, 0, valueCopies, 0, values.length);

			// Sort values ascending
			Arrays.sort(valueCopies);

			float[] lowerPercentile;
			float[] upperPercentile;

			if (n % 2 == 0)
			{
				lowerPercentile = new float[n / 2];
				upperPercentile = new float[n / 2];

				// Even
				for (int i = 0; i < n; i++)
				{
					if (i < n / 2)
					{
						lowerPercentile[i] = valueCopies[i];
					}
					else
					{
						upperPercentile[i - n / 2] = valueCopies[i];
					}
				}
			}
			else
			{
				lowerPercentile = new float[(n - 1) / 2];
				upperPercentile = new float[(n - 1) / 2];

				// Odd
				for (int i = 0; i < n; i++)
				{
					if (i < (n - 1) / 2)
					{
						lowerPercentile[i] = valueCopies[i];
					}

					// Exclude median

					if (i > (n - 1) / 2)
					{
						upperPercentile[i - ((n - 1) / 2) - 1] = valueCopies[i];
					}
				}
			}

			iqr = getMedian(upperPercentile) - getMedian(lowerPercentile);
		}

		return iqr;
	}

	/*
	* Calculates the crest factor
	*/
	public float getCrest(float[] values)
	{
		float absValue = 0;
		float crest = 0;
		float peak = 0;
		float rms = getRMS(values);

		for (int i = 0; i < values.length; i++)
		{
			absValue = Math.abs(values[i]);

			if (absValue > peak)
			{
				peak = absValue;
			}
		}

		if (rms > 0)
		{
			crest = peak / rms;
		}

		return crest;
	}
}
