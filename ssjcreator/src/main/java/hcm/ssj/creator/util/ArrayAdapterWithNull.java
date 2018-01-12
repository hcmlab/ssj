/*
 * ArrayAdapterWithNull.java
 * Copyright (c) 2018
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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Ionut Damian on 02.11.2017.
 */

public class ArrayAdapterWithNull extends ArrayAdapter<Object>
{
	private Context context;
	private String defaultLabel;
	private int resource;

	public ArrayAdapterWithNull(Context context, int resource, ArrayList<Object> objects, String defaultLabel)
	{
		super(context, resource, objects);
		this.context = context;
		this.defaultLabel = defaultLabel;
		this.resource = resource;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		TextView label;
		if (convertView == null) {
			label = (TextView)LayoutInflater.from(context).inflate(resource, parent, false);
		} else {
			label = (TextView)convertView;
		}

		String text = (getItem(position) == null) ? defaultLabel : getItem(position).getClass().getSimpleName();
		label.setText(text);
		return label;
	}

	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {

		TextView label;
		if (convertView == null) {
			label = (TextView)LayoutInflater.from(context).inflate(resource, parent, false);
		} else {
			label = (TextView)convertView;
		}

		String text = (getItem(position) == null) ? defaultLabel : getItem(position).getClass().getSimpleName();
		label.setText(text);
		return label;
	}
}
