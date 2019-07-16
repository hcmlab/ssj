/*
 * GraphActivity.java
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

package hcm.ssj.creator.activity;

import android.os.Bundle;
import android.os.Environment;
import androidx.appcompat.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import com.obsez.android.lib.filechooser.ChooserDialog;

import java.io.File;

import hcm.ssj.audio.AudioDecoder;
import hcm.ssj.audio.PlaybackListener;
import hcm.ssj.audio.PlaybackThread;
import hcm.ssj.core.stream.Stream;
import hcm.ssj.creator.R;
import hcm.ssj.creator.util.PlaybackThreadList;
import hcm.ssj.creator.view.StreamLayout;
import hcm.ssj.creator.view.StreamView;
import hcm.ssj.creator.view.WaveformView;
import hcm.ssj.file.FileCons;
import hcm.ssj.file.FileUtils;

/**
 * Visualizes user-saved data. This class supports visualization of stream files (.stream~) as well
 * as multiple media files like .mp3, .mp4, and .wav.
 */
public class GraphActivity extends AppCompatActivity
{
	private static final String SUPPORTED_MEDIA_TYPES = "mp3|mp4|wav";

	private ChooserDialog chooserDialog;
	private PlaybackThreadList playbackThreads = new PlaybackThreadList();
	private StreamLayout streamLayout;

	private Button playButton;
	private Button resetButton;

	private int maxAudioLength = Integer.MIN_VALUE;
	private int screenWidth;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.graph_layout);

		DisplayMetrics displayMetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		screenWidth = displayMetrics.widthPixels;

		streamLayout = (StreamLayout) findViewById(R.id.stream_layout);
		streamLayout.setOnTouchListener(new View.OnTouchListener()
		{
			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				v.performClick();
				float percentage = event.getX() / screenWidth;
				playbackThreads.seekTo((int) (percentage * maxAudioLength));
				return true;
			}
		});

		initializeUI();
	}

	private void initializeUI()
	{
		playButton = (Button) findViewById(R.id.play);
		playButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				if (playbackThreads.isPlaying())
				{
					playbackThreads.pause();
					playButton.setText(R.string.play);
				}
				else
				{
					playbackThreads.play();
					playButton.setText(R.string.pause);
				}
			}
		});

		resetButton = (Button) findViewById(R.id.reset);
		resetButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				playbackThreads.reset();
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
							if (type.matches(SUPPORTED_MEDIA_TYPES))
							{
								AudioDecoder decoder = new AudioDecoder(file.getPath());
								int audioLength = decoder.getAudioLength();

								WaveformView waveform = new WaveformView(GraphActivity.this);
								waveform.setSamples(decoder.getSamples());
								waveform.setAudioLength(audioLength);
								streamLayout.addView(waveform, 0);

								showMediaButtons();

								playbackThreads.add(new PlaybackThread(GraphActivity.this, file));

								if (audioLength > maxAudioLength)
								{
									maxAudioLength = audioLength;
									playbackThreads.removePlaybackListener();
									PlaybackListener playbackListener = new PlaybackListener() {
										@Override
										public void onProgress(int progress)
										{
											streamLayout.setMarkerProgress(progress);
										}

										@Override
										public void onCompletion()
										{
											playButton.setText(R.string.play);
											streamLayout.resetMarker();
											playbackThreads.resetFinishedPlaying();
										}
									};
									playbackThreads.setPlaybackListener(playbackListener);
								}
							}
							else if (type.matches(FileCons.FILE_EXTENSION_STREAM))
							{
								Stream streamData = Stream.load(file.getPath());
								StreamView streamView = new StreamView(GraphActivity.this,
																	   streamData);
								streamLayout.addView(streamView);
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

	private void showMediaButtons()
	{
		playButton.setVisibility(View.VISIBLE);
		resetButton.setVisibility(View.VISIBLE);
	}
}
