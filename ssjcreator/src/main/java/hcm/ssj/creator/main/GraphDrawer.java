/*
 * GraphDrawer.java
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

package hcm.ssj.creator.main;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import hcm.ssj.file.FileCons;
import hcm.ssj.file.FileUtils;

/**
 * Visualize stream file data on the GraphView.
 */
public class GraphDrawer
{
	/**
	 * Amount of data points shown on screen at once.
	 */
	private static final int MAX_DATA_POINTS = Integer.MAX_VALUE;

	private GraphView graph;

	/**
	 * Instantiate stream data visualizer.
	 * @param graphView Reference to the GraphView on which to draw the data.
	 */
	public GraphDrawer(GraphView graphView)
	{
		graph = graphView;
	}

	public void plot(File file)
	{
		String type = FileUtils.getFileType(file);
		if (type.equalsIgnoreCase("mp4"))
		{
			try
			{
				File decoded = decode(file.getPath());
				drawWaveform(decoded);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		else if (type.equalsIgnoreCase("stream~"))
		{
			drawGraph(file);
		}
	}

	/**
	 * Draw waveform out of bytes from a given raw audio file.
	 * @param file Raw file containing audio data.
	 */
	private void drawWaveform(File file)
	{
		LineGraphSeries<DataPoint> series = new LineGraphSeries<>();
		short[] samples = getAudioSample(file);
		int i = 0;
		for (short sample : samples)
		{
			DataPoint point = new DataPoint(i++, sample);
			series.appendData(point, false, MAX_DATA_POINTS);
		}
		graph.addSeries(series);
	}

	/**
	 * Convert given audio file into a byte array.
	 * @param file Audio file to convert.
	 * @return Byte array in little-endian byte order.
	 */
	private short[] getAudioSample(File file)
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
	 * Visualize data of a given stream file.
	 * @param file Stream file.
	 */
	private void drawGraph(File file)
	{
		graph.removeAllSeries();
		int columnNum = getColumnNum(file);
		try
		{
			for (int col = 0; col < columnNum; col++)
			{
				BufferedReader br = new BufferedReader(new FileReader(file));
				LineGraphSeries<DataPoint> series = new LineGraphSeries<>();
				int count = 0;
				String dataRow;
				while ((dataRow = br.readLine()) != null)
				{

					DataPoint point = new DataPoint(count++, Float.parseFloat(dataRow.split(" ")[col]));
					series.appendData(point, false, MAX_DATA_POINTS);
				}
				graph.addSeries(series);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Get number of columns of a given stream file.
	 * @param file Stream file.
	 * @return Number of columns.
	 */
	private int getColumnNum(File file)
	{
		try
		{
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line = br.readLine();
			return line.split(" ").length;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return 0;
		}
	}

	/**
	 * Decode audio file into a raw file.
	 * @param filepath Path of the file to decode.
	 * @return Decoded raw audio file.
	 * @throws Exception IOException or FileNotFound exception.
	 */
	private File decode(String filepath) throws Exception
	{
		// Set audio source for the extractor.
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
}
