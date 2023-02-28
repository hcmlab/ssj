/*
 * RRTimeFeatures.java
 * Copyright (c) 2023
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

package hcm.ssj.biosig;

import java.util.ArrayList;
import java.util.List;

import hcm.ssj.core.Cons;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.Transformer;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;

/**
 * Created by Michael Dietz on 14.02.2023.
 * Calculates QRS HRV time domain features
 *
 * Code adapted from SSI's QRSHRVtime.cpp
 */
public class RRTimeFeatures extends Transformer
{
	public class Options extends OptionList
	{
		public final Option<Boolean> calculateRR = new Option<>("calculateRR", false, Boolean.class, "Whether RR intervals need to be calculated first");

		private Options()
		{
			addOptions();
		}
	}
	public final Options options = new Options();

	boolean firstRR;
	int samplesSinceLastRR;
	double timePerSample;
	boolean calibrated;
	List<Float> rrIntervals;

	public RRTimeFeatures()
	{
		_name = "RRTimeFeatures";
	}

	@Override
	public OptionList getOptions()
	{
		return options;
	}

	@Override
	public void enter(Stream[] stream_in, Stream stream_out) throws SSJFatalException
	{
		timePerSample = 1000.0 / stream_in[0].sr;

		firstRR = true;
		samplesSinceLastRR = 0;

		calibrated = false;
		rrIntervals = new ArrayList<>();
	}

	@Override
	public void transform(Stream[] stream_in, Stream stream_out) throws SSJFatalException
	{
		int sampleNumber = stream_in[0].num;
		float[] ptrIn = stream_in[0].ptrF();
		float[] ptrOut = stream_out.ptrF();

		int nRRs = 0;
		float rrDiff = 0.0f;
		float rr = 0.0f;
		float sumRR = 0.0f;

		// Mean RR interval in ms
		float mRR = 0.0f;

		// Mean heart rate in bpm
		float mHR = 0.0f;

		// Standard deviation of RR-Intervals in ms
		float SDRR = 0.0f;

		// Standard deviation of heart rate in bpm
		float SDHR = 0.0f;

		// Coefficient of variance of RR's
		float CVRR = 0.0f;

		// Root mean square successive difference in ms
		float RMSSD = 0.0f;

		// Number of pairs of RR's differing by >20ms in percent
		float pRR20 = 0.0f;

		// Number of pairs of RR's differing by >50ms in percent
		float pRR50 = 0.0f;

		if (options.calculateRR.get())
		{
			for (int i = 0; i < sampleNumber; i++)
			{
				if (ptrIn[i] == 1)
				{
					if (firstRR)
					{
						firstRR = false;
					}
					else
					{
						rr = (float) (samplesSinceLastRR * timePerSample);
						rrIntervals.add(rr);
					}

					samplesSinceLastRR = 0;
				}
				else
				{
					samplesSinceLastRR++;
				}
			}
		}
		else
		{
			for (int i = 0; i < sampleNumber; i++)
			{
				if (ptrIn[i] > 0.0)
				{
					rrIntervals.add(ptrIn[i]);
				}
			}
		}

		nRRs = rrIntervals.size();

		// Check if there are rr intervals
		if (nRRs > 1)
		{
			// Calculate features
			for (int i = 0; i < nRRs; i++)
			{
				sumRR += rrIntervals.get(i);
				mHR += (60000.0f / rrIntervals.get(i));
			}
			mRR = sumRR / nRRs;
			mHR = mHR / nRRs;

			for (int i = 0; i < nRRs; i++)
			{
				SDRR += Math.pow(rrIntervals.get(i) - mRR, 2);
				SDHR += Math.pow((60000.0f / rrIntervals.get(i)) - mHR, 2);
			}
			SDRR = (float) Math.sqrt(SDRR / (nRRs - 1));
			SDHR = (float) Math.sqrt(SDHR / (nRRs - 1));
			CVRR = (SDRR * 100.0f) / mRR;

			for (int i = 0; i < nRRs - 1; i++)
			{
				rrDiff = rrIntervals.get(i + 1) - rrIntervals.get(i);

				RMSSD += Math.pow(rrDiff, 2);

				if (Math.abs(rrDiff) > 20.0f)
				{
					pRR20++;

					if (Math.abs(rrDiff) > 50.0f)
					{
						pRR50++;
					}
				}
			}
			RMSSD = (float) Math.sqrt(RMSSD / (nRRs - 1));
			pRR20 = (pRR20 * 100.0f) / (nRRs - 1);
			pRR50 = (pRR50 * 100.0f) / (nRRs - 1);

			rrIntervals.clear();
		}

		int ptrIndex = 0;
		ptrOut[ptrIndex++] = mRR;
		ptrOut[ptrIndex++] = mHR;
		ptrOut[ptrIndex++] = SDRR;
		ptrOut[ptrIndex++] = SDHR;
		ptrOut[ptrIndex++] = CVRR;
		ptrOut[ptrIndex++] = RMSSD;
		ptrOut[ptrIndex++] = pRR20;
		ptrOut[ptrIndex] = pRR50;
	}

	@Override
	public int getSampleDimension(Stream[] stream_in)
	{
		return 8;
	}

	@Override
	public Cons.Type getSampleType(Stream[] stream_in)
	{
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
		stream_out.desc = new String[]{"mRR", "mHR", "SDRR", "SDHR", "CVRR", "RMSSD", "pRR20", "pRR50"};
	}
}
