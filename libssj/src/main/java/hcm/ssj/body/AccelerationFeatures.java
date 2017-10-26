/*
 * AccelerationFeatures.java
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

package hcm.ssj.body;

import org.jtransforms.fft.FloatFFT_1D;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.SSJFatalException;
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
		public final Option<Boolean> displacementX = new Option<>("displacementX", false, Boolean.class, "Displacement for x-axis");
		public final Option<Boolean> displacementY = new Option<>("displacementY", false, Boolean.class, "Displacement for y-axis");
		public final Option<Boolean> displacementZ = new Option<>("displacementZ", false, Boolean.class, "Displacement for z-axis");
		public final Option<Boolean> entropyX = new Option<>("entropyX", true, Boolean.class, "Frequency domain entropy for x-axis");
		public final Option<Boolean> entropyY = new Option<>("entropyY", true, Boolean.class, "Frequency domain entropy for y-axis");
		public final Option<Boolean> entropyZ = new Option<>("entropyZ", true, Boolean.class, "Frequency domain entropy for z-axis");
		public final Option<Boolean> skewX = new Option<>("skewX", true, Boolean.class, "Skew for x-axis");
		public final Option<Boolean> skewY = new Option<>("skewY", true, Boolean.class, "Skew for y-axis");
		public final Option<Boolean> skewZ = new Option<>("skewZ", true, Boolean.class, "Skew for z-axis");
		public final Option<Boolean> kurtosisX = new Option<>("kurtosisX", true, Boolean.class, "Kurtosis for x-axis");
		public final Option<Boolean> kurtosisY = new Option<>("kurtosisY", true, Boolean.class, "Kurtosis for y-axis");
		public final Option<Boolean> kurtosisZ = new Option<>("kurtosisZ", true, Boolean.class, "Kurtosis for z-axis");
		public final Option<Boolean> iqrX = new Option<>("iqrX", true, Boolean.class, "Interquartile range for x-axis");
		public final Option<Boolean> iqrY = new Option<>("iqrY", true, Boolean.class, "Interquartile range for y-axis");
		public final Option<Boolean> iqrZ = new Option<>("iqrZ", true, Boolean.class, "Interquartile range for z-axis");
		public final Option<Boolean> madX = new Option<>("madX", true, Boolean.class, "Mean absolute deviation for x-axis");
		public final Option<Boolean> madY = new Option<>("madY", true, Boolean.class, "Mean absolute deviation for y-axis");
		public final Option<Boolean> madZ = new Option<>("madZ", true, Boolean.class, "Mean absolute deviation for z-axis");
		public final Option<Boolean> rmsX = new Option<>("rmsX", true, Boolean.class, "Root mean square for x-axis");
		public final Option<Boolean> rmsY = new Option<>("rmsY", true, Boolean.class, "Root mean square for y-axis");
		public final Option<Boolean> rmsZ = new Option<>("rmsZ", true, Boolean.class, "Root mean square for z-axis");
		public final Option<Boolean> varianceX = new Option<>("varianceX", true, Boolean.class, "Variance for x-axis");
		public final Option<Boolean> varianceY = new Option<>("varianceY", true, Boolean.class, "Variance for y-axis");
		public final Option<Boolean> varianceZ = new Option<>("varianceZ", true, Boolean.class, "Variance for z-axis");
		public final Option<Boolean> signalMagnitudeArea = new Option<>("signalMagnitudeArea", true, Boolean.class, "Signal magnitude area for all axes");
		public final Option<Boolean> haarFilterX = new Option<>("haarFilterX", true, Boolean.class, "Haar-like filter for x-axis");
		public final Option<Boolean> haarFilterY = new Option<>("haarFilterY", true, Boolean.class, "Haar-like filter for y-axis");
		public final Option<Boolean> haarFilterZ = new Option<>("haarFilterZ", true, Boolean.class, "Haar-like filter for z-axis");
		public final Option<Boolean> haarFilterBiaxialXY = new Option<>("haarFilterBiaxialXY", true, Boolean.class, "Biaxial Haar-like filter between x and y-axis");
		public final Option<Boolean> haarFilterBiaxialYZ = new Option<>("haarFilterBiaxialYZ", true, Boolean.class, "Biaxial Haar-like filter between y and z-axis");
		public final Option<Boolean> haarFilterBiaxialZX = new Option<>("haarFilterBiaxialZX", true, Boolean.class, "Biaxial Haar-like filter between z and x-axis");
		public final Option<Boolean> crestX = new Option<>("crestX", true, Boolean.class, "Crest factor for x-axis");
		public final Option<Boolean> crestY = new Option<>("crestY", true, Boolean.class, "Crest factor for y-axis");
		public final Option<Boolean> crestZ = new Option<>("crestZ", true, Boolean.class, "Crest factor for z-axis");
		public final Option<Boolean> spectralFluxX = new Option<>("spectralFluxX", true, Boolean.class, "Spectral flux for x-axis");
		public final Option<Boolean> spectralFluxY = new Option<>("spectralFluxY", true, Boolean.class, "Spectral flux for y-axis");
		public final Option<Boolean> spectralFluxZ = new Option<>("spectralFluxZ", true, Boolean.class, "Spectral flux for z-axis");
		public final Option<Boolean> spectralCentroidX = new Option<>("spectralCentroidX", true, Boolean.class, "Spectral centroid for x-axis");
		public final Option<Boolean> spectralCentroidY = new Option<>("spectralCentroidY", true, Boolean.class, "Spectral centroid for y-axis");
		public final Option<Boolean> spectralCentroidZ = new Option<>("spectralCentroidZ", true, Boolean.class, "Spectral centroid for z-axis");
		public final Option<Boolean> spectralRolloffX = new Option<>("spectralRolloffX", true, Boolean.class, "Spectral rolloff for x-axis");
		public final Option<Boolean> spectralRolloffY = new Option<>("spectralRolloffY", true, Boolean.class, "Spectral rolloff for y-axis");
		public final Option<Boolean> spectralRolloffZ = new Option<>("spectralRolloffZ", true, Boolean.class, "Spectral rolloff for z-axis");

		private Options()
		{
			addOptions();
		}
	}
	public final Options options = new Options();

	FloatFFT_1D fft;
	float[] inputCopy;

	float[] xValues;
	float[] yValues;
	float[] zValues;

	float[] joined;
	float[] psd;
	float[] xValuesFFT;
	float[] yValuesFFT;
	float[] zValuesFFT;

	@Override
	public void enter(Stream[] stream_in, Stream stream_out) throws SSJFatalException
	{
		int values = stream_in[0].num;

		fft = new FloatFFT_1D(values);
		inputCopy = new float[values];

		xValues = new float[values];
		yValues = new float[values];
		zValues = new float[values];

		joined = new float[(values >> 1) + 1];
		psd = new float[(values >> 1) + 1];
		xValuesFFT = new float[(values >> 1) + 1];
		yValuesFFT = new float[(values >> 1) + 1];
		zValuesFFT = new float[(values >> 1) + 1];
	}

	@Override
	public void transform(Stream[] stream_in, Stream stream_out) throws SSJFatalException
	{
		MathTools math = MathTools.getInstance();

		getValues(stream_in[0], 0, xValues);
		getValues(stream_in[0], 1, yValues);
		getValues(stream_in[0], 2, zValues);

		calculateFFT(xValues, xValuesFFT);
		calculateFFT(yValues, yValuesFFT);
		calculateFFT(zValues, zValuesFFT);

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
			out[featureCount++] = getEnergy(xValuesFFT);
		}
		if (options.energyY.get())
		{
			out[featureCount++] = getEnergy(yValuesFFT);
		}
		if (options.energyZ.get())
		{
			out[featureCount++] = getEnergy(zValuesFFT);
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
		if (options.entropyX.get())
		{
			out[featureCount++] = getEntropy(xValuesFFT);
		}
		if (options.entropyY.get())
		{
			out[featureCount++] = getEntropy(yValuesFFT);
		}
		if (options.entropyZ.get())
		{
			out[featureCount++] = getEntropy(zValuesFFT);
		}
		if (options.skewX.get())
		{
			out[featureCount++] = math.getSkew(xValues);
		}
		if (options.skewY.get())
		{
			out[featureCount++] = math.getSkew(yValues);
		}
		if (options.skewZ.get())
		{
			out[featureCount++] = math.getSkew(zValues);
		}
		if (options.kurtosisX.get())
		{
			out[featureCount++] = math.getKurtosis(xValues);
		}
		if (options.kurtosisY.get())
		{
			out[featureCount++] = math.getKurtosis(yValues);
		}
		if (options.kurtosisZ.get())
		{
			out[featureCount++] = math.getKurtosis(zValues);
		}
		if (options.iqrX.get())
		{
			out[featureCount++] = math.getIQR(xValues);
		}
		if (options.iqrY.get())
		{
			out[featureCount++] = math.getIQR(yValues);
		}
		if (options.iqrZ.get())
		{
			out[featureCount++] = math.getIQR(zValues);
		}
		if (options.madX.get())
		{
			out[featureCount++] = math.getMAD(xValues);
		}
		if (options.madY.get())
		{
			out[featureCount++] = math.getMAD(yValues);
		}
		if (options.madZ.get())
		{
			out[featureCount++] = math.getMAD(zValues);
		}
		if (options.rmsX.get())
		{
			out[featureCount++] = math.getRMS(xValues);
		}
		if (options.rmsY.get())
		{
			out[featureCount++] = math.getRMS(yValues);
		}
		if (options.rmsZ.get())
		{
			out[featureCount++] = math.getRMS(zValues);
		}
		if (options.varianceX.get())
		{
			out[featureCount++] = math.getVariance(xValues);
		}
		if (options.varianceY.get())
		{
			out[featureCount++] = math.getVariance(yValues);
		}
		if (options.varianceZ.get())
		{
			out[featureCount++] = math.getVariance(zValues);
		}
		if (options.signalMagnitudeArea.get())
		{
			out[featureCount++] = getSignalMagnitudeArea(xValues, yValues, zValues);
		}
		if (options.haarFilterX.get())
		{
			out[featureCount++] = getHaarFilter(xValues);
		}
		if (options.haarFilterY.get())
		{
			out[featureCount++] = getHaarFilter(yValues);
		}
		if (options.haarFilterZ.get())
		{
			out[featureCount++] = getHaarFilter(zValues);
		}
		if (options.haarFilterBiaxialXY.get())
		{
			out[featureCount++] = getHaarFilterBiaxial(xValues, yValues);
		}
		if (options.haarFilterBiaxialYZ.get())
		{
			out[featureCount++] = getHaarFilterBiaxial(yValues, zValues);
		}
		if (options.haarFilterBiaxialZX.get())
		{
			out[featureCount++] = getHaarFilterBiaxial(zValues, xValues);
		}
		if (options.crestX.get())
		{
			out[featureCount++] = math.getCrest(xValues);
		}
		if (options.crestY.get())
		{
			out[featureCount++] = math.getCrest(yValues);
		}
		if (options.crestZ.get())
		{
			out[featureCount++] = math.getCrest(zValues);
		}
		if (options.spectralFluxX.get())
		{
			out[featureCount++] = getSpectralFlux(xValuesFFT);
		}
		if (options.spectralFluxY.get())
		{
			out[featureCount++] = getSpectralFlux(yValuesFFT);
		}
		if (options.spectralFluxZ.get())
		{
			out[featureCount++] = getSpectralFlux(zValuesFFT);
		}
		if (options.spectralCentroidX.get())
		{
			out[featureCount++] = getSpectralCentroid(xValuesFFT);
		}
		if (options.spectralCentroidY.get())
		{
			out[featureCount++] = getSpectralCentroid(yValuesFFT);
		}
		if (options.spectralCentroidZ.get())
		{
			out[featureCount++] = getSpectralCentroid(zValuesFFT);
		}
		if (options.spectralRolloffX.get())
		{
			out[featureCount++] = getSpectralRolloff(xValuesFFT);
		}
		if (options.spectralRolloffY.get())
		{
			out[featureCount++] = getSpectralRolloff(yValuesFFT);
		}
		if (options.spectralRolloffZ.get())
		{
			out[featureCount++] = getSpectralRolloff(zValuesFFT);
		}
	}

	/*
	* Energy for acceleration value
	*
	* based on
	* Bao, Ling et al. - Activity Recognition from User-Annotated Acceleration Data
	* Ravi, N. et al. - Activity recognition from accelerometer data
	*/
	private float getEnergy(float[] fftValues)
	{
		float energy = 0;

		// Calculate energy
		for (int i = 0; i < fftValues.length; i++)
		{
			energy += Math.pow(fftValues[i], 2);
		}

		if (fftValues.length > 0)
		{
			energy = energy / (float) fftValues.length;
		}

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

	/*
	* Frequency domain entropy for acceleration value
	*
	* based on
	* Bao, Ling et al. - Activity Recognition from User-Annotated Acceleration Data
	* Huynh, T. et al. - Analyzing features for activity recognition
	* Lara, Oscar D. et al. - A Survey on Human Activity Recognition using Wearable Sensors
	* Khan, A. et al. - Accelerometer's position free human activity recognition using a hierarchical recognition model
	*
	* http://stackoverflow.com/questions/30418391/what-is-frequency-domain-entropy-in-fft-result-and-how-to-calculate-it
	* http://dsp.stackexchange.com/questions/23689/what-is-spectral-entropy
	*/
	private float getEntropy(float[] fftValues)
	{
		float entropy = 0;

		if (fftValues.length > 0)
		{
			// Calculate Power Spectral Density
			for (int i = 0; i < fftValues.length; i++)
			{
				psd[i] = (float) (Math.pow(fftValues[i], 2) / fftValues.length);
			}

			float psdSum = MathTools.getInstance().getSum(psd);

			if (psdSum > 0)
			{
				// Normalize calculated PSD so that it can be viewed as a Probability Density Function
				for (int i = 0; i < fftValues.length; i++)
				{
					psd[i] = psd[i] / psdSum;
				}

				// Calculate the Frequency Domain Entropy
				for (int i = 0; i < fftValues.length; i++)
				{
					if (psd[i] != 0)
					{
						entropy += psd[i] * Math.log(psd[i]);
					}
				}

				entropy *= -1;
			}
		}

		return entropy;
	}

	/*
	* Signal magnitude area for acceleration
	*
	* based on
	* Khan, A. et al. - Accelerometer's position free human activity recognition using a hierarchical recognition model
	*/
	private float getSignalMagnitudeArea(float[] xValues, float[] yValues, float[] zValues)
	{
		float sma = 0;

		if (xValues.length == yValues.length && yValues.length == zValues.length)
		{
			for (int i = 0; i < xValues.length; i++)
			{
				sma += Math.abs(xValues[i]) + Math.abs(yValues[i]) + Math.abs(zValues[i]);
			}
		}

		return sma;
	}

	/*
	* Haar-like filter for acceleration
	*
	* based on
	* Hanai, Yuya et al. - Haar-Like Filtering for Human Activity Recognition Using 3D Accelerometer
	*/
	private float getHaarFilter(float[] values)
	{
		float haar = 0;

		// Sizes in number of samples
		int wFrame = values.length;
		int wFilter = (int) (0.2 * wFrame);
		int wShift = (int) (0.5 * wFilter);
		int N = (wFrame - wFilter) / wShift + 1;

		float filterValue;

		for (int n = 0; n < N; n++)
		{
			filterValue = 0;

			for (int k = 0; k < wFilter; k++)
			{
				if (n * wShift + k < wFrame)
				{
					if (k < wFilter / 2)
					{
						// Left side of haar filter
						filterValue -= values[n * wShift + k];
					}
					else
					{
						// Right side of haar filter
						filterValue += values[n * wShift + k];
					}
				}
			}

			haar += Math.abs(filterValue);
		}

		return haar;
	}

	/*
	* Biaxial Haar-like filter for acceleration
	*
	* based on
	* Hanai, Yuya et al. - Haar-Like Filtering for Human Activity Recognition Using 3D Accelerometer
	*/
	private float getHaarFilterBiaxial(float[] aValues, float[] bValues)
	{
		float haarBiaxial = 0;

		// Sizes in number of samples
		int wFrame = aValues.length;
		int wFilter = (int)(0.2 * wFrame);
		int wShift = (int)(0.5 * wFilter);
		int N = (wFrame - wFilter) / wShift + 1;

		float aFilterValue;
		float bFilterValue;

		for (int n = 0; n < N; n++)
		{
			aFilterValue = 0;
			bFilterValue = 0;

			for (int k = 0; k < wFilter; k++)
			{
				if (n * wShift + k < wFrame)
				{
					if (k < wFilter / 2)
					{
						// Left side of haar filter
						aFilterValue -= aValues[n * wShift + k];
						bFilterValue -= bValues[n * wShift + k];
					}
					else
					{
						// Right side of haar filter
						aFilterValue += aValues[n * wShift + k];
						bFilterValue += bValues[n * wShift + k];
					}
				}
			}

			haarBiaxial += Math.abs(aFilterValue - bFilterValue);
		}

		return haarBiaxial;
	}

	/*
	* Spectral flux for acceleration
	*
	* based on
	* Rahman, Shah et al. - Unintrusive eating recognition using Google glass
	* Lu, Hong et al. - SoundSense: Scalable Sound Sensing for People-Centric Applications on Mobile Phones
	*/
	private float getSpectralFlux(float[] fftValues)
	{
		float spectralFlux = 0;

		if (fftValues.length > 0)
		{
			float previousValue = 0;
			float currentValue = 0;

			for (int i = 0; i < fftValues.length; i++)
			{
				currentValue = fftValues[i];

				spectralFlux += Math.pow(currentValue - previousValue, 2);

				previousValue = fftValues[i];
			}
		}

		return spectralFlux;
	}

	/*
	* Spectral centroid for acceleration
	*
	* based on
	* Rahman, Shah et al. - Unintrusive eating recognition using Google glass
	* Lu, Hong et al. - SoundSense: Scalable Sound Sensing for People-Centric Applications on Mobile Phones
	*/
	private float getSpectralCentroid(float[] fftValues)
	{
		float spectralCentroid = 0;

		if (fftValues.length > 0)
		{
			float sumTop = 0;
			float sumBottom = 0;

			for (int i = 0; i < fftValues.length; i++)
			{
				sumTop += i * Math.pow(fftValues[i], 2);
				sumBottom += Math.pow(fftValues[i], 2);
			}

			if (sumBottom > 0)
			{
				spectralCentroid = sumTop / sumBottom;
			}
		}

		return spectralCentroid;
	}

	/*
	* Spectral rolloff for acceleration
	*
	* based on
	* Rahman, Shah et al. - Unintrusive eating recognition using Google glass
	* Lu, Hong et al. - SoundSense: Scalable Sound Sensing for People-Centric Applications on Mobile Phones
	*/
	private float getSpectralRolloff(float[] fftValues)
	{
		float spectralRolloff = 0;
		float threshold = 0.93f;

		if (fftValues.length > 0)
		{
			float fftSumTotal = 0;
			float fftSum = 0;

			for (int i = 0; i < fftValues.length; i++)
			{
				fftSumTotal += fftValues[i];
			}

			for (int i = 0; i < fftValues.length; i++)
			{
				fftSum += fftValues[i];

				if (fftSum / fftSumTotal >= threshold)
				{
					spectralRolloff = i;
					break;
				}
			}
		}

		return spectralRolloff;
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

	private void calculateFFT(float[] values, float[] out)
	{
		System.arraycopy(values, 0, inputCopy, 0, values.length);

		// Calculate FFT
		fft.realForward(inputCopy);

		// Format values like in SSI
		Util.joinFFT(inputCopy, out);
	}

	@Override
	public int getSampleDimension(Stream[] stream_in)
	{
		int dim = 0;

		if (stream_in[0].dim != 3)
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
		if (options.entropyX.get()) dim++;
		if (options.entropyY.get()) dim++;
		if (options.entropyZ.get()) dim++;
		if (options.skewX.get()) dim++;
		if (options.skewY.get()) dim++;
		if (options.skewZ.get()) dim++;
		if (options.kurtosisX.get()) dim++;
		if (options.kurtosisY.get()) dim++;
		if (options.kurtosisZ.get()) dim++;
		if (options.iqrX.get()) dim++;
		if (options.iqrY.get()) dim++;
		if (options.iqrZ.get()) dim++;
		if (options.madX.get()) dim++;
		if (options.madY.get()) dim++;
		if (options.madZ.get()) dim++;
		if (options.rmsX.get()) dim++;
		if (options.rmsY.get()) dim++;
		if (options.rmsZ.get()) dim++;
		if (options.varianceX.get()) dim++;
		if (options.varianceY.get()) dim++;
		if (options.varianceZ.get()) dim++;
		if (options.signalMagnitudeArea.get()) dim++;
		if (options.haarFilterX.get()) dim++;
		if (options.haarFilterY.get()) dim++;
		if (options.haarFilterZ.get()) dim++;
		if (options.haarFilterBiaxialXY.get()) dim++;
		if (options.haarFilterBiaxialYZ.get()) dim++;
		if (options.haarFilterBiaxialZX.get()) dim++;
		if (options.crestX.get()) dim++;
		if (options.crestY.get()) dim++;
		if (options.crestZ.get()) dim++;
		if (options.spectralFluxX.get()) dim++;
		if (options.spectralFluxY.get()) dim++;
		if (options.spectralFluxZ.get()) dim++;
		if (options.spectralCentroidX.get()) dim++;
		if (options.spectralCentroidY.get()) dim++;
		if (options.spectralCentroidZ.get()) dim++;
		if (options.spectralRolloffX.get()) dim++;
		if (options.spectralRolloffY.get()) dim++;
		if (options.spectralRolloffZ.get()) dim++;

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
		if (stream_in[0].type != Cons.Type.FLOAT)
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
	protected void describeOutput(Stream[] stream_in, Stream stream_out)
	{
		stream_out.desc = new String[stream_out.dim];

		int featureCount = 0;

		if (options.meanX.get())
		{
			stream_out.desc[featureCount++] = "meanX";
		}
		if (options.meanY.get())
		{
			stream_out.desc[featureCount++] = "meanY";
		}
		if (options.meanZ.get())
		{
			stream_out.desc[featureCount++] = "meanZ";
		}
		if (options.stdDeviationX.get())
		{
			stream_out.desc[featureCount++] = "stdDeviationX";
		}
		if (options.stdDeviationY.get())
		{
			stream_out.desc[featureCount++] = "stdDeviationY";
		}
		if (options.stdDeviationZ.get())
		{
			stream_out.desc[featureCount++] = "stdDeviationZ";
		}
		if (options.energyX.get())
		{
			stream_out.desc[featureCount++] = "energyX";
		}
		if (options.energyY.get())
		{
			stream_out.desc[featureCount++] = "energyY";
		}
		if (options.energyZ.get())
		{
			stream_out.desc[featureCount++] = "energyZ";
		}
		if (options.correlationXY.get())
		{
			stream_out.desc[featureCount++] = "correlationXY";
		}
		if (options.correlationXZ.get())
		{
			stream_out.desc[featureCount++] = "correlationXZ";
		}
		if (options.correlationYZ.get())
		{
			stream_out.desc[featureCount++] = "correlationYZ";
		}
		if (options.displacementX.get())
		{
			stream_out.desc[featureCount++] = "displacementX";
		}
		if (options.displacementY.get())
		{
			stream_out.desc[featureCount++] = "displacementY";
		}
		if (options.displacementZ.get())
		{
			stream_out.desc[featureCount++] = "displacementZ";
		}
		if (options.entropyX.get())
		{
			stream_out.desc[featureCount++] = "entropyX";
		}
		if (options.entropyY.get())
		{
			stream_out.desc[featureCount++] = "entropyY";
		}
		if (options.entropyZ.get())
		{
			stream_out.desc[featureCount++] = "entropyZ";
		}
		if (options.skewX.get())
		{
			stream_out.desc[featureCount++] = "skewX";
		}
		if (options.skewY.get())
		{
			stream_out.desc[featureCount++] = "skewY";
		}
		if (options.skewZ.get())
		{
			stream_out.desc[featureCount++] = "skewZ";
		}
		if (options.kurtosisX.get())
		{
			stream_out.desc[featureCount++] = "kurtosisX";
		}
		if (options.kurtosisY.get())
		{
			stream_out.desc[featureCount++] = "kurtosisY";
		}
		if (options.kurtosisZ.get())
		{
			stream_out.desc[featureCount++] = "kurtosisZ";
		}
		if (options.iqrX.get())
		{
			stream_out.desc[featureCount++] = "iqrX";
		}
		if (options.iqrY.get())
		{
			stream_out.desc[featureCount++] = "iqrY";
		}
		if (options.iqrZ.get())
		{
			stream_out.desc[featureCount++] = "iqrZ";
		}
		if (options.madX.get())
		{
			stream_out.desc[featureCount++] = "madX";
		}
		if (options.madY.get())
		{
			stream_out.desc[featureCount++] = "madY";
		}
		if (options.madZ.get())
		{
			stream_out.desc[featureCount++] = "madZ";
		}
		if (options.rmsX.get())
		{
			stream_out.desc[featureCount++] = "rmsX";
		}
		if (options.rmsY.get())
		{
			stream_out.desc[featureCount++] = "rmsY";
		}
		if (options.rmsZ.get())
		{
			stream_out.desc[featureCount++] = "rmsZ";
		}
		if (options.varianceX.get())
		{
			stream_out.desc[featureCount++] = "varianceX";
		}
		if (options.varianceY.get())
		{
			stream_out.desc[featureCount++] = "varianceY";
		}
		if (options.varianceZ.get())
		{
			stream_out.desc[featureCount++] = "varianceZ";
		}
		if (options.signalMagnitudeArea.get())
		{
			stream_out.desc[featureCount++] = "signalMagnitudeArea";
		}
		if (options.haarFilterX.get())
		{
			stream_out.desc[featureCount++] = "haarFilterX";
		}
		if (options.haarFilterY.get())
		{
			stream_out.desc[featureCount++] = "haarFilterY";
		}
		if (options.haarFilterZ.get())
		{
			stream_out.desc[featureCount++] = "haarFilterZ";
		}
		if (options.haarFilterBiaxialXY.get())
		{
			stream_out.desc[featureCount++] = "haarFilterBiaxialXY";
		}
		if (options.haarFilterBiaxialYZ.get())
		{
			stream_out.desc[featureCount++] = "haarFilterBiaxialYZ";
		}
		if (options.haarFilterBiaxialZX.get())
		{
			stream_out.desc[featureCount++] = "haarFilterBiaxialZX";
		}
		if (options.crestX.get())
		{
			stream_out.desc[featureCount++] = "crestX";
		}
		if (options.crestY.get())
		{
			stream_out.desc[featureCount++] = "crestY";
		}
		if (options.crestZ.get())
		{
			stream_out.desc[featureCount++] = "crestZ";
		}
		if (options.spectralFluxX.get())
		{
			stream_out.desc[featureCount++] = "spectralFluxX";
		}
		if (options.spectralFluxY.get())
		{
			stream_out.desc[featureCount++] = "spectralFluxY";
		}
		if (options.spectralFluxZ.get())
		{
			stream_out.desc[featureCount++] = "spectralFluxZ";
		}
		if (options.spectralCentroidX.get())
		{
			stream_out.desc[featureCount++] = "spectralCentroidX";
		}
		if (options.spectralCentroidY.get())
		{
			stream_out.desc[featureCount++] = "spectralCentroidY";
		}
		if (options.spectralCentroidZ.get())
		{
			stream_out.desc[featureCount++] = "spectralCentroidZ";
		}
		if (options.spectralRolloffX.get())
		{
			stream_out.desc[featureCount++] = "spectralRolloffX";
		}
		if (options.spectralRolloffY.get())
		{
			stream_out.desc[featureCount++] = "spectralRolloffY";
		}
		if (options.spectralRolloffZ.get())
		{
			stream_out.desc[featureCount++] = "spectralRolloffZ";
		}
	}
}
