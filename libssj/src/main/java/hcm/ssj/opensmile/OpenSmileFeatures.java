/*
 * OpenSmile.java
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

import android.content.Context;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.SSJApplication;
import hcm.ssj.core.SSJException;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.Transformer;
import hcm.ssj.core.Util;
import hcm.ssj.core.option.FilePath;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;
import hcm.ssj.file.FileCons;
import hcm.ssj.file.FileUtils;

/**
 * Created by Michael Dietz on 05.05.2021.
 */
public class OpenSmileFeatures extends Transformer
{
	public static final String DEFAULT_CONFIG_NAME = "opensmile_egemaps_23.conf";
	public static final String DEFAULT_CONFIG_FOLDER = "opensmile";
	public static final String DEFAULT_CONFIG_PATH = FileCons.CONFIGS_DIR + File.separator + DEFAULT_CONFIG_FOLDER;

	public class Options extends OptionList
	{
		public final Option<FilePath> configFile = new Option<>("path", new FilePath(DEFAULT_CONFIG_PATH + File.separator + DEFAULT_CONFIG_NAME), FilePath.class, "location of openSMILE config file");
		public final Option<Cons.AudioFormat> audioFormat = new Option<>("audioFormat", Cons.AudioFormat.ENCODING_DEFAULT, Cons.AudioFormat.class, "input audio format");
		public final Option<Integer> featureCount = new Option<>("featureCount", 23, Integer.class, "number of openSMILE features");
		public final Option<String> featureNames = new Option<>("featureNames", "loudness,alphaRatio,hammarbergIndex,slope0-500,slope500-1500,spectralFlux,mfcc1,mfcc2,mfcc3,mfcc4,F0semitoneFrom27.5Hz,jitterLocal,shimmerLocaldB,HNRdBACF,logRelF0-H1-H2,logRelF0-H1-A3,F1frequency,F1bandwidth,F1amplitudeLogRelF0,F2frequency,F2amplitudeLogRelF0,F3frequency,F3amplitudeLogRelF0", String.class, "names of features separated by comma");
		public final Option<Boolean> showLog = new Option<>("showLog", false, Boolean.class, "show openSMILE log output");

		private Options()
		{
			addOptions();
		}
	}

	public final Options options = new Options();

	Cons.AudioDataFormat dataFormat;
	OpenSmileWrapper osWrapper;
	OpenSmileDataCallback callback;
	String[] featureNames;
	byte[] byteBuffer;

	public OpenSmileFeatures()
	{
		_name = this.getClass().getSimpleName();
	}

	@Override
	public OptionList getOptions()
	{
		return options;
	}

	@Override
	public void init(double frame, double delta) throws SSJException
	{
		super.init(frame, delta);

		Context context = SSJApplication.getAppContext();
		List<String> assetList = new ArrayList<>();

		FileUtils.listAssetFiles(context, DEFAULT_CONFIG_FOLDER, assetList);

		for (String file : assetList)
		{
			String targetPath = FileCons.CONFIGS_DIR + File.separator + file;

			// Copy default openSMILE config if it does not exist
			if (!new File(targetPath).exists())
			{
				Log.i("Providing openSMILE config: " + file);
				FileUtils.copyAsset(context, file, targetPath);
			}
		}
	}

	@Override
	public void enter(Stream[] stream_in, Stream stream_out) throws SSJFatalException
	{
		if (stream_in.length != 1)
		{
			throw new SSJFatalException("Stream count not supported");
		}

		switch (stream_in[0].type)
		{
			case BYTE:
			{
				dataFormat = Cons.AudioDataFormat.BYTE;
				break;
			}
			case SHORT:
			{
				dataFormat = Cons.AudioDataFormat.SHORT;
				break;
			}
			case FLOAT:
			{
				if (options.audioFormat.get() == Cons.AudioFormat.ENCODING_DEFAULT || options.audioFormat.get() == Cons.AudioFormat.ENCODING_PCM_16BIT)
				{
					dataFormat = Cons.AudioDataFormat.FLOAT_16;
				}
				else if (options.audioFormat.get() == Cons.AudioFormat.ENCODING_PCM_8BIT)
				{
					dataFormat = Cons.AudioDataFormat.FLOAT_8;
				}
				else
				{
					Log.e("Audio format not supported");
				}
				break;
			}
			default:
			{
				Log.e("Stream type not supported");
				return;
			}
		}

		Log.d("Audio format: " + dataFormat.toString());
		byteBuffer = new byte[stream_in[0].num * stream_in[0].dim * dataFormat.byteCount];

		callback = new OpenSmileDataCallback();
		osWrapper = new OpenSmileWrapper(options.configFile.get().toString(), options.showLog.get());
		osWrapper.start(callback);
	}

	@Override
	public void transform(Stream[] stream_in, Stream stream_out) throws SSJFatalException
	{
		// Fill byte buffer (inverse operations from audio channel)
		switch (dataFormat)
		{
			case BYTE:
			{
				byte[] audioIn = stream_in[0].ptrB();

				System.arraycopy(audioIn, 0, byteBuffer, 0, byteBuffer.length);
				break;
			}
			case SHORT:
			{
				short[] audioIn = stream_in[0].ptrS();

				ByteBuffer.wrap(byteBuffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(audioIn, 0, byteBuffer.length / 2);
				break;
			}
			case FLOAT_8:
			{
				float[] audioIn = stream_in[0].ptrF();

				for (int i = 0; i < audioIn.length; i++)
				{
					byteBuffer[i] = (byte) (audioIn[i] * 128);
				}
				break;
			}
			case FLOAT_16:
			{
				float[] audioIn = stream_in[0].ptrF();

				for (int i = 0, j = 0; i < audioIn.length; i++)
				{
					short value = (short) (audioIn[i] * 32768);
					byteBuffer[j++] = (byte) (value & 0xff);
					byteBuffer[j++] = (byte) ((value >> 8) & 0xff);
				}
				break;
			}
		}

		osWrapper.writeData(byteBuffer);

		if (callback.openSmileData != null)
		{
			float[] out = stream_out.ptrF();
			System.arraycopy(callback.getData(), 0, out, 0, out.length);
		}
	}

	@Override
	public void flush(Stream[] stream_in, Stream stream_out) throws SSJFatalException
	{
		if (osWrapper != null)
		{
			osWrapper.stop();
		}
	}

	@Override
	public int getSampleDimension(Stream[] stream_in)
	{
		return options.featureCount.get();
	}

	@Override
	public int getSampleBytes(Stream[] stream_in)
	{
		return Util.sizeOf(getSampleType(stream_in));
	}

	@Override
	public Cons.Type getSampleType(Stream[] stream_in)
	{
		return Cons.Type.FLOAT;
	}

	@Override
	public int getSampleNumber(int sampleNumber_in)
	{
		return 1;
	}

	@Override
	protected void describeOutput(Stream[] stream_in, Stream stream_out)
	{
		stream_out.desc = new String[stream_out.dim];

		if (options.featureNames.get() != null && !options.featureNames.get().equalsIgnoreCase(""))
		{
			featureNames = options.featureNames.get().split(",");
		}

		for (int i = 0; i < stream_out.dim; i++)
		{
			if (featureNames != null && i < featureNames.length)
			{
				stream_out.desc[i] = featureNames[i];
			}
			else
			{
				stream_out.desc[i] = "OS feat " + (i + 1);
			}
		}
	}
}
