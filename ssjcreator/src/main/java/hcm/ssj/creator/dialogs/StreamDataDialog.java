/*
 * StreamDataDialog.java
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

package hcm.ssj.creator.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import java.io.File;
import java.io.FileFilter;

import hcm.ssj.creator.GraphActivity;
import hcm.ssj.creator.R;
import hcm.ssj.file.FileCons;

/**
 * Dialog for stream file selection to visualize data via GraphView.
 */
public class StreamDataDialog extends DialogFragment
{
	private static final int DEFAULT_SELECTED = 0;
	private static final String SUFFIX = "~";

	private File[] streamDirs = new File(FileCons.SSJ_DATA).listFiles();
	private File selectedDir = streamDirs[DEFAULT_SELECTED];
	private String[] streamDirNames;

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle("Select stream file");
		getStreamDirNames();

		builder.setPositiveButton(R.string.str_ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i)
			{
				File[] streamFiles = selectedDir.listFiles(new FileFilter()
				{
					@Override
					public boolean accept(File file)
					{
						return file.getName().endsWith(SUFFIX);
					}
				});

				GraphActivity.setStreamFiles(streamFiles);
			}
		});

		builder.setNegativeButton(R.string.str_cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i)
			{
			}
		});

		builder.setSingleChoiceItems(streamDirNames, DEFAULT_SELECTED, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				selectedDir = streamDirs[which];
			}
		});

		return builder.create();
	}

	/**
	 * Cache folder names of all directories that contain stream data files.
	 */
	private void getStreamDirNames()
	{
		streamDirNames = new String[streamDirs.length];

		for (int i = 0; i < streamDirs.length; i++)
		{
			streamDirNames[i] = streamDirs[i].getName();
		}
	}
}
