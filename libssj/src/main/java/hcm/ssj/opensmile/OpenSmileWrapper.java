/*
 * OpenSmileWrapper.java
 * Copyright (c) 2021
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

package hcm.ssj.opensmile;

import com.audeering.opensmile.OpenSmileAdapter;
import com.audeering.opensmile.smileres_t;

import java.util.HashMap;

import hcm.ssj.core.Log;
import hcm.ssj.core.SSJFatalException;

/**
 * Created by Michael Dietz on 05.05.2021.
 */
public class OpenSmileWrapper implements Runnable
{
	public static final int OS_LOG_LEVEL = 3;
	public static final int OS_DEBUG = 1;
	public final int OS_CONSOLE_OUTPUT;

	HashMap<String, String> params = new HashMap<>();
	boolean running = false;
	smileres_t state;
	Thread osThread;
	String configPath;

	OpenSmileAdapter osa;

	public OpenSmileWrapper(String configPath, boolean showLog)
	{
		this.configPath = configPath;

		if (showLog)
		{
			OS_CONSOLE_OUTPUT = 1;
		}
		else
		{
			OS_CONSOLE_OUTPUT = 0;
		}
	}

	public synchronized void start(OpenSmileDataCallback callback) throws SSJFatalException
	{
		if (!running)
		{
			running = true;

			osa = new OpenSmileAdapter();

			state = osa.smile_initialize(configPath, params, OS_LOG_LEVEL, OS_DEBUG, OS_CONSOLE_OUTPUT);
			checkState("smile_initialize");

			state = osa.smile_extsink_set_data_callback("externalSink", callback);
			checkState("smile_extsink_set_data_callback");

			osThread = new Thread(this);
			osThread.start();
		}
	}

	@Override
	public void run()
	{
		state = osa.smile_run();

		Log.i("openSMILE thread finished running!");
	}

	public synchronized void stop()
	{
		if (running)
		{
			if (osa != null)
			{
				osa.smile_extaudiosource_set_external_eoi("externalAudioSource");
				osa.smile_abort();

				try
				{
					osThread.join(1000);
				}
				catch (InterruptedException e)
				{
					Log.e("Failed to wait for openSMILE thread to finish.");
				}

				osa.smile_free();
			}

			running = false;
		}
	}

	public void writeData(byte[] data)
	{
		osa.smile_extaudiosource_write_data("externalAudioSource", data);
	}

	private void checkState(String step) throws SSJFatalException
	{
		if (state != smileres_t.SMILE_SUCCESS)
		{
			running = false;

			throw new SSJFatalException("openSMILE step '" + step + "' failed!");
		}
	}
}
