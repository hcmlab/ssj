/*
 * PlaybackThread.java
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

package hcm.ssj.audio;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Background thread for audio playback.
 */
public class PlaybackThread
{
	private final static int INITIAL_PLAYBACK_DELAY = 0;
	private final static int MARKER_UPDATE_INTERVAL = 1000 / 60;

	private PlaybackListener playbackListener;
	private MediaPlayer mediaPlayer;
	private Context context;
	private File audioFile;
	private ScheduledExecutorService executor;
	private Runnable markerUpdateTask;

	private boolean finishedPlaying = false;

	public PlaybackThread(Context c, File file)
	{
		context = c.getApplicationContext();
		audioFile = file;
		loadMedia();
	}

	/**
	 * Plays the loaded audio file and starts updating marker's position.
	 */
	public void play()
	{
		if (mediaPlayer != null && !mediaPlayer.isPlaying() && !finishedPlaying)
		{
			mediaPlayer.start();
			startUpdatingMarkerPosition();
		}
	}

	/**
	 * Pauses the playback of the loaded audio file.
	 */
	public void pause()
	{
		if (mediaPlayer != null && mediaPlayer.isPlaying())
		{
			mediaPlayer.pause();
		}
	}

	/**
	 * Stops and resets the playback of the loaded audio file and repositions the marker's
	 * position to the origin.
	 */
	public void reset()
	{
		if (mediaPlayer != null)
		{
			mediaPlayer.reset();
		}
		loadMedia();
		stopUpdatingMarkerPosition();
	}

	public void resetFinishedPlaying()
	{
		finishedPlaying = false;
	}

	/**
	 * Sets the listener for the current thread. Only one such thread is allowed to have
	 * a {@link hcm.ssj.audio.PlaybackListener}.
	 * @param listener {@link hcm.ssj.audio.PlaybackListener}.
	 */
	public void setPlaybackListener(PlaybackListener listener)
	{
		playbackListener = listener;
	}

	/**
	 * Removes the listener of the current thread.
	 */
	public void removePlaybackListener()
	{
		playbackListener = null;
	}

	/**
	 * Returns true if the current thread is currently playing audio.
	 * @return True if audio is being played back, false otherwise.
	 */
	public boolean isPlaying()
	{
		return mediaPlayer != null && mediaPlayer.isPlaying();
	}

	/**
	 * Skips the playback of the current thread to the selected time.
	 * @param progress Time in milliseconds to skip forward or backward to.
	 */
	public void seekTo(int progress)
	{
		if (mediaPlayer != null)
		{
			mediaPlayer.seekTo(progress);
			play();
		}
	}

	/**
	 * Loads audio file and sets up OnCompletionListener.
	 */
	private void loadMedia()
	{
		if (context != null && audioFile != null)
		{
			mediaPlayer = MediaPlayer.create(context.getApplicationContext(), Uri.fromFile(audioFile));
			mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener()
			{
				@Override
				public void onCompletion(MediaPlayer mp)
				{
					finishedPlaying = true;
					if (playbackListener != null)
					{
						playbackListener.onCompletion();
					}
				}
			});
		}
	}

	/**
	 * Starts updating playback marker's progress at a specified time interval.
	 */
	private void startUpdatingMarkerPosition()
	{
		if (executor == null)
		{
			executor = Executors.newSingleThreadScheduledExecutor();
		}
		if (markerUpdateTask == null)
		{
			markerUpdateTask = new Runnable()
			{
				@Override
				public void run()
				{
					updateMarkerProgress();
				}
			};
		}
		executor.scheduleAtFixedRate(
				markerUpdateTask,
				INITIAL_PLAYBACK_DELAY,
				MARKER_UPDATE_INTERVAL,
				TimeUnit.MILLISECONDS
		);
	}

	/**
	 * Stops updating playback marker's progress.
	 */
	private void stopUpdatingMarkerPosition()
	{
		if (executor != null)
		{
			executor.shutdown();
			executor = null;
			markerUpdateTask = null;
			if (playbackListener != null)
			{
				playbackListener.onCompletion();
			}
		}
	}

	/**
	 * Updates playback marker's progress.
	 */
	private void updateMarkerProgress()
	{
		if (mediaPlayer != null && mediaPlayer.isPlaying())
		{
			int currentPosition = mediaPlayer.getCurrentPosition();
			if (playbackListener != null)
			{
				playbackListener.onProgress(currentPosition);
			}
		}
	}
}
