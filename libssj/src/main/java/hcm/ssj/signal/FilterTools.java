/*
 * FilterTools.java
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

package hcm.ssj.signal;

import hcm.ssj.core.Log;
import hcm.ssj.signal.Matrix.MATRIX_DIMENSION;

import static java.lang.Math.PI;

/**
 * Created by Michael Dietz on 13.08.2015.
 */
public class FilterTools
{
	private static FilterTools _instance = null;

	public enum WINDOW_TYPE
	{
		//! rectangular window
		RECTANGLE,
		//! triangle window
		TRIANGLE,
		//! gauss window
		GAUSS,
		//! hamming window
		HAMMING
	}

	;

	/**
	 * Private constructor for singleton pattern.
	 */
	private FilterTools()
	{

	}

	/**
	 * Method for accessing the singleton instance.
	 *
	 * @return singleton instance
	 */
	public static FilterTools getInstance()
	{
		if (_instance == null)
		{
			_instance = new FilterTools();
		}

		return _instance;
	}

	public Matrix<Float> getLPButter(int order, double cutoff)
	{
		int sections = (order + 1) / 2;

		Matrix<Float> sos = new Matrix<>(sections, 6);
		sos.fillValue(1.0f);

		double freq = cutoff / 2.0;

		Matrix<Complex> poles = getButterPoles(sections, freq);

		for (int i = 0; i < sections; i++)
		{
			double poleReal = poles.getData(i).real();
			double poleImag = poles.getData(i).imag();
			double a1 = -2.0 * poleReal;
			double a2 = poleReal * poleReal + poleImag * poleImag;
			double gain = 4.0 / (1.0 + a1 + a2);

			sos.setData(i, 0, (float) (1.0 / gain));
			sos.setData(i, 1, (float) (2.0 / gain));
			sos.setData(i, 2, (float) (1.0 / gain));
			sos.setData(i, 3, (float) (1.0));
			sos.setData(i, 4, (float) (a1));
			sos.setData(i, 5, (float) (a2));
		}

		return sos;
	}

	public Matrix<Float> getHPButter(int order, double cutoff)
	{
		int sections = (order + 1) / 2;

		Matrix<Float> sos = new Matrix<>(sections, 6);
		sos.fillValue(1.0f);

		double freq = cutoff / 2.0;

		Matrix<Complex> poles = getButterPoles(sections, 0.5 - freq);

		for (int i = 0; i < sections; i++)
		{
			double poleReal = -poles.getData(i).real();
			double poleImag = poles.getData(i).imag();
			double a1 = -2.0 * poleReal;
			double a2 = poleReal * poleReal + poleImag * poleImag;

			Matrix<Complex> tmp = new Matrix<>(3, 1);
			tmp.setData(0, new Complex(1.0, 0.0));
			tmp.setData(1, new Complex(Math.cos(0.5 * PI), Math.sin(0.5 * PI)));
			tmp.setData(2, new Complex(Math.cos(PI), Math.sin(PI)));

			Matrix<Complex> tmp2 = new Matrix<>(1, 3);
			tmp2.setData(0, new Complex(1.0, 0.0));
			tmp2.setData(1, new Complex(a1, 0.0));
			tmp2.setData(2, new Complex(a2, 0.0));

			double gain = new Complex(2.0, 0.0).div(MatrixOps.getInstance().multiplyVector(tmp2, tmp)).mod();

			sos.setData(i, 0, (float) (1.0 / gain));
			sos.setData(i, 1, (float) (-2.0 / gain));
			sos.setData(i, 2, (float) (1.0 / gain));
			sos.setData(i, 3, (float) (1.0));
			sos.setData(i, 4, (float) (a1));
			sos.setData(i, 5, (float) (a2));
		}

		return sos;
	}

	public Matrix<Float> getBPButter(int order, double lowCutoff, double highCutoff)
	{
		int sections = (order + 1) / 2;

		Matrix<Float> sos = new Matrix<>(sections, 6);
		sos.fillValue(1.0f);

		double lFreq = lowCutoff / 2.0;
		double hFreq = highCutoff / 2.0;

		Matrix<Complex> polesTmp = getButterPoles(sections / 2, hFreq - lFreq);

		double wLow = 2 * PI * lFreq;
		double wHigh = 2 * PI * hFreq;
		double ang = Math.cos((wHigh + wLow) / 2) / Math.cos((wHigh - wLow) / 2);

		Matrix<Complex> poles = new Matrix<>(sections, 1);
		poles.fillValue(new Complex(0, 0));

		for (int i = 0; i < sections / 2; i++)
		{
			Complex p1 = new Complex(polesTmp.getData(i).real() + 1, polesTmp.getData(i).imag());
			Complex tmp = p1.times(p1).times(ang * ang * 0.25).minus(polesTmp.getData(i)).sqrt();

			poles.setData(2 * i, p1.times(ang * 0.5).plus(tmp));
			poles.setData(2 * i + 1, p1.times(ang * 0.5).minus(tmp));
		}

		for (int i = 0; i < sections; i++)
		{
			double poleReal = poles.getData(i).real();
			double poleImag = poles.getData(i).imag();
			double a1 = -2.0 * poleReal;
			double a2 = poleReal * poleReal + poleImag * poleImag;

			Matrix<Complex> tmp = new Matrix<>(3, 1);
			tmp.setData(0, new Complex(1.0, 0.0));
			tmp.setData(1, new Complex(Math.cos((lFreq + hFreq) * PI), Math.sin((lFreq + hFreq) * PI)));
			tmp.setData(2, new Complex(Math.cos(2 * (lFreq + hFreq) * PI), Math.sin(2 * (lFreq + hFreq) * PI)));

			Matrix<Complex> tmp2 = new Matrix<>(1, 3);
			tmp2.setData(0, new Complex(1.0, 0.0));
			tmp2.setData(1, new Complex(a1, 0.0));
			tmp2.setData(2, new Complex(a2, 0.0));

			double gain = Math.abs(new Complex(0.1685, 0.5556).div(MatrixOps.getInstance().multiplyVector(tmp2, tmp)).mod());

			sos.setData(i, 0, (float) (1.0 / gain));
			sos.setData(i, 1, 0.0f);
			sos.setData(i, 2, (float) (-1.0 / gain));
			sos.setData(i, 3, (float) (1.0));
			sos.setData(i, 4, (float) (a1));
			sos.setData(i, 5, (float) (a2));
		}

		return sos;
	}

	public Matrix<Complex> getButterPoles(int sections, double frequency)
	{
		int columns = 1;
		Matrix<Complex> poles = new Matrix<>(sections, columns);

		// Fill with zeros
		for (int row = 0; row < sections; row++)
		{
			for (int col = 0; col < columns; col++)
			{
				poles.setData(row, col, new Complex(0, 0));
			}
		}

		double w = PI * frequency;
		double tanW = Math.sin(w) / Math.cos(w);

		int polesIndex = 0;

		for (int m = sections; m <= 2 * sections - 1; m++)
		{
			double ang = (2.0 * m + 1) * PI / (4.0 * sections);
			double d = 1.0 - 2.0 * tanW * Math.cos(ang) + tanW * tanW;

			double real = (1.0 - tanW * tanW) / d;
			double imag = 2.0 * tanW * Math.sin(ang) / d;

			poles.setData(polesIndex++, new Complex(real, imag));
		}

		return poles;
	}

	Matrix<Float> Filterbank(int size, double sample_rate, Matrix<Float> intervals, WINDOW_TYPE type)
	{

		Matrix<Float> filterbank = new Matrix<>(intervals.getRows(), size);
		filterbank.fillValue(0f);

		sample_rate /= 2; // convert sampling to nyquist rate

		int intervalsptr = 0;
		for (int i = 0; i < filterbank.getRows(); i++)
		{

			int minind = (int) ((intervals.getData(intervalsptr) / sample_rate) * size + 0.5f);
			intervalsptr++;

			int maxind = (int) ((intervals.getData(intervalsptr) / sample_rate) * size + 0.5f);
			intervalsptr++;

			maxind = Math.min(maxind, size - 1);
			Matrix<Float> winmat = Window(1 + (maxind - minind), type, MATRIX_DIMENSION.ROW);
			MatrixOps.getInstance().div(winmat, MatrixOps.getInstance().sum(winmat));
			filterbank.setSubMatrix(i, minind, winmat);
		}

		return filterbank;
	}

	public Matrix<Float> Window(int size, WINDOW_TYPE type, MATRIX_DIMENSION dimension)
	{
		Matrix<Float> window;

		if (size < 1)
		{
			window = new Matrix<>(0, 0);
		}
		else if (size == 1)
		{
			window = new Matrix<>(1, 1);
			window.setData(0, 1f);
		}
		else
		{
			switch (type)
			{
				default:
				case RECTANGLE:
					window = new Matrix<>(1, size);
					window.fillValue(1f);
					break;

				case TRIANGLE:
					Log.e("window "+type.name()+" not supported yet");
					return null;

				case GAUSS:
					Log.e("window "+type.name()+" not supported yet");
					return null;

				case HAMMING:
				{
					window = MatrixOps.getInstance().array(0,1, size-1, MATRIX_DIMENSION.ROW);
					float scalar = (float) ((2.0 * PI) / size);

					MatrixOps.getInstance().mult (window, scalar);
					MatrixOps.getInstance().cos (window);
					MatrixOps.getInstance().mult (window, -0.46f);
					MatrixOps.getInstance().plus (window, 0.54f);
				}
				break;
			}

			// swap dimension if column vector
			if (dimension == MATRIX_DIMENSION.COL)
			{
				window.transpose();
			}
		}

		return window;
	}
}
