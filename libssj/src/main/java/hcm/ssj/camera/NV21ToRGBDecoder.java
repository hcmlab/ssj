/*
 * NV21ToRGBDecoder.java
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

package hcm.ssj.camera;

import android.graphics.ImageFormat;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.SSJFatalException;
import hcm.ssj.core.Transformer;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.ImageStream;
import hcm.ssj.core.stream.Stream;

/**
 * Transformer that is responsible for decoding of NV21 raw image data into
 * RGB color format.
 *
 * @author Vitaly
 */
public class NV21ToRGBDecoder extends Transformer
{
	private static final int CHANNELS_PER_PIXEL = 3;

	private int width;
	private int height;

	public NV21ToRGBDecoder()
	{
		_name = "NV21ToRGBDecoder";
	}

	@Override
	public void enter(Stream[] stream_in, Stream stream_out) throws SSJFatalException
	{
		// Initialize image stream size
		ImageStream imgstrm = (ImageStream)stream_in[0];
		width = imgstrm.width;
		height = imgstrm.height;

		if(imgstrm.format != ImageFormat.NV21)
		{
			Log.e("Unsupported input video format. Expecting NV21.");
		}
	}

	@Override
	public void transform(Stream[] stream_in, Stream stream_out) throws SSJFatalException
	{
		// Fetch raw NV21 pixel data
		byte[] nv21Data = stream_in[0].ptrB();

		// Convert NV21 to RGB and save the pixel data to the output stream
		byte out[] = stream_out.ptrB();
		CameraUtil.convertNV21ToRGB(out, nv21Data, width, height, false);
	}

	@Override
	public int getSampleDimension(Stream[] stream_in)
	{
		ImageStream imgstrm = (ImageStream)stream_in[0];
		return imgstrm.width * imgstrm.height * CHANNELS_PER_PIXEL; //RGB
	}

	@Override
	public int getSampleBytes(Stream[] stream_in)
	{
		return stream_in[0].bytes;
	}

	@Override
	public Cons.Type getSampleType(Stream[] stream_in)
	{
		if(stream_in[0].type != Cons.Type.IMAGE)
			Log.e("Input stream type (" +stream_in[0].type.toString()+ ") is unsupported. Expecting " + Cons.Type.IMAGE.toString());
		return Cons.Type.IMAGE;
	}

	@Override
	public int getSampleNumber(int sampleNumber_in)
	{
		return sampleNumber_in;
	}

	@Override
	protected void describeOutput(Stream[] stream_in, Stream stream_out)
	{
		stream_out.desc = new String[] { "video" };

		((ImageStream) stream_out).width = ((ImageStream) stream_in[0]).width;
		((ImageStream) stream_out).height = ((ImageStream) stream_in[0]).height;
		((ImageStream) stream_out).format = 0x29; //ImageFormat.FLEX_RGB_888;
	}

	@Override
	public OptionList getOptions()
	{
		return null;
	}
}
