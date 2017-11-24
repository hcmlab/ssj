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

package hcm.ssj.core;

import android.os.Environment;

import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import hcm.ssj.file.FileCons;
import hcm.ssj.file.SimpleXmlParser;

import static hcm.ssj.core.Cons.GARBAGE_CLASS;
import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * Created by Johnny on 30.03.2017.
 */

public class Annotation
{
	public class Entry
	{
		public String classlabel;
		public double from;
		public double to;
		public float confidence = 1.0f;

		public Entry(String classlabel, double from, double to)
		{
			this.classlabel = classlabel;
			this.from = from;
			this.to = to;
		}

		public Entry(String classlabel, double from, double to, float confidence)
		{
			this.classlabel = classlabel;
			this.from = from;
			this.to = to;
			this.confidence = confidence;
		}
	}

	private ArrayList<String> classes = new ArrayList<>();
	private ArrayList<Entry> entries = new ArrayList<>();
	private String name;
	private String path;

	private EventChannel channel = new EventChannel();

	public Annotation()
	{
		clear();
	}

	public Annotation(Annotation original)
	{
		this.name = original.name;
		this.path = original.path;

		for(Entry e : original.getEntries())
		{
			this.getEntries().add(new Entry(e.classlabel, e.from, e.to));
		}

		for(String classlabel : original.getClasses())
		{
			this.getClasses().add(classlabel);
		}
	}

	public ArrayList<String> getClasses()
	{
		return classes;
	}

	public ArrayList<Entry> getEntries()
	{
		return entries;
	}

	public void setClasses(ArrayList<String> classes)
	{
		this.classes = classes;
	}

	public void addClass(String anno)
	{
		this.classes.add(anno);
	}

	public void addEntry(String label, double from, double to)
	{
		addEntry(new Entry(label, from, to));
	}

	public void addEntry(Entry e)
	{
		this.entries.add(e);

		if(classes.indexOf(e.classlabel) == -1)
		{
			classes.add(e.classlabel);
		}
	}

	public void removeClass(String anno)
	{
		this.classes.remove(anno);
	}

	public void clear()
	{
		name = "anno";
		path = (Environment.getExternalStorageDirectory().getAbsolutePath()
				+ File.separator + "SSJ" + File.separator + "[time]");

		entries.clear();
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

	public void sort()
	{
		Collections.sort(entries, new Comparator<Entry>() {
			@Override
			public int compare(Entry lhs, Entry rhs)
			{
				if (lhs.from == rhs.from)
				{
					if (lhs.to < rhs.to)
						return -1;
					else if(lhs.to > rhs.to)
						return 1;
					else
						return 0;
				}
				else
				{
					if (lhs.from < rhs.from)
						return -1;
					else if(lhs.from > rhs.from)
						return 1;
					else
						return 0;
				}
			}
		});
	}

	public boolean convertToFrames(double frame_s, String emptyClassName, double duration, double empty_percent)
	{
		if(entries == null || entries.size() == 0 || classes == null || classes.size() == 0)
			return false;

		boolean add_empty = emptyClassName != null;

		if (duration <= 0)
		{
			duration = entries.get(entries.size()-1).to;
		}

		int n_frames = (int)(duration / frame_s);

		double frame_dur = frame_s;
		double frame_from = 0;
		double frame_to = frame_dur;

		HashMap<String, Double> percent_class = new HashMap<>();
		double percent_garbage;

		// copy labels and clear annotation
		Annotation original = new Annotation(this);
		original.sort();
		entries.clear();

		int iter = 0;
		int last_iter = iter;
		int clone_end = original.getEntries().size();

		for (int i = 0; i < n_frames; i++)
		{
			Entry new_entry = new Entry(emptyClassName, frame_from, frame_from + frame_dur);

			for (String cl : original.getClasses())
			{
				percent_class.put(cl, 0.0);
			}
			percent_garbage = 0;

			// skip labels before the current frame
			iter = last_iter;
			while (iter != clone_end && original.getEntries().get(iter).to < frame_from)
			{
				iter++;
				last_iter++;
			}

			if (iter != clone_end)
			{
				boolean found_at_least_one = false;

				// find all classes within the current frame
				while (iter != clone_end && original.getEntries().get(iter).from < frame_to)
				{
					Entry e = original.getEntries().get(iter);
					double dur = (min(frame_to, e.to) - max(frame_from, e.from)) / frame_dur;
					if (e.classlabel == null)
					{
						percent_garbage += dur;
					}
					else
					{
						percent_class.put(e.classlabel, percent_class.get(e.classlabel) + dur);
					}
					iter++;
					found_at_least_one = true;
				}

				if (found_at_least_one)
				{
					// find dominant class
					double max_percent = percent_garbage;
					double percent_sum = percent_garbage;
					String max_class = GARBAGE_CLASS;
					for (String cl : original.getClasses())
					{
						if (max_percent < percent_class.get(cl))
						{
							max_class = cl;
							max_percent = percent_class.get(cl);
						}
						percent_sum += percent_class.get(cl);
					}

					// add label
					if (percent_sum > empty_percent)
					{
						new_entry.classlabel = (max_class != null) ? max_class : GARBAGE_CLASS;
						addEntry(new_entry);
					}
					else if (add_empty)
					{
						addEntry(new_entry);
					}
				}
				else if (add_empty)
				{
					addEntry(new_entry);
				}
			}
			else if (add_empty) {
				addEntry(new_entry);
			}

			frame_from += frame_s;
			frame_to += frame_s;
		}
		return true;
	}


	public void load() throws IOException, XmlPullParserException
	{
		load(path + File.separator + name);
	}

	public void load(String path) throws IOException, XmlPullParserException
	{
		if(path.endsWith(FileCons.FILE_EXTENSION_ANNO + FileCons.TAG_DATA_FILE))
		{
			path = path.substring(0, path.length()-2);
		}
		else if(!path.endsWith(FileCons.FILE_EXTENSION_ANNO))
		{
			path += "." + FileCons.FILE_EXTENSION_ANNO;
		}

		/*
		 * INFO
		 */
		SimpleXmlParser simpleXmlParser = new SimpleXmlParser();
		SimpleXmlParser.XmlValues xmlValues = simpleXmlParser.parse(
				new FileInputStream(new File(path)),
				new String[]{"annotation", "info"},
				new String[]{"size"}
		);
		//resize array list
		for(int i = 0; i < Integer.valueOf(xmlValues.foundAttributes.get(0)[0]); i++)
			classes.add(null);

		/*
		 * SCHEME
		 */
		simpleXmlParser = new SimpleXmlParser();
		xmlValues = simpleXmlParser.parse(
				new FileInputStream(new File(path)),
				new String[]{"annotation", "scheme"},
				new String[]{"type"}
		);

		for(String[] scheme : xmlValues.foundAttributes)
		{
			if (!scheme[0].equalsIgnoreCase("DISCRETE"))
			{
				Log.e("unsupported annotation scheme: " + scheme[0]);
				return;
			}
		}

		/*
		 * SCHEME ITEMS
		 */
		simpleXmlParser = new SimpleXmlParser();
		xmlValues = simpleXmlParser.parse(
				new FileInputStream(new File(path)),
				new String[]{"annotation", "scheme", "item"},
				new String[]{"id", "name"}
		);

		for(String[] item : xmlValues.foundAttributes)
		{
			classes.set(Integer.valueOf(item[0]), item[1]); //id, name
		}

		loadData(path + FileCons.TAG_DATA_FILE);
	}

	private void loadData(String path) throws IOException, XmlPullParserException
	{
		InputStream inputStream = new FileInputStream(new File(path));
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

		String line = reader.readLine();
		while(line != null)
		{
			String[] tokens = line.split(FileCons.DELIMITER_ANNOTATION);
			Entry e = new Entry(classes.get(Integer.valueOf(tokens[2])),
								Double.valueOf(tokens[0]),
								Double.valueOf(tokens[1]),
								Float.valueOf(tokens[3]));
			addEntry(e);
			line = reader.readLine();
		}
	}

	public void save() throws IOException, XmlPullParserException
	{
		save(path + File.separator + name);
	}

	public void save(String path) throws IOException, XmlPullParserException
	{
		if(path.endsWith(FileCons.FILE_EXTENSION_ANNO + FileCons.TAG_DATA_FILE))
		{
			path = path.substring(0, path.length()-2);
		}
		else if(!path.endsWith(FileCons.FILE_EXTENSION_ANNO))
		{
			path += "." + FileCons.FILE_EXTENSION_ANNO;
		}

		//parse wildcards
		if (path.contains("[time]"))
		{
			path = path.replace("[time]", Util.getTimestamp(Pipeline.getInstance().getCreateTimeMs()));
		}

		StringBuilder builder = new StringBuilder();

		builder.append("<annotation ssi-v=\"3\" ssj-v=\"");
		builder.append(Pipeline.getVersion());
		builder.append("\">").append(FileCons.DELIMITER_LINE);

		builder.append("<info ftype=\"ASCII\" size=\"");
		builder.append(classes.size());
		builder.append("\"/>").append(FileCons.DELIMITER_LINE);

		builder.append("<scheme name=\"ssj\" type=\"DISCRETE\">").append(FileCons.DELIMITER_LINE);
		for(int i = 0; i < classes.size(); ++i)
		{
			builder.append("<item name=\"");
			builder.append(classes.get(i));
			builder.append("\" id=\"");
			builder.append(i);
			builder.append("\"/>").append(FileCons.DELIMITER_LINE);
		}
		builder.append("</scheme>").append(FileCons.DELIMITER_LINE);
		builder.append("</annotation>").append(FileCons.DELIMITER_LINE);

		OutputStream ouputStream = new FileOutputStream(new File(path));
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(ouputStream));

		writer.write(builder.toString());
		writer.flush();
		writer.close();

		saveData(path + FileCons.TAG_DATA_FILE);
	}

	private void saveData(String path) throws IOException, XmlPullParserException
	{
		OutputStream ouputStream = new FileOutputStream(new File(path));
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(ouputStream));

		StringBuilder builder = new StringBuilder();
		for(Entry e : entries)
		{
			builder.delete(0, builder.length());

			builder.append(e.from).append(FileCons.DELIMITER_ANNOTATION);
			builder.append(e.to).append(FileCons.DELIMITER_ANNOTATION);
			builder.append(classes.indexOf(e.classlabel)).append(FileCons.DELIMITER_ANNOTATION);
			builder.append(e.confidence);

			writer.write(builder.toString());
			writer.newLine();
		}

		writer.flush();
		writer.close();
	}
}
