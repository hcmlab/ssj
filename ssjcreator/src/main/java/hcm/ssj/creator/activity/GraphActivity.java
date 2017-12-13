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
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.obsez.android.lib.filechooser.ChooserDialog;

import java.io.File;
import java.util.ArrayList;

import hcm.ssj.audio.AudioDecoder;
import hcm.ssj.audio.PlaybackListener;
import hcm.ssj.audio.PlaybackThread;
import hcm.ssj.creator.R;
import hcm.ssj.creator.view.TimeAxisView;
import hcm.ssj.creator.view.WaveformView;
import hcm.ssj.file.FileUtils;

/**
 * Visualize user-saved stream file data with the GraphView.
 */
public class GraphActivity extends AppCompatActivity
{
	private ChooserDialog chooserDialog;
	private PlaybackThread playbackThread;
	private TimeAxisView timeAxisView;
	private DisplayMetrics displayMetrics;
	private LinearLayout streamLayout;

	private ArrayList<WaveformView> waveforms = new ArrayList<>();

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.graph_layout);

		displayMetrics = new DisplayMetrics();

		timeAxisView = (TimeAxisView) findViewById(R.id.time_axis);
		streamLayout = (LinearLayout) findViewById(R.id.stream_layout);

		initializeUI();
	}

	/*
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int x = (int) event.getX();
		if (waveformView != null && playbackThread != null)
		{
			getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
			int width = displayMetrics.widthPixels;
			int length = playbackThread.getAudioLength();
			float progress = ((float) x / (float) width) * length;
			timeAxisView.setMarkerPosition((int) progress);
			playbackThread.seekTo((int) progress);
		}
		return false;
	}
	*/

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
							if (type.equalsIgnoreCase("mp4")
									|| type.equalsIgnoreCase("mp3")
									|| type.equalsIgnoreCase("wav"))
							{
								AudioDecoder decoder = new AudioDecoder(file.getPath());

								timeAxisView.setAudioLength(decoder.getAudioLength());
								WaveformView waveform = new WaveformView(GraphActivity.this);
								waveform.setSamples(decoder.getSamples());
								streamLayout.addView(waveform, 0);

								playButton.setVisibility(View.VISIBLE);
								resetButton.setVisibility(View.VISIBLE);

								playbackThread = new PlaybackThread(GraphActivity.this, file, new PlaybackListener() {
									@Override
									public void onProgress(int progress)
									{
										timeAxisView.setMarkerPosition(progress);
									}
									@Override
									public void onCompletion()
									{
										playButton.setText(R.string.play);
										timeAxisView.setMarkerPosition(-1);
									}
								});
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
