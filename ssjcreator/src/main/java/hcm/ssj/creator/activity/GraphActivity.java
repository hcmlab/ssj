/*
 * GraphActivity.java
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

package hcm.ssj.creator.activity;

import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.obsez.android.lib.filechooser.ChooserDialog;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;

import hcm.ssj.creator.R;
import hcm.ssj.creator.main.GraphDrawer;
import hcm.ssj.file.FileCons;

/**
 * Visualize user-saved stream file data with the GraphView.
 */
public class GraphActivity extends AppCompatActivity
{
	private GraphView graph;
	private ChooserDialog chooserDialog;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.graph_layout);

		graph = (GraphView) findViewById(R.id.graph);

		Button loadButton = (Button) findViewById(R.id.load_stream_file);
		loadButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				chooserDialog = new ChooserDialog().with(GraphActivity.this);
				chooserDialog.withStartFile(Environment.getExternalStorageDirectory().getPath());
				chooserDialog.withChosenListener(new ChooserDialog.Result() {
					@Override
					public void onChoosePath(String path, File pathFile) {
						/*
						GraphDrawer drawer = new GraphDrawer(graph);
						drawer.drawGraph(pathFile);
						*/
						try
						{
							decode(pathFile.getPath());
						}
						catch (Exception e)
						{
							e.printStackTrace();
						}
					}
				}).build();
				chooserDialog.show();
			}
		});
	}

	private void decode(String filepath) throws Exception
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
		BufferInfo bufferInfo = new BufferInfo();
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
				break;
			}
		}
	}
}
