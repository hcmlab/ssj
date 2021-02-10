/*
 * LandmarkSmoothingCalculator.java
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

package hcm.ssj.landmark.utils;

import java.util.List;

import hcm.ssj.ssd.Landmark;

/**
 * Created by Michael Dietz on 29.01.2021.
 *
 * Based on:
 * https://github.com/google/mediapipe/blob/master/mediapipe/calculators/util/landmarks_smoothing_calculator.proto
 * https://github.com/google/mediapipe/blob/master/mediapipe/calculators/util/landmarks_smoothing_calculator.cc
 */
public class LandmarkSmoothingCalculator
{
	static final float MIN_ALLOWED_OBJECT_SCALE = 1e-6f;

	final int imageWidth;
	final int imageHeight;

	final int windowSize;
	final float velocityScale;
	final int nLandmarks;

	RelativeVelocityFilter[] xFilters;
	RelativeVelocityFilter[] yFilters;
	// RelativeVelocityFilter[] zFilters;

	// Cache variables
	long timestamp;
	float objectScale;
	float valueScale;

	public LandmarkSmoothingCalculator(int windowSize, float velocityScale, int nLandmarks, int imageWidth, int imageHeight)
	{
		this.windowSize = windowSize;
		this.velocityScale = velocityScale;
		this.nLandmarks = nLandmarks;

		this.imageWidth = imageWidth;
		this.imageHeight = imageHeight;

		initializeFilters(nLandmarks);
	}

	public void initializeFilters(int nLandmarks)
	{
		xFilters = new RelativeVelocityFilter[nLandmarks];
		yFilters = new RelativeVelocityFilter[nLandmarks];
		// zFilters = new RelativeVelocityFilter[nLandmarks];

		for (int i = 0; i < nLandmarks; i++)
		{
			xFilters[i] = new RelativeVelocityFilter(windowSize, velocityScale);
			yFilters[i] = new RelativeVelocityFilter(windowSize, velocityScale);
			// zFilters[i] = new RelativeVelocityFilter(windowSize, velocityScale);
		}
	}

	public void process(List<Landmark> inLandmarks, List<Landmark> outLandmarks)
	{
		outLandmarks.clear();

		timestamp = System.currentTimeMillis();

		objectScale = getObjectScale(inLandmarks);

		if (objectScale < MIN_ALLOWED_OBJECT_SCALE)
		{
			outLandmarks.addAll(inLandmarks);

			return;
		}

		valueScale = 1.0f / objectScale;

		for (int i = 0; i < inLandmarks.size(); i++)
		{
			final Landmark inLandmark = inLandmarks.get(i);

			Landmark outLandmark = new Landmark(inLandmark.visibility);

			outLandmark.x = xFilters[i].apply(timestamp, valueScale, inLandmark.x * imageWidth) / imageWidth;
			outLandmark.y = yFilters[i].apply(timestamp, valueScale, inLandmark.y * imageHeight) / imageHeight;
			outLandmark.z = inLandmark.z;
			// outLandmark.z = zFilters[i].apply(timestamp, valueScale, inLandmark.z * imageWidth) / imageWidth;

			outLandmarks.add(outLandmark);
		}
	}

	public float getObjectScale(List<Landmark> landmarks)
	{
		float xMin = Float.MAX_VALUE;
		float xMax = -Float.MAX_VALUE;
		float yMin = Float.MAX_VALUE;
		float yMax = -Float.MAX_VALUE;

		for (Landmark landmark : landmarks)
		{
			if (landmark.x < xMin)
			{
				xMin = landmark.x;
			}

			if (landmark.x > xMax)
			{
				xMax = landmark.x;
			}

			if (landmark.y < yMin)
			{
				yMin = landmark.y;
			}

			if (landmark.y > yMax)
			{
				yMax = landmark.y;
			}
		}

		final float objectWidth = (xMax - xMin) * imageWidth;
		final float objectHeight = (yMax - yMin) * imageHeight;

		return (objectWidth + objectHeight) / 2.0f;
	}
}