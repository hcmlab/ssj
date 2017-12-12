/*
 * FileChooser.java
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

import android.app.Activity;

import com.obsez.android.lib.filechooser.ChooserDialog;

import java.io.File;

/**
 * Created by Ionut Damian on 12.12.2017.
 */

public abstract class FileChooser
{
	private ChooserDialog chooserDialog;

	public FileChooser(Activity activity, String startPath, boolean dirOnly, String... extensions)
	{
		chooserDialog = new ChooserDialog().with(activity);
		chooserDialog.withStartFile(startPath);
		chooserDialog.withFilter(dirOnly, false, extensions);

		chooserDialog.withChosenListener(new ChooserDialog.Result()
		{
			@Override
			public void onChoosePath(String path, File pathFile)
			{
				onResult(path, pathFile);
			}
		}).build();
	}

	public void show()
	{
		chooserDialog.show();
	}

	public abstract void onResult(String path, File pathFile);
}
