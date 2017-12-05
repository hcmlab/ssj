/*
 * AudioUtils.java
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

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.Arrays;

import hcm.ssj.file.FileCons;

/**
 * Collection of helper functions for audio.
 */
public final class AudioUtils
{
	/**
	 * Prevent from instantiating the class.
	 */
	private AudioUtils() {}

	/**
	 * Calculate the length of the audio file in milliseconds.
	 * @param sampleCount Number of samples.
	 * @param sampleRate Sample rate (i.e. 16000, 44100, ..)
	 * @param channelCount Number of audio channels.
	 * @return length of audio file in seconds.
	 */
	public static int calculateAudioLength(int sampleCount, int sampleRate, int channelCount)
	{
		return ((sampleCount / channelCount) * 1000) / sampleRate;
	}

	public static short[][] getExtremes(short[] data, int sampleSize)
	{
		short[][] newData = new short[sampleSize][];
		int groupSize = data.length / sampleSize;

		for (int i = 0; i < sampleSize; i++)
		{
			short[] group = Arrays.copyOfRange(data, i * groupSize,
											   Math.min((i + 1) * groupSize, data.length));
			short min = Short.MAX_VALUE;
			short max = Short.MIN_VALUE;

			for (short a : group)
			{
				min = (short) Math.min(min, a);
				max = (short) Math.max(max, a);
			}
			newData[i] = new short[] { max, min };
		}
		return newData;
	}

	/**
	 * Convert given audio file into a byte array.
	 * @param file Audio file to convert.
	 * @return Byte array in little-endian byte order.
	 */
	public static short[] getAudioSample(File file)
	{
		byte[] data = new byte[(int) file.length()];
		try
		{
			FileInputStream fileInputStream = new FileInputStream(file);
			fileInputStream.read(data);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		ShortBuffer sb = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
		short[] samples = new short[sb.limit()];
		sb.get(samples);
		return samples;
	}

	/**
	 * Decode audio file into a raw file.
	 * @param filepath Path of the file to decode.
	 * @return Decoded raw audio file.
	 * @throws Exception IOException or FileNotFound exception.
	 */
	public static File decode(String filepath) throws Exception
	{
		// Set selected audio file as a source.
		MediaExtractor extractor = new MediaExtractor();
		extractor.setDataSource(filepath);

		// Get audio format.
		MediaFormat format = extractor.getTrackFormat(0);
		String mime = format.getString(MediaFormat.KEY_MIME);

		// Create and configure decoder based on audio format.
		MediaCodec decoder = MediaCodec.createDecoderByType(mime);
		decoder.configure(format, null, null, 0);
		decoder.start();

		// Create input/output buffers.
		ByteBuffer[] inputBuffers = decoder.getInputBuffers();
		ByteBuffer[] outputBuffers = decoder.getOutputBuffers();
		MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
		extractor.selectTrack(0);

		File dst = new File(FileCons.SSJ_EXTERNAL_STORAGE + File.separator + "output.raw");
		FileOutputStream f = new FileOutputStream(dst);

		boolean endOfStreamReached = false;

		while (true)
		{
			if (!endOfStreamReached)
			{
				int inputBufferIndex = decoder.dequeueInputBuffer(10 * 1000);
				if (inputBufferIndex >= 0)
				{
					ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
					int sampleSize = extractor.readSampleData(inputBuffer, 0);
					if (sampleSize < 0)
					{
						// Pass empty buffer and the end of stream flag to the codec.
						decoder.queueInputBuffer(inputBufferIndex, 0, 0,
												 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
						endOfStreamReached = true;
					}
					else
					{
						// Pass data-filled buffer to the decoder.
						decoder.queueInputBuffer(inputBufferIndex, 0, sampleSize,
												 extractor.getSampleTime(), 0);
						extractor.advance();
					}
				}
			}

			int outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, 10 * 1000);
			if (outputBufferIndex >= 0)
			{
				ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
				byte[] data = new byte[bufferInfo.size];
				outputBuffer.get(data);
				outputBuffer.clear();

				if (data.length > 0)
				{
					f.write(data, 0, data.length);
				}
				decoder.releaseOutputBuffer(outputBufferIndex, false);

				if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0)
				{
					endOfStreamReached = true;
				}
			}
			else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED)
			{
				outputBuffers = decoder.getOutputBuffers();
			}

			if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0)
			{
				return dst;
			}
		}
	}

	/**
	 * Get sample rate of a given file.
	 * @param filepath Path of the file.
	 * @return Sample rate.
	 * @throws IOException if extractor couldn't set data source.
	 */
	public static int getSampleRate(String filepath) throws IOException
	{
		MediaExtractor extractor = new MediaExtractor();
		extractor.setDataSource(filepath);
		MediaFormat format = extractor.getTrackFormat(0);
		return format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
	}
}
