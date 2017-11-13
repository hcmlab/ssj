/*
 * GraphDrawer.java
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

package hcm.ssj.creator.main;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

/**
 * Visualize stream file data on the GraphView.
 */
public class GraphDrawer
{
	private GraphView graph;

	/**
	 * Instantiate stream data visualizer.
	 * @param graphView Reference to the GraphView on which to draw the data.
	 */
	public GraphDrawer(GraphView graphView)
	{
		graph = graphView;
	}

	/**
	 * Visualize all stream file data located in a selected directory.
	 * @param streamFiles List of stream data files to visualize.
	 */
	public void drawData(File[] streamFiles)
	{
		for (File steamFile : streamFiles)
		{
			drawGraph(steamFile);
		}
	}

	/**
	 * Visualize data of a given stream file.
	 * @param file Stream file.
	 */
	public void drawGraph(File file)
	{
		graph.removeAllSeries();
		int columnNum = getColumnNum(file);
		try
		{
			for (int col = 0; col < columnNum; col++)
			{
				BufferedReader br = new BufferedReader(new FileReader(file));
				LineGraphSeries<DataPoint> series = new LineGraphSeries<>();
				int count = 0;
				String dataRow;
				while ((dataRow = br.readLine()) != null)
				{

					DataPoint point = new DataPoint(count++, Float.parseFloat(dataRow.split(" ")[col]));
					series.appendData(point, false, Integer.MAX_VALUE);
				}
				graph.addSeries(series);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Get number of columns of a given stream file.
	 * @param file Stream file.
	 * @return Number of columns.
	 */
	private int getColumnNum(File file)
	{
		try
		{
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line = br.readLine();
			return line.split(" ").length;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return 0;
		}
	}
}
