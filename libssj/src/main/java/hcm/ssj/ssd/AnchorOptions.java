/*
 * AnchorOptions.java
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

package hcm.ssj.ssd;

/**
 * Created by Michael Dietz on 11.01.2021.
 */
public class AnchorOptions
{
	public int numLayers = 4;
	public float minScale = 0.1484375f;
	public float maxScale = 0.75f;
	public int inputSizeHeight = 128;
	public int inputSizeWidth = 128;
	public float anchorOffsetX = 0.5f;
	public float anchorOffsetY = 0.5f;
	public int[] strides = new int[] {8, 16, 16, 16};
	public float[] aspectRatios = new float[] {1.0f};
	public boolean fixedAnchorSize = true;

	public boolean reduceBoxesInLowestLayer = false;
	public float interpolatedScaleAspectRatio = 1.0f;
	public int[] featureMapHeight = new int[] {};
	public int[] featureMapWidth = new int[] {};
}
