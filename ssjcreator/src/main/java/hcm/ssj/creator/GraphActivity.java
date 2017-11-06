/*
 * GraphActivity.java
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

package hcm.ssj.creator;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import hcm.ssj.creator.dialogs.StreamDataDialog;

/**
 * Visualize user-saved stream file data with the GraphView.
 */
public class GraphActivity extends AppCompatActivity
{
	private static File[] streamFiles;
	private static GraphView graph;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.graph_layout);

		graph = (GraphView) findViewById(R.id.graph);

		Button loadButton = (Button) findViewById(R.id.load_stream_file);
		loadButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				StreamDataDialog dialog = new StreamDataDialog();
				dialog.show(getSupportFragmentManager(), GraphActivity.this.getClass().getSimpleName());
			}
		});
	}

	public static void setStreamFiles(File[] files)
	{
		streamFiles = files;

		for (File f : files)
		{
			try
			{
				BufferedReader br = new BufferedReader(new FileReader(f));
				LineGraphSeries<DataPoint> series = new LineGraphSeries<>();
				int i = 0;

				String line;
				while ((line = br.readLine()) != null)
				{
					String[] currentColumn = line.split(" ");
					float point = Float.parseFloat(currentColumn[2]);
					series.appendData(new DataPoint(i++, point), false, Integer.MAX_VALUE);
				}
				graph.addSeries(series);
			}
			catch (Exception e)
			{
				e.printStackTrace();
				return;
			}
		}
	}
}
