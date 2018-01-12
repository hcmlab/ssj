/*
 * AuditoryFeedback.java
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

package hcm.ssj.feedback;

import android.media.AudioManager;
import android.media.SoundPool;

import hcm.ssj.core.Log;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.event.Event;
import hcm.ssj.core.option.FilePath;
import hcm.ssj.core.option.Option;

/**
 * Created by Antonio Grieco on 06.09.2017.
 */

public class AuditoryFeedback extends Feedback
{
	public final Options options = new Options();
	private SoundPool player;
	private int soundId;

	public AuditoryFeedback()
	{
		_name = "AuditoryFeedback";
		Log.d("Instantiated AuditoryFeedback " + this.hashCode());
	}

	@Override
	public Feedback.Options getOptions()
	{
		return options;
	}

	@Override
	public void enterFeedback() throws SSJFatalException
	{
		if (_evchannel_in == null || _evchannel_in.size() == 0)
		{
			throw new SSJFatalException("no input channels");
		}

		player = new SoundPool(4, AudioManager.STREAM_NOTIFICATION, 0);
		soundId = player.load(options.audioFile.get().value, 1);
	}

	@Override
	public void notifyFeedback(Event event)
	{
		player.play(soundId, options.intensity.get(), options.intensity.get(), 1, 0, 1);
	}

	@Override
	public void flush() throws SSJFatalException
	{
		player.release();
	}

	public class Options extends Feedback.Options
	{
		public final Option<Float> intensity = new Option<>("intensity", 1.0f, Float.class, "intensity of auditory feedback");
		public final Option<FilePath> audioFile = new Option<>("audioFile", null, FilePath.class, "audiofile to play");

		private Options()
		{
			super();
			addOptions();
		}
	}
}
