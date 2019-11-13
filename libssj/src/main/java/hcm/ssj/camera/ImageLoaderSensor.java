/*
 * StaticImageLoader.java
 * Copyright (c) 2019
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

package hcm.ssj.camera;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;

import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.Sensor;
import hcm.ssj.core.option.FolderPath;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.file.FileCons;

/**
 * Created by Michael Dietz on 13.11.2019.
 */
public class ImageLoaderSensor extends Sensor
{
	public class Options extends OptionList
	{
		public final Option<FolderPath> filePath = new Option<>("path", new FolderPath(FileCons.SSJ_EXTERNAL_STORAGE + File.separator + "[time]"), FolderPath.class, "folder containing image file");
		public final Option<String> fileName = new Option<>("fileName", "image.jpg", String.class, "image name");

		private Options()
		{
			addOptions();
		}
	}

	public final Options options = new Options();

	@Override
	public OptionList getOptions()
	{
		return options;
	}

	protected Bitmap image;

	public ImageLoaderSensor()
	{
		_name = this.getClass().getSimpleName();
	}

	@Override
	protected boolean connect() throws SSJFatalException
	{
		loadImage();

		return true;
	}

	protected void loadImage() throws SSJFatalException
	{
		File imagePath = new File(options.filePath.parseWildcards(), options.fileName.get());

		if (imagePath.exists())
		{
			image = BitmapFactory.decodeFile(imagePath.getAbsolutePath());
		}
		else
		{
			throw new SSJFatalException("Image " + imagePath.getAbsolutePath() + " does not exist!");
		}
	}

	protected int getImageWidth()
	{
		int result = -1;

		if (image != null)
		{
			result = image.getWidth();
		}

		return result;
	}

	protected int getImageHeight()
	{
		int result = -1;

		if (image != null)
		{
			result = image.getHeight();
		}

		return result;
	}

	@Override
	protected void disconnect() throws SSJFatalException
	{
		if (image != null)
		{
			image.recycle();
			image = null;
		}
	}
}
