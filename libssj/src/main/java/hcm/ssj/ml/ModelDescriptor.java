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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.Pipeline;
import hcm.ssj.core.Util;
import hcm.ssj.core.stream.Stream;
import hcm.ssj.file.FileCons;
import hcm.ssj.file.FileUtils;

/**
 * Created by Ionut Damian on 26.10.2017.
 */

public class ModelDescriptor
{
	private String classNames[];

	private int[] select_dimensions = null;
	private String modelType = null;
	private String modelFileName;
	private String modelOptionFileName;
	private int bytes = 0;
	private int dim = 0;
	private double sr = 0;
	private Cons.Type type = Cons.Type.UNDEF;
	private String dirPath = null;

	private IModelHandler source = null;
	private Model model;

	public ModelDescriptor(IModelHandler source)
	{
		this.source = source;
	}

	public ModelDescriptor(String filePath) throws IOException, XmlPullParserException
	{
		String fileName = filePath.substring(filePath.lastIndexOf(File.separator));
		dirPath = filePath.substring(0, filePath.lastIndexOf(File.separator));

		parseTrainerFile(FileUtils.getFile(dirPath, fileName));
	}

	public ModelDescriptor(Model model, int bytes_input, int dim_input, double sr_input, Cons.Type type_input)
	{
		this.model = model;
		modelType = model.getClass().getSimpleName();

		this.bytes = bytes_input;
		this.dim = dim_input;
		this.sr = sr_input;
		this.type = type_input;

		this.classNames = model.getClassNames();
	}

	public void loadModel() throws IOException
	{
		if(source != null)
		{
			source.getModelDescriptor().waitForModelLoad();
			model = source.getModelDescriptor().getModel();
		}
		else
		{
			model = Model.create(modelType);
			model.setNumClasses(classNames.length);
			model.setClassNames(classNames);

			model.load(FileUtils.getFile(dirPath, modelFileName + "." + FileCons.FILE_EXTENSION_MODEL));

			if(modelOptionFileName != null && !modelOptionFileName.isEmpty())
				model.loadOption(FileUtils.getFile(dirPath, modelOptionFileName + "." + FileCons.FILE_EXTENSION_OPTION));

			//wake up threads waiting for model load
			Log.d("model loaded, waking up waiting threads ... ");
			synchronized (this)
			{
				this.notifyAll();
			}
		}
	}

	private void waitForModelLoad()
	{
		if(model == null)
		{
			Log.d("model not loaded yet, waiting ... ");
			synchronized (this)
			{
				try
				{
					this.wait();
				}
				catch (InterruptedException e)
				{
				}
			}
		}
	}

	public Model getModel()
	{
		return model;
	}

	public void validateInput(Stream input[]) throws IOException
	{
		if(source != null)
		{
			source.getModelDescriptor().validateInput(input);
			return;
		}

		if(model == null || !model.isTrained())
		{
			throw new IOException("model not loaded");
		}

		if(input[0].bytes != bytes || input[0].type != type) {
			throw new IOException("input stream (type=" + input[0].type + ", bytes=" + input[0].bytes
												+ ") does not match model's expected input (type=" + type + ", bytes=" + bytes + ", sr=" + sr + ")");
		}
		if(input[0].sr != sr) {
			Log.w("input stream (sr=" + input[0].sr + ") may not be correct for model (sr=" + sr + ")");
		}

		if(input[0].dim != dim) {
			throw new IOException("input stream (dim=" + input[0].dim + ") does not match model (dim=" + dim + ")");
		}
		if (input[0].num > 1) {
			Log.w ("stream num > 1, only first sample is used");
		}
	}

	private void parseTrainerFile(File file) throws XmlPullParserException, IOException
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

		ArrayList<String> classNamesList = new ArrayList<>();

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
						classNamesList.add(parser.getAttributeValue(null, "name"));
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
				modelType = parser.getAttributeValue(null, "create");
				modelFileName = parser.getAttributeValue(null, "path");
				modelOptionFileName =  parser.getAttributeValue(null, "option");
			}

			if (parser.getEventType() == XmlPullParser.END_TAG && parser.getName().equalsIgnoreCase("trainer"))
				break;
		}

		classNames = classNamesList.toArray(new String[0]);
	}

	public void save(String path, String name) throws IOException
	{
		if(model == null)
			return;

		if(name.endsWith(FileCons.FILE_EXTENSION_TRAINER + FileCons.TAG_DATA_FILE))
		{
			name = name.substring(0, name.length()-2);
		}
		else if(!name.endsWith(FileCons.FILE_EXTENSION_TRAINER))
		{
			name += "." + FileCons.FILE_EXTENSION_TRAINER;
		}

		//parse wildcards
		if (path.contains("[time]"))
		{
			path = path.replace("[time]", Util.getTimestamp(Pipeline.getInstance().getCreateTimeMs()));
		}

		StringBuilder builder = new StringBuilder();

		builder.append("<trainer ssi-v=\"5\" ssj-v=\"");
		builder.append(Pipeline.getVersion());
		builder.append("\">").append(FileCons.DELIMITER_LINE);

		builder.append("<info trained=\"");
		builder.append(model.isTrained());
		builder.append("\"/>").append(FileCons.DELIMITER_LINE);

		builder.append("<streams>").append(FileCons.DELIMITER_LINE);
		builder.append("<item byte=\"");
		builder.append(bytes);
		builder.append("\" dim=\"");
		builder.append(dim);
		builder.append("\" sr=\"");
		builder.append(sr);
		builder.append("\" type=\"");
		builder.append(type);
		builder.append("\"/>").append(FileCons.DELIMITER_LINE);
		builder.append("</streams>").append(FileCons.DELIMITER_LINE);

		builder.append("<classes>").append(FileCons.DELIMITER_LINE);
		for(String className : classNames)
		{
			builder.append("<item name=\"");
			builder.append(className);
			builder.append("\"/>").append(FileCons.DELIMITER_LINE);
		}
		builder.append("</classes>").append(FileCons.DELIMITER_LINE);

		builder.append("<users>").append(FileCons.DELIMITER_LINE);
		builder.append("<item name=\"userLocal\"/>").append(FileCons.DELIMITER_LINE);
		builder.append("</users>").append(FileCons.DELIMITER_LINE);

		modelFileName = name + "." + modelType;
		modelOptionFileName = name + "." + modelType;

		builder.append("<model create=\"");
		builder.append(modelType);
		builder.append("\" stream=\"0\" path=\"");
		builder.append(modelFileName);
		//builder.append("\" option=\"");
		//builder.append(modelOptionFileName);
		builder.append("\"/>").append(FileCons.DELIMITER_LINE);

		builder.append("</trainer>").append(FileCons.DELIMITER_LINE);

		OutputStream ouputStream = new FileOutputStream(new File(path, name));
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(ouputStream));

		writer.write(builder.toString());
		writer.flush();
		writer.close();

		model.save(new File(path, modelFileName + "." + FileCons.FILE_EXTENSION_MODEL));
	}

	public String[] getClassNames()
	{
		if(source != null)
			return source.getModelDescriptor().getClassNames();

		return classNames;
	}

	public int getNumClasses()
	{
		if(source != null)
			return source.getModelDescriptor().getNumClasses();

		return classNames.length;
	}

	public int[] getSelectDimensions()
	{
		if(source != null)
			return source.getModelDescriptor().getSelectDimensions();

		return select_dimensions;
	}

	public String getModelType()
	{
		if(source != null)
			return source.getModelDescriptor().getModelType();

		return modelType;
	}

	public String getModelFileName()
	{
		if(source != null)
			return source.getModelDescriptor().getModelFileName();

		return modelFileName;
	}

	public String getModelOptionFileName()
	{
		if(source != null)
			return source.getModelDescriptor().getModelOptionFileName();

		return modelOptionFileName;
	}

	public int getStreamBytes()
	{
		if(source != null)
			return source.getModelDescriptor().getStreamBytes();

		return bytes;
	}

	public int getStreamDimension()
	{
		if(source != null)
			return source.getModelDescriptor().getStreamDimension();

		return dim;
	}

	public double getStreamSampleRate()
	{
		if(source != null)
			return source.getModelDescriptor().getStreamSampleRate();

		return sr;
	}

	public Cons.Type getStreamType()
	{
		if(source != null)
			return source.getModelDescriptor().getStreamType();

		return type;
	}
}
