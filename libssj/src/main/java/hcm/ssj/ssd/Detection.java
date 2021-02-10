/*
 * Detection.java
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

import java.util.ArrayList;
import java.util.List;

public class Detection
{
	public int id;
	public float yMin;
	public float xMin;
	public float width;
	public float height;
	public float score;
	public int classId;
	public List<Keypoint> keypoints = new ArrayList<>();

	public float getArea()
	{
		return width * height;
	}

	public float getOverlap(Detection other)
	{
		float overlap = 0;

		// Get top left coordinate of overlapping area
		float overlapX1 = Math.max(xMin, other.xMin);
		float overlapY1 = Math.max(yMin, other.yMin);

		// Get bottom right coordinate of overlapping area
		float overlapX2 = Math.min(xMin + width, other.xMin + other.width);
		float overlapY2 = Math.min(yMin + height, other.yMin + other.height);

		float overlapWidth = Math.max(0, overlapX2 - overlapX1);
		float overlapHeight = Math.max(0, overlapY2 - overlapY1);

		float overlapArea = overlapWidth * overlapHeight;

		if (overlapArea > 0)
		{
			// Overlap value is calculated by dividing the intersection through the total area
			overlap = overlapArea / (getArea() + other.getArea() - overlapArea);
		}

		return overlap;
	}
}
