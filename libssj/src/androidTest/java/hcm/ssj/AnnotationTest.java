/*
 * AnnotationTest.java
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

package hcm.ssj;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import hcm.ssj.core.Annotation;
import hcm.ssj.core.Cons;

/**
 * Created by Michael Dietz on 07.11.2017.
 */

@RunWith(AndroidJUnit4.class)
@SmallTest
public class AnnotationTest
{
	private String trainerFileName = "activity.NaiveBayes.trainer";
	private String modelPath = "/sdcard/SSJ/Creator/res";

	// Helper variables
	private String modelFileName;
	private String modelOptionFileName;

	private int[] select_dimensions;
	private String[] classNames;
	private int bytes;
	private int dim;
	private float sr;
	private Cons.Type type;

	@Test
	public void LoadSaveTest() throws Exception
	{
		Annotation anno = new Annotation();
		anno.load("/sdcard/SSJ/mouse.annotation");
		anno.save("/sdcard/SSJ/mouse2.annotation");

		anno.convertToFrames(1.0, "xx", 0, 0.5);
		anno.save("/sdcard/SSJ/mouse_cont.annotation");

		return;

	}
}
