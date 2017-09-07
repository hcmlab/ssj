/*
 * AuditoryFeedback.java
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

package hcm.ssj.feedback;

import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.SoundPool;

import java.io.File;
import java.io.IOException;

import hcm.ssj.core.Log;
import hcm.ssj.core.SSJApplication;
import hcm.ssj.core.event.Event;
import hcm.ssj.core.option.Option;
import hcm.ssj.file.LoggingConstants;

/**
 * Created by Antonio Grieco on 06.09.2017.
 */

public class AuditoryFeedback extends Feedback
{
	public class Options extends Feedback.Options
	{
		public final Option<Float> intensity = new Option<>("intensity", 1.0f, Float.class, "intensity of auditory feedback");
		public final Option<Boolean> fromAssets = new Option<>("fromAssets", false, Boolean.class, "load  audio file from assets");
		public final Option<String> audioFilePath = new Option<>("audioFilePath", LoggingConstants.SSJ_EXTERNAL_STORAGE, String.class, "location of audio file");
		public final Option<String> audioFileName = new Option<>("audioFileName", "Creator/res/blop.mp3", String.class, "name of audio file");

		private Options()
		{
			super();
			addOptions();
		}
	}
	public final Options options = new Options();

	private SoundPool player;
	private int soundId;

	public AuditoryFeedback()
	{
		_name = "AuditoryFeedback";
		Log.d("Instantiated AuditoryFeedback "+this.hashCode());
	}

	@Override
	public void enter()
	{
		if(_evchannel_in == null || _evchannel_in.size() == 0)
			throw new RuntimeException("no input channels");

		player = new SoundPool(4, AudioManager.STREAM_NOTIFICATION, 0);

		try
		{
			if(options.fromAssets.get())
			{
				AssetFileDescriptor fd =  SSJApplication.getAppContext().getAssets().openFd(options.audioFileName.get());
				soundId = player.load(fd, 1);
				fd.close();
			}
			else
			{
				String path = options.audioFilePath.get() + File.separator + options.audioFileName.get();
				soundId = player.load(path, 1);
			}

		}
		catch(IOException e)
		{
			Log.e("error parsing config file", e);
		}
	}

	@Override
	public void notify(Event event)
	{
		// Execute only if lock has expired
		if(checkLock(options.lock.get()))
		{
			player.play(this.soundId, options.intensity.get(), options.intensity.get(), 1, 0, 1);
		}
	}



	@Override
	public void flush()
	{
		player.release();
	}
}
