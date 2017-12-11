/*
 * PlaybackThread.java
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

package hcm.ssj.audio;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;

import java.io.File;

/**
 * Background thread for audio playback.
 */
public class PlaybackThread
{
	private PlaybackListener playbackListener;
	private MediaPlayer mediaPlayer;
	private Context context;
	private File audioFile;

	public PlaybackThread(Context c, File file, PlaybackListener listener)
	{
		playbackListener = listener;
		context = c.getApplicationContext();
		audioFile = file;
		loadMedia();
	}

	public void play()
	{
		if (mediaPlayer != null && !mediaPlayer.isPlaying())
		{
			mediaPlayer.start();
		}
	}

	public void pause()
	{
		if (mediaPlayer != null && mediaPlayer.isPlaying())
		{
			mediaPlayer.pause();
		}
	}

	public void reset()
	{
		if (mediaPlayer != null)
		{
			mediaPlayer.reset();
		}
		loadMedia();
	}

	public boolean isPlaying()
	{
		return mediaPlayer.isPlaying();
	}

	private void loadMedia()
	{
		if (context != null && audioFile != null)
		{
			mediaPlayer = MediaPlayer.create(context.getApplicationContext(), Uri.fromFile(audioFile));
		}
	}
}
