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

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import java.nio.ShortBuffer;

/**
 * Background thread for audio playback.
 */
public class PlaybackThread
{
	private Thread thread;
	private PlaybackListener playbackListener;

	private ShortBuffer samplesBuffer;

	private int sampleRate;
	private int numSamples;

	private boolean shouldContinue;

	public PlaybackThread(short[] samples, int rate, PlaybackListener listener)
	{
		playbackListener = listener;
		sampleRate = rate;
		try
		{
			numSamples = samples.length;
			samplesBuffer = ShortBuffer.wrap(samples);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void startPlayback()
	{
		if (thread != null)
		{
			return;
		}
		shouldContinue = true;
		thread = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				play();
			}
		});
		thread.start();
	}

	public void stopPlayback()
	{
		if (thread == null)
		{
			return;
		}
		shouldContinue = false;
		thread = null;
	}

	public boolean isPlaying()
	{
		return thread != null;
	}

	/**
	 * Play raw audio file. Correct sample rate is set automatically.
	 * To query the encoding type the API level of at least 24 is necessary, so for now
	 * default encoding (PCM-16bit) is hard coded.
	 */
	private void play()
	{
		int bufferSize = AudioTrack.getMinBufferSize(sampleRate,
													 AudioFormat.CHANNEL_OUT_MONO,
													 AudioFormat.ENCODING_PCM_16BIT);
		if (bufferSize == AudioTrack.ERROR || bufferSize == AudioTrack.ERROR_BAD_VALUE)
		{
			bufferSize = sampleRate * 2;
		}

		AudioTrack audioTrack = new AudioTrack(
				AudioManager.STREAM_MUSIC,
				sampleRate,
				AudioFormat.CHANNEL_OUT_MONO,
				AudioFormat.ENCODING_DEFAULT,
				bufferSize,
				AudioTrack.MODE_STREAM
		);

		audioTrack.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener()
		{
			@Override
			public void onPeriodicNotification(AudioTrack track)
			{
				if (playbackListener != null && track.getPlayState() == AudioTrack.PLAYSTATE_PLAYING)
				{
					playbackListener.onProgress((track.getPlaybackHeadPosition() * 1000) / sampleRate);
				}
			}
			@Override
			public void onMarkerReached(AudioTrack track)
			{
				track.release();
				stopPlayback();
				if (playbackListener != null)
				{
					playbackListener.onCompletion();
				}
			}
		});

		// Notification occurs 30 times per second.
		audioTrack.setPositionNotificationPeriod(sampleRate / 30);
		audioTrack.setNotificationMarkerPosition(numSamples);

		audioTrack.play();

		short[] buffer = new short[bufferSize];
		samplesBuffer.rewind();
		int limit = numSamples;
		while (samplesBuffer.position() < limit && shouldContinue)
		{
			int numSamplesLeft = limit - samplesBuffer.position();
			int samplesToWrite;
			if (numSamplesLeft >= buffer.length)
			{
				samplesBuffer.get(buffer);
				samplesToWrite = buffer.length;
			}
			else
			{
				for (int i = numSamplesLeft; i < buffer.length; i++)
				{
					buffer[i] = 0;
				}
				samplesBuffer.get(buffer, 0, numSamplesLeft);
				samplesToWrite = numSamplesLeft;
			}
			audioTrack.write(buffer, 0, samplesToWrite);
		}

		if (!shouldContinue)
		{
			audioTrack.release();
		}
	}
}
