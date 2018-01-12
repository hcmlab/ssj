/*
 * PlaybackThreadList.java
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

package hcm.ssj.creator.util;

import java.util.ArrayList;

import hcm.ssj.audio.PlaybackListener;
import hcm.ssj.audio.PlaybackThread;

/**
 * Convenience class for playback thread management. Only one thread is allowed to have a
 * {@link hcm.ssj.audio.PlaybackListener}, as playback marker's position is updated by a single
 * thread (the one with the longest audio duration, meaning that the marker is reset only when
 * the longest audio file is finished playing).
 */
public class PlaybackThreadList
{
	private ArrayList<PlaybackThread> playbackThreads = new ArrayList<>();

	private int leadThreadIndex = 0;

	public void add(PlaybackThread thread)
	{
		playbackThreads.add(thread);
	}

	/**
	 * Starts playing all threads.
	 */
	public void play()
	{
		for (PlaybackThread thread : playbackThreads)
		{
			thread.play();
		}
	}

	/**
	 * Pauses playback of all threads.
	 */
	public void pause()
	{
		for (PlaybackThread thread : playbackThreads)
		{
			thread.pause();
		}
	}

	/**
	 * Stops playback of all threads.
	 */
	public void reset()
	{
		for (PlaybackThread thread : playbackThreads)
		{
			thread.reset();
		}
	}

	/**
	 * Prepares playback threads to be played.
	 */
	public void resetFinishedPlaying()
	{
		for (PlaybackThread thread : playbackThreads)
		{
			thread.resetFinishedPlaying();
		}
	}

	/**
	 * Skips playback of all threads to the given time.
	 * @param progress Time in milliseconds to skip to.
	 */
	public void seekTo(int progress)
	{
		resetFinishedPlaying();
		for (PlaybackThread thread : playbackThreads)
		{
			thread.seekTo(progress);
		}
	}

	/**
	 * Removes playback listener of the leading thread. This method should be called whenever a new
	 * audio file is selected for visualization which has audio length longer than any
	 * previously selected audio files.
	 */
	public void removePlaybackListener()
	{
		playbackThreads.get(leadThreadIndex).removePlaybackListener();
	}

	/**
	 * Sets {@link hcm.ssj.audio.PlaybackListener} on the newly created thread, and updates the
	 * index of the leading playback thread. This method should be called whenever a new
	 * audio file is selected for visualization which has audio length longer than any previously
	 * selected audio files, meaning this thread is now the leading thread, and thus is responsible
	 * for playback marker progress update.
	 * @param listener Listener for the lead playback thread.
	 */
	public void setPlaybackListener(PlaybackListener listener)
	{
		leadThreadIndex = playbackThreads.size() - 1;
		playbackThreads.get(leadThreadIndex).setPlaybackListener(listener);
	}

	/**
	 * Return true if at least one thread in the collection is currently running.
	 * @return True if at least one thread is currently running, false otherwise.
	 */
	public boolean isPlaying()
	{
		for (PlaybackThread thread : playbackThreads)
		{
			if (thread.isPlaying())
			{
				return true;
			}
		}
		return false;
	}
}
