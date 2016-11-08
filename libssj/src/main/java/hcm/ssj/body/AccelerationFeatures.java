/*
 * AccelerationFeatures.java
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

package hcm.ssj.body;

import org.jtransforms.fft.FloatFFT_1D;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.Transformer;
import hcm.ssj.core.Util;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;
import hcm.ssj.signal.MathTools;

/**
 * Created by Michael Dietz on 18.10.2016.
 */

public class AccelerationFeatures extends Transformer
{
	public class Options extends OptionList
	{
		public final Option<Boolean> meanX = new Option<>("meanX", true, Boolean.class, "Mean acceleration for x-axis");
		public final Option<Boolean> meanY = new Option<>("meanY", true, Boolean.class, "Mean acceleration for y-axis");
		public final Option<Boolean> meanZ = new Option<>("meanZ", true, Boolean.class, "Mean acceleration for z-axis");
		public final Option<Boolean> stdDeviationX = new Option<>("stdDeviationX", true, Boolean.class, "Standard deviation for x-axis");
		public final Option<Boolean> stdDeviationY = new Option<>("stdDeviationY", true, Boolean.class, "Standard deviation for y-axis");
		public final Option<Boolean> stdDeviationZ = new Option<>("stdDeviationZ", true, Boolean.class, "Standard deviation for z-axis");
		public final Option<Boolean> energyX = new Option<>("energyX", true, Boolean.class, "Energy for x-axis");
		public final Option<Boolean> energyY = new Option<>("energyY", true, Boolean.class, "Energy for y-axis");
		public final Option<Boolean> energyZ = new Option<>("energyZ", true, Boolean.class, "Energy for z-axis");
		public final Option<Boolean> correlationXY = new Option<>("correlationXY", true, Boolean.class, "Correlation between x and y-axis");
		public final Option<Boolean> correlationXZ = new Option<>("correlationXZ", true, Boolean.class, "Correlation between x and z-axis");
		public final Option<Boolean> correlationYZ = new Option<>("correlationYZ", true, Boolean.class, "Correlation between y and z-axis");
		public final Option<Boolean> displacementX = new Option<>("displacementX", true, Boolean.class, "Displacement for x-axis");
		public final Option<Boolean> displacementY = new Option<>("displacementY", true, Boolean.class, "Displacement for y-axis");
		public final Option<Boolean> displacementZ = new Option<>("displacementZ", true, Boolean.class, "Displacement for z-axis");

		private Options()
		{
			addOptions();
		}
	}
	public final Options options = new Options();

	FloatFFT_1D fft;
	float[] inputCopy;
	float[] joined;

	float[] xValues;
	float[] yValues;
	float[] zValues;

	@Override
	public void enter(Stream[] stream_in, Stream stream_out)
	{
		int values = stream_in[0].num;

		fft = new FloatFFT_1D(values);
		inputCopy = new float[values];
		joined = new float[(values >> 1) + 1];

		xValues = new float[values];
		yValues = new float[values];
		zValues = new float[values];
	}

	@Override
	public void transform(Stream[] stream_in, Stream stream_out)
	{
		MathTools math = MathTools.getInstance();

		getValues(stream_in[0], 0, xValues);
		getValues(stream_in[0], 1, yValues);
		getValues(stream_in[0], 2, zValues);

		float[] out = stream_out.ptrF();

		int featureCount = 0;

		if (options.meanX.get())
		{
			out[featureCount++] = math.getMean(xValues);
		}
		if (options.meanY.get())
		{
			out[featureCount++] = math.getMean(yValues);
		}
		if (options.meanZ.get())
		{
			out[featureCount++] = math.getMean(zValues);
		}
		if (options.stdDeviationX.get())
		{
			out[featureCount++] = math.getStdDeviation(xValues);
		}
		if (options.stdDeviationY.get())
		{
			out[featureCount++] = math.getStdDeviation(yValues);
		}
		if (options.stdDeviationZ.get())
		{
			out[featureCount++] = math.getStdDeviation(zValues);
		}
		if (options.energyX.get())
		{
			out[featureCount++] = getEnergy(xValues);
		}
		if (options.energyY.get())
		{
			out[featureCount++] = getEnergy(yValues);
		}
		if (options.energyZ.get())
		{
			out[featureCount++] = getEnergy(zValues);
		}
		if (options.correlationXY.get())
		{
			out[featureCount++] = getCorrelation(xValues, yValues);
		}
		if (options.correlationXZ.get())
		{
			out[featureCount++] = getCorrelation(xValues, zValues);
		}
		if (options.correlationYZ.get())
		{
			out[featureCount++] = getCorrelation(yValues, zValues);
		}
		if (options.displacementX.get())
		{
			out[featureCount++] = getDisplacement(xValues, stream_in[0].sr);
		}
		if (options.displacementY.get())
		{
			out[featureCount++] = getDisplacement(yValues, stream_in[0].sr);
		}
		if (options.displacementZ.get())
		{
			out[featureCount++] = getDisplacement(zValues, stream_in[0].sr);
		}
	}

	/*
	* Energy for acceleration value
	*
	* based on
	* Bao, Ling et al. - Activity Recognition from User-Annotated Acceleration Data
	* Ravi, N. et al. - Activity recognition from accelerometer data
	*/
	private float getEnergy(float[] values)
	{
		float energy = 0;
		System.arraycopy(values, 0, inputCopy, 0, values.length);

		// Calculate FFT
		fft.realForward(inputCopy);

		// Format values like in SSI
		float[] output = joinFFT(inputCopy);

		// Calculate energy
		for (int i = 0; i < output.length; i++)
		{
			energy += Math.pow(output[i], 2);
		}

		energy = energy / (float) output.length;

		return energy;
	}

	/*
	* Correlation for acceleration value
	*
	* based on
	* Bao, Ling et al. - Activity Recognition from User-Annotated Acceleration Data
	* Ravi, N. et al. - Activity recognition from accelerometer data
	*/
	private float getCorrelation(float[] aValues, float[] bValues)
	{
		MathTools math = MathTools.getInstance();

		float correlation = 0;
		float covariance = 0;

		if (aValues.length > 0 && bValues.length > 0)
		{
			float meanA = math.getMean(aValues);
			float meanB = math.getMean(bValues);
			float stdDeviationA = math.getStdDeviation(aValues);
			float stdDeviationB = math.getStdDeviation(bValues);

			for (int i = 0; i < aValues.length; i++)
			{
				covariance += (aValues[i] - meanA) * (bValues[i] - meanB);
			}

			covariance = covariance / (float) aValues.length;

			if (stdDeviationA * stdDeviationB != 0)
			{
				correlation = covariance / (stdDeviationA * stdDeviationB);
			}
		}

		return correlation;
	}

	/*
	 * Displacement for acceleration value in meter
	 */
	private float getDisplacement(float[] values, double sampleRate)
	{
		float displacement = 0;
		float a = 0;
		float velNew = 0;
		float velOld = 0;

		// Time for which the object moves with that acceleration
		float t = 1.0f / (float) sampleRate;

		// Sum up displacement steps
		for (int i = 0; i < values.length; i++)
		{
			a = (Math.abs(values[i]) < 0.1) ? 0 : values[i];

			// v1 = a * t + v0
			velNew = a * t + velOld;

			// d = v0 * t + 0.5 * a * t²  or  d = v0 * t + 0.5 * (v1 - v0) * t
			displacement += velOld * t + 0.5f * (velNew - velOld) * t;

			// Update v0
			velOld = velNew;
		}

		return displacement;
	}

	/**
	 * Helper function to format fft values similar to SSI
	 */
	private float[] joinFFT(float[] fft)
	{
		for (int i = 0; i < fft.length; i = i + 2)
		{
			if (i == 0)
			{
				joined[0] = fft[0];
				joined[1] = fft[1];
			}
			else
			{
				joined[i / 2 + 1] = (float) Math.sqrt(Math.pow(fft[i], 2) + Math.pow(fft[i + 1], 2));
			}
		}

		return joined;
	}

	/**
	 * Helper function to get all values from a specific dimension
	 */
	private void getValues(Stream stream, int dimension, float[] out)
	{
		float[] in = stream.ptrF();

		for (int i = 0; i < stream.num; i++)
		{
			out[i] = in[i * stream.dim + dimension];
		}
	}

	@Override
	public int getSampleDimension(Stream[] stream_in)
	{
		int dim = 0;

		if(stream_in[0].dim != 3)
		{
			Log.e("Unsupported input stream dimension");
		}

		if (options.meanX.get()) dim++;
		if (options.meanY.get()) dim++;
		if (options.meanZ.get()) dim++;
		if (options.stdDeviationX.get()) dim++;
		if (options.stdDeviationY.get()) dim++;
		if (options.stdDeviationZ.get()) dim++;
		if (options.energyX.get()) dim++;
		if (options.energyY.get()) dim++;
		if (options.energyZ.get()) dim++;
		if (options.correlationXY.get()) dim++;
		if (options.correlationXZ.get()) dim++;
		if (options.correlationYZ.get()) dim++;
		if (options.displacementX.get()) dim++;
		if (options.displacementY.get()) dim++;
		if (options.displacementZ.get()) dim++;

		return dim;
	}

	@Override
	public int getSampleBytes(Stream[] stream_in)
	{
		return Util.sizeOf(getSampleType(stream_in));
	}

	@Override
	public Cons.Type getSampleType(Stream[] stream_in)
	{
		if(stream_in[0].type != Cons.Type.FLOAT)
		{
			Log.e("Unsupported input stream type");
		}
		return Cons.Type.FLOAT;
	}

	@Override
	public int getSampleNumber(int sampleNumber_in)
	{
		return 1;
	}

	@Override
	protected void defineOutputClasses(Stream[] stream_in, Stream stream_out)
	{
		stream_out.dataclass = new String[stream_out.dim];

		int featureCount = 0;

		if (options.meanX.get())
		{
			stream_out.dataclass[featureCount++] = "meanX";
		}
		if (options.meanY.get())
		{
			stream_out.dataclass[featureCount++] = "meanY";
		}
		if (options.meanZ.get())
		{
			stream_out.dataclass[featureCount++] = "meanZ";
		}
		if (options.stdDeviationX.get())
		{
			stream_out.dataclass[featureCount++] = "stdDeviationX";
		}
		if (options.stdDeviationY.get())
		{
			stream_out.dataclass[featureCount++] = "stdDeviationY";
		}
		if (options.stdDeviationZ.get())
		{
			stream_out.dataclass[featureCount++] = "stdDeviationZ";
		}
		if (options.energyX.get())
		{
			stream_out.dataclass[featureCount++] = "energyX";
		}
		if (options.energyY.get())
		{
			stream_out.dataclass[featureCount++] = "energyY";
		}
		if (options.energyZ.get())
		{
			stream_out.dataclass[featureCount++] = "energyZ";
		}
		if (options.correlationXY.get())
		{
			stream_out.dataclass[featureCount++] = "correlationXY";
		}
		if (options.correlationXZ.get())
		{
			stream_out.dataclass[featureCount++] = "correlationXZ";
		}
		if (options.correlationYZ.get())
		{
			stream_out.dataclass[featureCount++] = "correlationYZ";
		}
		if (options.displacementX.get())
		{
			stream_out.dataclass[featureCount++] = "displacementX";
		}
		if (options.displacementY.get())
		{
			stream_out.dataclass[featureCount++] = "displacementY";
		}
		if (options.displacementZ.get())
		{
			stream_out.dataclass[featureCount++] = "displacementZ";
		}
	}
}
