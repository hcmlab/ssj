/*
 * Annotation.java
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

package hcm.ssj.creator.core;

import android.os.Environment;

import java.io.File;
import java.util.ArrayList;

import hcm.ssj.core.EventChannel;
import hcm.ssj.creator.util.Util;

/**
 * Created by Johnny on 30.03.2017.
 */

public class Annotation
{
	private ArrayList<String> classes = new ArrayList<>();
	private String name;
	private String path;

	private EventChannel channel = new EventChannel();

	private static Annotation instance;

	public static Annotation getInstance()
	{
		if(instance == null)
			instance = new Annotation();

		return instance;
	}

	private Annotation()
	{
		clear();
	}

	public ArrayList<String> getClasses()
	{
		return classes;
	}

	public void setClasses(ArrayList<String> classes)
	{
		this.classes = classes;
	}

	public void addClass(String anno)
	{
		this.classes.add(anno);
	}

	public void removeClass(String anno)
	{
		this.classes.remove(anno);
	}

	public void clear()
	{
		name = "anno";
		path = (Environment.getExternalStorageDirectory().getAbsolutePath()
				+ File.separator + Util.SSJ + File.separator + "[time]");
		classes.clear();
	}


	public String getFileName()
	{
		return name;
	}

	public void setFileName(String name)
	{
		this.name = name;
	}

	public String getFilePath()
	{
		return path;
	}

	public void setFilePath(String path)
	{
		this.path = path;
	}

	public EventChannel getChannel()
	{
		return channel;
	}
}
