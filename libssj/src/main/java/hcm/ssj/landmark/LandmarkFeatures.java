/*
 * LandmarkFeatures.java
 * Copyright (c) 2021
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

package hcm.ssj.landmark;

import hcm.ssj.core.Cons;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.Transformer;
import hcm.ssj.core.Util;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;

/**
 * Created by Michael Dietz on 21.06.2021.
 */
public class LandmarkFeatures extends Transformer
{
	public static final float INVALID_VALUE = -1;

	public class Options extends OptionList
	{
		public final Option<Boolean> mocs = new Option<>("mocs", true, Boolean.class, "calculates the mouth open/close score");

		private Options()
		{
			addOptions();
		}
	}

	public final Options options = new Options();

	public LandmarkFeatures()
	{
		_name = this.getClass().getSimpleName();
	}

	@Override
	public OptionList getOptions()
	{
		return options;
	}

	@Override
	public void transform(Stream[] stream_in, Stream stream_out) throws SSJFatalException
	{
		float[] in = stream_in[0].ptrF();
		float[] out = stream_out.ptrF();

		if (options.mocs.get())
		{
			// Based on https://ieeexplore.ieee.org/document/5634522
			double innerLipDistance = getInnerLipDistance(in);
			double outerLipDistance = getOuterLipDistance(in);

			if (outerLipDistance > 0)
			{
				out[0] = (float) (innerLipDistance / outerLipDistance);
			}
			else
			{
				out[0] = INVALID_VALUE;
			}
		}
	}

	public double getInnerLipDistance(float[] faceLandmarks)
	{
		// Upper lip center landmark
		float x1 = getLandmarkX(13, faceLandmarks);
		float y1 = getLandmarkY(13, faceLandmarks);

		// Lower lip center landmark
		float x2 = getLandmarkX(14, faceLandmarks);
		float y2 = getLandmarkY(14, faceLandmarks);

		// Return distance
		return getDistance(x1, y1, x2, y2);
	}

	public double getOuterLipDistance(float[] faceLandmarks)
	{
		// Upper lip center landmark
		float x1 = getLandmarkX(0, faceLandmarks);
		float y1 = getLandmarkY(0, faceLandmarks);

		// Lower lip center landmark
		float x2 = getLandmarkX(17, faceLandmarks);
		float y2 = getLandmarkY(17, faceLandmarks);

		// Return distance
		return getDistance(x1, y1, x2, y2);
	}

	public double getEyeDistance(float[] faceLandmarks)
	{
		// Left eye outer landmark
		float x1 = getLandmarkX(33, faceLandmarks);
		float y1 = getLandmarkY(33, faceLandmarks);

		// Right eye outer landmark
		float x2 = getLandmarkX(263, faceLandmarks);
		float y2 = getLandmarkY(363, faceLandmarks);

		// Return distance
		return getDistance(x1, y1, x2, y2);
	}

	public float getLandmarkX(int landmarkIndex, float[] faceLandmarks)
	{
		return faceLandmarks[landmarkIndex * 2];
	}

	public float getLandmarkY(int landmarkIndex, float[] faceLandmarks)
	{
		return faceLandmarks[landmarkIndex * 2 + 1];
	}

	public double getDistance(float x1, float y1, float x2, float y2)
	{
		return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
	}

	@Override
	public int getSampleDimension(Stream[] stream_in)
	{
		return 1;
	}

	@Override
	public int getSampleBytes(Stream[] stream_in)
	{
		return Util.sizeOf(getSampleType(stream_in));
	}

	@Override
	public Cons.Type getSampleType(Stream[] stream_in)
	{
		return Cons.Type.FLOAT;
	}

	@Override
	public int getSampleNumber(int sampleNumber_in)
	{
		return sampleNumber_in;
	}

	@Override
	protected void describeOutput(Stream[] stream_in, Stream stream_out)
	{
		stream_out.desc = new String[stream_out.dim];

		if (options.mocs.get())
		{
			stream_out.desc[0] = "Mouth open";
		}
	}
}
