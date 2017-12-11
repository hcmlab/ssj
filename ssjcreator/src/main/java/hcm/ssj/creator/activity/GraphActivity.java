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

package hcm.ssj.creator.activity;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.jjoe64.graphview.GraphView;
import com.obsez.android.lib.filechooser.ChooserDialog;

import java.io.File;

import hcm.ssj.audio.AudioUtils;
import hcm.ssj.audio.PlaybackListener;
import hcm.ssj.audio.PlaybackThread;
import hcm.ssj.creator.R;
import hcm.ssj.creator.main.GraphDrawer;
import hcm.ssj.creator.view.WaveformView;
import hcm.ssj.file.FileUtils;

/**
 * Visualize user-saved stream file data with the GraphView.
 */
public class GraphActivity extends AppCompatActivity
{
	private ChooserDialog chooserDialog;
	private PlaybackThread playbackThread;
	private GraphView graph;
	private GraphDrawer drawer = new GraphDrawer(graph);
	private WaveformView waveformView;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.graph_layout);

		//graph = (GraphView) findViewById(R.id.graph);
		drawer = new GraphDrawer(graph);
		waveformView = (WaveformView) findViewById(R.id.waveform);
		waveformView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

		initializeUI();
	}

	private void initializeUI()
	{
		final Button playButton = (Button) findViewById(R.id.play);
		playButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				if (playbackThread.isPlaying())
				{
					playbackThread.pause();
					playButton.setText(R.string.play);
				}
				else
				{
					playbackThread.play();
					playButton.setText(R.string.pause);
				}
			}
		});

		final Button resetButton = (Button) findViewById(R.id.reset);
		resetButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				playbackThread.reset();
				playButton.setText(R.string.play);
			}
		});


		Button loadButton = (Button) findViewById(R.id.load_stream_file);
		loadButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				chooserDialog = new ChooserDialog().with(GraphActivity.this);
				chooserDialog.withStartFile(Environment.getExternalStorageDirectory().getPath());
				chooserDialog.withChosenListener(new ChooserDialog.Result() {
					@Override
					public void onChoosePath(String path, File file) {
						try
						{
							String type = FileUtils.getFileType(file);
							if (type.equalsIgnoreCase("mp4") || type.equalsIgnoreCase("mp3"))
							{
								File rawData = AudioUtils.decode(file.getPath());
								int sampleRate = AudioUtils.getSampleRate(file.getPath());
								short[] samples = AudioUtils.getAudioSample(rawData);

								waveformView.setSampleRate(sampleRate);
								waveformView.setSamples(samples);

								playButton.setVisibility(View.VISIBLE);
								resetButton.setVisibility(View.VISIBLE);

								playbackThread = new PlaybackThread(GraphActivity.this, file, new PlaybackListener() {
									@Override
									public void onProgress(int progress)
									{
									}
									@Override
									public void onCompletion()
									{
									}
								});
							}
							else if (type.equalsIgnoreCase("stream~"))
							{
								graph.setVisibility(View.VISIBLE);
								drawer.drawGraph(file);
							}
						}
						catch (Exception e)
						{
							e.printStackTrace();
						}
					}
				}).build();
				chooserDialog.show();
			}
		});
	}
}
