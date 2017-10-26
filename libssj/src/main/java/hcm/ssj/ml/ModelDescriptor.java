/*
 * ModelDescriptor.java
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

package hcm.ssj.ml;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;

/**
 * Created by Ionut Damian on 26.10.2017.
 */

public class ModelDescriptor
{
	public ArrayList<String> classNames = new ArrayList<>();
	public int[] select_dimensions = null;
	public String modelName = null;
	public String modelFileName;
	public String modelOptionFileName;
	public int bytes = 0;
	public int dim = 0;
	public float sr = 0;
	public Cons.Type type = Cons.Type.UNDEF;

	public void parseTrainerFile(File file) throws XmlPullParserException, IOException
	{
		XmlPullParser parser = Xml.newPullParser();
		parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
		parser.setInput(new FileReader(file));

		parser.next();
		if (parser.getEventType() != XmlPullParser.START_TAG || !parser.getName().equalsIgnoreCase("trainer"))
		{
			Log.w("unknown or malformed trainer file");
			return;
		}

		while (parser.next() != XmlPullParser.END_DOCUMENT) {

			//STREAM
			if (parser.getEventType() == XmlPullParser.START_TAG && parser.getName().equalsIgnoreCase("streams")) {

				parser.nextTag(); //item
				if (parser.getEventType() == XmlPullParser.START_TAG && parser.getName().equalsIgnoreCase("item")) {

					bytes = Integer.valueOf(parser.getAttributeValue(null, "byte"));
					dim = Integer.valueOf(parser.getAttributeValue(null, "dim"));
					sr = Float.valueOf(parser.getAttributeValue(null, "sr"));
					type = Cons.Type.valueOf(parser.getAttributeValue(null, "type"));
				}
			}

			// CLASS
			if (parser.getEventType() == XmlPullParser.START_TAG && parser.getName().equalsIgnoreCase("classes"))
			{
				parser.nextTag();

				while (parser.getName().equalsIgnoreCase("item"))
				{
					if (parser.getEventType() == XmlPullParser.START_TAG)
					{
						classNames.add(parser.getAttributeValue(null, "name"));
					}
					parser.nextTag();
				}
			}

			//SELECT
			if (parser.getEventType() == XmlPullParser.START_TAG && parser.getName().equalsIgnoreCase("select")) {

				parser.nextTag(); //item
				if (parser.getEventType() == XmlPullParser.START_TAG && parser.getName().equalsIgnoreCase("item")) {

					int stream_id = Integer.valueOf(parser.getAttributeValue(null, "stream"));
					if (stream_id != 0)
						Log.w("multiple input streams not supported");
					String[] select = parser.getAttributeValue(null, "select").split(" ");
					select_dimensions = new int[select.length];
					for (int i = 0; i < select.length; i++) {
						select_dimensions[i] = Integer.valueOf(select[i]);
					}
				}
			}

			//MODEL
			if (parser.getEventType() == XmlPullParser.START_TAG && parser.getName().equalsIgnoreCase("model"))
			{
				modelName = parser.getAttributeValue(null, "create");
				modelFileName = parser.getAttributeValue(null, "path") + ".model";
				modelOptionFileName =  parser.getAttributeValue(null, "option") + ".option";
			}

			if (parser.getEventType() == XmlPullParser.END_TAG && parser.getName().equalsIgnoreCase("trainer"))
				break;
		}
	}
}
