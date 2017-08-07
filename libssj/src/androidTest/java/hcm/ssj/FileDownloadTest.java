/*
 * FileDownloadTest.java
 * Copyright (c) 2017
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

package hcm.ssj;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import hcm.ssj.file.FileDownloader;
import hcm.ssj.file.LoggingConstants;

/**
 * @author Vitaly
 */

@RunWith(AndroidJUnit4.class)
@SmallTest
public class FileDownloadTest
{
	private static final String trainerURL = "https://raw.githubusercontent.com/vitaly-krumins/ssj/master/libssj/src/androidTest/assets/inception.trainer";
	private static final String modelURL = "https://raw.githubusercontent.com/vitaly-krumins/ssj/master/libssj/src/androidTest/assets/inception.model";
	private static final String optionURL = "https://raw.githubusercontent.com/vitaly-krumins/ssj/master/libssj/src/androidTest/assets/inception.option";
	private static final String modelsDir = LoggingConstants.TENSORFLOW_MODELS_DIR;


	@Test
	public void downloadAllFiles()
	{
		downloadFile(trainerURL, "inception.trainer");
		downloadFile(modelURL, "inception.model");
		downloadFile(optionURL, "inception.option");
	}


	private void downloadFile(String url, String fileName)
	{
		File filePath = new File(modelsDir + File.separator + fileName);

		if (!filePath.exists())
		{
			FileDownloader.downloadFile(url, fileName);
		}
	}
}
