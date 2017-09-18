/*
 * Interface.java
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

package hcm.ssj.mobileSSI;

import java.io.IOException;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Pipeline;
import hcm.ssj.core.stream.Stream;
import hcm.ssj.file.FileCons;
import hcm.ssj.file.FileUtils;

import static java.lang.System.loadLibrary;

/**
 * Interfaace for communication with SSI framework
 * Created by Ionut Damian on 07.09.2017.
 */
public class SSI
{
	public enum ObjectName
	{
		//TRANSFORMERS
		AudioActivity("ssiaudio"),
		AudioConvert("ssiaudio"),
		AudioIntensity("ssiaudio"),
		AudioLpc("ssiaudio"),
		AudioMono("ssiaudio"),
		SNRatio("ssiaudio"),

		ClassifierT("ssimodel"),

		Bundle("ssisignal"),
		Butfilt("ssisignal"),
		ConvPower("ssisignal"),
		Derivative("ssisignal"),
		DownSample("ssisignal"),
		Energy("ssisignal"),
		Expression("ssisignal"),
		FFTfeat("ssisignal"),
		Functionals("ssisignal"),
		Gate("ssisignal"),
		IIR("ssisignal"),
		Integral("ssisignal"),
		Intensity("ssisignal"),
		Limits("ssisignal"),
		Mean("ssisignal"),
		MFCC("ssisignal"),
		MvgAvgVar("ssisignal"),
		MvgConDiv("ssisignal"),
		MvgDrvtv("ssisignal"),
		MvgMedian("ssisignal"),
		MvgMinMax("ssisignal"),
		MvgNorm("ssisignal"),
		MvgPeakGate("ssisignal"),
		Noise("ssisignal"),
		Normalize("ssisignal"),
		Pulse("ssisignal"),
		Relative("ssisignal"),
		Spectrogram("ssisignal"),
		Statistics("ssisignal"),
		Sum("ssisignal");

		public String lib;
		ObjectName(String lib)
		{
			this.lib = lib;
		}
	}

	/**
	 * Copy library so it can be found and loaded by SSI
	 * @param name name of the library
	 * @param path path to the library
	 * @throws IOException
	 */
	public static void prepareLibrary(String name, String path) throws IOException
	{
		if(!name.startsWith("lib")) name = "lib" + name;
		if(!name.endsWith(".so")) name += ".so";

		if(path == null || path.length() == 0 || path.startsWith("http://") || path.startsWith("https://"))
		{
			Pipeline.getInstance().download(name, FileCons.REMOTE_LIB_PATH, FileCons.INTERNAL_LIB_DIR, true); //download from trusted server
		}
		else
			FileUtils.copyFile(name, path, FileCons.INTERNAL_LIB_DIR); //copy from sdcard

	}

	static
	{
		loadLibrary("ssissjbridge");
	}

	public static native long create(String name, String libname, String libpath);

	public static native boolean setOption(long ssiobj, String name, String value);

	public static native void transformEnter(long ssiobj, Stream stream_in, Stream stream_out);
	public static native void transform(long ssiobj, Stream stream_in, Stream stream_out);
	public static native void transformFlush(long ssiobj, Stream stream_in, Stream stream_out);

	public static native int getSampleNumberOut(long ssiobj, int sample_number_in);
	public static native int getSampleDimensionOut(long ssiobj, int sample_dimension_in);
	public static native int getSampleBytesOut(long ssiobj, int sample_bytes_in);
	public static native int getSampleTypeOut(long ssiobj, Cons.Type sample_type_in);

	public static native long clear();
}
