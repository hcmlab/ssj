/*
 * StreamData.java
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

package hcm.ssj.creator.util;

import java.io.File;
import java.io.FileFilter;

import hcm.ssj.file.FileCons;

/**
 * Wrapper class for stream data management.
 */
public class StreamData
{
	private static final int DEFAULT_SELECTED = 0;
	private static final String SUFFIX = "~";

	private File[] streamDirs = new File(FileCons.SSJ_DATA).listFiles();
	private File selectedDir = streamDirs[DEFAULT_SELECTED];

	/**
	 * Return folder names of all directories that contain stream data files.
	 */
	public String[] getDirNames()
	{
		String[] streamDirNames = new String[streamDirs.length];
		for (int i = 0; i < streamDirs.length; i++)
		{
			streamDirNames[i] = streamDirs[i].getName();
		}
		return streamDirNames;
	}

	/**
	 * Return only data files containing stream data in the selected directory.
	 * @return Stream file array.
	 */
	public File[] getStreamFiles()
	{
		FileFilter filter = new FileFilter()
		{
			@Override
			public boolean accept(File file)
			{
				return file.getName().endsWith(SUFFIX);
			}
		};
		return selectedDir.listFiles(filter);
	}

	/**
	 * Select a specific stream directory for data visualization.
	 * @param index Stream file index.
	 */
	public void selectDir(int index)
	{
		selectedDir = streamDirs[index];
	}

	/**
	 * Return index of a file that is selected by default.
	 * This index is used to check radio box in a file selection dialog window when it first appears.
	 * @return Default file index.
	 */
	public int getDefaultSelected()
	{
		return DEFAULT_SELECTED;
	}
}
