/*
 * FileDownloadTest.java
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

import java.io.File;
import java.io.IOException;

import hcm.ssj.file.FileUtils;

import static org.junit.Assert.assertTrue;

/**
 * Downloads necessary files for inference with Inception and tests whether
 * they all are on SD card thereafter.
 *
 * @author Vitaly
 */

@RunWith(AndroidJUnit4.class)
@SmallTest
public class FileDownloadTest
{
	private static final String assetsURL = "https://raw.githubusercontent.com/vitaly-krumins/ssj/master/libssj/src/androidTest/assets/";
	private static final String trainer = "inception.trainer";
	private static final String model = "inception.model";
	private static final String option = "inception.option";


	@Test
	public void downloadTrainerFile()
	{
		File file = null;
		try
		{
			file = FileUtils.downloadFile(assetsURL, trainer);
		}
		catch (IOException e)
		{
			throw new RuntimeException();
		}
		assertTrue(file.exists());
	}


	@Test
	public void downloadModelFile()
	{
		File file = null;
		try
		{
			file = FileUtils.downloadFile(assetsURL, model);
		}
		catch (IOException e)
		{
			throw new RuntimeException();
		}
		assertTrue(file.exists());
	}


	@Test
	public void downloadOptionFile()
	{
		File file = null;
		try
		{
			file = FileUtils.downloadFile(assetsURL, option);
		}
		catch (IOException e)
		{
			throw new RuntimeException();
		}
		assertTrue(file.exists());
	}
}
