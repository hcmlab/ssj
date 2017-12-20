/*
 * PlaybackThreadList.java
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

package hcm.ssj.creator.util;

import java.util.ArrayList;

import hcm.ssj.audio.PlaybackListener;
import hcm.ssj.audio.PlaybackThread;

public class PlaybackThreadList
{
	private ArrayList<PlaybackThread> playbackThreads = new ArrayList<>();

	public void add(PlaybackThread thread)
	{
		playbackThreads.add(thread);
	}

	public void play()
	{
		for (PlaybackThread thread : playbackThreads)
		{
			thread.play();
		}
	}

	public void pause()
	{
		for (PlaybackThread thread : playbackThreads)
		{
			thread.pause();
		}
	}

	public void reset()
	{
		for (PlaybackThread thread : playbackThreads)
		{
			thread.reset();
		}
	}

	public void removePlaybackListeners()
	{
		for (PlaybackThread thread : playbackThreads)
		{
			thread.removePlaybackListener();
		}
	}

	public void setPlaybackListener(PlaybackListener listener)
	{
		playbackThreads.get(playbackThreads.size() - 1).setPlaybackListener(listener);
	}
}
